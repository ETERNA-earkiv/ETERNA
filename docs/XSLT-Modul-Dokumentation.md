# XSLT-modul för ETERNA

## Innehållsförteckning

1. [Översikt](#översikt)
2. [Arkitektur](#arkitektur)
3. [Hur XSLT-stilmallar hittas](#hur-xslt-stilmallar-hittas)
4. [Paketera XSLT i E-ARK SIP](#paketera-xslt-i-e-ark-sip)
5. [Globala stilmallar](#globala-stilmallar)
6. [Välj stilmall via dropdown](#välj-stilmall-via-dropdown)
7. [Visa original-XML](#visa-original-xml)
8. [Ladda upp egen XSLT](#ladda-upp-egen-xslt)
9. [Skriv ut dokument](#skriv-ut-dokument)
10. [Behörigheter](#behörigheter)
11. [REST API](#rest-api)
12. [Konfiguration](#konfiguration)
13. [Tekniska detaljer](#tekniska-detaljer)
14. [Kända begränsningar](#kända-begränsningar)

---

## Översikt

XSLT-modulen gör det möjligt att visa XML-filer i ETERNA som formaterade, läsbara HTML-dokument genom att applicera XSLT-stilmallar. Modulen stödjer tre nivåer av stilmallar:

1. **AIP-bundna stilmallar** — XSLT-filer som paketeras tillsammans med XML-filerna i AIP:ets dokumentationsmapp. Varje XML-fil kan ha sin egen matchande stilmall.
2. **Globala stilmallar** — Fördefinierade stilmallar som konfigureras i ETERNA och matchas via XML-namnrymd.
3. **Användaruppladdade stilmallar** — Användare med rätt behörighet kan ladda upp en egen XSLT-fil direkt i gränssnittet för att tillfälligt transformera ett dokument.

Utöver stilmallsval erbjuder modulen:

- **Dropdown för flera stilmallar** — Om AIP:et innehåller fler än en XSLT-fil för en XML-fil visas en dropdown där användaren kan välja vilken stilmall som ska tillämpas.
- **Visa original-XML** — En knapp låter användaren växla mellan renderad HTML-vy och oformaterad XML-källkod.

### Sökordning

När en XML-fil visas söker systemet efter stilmallar i följande ordning:

```
1. AIP-dokumentation (representationsnivå)
   └── Filnamnsträff (t.ex. Jrnl4.xml → Jrnl4.xslt)
   └── Första XSLT som fallback
2. AIP-dokumentation (paketnivå)
   └── Filnamnsträff
   └── Första XSLT som fallback
3. Global stilmall (via namnrymdsdetektering)
4. Ingen stilmall → 404 Not Found
```

---

## Arkitektur

### Komponentöversikt

```
┌─────────────────────────────────────────────────────────┐
│  Webbläsare (GWT-klient)                                │
│  BitstreamPreview.java                                   │
│  ┌─────────┐ ┌──────────────┐ ┌───────────────────────┐ │
│  │ Skriv ut│ │ Välj XSLT    │ │ Applicera XSLT        │ │
│  └────┬────┘ └──────┬───────┘ └───────────┬───────────┘ │
│       │              │                     │             │
│  ┌────▼──────────────▼─────────────────────▼───────────┐ │
│  │              <iframe srcdoc="...">                   │ │
│  │              Renderad HTML                           │ │
│  └─────────────────────────────────────────────────────┘ │
└────────────────────┬───────────────────────┬─────────────┘
                     │ GET                   │ POST
                     ▼                       ▼
┌────────────────────────────────────────────────────────────┐
│  REST API (FilesController.java)                           │
│  GET  /api/v2/files/{uuid}/preview/html                    │
│  POST /api/v2/files/{uuid}/preview/html/transform          │
└────────────────────┬───────────────────────┬───────────────┘
                     │                       │
                     ▼                       ▼
┌────────────────────────────────────────────────────────────┐
│  Tjänstelager (FilesService.java)                          │
│  - Namnrymdsdetektering (XMLStreamReader)                  │
│  - XSLT-sökning i AIP-dokumentation                        │
│  - Fallback till global stilmall                           │
└────────────────────┬───────────────────────┬───────────────┘
                     │                       │
                     ▼                       ▼
┌────────────────────────────────────────────────────────────┐
│  Transformering (RodaUtils.java + HTMLUtils.java)          │
│  - Saxon HE (XSLT 2.0-processor)                          │
│  - Cache för kompilerade stilmallar (1 min TTL)            │
│  - i18n-parametrar till stilmallar                         │
└────────────────────────────────────────────────────────────┘
```

### Berörda filer

| Fil | Ansvar |
|-----|--------|
| `FilesController.java` | REST-endpoints för förhandsvisning och transformation |
| `FilesService.java` | Affärslogik: XSLT-sökning, namnrymdsdetektering, orchestrering |
| `HTMLUtils.java` | Delegering till Saxon, i18n-hantering |
| `RodaUtils.java` | Saxon XSLT 2.0-kompilering och transformation |
| `BitstreamPreview.java` | GWT-klient: iframe, verktygsfält, utskrift |
| `roda-roles.properties` | Rollmappning för behörighet |
| `roda-wui.properties` | UI-registrering av roller |
| `crosswalks/dissemination/html/` | Globala stilmallar |

---

## Hur XSLT-stilmallar hittas

### Steg 1: AIP-dokumentation (representationsnivå)

Systemet söker i AIP:ets representationsdokumentation (`representations/rep1/documentation/`) efter `.xslt`-filer.

**Filnamnsträff:** Om XML-filen heter `Jrnl4.xml` letar systemet efter `Jrnl4.xslt`. Om en exakt matchning hittas används den direkt.

**Fallback:** Om ingen filnamnsträff finns, används den första `.xslt`-filen som hittas i dokumentationsmappen.

### Steg 2: AIP-dokumentation (paketnivå)

Om ingen XSLT hittas på representationsnivå söker systemet i paketets dokumentationsmapp (`documentation/`).

> **OBS:** På grund av en begränsning i commons-ip2-biblioteket (v2.10.1) hamnar representationsdokumentation ofta på paketnivå efter inmatning, även om den var korrekt placerad på representationsnivå i SIP-paketet. Därför är fallback till paketnivå viktig.

### Steg 3: Global stilmall via namnrymd

Om ingen AIP-bundlad XSLT hittas detekterar systemet XML-filens namnrymd (namespace) och slår upp en global stilmall via konfigurationen.

Namnrymdsdetekteringen:
1. Läser de första 4 KB av XML-filen
2. Använder `XMLStreamReader` för att hitta rotelementets namnrymd
3. Matchar mot konfigurerade regler i `ui.viewer.xslt.representation.rules`

Exempel: En XML-fil med namnrymden `http://www.example.org/ns/v1` matchas mot stilmallen `example_v1.xslt`.

### Steg 4: Ingen stilmall

Om ingen stilmall hittas returneras HTTP 404. Klienten visar då XML-filen som ren text istället.

---

## Paketera XSLT i E-ARK SIP

### Bakgrund

Enligt E-ARK CSIP 2.0 (CSIPSTR16) kan dokumentation placeras antingen på paketnivå (`documentation/`) eller på representationsnivå (`representations/rep1/documentation/`). XSLT-stilmallar bör placeras på representationsnivå för att tydligt kopplas till den representation de tillhör.

### SIP-paketstruktur

```
SIP-200801010141-xxxxxxxx/
├── METS.xml                                    (rot-METS)
├── metadata/
│   └── descriptive/
│       ├── 200801010141.xml                    (verksamhets-XML)
│       └── dc.xml                              (Dublin Core-metadata)
├── schemas/
│   └── ...
└── representations/
    └── rep1/
        ├── METS.xml                            (representations-METS)
        ├── data/
        │   ├── 200801010141_Jrnl1.xml
        │   ├── 200801010141_Jrnl2.xml
        │   └── ...
        └── documentation/
            ├── 200801010141_Jrnl1.xslt         (stilmall för Jrnl1)
            ├── 200801010141_Jrnl2.xslt         (stilmall för Jrnl2)
            └── ...
```

### Krav på representations-METS

För att stilmallarna ska upptäckas vid inmatning måste representations-METS (`representations/rep1/METS.xml`) innehålla:

**1. Filgrupp i `<mets:fileSec>`:**

```xml
<mets:fileGrp ID="fileGrp-rep1-documentation" USE="Documentation">
  <mets:file ID="file-0100" MIMETYPE="application/xslt+xml"
             SIZE="1234" CHECKSUM="abc123..." CHECKSUMTYPE="SHA-256">
    <mets:FLocat LOCTYPE="URL" xlink:type="simple"
                 xlink:href="documentation/200801010141_Jrnl1.xslt"/>
  </mets:file>
  <!-- fler filer... -->
</mets:fileGrp>
```

**2. Filpekare i `<mets:structMap>`:**

```xml
<mets:div ID="div-rep1-documentation" LABEL="Documentation">
  <mets:fptr FILEID="fileGrp-rep1-documentation"/>
</mets:div>
```

> **Viktigt:** `<mets:fptr>`-elementet är obligatoriskt. Utan det ignorerar commons-ip2-biblioteket dokumentationsfilerna vid inmatning.

### Bygga SIP-paket

För att paketera XSLT-stilmallar tillsammans med XML-filer i ett E-ARK SIP-paket
behöver två steg utföras: generera stilmallar och paketera dem. Vilka verktyg
som används är upp till respektive uppsättning — det här avsnittet beskriver
vad paketeringen ska åstadkomma.

Stegen:
1. Samla alla `.xslt`-filer för respektive XML-namespace.
2. Kopiera representationsfiler från ett befintligt SIP eller skapa nya.
3. Injicera dokumentationsreferenser i representations-METS (se `<mets:fptr>`-avsnittet ovan).
4. Placera stilmallarna i `representations/rep1/documentation/`.
5. Paketera till ett E-ARK SIP som ZIP-fil.

---

## Globala stilmallar

Globala stilmallar finns i:

```
config/crosswalks/dissemination/html/              (för beskrivande metadata)
config/crosswalks/dissemination/html/representation/  (för representationsfiler)
```

### Tillgängliga stilmallar

| Stilmall | Beskrivning |
|----------|-------------|
| `dc.xslt` | Dublin Core |
| `ead_2002.xslt` | Encoded Archival Description 2002 |
| `ead_3.xslt` | Encoded Archival Description 3 |
| `plain.xslt` | Ren textdump av XML |
| `key-value.xslt` | Generiska nyckel-värdepar |
| `event.xslt` | PREMIS-bevarandehändelser |

### Skapa en ny global stilmall

1. Skapa en `.xslt`-fil i `config/crosswalks/dissemination/html/representation/`
2. Konfigurera namnrymdsmatchning i `roda-wui.properties`:

```properties
ui.viewer.xslt.representation.rules = rule1
ui.viewer.xslt.representation.rules.rule1.namespace = http://www.example.com/ns
ui.viewer.xslt.representation.rules.rule1.xslt = min_stilmall
```

3. Stilmallen behöver inte kompileras — ETERNA läser den vid körning

### Stilmallsformat

ETERNA använder Saxon HE som stödjer XSLT 2.0. Stilmallar får tillgång till i18n-parametrar via XSLT-parametern `$i18n`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:ex="http://www.example.org/ns/v1">

  <xsl:output method="xml" indent="yes" omit-xml-declaration="yes"/>
  <xsl:param name="i18n" as="map(xs:string, xs:string)?" xmlns:xs="http://www.w3.org/2001/XMLSchema"/>

  <xsl:template match="/">
    <div class="descriptiveMetadata">
      <h1><xsl:value-of select="//ex:Title"/></h1>
      <!-- mer transformation... -->
    </div>
  </xsl:template>
</xsl:stylesheet>
```

---

## Välj stilmall via dropdown

Om AIP:et innehåller **fler än en XSLT-fil** i dokumentationsmappen för en XML-fil visas en dropdown ovanför den renderade vyn. Dropdown:en är dold om bara en stilmall finns.

### Så här gör du

1. Navigera till en XML-fil vars AIP har flera XSLT-filer i dokumentationen
2. Klicka på fliken **VISA** — dropdownen **"Välj stilmall:"** visas automatiskt
3. Välj önskad stilmall — vyn uppdateras direkt

### Tekniskt

- En ny API-endpoint `GET /api/v2/files/{uuid}/preview/html/xslts` returnerar listan av tillgängliga stilmallar som JSON
- GWT-klienten hämtar listan vid visning och visar `ListBox` om fler än ett alternativ finns
- Vid val anropas `GET /api/v2/files/{uuid}/preview/html?xslt={id}` med stilmallens filnamn som `id`
- Filnamnsträff har alltid högst prioritet och visas först i listan

---

## Visa original-XML

Knappen **"Visa original-XML"** finns ovanför den renderade vyn och är tillgänglig för alla användare med läsbehörighet.

### Så här gör du

1. Navigera till en XML-fil med XSLT-renderad vy
2. Klicka **Visa original-XML** — vyn ersätts med oformaterad XML-källkod
3. Klicka **Visa renderad vy** för att återgå till XSLT-renderad HTML

> **OBS:** Dropdown för stilmallsval döljs tillfälligt i XML-läge och visas igen när du återgår till renderad vy.

---

## Ladda upp egen XSLT

Användare med behörigheten **"Applicera egen XSLT-stilmall"** kan ladda upp en egen XSLT-fil direkt i gränssnittet för att tillfälligt transformera ett XML-dokument.

### Så här gör du

1. Navigera till en XML-fil i ett AIP
2. Klicka på fliken **VISA**
3. Ovanför den renderade vyn finns ett verktygsfält med en filväljare
4. Klicka **Välj fil** och välj en `.xslt`- eller `.xsl`-fil
5. Klicka **Applicera XSLT**
6. Dokumentet omtransformeras med den uppladdade stilmallen

> **OBS:** Den uppladdade stilmallen sparas inte — den appliceras bara tillfälligt för den aktuella visningen. Ladda om sidan för att återgå till standardstilmallen.

### Behörighet

Verktygsfältet visas bara om användaren har rollen `representation.apply_xslt`. Se avsnittet [Behörigheter](#behörigheter) för hur rollen tilldelas.

---

## Skriv ut dokument

Knappen **"Skriv ut"** finns ovanför den renderade XML-vyn och är tillgänglig för alla användare.

### Så här gör du

1. Navigera till en XML-fil med XSLT-renderad vy
2. Klicka **Skriv ut**
3. Ett nytt fönster öppnas med dokumentinnehållet
4. Webbläsarens utskriftsdialog visas automatiskt
5. Välj skrivare eller "Spara som PDF"

### Tips för renare utskrifter

I utskriftsdialogen (Edge/Chrome):
1. Expandera **"Fler inställningar"**
2. Stäng av **"Sidhuvuden och sidfötter"** — detta tar bort URL och sidnummer
3. Justera marginaler vid behov

---

## Behörigheter

### Roller

| Roll | Beskrivning | Krävs för |
|------|-------------|-----------|
| `representation.read` | Läsa representationer och filer | Visa XSLT-renderad förhandsvisning |
| `representation.apply_xslt` | Applicera egen XSLT-stilmall | Ladda upp och applicera egen XSLT |

### Tilldela rollen i gränssnittet

1. Gå till **Administration → Användare**
2. Redigera användaren
3. Under **Behörigheter → Representationer och filer**, markera **"Applicera egen XSLT-stilmall"**
4. Spara

> **OBS:** Gruppen "administrators" är skyddad och kan inte redigeras via gränssnittet. Tilldela rollen direkt på användarobjektet istället.

### Tekniskt

Rollen definieras i `roda-roles.properties`:

```properties
core.roles.org.roda.wui.api.v2.controller.FilesController.previewFileWithCustomXSLT = representation.apply_xslt
```

Rollen registreras i gränssnittet via `roda-wui.properties`:

```properties
ui.role: representation.apply_xslt
```

Rollen skapas automatiskt i LDAP vid ETERNA:s första start (bootstrap) baserat på `roda-roles.properties`.

---

## REST API

### Lista tillgängliga stilmallar

```http
GET /api/v2/files/{uuid}/preview/html/xslts
```

| Parameter | Typ | Beskrivning |
|-----------|-----|-------------|
| `uuid` | Sökväg | Filens UUID |

**Svar:** JSON-array med objekt `{id, label}` för varje tillgänglig stilmall.

**Behörighet:** `representation.read`

### Förhandsvisning av XML som HTML

```http
GET /api/v2/files/{uuid}/preview/html?lang={locale}&xslt={id}
```

| Parameter | Typ | Beskrivning |
|-----------|-----|-------------|
| `uuid` | Sökväg | Filens UUID |
| `lang` | Query (valfri, standard: `sv`) | Språk/locale |
| `xslt` | Query (valfri) | Stilmallens ID från `/xslts`-endpointen — utelämnas för standardval |

**Svar:**
- `200 OK` — HTML-renderat dokument
- `401 Unauthorized` — Saknar behörighet
- `404 Not Found` — Ingen stilmall hittades

**Behörighet:** `representation.read`

### Transformation med egen XSLT

```http
POST /api/v2/files/{uuid}/preview/html/transform?lang={locale}
Content-Type: multipart/form-data
```

| Parameter | Typ | Beskrivning |
|-----------|-----|-------------|
| `uuid` | Sökväg | Filens UUID |
| `lang` | Query (valfri, standard: `sv`) | Språk/locale |
| `xslt` | Fil (multipart) | XSLT-stilmall att applicera |

**Svar:**
- `200 OK` — HTML-renderat dokument med den uppladdade stilmallen
- `401 Unauthorized` — Saknar behörighet
- `404 Not Found` — Filen hittades inte

**Behörighet:** `representation.apply_xslt`

### Exempel med curl

> **OBS:** Använd dina egna inloggningsuppgifter — använd aldrig standardlösenord
> i produktion. `$RODA_USER` och `$RODA_PASSWORD` används som platshållare nedan.

```bash
# Förhandsvisning med standard-XSLT
curl -u "$RODA_USER:$RODA_PASSWORD" \
  'http://localhost:8080/api/v2/files/{uuid}/preview/html?lang=sv'

# Transformation med egen XSLT
curl -u "$RODA_USER:$RODA_PASSWORD" \
  -X POST \
  -F 'xslt=@min_stilmall.xslt' \
  'http://localhost:8080/api/v2/files/{uuid}/preview/html/transform?lang=sv'
```

---

## Konfiguration

### Namnrymdsregler

I `roda-wui.properties` kan man konfigurera vilken global stilmall som ska användas för en viss XML-namnrymd:

```properties
ui.viewer.xslt.representation.rules = example
ui.viewer.xslt.representation.rules.example.namespace = http://www.example.org/ns/v1
ui.viewer.xslt.representation.rules.example.xslt = example_v1
```

### Stilmallsplacering

Globala stilmallar placeras i:

```
~/.roda/config/crosswalks/dissemination/html/representation/   (representationsfiler)
~/.roda/config/crosswalks/dissemination/html/                  (beskrivande metadata)
```

Filer i `~/.roda/config/` har företräde framför standardkonfigurationen i JAR-filen.

### Saxon-cache

Kompilerade XSLT-stilmallar cachas i 1 minut. Ändringar i stilmallar på disk slår igenom efter att cachen löper ut — ingen omstart behövs.

---

## Tekniska detaljer

### XSLT-motor

- **Bibliotek:** Saxon HE (Home Edition)
- **XSLT-version:** 2.0
- **Cache:** LoadingCache med 1 minuts TTL för kompilerade stilmallar
- **Säkerhet:** Externa entiteter och DTD-bearbetning är avaktiverade

### Namnrymdsdetektering

Systemet läser de första 4 KB av XML-filen och använder `XMLStreamReader` för att extrahera rotelementets namnrymd. Detta görs utan att ladda hela filen i minnet.

### Klient (GWT)

- XML-förhandsvisningen renderas i en `<iframe>` med `srcdoc`-attribut
- HTML hämtas via `RequestBuilder` (GET) och injiceras direkt
- Uppladdning av egen XSLT använder `FormData` och `XMLHttpRequest` (nativ JavaScript via JSNI)
- Utskrift öppnar ett nytt fönster med dokumentinnehållet och anropar `window.print()`

### Behörighetskontroll

Behörighetskontrollen sker på två nivåer:

1. **Serversidan:** `FilesController` kontrollerar rollen via `controllerAssistant.checkRoles()`
2. **Klientsidan:** `BitstreamPreview` kontrollerar rollen asynkront via `UserLogin.getInstance().checkRole()` för att visa/dölja verktygsfältet

### commons-ip2-begränsning

Vid inmatning av SIP-paket via commons-ip2 (v2.10.1) placeras representationsdokumentation alltid på paketnivå i AIP:et, även om den var korrekt placerad på representationsnivå i SIP:et. ETERNA:s sökkod hanterar detta genom att söka på båda nivåerna.

---

## Kända begränsningar

1. **commons-ip2 dokumentationsplacering** — Representationsdokumentation hamnar på paketnivå efter inmatning. Hanteras av fallback-sökning i koden.

2. **Stilmallscache** — Ändringar i globala stilmallar kan ta upp till 1 minut att slå igenom på grund av Saxon-cachen.

3. **Utskrift** — Webbläsarens sidhuvud/sidfot (URL, sidnummer) måste stängas av manuellt i utskriftsdialogen under "Fler inställningar".

4. **Uppladdad XSLT sparas inte** — Användaruppladdade stilmallar appliceras bara tillfälligt. De sparas inte i AIP:et eller på servern.

5. **Skyddade grupper** — Gruppen "administrators" är skyddad och kan inte ändras via gränssnittet. Roller måste tilldelas direkt på användarnivå.

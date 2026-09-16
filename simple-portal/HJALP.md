# Sökportalen – hjälp vid release och drift

Den här filen är för den som ska sätta upp, konfigurera och förvalta sökportalen
(`simple-portal`) för en arkivinstitution. För utvecklingsuppsättning, se [README.md](README.md).

**Innehåll**

1. [Vad portalen är](#1-vad-portalen-är)
2. [Förutsättningar](#2-förutsättningar)
3. [Installation och drift](#3-installation-och-drift)
4. [Konfiguration](#4-konfiguration)
5. [Funktioner för besökare](#5-funktioner-för-besökare)
6. [Administration](#6-administration)
7. [Kända begränsningar](#7-kända-begränsningar)
8. [Felsökning](#8-felsökning)
9. [Kontroll inför release](#9-kontroll-inför-release)

---

## 1. Vad portalen är

Sökportalen är en fristående webbapplikation som låter allmänheten söka i ett
ETERNA-arkiv utan att logga in. Den har ingen egen databas – all data hämtas
live från ETERNA:s V2-API, och all inställning ligger i en enda fil,
`public/assets/config/config.json`.

```
Besökare ──► Sökportalen (Node, port 4321) ──► ETERNA (port 8080)
                 │
                 └─ /api/v2/*  proxas vidare:
                      • anonym besökare  → utan inloggning (ETERNA ser "guest")
                      • inloggad admin   → med sessionscookie
```

Portalen skickar **inga lösenord** för anonyma besökare. Vad en anonym besökare
får se bestäms helt av vilka rättigheter gruppen `guests` har i ETERNA
(se [avsnitt 7](#7-kända-begränsningar)).

Byggd med Astro 6 (server-renderad), React 19 och Arbetsförmedlingens
designsystem Digi.

## 2. Förutsättningar

| Krav | Kommentar |
|---|---|
| **Node.js 22 eller senare** | Kontrollera med `node --version`. |
| **bun** | `npm install -g bun`. Används för installation, bygge och start. |
| **En körande ETERNA** | Portalen är bara ett skal – arkivet, indexet och behörigheterna bor i ETERNA. |
| **API-autentisering påslagen i ETERNA** | `ui.filter.internal.enabled = true` (standard i `roda-wui.properties`). Krävs för att administratörer ska kunna logga in. Se [avsnitt 8](#8-felsökning) för fällan med startordning. |
| **Ett administratörskonto i ETERNA** | Portalen har inga egna användare – man loggar in med sitt ETERNA-konto. |
| **AIP:er med EAD3-metadata** | Träfflistans kolumner Arkivbildare och Datum läses från EAD3 (`<origination>`, `<unitdate>`/`<unitdatestructured>`). |

## 3. Installation och drift

### 3.1 Hämta och installera

```bash
git clone https://github.com/ETERNA-earkiv/ETERNA.git
cd ETERNA
git checkout feat/portal-dev        # eller den release-tagg ni bestämt
cd simple-portal
bun install
```

### 3.2 Peka portalen mot ETERNA

```bash
cp .env.example .env
```

`.env` har en enda inställning:

```ini
RODA_API_URL=http://localhost:8080
```

Ange adressen till er ETERNA. Portalen måste startas om efter ändring.

### 3.3 Startordning – ETERNA först, helt klar

ETERNA måste vara **helt** uppstartad innan portalen (eller någon annan) gör sitt
första anrop mot `/api/*`. Vänta på raden `Started RODA in …` i ETERNA:s logg –
det räcker inte att porten svarar. Bakgrunden står i [avsnitt 8](#8-felsökning).

Kontroll att autentiseringen är aktiv (ska ge **401**):

```bash
curl -s -o /dev/null -w '%{http_code}\n' -u 'finnsinte:x' \
  -X POST http://localhost:8080/api/v2/aips/find \
  -H 'Content-Type: application/json' \
  -d '{"filter":{"parameters":[{"type":"AllFilterParameter"}]},"sublist":{"firstElementIndex":0,"maximumElementCount":1}}'
```

### 3.4 Starta

**Utveckling / test** (kod läses direkt, port 4321):

```bash
bun dev
```

Obs: automatisk omladdning är avstängd. Ladda om hårt (Ctrl+Shift+R) efter
kodändringar och omstarter.

**Produktion** (byggd version):

```bash
bun run build
HOST=0.0.0.0 PORT=4321 RODA_API_URL=http://localhost:8080 node ./dist/server/entry.mjs
```

Kör kommandot **från `simple-portal/`-katalogen**: servern läser och skriver
`public/assets/config/config.json` relativt arbetskatalogen, så
adminändringar hamnar där. Ta med katalogen `public/` i backup.

Lägg portalen bakom en reverse proxy (nginx/Traefik) med HTTPS. Portalen sätter
sessionscookien med `Secure`, så inloggning fungerar inte över ren HTTP
annat än mot `localhost`.

### 3.5 Uppdatera

```bash
git pull
bun install
bun run build
# starta om node-processen
```

`config.json` ligger i repot men är kundspecifik – ta en kopia före uppdatering
(eller använd Exportera under Admin → Konfiguration) och kontrollera att den inte
skrivits över.

## 4. Konfiguration

### 4.1 `config.json` – allt på ett ställe

Filen läses vid varje sidladdning; ingen omstart behövs efter ändring.

| Block | Styr |
|---|---|
| `siteConfig.siteName` | Portalens namn i sidhuvudet och fliken. |
| `siteConfig.primaryColor` | Varumärkesfärgen (knappar, accentlinjer, ikoner). Hex, t.ex. `#C4122A`. |
| `siteConfig.secondaryColor` | Sekundär färg (text/grå toner). |
| `siteConfig.logoUrl` | Logotyp, som URL eller inbäddad base64. Tom = ingen logotyp. |
| `searchConfig.mainSearchField.label` | Rubriken över sökrutan ("Sök i arkivet"). |
| `searchConfig.advancedFields[]` | Fälten under *Avancerad sök*: `fieldName`, `label`, `type` (`text`, `select`, `date-range`), `enabled`. Ordningen i listan är ordningen på sidan. |
| `searchConfig.resultsPerPage` | Träffar per sida. |
| `visibilityConfig.allowedLevels` | Vilka beskrivningsnivåer (EAD-koder) som får visas i träfflistan. Poster på andra nivåer visas inte alls. |
| `visibilityConfig.visibleMetadataFields` | Per metadataschema (`ead_3`, `ead_2002`): vilka fält som visas i den utfällda träffen och tas med i nedladdad metadata. |
| `visibilityConfig.knownMetadataFields` | Listan admin-sidan väljer fält ur. Behöver bara ändras om nya fält tillkommer i metadatan. |
| `visibilityConfig.xpathRules` / `xpathRulesEnabled` | Regler som utifrån ett värde i metadatan visar allt, bara titel eller döljer posten. Avstängt som standard. |
| `aboutConfig` | Sidan *Om arkivet*: `menuLabel`, `title`, `sections[]` (`heading`, `body`), `sourceLinks[]`. Tas blocket bort försvinner menyvalet. |
| `downloadConfig.sourceText` | Källrad i nedladdad metadata-PDF/HTML, t.ex. "Informationen är hämtad från Jämtlands läns e-arkiv". Tom = ingen rad. |

**Sökfält som filtreras i portalen.** Fälten `dates` (Datum) och `origination`
(Arkivbildare) har `"clientFilter": true`. ETERNA:s sökindex saknar dessa värden
(EAD3-omvandlingen indexerar bara `<daterange>`, inte `<datesingle>`), så
portalen hämtar metadatan för träffarna och filtrerar själv. Det fungerar bra
för arkiv upp till några tusen poster; på större arkiv blir avancerad sök på
dessa fält märkbart långsammare. Ta inte bort flaggan – då slutar filtret
fungera.

**Beskrivningsnivåer** anges med EAD-koder. De vanligaste:

| Kod | Visas som |
|---|---|
| `fonds` | Arkivbestånd |
| `series` | Serie |
| `file` | Volym |
| `item` | Handling |
| `recordgrp` | Förvaringsgrupp |
| `collection` | Samling |

**Textstycken i `aboutConfig`.** Två radbrytningar (`\n\n`) i `body` ger nytt
stycke. Enkel radbrytning (`\n`) behålls inom stycket, t.ex. för adressrader.

### 4.2 Miljövariabler

| Variabel | Beskrivning | Standard |
|---|---|---|
| `RODA_API_URL` | Adressen till ETERNA | `http://localhost:8080` |
| `HOST` / `PORT` | Endast produktion: vad node-servern lyssnar på | `0.0.0.0` / `4321` |

Inga lösenord eller servicekonton konfigureras i portalen.

### 4.3 Exempel: sätta upp för en ny kund

1. Kopiera `public/assets/config/config.json` från en befintlig installation
   eller exportera den under *Admin → Konfiguration*.
2. Ändra `siteConfig` (namn, färg, logotyp), `aboutConfig` (texterna på Om-sidan)
   och `downloadConfig.sourceText`.
3. Justera `visibilityConfig.allowedLevels` efter hur kundens arkiv är
   strukturerat – visas inga träffar är det oftast här felet sitter.
4. Ladda om portalen och kontrollera *Om*-sidan, en sökning och en nedladdning.

## 5. Funktioner för besökare

Inget av detta kräver inloggning.

**Sök (`/sok`)**
- Fritextsökning i hela arkivet. Ett **i** vid rubriken fäller ut en kort hjälptext.
- *Avancerad sök* öppnar de fält som är konfigurerade: Titel, Beskrivning,
  Datum (från–till), Omfattning och innehåll, Arkivbildare. Datumfiltret ger
  poster vars start–slutdatum *överlappar* det angivna intervallet; det går att
  ange bara *från* eller bara *till*. Ett årtal räcker (1940 tolkas som hela året).
- Träfflistan visar antal träffar och kolumnerna **Titel / Arkivbildare / Datum**.
  Klicka på en kolumnrubrik för att sortera; klicka igen för omvänd ordning.
- Paginering längst ned.

**Utfälld träff** (klicka på en rad)
- Metadata enligt de synliga fälten under rubriken *Ytterligare information*.
  Startdatum och Slutdatum visas alltid, även när slutdatum saknas.
- Finns beskrivande metadata på representationen visas den under *Metadata för
  representationen*.
- Filer med miniatyrbild. Klicka för att förhandsgranska bild, PDF, text, video
  och ljud direkt i portalen (bläddra med piltangenterna).
- Knappen **Öppna och ladda ner fil** skapar en ZIP med alla filer i
  representationen samt `_metadata.pdf` (metadatan enligt synliga fält, med
  källrad och hämtningsdatum). Om PDF inte kan skapas läggs `_metadata.html` med
  i stället. Mappnamn kortas till 50 tecken så att Windows sökvägsgräns inte
  överskrids.

**Om arkivet (`/om`)** – institutionens egen text från `aboutConfig`.

## 6. Administration

### 6.1 Logga in och ut

- Knappen **Logga in som administratör** uppe till höger leder till `/logga-in`.
  Använd ett administratörskonto från ETERNA.
- Inloggad ser menyn **Admin** och knappen **Logga ut**. Menyn visas bara efter
  att portalen kontrollerat sessionen mot ETERNA – en gammal cookie räcker inte.
- Sessionen är ETERNA:s (`JSESSIONID`). Går den ut skickas man till
  inloggningen med meddelandet "Din session har gått ut".
- Alla sidor under `/admin` och skrivning till `config.json` kräver en giltig
  session som inte är gäst.

### 6.2 Admin-sidorna

| Sida | Inställningar |
|---|---|
| **Admin → Tema** (`/admin/tema`) | Portalnamn, varumärkesfärg, ladda upp / ta bort logotyp. |
| **Admin → Metadata** (`/admin/metadata`) | Tillåtna beskrivningsnivåer; sökfilter (lägg till, ta bort, byt etikett, ordna, aktivera); synliga metadatafält per schema; XPath-regler. |
| **Admin → Konfiguration** (`/admin/konfiguration`) | Exportera hela `config.json` som fil, eller importera en. Bra för backup och för att flytta inställningar mellan miljöer. |

Allt admin-UI:t ändrar skrivs till samma `config.json`. `aboutConfig`,
`downloadConfig` och `resultsPerPage` saknar UI och redigeras direkt i filen.

### 6.3 Behörigheter i ETERNA

Portalen lägger inte till några egna roller. Det som styr är:

- **Anonyma besökare** = ETERNA:s grupp `guests`. Behöver `aip.read`,
  `descriptive_metadata.read` och `representation.read` för att kunna söka,
  läsa metadata och ladda ner.
- **Administratörer** = vilket ETERNA-konto som helst som inte är gäst. Den som
  kan logga in kan ändra portalens inställningar. Begränsa därför vem som har
  konto i ETERNA snarare än att lita på portalen.

## 7. Kända begränsningar

- **Anonym sökning kan ge 403 i en standarduppsättning av ETERNA.** Gruppen
  `guests` saknar då läsrättigheter, och ETERNA tillåter inte att de skyddade
  grupperna `guests`/`guest` redigeras ("Illegal operation"). Frågan är inte
  löst i portalen. Tills vidare fungerar allt när man är inloggad, eftersom
  sökningen då går via sessionen.
- **Datum och Arkivbildare filtreras i portalen**, inte i ETERNA:s index (se
  4.1). Prestandan sjunker med arkivets storlek.
- **Vissa texter är hårdkodade**: hjälptexten bakom **i**-ikonen, rubrikerna
  *Ytterligare information* / *Metadata för representationen*, knapptexter och
  de svenska nivånamnen. Vill en kund ändra dem krävs kodändring.
- **Sökrutans platshållartext** är avstängd; `placeholder` i `config.json` har
  ingen effekt.
- **Ingen egen användarhantering** – portalen litar helt på ETERNA.
- **Dev-servern saknar hot reload** – ladda om hårt efter ändringar.

## 8. Felsökning

| Symptom | Orsak och åtgärd |
|---|---|
| **Inloggning misslyckas trots rätt uppgifter**; curl-testet i 3.3 ger 403 i stället för 401 | ETERNA:s API-filter låstes *av* vid uppstart. Filtret läser `ui.filter.internal.enabled` en gång vid det första anropet mot `/api/*`, och porten öppnas några sekunder innan inställningen är inläst. Träffar portalen, en öppen webbläsarflik eller en Docker-healthcheck API:t i det fönstret blir alla inloggningar gäst tills ETERNA startas om. **Åtgärd:** stoppa portalen, starta om ETERNA, vänta på `Started RODA in`, kör curl-testet (401), starta portalen. **Kör ni ETERNA i Docker med egen monterad `roda-core.properties`:** lägg `ui.filter.internal.enabled = true` i den filen (den läses vid init) och kopiera även in `roda-wui.properties`, `roda-roles.properties` och `roda-permissions.properties` – extern konfiguration ersätter, den slås inte ihop. |
| **"Kunde inte ansluta till arkivet"** | Fel `RODA_API_URL`, eller ETERNA nere. Starta om portalen efter ändring i `.env`. |
| **Sökning ger 403 / inga träffar utan inloggning**, men fungerar inloggad | Gästgruppen saknar läsrättigheter i ETERNA (avsnitt 7). |
| **Inga träffar alls, även inloggad** | Kontrollera `visibilityConfig.allowedLevels` – posterna ligger på en nivå som inte är tillåten. Testa med `["fonds","series","file","item"]`. |
| **Admin-menyn syns fast man loggat ut**, eller inte alls fast man loggat in | Ladda om hårt. Kvarstår det: kontrollera att ETERNA svarar på `GET /api/v2/members/users/authenticated` med cookien. |
| **Datumfiltret ger alla poster** | Löst i version 92ad0f154. Uppstår det igen: notera exakt vad som skrevs i fälten och webbläsare – det är en regression. |
| **"Sökvägen är för lång" vid uppackning av ZIP (Windows)** | Löst: mapp- och filnamn kortas. Kvarstår det: packa upp närmare rotkatalogen (t.ex. `C:\Arkiv\`). |
| **Nedladdning misslyckas med "Failed to fetch dynamically imported module"** (dev) | Vites beroendecache är trasig, typiskt efter att `astro check` körts mot en körande dev-server. `rm -rf node_modules/.vite` och starta om. Typkontrollera med `bunx tsc --noEmit` medan servern kör. |
| **Ändringar i koden syns inte** (dev) | Hot reload är av: Ctrl+Shift+R. Nya filer under `src/pages/` kräver omstart av `bun dev`. |
| **Ändringar i `config.json` syns inte** | Ladda om sidan; filen läses per anrop. I produktion: kontrollera att servern startats från `simple-portal/` så att rätt fil läses. |

## 9. Kontroll inför release

Kör och bocka av innan ni släpper till en kund:

```bash
bun run test          # testsvit (proxy-auth, sökstore, datumintervall, middleware)
bunx tsc --noEmit     # typkontroll
bun run build         # produktionsbygge
```

Manuellt mot kundens ETERNA:

- [ ] `/sok` laddar utan inloggning; en fritextsökning ger träffar.
- [ ] Kolumnrubrikerna sorterar; träffantalet stämmer.
- [ ] Avancerad sök → Datum med årtal (t.ex. 1940–1949) ger *bara* poster i intervallet.
- [ ] Avancerad sök → Arkivbildare filtrerar.
- [ ] Utfälld träff visar rätt fält enligt `visibleMetadataFields`; Slutdatum syns.
- [ ] *Öppna och ladda ner fil* ger ZIP med filer + `_metadata.pdf` med kundens källtext; packar upp utan fel på Windows.
- [ ] *Om*-menyn har kundens text och länkar.
- [ ] Logotyp, portalnamn och varumärkesfärg är kundens.
- [ ] Fel lösenord → tydligt felmeddelande; rätt lösenord direkt därefter → inloggad.
- [ ] Inloggad: Admin-menyn syns; Logga ut → tillbaka till `/sok`, Admin borta (kontrollera även i inkognitofönster).
- [ ] Admin → Konfiguration → Exportera ger en fil; spara den som kundens referenskonfiguration.
- [ ] Gästgruppens rättigheter i ETERNA är genomgångna, eller så är det dokumenterat för kunden att sökning kräver inloggning.

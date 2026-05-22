# XML-visning med XSLT-stilmallar

ETERNA kan rendera XML-filer som en formaterad HTML-sida via stilmallar, direkt i gränssnittet. Verktygsraden låter dig växla tillbaka till rå XML, rendera vy med hjälp av XSLT-filer i paketet, eller, om du har behörigheten *Applicera egen XSLT-stilmall*, tillfälligt använda en stilmall från din lokala dator utan att arkivpaketet ändras.

## Vad är en stilmall?

En **stilmall** (XSLT — *eXtensible Stylesheet Language Transformations*) är en fil som omvandlar en XML-fil till ett annat format, vanligen HTML. ETERNA använder XSLT för att göra strukturerad arkivmetadata (METS, EAD, samt andra standarder) läsbara för människor.

## Öppna en XML-fil

1. Navigera till arkivpaketet (AIP) som innehåller XML-filen.
2. Öppna fillistan och hitta filen. ETERNA känner igen XML via MIME-typ (`text/xml`, `application/xml`, `*+xml`) eller filändelse (`.xml`).
3. Klicka på filen. Visaren öppnas i samma område som övriga förhandsvisningar.

Finns en matchande stilmall i arkivpaketet så laddas den renderade HTML-vyn direkt. Annars visas rå XML — du kan fortfarande granska, skriva ut filen, eller ladda upp en lokal stilmall (om du har behörighet).

## Använd verktygsraden

Knapparna ovanför förhandsvisningen är:

### Växla: renderad ↔ rå XML

Första knappen växlar mellan **renderad HTML-vy** och **rå XML-vy**. Etiketten visar vad nästa klick gör (*Visa original-XML* / *Visa renderad vy*). Använd den när du behöver verifiera den underliggande XML:en eller jämföra med den renderade utdatan.

### Skriv ut

Skriv ut-knappen öppnar den aktuella renderade HTML:en i ett nytt webbläsarfönster och triggar utskriftsdialogen. Utskriften använder snålare marginaler och döljer webbläsarens default-sidhuvuden/sidfötter där webbläsaren tillåter det.

> **Notera:** Utskrift är endast tillgängligt i den renderade vyn. Att skriva ut den råa XML-vyn stöds inte — använd webbläsarens egen utskriftsfunktion om det behövs.

**Tips för renare utskrifter** (Chrome / Edge): expandera *Fler inställningar* i utskriftsdialogen och stäng av **Sidhuvuden och sidfötter** för att ta bort sidans URL och sidnummer. Justera marginalerna från samma panel vid behov.

### Stilmallsväljare

Rullisten visar alla stilmallar som gäller för aktuell XML-fil. Källorna slås ihop i en lista:

| Prefix | Källa | Beskrivning |
|--------|-------|-------------|
| (inget prefix) | **Lokal** | En `.xsl` / `.xslt`-fil som ligger bredvid XML:en i samma representation. ETERNA prioriterar stilmallar med samma basnamn (`Foo.xml` → `Foo.xslt`). |
| (inget prefix) | **AIP-dokumentation** | En stilmall som levererats i AIP:ns `documentation`-mapp. |
| **Global:** | **Global** | En stilmall som administratören installerat och kopplat till XML:ens namespace. |

Saknas matchande stilmall visar rullisten *(Ingen)* och rå XML förvalt.

### Använd lokal stilmall (privilegierad funktion)

Användare med behörigheten **Applicera egen XSLT-stilmall** ser även en *Välj fil*-knapp under rullisten. Använd den för att ladda upp en `.xsl`- eller `.xslt`-fil från din egen dator för en engångs-rendering:

- Filen skickas till servern, appliceras på XML:en och resultatet visas.
- Den uppladdade stilmallen **sparas inte** i arkivpaketet, en notis i verktygsfältet påminner om detta.
- Om du senare väljer samma filnamn igen återanvänds föregående renderade utdata utan ny uppladdning, så länge visarsessionen är aktiv.

**Gränser:**

- Maximal filstorlek: **1 MB**.
- Filen måste vara välformad XML. Externa entitetsreferenser (DTD) tillåts inte.
- Filer som är för stora eller som inte validerar avvisas med felmeddelande; den renderade vyn ändras inte.

## Revisionsspår

Server-side rendering registreras i ETERNAs revisionslogg:

| Händelse | Loggade värden |
|---|---|
| Lista tillgängliga stilmallar | Filens UUID |
| Rendering med global / AIP-bundlad stilmall | Filens UUID, stilmallens ID, språk |
| Rendering med uppladdad stilmall | Filens UUID, uppladdat filnamn, filstorlek, språk |

**Innehållet** i en uppladdad stilmall loggas aldrig — endast filnamn och storlek.

Rent klientsidiga händelser (växla renderad/rå, utskrift, om-val av cachad uppladdning) auditeras **inte** separat — i linje med ETERNAs serverbaserade auditmodell. Kontakta administratören om strängare revisionsspår krävs för dessa.

## Vanliga frågor

### Varför är *Visa renderad vy* gråad?

Ingen matchande stilmall hittades för denna XML-fil, så det finns inget att rendera. Ladda upp en lokal stilmall (om du har behörighet) eller be administratören installera en global stilmall för XML:ens namespace.

### Varför försvinner min uppladdade stilmall när jag öppnar filen senare?

Lokala stilmallsuppladdningar är medvetet sessionsbundna — de finns för att inspektera ad hoc utan att ändra arkivet. För att göra en stilmall permanent: placera den bredvid XML:en i representationen, i AIP:ns documentation-mapp, eller låt administratören installera den som global stilmall.

### Jag får *XSLT-filen är för stor* — vad är gränsen?

1 MB. Större stilmallar avvisas vid uppladdning. De flesta renderings-stilmallar är långt mindre än så; om du slår i taket beror det oftast på inlagt statiskt innehåll som istället borde refereras.

### Min XSLT fungerar i en desktopverktyg men inte i ETERNA — varför?

ETERNAs transformer stänger av externa resurser (DTD, externa entiteter, `document()`-anrop ut ur paketet) av säkerhetsskäl. Stilmallar som hämtar externa filer vid transformeringen stöds inte. Lägg in det du behöver i stilmallen, eller använd en global stilmall som administratören placerar under ETERNAs konfiguration.

### Kan jag skriva ut rå XML?

Skriv ut-knappen är bara aktiv i den renderade vyn. För att skriva ut rå XML, växla till *Visa original-XML* och använd webbläsarens egen utskriftsfunktion (`Ctrl + P` / `⌘ + P`).

## Tekniska begränsningar

- Visaren renderar XML server-side; mycket stora XML-filer kan ta tid eller tajma ut efter 30 sekunder vid lokal uppladdning.
- Den renderade HTML:en körs i en sandlådad iframe utan JavaScript-exekvering, formulär-submit eller toppnivå-navigation. Interaktiva element i stilmallens utdata är medvetet inerta.
- Utskriftsresultatet beror på webbläsarens utskriftsmotor och stilmallens CSS — komplexa layouter kan behöva en utskriftsspecifik stilmall för bästa resultat.

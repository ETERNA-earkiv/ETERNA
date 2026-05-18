# Webbarkivvisaren (WARC och WACZ)

ETERNA kan spela upp arkiverade webbsidor direkt i arkivvyn. Stödet bygger på [ReplayWeb.page](https://replayweb.page/) — ett verktyg från Webrecorder som spelar upp ett webbarkiv helt i din webbläsare. Du behöver inget extra program.

## Vad är ett webbarkiv?

Ett **webbarkiv** är en ögonblicksbild av en eller flera webbsidor. Filen innehåller HTML, bilder, stilmallar, skript och annat innehåll som behövs för att återskapa hur sidan såg ut vid arkiveringstillfället, tillsammans med metadata om när och hur arkiveringen skedde.

ETERNA stödjer två filformat:

| Format | Filändelse | Beskrivning |
|--------|-----------|-------------|
| **WARC** | `.warc` | ISO 28500 — standardformatet för webbarkivering. En sekvens av poster där varje post är en HTTP-förfrågan och dess svar. |
| **WACZ** | `.wacz` | Webrecorders paketformat. En zip-fil som innehåller en eller flera WARC-filer plus index och metadata. Snabbare att navigera i för stora arkiv. |

## Öppna ett webbarkiv

1. Navigera till det arkivpaket (AIP) som innehåller webbarkivfilen.
2. Öppna fillistan och leta upp filen — ETERNA känner igen filen genom dess MIME-typ, filändelse eller PRONOM-kod.
3. Klicka på filen. Visaren öppnas i samma vy som andra förhandsvisningar (PDF, bild, video).

Visaren laddas i en inbäddad ram och tar upp hela förhandsvisningsytan. Första gången du öppnar ett webbarkiv tar det några sekunder innan innehållet visas — webbläsaren bygger då ett index över arkivet.

## Använda visaren

När visaren har laddats ser du ReplayWeb.pages gränssnitt. De viktigaste delarna är:

### Adressfältet

Här visas vilken arkiverad URL som spelas upp just nu. Du kan klicka i fältet och skriva in en annan URL som finns i arkivet — visaren hoppar då till motsvarande arkiverade version. Om du anger en URL som inte finns i arkivet visas ett felmeddelande.

### Tidsväljaren

Bredvid adressfältet finns en tidsstämpel som visar **när** den aktuella sidan arkiverades. Om samma URL finns i flera versioner i arkivet kan du växla mellan dem via tidsväljaren — det här kallas ofta för *time travel* och låter dig se hur sidan ändrats över tid.

### Sidopanelens flikar

I sidopanelen finns följande flikar (enligt ReplayWeb.pages officiella terminologi):

- **Pages** — listar de webbsidor som finns i arkivet, ofta med en miniatyr och tidsstämpel. Du kan filtrera listan via sökrutan i fliken och söka på sidtitel, URL eller extraherad textinnehåll (om en fulltextindex finns med i WACZ-filen).
- **Resources** — listar varje enskild resurs som finns i arkivet (HTML-sidor, bilder, skript, stilmallar, typsnitt osv.). Söks på URL och du kan välja matchningstyp: *exact*, *prefix* eller *substring*.
- **Story** — visas bara om arkivet innehåller en kuraterad samling. Då presenteras utvalda sidor i en bestämd ordning tillsammans med en beskrivande text — vanligt i WACZ-filer från Webrecorder Studio och liknande verktyg.

> ReplayWeb.pages officiella användarguide finns på <https://replayweb.page/docs/user-guide/exploring/> och är referenskälla för visarens funktioner.

### Metadata om arkivfilen

Metadata om själva webbarkivet kan visas på två olika sätt:

**1. ETERNAs fildetaljvy** (primär källa för fil- och bevarandemetadata)

I ETERNAs fillista och fildetaljvy ser du de tekniska och administrativa egenskaperna för WARC/WACZ-filen:

- Filnamn och filstorlek
- MIME-typ (`application/warc` eller `application/wacz`)
- PRONOM-format-ID (t.ex. `fmt/289` för WARC 1.0)
- Checksum / hash (för fixity)
- Datum då filen levererades in och inkluderades i arkivpaketet
- Skapare av arkivpaketet (AIP), behörigheter och beskrivande metadata

Använd den här vyn för all bevarande- och granskningsrelaterad information.

**2. Visarens *Archive Info*-ruta (intern arkivmetadata)**

ReplayWeb.page innehåller en inbyggd informationsruta med titeln **Archive Info** som öppnas via informationsikonen i visarens verktygsfält. Rutan visar metadata om den laddade WARC/WACZ-filen:

| Fält | Innebörd |
|------|----------|
| **Title** | Titel som angetts i arkivet (för WACZ-filer hämtas värdet från `datapackage.json`). |
| **Filename** | Filnamnet som visaren har laddat. |
| **Source** | Den URL som arkivet hämtades från — i ETERNA pekar den på `/api/v2/files/...`-endpoint:en. |
| **Archived Item ID** | Visarens interna identifierare för det laddade objektet. |
| **Date Loaded** | Tidpunkt då arkivet senast lästes in i visaren (lokal tid i din webbläsare). |
| **Total Size** | Total storlek på arkivfilen. |
| **Validation** | Resultat av hash-verifieringen av WACZ-paketets innehållsfiler. Visar antal verifierade respektive ogiltiga hashar. *Ogiltiga* hashar betyder att en eller flera filer i paketet inte matchar de checksums som anges i `datapackage.json`. |
| **Package Hash** | Hash för hela WACZ-paketet (när signerad). *Not Available* när paketet saknar en yttre signatur. |
| **Observer Public Key** | Publik nyckel för den observatör som signerade WACZ-paketet (när signerad). |
| **Loading Mode** | *Download On-Demand* betyder att visaren bara läser de byte-områden den behöver från servern (range requests), inte hela filen på en gång. *Full Download* betyder att hela filen lästes ner innan uppspelningen började. |

Använd Archive Info-rutan när du behöver verifiera filens integritet eller se varifrån arkivet kommer. ETERNAs fildetaljvy är fortfarande den auktoritativa källan för arkivpaketets administrativa metadata (PRONOM, AIP-relationer, behörigheter osv.).

### Inom den uppspelade sidan

Den arkiverade sidan beter sig som en vanlig webbsida — du kan klicka på länkar, scrolla, expandera menyer och så vidare. Länkar som leder till andra arkiverade sidor inom samma arkiv följs och spelas upp lokalt. Länkar som pekar ut från arkivet (till exempel till en URL som inte arkiverats) leder ingen vart — webbläsaren visar antingen ett felmeddelande från ReplayWeb.page eller en tom sida.

> **Observera:** Sidan spelas upp lokalt i din webbläsare. Inga begäranden går ut till det öppna internet — också om sidan innehåller länkar till externa skript eller bilder hämtas dessa enbart från arkivet.

## Söka i arkivet

Många webbarkiv innehåller en intern söklista i Pages-fliken. Du kan filtrera listan på titel eller URL för att snabbt hitta en specifik sida.

ETERNAs vanliga sökruta indexerar **inte** innehållet i webbarkivet — den letar bara efter själva arkivfilen och dess metadata. För att söka i sidornas innehåll måste du öppna arkivet i visaren och använda dess egna verktyg.

## Ladda ner originalfilen

Om du behöver originalfilen (.warc eller .wacz) — till exempel för långtidsbevarande, vidare bearbetning eller granskning utanför ETERNA — använd den vanliga nedladdningsknappen i fillistan eller filens detaljvy. Filen laddas ner exakt som den lagrats, utan modifieringar.

## Visa i fullskärm

Visaren har en inbyggd fullskärmsknapp — ikonen som föreställer en datorskärm/monitor i verktygsfältet. Klicka på den för att låta visaren ta upp hela skärmytan; klicka igen eller tryck `Esc` för att gå tillbaka. Webbläsarens egna fullskärmsläge (oftast `F11`) fungerar också som alternativ.

## Vanliga frågor

### Varför är visaren tom eller laddar inte?

Visaren kräver att din webbläsare stödjer **Service Workers** — alla moderna webbläsare gör det, men funktionen kan vara avstängd i privat läge eller via webbläsarinställningar. Försök:

1. Stänga inkognito-/privatläge och öppna arkivet i ett vanligt fönster.
2. Ladda om sidan (`Ctrl + Shift + R` för hård omladdning).
3. Verifiera att din webbläsare är uppdaterad.

### Varför tar det lång tid att ladda?

Hela arkivfilen läses in i webbläsaren när visaren startar. Stora WARC-filer (flera hundra MB eller mer) kan ta tid att indexera, särskilt över långsamma nätverk. WACZ-formatet är effektivare för stora arkiv eftersom det innehåller ett färdigt index.

### Varför fungerar inte alla länkar i den arkiverade sidan?

Webbarkiv innehåller bara det som arkiverades vid insamlingstillfället. Om en länk pekar på en sida som **inte** arkiverades — till exempel en extern domän eller en sida som inte ingick i arkiveringen — kan visaren inte spela upp den. Det betyder inte att arkivet är trasigt; det betyder att den specifika resursen aldrig fångades in.

### Kan jag dela en länk till en specifik arkiverad sida?

Adressen i adressfältet pekar på ETERNAs visarsida med information om vilken arkivfil och URL som spelas upp. Den länken fungerar för andra användare som har behörighet till samma arkivpaket i ETERNA.

### Vilka tecken stöds i URL:er?

Visaren stödjer URL:er med svenska tecken (å, ä, ö) och andra Unicode-tecken. URL:en kodas korrekt mot ETERNAs API.

## Tekniska begränsningar

- **Filstorlek:** Hela arkivfilen laddas till webbläsarens minne. För arkiv över ~1 GB kan prestandan försämras eller webbläsaren bli ostabil.
- **Strömmande media:** Video och ljud i arkiv kan ha begränsad funktionalitet beroende på hur de fångades in.
- **JavaScript-tunga sidor:** Sidor som hämtar mycket data dynamiskt (till exempel via API-anrop som inte arkiverades) kan se ofullständiga ut.
- **Webbläsarstöd:** Kräver en modern webbläsare med stöd för Service Worker API. ETERNA visar ingen särskild varning på äldre webbläsare — visaren förblir då tom.

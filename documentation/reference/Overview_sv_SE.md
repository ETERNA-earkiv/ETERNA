
# Överblick

ETERNA är ett komplett digitalt arkiv som levererar funktionalitet för alla huvudenheterna i OAIS-modellen. ETERNA kan ta in, hantera och ge tillgång till olika typer av digitala objekt som produceras av privata aktörer eller offentliga verksamheter. ETERNA är baserat på öppen källkod och stöds av befintliga standarder så som OAIS, METS, EAD och PREMIS.

ETERNA implementerar också en rad specifikationer och standarder. För att veta mer om OAIS-informationspaketen som ETERNA implementerar, vänligen läs [Digital Information LifeCycle Interoperability Standards Board](http://www.dilcis.eu/) på GitHub https://github.com/dilcisboard.

## Funktioner

* Användarvänligt grafiskt användargränssnitt baserat på HTML 5 och CSS 3
* Lagring och hantering av digitala objekt
* Katalog baserad på omfattande metadata (stödjer alla XML-baserade format som beskrivande metadata)
* Full support för Dublin Core och Encoded Archival Description.
* Konfigurerbart arbetsflöde för inleverans i flera steg
* PREMIS 3 för metadata
* Autentisering och rättigheter via LDAP och CAS
* Rapportering och statistik
* REST API
* Stödjer utbyggbara funktioner för arkivvårdsjobb.
* Integrerad riskhantering
* Integrerat formatregister
* Använder inbyggt filsystem för datalagring
* 100% kompatibelt med E-ARK SIP, AIP, och DIP specifikationer
* Stöd för teman

För mer information, besök gärna ETERNA:s webbplats:
**<https://www.whitered.se/eterna>**


## Funktioner

ETERNA har UI-stöd för följande funktionella enheter. Funktionerna är organiserade i menyn under följande huvudkategorier: **Katalog**, **Sök**, **Leverans** (Inleverans, Process, Ankomstkontroll), **Administration** (Arkivvårdsjobb, Interna åtgärder, Granskningslogg, Aviseringslogg, Rapportering, Användare och grupper), **Gallring** (Gallringspolicyer, Gallringsbekräftelse, Förfallna gallringar, Gallrade objekt) och **Planering** (Representationsnätverk, Riskregister, Bevarandehändelser, Bevarandeaktör).

### Katalog

Katalogen är en inventering av alla handlingar och information i arkivet. En handling kan representera olika typer av information i arkivet (t ex böcker, elektroniska dokument, bilder, databaser export mm). Handlingar är vanligtvis samlat i en samling (eller arkivbestånd) och vidare indelat i undersamlingar, sektioner, serier, filer osv. Den här sidan listar alla samlingar på högsta nivå i arkivet. Du kan komma ner till undersamlingar genom att klicka på samlingens namn.

### Sök & Avancerad sökning

På söksidan kan du söka efter logiska enheter, representationer eller filer (använd nedåtpilen för att välja sökdomän). För var och en av dessa domäner kan du söka i alla dess egenskaper eller i specifika egenskaper (använd nedåtpilen för att utöka den avancerade sökningen). Om du till exempel väljer logiska enheter kan du söka i ett specifikt fält av beskrivande metadata, eller hitta filer av ett visst format om filer avancerad sökning är vald.

Sökmotorn hittar endast hela ord. Om du vill söka efter delar av ord så använd '*'-tecken. För mer information om sökverktyg, se nästa sektion.

### Leveransförberedelse

Processen för leveransförberedelse åskådliggör möjligheten  att skapa Submission Information Packages (SIP) som innehåller data och metadata (i en definierad struktur) för att kunna överlämna dem till systemet för inleverans. SIP:arna som skapas förväntas följa policys som är fastställda med (eller genom) systemet. 

### Inleverans

Inleveransytan är en tillfällig lagringsyta för att ta emot inlämningsinformationspaket (SIP) från producenter. SIP:ar kan levereras via t.ex. elektronisk överföring (t.ex. FTP). Den här sidan gör det också möjligt för användaren att skapa/ta bort mappar och ladda upp flera SIP:er samtidigt till systemet för vidare bearbetning och inleverans. Inleveransprocessen kan initieras genom att välja de SIP:er som du vill inkludera i bearbetningsbatchen. Klicka på knappen "Starta ny process" för att initiera inleveransprocessen.

### Process

Inleveransprocessen innehåller funktioner för att acceptera inlämningspaket (SIP) från producenter, förbereda arkivpaket (AIP) för lagring och säkerställa att arkivpaket och deras stödjande beskrivande information etableras i e-arkivet. Den här sidan listar alla inleveranser som för närvarande körs och alla leveranser som har körts tidigare. I den högra sidopanelen är det möjligt att filtrera jobb baserat på deras tillstånd och vilken användare som initierade jobbet. Genom att klicka på ett objekt i tabellen är det möjligt att se hur arbetet fortskrider samt ytterligare detaljer.

### Ankomstkontroll

Ankomstkontroll är en process för att avgöra om informationen och annat material har bevarandevärde. Bedömning kan göras på samling-, skapar-, serie-, fil- eller objektsnivå. Ankomstkontrollen kan ske före eller efter överföringen. Grunden för beslut kan omfatta ett antal faktorer inklusive informationens härkomst, innehåll, autenticitet, tillförlitlighet, ordning, fullständighet, skick, kostnader för bevarandet samt informationens egenvärde.

### Arkivvårdsjobb

Arkivvårdsjobb är åtgärder som utförs på arkiverat material för att förbättra tillgänglighet och minska risker vid digitalt bevarande. Inom ETERNA hanteras dessa via en exekveringsmodul, där behöriga användare kan köra jobb på AIP:er, representationer eller filer. Exempel på arkivvårdsjobb är formatkonverteringar, kontrollsummeverifiering, rapportering och viruskontroller.

Ett arkivvårdsjobb kan startas på två sätt: välj en eller flera rader i tabellen och klicka på "Starta ny process" i åtgärdsmenyn till höger för att köra jobbet på de markerade objekten, eller klicka på knappen "Bevarande" -> "Starta ny process" längst upp för att köra jobbet på hela hela AIP:en, representationen eller filen.

### Interna åtgärder

Interna åtgärder visar en lista av loggar av komplexa uppgifter som utförs av systemet som bakgrundsjobb, vilka förbättrar användarupplevelsen genom att inte blockera användargränssnittet under arbeten som tar längre tid. Exempel på sådana arbeten är att flytta AIP:er, återindexera delar av e-arkivet eller att radera ett stort antal filer.


### Granskningslogg

Händelseloggar är speciella filer som registrerar viktiga händelser som sker i systemet. Till exempel registrerar systemet varje gång en användare loggar in, när en nedladdning utförs eller när en ändring görs i en beskrivande metadatafil. Närhelst dessa händelser inträffar registrerar systemet den nödvändiga informationen i händelseloggen för att möjliggöra framtida granskning av systemaktiviteten. För varje händelse registreras följande information: Datum, involverad komponent, systemmetod eller funktion, användare som utförde åtgärden, åtgärdens varaktighet och IP-adressen till användaren som utförde åtgärden. Användare kan filtrera händelser efter typ, datum och andra attribut genom att välja de tillgängliga alternativen i den högra sidopanelen.

### Aviseringslogg

Notifieringar i ETERNA är ett sätt att informera användaren om specifika händelser i systemet. Informationen skickas i ett mail, som innehåller en beskrivning av händelsen och en länk där användaren kan bekräfta. 

### Rapportering

Den här sidan visar en instrumentpanel med statistik som rör flera olika delar av systemet. Statistiken är organiserad efter sektioner, där var och en av dessa fokuserar på en viss aspekt av systemet så som t.ex. frågor som har med metadata, information, statistik som rör inleverans samt arkivvårdsjobb, användarstatistik och frågor rörande autentisering, bevarandeaktiviteter, riskhantering och notiser.

### Användare och grupper

Här är det möjligt för användare med rätt behörighet att skapa eller ändra inloggningsuppgifter för alla användare i systemet. Här kan även systemadministratören definiera grupper och behörigheter för var och en av de registrerade användarna. Systemadministratören kan också filtrera de användare och grupper som visas genom att klicka på de tillgängliga alternativen i den högra sidopanelen.  

- **För att skapa en ny användare**:  
    Klicka på knappen "Lägg till användare".  

- **För att skapa en ny användargrupp**:   
    Klicka på knappen "Lägg till grupp".  

- **För att redigera en befintlig användare eller grupp**:  
Klicka på ett objekt i listan.  

> Kom ihåg att det är viktigt att ha en strikt behörighetshantering för att minimera risken för otillåten röjning av sekretess!

#### Lösenord vid skapande av användare

När en ny användare skapas skickar systemet normalt ett aktiveringsmail med en länk för att användaren ska kunna sätta sitt lösenord. Om e-post inte är konfigurerat i miljön fungerar inte detta flöde.

**Workaround för miljöer utan e-post:**

1. Skapa användaren som vanligt via "Lägg till användare".
2. Öppna den nyss skapade användaren och klicka på "Ändra".
3. Sätt ett lösenord manuellt i fältet för lösenord.
4. Kommunicera lösenordet till användaren via en lämplig intern kanal.

> Lösenord kan alltid sättas eller ändras i efterhand via "Ändra" på en befintlig användare.

### Gallring

Gallring i ETERNA innebär att information tas bort enligt fastställda gallringsregler och gallringsscheman, till exempel när en lagringstid har löpt ut. Varje logisk enhet är kopplad till ett gallringsschema som styr hur länge den bevaras och när gallring får ske.

Gallringen genomförs via en kontrollerad process och kan tillfälligt stoppas genom ett gallringsstopp. Inför gallring skapas en gallringsbekräftelse som innehåller en rapport över berörda enheter. Gallringen måste startas explicit och kan efteråt antingen återställas eller genomföras permanent.

Översikter i ETERNA visar enheter som är redo för gallring, redo för granskning, redan gallrade eller där beräkning av gallringsfrist har misslyckats. Metadata om gallrade objekt bevaras för att visa att gallringen har skett korrekt enligt gällande gallringsschema.

Läs mer om gallring under administrationsguiden "Gallringspolicyer."

### Representationsnätverk

Representationsinformation är all information som krävs för att kunna förstå och tolka både det digitala materialet samt tillhörande metadata. Digitala objekt lagras som bitströmmar som inte kan förstås av en människa om inte finns ytterligare data för att tolka dem. Representationsinformation är den tillkommande information om struktur eller semantik som omvandlar rådata till något mer meningsfullt.

### Riskregister

Riskregistret listar alla identifierade risker som kan påverka systemet. Det bör vara så omfattande som möjligt och inkludera alla identifierade risker samt en uppskattad sannolikhet att respektive risk inträffar, hur risken kan påverka om den inträffar, när den kan inträffa och hur ofta. Riskhantering används för att minimera sannolikheten att risken inträffar. 

### Bevarandehändelser

En bevarandehändelse samlar metadata om händelser, specificerar och dokumenterar vilka objekt som har påverkats och vilken person eller mjukvara som varit inblandad. Dokumentation av händelser som modifierar ett objekt är avgörande för att upprätthålla digital härkomst, en nyckelfaktor för äkthet. Händelser som skapar nya relationer eller ändrar befintliga relationer är viktiga för att förklara dessa relationer. Även händelser som inte förändrar någonting, såsom giltighet och integritetskontroller av objekt, kan vara viktiga att registrera för förvaltningsändamål.

### Bevarandeaktör

Enligt PREMIS (Preservation Metadata Implementation Strategies) är bevarandeaktörer (Preservation Agents) personer eller system som ansvarar för att utföra uppgifter eller roller relaterade till bevarandet av digitala objekt. Bevarandeaktörer kan omfatta både mänskliga aktörer (t.ex. arkivarier, kuratorer eller bevarandeadministratörer) och mjukvaruverktyg eller system (t.ex. innehållshanteringssystem, digitala bevarandesystem eller migrationsverktyg).

I bevarandemetadata kopplas bevarandeaktörer ofta till specifika händelser eller åtgärder som sker under ett digitalt objekts livscykel. Genom att dokumentera dessa aktörer och deras roller kan organisationer upprätthålla en tydlig dokumentation över vem eller vad som har utfört olika bevarandeåtgärder och därigenom bidra till att säkerställa digitala resursernas långsiktiga tillgänglighet och integritet.

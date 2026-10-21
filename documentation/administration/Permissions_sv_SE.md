# Behörigheter i ETERNA

Det här dokumentet beskriver hur behörigheter fungerar och administreras i ETERNA: rättigheter och grupper, objektbehörigheter på strukturenheter, åtkomstregler som spärrar läsning utifrån sekretess- och personuppgiftsbedömning, behörighetsöversikten samt den konfiguration som styr allt detta.

## Begrepp

| Begrepp | Betydelse i ETERNA |
|---|---|
| **Roll = grupp** | Det som i kravställningar kallas *roll* är i ETERNA en **grupp** (t.ex. `archivists`, `socialtjanst`). En grupp bär en uppsättning rättigheter, och en användare som är medlem i gruppen får dessa rättigheter. Antalet grupper är obegränsat. |
| **Rättighet** | En funktionsbehörighet med tekniskt namn, t.ex. `aip.read`, `job.manage`, `access_rules.manage`. Rättigheter tilldelas grupper (normalfallet) eller direkt till en användare. Varje REST-metod är knuten till en rättighet i `roda-roles.properties`; saknar användaren rättigheten svarar servern 403. |
| **Rättighetsområde** | Rättigheterna grupperas i områden i gränssnittet (t.ex. *Strukturenheter (AIP)*, *Användare och grupper*, *Åtkomstregler (sekretess/personuppgifter)*). Området är prefixet före första punkten i rättighetens namn och rubriken kommer från `ui.roleTitle` i `roda-wui.properties`. |
| **Objektbehörighet** | Behörighetslistan på en enskild strukturenhet (AIP). Fem typer per användare och grupp: **Läsa** (READ), **Skapa** (CREATE), **Uppdatera** (UPDATE), **Ta bort** (DELETE) och **Tilldela** (GRANT). Den som saknar READ ser inte strukturenheten alls, varken i arkivbeståndet, i sökresultat eller i rapporter. |
| **Åtkomstregel** | En regel per grupp som anger vilka sekretess- och personuppgiftsbedömningar (och lagrum) gruppen får läsa. Reglerna utvärderas mot strukturenhetens administrativa metadata (`eterna_admin`) och **spärrar** läsbehörighet som gruppen annars skulle ha. Reglerna ger aldrig behörighet. |
| **Hanterad grupp** | En grupp som har en åtkomstregel (aktiv eller avstängd). Bara hanterade grupper rörs av regelmotorn. |
| **Spärrad grupp** | En hanterad grupp vars läsbehörighet på en viss strukturenhet tagits bort av åtkomstreglerna. Spärren lagras på strukturenheten (`accessRuleState.restrictedGroups`) så att behörigheten kan återställas när bedömningen eller regeln ändras. Visas i fliken **Behörigheter** under rubriken *Spärrade av åtkomstregel* och i behörighetsöversikten. |

## Rättigheter och grupper

Grupper och användare administreras under **Administration → Användare och grupper**.

- **Obegränsat antal grupper.** Skapa en grupp per roll i verksamheten (t.ex. `arkivarie`, `handlaggare_socialtjanst`, `registrator`). Gruppen får ett tekniskt namn (utan blanksteg) och ett visningsnamn.
- **Rättigheter tilldelas gruppen.** På gruppsidan väljs rättigheter per rättighetsområde med kryssrutor. Ändras rättigheterna på gruppen slår ändringen igenom för alla medlemmar.
- **Flera grupper per användare.** En användare kan vara medlem i flera grupper och får summan av gruppernas rättigheter. Rättigheter kan dessutom tilldelas direkt på användaren (*direkta rättigheter*); de *effektiva rättigheterna* är summan av direkta rättigheter och gruppernas.
- **Ny inloggning efter ändrat gruppmedlemskap.** Användarobjektet (inklusive grupper och rättigheter) läses in vid inloggning och hålls i sessionen. När en användare läggs till i eller tas bort ur en grupp, eller gruppens rättigheter ändras, får den berörda användaren fullt genomslag först vid nästa inloggning.
- **Skyddade grupper och användare.** `administrators`, `users` och `guests` samt användaren `guest` är skyddade (`core.ldap.protectedGroups` / `core.ldap.protectedUsers` i `roda-core.properties`) och kan inte tas bort eller byta namn i gränssnittet.

## Objektbehörigheter per strukturenhet

Varje strukturenhet har fliken **Behörigheter**.

- Fliken listar behörigheter per användare och per grupp med kryssrutor för Läsa, Skapa, Uppdatera, Ta bort och Tilldela. Användare och grupper läggs till med knapparna för att lägga till användare respektive grupp.
- **Rekursiv tillämpning.** Vid sparande kan ändringen tillämpas på hela delträdet under strukturenheten (alternativet för rekursiv uppdatering). Ändringen körs som ett jobb (`PATCH /api/v2/aips/permissions/update`) och syns i jobblistan.
- **Arv vid skapande.** En ny strukturenhet får vid inleverans eller skapande sina behörigheter från föräldern samt de standardbehörigheter som konfigurerats med `core.aip.default_permissions.*` (se avsnittet *Standardbehörigheter vid skapande av strukturenheter* nedan).
- **Sektionen "Spärrade av åtkomstregel".** Längst ned i fliken (bara när en enskild strukturenhet visas) listas de grupper vars läsbehörighet tagits bort av åtkomstreglerna, eller texten *Inga*. En grupp som står här har haft READ (direkt eller genom arv) men nekas av sin regel. Ger administratören gruppen READ manuellt tas behörigheten omedelbart bort igen av regelmotorn (se nedan) och gruppen hamnar åter under *Spärrade av åtkomstregel*.
- Användaren `admin` förbigår objektbehörigheterna. Gruppen `administrators` har normalt fullständiga behörigheter via `core.aip.default_permissions` men är en vanlig grupp i objektbehörigheterna.

## Åtkomstregler för sekretess och personuppgifter

Åtkomstreglerna kopplar behörighet till metadata om sekretess (lagrum, kapitel och paragraf) och om personuppgifter och personuppgiftsnivå. De administreras under **Administration → Åtkomstregler**.

### Vad som styr

Reglerna utvärderas mot strukturenhetens administrativa metadata av typen `eterna_admin` (konfigurerbart med `core.access_rules.metadata_type`). Fyra uppgifter används:

| Fält i `eterna_admin` | Vokabulär | Används till |
|---|---|---|
| `sekretess` – sekretessbedömning | `ej_bedomd`, `ingen_sekretess`, `sekretess_forekommer`, `helt_sekretessbelagd` | Regeln listar vilka bedömningar gruppen får läsa. |
| `sekretessgrund` – fritext med sekretessbestämmelsen, t.ex. "OSL 26 kap. 1 § och 21 kap. 7 §" | fritext | Tolkas till lagrum (lag, kapitel, paragraf) som jämförs med regelns lagrumsmönster. Kontrollen görs bara när sekretessbedömningen är något annat än `ingen_sekretess` eller `ej_bedomd`. |
| `personuppgifter` – personuppgiftsbedömning | `ej_bedomd`, `inga`, `forekommer`, `kansliga_forekommer` | Regeln listar vilka bedömningar gruppen får läsa. |
| `personuppgiftsniva` – personuppgiftsnivå enligt organisationens riktlinje (valfritt fält) | `core.access_rules.personuppgiftsniva.values[]`, standard `niva_1`, `niva_2`, `niva_3` | Kontrolleras bara om strukturenheten har en nivå. |

Saknar strukturenheten eget `eterna_admin`-dokument ärver den bedömningen från närmaste förfader som har ett (policyvalet `missingMetadata = inherit`, standard). Finns ingen förfader med bedömning är strukturenheten **obedömd struktur** (t.ex. arkivbildare, klasser, serier och diarieposter som ingen bedömt) och lämnas **öppen** – reglerna styr bara strukturenheter med egen eller ärvd bedömning, och för obedömd struktur gäller enbart objektbehörigheten. Ska sådana strukturenheter i stället behandlas som *ej bedömda* (och därmed stängas) används `missingMetadata = inherit-closed`. En strukturenhet vars egen eller ärvda bedömning uttryckligen är `ej_bedomd` är däremot alltid stängd med `ejBedomd = closed`.

### Regelsyntax för lagrum

Lagrum skrivs i regeln som ett mönster per rad. Godkända former:

| Mönster | Betydelse |
|---|---|
| `*` | Alla lagrum (och även objekt helt utan tolkbart lagrum). |
| `OSL 21` eller `OSL 21 kap.` | Hela kapitlet: täcker varje paragraf i 21 kap. OSL. |
| `OSL 21:7` eller `OSL 21 kap. 7 §` | Exakt paragraf. `OSL 21:7a` (7 a §) är en annan paragraf än `OSL 21:7`. |
| `21:7` | Lagbeteckning utelämnad: tolkas som `defaultAct` i regelfilen (standard `OSL`). |

Ett mönster för en paragraf täcker inte ett lagrum som avser hela kapitlet (det är bredare än mönstret). Ett mönster för hela kapitlet täcker däremot alla paragrafer i kapitlet.

Fritexten i `sekretessgrund` tolkas tolerant: `OSL 21 kap. 7 §`, `21 kap 7 §`, `OSL 21:7`, `OSL (2009:400) 21 kap. 7 §`, `OSL 21 kap. 7 § 1 st.` samt uppräkningar med `,`, `;`, `/`, `och` och `samt` ("OSL 21 kap. 1, 3 och 7 §§" ger tre lagrum; "TF 2 kap. 1 § och 3 kap. 2 §" ärver lagen TF). Delar som inte kan tolkas blir **varningar** och räknas som otäckta lagrum, dvs. otolkbar text stänger hellre än öppnar. Knappen **Normalisera** på regelsidan visar hur en text tolkas.

### Policyval (defaults i regelfilen)

| Policy | Värden | Standard | Betydelse |
|---|---|---|---|
| `ejBedomd` | `closed`, `open` | `closed` | `closed`: en strukturenhet med sekretess eller personuppgifter *ej bedömd* (eller utan värde) får läsas bara av grupper vars regel uttryckligen listar `ej_bedomd` (eller `*`). `open`: ej bedömda objekt tillåts alltid. |
| `missingMetadata` | `inherit`, `inherit-closed`, `open`, `closed` | `inherit` | Strukturenhet utan eget `eterna_admin`: `inherit` = ärv närmaste förfaders bedömning; finns ingen bedömd förfader lämnas strukturenheten öppen (obedömd struktur styrs inte av reglerna). `inherit-closed` = som `inherit`, men utan egen eller ärvd bedömning behandlas strukturenheten som ej bedömd (stängd om inte regeln listar `ej_bedomd` eller `ejBedomd = open`). `open` = ingen arvslogik, alla hanterade grupper tillåts. `closed` = ingen arvslogik, ingen hanterad grupp tillåts. |
| `missingLagrum` | `closed`, `open` | `closed` | Sekretessbelagd strukturenhet utan tolkbart lagrum: `closed` = tillåts bara av regler med lagrum `*`; `open` = lagrumskravet hoppas över. |
| `multipleLagrum` | `all`, `any` | `all` | `all`: varje lagrum på strukturenheten måste täckas av regeln; `any`: det räcker att ett täcks. |
| `defaultAct` | lagbeteckning | `OSL` | Lag som antas när lagrumstexten saknar lagbeteckning. Har företräde framför `core.access_rules.default_lag`. |

Standardinställningen är den restriktiva tolkningen för allt som faktiskt bedömts (ej bedömt stängt, saknat lagrum stängt, alla lagrum måste täckas); obedömd struktur utan bedömd förfader lämnas däremot öppen med `inherit` – välj `inherit-closed` om även den ska stängas. Okända policyvärden och okända vokabulärvärden stänger alltid.

### Beslutsregeln

För en hanterad grupp G och en strukturenhet med fakta F får G läsa om **alla** villkor uppfylls:

1. F.sekretess finns i G:s lista över tillåtna sekretessbedömningar (`*` = alla).
2. Om F.sekretess är `sekretess_forekommer`, `helt_sekretessbelagd` eller något okänt värde: strukturenhetens lagrum täcks av G:s lagrumsmönster enligt `missingLagrum`/`multipleLagrum`.
3. F.personuppgifter finns i G:s lista över tillåtna personuppgiftsbedömningar.
4. Om F har personuppgiftsnivå: nivån finns i G:s lista över tillåtna nivåer.

En tom lista tillåter inget. En **avstängd regel** (Aktiv = Nej) gör gruppen hanterad men tillåter aldrig något, dvs. gruppen spärras från alla strukturenheter; ta bort regeln om gruppen inte längre ska hanteras. Finns flera regler för samma grupp i filen är hela filen ogiltig (valideringsfel "gruppen har mer än en regel"); senast giltiga version behålls. Gränssnittet tillåter bara en regel per grupp.

### Reglerna spärrar, ger aldrig

- Reglerna rör **bara hanterade grupper**. Icke-hanterade grupper och alla enskilda användare lämnas orörda. CREATE rörs aldrig.
- En hanterad grupp får READ bara om den redan hade READ på strukturenheten (via objektbehörigheten) **eller** om reglerna tidigare tagit READ ifrån den (gruppen står i *Spärrade av åtkomstregel*). Reglerna lägger aldrig till en grupp som inte haft läsbehörighet genom strukturen.
- **Nekad grupp förlorar även skrivrättigheter.** Med `core.access_rules.strip_write_on_deny = true` (standard) tas även UPDATE, DELETE och GRANT bort för en nekad grupp, annars kunde gruppen själv ändra bedömningen eller ge sig READ tillbaka. De borttagna typerna sparas på strukturenheten (`accessRuleState.removedPermissions`) och återställs automatiskt när gruppen åter tillåts.
- Tas regeln för en grupp bort blir gruppen ohanterad; behörigheter som en tidigare regel tagit bort återställs **inte** förrän gruppen åter har en regel som tillåter den (varning i jobbrapport och logg).
- Stängs hela regelfilen av (`enabled = false` i `access-rules.json`) tillåts alla hanterade grupper och READ/skrivrättigheter som reglerna tagit bort återställs nästa gång strukturenheterna utvärderas (jobb eller observer). För att bara stoppa all utvärdering utan att återställa något används i stället `core.access_rules.enabled = false` i `roda-core.properties`.

### När reglerna tillämpas

| Tillfälle | Mekanism |
|---|---|
| **Vid inleverans** | Obligatoriskt inleveranssteg *Tillämpa åtkomstregler* (`parameter.do_apply_access_rules`, låst till på) som körs efter producentbehörighetskontrollen och före gallringsreglerna, så att paketet aldrig blir aktivt med läsbehörighet för nekade grupper. |
| **Vid ändring av bedömning** | Observern utvärderar strukturenheten direkt när `eterna_admin` skapas, uppdateras eller tas bort – via metadataformuläret, massredigering (*Massredigering av metadatafält*) eller återställning av en tidigare metadataversion. Har strukturenheten efterkommande utan egen bedömning startas automatiskt ett jobb *Åtkomstregler: efterkommande till …* för dem (ett jobb per massredigering). |
| **Vid manuell behörighetsändring** | Observern utvärderar strukturenheten på nytt efter varje behörighetsändring. Ger någon en spärrad grupp READ tas den omedelbart bort igen. |
| **Vid flytt** | En flyttad strukturenhet utvärderas om (ärvd bedömning kan ha ändrats); saknar den egen bedömning utvärderas även dess efterkommande. |
| **Jobbet "Tillämpa åtkomstregler"** | Startas från regelsidan (*Tillämpa reglerna nu* / *Torrkörning*), via REST (`POST /api/v2/access-rules/apply`) eller som arkivvårdsjobb under **Administration → Arkivvårdsjobb** på valfritt urval. Jobbet utvärderar alla aktiva strukturenheter som jobbanvändaren får läsa och bifogar en CSV-rapport. |
| **Torrkörning** | Samma jobb med parametern *Torrkörning*: beräknar och rapporterar vad som skulle ändras utan att skriva något och utan bevarandehändelser. |

Ändringar i reglerna gäller omedelbart för nya bedömningar (inleverans, metadataändring). Befintliga strukturenheter uppdateras först när jobbet körs – kryssa i *Tillämpa reglerna på arkivet direkt efter sparning* eller starta jobbet från regelsidan.

Alla skrivningar som ändrar behörigheter ger en bevarandehändelse (PREMIS, typ UPDATE, beskrivning *Tillämpning av åtkomstregler (sekretess/personuppgifter) på behörigheter*) med fakta, motivering per grupp och ändrade behörigheter, och den utlösande användaren som agent. Händelserna kan stängas av med `core.access_rules.observer.create_event`, `core.access_rules.ingest.create_event` respektive jobbparametern *Skapa bevarandehändelser*.

### Administratörsbehandling

- Användaren `admin` förbigår all objektbehörighet och påverkas inte av reglerna.
- Gruppen `administrators` tas normalt inte med i regelfilen och behåller då sina behörigheter oförändrade. Ges den en regel behandlas den som vilken hanterad grupp som helst.
- Rollen `access_rules.manage` är arkivövergripande: tillämpningsjobbet körs i anroparens namn och spärrar de hanterade gruppernas läsbehörighet på alla strukturenheter anroparen själv får läsa, oavsett vem som annars får administrera enhetens behörigheter. Objektbehörigheten GRANT krävs inte, eftersom reglerna bara kan ta bort behörighet. Strukturenheter som jobbanvändaren inte får läsa rapporteras som överhoppade utan uppgifter om sekretess.

### Portalens gästgrupp

Anonyma portalanrop sker som användaren `guest` i gruppen `guests`. Gruppen behandlas som vilken grupp som helst: ge den läsbehörighet på det som ska vara öppet (objektbehörighet eller `core.aip.default_permissions.group[] = guests`) och lägg till en åtkomstregel för `guests` som bara tillåter `ingen_sekretess` och `inga`/`ej_bedomd` enligt organisationens policy. Då spärras sekretessbelagda och personuppgiftsbärande strukturenheter automatiskt från portalen, även när de råkar ha fått READ för `guests` genom arv. Obedömd struktur (utan egen eller ärvd bedömning) spärras däremot inte av regeln med `missingMetadata = inherit`; ska portalen bara visa bedömt material används `inherit-closed`.

## Administration → Åtkomstregler

Sidan kräver rättigheten `access_rules.read` (visa) respektive `access_rules.manage` (ändra och tillämpa). Hjälptexten fälls ut med informationsikonen vid rubriken.

**Statusraden** visar *Status* (Aktiva/Avstängda – kräver både `core.access_rules.enabled` och regelfilens `enabled`), *Version* (räknas upp vid varje sparning), *Senast tillämpad* (tidpunkt och länk *Visa jobb* till senaste tillämpningsjobb som inte var torrkörning, annars *Aldrig*) och *Senast ändrad* (av vem och när).

**Regellistan** har kolumnerna *Grupp* (länk till gruppsidan), *Aktiv*, *Tillåtna sekretessbedömningar*, *Tillåtna lagrum*, *Tillåtna personuppgiftsbedömningar*, *Tillåtna personuppgiftsnivåer* samt *Ändra* och *Ta bort*. Utan regler visas texten "Inga åtkomstregler är definierade ännu …".

Steg för steg:

1. **Lägg till regel för grupp** (sidopanelen *Åtgärder*). Välj grupp i dialogen *Välj den grupp som regeln ska gälla för*. Formuläret *Redigera åtkomstregel* öppnas med *Alla (\*)* förvalt i varje grupp av kryssrutor, så att en orörd regel inte spärrar gruppen från hela arkivet.
2. **Aktiv** – kryssrutan styr om regeln utvärderas. Hjälptexten påminner om att en avstängd regel spärrar gruppen helt.
3. **Tillåtna sekretessbedömningar** – kryssa i *Alla (\*)*, *Ej bedömd*, *Ingen sekretess*, *Sekretess förekommer* och/eller *Helt sekretessbelagd*. Markeras *Alla (\*)* låses övriga rutor. Inga markerade = inget tillåtet.
4. **Tillåtna lagrum** – ett lagrum per rad. *Lägg till lagrum* skapar en ny rad, *Ta bort* tar bort raden. *Normalisera* skickar raderna till servern och visar *Normaliserad form* (t.ex. `OSL 21 kap. 7 §` → `OSL 21:7`) och eventuella *Varningar*; knappen *Använd normaliserad form* ersätter raderna med de normaliserade mönstren. En rad som ger flera lagrum avvisas vid sparning – dela upp den.
5. **Tillåtna personuppgiftsbedömningar** – *Alla (\*)*, *Ej bedömd*, *Inga personuppgifter*, *Personuppgifter förekommer*, *Känsliga personuppgifter förekommer*.
6. **Tillåtna personuppgiftsnivåer** – *Alla (\*)*, *Nivå 1 – harmlösa*, *Nivå 2 – skyddsvärda*, *Nivå 3 – känsliga* (vokabulären från konfigurationen, etiketterna från `eterna_admin.personuppgiftsniva.<värde>` i `ServerMessages_sv_SE.properties`). Saknas vokabulär visas "Ingen vokabulär för personuppgiftsnivåer är konfigurerad …" och *Alla (\*)* är låst.
7. **Anteckning** – fritext, t.ex. beslutsreferens.
8. **Tillämpa reglerna på arkivet direkt efter sparning** – kryssa i för att starta tillämpningsjobbet direkt. Klicka **Spara**. Servern validerar regeln (vokabulär, lagrumsmönster, att gruppen finns i katalogen) och svarar 400 med en problemlista om något är fel. Bekräftelse: "Åtkomstreglerna är sparade och gäller omedelbart för nya bedömningar" (med tillägg om jobbet startats).
9. **Ta bort** (i listan eller i formuläret) frågar "Vill du ta bort åtkomstregeln för gruppen …? Gruppen hanteras då inte längre av åtkomstreglerna."
10. **Tillämpa reglerna nu** startar tillämpningsjobbet på alla aktiva strukturenheter efter bekräftelse; **Torrkörning** gör samma sak utan att skriva. Efter start visas "Jobbet har startats – Resultatet visas i jobbrapporten" och en fråga om du vill följa jobbet.

På **gruppsidan** (Administration → Användare och grupper → grupp) visas gruppens åtkomstregel i sammanfattning under rubriken *Åtkomstregler* (eller *Ingen regel*) med länk *Ändra* för den som får hantera regler.

## Behörighetsöversikt och rapport

**Administration → Behörighetsöversikt** (rättighet `member.read`) ger en samlad läsvy med fyra flikar:

1. **Rättigheter per grupp** – matris med en rad per rättighet (namn och beskrivning, grupperade per område) och en kolumn per grupp; bock = gruppen har rättigheten (effektivt). Gruppnamnen länkar till gruppsidan; `administrators` markeras med sköldikon.
2. **Användare** – användarnamn (länk), namn, e-post, aktiv, grupper, antal direkta respektive effektiva rättigheter (utfällbara listor). Textfilter på användarnamn, namn, e-post eller grupp.
3. **Strukturenheter** – lista över strukturenheter på nivåerna i `ui.permissionOverview.levels` (standard `fonds,class,series`) med kolumner för grupper och användare med Läsa samt grupper med Uppdatera, Skapa, Ta bort och Tilldela; facett på läsbehörighet per grupp. Listan visar bara strukturenheter den inloggade får läsa.
4. **Åtkomstregler** – de aktiva reglerna (grupp, aktiv, sekretess, lagrum, personuppgifter, personuppgiftsnivå, anteckning), eller texten "Inga åtkomstregler är aktiva".

Knappen **Rapport (HTML)** öppnar en utskriftsvänlig rapport (`behorighetsoversikt.html`, `GET /api/v2/permission-overview/report/html`) i ett nytt fönster – skriv ut eller spara som PDF. Knappen **CSV** laddar ner samma uppgifter som `behorighetsoversikt.csv` (`GET /api/v2/permission-overview/report/csv`, avgränsare enligt `csv.delimiter`, standard `;`). Rapporterna byggs live ur katalogen och indexet; kan ett avsnitt inte hämtas (t.ex. LDAP nere) visas en varning "Varningar – rapporten är ofullständig" i stället för ett avsnitt som ser komplett ut. Rapporten i HTML/CSV innehåller dessutom per strukturenhet kolumnen *Spärrade grupper*.

## Systemadministratörskonton

- ETERNA stödjer individuella administratörskonton: skapa ett personligt konto per administratör under **Administration → Användare och grupper** och gör det till medlem i `administrators` (eller i en egen grupp med ett begränsat urval administrativa rättigheter, t.ex. bara `member.manage` eller bara `access_rules.manage`). Administratörsbehörighet är rättigheter som andra och kan delas upp så att ingen behöver ett mer omfattande konto än uppgiften kräver.
- Det inbyggda kontot `admin` är ett **bootstrap-konto** som skapas vid första start (lösenord enligt installationen) och som förbigår all objektbehörighet. Använd det för installation och återställning, byt dess lösenord och använd personliga konton i det dagliga arbetet så att granskningsloggen visar vem som gjort vad.
- Alla behörighetsändringar (grupper, användare, objektbehörigheter, åtkomstregler, tillämpningsjobb) loggas i **Administration → Granskningslogg** med tidpunkt, användare och berört objekt.

## Konfiguration

### Nycklar `core.access_rules.*` (roda-core.properties)

| Nyckel | Standard | Betydelse |
|---|---|---|
| `core.access_rules.enabled` | `false` | Huvudbrytare; sätt till `true` för att använda åtkomstregler. `false` ⇒ ingen regel utvärderas alls (observer, jobb och inleveranssteg hoppar över, inget återställs). |
| `core.access_rules.file` | `access-rules.json` | Regelfilens namn, relativt `RODA_HOME/config`. En sökväg utanför konfigurationskatalogen avvisas. Saknas filen används standardfilen på klassvägen (tom regellista, `enabled=false`). |
| `core.access_rules.observer.enabled` | `true` | `true` ⇒ reglerna utvärderas direkt vid ändrad bedömning, behörighetsändring och flytt. `false` ⇒ bara via inleveranssteg och jobb. |
| `core.access_rules.observer.create_event` | `true` | Bevarandehändelse när observern ändrar behörigheter. |
| `core.access_rules.metadata_type` | `eterna_admin` | Beskrivande metadatatyp som bär bedömningarna (utan version). |
| `core.access_rules.default_lag` | `OSL` | Lag som antas när lagrumstexten saknar lagbeteckning; `defaults.defaultAct` i regelfilen har företräde. |
| `core.access_rules.ingest.create_event` | `true` | Bevarandehändelse när inleveranssteget ändrar behörigheter. |
| `core.access_rules.strip_write_on_deny` | `true` | Nekad hanterad grupp förlorar även UPDATE/DELETE/GRANT (återställs när gruppen åter tillåts). `false` ⇒ bara READ rörs och en varning loggas. |
| `core.access_rules.personuppgiftsniva.values[]` | `niva_1`, `niva_2`, `niva_3` | Tillåtna personuppgiftsnivåer (organisationens riktlinje). Används av regelredigeraren och vid validering av regelfilen; andra nivåvärden i metadata tillåts bara av regler med `*`. |

Nycklarna kan överlagras i en extern `RODA_HOME/config/roda-core.properties`; omstart krävs.

### Regelfilen `RODA_HOME/config/access-rules.json`

```json
{
  "version": 3,
  "enabled": true,
  "defaults": {
    "ejBedomd": "closed",
    "missingMetadata": "inherit",
    "missingLagrum": "closed",
    "multipleLagrum": "all",
    "defaultAct": "OSL"
  },
  "rules": [
    {
      "group": "socialtjanst",
      "enabled": true,
      "sekretess": ["ingen_sekretess", "sekretess_forekommer"],
      "lagrum": ["OSL 26:1", "OSL 21"],
      "personuppgifter": ["inga", "forekommer", "kansliga_forekommer"],
      "personuppgiftsniva": ["niva_1", "niva_2"],
      "note": "Beslut KS 2026/0123"
    },
    {
      "group": "guests",
      "enabled": true,
      "sekretess": ["ingen_sekretess"],
      "lagrum": [],
      "personuppgifter": ["inga"],
      "personuppgiftsniva": ["*"],
      "note": "Portalens anonyma besökare"
    }
  ],
  "updatedBy": "anna.jansson",
  "updatedOn": "2026-10-09T12:34:56Z"
}
```

- `version` räknas upp av servern vid varje sparning och stämplas på strukturenheterna (`accessRuleState.rulesVersion`) så att drift mot en äldre regelversion kan upptäckas. Vid `PUT /api/v2/access-rules` måste inskickad version vara exakt den lagrade (409 annars).
- Objektet `_example` i standardfilen ignoreras vid inläsning och tas bort vid första sparning.
- Värden normaliseras vid inläsning (policyval och vokabulär till gemener, gruppnamn och mönster trimmas). Filen valideras: policyval, gruppnamn (ej tomma, inga blanksteg, inga dubbletter, finns i katalogen vid sparning via API), vokabulär enligt `eterna_admin.xsd` plus `*`, personuppgiftsnivå mot `core.access_rules.personuppgiftsniva.values[]`, lagrumsmönster högst 200 tecken och tolkbara. En ogiltig fil loggas som fel och den senast giltiga versionen behålls; finns ingen behandlas reglerna som avstängda utan hanterade grupper (ingenting rörs).
- Filen läses om automatiskt när dess tidsstämpel eller storlek ändras (redigering direkt på disk fungerar utan omstart). Skrivning sker atomiskt (temporär fil + flytt).
- **Historik:** före varje sparning kopieras föregående fil till `RODA_HOME/config/access-rules.history/<ååååMMddHHmmss>-<användare>.json`. Återställ en tidigare version genom att kopiera tillbaka den till `access-rules.json` (versionen i filen ersätts vid nästa sparning via gränssnittet).

### Roller (roda-roles.properties och roda-wui.properties)

| Rättighet | Ger |
|---|---|
| `access_rules.read` | Visa åtkomstregler: lista, hämta, status, vokabulär, lagrumsnormalisering (`AccessRulesController.listAccessRules`, `retrieveAccessRule`, `retrieveStatus`, `retrieveVocabulary`, `normalizeLagrum`). Menyvalet styrs av `ui.menu.administration.access_rules.role: access_rules.read`. |
| `access_rules.manage` | Hantera åtkomstregler och tillämpa dem på alla strukturenheter användaren får läsa (`updateAccessRule`, `deleteAccessRule`, `updateAccessRuleSet`, `applyAccessRules`). |
| `member.read` | Behörighetsöversikten och rapporterna (`PermissionOverviewController.retrieveOverview`, `downloadOverviewHtml`, `downloadOverviewCsv`); menyvalet via `ui.menu.administration.permission_overview.role: member.read`. |
| `job.manage` | Starta jobbet *Tillämpa åtkomstregler* som arkivvårdsjobb på ett eget urval. |

Rättigheterna skapas i LDAP vid uppstart och visas som kryssrutor under rättighetsområdet *Åtkomstregler (sekretess/personuppgifter)* (`ui.roleTitle: access_rules`, `ui.role: access_rules.read`, `ui.role: access_rules.manage`).

### `ui.permissionOverview.levels` (roda-wui.properties)

```properties
# Nivåer (IndexedAIP.level) som tas med i behörighetsöversiktens avsnitt
# "Strukturenheter". Kommaseparerad lista i önskad sorteringsordning;
# saknas nyckeln används fonds,class,series.
ui.permissionOverview.levels = fonds,class,series
```

Nyckeln är vitlistad för klienten (`ui.sharedProperties.whitelist.configuration.prefix = ui.permissionOverview`) och läses av både sidan och rapporten. Kolumner och facetter för fliken *Strukturenheter* konfigureras som andra listor under `ui.lists.PermissionOverview_structureNodes.*`.

### Standardbehörigheter vid skapande av strukturenheter

Det är möjligt att ändra hur AIP-behörigheter beviljas till användare och grupper när ett AIP skapas i webbgränssnittet
eller när det skapas via inmatning. För att göra detta måste följande konfigurationer ändras i filen roda-core.properties.

1. Om vi vill lägga till en grupp som supergrupp som har fullständiga behörigheter när AIP skapas, behöver vi helt enkelt lägga till
   följande rader:
   ```properties
    # Admin users or groups so AIPs can be administered

    core.aip.default_permissions.admin.group[] = administrators

    core.aip.default_permissions.admin.group[].administrators.permission[] = READ
    core.aip.default_permissions.admin.group[].administrators.permission[] = UPDATE
    core.aip.default_permissions.admin.group[].administrators.permission[] = CREATE
    core.aip.default_permissions.admin.group[].administrators.permission[] = GRANT
    core.aip.default_permissions.admin.group[].administrators.permission[] = DELETE
    ```

   Raderna ovan konfigurerar administrators-gruppen. Om vi ville lägga till en superanvändare, till exempel en admin-användare, skulle vi
   göra det så här:
    ```properties
    core.aip.default_permissions.admin.user[] = admin

    core.aip.default_permissions.admin.user[].admin.permission[] = READ
    core.aip.default_permissions.admin.user[].admin.permission[] = UPDATE
    core.aip.default_permissions.admin.user[].admin.permission[] = CREATE
    core.aip.default_permissions.admin.user[].admin.permission[] = GRANT
    core.aip.default_permissions.admin.user[].admin.permission[] = DELETE
    ```

   Om vi vill ställa in behörigheter för användaren som skapar AIP, gör vi det så här:
    ```properties
    # Direct creator permissions
    core.aip.default_permissions.creator.user.permission[] = CREATE
    core.aip.default_permissions.creator.user.permission[] = UPDATE
    core.aip.default_permissions.creator.user.permission[] = READ
    ```

   Om vi vill ställa in behörigheter för en specifik användare, lägg helt enkelt till följande (exempel på att ställa in behörigheter för
   användaren 'foo'):
    ```properties
    # Direct creator permissions
    core.aip.default_permissions.users[] = foo

    core.aip.default_permissions.users[].foo.permission[] = CREATE
    core.aip.default_permissions.users[].foo.permission[] = DELETE
    core.aip.default_permissions.users[].foo.permission[] = READ
    ```

   Slutligen kan vi också lägga till andra grupper, andra än supergrupper. Dessa är normala grupper och har inte fullständiga
   behörigheter.
    ```properties
    # Additional group permissions
    core.aip.default_permissions.group[] = archivists
    core.aip.default_permissions.group[] = producers
    core.aip.default_permissions.group[] = guests

    core.aip.default_permissions.group[].archivists.permission[] = READ
    core.aip.default_permissions.group[].archivists.permission[] = UPDATE
    core.aip.default_permissions.group[].archivists.permission[] = CREATE

    core.aip.default_permissions.group[].producers.permission[] = READ

    core.aip.default_permissions.group[].guests.permission[] = READ
    ```

2. Ett krav kan vara att användaren som skapade AIP måste tillhöra en av grupperna som definieras i
   konfigurationsfilen och har åtminstone READ och UPDATE-behörigheter. Om skärningspunkten är tom, eller generellt, om
   de inställda behörigheterna inte ger användaren åtminstone READ och UPDATE-behörigheter till det skapade AIP, så
   laddas dessa behörigheter som minimibehörigheter från konfigurationsfilen.
   ```properties
    # Intersect creator groups with the configuration groups
    core.aip.default_permissions.intersect_groups = true

    # System expects a minimum set of direct or indirect permissions for the creator (DO NOT CHANGE THIS!)
    core.aip.default_permissions.creator.minimum.permissions[] = UPDATE
    core.aip.default_permissions.creator.minimum.permissions[] = READ
    ```

   Som standard, och för att överensstämma med äldre beteende, beviljas skaparens användare alla behörigheter när ett AIP skapas. Om
   du inte vill använda äldre behörigheter, ställ in denna egenskap enligt följande:
    ```properties
    # Use legacy behaviour
    core.aip.default_permissions.legacy_permissions = false
    ```

Observera att standardbehörigheterna bara sätter utgångsläget; åtkomstreglerna tillämpas därefter i inleveranssteget och kan ta bort READ (och skrivrättigheter) för hanterade grupper.

## Felsökning

| Symptom | Trolig orsak och åtgärd |
|---|---|
| **403** på Administration → Åtkomstregler eller på `/api/v2/access-rules` | Användaren saknar rättigheten `access_rules.read` (visa) eller `access_rules.manage` (ändra/tillämpa). Tilldela rättigheten på gruppen och logga in på nytt. Behörighetsöversikten kräver `member.read`. |
| Menyvalet *Åtkomstregler* eller *Behörighetsöversikt* syns inte | Samma orsak: menyvalen styrs av `ui.menu.administration.access_rules.role` respektive `ui.menu.administration.permission_overview.role`. Nyinloggning efter ändrad grupp. |
| Strukturenheter som är **"ej bedömd" syns inte** för en grupp | Avsett beteende med `ejBedomd = closed`: en bedömning som är `ej_bedomd` (egen eller ärvd) tillåts bara om regeln listar *Ej bedömd* (eller *Alla*). Med `missingMetadata = inherit-closed` gäller det även strukturenheter utan eget `eterna_admin` och utan förfader med bedömning. Lägg till `ej_bedomd` i regeln eller gör bedömningen. |
| Strukturenheter **utan bedömning syns** för en grupp trots regeln | Avsett beteende med `missingMetadata = inherit` (standard): en strukturenhet utan eget `eterna_admin` och utan bedömd förfader är obedömd struktur som reglerna inte styr – bara objektbehörigheten gäller. Ska sådana strukturenheter stängas: sätt `missingMetadata = inherit-closed` (eller bedöm strukturnivån, så ärvs bedömningen nedåt) och kör *Tillämpa reglerna nu*. |
| Grupp förlorade läsbehörighet på allt | Regeln är avstängd (Aktiv = Nej spärrar helt) eller har en tom lista (inga markerade = inget tillåtet). Markera *Alla (\*)* i de kryssrutegrupper som inte ska begränsa. |
| Sekretessbelagd strukturenhet nekas trots rätt lagrum i regeln | Texten i `sekretessgrund` kunde inte tolkas (varning i jobbrapporten, t.ex. intervall "1–3 §§" eller fritext) eller strukturenheten har flera lagrum och `multipleLagrum = all`. Rätta texten (se *Normalisera*) eller lägg till mönstren. Ett paragrafmönster täcker inte ett helt kapitel. |
| Ändrad regel har inte slagit igenom på befintliga strukturenheter | Reglerna gäller direkt bara för nya bedömningar. Kör *Tillämpa reglerna nu* (eller spara med *Tillämpa reglerna på arkivet direkt efter sparning*). Statusraden visar *Senast tillämpad*. |
| Jobbet hoppar över strukturenheter: "Jobbanvändaren saknar läsbehörighet till strukturenheten" | Tillämpningsjobbet behandlar bara strukturenheter jobbanvändaren får läsa. Starta jobbet som `admin` eller som en användare med READ på hela beståndet. |
| Jobbet hoppar över allt: "Åtkomstreglerna är avstängda i konfigurationen" | `core.access_rules.enabled = false` i `roda-core.properties`. |
| Jobbet hoppar över allt: "Regelfilen innehåller inga regler" | Regelfilen saknas eller är ogiltig och standardfilen (tom) används. Kontrollera loggen efter "Regelfilen … kan inte användas" och rätta JSON/vokabulär. |
| Loggen varnar "Behörighetsregeln för gruppen '…' matchar ingen grupp i katalogen" | Gruppnamnet i regeln stämmer inte exakt (skiftläge) med gruppen i LDAP. Regeln får ingen effekt tills namnet rättas. |
| Spara regel ger 400 "Ogiltig regeluppsättning: …" | Problemlistan i meddelandet anger fältet: okänt vokabulärvärde, otolkbart lagrumsmönster, grupp som saknas i katalogen, dubblett. |
| Spara regeluppsättning via API ger 409 | Versionskonflikt: läs om reglerna (`GET /api/v2/access-rules`) och skicka den aktuella versionen. |
| Metadataändring avvisas med "Åtkomstreglerna kunde inte tillämpas på strukturenheten …" | Observern arbetar *fail closed*: ett lagrings- eller modellfel under utvärderingen stoppar ändringen så att en strukturenhet aldrig får nya fakta utan att reglerna tillämpats. Se felet i loggen, åtgärda och gör om ändringen. |
| Var hittar jag jobben? | **Administration → Arkivvårdsjobb** (i äldre texter "Bevarande → Åtgärder") listar *Tillämpa åtkomstregler* och *Åtkomstregler: efterkommande till …*. Jobbrapporten visar per strukturenhet faktakälla, bedömning, lagrum, beslut per grupp och ändrade behörigheter; CSV-bilagan `atkomstregler-<jobb-id>.csv` hämtas från jobbsidan (`GET /api/v2/jobs/{id}/attachment/{bilaga}`). |
| Loggen | ETERNA:s serverlogg (`roda-core`) innehåller alla meddelanden från regelmotorn på svenska: inläsning/sparning av regelfilen ("Läste behörighetsregler version …"), observerns beslut ("behörigheter ändrade av åtkomstreglerna …"), startade jobb ("Startade åtkomstregeljobb …") och varningar. Första förekomsten av en varning loggas på WARN, upprepningar i samma körning på DEBUG. |
| Nedgradering till ett bygge utan åtkomstregler | När reglerna tillämpats innehåller `aip.json` fältet `accessRuleState`; ett äldre bygge kan inte läsa sådana strukturenheter. Ta bort fältet ur `aip.json` före nedgradering. |

# Automatiska regressionstester för ETERNA — design

**Datum:** 2026-10-10
**Status:** Godkänd design, inför implementationsplan
**Gren:** `feat/regression-tests` (från `dev-1.x`)

## Syfte

En automatisk regressionssvit som alltid körs inför en ny release eller en
uppgradering av en komponent (Solr, ZooKeeper, Tomcat/ETERNA-image, Siegfried,
ClamAV, OpenLDAP, unoserver). Sviten ska visa att den **driftsatta stacken**
fungerar från början till slut, inte bara att koden kompilerar.

### Bakgrund

- Core-sviten (TestNG, 47 klasser i `roda-core-tests`) är avstängd i CI eftersom
  gruppen `travis` hänger sig mot Solr Cloud. Den testar logik, inte driftsatt stack.
- Grenen `feat/playwright-e2e-tests` (Java, GWT-UI) är inte mergad.
- Det finns i dag inget som testar ETERNA-imagen tillsammans med den riktiga
  infrastrukturen.

### Mål

1. Ett kommando lokalt som startar en kortlivad stack, kör sviten och river stacken.
2. Samma svit kan köras mot en befintlig miljö (stage, prod-lik) via URL och
   inloggningsuppgifter.
3. Uppgradering av en komponent testas genom att överstyra en image-tagg, utan
   kodändring.
4. Varje extern komponent som ingest, sökning och åtkomst beror på (Solr,
   ZooKeeper, LDAP, Siegfried, ClamAV) täcks av minst ett test som misslyckas om
   komponenten inte fungerar. unoserver startas i stacken men testas först
   tillsammans med konverterings-plugins (v2).

### Utanför omfånget (v1)

- Manuell checklista inför release (separat spec).
- UI-E2E i webbläsare (`tests/ui/`, senare, i samma svit).
- Gallring, risker, reindex, audit log, notifieringar via Mailpit, CAS/SSO,
  ETERNA-plugins (office/media-converter, PDF-redaktör).
- Att laga core-sviten i CI.
- PostgreSQL. Den är valfri och avstängd som förval i
  `deploys/standalone/docker-compose.yaml`, så den kortlivade stacken följer det.
  Den kan läggas till som en compose-profil när en driftmiljö använder den.

## Beslut

| Fråga | Beslut | Motivering |
|---|---|---|
| Testlager | Black-box mot V2 REST API | Fångar fel vid komponentuppgraderingar och överlever bytet från GWT |
| Mål | Parametriserad URL plus kortlivad compose som förval | Reproducerbart i CI och användbart som rökprov mot stage |
| Verktyg | TypeScript + Playwright Test (`request`-API), Bun | Samma ramverk för senare UI-tester, HTML-rapport med traces |
| Placering | `regression/` i ETERNA-repot, utanför Maven-bygget | Testerna versioneras med koden de testar |

## Struktur

```
regression/
  package.json              # @playwright/test, typescript; körs med bun
  playwright.config.ts      # projekt: smoke, core (core beror på smoke)
  .env.example              # ETERNA_URL, ETERNA_USER, ETERNA_PASSWORD, ...
  env/
    docker-compose.yaml     # kortlivad stack, image-taggar via env-variabler
    up.sh                   # starta → vänta på health (timeout 5 min)
    down.sh                 # riv inklusive volymer
  fixtures/
    sip/valid-earks.zip     # litet E-ARK-SIP med känt innehåll
    sip/broken.zip          # medvetet trasigt SIP
    expected/valid-earks.json  # förväntade titlar, filer, PRONOM-ID:n, SHA-256
  src/
    config.ts               # läser och validerar miljövariabler
    client.ts               # tunn API-klient (Basic Auth, JSON, fel-utskrift)
    jobs.ts                 # starta jobb, vänta på slutstatus, hämta jobbrapport
    ingest.ts               # upload → job → vänta → AIP-id
    eicar.ts                # bygger EICAR-SIP vid körning (inte incheckat)
    run-id.ts               # körnings-ID för att märka och städa testdata
  tests/
    smoke/*.spec.ts
    core/*.spec.ts
  docs/
    2026-10-10-regression-tests-design.md
  README.md
```

### Konfiguration (miljövariabler)

| Variabel | Förval | Användning |
|---|---|---|
| `ETERNA_URL` | `http://localhost:8080` | Bas-URL för alla anrop |
| `ETERNA_USER` / `ETERNA_PASSWORD` | `admin` / `eterna` | Basic Auth |
| `EXPECTED_VERSION` | (tom) | Om den är satt måste ETERNA:s rapporterade version matcha |
| `JOB_TIMEOUT_MS` | `600000` | Max väntetid per jobb |
| `JOB_POLL_MS` | `3000` | Intervall för polling av jobb |
| `KEEP_DATA` | (tom) | `1` hoppar över städning |
| `*_IMAGE` | samma som `deploys/standalone/docker-compose.yaml` | Överstyr images i den kortlivade stacken, t.ex. `SOLR_IMAGE` |

## Kortlivad stack

- `env/docker-compose.yaml` använder samma tjänster och images som
  `deploys/standalone/docker-compose.yaml`. Varje image anges som
  `${SOLR_IMAGE:-ghcr.io/eterna-earkiv/solr:9.10.1}` och så vidare.
- Ett eget compose-projektnamn (`eterna-regression`) och namngivna volymer, så
  att stacken inte krockar med dev-stacken. `down.sh` kör `down -v`.
- `up.sh` väntar tills alla containrar med healthcheck är healthy och tills
  ETERNA:s `/actuator/health` svarar `UP`. Vid timeout skriver den ut vilken
  container som inte blev healthy och avslutar med en felkod.

## Testflöden

Exakta sökvägar och payloads för `find`/`count` (som ärvs från gemensamma
interface) bekräftas mot koden i implementationsplanen.

### Rökprov — `tests/smoke/` (cirka 1 minut, skapar ingen data)

| Test | Verifierar |
|---|---|
| `GET /actuator/health` ger `UP` och versionen matchar `EXPECTED_VERSION` (om den är satt) | Tomcat, Spring och att rätt build är driftsatt |
| Basic Auth med rätt lösenord ger 200, med fel lösenord 401 | Autentiseringsfiltret |
| Lista medlemmar och hitta `admin` | LDAP |
| `find`/`count` på AIP:er och jobb returnerar giltig JSON | Solr och ZooKeeper |
| Lista transfers | Staging-lagringen är monterad |

Siegfried och ClamAV kan inte nås direkt via API:t och verifieras i kärnflödet.
Det anges i README:n.

### Kärnflöde — `tests/core/` (beror på rökprovet, körs seriellt)

1. **`ingest.spec.ts`**: laddar upp `valid-earks.zip`, startar E-ARK-ingesten och
   väntar tills jobbet är `COMPLETED`. AIP-id:t delas med följande tester.
2. **`aip.spec.ts`**: titel och beskrivande metadata stämmer med
   `expected/valid-earks.json`, och antalet representationer och filer stämmer.
   Varje fil har förväntat PRONOM-ID (Siegfried).
3. **`preservation.spec.ts`**: det finns preservation events för virusskanning med
   lyckat utfall (ClamAV), för formatidentifiering och för fixity.
4. **`search.spec.ts`**: AIP:n hittas via fritext på ett unikt ord i titeln och via
   filter på körnings-ID:t (Solr-indexering).
5. **`download.spec.ts`**: en nedladdad fil har förväntad SHA-256, och AIP:n laddas
   ned som en giltig zip.
6. **`permissions.spec.ts`**: en testanvändare utan behörighet ser varken AIP:n i
   `find` eller kan hämta den (403/404). Användaren tas bort efteråt.
7. **`negative.spec.ts`**:
   - `broken.zip` gör att jobbet slutar med fel och ingen AIP skapas.
   - EICAR-SIP:et gör att virusskanningen rapporterar fynd och ingen godkänd AIP
     skapas. Det förväntade beteendet (avvisning eller karantän) fastställs mot
     den nuvarande versionen när testet skrivs, och dokumenteras i testet.

### Testdata och städning

- All data som skapas märks med ett körnings-ID (`regtest-<UTC-timestamp>`) i
  titel eller namn, så att den kan hittas och städas.
- I en afterAll tas AIP:er, transfers och testanvändare med körnings-ID:t bort via
  API:t, utom om `KEEP_DATA=1`.
- EICAR-filen skapas vid körning så att den inte ligger i git och inte triggar
  virusskydd eller GitHub-skanning.

## Körning

```bash
cd regression
bun install && bunx playwright install      # första gången
./env/up.sh
bun run test              # smoke + core
bun run test:smoke        # bara rökprov
./env/down.sh
bun run regression        # up → test → down; river stacken även om testerna misslyckas

SOLR_IMAGE=solr:9.11.0 bun run regression   # testa en komponentuppgradering
ETERNA_URL=https://stage.example ETERNA_PASSWORD=… bun run test:smoke
```

## CI — `.github/workflows/regression.yml`

- `workflow_dispatch` med indata för `eterna_image` och övriga image-taggar.
- Körs automatiskt vid `v*`-taggar, i början som ett **separat jobb som inte
  blockerar** release-flödet. Det blir blockerande efter 3–5 gröna körningar i
  följd.
- Artefakter: Playwrights HTML-rapport och traces alltid, `docker compose logs`
  vid fel.

## Felhantering och rapportering

- Kärnflödet körs med `retries: 0`, eftersom ostabil ingest är ett fynd i sig.
  Rökprovet får 1 retry för att tåla en kallstart.
- När ett jobb misslyckas eller når timeout hämtas jobbrapporten och bifogas till
  testresultatet med `testInfo.attach`.
- API-klienten tar med metod, URL, status och svarskropp i felmeddelanden. Lösenord
  loggas aldrig.
- `config.ts` avbryter direkt med ett tydligt fel om obligatoriska variabler har
  ogiltiga värden.

## Kriterier för klart

- `bun run regression` blir grön mot den kortlivade stacken med images från
  `deploys/standalone/docker-compose.yaml`.
- Varje rad i tabellen för rökprovet och varje kärnflödestest misslyckas om
  respektive komponent stoppas. Det verifieras manuellt en gång genom att
  stoppa containern och köra om.
- `bun run test:smoke` fungerar mot en befintlig miljö med bara `ETERNA_URL` och
  inloggningsuppgifter.
- `regression.yml` körs grönt via `workflow_dispatch`.
- README beskriver lokal körning, körning mot extern miljö, uppgraderingstest
  och vad som inte täcks.

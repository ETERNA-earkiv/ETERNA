# Automatiska regressionstester för ETERNA — design

**Datum:** 2026-10-10
**Status:** Godkänd design, med justeringar efter kartläggning av API:t (se "Ändringar efter kartläggning")
**Gren:** `feat/regression-tests` (från `dev-1.x`)
**Implementationsplan:** [2026-10-10-regression-tests-plan.md](2026-10-10-regression-tests-plan.md)

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
| Verktyg | TypeScript + Playwright Test (`request`-API), Bun | Samma ramverk för senare UI-tester, HTML-rapport |
| Placering | `regression/` i ETERNA-repot, utanför Maven-bygget | Testerna versioneras med koden de testar |
| Testdata | SIP:ar genereras vid körning (`src/sip.ts`) | Titeln får ett unikt körnings-ID, checksummor beräknas i stället för att hårdkodas, inget EICAR i git |

## Struktur

```
regression/
  package.json              # @playwright/test, fflate, typescript; körs med bun
  playwright.config.ts      # projekt: unit, smoke, core (core beror på smoke)
  .env.example              # ETERNA_URL, ETERNA_USER, ETERNA_PASSWORD, ...
  env/
    docker-compose.yaml     # kortlivad stack, image-taggar via env-variabler
    up.sh                   # starta → vänta på health (timeout 10 min)
    down.sh                 # riv inklusive volymer
  scripts/
    regression.sh           # up → test → (loggar vid fel) → down
  fixtures/
    schemas/*.xsd           # METS/CSIP-scheman som läggs i genererade SIP:ar
  src/
    config.ts               # läser och validerar miljövariabler
    run-id.ts               # körnings-ID (en enda Solr-token, t.ex. regtest20261010t120000)
    types.ts                # minimala typer för API-svaren
    filters.ts              # byggare för filterparametrar
    client.ts               # tunn API-klient (Basic Auth, JSON, fel-utskrift)
    jobs.ts                 # vänta på jobbets slutstatus, hämta jobbrapporter
    ingest.ts               # upload → job → vänta → AIP-id
    sip.ts                  # bygger CSIP-SIP och trasigt SIP i minnet
    eicar.ts                # EICAR-testfilen, avkodad vid körning
    cleanup.ts              # spårar och tar bort testdata
    fixtures.ts             # Playwright-fixtures (klient, körnings-ID, ingest, städning)
  tests/
    unit/*.spec.ts          # rena enhetstester av hjälparna, ingen stack krävs
    smoke/*.spec.ts
    core/*.spec.ts
  docs/
    2026-10-10-regression-tests-design.md
    2026-10-10-regression-tests-plan.md
    api-probes.md           # verifierade API-svar från en körande stack
  README.md
```

### Konfiguration (miljövariabler)

| Variabel | Förval | Användning |
|---|---|---|
| `ETERNA_URL` | `http://localhost:8080` | Bas-URL för alla anrop |
| `ETERNA_USER` / `ETERNA_PASSWORD` | `admin` / `eterna` | Basic Auth |
| `EXPECTED_VERSION` | (tom) | Om den är satt måste `git.build.version` i `/version.json` matcha |
| `JOB_TIMEOUT_MS` | `600000` | Max väntetid per jobb |
| `JOB_POLL_MS` | `3000` | Intervall för polling av jobb |
| `KEEP_DATA` | (tom) | `1` hoppar över städning |
| `ETERNA_PORT` | `8080` | Port som den kortlivade stacken publicerar ETERNA på |
| `*_IMAGE` | samma som `deploys/standalone/docker-compose.yaml` | Överstyr images i den kortlivade stacken, t.ex. `SOLR_IMAGE` |

## Kortlivad stack

- `env/docker-compose.yaml` är en kopia av tjänsterna i
  `deploys/standalone/docker-compose.yaml`, med samma images och miljövariabler.
  Varje image anges som `${SOLR_IMAGE:-ghcr.io/eterna-earkiv/solr:9.10.1}` och så
  vidare. Konfigurationsfilerna (`roda-core.properties`, `pbkdf2.ldif`) monteras
  direkt från `deploys/standalone/` så att de inte kopieras.
- Mailpit läggs till med tjänstenamnet `mailserver`, som ETERNA:s `SMTP_HOST`
  pekar på. Annars kan skapande av användare (som skickar e-post) misslyckas.
- Bara ETERNA publicerar en port (`ETERNA_PORT`). Ett eget projektnamn
  (`eterna-regression`) och egna volymer gör att stacken inte krockar med
  dev-stacken. `down.sh` kör `down -v`.
- `up.sh` väntar tills `/actuator/health` svarar `UP` (timeout 10 min, eftersom
  ClamAV laddar ned virusdefinitioner vid första start). Vid timeout skriver den ut
  status och health för containrarna och avslutar med en felkod.

## Testflöden

Endpoints och fält är verifierade i koden och bekräftas mot en körande stack i
`docs/api-probes.md`. Sviten använder `find` (och läser `totalCount`) i stället för
`count`, eftersom `count` för jobb och medlemmar saknar rollnyckel och troligen
ger 403.

### Rökprov — `tests/smoke/` (cirka 1 minut, skapar ingen data)

| Test | Verifierar |
|---|---|
| `GET /actuator/health` ger `UP` | Tomcat och Spring |
| `git.build.version` i `/version.json` matchar `EXPECTED_VERSION` (om den är satt) | Att rätt build är driftsatt |
| Basic Auth med rätt lösenord: `GET /api/v2/members/users/authenticated` ger användaren | Autentisering mot LDAP |
| Basic Auth med fel lösenord ger 401 | Autentiseringsfiltret |
| `GET /api/v2/members/users/{user}` ger användaren | Läsning från LDAP |
| `find` på AIP:er, jobb och transfers ger giltig `IndexResult` | Solr och ZooKeeper |

Siegfried och ClamAV kan inte nås direkt via API:t och verifieras i kärnflödet.
Det anges i README:n.

### Kärnflöde — `tests/core/` (beror på rökprovet, körs seriellt)

Ett giltigt SIP genereras med två datafiler (`document.txt` och `record.xml`) och en
titel som innehåller körnings-ID:t. Det ingestas en gång per körning, via en
worker-fixture, med `ConfigurableIngestPlugin` (standardinleveransen i
gränssnittet) och alla steg påslagna.

1. **`ingest.spec.ts`**: jobbet når `COMPLETED` med 1 lyckat och 0 misslyckade
   källobjekt, och exakt en AIP skapas. Jobbrapporterna bifogas till resultatet.
2. **`aip.spec.ts`**: den indexerade AIP:ns titel stämmer, och den har state
   `ACTIVE`, en representation och de två datafilerna. Varje fil har förväntat
   PRONOM-ID (Siegfried): `x-fmt/111` för txt och `fmt/101` för xml, bekräftat i
   `api-probes.md`.
3. **`preservation.spec.ts`**: det finns events med utfallet `SUCCESS` för
   `virus check` (ClamAV), `format identification` (Siegfried) och
   `message digest calculation` (checksummor vid ingest; `fixity check` skapas
   bara av det fristående fixity-pluginet).
4. **`search.spec.ts`**: AIP:n hittas via fritext på körnings-ID:t och via filter på
   `ingestJobId`, alltså två olika frågevägar i Solr.
5. **`download.spec.ts`**: varje nedladdad fil har samma SHA-256 som i SIP:et, och
   AIP:n laddas ned som zip (magiska bytes `PK`).
6. **`permissions.spec.ts`**: en testanvändare med rollerna `aip.read` och
   `representation.read`, men utan objektbehörighet, kan logga in men ser inte
   AIP:n i `find`. Den får 403 på `GET /api/v2/aips/find/{id}` och på
   filnedladdning. Användaren tas bort efteråt.
7. **`negative.spec.ts`**:
   - Ett SIP utan `METS.xml` gör att jobbet har 1 misslyckat källobjekt och att
     ingen AIP skapas.
   - Ett SIP med EICAR-filen gör att antivirussteget rapporterar `FAILURE`.
   - **Känt beteende att bekräfta:** virussteget i `ConfigurableIngestPlugin` är
     `mandatory=false`, så enligt koden kan ett infekterat SIP ändå bli en `ACTIVE`
     AIP. Testet "infekterat SIP blir inte en ACTIVE AIP" är markerat med
     `test.fail()` om körningen i `api-probes.md` bekräftar beteendet. Det
     dokumenterar avvikelsen utan att sviten blir röd, och slår larm om beteendet
     ändras. Om det är en bugg eller avsett beteende beslutas separat.

### Testdata och städning

- All data som skapas märks med körnings-ID:t (`regtest<UTC-tidsstämpel>`, en
  enda Solr-token) i SIP-namn, titel och användarnamn.
- En worker-fixture spårar AIP:er, transfers och användare. Vid teardown tas de
  bort via API:t (asynkrona radera-jobb väntas in), utom om `KEEP_DATA=1`.
  Fel vid städningen rapporteras och döljs inte.
- EICAR-filen lagras base64-kodad i källkoden och avkodas bara i minnet, så att
  den inte triggar virusskydd eller GitHub-skanning.

## Körning

API-tester behöver inga webbläsare, så `playwright install` behövs inte.

```bash
cd regression
bun install
./env/up.sh
bun run test              # unit + smoke + core
bun run test:smoke        # bara rökprov
bun run test:unit         # bara enhetstester (ingen stack)
./env/down.sh
bun run regression        # up → test → down; river stacken även om testerna misslyckas

SOLR_IMAGE=solr:9.11.0 bun run regression   # testa en komponentuppgradering
ETERNA_URL=https://stage.example ETERNA_PASSWORD=… bun run test:smoke
```

## CI — `.github/workflows/regression.yml`

- `workflow_dispatch` med indata för `eterna_image` och övriga image-taggar.
- `workflow_run` efter ett lyckat `Release`-flöde, med imagen
  `ghcr.io/eterna-earkiv/eterna:<tagg>`. Det är ett eget workflow och blockerar
  därför inte releasen. Det blir blockerande efter 3–5 gröna körningar i följd.
- Artefakter: Playwrights HTML-rapport och `test-results/` alltid,
  `docker compose logs` vid fel.

## Felhantering och rapportering

- Kärnflödet körs med `retries: 0`, eftersom ostabil ingest är ett fynd i sig.
  Rökprovet får 1 retry för att tåla en kallstart.
- Jobbrapporterna bifogas till testresultatet med `testInfo.attach`.
- API-klienten tar med metod, URL, status och svarskropp (högst 2000 tecken) i
  felmeddelanden. Lösenord och headers loggas aldrig.
- `config.ts` avbryter direkt med ett tydligt fel om en variabel har ett ogiltigt
  värde.

## Kriterier för klart

- `bun run regression` blir grön mot den kortlivade stacken med images från
  `deploys/standalone/docker-compose.yaml`.
- Komponenttäckningen är verifierad manuellt en gång: med `clamd`, `siegfried`
  respektive `openldap` stoppad misslyckas minst ett test, och det noteras i
  `api-probes.md`.
- `bun run test:smoke` fungerar mot en befintlig miljö med bara `ETERNA_URL` och
  inloggningsuppgifter.
- `regression.yml` körs grönt via `workflow_dispatch`.
- README beskriver lokal körning, körning mot extern miljö, uppgraderingstest
  och vad som inte täcks.

## Ändringar efter kartläggning

Följande justerades efter att API:t kartlagts (2026-10-10), jämfört med den
ursprungligen godkända designen:

- Statiska fixture-zip-filer och `expected/*.json` ersattes av SIP:ar som
  genereras vid körning (`src/sip.ts`).
- Versionen läses från `/version.json`, eftersom actuator inte exponerar den.
- Fixity vid ingest verifieras via `message digest calculation`.
- EICAR-fallet hanteras med `test.fail()` om koden visar sig släppa igenom
  infekterade SIP:ar (virussteget är inte obligatoriskt).
- Mailpit lades till i stacken. Timeouten för `up.sh` höjdes till 10 min.
- `count` ersattes av `find` och `totalCount`.
- Ingen webbläsarinstallation behövs.
- CI-triggern för releaser är `workflow_run` efter `Release`, eftersom imagen
  först finns när release-flödet är klart.

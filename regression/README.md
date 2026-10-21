# ETERNA regressionstester

Automatiska black-box-tester mot ETERNA:s V2 REST API. De körs inför varje release
och varje uppgradering av en komponent (Solr, ZooKeeper, ClamAV, Siegfried, OpenLDAP,
unoserver eller ETERNA-imagen).

Design: [docs/2026-10-10-regression-tests-design.md](docs/2026-10-10-regression-tests-design.md) ·
Verifierade API-fakta: [docs/api-probes.md](docs/api-probes.md)

## Krav

- Bun ≥ 1.2 och Node ≥ 22 (Playwright körs på Node)
- Docker med Compose v2
- Inga webbläsare behövs, eftersom sviten bara använder Playwrights `request`-API.

```bash
cd regression
bun install
```

## Köra allt mot en kortlivad stack

```bash
bun run regression
```

Kommandot startar stacken i `env/docker-compose.yaml` (projektet `eterna-regression`,
egna volymer), väntar på `/actuator/health` och på att RODA startat klart, kör alla
tester och river stacken med volymer, även om något fallerar. Vid fel sparas
containerloggarna i `test-results/compose.log`. Första starten tar några minuter
eftersom ClamAV laddar ned virusdefinitioner.

Port 8080 används som förval. Om något annat redan använder den:
`ETERNA_PORT=18080 bun run regression`.

Stegvis, till exempel vid felsökning:

```bash
./env/up.sh
bun run test:unit     # enhetstester, ingen stack krävs
bun run test:smoke    # rökprov, läser bara
bun run test:core     # rökprov + kärnflöde
KEEP_DATA=1 bun run test:core   # låt testdatan ligga kvar
./env/down.sh
bun run report        # öppna HTML-rapporten
```

## Stacken

Stacken körs utan PostgreSQL, precis som `deploys/standalone`. Därför sätts
`MANAGEMENT_HEALTH_DB_ENABLED=false`; annars blir `/actuator/health` DOWN.

ETERNA v1.0.0 har ett startrace i `OnOffFilter`: om `/api/*` anropas innan konfigurationen
laddats blir Basic Auth permanent AV och alla anrop blir `guest`. Imagens egen healthcheck
utlöser detta, så compose-filen ersätter den med `/actuator/health` och `env/up.sh`
väntar dessutom på loggraden "Started RODA". Se `docs/api-probes.md`, "Noter om stacken".

Rökprovet `fel lösenord ger 401` (`smoke/auth.spec.ts`) upptäcker just det här felet i
vilken miljö som helst. Det gör rökproven värdefulla även mot stage och prod.

## Testa en komponentuppgradering

Överstyr imagen med en miljövariabel. Ingen kodändring behövs:

```bash
SOLR_IMAGE=ghcr.io/eterna-earkiv/solr:9.11.0 bun run regression
ETERNA_IMAGE=ghcr.io/eterna-earkiv/eterna:v1.0.1 EXPECTED_VERSION=1.0.1 bun run regression
```

Variabler: `ETERNA_IMAGE`, `SOLR_IMAGE`, `ZOOKEEPER_IMAGE`, `CLAMAV_IMAGE`,
`SIEGFRIED_IMAGE`, `OPENLDAP_IMAGE`, `UNOSERVER_IMAGE`, `MAILPIT_IMAGE`. Förvalen är
desamma som i `deploys/standalone/docker-compose.yaml`. Håll de två filerna i synk.

## Köra mot en befintlig miljö

```bash
ETERNA_URL=https://stage.example ETERNA_USER=admin ETERNA_PASSWORD=… bun run test:smoke
```

Rökproven skapar ingen data. `test:core` skapar AIP:er, transfers och en
testanvändare, märkta med ett körnings-ID (`regtest<tidsstämpel><4 hextecken>`), och tar bort dem
efteråt. Kör `test:core` mot en delad miljö bara om det är okej att den får tillfälliga
testobjekt. Inställningarna kan också läggas i `.env` (se `.env.example`).

## Vad som täcks

| Komponent | Test |
|---|---|
| Tomcat/Spring | `smoke/health.spec.ts` |
| OpenLDAP | `smoke/auth.spec.ts` (Basic Auth och användaruppslag) |
| Solr + ZooKeeper | `smoke/index.spec.ts`, `core/search.spec.ts` |
| Ingest (E-ARK CSIP) | `core/ingest.spec.ts`, `core/aip.spec.ts` |
| ClamAV | `core/preservation.spec.ts` (`virus check`), `core/negative.spec.ts` (EICAR) |
| Siegfried | `core/aip.spec.ts` (PRONOM), `core/preservation.spec.ts` |
| Lagring och nedladdning | `core/download.spec.ts` (SHA-256 och AIP-zip) |
| Behörigheter | `core/permissions.spec.ts` |

Siegfried och ClamAV kan inte nås direkt via API:t, så de täcks bara av kärnflödet,
inte av rökproven.

**Täcks inte (än):** unoserver/konverterings-plugins, gallring, risker, reindex,
audit log, e-postnotifieringar, CAS/SSO, gränssnittet (GWT). PostgreSQL ingår inte i
stacken.

## Kända avvikelser

- `core/negative.spec.ts` › "infekterat SIP blir inte en ACTIVE AIP" är markerat med
  `test.fail()`. Ett infekterat SIP blir en ACTIVE AIP (virus check-eventet får
  `FAILURE` och jobbet `PARTIAL_SUCCESS`, eftersom virussteget är `mandatory=false`).
  Testet rapporteras som "expected to fail" och larmar när beteendet ändras. Se
  `docs/api-probes.md`, rad 15.
- `GET /api/v2/members/users/{name}` ger 200 med `id: null` för en användare som inte
  finns, inte 404. Den som verifierar att en användare är borttagen måste kontrollera
  `id`, inte statuskoden. Se `docs/api-probes.md`, rad 11.
- Om ett kärntest fallerar startar Playwright om workern, och då körs den delade
  ingesten om (nytt körnings-ID). Följdfel efter det första felet är därför ofta
  brus: titta alltid på det första felet. I CI avbryts körningen efter fem fel
  (`maxFailures`) och jobb får högst 5 minuter (`JOB_TIMEOUT_MS`).

## CI

`.github/workflows/regression.yml` är förberedd för manuell körning (Actions →
Regression → Run workflow, med valfria image-taggar) och för automatisk körning efter
ett lyckat `Release`-flöde, utan att blockera releasen.

**Inte aktiv än.** GitHub kör `workflow_dispatch` och `workflow_run` bara från
workflow-filer på repots förvalda gren (`main`). Beslut 2026-10-10: sviten körs
lokalt med `bun run regression` tills vidare. CI aktiveras genom att lägga in
`regression.yml` på `main`. Den checkar ut den ref som testas, så själva testerna
hämtas från `dev-1.x` eller release-taggen.

# Verifierade API-fakta

Antaganden från kodkartläggningen (2026-10-10) och hur de bekräftats mot en körande
stack. Uppdatera tabellen när ETERNA eller en komponent uppgraderas och ett test
börjar fallera på grund av ändrat API-beteende.

Stack: images enligt `env/docker-compose.yaml`, ETERNA-version enligt `/version.json`
(`git.build.version` = 1.0.0, commit acbc354).

| # | Antagande | Status | Resultat |
|---|---|---|---|
| 1 | `/actuator/health` är öppen och svarar `{"status":"UP"}` | Bekräftad | `{"status":"UP"}` utan inloggning |
| 2 | `/version.json` har `git.build.version` | Bekräftad | `"git.build.version": "1.0.0"` |
| 3 | Fel lösenord ger 401, rätt ger 200 | Bekräftad | `admin:eterna` 200, `admin:fel` 401 (förutsätter att `InternalApiAuthFilter` är PÅ, se not 1) |
| 4 | `POST /api/v2/jobs/count` ger 403 för admin, så sviten använder `find` | Avviker | `count` ger 200 `{"result":-1}` (inte 403, men värdet är oanvändbart). `jobs/find` ger 200 med `totalCount` och fungerar. Slutsatsen (använd `find`) står sig |
| 5 | `GET /api/v2/members/users/{name}` har `type: "User"` | Bekräftad | `GET .../users/admin` ger `name` = `admin` och `type` = `User` (rökprovet `användaren kan läsas från LDAP`) |
| 6 | Uppladdning ger `TransferredResource` med `uuid` | Bekräftad | `POST /transfers/create/resource` (multipart `resource` + `commit=true`) ger `TransferredResource` med `uuid`, `id`, `name` (ingestflödet) |
| 7 | Ingest av giltigt SIP: `COMPLETED`, 1 lyckat, rapportens `outcomeObjectId` = AIP-id | Bekräftad | Jobb `COMPLETED`, 1 lyckat, 0 misslyckade, exakt en AIP via rapportens `outcomeObjectId` (ingest ca 10-15 s totalt inkl. indexering) |
| 8 | `files/find` med `aipId` + `isDirectory=false` ger bara de två datafilerna, med `fileFormat.pronom` `x-fmt/111` och `fmt/101` | Bekräftad | Exakt datafilerna, med `fileFormat.pronom` enligt `VALID_SIP_PRONOM` (`x-fmt/111`, `fmt/101`); inga METS-poster |
| 9 | Events med `aipID` innehåller `virus check`, `format identification`, `message digest calculation` med `SUCCESS` | Bekräftad | Alla tre eventtyper finns med `eventOutcome` `SUCCESS` |
| 10 | `GET /api/v2/aips/{id}/download` ger zip (`PK`) | Bekräftad | Svaret börjar med `PK` |
| 11 | Användare skapad via API med `password: {"chars": ...}` kan logga in direkt | Bekräftad | `POST /api/v2/members/users` (email, name, fullName, roles, groups, guest=false, password.chars, values=[]) ger en användare som direkt kan logga in; ingen aktivering/e-postbekräftelse behövs. Radering via `DELETE /members/users/{name}`; efteråt ger `GET .../users/{name}` dock inte 404 utan 200 med `id: null`, `name: null` (kontrollera `id`) |
| 12 | Användare utan objektbehörighet: `find` utelämnar AIP:n, `find/{id}` och filnedladdning ger 403 | Bekräftad | Användare med `aip.read` + `representation.read` ser AIP:n varken i `aips/find` (`totalCount` 0), får 403 på `aips/find/{id}` (admin får 200 på samma sökväg) eller på `files/{uuid}/download`. Nya AIP:er är alltså inte läsbara för alla som förval |
| 13 | SIP utan METS.xml: `COMPLETED`, 1 misslyckat, `outcomeObjectId` = `NO_OUTCOME_ID` | Bekräftad | Jobb `COMPLETED`, `sourceObjectsProcessedWithFailure` 1, ingen AIP (`aipIds` tom) |
| 14 | EICAR: antivirussteget (plugin-klass innehåller `AntivirusPlugin`) har `FAILURE` | Avviker | Jobbrapporten är platt: en enda rapport (`ConfigurableIngestPlugin`, `PARTIAL_SUCCESS`) utan delsteg, så `AntivirusPlugin` finns inte som rapport. Fyndet syns i rapportens `pluginDetails` (`...eicar.txt: Eicar-Test-Signature FOUND`, `Infected files: 1`) och som preservation-eventet `virus check` med `eventOutcome` `FAILURE`. ClamAV loggar `instream(...): Eicar-Test-Signature FOUND`. Testet kontrollerar detta i stället |
| 15 | EICAR: AIP skapas ändå och blir `ACTIVE` (virussteget `mandatory=false`) | Bekräftad | Jobb `COMPLETED` med `sourceObjectsProcessedWithPartialSuccess` 1 (0 misslyckade); exakt en AIP med `state` `ACTIVE` trots `virus check` `FAILURE`. Fynd: infekterat innehåll hamnar i arkivet. Testet använder `test.fail()` och larmar när beteendet ändras |
| 16 | Radering av AIP och transfer via `POST .../delete` ger ett jobb som når `COMPLETED` | Bekräftad | Städningen via `POST .../delete` nådde `COMPLETED`; efter körning ger sökning på `regtest*` `totalCount` 0 (med `KEEP_DATA=1` 1) |

## Noter om stacken

1. **Startrace i ETERNA 1.0.0 (kringgås).** `OnOffFilter` avgör
   `ui.filter.internal.enabled` vid första anropet mot `/api/*`. Sker det innan RODA
   initierat klart blir `InternalApiAuthFilter` permanent AV. Då ignoreras all basic auth
   och varje anrop blir användaren `guest` (`/members/users/authenticated` ger 200 oavsett
   lösenord, övriga anrop 403). Imagens egen healthcheck (`curl /api/v2/openapi`) utlöser
   detta under uppstart, så compose-filen ersätter den med `/actuator/health`, och
   `up.sh` väntar dessutom på loggraden "Started RODA" (health blir UP före det).
   Anropa aldrig `/api/*` innan RODA startat. Loggraden "InternalApiAuthFilter is ON"
   finns i `/roda/log/*.log` i containern (inte i `docker logs`).
   Racet reproducerades utanför regressionsstacken: användarens andra stack
   `eterna_sie5_2` (v1.0.0, imagens healthcheck) observerades 2026-10-10 av controllern med
   health DOWN och fel lösenord som gav 200. Den stacken är orörd.
2. **Ingen PostgreSQL.** Deploy-compose kör utan PostgreSQL (utkommenterad), och
   regressionsstacken speglar det. Utan databas blir `/actuator/health` DOWN efter 30 s
   (DB-indikatorn), därför sätts `MANAGEMENT_HEALTH_DB_ENABLED=false`. Appen fungerar utan
   databas (`jobs/find` ger 200 på ca 0,1 s).
3. `/api/v2/members/users/authenticated` ger 401 med fel lösenord först när filtret är PÅ.

## Komponenttäckning

Fylls i under uppgift 14: vilket test som fallerar när respektive container stoppas.

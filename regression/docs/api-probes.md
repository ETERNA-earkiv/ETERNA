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
| 5 | `GET /api/v2/members/users/{name}` har `type: "User"` | Bekräftas i uppgift 8 | |
| 6 | Uppladdning ger `TransferredResource` med `uuid` | Bekräftas i uppgift 9 | |
| 7 | Ingest av giltigt SIP: `COMPLETED`, 1 lyckat, rapportens `outcomeObjectId` = AIP-id | Bekräftas i uppgift 9 | |
| 8 | `files/find` med `aipId` + `isDirectory=false` ger bara de två datafilerna, med `fileFormat.pronom` `x-fmt/111` och `fmt/101` | Bekräftas i uppgift 9 | |
| 9 | Events med `aipID` innehåller `virus check`, `format identification`, `message digest calculation` med `SUCCESS` | Bekräftas i uppgift 9 | |
| 10 | `GET /api/v2/aips/{id}/download` ger zip (`PK`) | Bekräftas i uppgift 9 | |
| 11 | Användare skapad via API med `password: {"chars": ...}` kan logga in direkt | Bekräftas i uppgift 10 | |
| 12 | Användare utan objektbehörighet: `find` utelämnar AIP:n, `find/{id}` och filnedladdning ger 403 | Bekräftas i uppgift 10 | |
| 13 | SIP utan METS.xml: `COMPLETED`, 1 misslyckat, `outcomeObjectId` = `NO_OUTCOME_ID` | Bekräftas i uppgift 11 | |
| 14 | EICAR: antivirussteget (plugin-klass innehåller `AntivirusPlugin`) har `FAILURE` | Bekräftas i uppgift 11 | |
| 15 | EICAR: AIP skapas ändå och blir `ACTIVE` (virussteget `mandatory=false`) | Bekräftas i uppgift 11 | |
| 16 | Radering av AIP och transfer via `POST .../delete` ger ett jobb som når `COMPLETED` | Bekräftas i uppgift 8 | |

## Noter om stacken

1. **Startrace i ETERNA 1.0.0 (kringgås i compose-filen).** `OnOffFilter` avgör
   `ui.filter.internal.enabled` vid första anropet mot `/api/*`. Imagens egen
   healthcheck (`curl /api/v2/openapi`) körs redan under uppstart, innan konfigurationen
   är laddad, så `InternalApiAuthFilter` blir permanent AV. Då ignoreras all basic auth
   och varje anrop blir användaren `guest` (`/members/users/authenticated` ger 200 oavsett
   lösenord, övriga anrop 403). Compose-filen ersätter därför healthchecken med
   `/actuator/health`. Anropa aldrig `/api/*` innan ETERNA loggat "Started RODA".
2. **Postgres krävs.** ETERNA 1.0.0 behöver en databas; deploy-compose saknar den (utkommenterad),
   så `/actuator/health` blir aldrig UP. Regressionsstacken har en `postgres`-tjänst
   och `SPRING_DATASOURCE_URL`.
3. `/api/v2/members/users/authenticated` är inte ett bra inloggningsprov med fel lösenord
   om filtret är AV (se 1); den ger 401 först när filtret är PÅ.

## Komponenttäckning

Fylls i under uppgift 14: vilket test som fallerar när respektive container stoppas.

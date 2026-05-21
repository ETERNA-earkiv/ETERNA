# Design: Granskningslogg för findByUuid i alla REST-controllers

**Datum:** 2026-05-21
**Kontext:** Del av feature/katalogträd-ghost-noder — säkerställer att AIP-åtkomst via katalogträdet loggas
**Branch:** `feat/480-katalogträd-ghost-noder`

---

## Problembeskrivning

`findByUuid()` i alla 17 REST-controllers är idag implementerade som direkta `indexService.retrieve()`-anrop utan `requestHandler.processRequest()`-omslutning. Det innebär att ingen `LogEntry` skapas när en användare hämtar ett enskilt objekt via UUID. Övriga läs-operationer (t.ex. `getAncestors()` i `AIPController`) är korrekt omslutna och loggar till granskningsloggen.

Det primära behovet: när en användare klickar på en nod i katalogträdet triggas `BrowseAIP.getAndRefresh()` → `AIPController.findByUuid()`. Det anropet ska loggas. Eftersom `findByUuid` saknar loggning konsekvent i hela kodbasen åtgärdas detta för alla 17 controllers.

---

## Lösning

### Övergripande

Två lager ändras:

| Lager | Förändring |
|---|---|
| 17 REST-controllers | `findByUuid()` wrappas i `requestHandler.processRequest()` |
| `BrowseAIP.java` | `getAndRefresh()` skapar `Services` med orsakssträng |
| `ClientMessages.java` + `.properties`-filer | Ny nyckel: `browseAIPReasonViewAIP` |

---

### Mönster för controllers

**Typ A — innehållsentiteter med objektbehörighet** (AIP, Representation, File, DIP, DIPFile, PreservationEvent):

```java
@Override
public <EntityType> findByUuid(String uuid, String localeString) {
  return requestHandler.processRequest(
    new RequestHandler.RequestProcessor<EntityType>() {
      @Override
      public EntityType process(RequestContext requestContext,
          RequestControllerAssistant controllerAssistant)
          throws RODAException, RESTException {
        controllerAssistant.setRelatedObjectId(uuid);
        EntityType obj = requestContext.getIndexService()
          .retrieve(EntityType.class, uuid, new ArrayList<>());
        controllerAssistant.checkObjectPermissions(requestContext.getUser(), obj);
        return obj;
      }
    });
}
```

`AIPController.findByUuid()` bevarar befintlig distributed mode-logik inuti `processRequest`.

**Typ B — systementiteter utan per-objekt-behörighet** (AuditLog, Job, JobReport, Risk, RiskIncidence, RepresentationInformation, Members, Notification, PreservationAgent, TransferredResource, DisposalConfirmation):

Samma mönster som Typ A men utan `checkObjectPermissions`. Rollen kontrolleras av `processRequest` via `checkRoles()`. Bestäms per controller baserat på om befintliga metoder i samma klass använder `checkObjectPermissions` eller inte — om de gör det, lägg till det; annars utelämna.

---

### BrowseAIP — orsakssträng

`BrowseAIP.refresh()` (privat metod) skapar idag `new Services("Retrieve AIP", "get")` med en hårdkodad engelska sträng. Ersätts med i18n-nyckeln:

```java
// Före
Services service = new Services("Retrieve AIP", "get");

// Efter
Services service = new Services(messages.browseAIPReasonViewAIP(), "get");
```

`Services`-instansen återanvänds för alla efterföljande anrop i `refresh()` (`getAncestors`, `retrieveAIPRuleProperties` etc.) — detta är befintligt beteende och ändras inte.

---

### i18n

Ny nyckel i tre filer:

| Fil | Nyckel | Värde |
|---|---|---|
| `ClientMessages.java` | `String browseAIPReasonViewAIP()` | — |
| `ClientMessages.properties` | `browseAIPReasonViewAIP` | `View AIP` |
| `ClientMessages_sv_SE.properties` | `browseAIPReasonViewAIP` | `Visa AIP` |

---

### Resultat i granskningsloggen

Varje `findByUuid`-anrop skapar en `LogEntry` med:

| Fält | Värde |
|---|---|
| `actionComponent` | Controllerns fullt kvalificerade klassnamn |
| `actionMethod` | `findByUuid` |
| `relatedObjectId` | Objektets UUID |
| `state` | `SUCCESS` / `UNAUTHORIZED` / `FAILURE` |
| `auditLogRequestHeaders.reason` | T.ex. `"Visa AIP"` (från BrowseAIP) |
| `username` | Inloggad användare |
| `datetime` | Tidsstämpel |

---

### Felhantering

| Scenario | Hantering |
|---|---|
| `NotFoundException` | `state = FAILURE`, undantag propageras (HTTP 404) |
| `AuthorizationDeniedException` | `state = UNAUTHORIZED`, undantag propageras (HTTP 403) |
| Övriga undantag | `state = FAILURE`, undantag propageras |

`processRequest` hanterar state-sättning automatiskt i sin finally-block.

---

### Berörda filer

**Controllers (17 st):**
- `roda-ui/roda-wui/src/main/java/org/roda/wui/api/v2/controller/AIPController.java`
- `roda-ui/roda-wui/src/main/java/org/roda/wui/api/v2/controller/RepresentationController.java`
- `roda-ui/roda-wui/src/main/java/org/roda/wui/api/v2/controller/FilesController.java`
- `roda-ui/roda-wui/src/main/java/org/roda/wui/api/v2/controller/DIPController.java`
- `roda-ui/roda-wui/src/main/java/org/roda/wui/api/v2/controller/DIPFileController.java`
- `roda-ui/roda-wui/src/main/java/org/roda/wui/api/v2/controller/PreservationEventController.java`
- `roda-ui/roda-wui/src/main/java/org/roda/wui/api/v2/controller/PreservationAgentController.java`
- `roda-ui/roda-wui/src/main/java/org/roda/wui/api/v2/controller/RiskController.java`
- `roda-ui/roda-wui/src/main/java/org/roda/wui/api/v2/controller/RiskIncidenceController.java`
- `roda-ui/roda-wui/src/main/java/org/roda/wui/api/v2/controller/RepresentationInformationController.java`
- `roda-ui/roda-wui/src/main/java/org/roda/wui/api/v2/controller/DisposalConfirmationController.java`
- `roda-ui/roda-wui/src/main/java/org/roda/wui/api/v2/controller/TransferredResourceController.java`
- `roda-ui/roda-wui/src/main/java/org/roda/wui/api/v2/controller/JobsController.java`
- `roda-ui/roda-wui/src/main/java/org/roda/wui/api/v2/controller/JobReportController.java`
- `roda-ui/roda-wui/src/main/java/org/roda/wui/api/v2/controller/NotificationController.java`
- `roda-ui/roda-wui/src/main/java/org/roda/wui/api/v2/controller/MembersController.java`
- `roda-ui/roda-wui/src/main/java/org/roda/wui/api/v2/controller/AuditLogController.java`

**Client:**
- `roda-ui/roda-wui/src/main/java/org/roda/wui/client/browse/BrowseAIP.java`

**i18n:**
- `roda-ui/roda-wui/src/main/java/config/i18n/client/ClientMessages.java`
- `roda-ui/roda-wui/src/main/resources/config/i18n/client/ClientMessages.properties`
- `roda-ui/roda-wui/src/main/resources/config/i18n/client/ClientMessages_sv_SE.properties`

---

### Acceptanskriterier

- [ ] Klick på nod i katalogträdet skapar en `LogEntry` med `actionMethod = findByUuid` och korrekt `relatedObjectId`
- [ ] `state = UNAUTHORIZED` loggas om användaren saknar behörighet till objektet
- [ ] `state = FAILURE` loggas om objektet inte finns
- [ ] `auditLogRequestHeaders.reason = "Visa AIP"` för katalogträd-navigering
- [ ] Samtliga 17 controllers wrappar `findByUuid` konsekvent
- [ ] Befintliga tester passerar

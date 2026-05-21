# findByUuid Granskningslogg — Implementationsplan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Wrappa `findByUuid()` i alla 17 REST-controllers med `requestHandler.processRequest()` så att varje AIP-åtkomst (och motsvarande för övriga entitetstyper) skapar en post i granskningsloggen.

**Architecture:** `requestHandler.processRequest()` är det etablerade mönstret i kodbasen — det sätter `requestContext`, anropar `registerAction()` i ett finally-block och sätter `state` baserat på eventuella undantag. Innehållsentiteter (AIP, Representation, File, DIP, DIPFile, PreservationEvent) får dessutom `checkObjectPermissions`. Systementiteter (Job, Risk, AuditLog m.fl.) får bara rollkontrollen från `processRequest`.

**Tech Stack:** Java 17, Spring Boot 3.4, GWT (klientsida för i18n), Maven

---

### Task 1: i18n-nyckel + BrowseAIP

**Files:**
- Modify: `roda-ui/roda-wui/src/main/java/config/i18n/client/ClientMessages.java`
- Modify: `roda-ui/roda-wui/src/main/resources/config/i18n/client/ClientMessages.properties`
- Modify: `roda-ui/roda-wui/src/main/resources/config/i18n/client/ClientMessages_sv_SE.properties`
- Modify: `roda-ui/roda-wui/src/main/java/org/roda/wui/client/browse/BrowseAIP.java`

- [ ] **Lägg till i18n-metod i ClientMessages.java**

Lägg till efter de befintliga `catalogTree*`-metoderna (kring rad 2730):

```java
String browseAIPReasonViewAIP();
```

- [ ] **Lägg till engelsk nyckel i ClientMessages.properties**

Lägg till efter `catalogTreeGhostNodeLabel`:

```
browseAIPReasonViewAIP: View AIP
```

- [ ] **Lägg till svensk nyckel i ClientMessages_sv_SE.properties**

Lägg till efter `catalogTreeGhostNodeLabel`:

```
browseAIPReasonViewAIP: Visa AIP
```

- [ ] **Ersätt hårdkodad sträng i BrowseAIP.java**

I `BrowseAIP.refresh()` — byt ut:

```java
Services service = new Services("Retrieve AIP", "get");
```

mot:

```java
Services service = new Services(messages.browseAIPReasonViewAIP(), "get");
```

Klassen använder redan `messages` — kontrollera att `private static final ClientMessages messages = GWT.create(ClientMessages.class);` finns i klasshuvudet.

- [ ] **Kompilera och verifiera**

```bash
wsl -d Ubuntu -- bash -c "cd ~/ETERNA/.worktrees/eterna-v1-alpha-2026-05-20 && mvn install -pl roda-ui/roda-wui -am -DskipTests --no-transfer-progress 2>&1 | tail -5"
```

Förväntat: `BUILD SUCCESS`

- [ ] **Commit**

```bash
wsl -d Ubuntu -- bash -c "cd ~/ETERNA/.worktrees/eterna-v1-alpha-2026-05-20 && git add roda-ui/roda-wui/src/main/java/config/i18n/client/ClientMessages.java roda-ui/roda-wui/src/main/resources/config/i18n/client/ClientMessages.properties roda-ui/roda-wui/src/main/resources/config/i18n/client/ClientMessages_sv_SE.properties roda-ui/roda-wui/src/main/java/org/roda/wui/client/browse/BrowseAIP.java && git commit -m 'feat: i18n-nyckel browseAIPReasonViewAIP och loggningsorsak i BrowseAIP'"
```

---

### Task 2: AIPController.findByUuid

**Files:**
- Modify: `roda-ui/roda-wui/src/main/java/org/roda/wui/api/v2/controller/AIPController.java:119-135`

- [ ] **Ersätt findByUuid i AIPController.java**

Nuvarande kod:
```java
@Override
public IndexedAIP findByUuid(String uuid, String localeString) {
  IndexedAIP retrieve = indexService.retrieve(IndexedAIP.class, uuid, new ArrayList<>());

  RodaConstants.DistributedModeType distributedModeType = RodaCoreFactory.getDistributedModeType();

  if (RODAInstanceUtils.isConfiguredAsDistributedMode()
    && RodaConstants.DistributedModeType.CENTRAL.equals(distributedModeType)) {
    boolean isLocalInstance = retrieve.getInstanceId().equals(RODAInstanceUtils.getLocalInstanceIdentifier());
    aipService.retrieveDistributedInstanceName(retrieve.getInstanceId(), isLocalInstance)
      .ifPresent(retrieve::setInstanceName);
    retrieve.setLocalInstance(isLocalInstance);
  }

  return retrieve;
}
```

Ersätt med:
```java
@Override
public IndexedAIP findByUuid(String uuid, String localeString) {
  return requestHandler.processRequest(new RequestHandler.RequestProcessor<IndexedAIP>() {
    @Override
    public IndexedAIP process(RequestContext requestContext,
        RequestControllerAssistant controllerAssistant)
        throws RODAException, RESTException {
      controllerAssistant.setRelatedObjectId(uuid);
      IndexedAIP retrieve = requestContext.getIndexService().retrieve(IndexedAIP.class, uuid, new ArrayList<>());
      controllerAssistant.checkObjectPermissions(requestContext.getUser(), retrieve);

      RodaConstants.DistributedModeType distributedModeType = RodaCoreFactory.getDistributedModeType();
      if (RODAInstanceUtils.isConfiguredAsDistributedMode()
        && RodaConstants.DistributedModeType.CENTRAL.equals(distributedModeType)) {
        boolean isLocalInstance = retrieve.getInstanceId()
          .equals(RODAInstanceUtils.getLocalInstanceIdentifier());
        aipService.retrieveDistributedInstanceName(retrieve.getInstanceId(), isLocalInstance)
          .ifPresent(retrieve::setInstanceName);
        retrieve.setLocalInstance(isLocalInstance);
      }

      return retrieve;
    }
  });
}
```

`RODAException`, `RESTException`, `RequestContext` och `RequestControllerAssistant` är redan importerade i AIPController (används av `getAncestors`).

- [ ] **Kompilera**

```bash
wsl -d Ubuntu -- bash -c "cd ~/ETERNA/.worktrees/eterna-v1-alpha-2026-05-20 && mvn install -pl roda-ui/roda-wui -am -DskipTests --no-transfer-progress 2>&1 | tail -5"
```

Förväntat: `BUILD SUCCESS`

- [ ] **Commit**

```bash
wsl -d Ubuntu -- bash -c "cd ~/ETERNA/.worktrees/eterna-v1-alpha-2026-05-20 && git add roda-ui/roda-wui/src/main/java/org/roda/wui/api/v2/controller/AIPController.java && git commit -m 'feat: granskningslogg för AIPController.findByUuid'"
```

---

### Task 3: Innehållscontrollers med checkObjectPermissions

**Files:**
- Modify: `roda-ui/roda-wui/src/main/java/org/roda/wui/api/v2/controller/RepresentationController.java:79`
- Modify: `roda-ui/roda-wui/src/main/java/org/roda/wui/api/v2/controller/FilesController.java:201`
- Modify: `roda-ui/roda-wui/src/main/java/org/roda/wui/api/v2/controller/DIPController.java:73`
- Modify: `roda-ui/roda-wui/src/main/java/org/roda/wui/api/v2/controller/DIPFileController.java:48`
- Modify: `roda-ui/roda-wui/src/main/java/org/roda/wui/api/v2/controller/PreservationEventController.java:77`

- [ ] **Ersätt RepresentationController.findByUuid**

```java
@Override
public IndexedRepresentation findByUuid(String uuid, String localeString) {
  return requestHandler.processRequest(new RequestHandler.RequestProcessor<IndexedRepresentation>() {
    @Override
    public IndexedRepresentation process(RequestContext requestContext,
        RequestControllerAssistant controllerAssistant)
        throws RODAException, RESTException {
      controllerAssistant.setRelatedObjectId(uuid);
      IndexedRepresentation obj = requestContext.getIndexService()
        .retrieve(IndexedRepresentation.class, uuid, new ArrayList<>());
      controllerAssistant.checkObjectPermissions(requestContext.getUser(), obj);
      return obj;
    }
  });
}
```

- [ ] **Ersätt FilesController.findByUuid**

```java
@Override
public IndexedFile findByUuid(String uuid, String localeString) {
  return requestHandler.processRequest(new RequestHandler.RequestProcessor<IndexedFile>() {
    @Override
    public IndexedFile process(RequestContext requestContext,
        RequestControllerAssistant controllerAssistant)
        throws RODAException, RESTException {
      controllerAssistant.setRelatedObjectId(uuid);
      IndexedFile obj = requestContext.getIndexService()
        .retrieve(IndexedFile.class, uuid, new ArrayList<>());
      controllerAssistant.checkObjectPermissions(requestContext.getUser(), obj);
      return obj;
    }
  });
}
```

- [ ] **Ersätt DIPController.findByUuid**

```java
@Override
public IndexedDIP findByUuid(String uuid, String localeString) {
  return requestHandler.processRequest(new RequestHandler.RequestProcessor<IndexedDIP>() {
    @Override
    public IndexedDIP process(RequestContext requestContext,
        RequestControllerAssistant controllerAssistant)
        throws RODAException, RESTException {
      controllerAssistant.setRelatedObjectId(uuid);
      IndexedDIP obj = requestContext.getIndexService()
        .retrieve(IndexedDIP.class, uuid, new ArrayList<>());
      controllerAssistant.checkObjectPermissions(requestContext.getUser(), obj);
      return obj;
    }
  });
}
```

- [ ] **Ersätt DIPFileController.findByUuid**

```java
@Override
public DIPFile findByUuid(String uuid, String localeString) {
  return requestHandler.processRequest(new RequestHandler.RequestProcessor<DIPFile>() {
    @Override
    public DIPFile process(RequestContext requestContext,
        RequestControllerAssistant controllerAssistant)
        throws RODAException, RESTException {
      controllerAssistant.setRelatedObjectId(uuid);
      DIPFile obj = requestContext.getIndexService()
        .retrieve(DIPFile.class, uuid, new ArrayList<>());
      controllerAssistant.checkObjectPermissions(requestContext.getUser(), obj);
      return obj;
    }
  });
}
```

- [ ] **Ersätt PreservationEventController.findByUuid**

```java
@Override
public IndexedPreservationEvent findByUuid(String uuid, String localeString) {
  return requestHandler.processRequest(new RequestHandler.RequestProcessor<IndexedPreservationEvent>() {
    @Override
    public IndexedPreservationEvent process(RequestContext requestContext,
        RequestControllerAssistant controllerAssistant)
        throws RODAException, RESTException {
      controllerAssistant.setRelatedObjectId(uuid);
      IndexedPreservationEvent obj = requestContext.getIndexService()
        .retrieve(IndexedPreservationEvent.class, uuid, new ArrayList<>());
      controllerAssistant.checkObjectPermissions(requestContext.getUser(), obj);
      return obj;
    }
  });
}
```

Om kompilatorn klagar på saknade importer, lägg till (se AIPController för referens):
```java
import org.roda.core.common.exceptions.RODAException;
import org.roda.wui.api.v2.exceptions.RESTException;
import org.roda.wui.common.model.RequestContext;
import org.roda.wui.common.RequestControllerAssistant;
```

- [ ] **Kompilera**

```bash
wsl -d Ubuntu -- bash -c "cd ~/ETERNA/.worktrees/eterna-v1-alpha-2026-05-20 && mvn install -pl roda-ui/roda-wui -am -DskipTests --no-transfer-progress 2>&1 | tail -5"
```

Förväntat: `BUILD SUCCESS`

- [ ] **Commit**

```bash
wsl -d Ubuntu -- bash -c "cd ~/ETERNA/.worktrees/eterna-v1-alpha-2026-05-20 && git add roda-ui/roda-wui/src/main/java/org/roda/wui/api/v2/controller/RepresentationController.java roda-ui/roda-wui/src/main/java/org/roda/wui/api/v2/controller/FilesController.java roda-ui/roda-wui/src/main/java/org/roda/wui/api/v2/controller/DIPController.java roda-ui/roda-wui/src/main/java/org/roda/wui/api/v2/controller/DIPFileController.java roda-ui/roda-wui/src/main/java/org/roda/wui/api/v2/controller/PreservationEventController.java && git commit -m 'feat: granskningslogg för findByUuid i innehållscontrollers'"
```

---

### Task 4: Enkla systemcontrollers

**Files:**
- Modify: `roda-ui/roda-wui/src/main/java/org/roda/wui/api/v2/controller/JobsController.java:358`
- Modify: `roda-ui/roda-wui/src/main/java/org/roda/wui/api/v2/controller/JobReportController.java:48`
- Modify: `roda-ui/roda-wui/src/main/java/org/roda/wui/api/v2/controller/MembersController.java:859`
- Modify: `roda-ui/roda-wui/src/main/java/org/roda/wui/api/v2/controller/NotificationController.java:95`
- Modify: `roda-ui/roda-wui/src/main/java/org/roda/wui/api/v2/controller/TransferredResourceController.java:245`
- Modify: `roda-ui/roda-wui/src/main/java/org/roda/wui/api/v2/controller/DisposalConfirmationController.java:75`

- [ ] **Ersätt JobsController.findByUuid**

```java
@Override
public IndexedJob findByUuid(String uuid, String localeString) {
  return requestHandler.processRequest(new RequestHandler.RequestProcessor<IndexedJob>() {
    @Override
    public IndexedJob process(RequestContext requestContext,
        RequestControllerAssistant controllerAssistant)
        throws RODAException, RESTException {
      controllerAssistant.setRelatedObjectId(uuid);
      return requestContext.getIndexService().retrieve(IndexedJob.class, uuid, new ArrayList<>());
    }
  });
}
```

- [ ] **Ersätt JobReportController.findByUuid**

```java
@Override
public IndexedReport findByUuid(String uuid, String localeString) {
  return requestHandler.processRequest(new RequestHandler.RequestProcessor<IndexedReport>() {
    @Override
    public IndexedReport process(RequestContext requestContext,
        RequestControllerAssistant controllerAssistant)
        throws RODAException, RESTException {
      controllerAssistant.setRelatedObjectId(uuid);
      return requestContext.getIndexService().retrieve(IndexedReport.class, uuid, new ArrayList<>());
    }
  });
}
```

- [ ] **Ersätt MembersController.findByUuid**

```java
@Override
public RodaPrincipal findByUuid(String uuid, String localeString) {
  return requestHandler.processRequest(new RequestHandler.RequestProcessor<RodaPrincipal>() {
    @Override
    public RodaPrincipal process(RequestContext requestContext,
        RequestControllerAssistant controllerAssistant)
        throws RODAException, RESTException {
      controllerAssistant.setRelatedObjectId(uuid);
      return requestContext.getIndexService().retrieve(RodaPrincipal.class, uuid, new ArrayList<>());
    }
  });
}
```

- [ ] **Ersätt NotificationController.findByUuid**

```java
@Override
public Notification findByUuid(String uuid, String localeString) {
  return requestHandler.processRequest(new RequestHandler.RequestProcessor<Notification>() {
    @Override
    public Notification process(RequestContext requestContext,
        RequestControllerAssistant controllerAssistant)
        throws RODAException, RESTException {
      controllerAssistant.setRelatedObjectId(uuid);
      return requestContext.getIndexService().retrieve(Notification.class, uuid, new ArrayList<>());
    }
  });
}
```

- [ ] **Ersätt TransferredResourceController.findByUuid**

```java
@Override
public TransferredResource findByUuid(String uuid, String localeString) {
  return requestHandler.processRequest(new RequestHandler.RequestProcessor<TransferredResource>() {
    @Override
    public TransferredResource process(RequestContext requestContext,
        RequestControllerAssistant controllerAssistant)
        throws RODAException, RESTException {
      controllerAssistant.setRelatedObjectId(uuid);
      return requestContext.getIndexService().retrieve(TransferredResource.class, uuid, new ArrayList<>());
    }
  });
}
```

- [ ] **Ersätt DisposalConfirmationController.findByUuid**

```java
@Override
public DisposalConfirmation findByUuid(String uuid, String localeString) {
  return requestHandler.processRequest(new RequestHandler.RequestProcessor<DisposalConfirmation>() {
    @Override
    public DisposalConfirmation process(RequestContext requestContext,
        RequestControllerAssistant controllerAssistant)
        throws RODAException, RESTException {
      controllerAssistant.setRelatedObjectId(uuid);
      return requestContext.getIndexService().retrieve(DisposalConfirmation.class, uuid, new ArrayList<>());
    }
  });
}
```

- [ ] **Kompilera**

```bash
wsl -d Ubuntu -- bash -c "cd ~/ETERNA/.worktrees/eterna-v1-alpha-2026-05-20 && mvn install -pl roda-ui/roda-wui -am -DskipTests --no-transfer-progress 2>&1 | tail -5"
```

Förväntat: `BUILD SUCCESS`

- [ ] **Commit**

```bash
wsl -d Ubuntu -- bash -c "cd ~/ETERNA/.worktrees/eterna-v1-alpha-2026-05-20 && git add roda-ui/roda-wui/src/main/java/org/roda/wui/api/v2/controller/JobsController.java roda-ui/roda-wui/src/main/java/org/roda/wui/api/v2/controller/JobReportController.java roda-ui/roda-wui/src/main/java/org/roda/wui/api/v2/controller/MembersController.java roda-ui/roda-wui/src/main/java/org/roda/wui/api/v2/controller/NotificationController.java roda-ui/roda-wui/src/main/java/org/roda/wui/api/v2/controller/TransferredResourceController.java roda-ui/roda-wui/src/main/java/org/roda/wui/api/v2/controller/DisposalConfirmationController.java && git commit -m 'feat: granskningslogg för findByUuid i enkla systemcontrollers'"
```

---

### Task 5: Systemcontrollers med fieldsToReturn

**Files:**
- Modify: `roda-ui/roda-wui/src/main/java/org/roda/wui/api/v2/controller/AuditLogController.java:70`
- Modify: `roda-ui/roda-wui/src/main/java/org/roda/wui/api/v2/controller/RiskController.java:70`
- Modify: `roda-ui/roda-wui/src/main/java/org/roda/wui/api/v2/controller/RiskIncidenceController.java:63`
- Modify: `roda-ui/roda-wui/src/main/java/org/roda/wui/api/v2/controller/PreservationAgentController.java:71`

- [ ] **Ersätt AuditLogController.findByUuid**

```java
@Override
public LogEntry findByUuid(String uuid, String localeString) {
  return requestHandler.processRequest(new RequestHandler.RequestProcessor<LogEntry>() {
    @Override
    public LogEntry process(RequestContext requestContext,
        RequestControllerAssistant controllerAssistant)
        throws RODAException, RESTException {
      controllerAssistant.setRelatedObjectId(uuid);
      final List<String> fieldsToReturn = Arrays.asList(RodaConstants.INDEX_UUID, RodaConstants.LOG_ID,
        RodaConstants.LOG_ACTION_COMPONENT, RodaConstants.LOG_ACTION_METHOD, RodaConstants.LOG_ADDRESS,
        RodaConstants.LOG_DATETIME, RodaConstants.LOG_RELATED_OBJECT_ID, RodaConstants.LOG_USERNAME,
        RodaConstants.LOG_PARAMETERS, RodaConstants.LOG_STATE, RodaConstants.LOG_REQUEST_HEADER_UUID,
        RodaConstants.LOG_REQUEST_HEADER_REASON, RodaConstants.LOG_REQUEST_HEADER_TYPE);
      return requestContext.getIndexService().retrieve(LogEntry.class, uuid, fieldsToReturn);
    }
  });
}
```

- [ ] **Ersätt RiskController.findByUuid**

```java
@Override
public IndexedRisk findByUuid(String uuid, String localeString) {
  return requestHandler.processRequest(new RequestHandler.RequestProcessor<IndexedRisk>() {
    @Override
    public IndexedRisk process(RequestContext requestContext,
        RequestControllerAssistant controllerAssistant)
        throws RODAException, RESTException {
      controllerAssistant.setRelatedObjectId(uuid);
      final List<String> fieldsToReturn = Arrays.asList(RodaConstants.INDEX_UUID, RodaConstants.RISK_ID,
        RodaConstants.RISK_NAME, RodaConstants.RISK_DESCRIPTION, RodaConstants.RISK_IDENTIFIED_ON,
        RodaConstants.RISK_IDENTIFIED_BY, RodaConstants.RISK_CATEGORIES, RodaConstants.RISK_NOTES,
        RodaConstants.RISK_PRE_MITIGATION_PROBABILITY, RodaConstants.RISK_PRE_MITIGATION_IMPACT,
        RodaConstants.RISK_PRE_MITIGATION_SEVERITY, RodaConstants.RISK_POST_MITIGATION_PROBABILITY,
        RodaConstants.RISK_POST_MITIGATION_IMPACT, RodaConstants.RISK_POST_MITIGATION_SEVERITY,
        RodaConstants.RISK_PRE_MITIGATION_NOTES, RodaConstants.RISK_POST_MITIGATION_NOTES,
        RodaConstants.RISK_MITIGATION_STRATEGY, RodaConstants.RISK_MITIGATION_OWNER,
        RodaConstants.RISK_MITIGATION_OWNER_TYPE,
        RodaConstants.RISK_MITIGATION_RELATED_EVENT_IDENTIFIER_TYPE,
        RodaConstants.RISK_MITIGATION_RELATED_EVENT_IDENTIFIER_VALUE);
      return requestContext.getIndexService().retrieve(IndexedRisk.class, uuid, fieldsToReturn);
    }
  });
}
```

- [ ] **Ersätt RiskIncidenceController.findByUuid**

```java
@Override
public RiskIncidence findByUuid(String uuid, String localeString) {
  return requestHandler.processRequest(new RequestHandler.RequestProcessor<RiskIncidence>() {
    @Override
    public RiskIncidence process(RequestContext requestContext,
        RequestControllerAssistant controllerAssistant)
        throws RODAException, RESTException {
      controllerAssistant.setRelatedObjectId(uuid);
      final List<String> fieldsToReturn = Arrays.asList(RodaConstants.INDEX_UUID,
        RodaConstants.RISK_INCIDENCE_ID, RodaConstants.RISK_INCIDENCE_RISK_ID,
        RodaConstants.RISK_INCIDENCE_DESCRIPTION, RodaConstants.RISK_INCIDENCE_STATUS,
        RodaConstants.RISK_INCIDENCE_SEVERITY, RodaConstants.RISK_INCIDENCE_DETECTED_BY,
        RodaConstants.RISK_INCIDENCE_DETECTED_ON, RodaConstants.RISK_INCIDENCE_MITIGATED_ON,
        RodaConstants.RISK_INCIDENCE_MITIGATED_BY, RodaConstants.RISK_INCIDENCE_MITIGATED_DESCRIPTION);
      return requestContext.getIndexService().retrieve(RiskIncidence.class, uuid, fieldsToReturn);
    }
  });
}
```

- [ ] **Ersätt PreservationAgentController.findByUuid**

```java
@Override
public IndexedPreservationAgent findByUuid(String uuid, String localeString) {
  return requestHandler.processRequest(new RequestHandler.RequestProcessor<IndexedPreservationAgent>() {
    @Override
    public IndexedPreservationAgent process(RequestContext requestContext,
        RequestControllerAssistant controllerAssistant)
        throws RODAException, RESTException {
      controllerAssistant.setRelatedObjectId(uuid);
      final List<String> fieldsToReturn = Arrays.asList(RodaConstants.INDEX_UUID,
        RodaConstants.PRESERVATION_AGENT_ID, RodaConstants.PRESERVATION_AGENT_NAME,
        RodaConstants.PRESERVATION_AGENT_TYPE, RodaConstants.PRESERVATION_AGENT_VERSION,
        RodaConstants.PRESERVATION_AGENT_NOTE, RodaConstants.PRESERVATION_AGENT_EXTENSION);
      return requestContext.getIndexService().retrieve(IndexedPreservationAgent.class, uuid, fieldsToReturn);
    }
  });
}
```

- [ ] **Kompilera**

```bash
wsl -d Ubuntu -- bash -c "cd ~/ETERNA/.worktrees/eterna-v1-alpha-2026-05-20 && mvn install -pl roda-ui/roda-wui -am -DskipTests --no-transfer-progress 2>&1 | tail -5"
```

Förväntat: `BUILD SUCCESS`

- [ ] **Commit**

```bash
wsl -d Ubuntu -- bash -c "cd ~/ETERNA/.worktrees/eterna-v1-alpha-2026-05-20 && git add roda-ui/roda-wui/src/main/java/org/roda/wui/api/v2/controller/AuditLogController.java roda-ui/roda-wui/src/main/java/org/roda/wui/api/v2/controller/RiskController.java roda-ui/roda-wui/src/main/java/org/roda/wui/api/v2/controller/RiskIncidenceController.java roda-ui/roda-wui/src/main/java/org/roda/wui/api/v2/controller/PreservationAgentController.java && git commit -m 'feat: granskningslogg för findByUuid i fieldsToReturn-controllers'"
```

---

### Task 6: RepresentationInformationController

**Files:**
- Modify: `roda-ui/roda-wui/src/main/java/org/roda/wui/api/v2/controller/RepresentationInformationController.java:88`

- [ ] **Ersätt RepresentationInformationController.findByUuid**

```java
@Override
public RepresentationInformation findByUuid(String uuid, String localeString) {
  return requestHandler.processRequest(new RequestHandler.RequestProcessor<RepresentationInformation>() {
    @Override
    public RepresentationInformation process(RequestContext requestContext,
        RequestControllerAssistant controllerAssistant)
        throws RODAException, RESTException {
      controllerAssistant.setRelatedObjectId(uuid);
      RepresentationInformation retrieve = requestContext.getIndexService()
        .retrieve(RepresentationInformation.class, uuid, new ArrayList<>(), true);

      retrieve.setFamilyI18n(
        translationService.getTranslation(localeString,
          "ri.family." + retrieve.getFamily(), retrieve.getFamily()));

      representationInformationService.setIndexService(requestContext.getIndexService());

      return representationInformationService
        .enrichRepresentationInformationRelations(retrieve, localeString, requestContext);
    }
  });
}
```

`translationService` och `representationInformationService` är @Autowired-fält i klassen och nås direkt från lambdan.

- [ ] **Kompilera**

```bash
wsl -d Ubuntu -- bash -c "cd ~/ETERNA/.worktrees/eterna-v1-alpha-2026-05-20 && mvn install -pl roda-ui/roda-wui -am -DskipTests --no-transfer-progress 2>&1 | tail -5"
```

Förväntat: `BUILD SUCCESS`

- [ ] **Commit**

```bash
wsl -d Ubuntu -- bash -c "cd ~/ETERNA/.worktrees/eterna-v1-alpha-2026-05-20 && git add roda-ui/roda-wui/src/main/java/org/roda/wui/api/v2/controller/RepresentationInformationController.java && git commit -m 'feat: granskningslogg för RepresentationInformationController.findByUuid'"
```

---

### Task 7: Slutverifiering

- [ ] **Full Maven-kompilering**

```bash
wsl -d Ubuntu -- bash -c "cd ~/ETERNA/.worktrees/eterna-v1-alpha-2026-05-20 && mvn install -pl roda-ui/roda-wui -am -DskipTests --no-transfer-progress 2>&1 | tail -10"
```

Förväntat: `BUILD SUCCESS`

- [ ] **Verifiera att alla 17 controllers är wrappade**

```bash
wsl -d Ubuntu -- bash -c "grep -rn 'public.*findByUuid' /home/annajansson/ETERNA/.worktrees/eterna-v1-alpha-2026-05-20/roda-ui/roda-wui/src/main/java/org/roda/wui/api/v2/controller/ --include='*.java' -A2 | grep -c 'processRequest'"
```

Förväntat: `17`

- [ ] **Verifiera git log**

```bash
wsl -d Ubuntu -- bash -c "cd ~/ETERNA/.worktrees/eterna-v1-alpha-2026-05-20 && git log --oneline -10"
```

Förväntat: 6 commits från denna feature ovanpå de befintliga ghost-nod-commits.

# Svenskt språkstöd — implementationsplan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Mål:** Lägga till 26 saknade svenska översättningar i ETERNA:s i18n-filer så att inget engelskt syns i det ordinarie gränssnittet.

**Arkitektur:** Alla ändringar görs uteslutande i properties-filer. Inga Java-, GWT- eller konfigurationsfiler ändras. Varje commit innehåller en logiskt sammanhållen grupp nycklar och kan återrullas självständigt.

**Teknikstack:** Java properties-filer (ISO 8859-1-kompatibel UTF-8), GWT i18n, Spring Boot i18n.

---

## Kritisk information: git commit-metod

På grund av ett känt problem med WSL/Windows-interaktion i detta repo **måste varje commit** skapas med git plumbing-kommandon i stället för vanlig `git commit`. Annars inkluderas oönskade borttagningar av scripts/-filer.

**Använd alltid denna commit-procedur från WSL-terminalen:**

```bash
# 1. Lägg till ändrade filer
git add <sökväg/till/fil>

# 2. Skriv index som ett träd
TREE=$(git write-tree)

# 3. Skapa commit från trädet (ersätt "commit-meddelande" nedan)
COMMIT=$(git commit-tree $TREE -p HEAD -m "commit-meddelande")

# 4. Uppdatera branchen
git update-ref HEAD $COMMIT

# 5. Verifiera att bara rätt filer ändrades
git show --stat HEAD
```

---

## Filer som ändras

| Fil | Antal nycklar |
|-----|---------------|
| `roda-ui/roda-wui/src/main/resources/config/i18n/client/ClientMessages_sv_SE.properties` | 2 |
| `roda-ui/roda-wui/src/main/resources/config/i18n/ServerMessages_sv_SE.properties` | 24 |

---

## Uppgift 1: Plugin-parametrar & detaljvy-etiketter (create_job-sidan)

**Filer:**
- Ändra: `roda-ui/roda-wui/src/main/resources/config/i18n/client/ClientMessages_sv_SE.properties`

- [ ] **Steg 1: Lägg till följande nycklar i slutet av ClientMessages_sv_SE.properties**

```properties
# Plugin-parametrar (create_job-sidan)
conversionProfileTitle=Konverteringsprofil
conversionProfileDescription=Inställningar för konverteringsprofil
changeRepresentationStatusToPreservationTitle=Ändra representationsstatus till Bevarande?
changeRepresentationStatusToPreservationDescription=
disseminationTitle=Spridningstitel
disseminationTitleDescription=Detta blir spridningens titel.
disseminationTitleDefaultValue=Spridningstitel
disseminationDescriptionTitle=Spridningsbeskrivning
disseminationDescriptionDescription=Detta blir spridningens beskrivning.
disseminationDescriptionDefaultValue=Spridningsbeskrivning
disseminationFiles=Filer
representationTypeTitle=Representationstyp
representationTypeDescription=Tilldela en typ när du skapar en ny representation

# Detaljvy-etiketter
detailsAIP=AIP
detailsRepresentation=Representation
detailsFile=Fil
detailsIdentifier=Identifierare
detailsLevel=Nivå
detailsType=Typ
detailsState=Tillstånd
detailsCreatedOn=Skapad
detailsCreatedBy=Skapare
detailsModifiedOn=Ändrad
detailsModifiedBy=Ändrad av
detailsIngest=Ingest
```

- [ ] **Steg 2: Verifiera att nycklarna lades till**

```bash
grep -c "conversionProfileTitle\|detailsCreatedBy\|disseminationTitle" \
  roda-ui/roda-wui/src/main/resources/config/i18n/client/ClientMessages_sv_SE.properties
```
Förväntat utfall: `3`

- [ ] **Steg 3: Committa med plumbing-metoden**

```bash
git add roda-ui/roda-wui/src/main/resources/config/i18n/client/ClientMessages_sv_SE.properties
TREE=$(git write-tree)
COMMIT=$(git commit-tree $TREE -p HEAD -m "i18n(sv): plugin-parametrar och detaljvy-etiketter")
git update-ref HEAD $COMMIT
git show --stat HEAD
```

Förväntat utfall: 1 fil ändrad, enbart `ClientMessages_sv_SE.properties`.

---

## Uppgift 2: Anledningsmeddelanden (reason messages)

**Filer:**
- Ändra: `roda-ui/roda-wui/src/main/resources/config/i18n/client/ClientMessages_sv_SE.properties`

- [ ] **Steg 1: Lägg till följande nycklar i slutet av ClientMessages_sv_SE.properties**

```properties
# Anledningsmeddelanden
reasonNoParentObject=Det påverkade objektet saknar förälder
reasonNoObjectSelected=Inget objekt är valt
reasonUserLacksPermission=Användaren saknar behörighet att utföra den här åtgärden
reasonCantActOnSingleObject=Kan inte utföras på ett enskilt objekt
reasonCantActOnMultipleObjects=Kan inte utföras på flera objekt
reasonCantActOnFileDirectory=Kan inte utföras på mapp
reasonCantActOnFileBitstream=Kan inte utföras på normal fil
reasonNoDisposalConfirmation=Kräver en befintlig gallringsbekräftelse
reasonJobIsFinishedOrStopping=Jobbet håller på att avslutas
reasonJobNotPendingApproval=Jobbet inväntar inte godkännande
reasonJobDoesNotNeedAppraisal=Jobbet inväntar inte bedömning
reasonCantActOnUser=Kan inte utföras på användare
reasonCantActOnGroup=Kan inte utföras på grupp
reasonAIPProtectedByDisposalPolicy=Det berörda AIP:et omfattas av en gallringspolicy
reasonAIPUnderAppraisal=Ett eller flera berörda AIP är fortfarande under bedömning
reasonAffectedAIPUnderAppraisal=Ett eller flera berörda AIP är fortfarande under bedömning
reasonFilesAreOnSameRepresentation=Filer kan inte tillhöra samma representation
reasonFilesAreOnDifferentRepresentations=Filer kan inte tillhöra olika representationer
reasonDisposalConfirmationIsPending=Gallringsbekräftelsen inväntar fortfarande godkännande
reasonDisposalConfirmationIsApproved=Gallringsbekräftelsen har redan godkänts
reasonDisposalConfirmationIsRecovered=Gallringsbekräftelsen har redan återställts
reasonDisposalConfirmationIsDeleted=Gallringsbekräftelsen har tagits bort
reasonDisposalConfirmationExecutionFailed=Körningen av gallringsbekräftelsen misslyckades
reasonInvalidContext=Åtgärden kan inte utföras i det här sammanhanget
reasonDisposalConfirmationHasRecords=Gallringsbekräftelsen har kopplade poster
reasonPluginIsNotIngest=Insticksprogrammet är inte ett ingestprogram
reasonRiskHasNoHistory=Risken har ingen historik
```

- [ ] **Steg 2: Verifiera**

```bash
grep -c "^reason" \
  roda-ui/roda-wui/src/main/resources/config/i18n/client/ClientMessages_sv_SE.properties
```
Förväntat utfall: fler än de som fanns innan (de flesta reason-nycklar var redan tillagda i föregående commit, nu tillkommer dessa 27).

- [ ] **Steg 3: Committa**

```bash
git add roda-ui/roda-wui/src/main/resources/config/i18n/client/ClientMessages_sv_SE.properties
TREE=$(git write-tree)
COMMIT=$(git commit-tree $TREE -p HEAD -m "i18n(sv): anledningsmeddelanden")
git update-ref HEAD $COMMIT
git show --stat HEAD
```

---

## Uppgift 3: Gallringsrelaterade texter

**Filer:**
- Ändra: `roda-ui/roda-wui/src/main/resources/config/i18n/client/ClientMessages_sv_SE.properties`

- [ ] **Steg 1: Lägg till följande nycklar i slutet av ClientMessages_sv_SE.properties**

```properties
# Gallringsrelaterade dialoger och etiketter
deleteDisposalSchedule=Gallringsplan {0} har tagits bort
destroyDisposalConfirmationContentDialogMessage=Är du säker på att du vill gallra de logiska enheterna från den här gallringsbekräftelsen?
destroyDisposalConfirmationContentDialogTitle=Gallra logiska enheter
liftDisposalHoldDialogTitle=Häv gallringsstopp
liftDisposalHoldDialogMessage[\=1]=Är du säker på att du vill häva gallringsstoppet?
liftDisposalHoldDialogMessage=Är du säker på att du vill häva de {0, number} markerade gallringsstoppen?
disposalPolicyNoneSummary=Inte kopplad till någon gallringsbekräftelse eller gallringsstopp
oneOfAObject[org.roda.core.data.v2.disposal.confirmation.DisposalConfirmation]=gallringsbekräftelse
oneOfAObject[org.roda.core.data.v2.disposal.hold.DisposalHold]=gallringsstopp
oneOfAObject[org.roda.core.data.v2.disposal.rule.DisposalRule]=gallringsregel
oneOfAObject[org.roda.core.data.v2.disposal.schedule.DisposalSchedule]=gallringsplan
someOfAObject[org.roda.core.data.v2.disposal.confirmation.DisposalConfirmation]=gallringsbekräftelse
someOfAObject[org.roda.core.data.v2.disposal.hold.DisposalHolds]=gallringsstopp
someOfAObject[org.roda.core.data.v2.disposal.rule.DisposalRules]=gallringsregler
someOfAObject[org.roda.core.data.v2.disposal.schedule.DisposalSchedules]=gallringsplaner
searchDropdownLabels[org.roda.core.data.v2.disposal.confirmation.DisposalConfirmation]=gallringsbekräftelser
```

- [ ] **Steg 2: Verifiera att liftDisposalHoldDialogMessage lades till med rätt syntax**

```bash
grep "liftDisposalHold" \
  roda-ui/roda-wui/src/main/resources/config/i18n/client/ClientMessages_sv_SE.properties
```
Förväntat: tre rader — `liftDisposalHoldDialogTitle`, `liftDisposalHoldDialogMessage[\=1]`, `liftDisposalHoldDialogMessage`.

- [ ] **Steg 3: Committa**

```bash
git add roda-ui/roda-wui/src/main/resources/config/i18n/client/ClientMessages_sv_SE.properties
TREE=$(git write-tree)
COMMIT=$(git commit-tree $TREE -p HEAD -m "i18n(sv): gallringsrelaterade texter")
git update-ref HEAD $COMMIT
git show --stat HEAD
```

---

## Uppgift 4: Dialoger & övrig UI

**Filer:**
- Ändra: `roda-ui/roda-wui/src/main/resources/config/i18n/client/ClientMessages_sv_SE.properties`

- [ ] **Steg 1: Lägg till följande nycklar i slutet av ClientMessages_sv_SE.properties**

```properties
# Risk-dialoger
riskCreatedTitle=Risk skapad
riskCreatedMessage=Risken skapades
riskHistoryRemoveConfirmDialogTitle=Bekräfta borttagning av risk
riskHistoryRemoveConfirmDialogMessage=Är du säker på att du vill ta bort den valda riskhistoriken?
riskHistoryRevertConfirmDialogTitle=Bekräfta återställning av risk
riskHistoryRevertConfirmDialogMessage=Är du säker på att du vill återställa risken till den valda historiken?

# Historik för beskrivande metadata
descriptiveHistoryRemoveConfirmDialogTitle=Bekräfta borttagning av historik för beskrivande metadata
descriptiveHistoryRemoveConfirmDialogMessage=Är du säker på att du vill ta bort den valda historiken för beskrivande metadata?
descriptiveHistoryRevertConfirmDialogTitle=Bekräfta återställning av beskrivande metadata
descriptiveHistoryRevertConfirmDialogMessage=Är du säker på att du vill återställa den beskrivande metadatan till den valda historiken?

# Sök och navigering
searchDropdownLabels[org.roda.core.data.v2.ip.metadata.IndexedPreservationAgent]=Bevarandeagenter
searchDropdownLabels[org.roda.core.data.v2.ip.metadata.IndexedPreservationEvent]=Bevarandehändelser
catalogueTransferredResourceTitle=Överfört material
sublevels=Undernivåer
genericRangeFieldFrom=från
genericRangeFieldTo=till
representationFolders=Mappar

# Behörigheter och loggar
editArchivalPackagePermissions=Redigera behörigheter
logEntryReason=Anledning
relatedAuditLogs=Relaterade granskningsloggar
objectCreatedDateShort=Skapad
objectLastModifiedShort=Ändrad

# Mapphantering
renameFolderAlreadyExistsTitle=Kan inte byta namn på mapp
renameFolderAlreadyExistsMessage=Mappnamnet används redan

# Ingest och SIP-etiketter
ingestIdentifier=Jobb
sipIdentifier=Identifierare
sipDeleted=Borttagen
aipNotInStorageError=AIP:et finns inte i lagringen
aipStillIngestingError=AIP:et håller fortfarande på att ingestas

# Lösenord och konto
recoverLoginEmail=E-postadress
setPasswordTitle=Ange lösenord
setPasswordSubmit=ANGE
successfullyUnsubscribedTitle=Avprenumererad
successfullyUnsubscribedMessage=Avprenumereringen lyckades
```

- [ ] **Steg 2: Verifiera att alla nycklar i denna commit finns**

```bash
grep -c "^riskCreatedTitle\|^descriptiveHistory\|^setPasswordTitle\|^sublevels" \
  roda-ui/roda-wui/src/main/resources/config/i18n/client/ClientMessages_sv_SE.properties
```
Förväntat utfall: `4`

- [ ] **Steg 3: Committa**

```bash
git add roda-ui/roda-wui/src/main/resources/config/i18n/client/ClientMessages_sv_SE.properties
TREE=$(git write-tree)
COMMIT=$(git commit-tree $TREE -p HEAD -m "i18n(sv): dialoger och övrig UI")
git update-ref HEAD $COMMIT
git show --stat HEAD
```

---

## Uppgift 5: ServerMessages — listeetiketter, e-post och granskningsloggskomponenter

**Filer:**
- Ändra: `roda-ui/roda-wui/src/main/resources/config/i18n/ServerMessages_sv_SE.properties`

- [ ] **Steg 1: Lägg till följande nycklar i slutet av ServerMessages_sv_SE.properties**

```properties
# E-post
email.setpassword.subject = Ange ETERNA-lösenord

# Listeetiketter — BrowseAIP
lists.label.BrowseAIP_auditLogs.single: logg
lists.label.BrowseAIP_auditLogs.multiple: loggar
lists.label.BrowseAIP_preservationEvents.single: bevarandehändelse
lists.label.BrowseAIP_preservationEvents.multiple: bevarandehändelser
lists.label.BrowseAIP_riskIncidences.single: risk
lists.label.BrowseAIP_riskIncidences.multiple: risker

# Listeetiketter — BrowseAIPPortal
lists.label.BrowseAIPPortal_auditLogs.single: logg
lists.label.BrowseAIPPortal_auditLogs.multiple: loggar
lists.label.BrowseAIPPortal_preservationEvents.single: bevarandehändelse
lists.label.BrowseAIPPortal_preservationEvents.multiple: bevarandehändelser
lists.label.BrowseAIPPortal_riskIncidences.single: risk
lists.label.BrowseAIPPortal_riskIncidences.multiple: risker

# Listeetiketter — BrowseRepresentation
lists.label.BrowseRepresentation_preservationEvents.single: bevarandehändelse
lists.label.BrowseRepresentation_preservationEvents.multiple: bevarandehändelser
lists.label.BrowseRepresentation_riskIncidences.single: risk
lists.label.BrowseRepresentation_riskIncidences.multiple: risker

# Listeetiketter — BrowseRepresentationPortal
lists.label.BrowseRepresentationPortal_preservationEvents.single: bevarandehändelse
lists.label.BrowseRepresentationPortal_preservationEvents.multiple: bevarandehändelser
lists.label.BrowseRepresentationPortal_riskIncidences.single: risk
lists.label.BrowseRepresentationPortal_riskIncidences.multiple: risker

# Listeetiketter — BrowseFile
lists.label.BrowseFile_preservationEvents.single: bevarandehändelse
lists.label.BrowseFile_preservationEvents.multiple: bevarandehändelser
lists.label.BrowseFile_riskIncidences.single: risk
lists.label.BrowseFile_riskIncidences.multiple: risker

# Listeetiketter — BrowseFilePortal
lists.label.BrowseFilePortal_preservationEvents.single: bevarandehändelse
lists.label.BrowseFilePortal_preservationEvents.multiple: bevarandehändelser
lists.label.BrowseFilePortal_riskIncidences.single: risk
lists.label.BrowseFilePortal_riskIncidences.multiple: risker

# Granskningslogg — API-komponenter (v2)
ui.facets.LogEntry.actionComponent.org.roda.wui.api.v2.controller.AIPController = AIP:er
ui.facets.LogEntry.actionComponent.org.roda.wui.api.v2.controller.ClassificationPlanController = Klassificeringsplan
ui.facets.LogEntry.actionComponent.org.roda.wui.api.v2.controller.ConfigurationController = Konfigurationer
ui.facets.LogEntry.actionComponent.org.roda.wui.api.v2.controller.DIPPlanController = Spridningspaket
ui.facets.LogEntry.actionComponent.org.roda.wui.api.v2.controller.DisposalConfirmationController = Gallringsbekräftelser
ui.facets.LogEntry.actionComponent.org.roda.wui.api.v2.controller.DisposalHoldController = Gallringsstopp
ui.facets.LogEntry.actionComponent.org.roda.wui.api.v2.controller.DisposalRuleController = Gallringsregler
ui.facets.LogEntry.actionComponent.org.roda.wui.api.v2.controller.DisposalScheduleController = Gallringsplaner
ui.facets.LogEntry.actionComponent.org.roda.wui.api.v2.controller.DistributedInstances = Distribuerade instanser
ui.facets.LogEntry.actionComponent.org.roda.wui.api.v2.controller.FilesController = Filer
ui.facets.LogEntry.actionComponent.org.roda.wui.api.v2.controller.JobsController = Jobb
ui.facets.LogEntry.actionComponent.org.roda.wui.api.v2.controller.MembersController = Användare och grupper
ui.facets.LogEntry.actionComponent.org.roda.wui.api.v2.controller.MetricsController = Mätvärden
ui.facets.LogEntry.actionComponent.org.roda.wui.api.v2.controller.NotificationController = Notifieringar
ui.facets.LogEntry.actionComponent.org.roda.wui.api.v2.controller.PreservationAgentController = Bevarandeagenter
ui.facets.LogEntry.actionComponent.org.roda.wui.api.v2.controller.PreservationEventController = Bevarandehändelser
ui.facets.LogEntry.actionComponent.org.roda.wui.api.v2.controller.RepresentationController = Representationer
ui.facets.LogEntry.actionComponent.org.roda.wui.api.v2.controller.RepresentationInformationController = Representationsinformation
ui.facets.LogEntry.actionComponent.org.roda.wui.api.v2.controller.RiskController = Risker
ui.facets.LogEntry.actionComponent.org.roda.wui.api.v2.controller.RiskIncidenceController = Riskincidenter
ui.facets.LogEntry.actionComponent.org.roda.wui.api.v2.controller.TransferredResourceController = Överfört material
ui.facets.LogEntry.actionComponent.org.roda.wui.api.v2.services.IndexService = Index
ui.facets.LogEntry.actionComponent.org.roda.wui.api.v2.services.MembersService = Medlemmar
ui.facets.LogEntry.actionComponent.org.roda.wui.security.SecurityObserverImpl = Säkerhet
```

- [ ] **Steg 2: Verifiera att nycklarna lades till**

```bash
grep -c "^lists.label\|^ui.facets.LogEntry.actionComponent.org.roda.wui.api.v2\|^email.setpassword" \
  roda-ui/roda-wui/src/main/resources/config/i18n/ServerMessages_sv_SE.properties
```
Förväntat utfall: `24` (24 nycklar tillagda i ServerMessages)

- [ ] **Steg 3: Committa**

```bash
git add roda-ui/roda-wui/src/main/resources/config/i18n/ServerMessages_sv_SE.properties
TREE=$(git write-tree)
COMMIT=$(git commit-tree $TREE -p HEAD -m "i18n(sv): ServerMessages — listeetiketter, e-post och granskningsloggskomponenter")
git update-ref HEAD $COMMIT
git show --stat HEAD
```

---

## Verifiering av hela featuren

- [ ] **Steg 1: Kontrollera antal återstående saknade nycklar i ClientMessages**

```bash
python3 -c "
import re
def get_keys(f):
    keys=set()
    with open(f,'r',encoding='utf-8',errors='replace') as fh:
        for l in fh:
            m=re.match(r'^([^ \t=:]+)',l.rstrip())
            if m and ('=' in l or ':' in l): keys.add(m.group(1))
    return keys
base='roda-ui/roda-wui/src/main/resources/config/i18n/client'
missing=get_keys(base+'/ClientMessages.properties')-get_keys(base+'/ClientMessages_sv_SE.properties')
print('Saknade ClientMessages-nycklar kvar:',len(missing))
"
```
Förväntat utfall: `0`

- [ ] **Steg 2: Kontrollera antal återstående saknade nycklar i ServerMessages**

```bash
python3 -c "
import re
def get_keys(f):
    keys=set()
    with open(f,'r',encoding='utf-8',errors='replace') as fh:
        for l in fh:
            m=re.match(r'^([^ \t=:]+)',l.rstrip())
            if m and ('=' in l or ':' in l): keys.add(m.group(1))
    return keys
base='roda-ui/roda-wui/src/main/resources/config/i18n'
missing=get_keys(base+'/ServerMessages.properties')-get_keys(base+'/ServerMessages_sv_SE.properties')
print('Saknade ServerMessages-nycklar kvar:',len(missing))
"
```
Förväntat utfall: `0`

- [ ] **Steg 3: Pusha branchen**

```bash
git push origin feat/swedish-language-support
```

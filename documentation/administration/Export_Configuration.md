# Configuring Search Result Export Fields

ETERNA supports exporting search results to CSV via a background job. The available fields shown in the export dialog are configurable in `roda-wui.properties`, making it possible to include custom indexed fields without any code changes.

## How It Works

When a user clicks the export button on a search result list, a dialog opens with a list of fields to include in the CSV. The user selects which fields to export and clicks **Start Export**. The export runs as a background job (visible under **Internal Actions**) and the resulting CSV file is available as a job attachment once the job completes.

The fields shown in the dialog are read from `roda-wui.properties` at startup.

## Supported Lists

The export dialog is available in the following views:

| View | Configuration prefix |
|---|---|
| AIP search | `ui.export.aip` |
| Preservation jobs | `ui.export.job` |
| Preservation jobs → Processes | `ui.export.report` |
| Internal logs | `ui.export.logentry` |
| Logs | `ui.export.logentry` |

## Default Configuration

### AIP search

```properties
ui.export.aip.fields=uuid,title,level,dateInitial,dateFinal,parentId,ingestSIPIds,createdOn,updatedOn
ui.export.aip.defaultCheckedFields=uuid,title,level,dateInitial,dateFinal

ui.export.aip.fields.uuid.label=Identifier
ui.export.aip.fields.title.label=Title
ui.export.aip.fields.level.label=Description level
ui.export.aip.fields.dateInitial.label=Start date
ui.export.aip.fields.dateFinal.label=End date
ui.export.aip.fields.parentId.label=Parent ID
ui.export.aip.fields.ingestSIPIds.label=SIP ID
ui.export.aip.fields.createdOn.label=Created
ui.export.aip.fields.updatedOn.label=Updated
```

### Preservation jobs

```properties
ui.export.job.fields=id,name,username,startDate,endDate,state,priority,pluginType,plugin
ui.export.job.defaultCheckedFields=id,name,state

ui.export.job.fields.id.label=ID
ui.export.job.fields.name.label=Name
ui.export.job.fields.username.label=User
ui.export.job.fields.startDate.label=Start date
ui.export.job.fields.endDate.label=End date
ui.export.job.fields.state.label=State
ui.export.job.fields.priority.label=Priority
ui.export.job.fields.pluginType.label=Type
ui.export.job.fields.plugin.label=Plugin
```

### Processes (job reports)

```properties
ui.export.report.fields=id,jobId,jobName,sourceObjectId,sourceObjectOriginalName,outcomeObjectId,pluginState,dateCreated,plugin,pluginDetails
ui.export.report.defaultCheckedFields=id,jobId,pluginState

ui.export.report.fields.id.label=ID
ui.export.report.fields.jobId.label=Job ID
ui.export.report.fields.jobName.label=Job name
ui.export.report.fields.sourceObjectId.label=Source object ID
ui.export.report.fields.sourceObjectOriginalName.label=Source object name
ui.export.report.fields.outcomeObjectId.label=Outcome object ID
ui.export.report.fields.pluginState.label=State
ui.export.report.fields.dateCreated.label=Created
ui.export.report.fields.plugin.label=Plugin
ui.export.report.fields.pluginDetails.label=Details
```

### Logs

```properties
ui.export.logentry.fields=uuid,datetime,username,actionComponent,actionMethod,address,relatedObjectID,duration,state
ui.export.logentry.defaultCheckedFields=datetime,username,actionComponent,actionMethod

ui.export.logentry.fields.uuid.label=ID
ui.export.logentry.fields.datetime.label=Date/time
ui.export.logentry.fields.username.label=User
ui.export.logentry.fields.actionComponent.label=Component
ui.export.logentry.fields.actionMethod.label=Method
ui.export.logentry.fields.address.label=IP address
ui.export.logentry.fields.relatedObjectID.label=Related object
ui.export.logentry.fields.duration.label=Duration (ms)
ui.export.logentry.fields.state.label=State
```

## Adding a New Field

To add a field to the export dialog, two things are required:

### 1. The field must be indexed in Solr

The field must exist on the relevant index class and be populated during indexing.

### 2. Add the field to `roda-wui.properties`

Open `roda-wui.properties` (in `~/.roda/config/roda-wui.properties` for a running instance, or in `roda-ui/roda-wui/src/main/resources/config/roda-wui.properties` in the source tree) and:

**Step 1** — add the field key to the comma-separated list for the relevant view:

```properties
ui.export.job.fields=id,name,username,startDate,endDate,state,priority,pluginType,plugin,my_custom_field
```

**Step 2** — add a label for the field:

```properties
ui.export.job.fields.my_custom_field.label=My Custom Field
```

The new field will now appear in the export dialog as an unchecked option.

## Changing the Default Checked Fields

The pre-checked fields in the export dialog are controlled by `defaultCheckedFields` for each view:

```properties
ui.export.job.defaultCheckedFields=id,name,state,my_custom_field
```

## Export File Format

- Format: CSV (comma-separated by default; delimiter is read from `csv.delimiter` in `roda-core.properties`)
- Header row: field keys in the selected order
- One row per object
- The file is available as a job attachment under **Internal Actions** after the job completes

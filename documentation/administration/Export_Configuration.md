# Configuring Search Result Export Fields

ETERNA supports exporting AIP search results to CSV via a background job. The available fields shown in the export dialog are configurable in `roda-wui.properties`, making it possible to include custom indexed fields without any code changes.

## How It Works

When a user clicks the export button on a search result list, a dialog opens with a list of fields to include in the CSV. The user selects which fields to export and clicks **Start Export**. The export runs as a background job (visible under **Internal Actions**) and the resulting CSV file is available as a job attachment once the job completes.

The fields shown in the dialog are read from `roda-wui.properties` at startup.

## Default Configuration

The following fields are included by default:

```properties
###############################################################################
# Configurable AIP export (issue #160)
###############################################################################
ui.export.aip.fields=uuid,title,level,dateInitial,dateFinal,parentId,ingestSIPIds,createdOn,updatedOn

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

The first five fields (`uuid`, `title`, `level`, `dateInitial`, `dateFinal`) are pre-checked in the dialog by default. All others are unchecked.

## Adding a New Field

To add a field to the export dialog, two things are required:

### 1. The field must be indexed in Solr

The field must exist on `IndexedAIP` and be populated during indexing. Fields from EAD descriptive metadata, custom schema extensions, and RODA's built-in AIP properties are all supported as long as they are indexed.

The following built-in fields are available out of the box:

| Field key | Description |
|---|---|
| `uuid` | AIP identifier |
| `title` | Title (from descriptive metadata) |
| `level` | Description level |
| `dateInitial` | Start date |
| `dateFinal` | End date |
| `parentId` | Parent AIP identifier |
| `ingestSIPIds` | SIP identifiers (semicolon-separated if multiple) |
| `createdOn` | Creation timestamp |
| `updatedOn` | Last updated timestamp |

### 2. Add the field to `roda-wui.properties`

Open `roda-wui.properties` (in `~/.roda/config/roda-wui.properties` for a running instance, or in `roda-ui/roda-wui/src/main/resources/config/roda-wui.properties` in the source tree) and:

**Step 1** — add the field key to the comma-separated list:

```properties
ui.export.aip.fields=uuid,title,level,dateInitial,dateFinal,parentId,ingestSIPIds,createdOn,updatedOn,my_custom_field
```

**Step 2** — add a label for the field:

```properties
ui.export.aip.fields.my_custom_field.label=My Custom Field
```

**Step 3** — restart RODA (or reload the configuration if hot-reload is supported).

The new field will now appear in the export dialog as an unchecked option.

### Example: Adding a custom security level field

If your installation indexes a field called `security_level` on `IndexedAIP`, add:

```properties
ui.export.aip.fields=uuid,title,level,dateInitial,dateFinal,parentId,ingestSIPIds,createdOn,updatedOn,security_level

ui.export.aip.fields.security_level.label=Security Level
```

You also need to add a handler for the field in `SearchExportPlugin.getFieldValue()` in the backend, otherwise the field will export as an empty string. Contact a developer to add support for new custom fields in the plugin.

## Changing the Default Checked Fields

The five fields that are pre-checked in the export dialog (`uuid`, `title`, `level`, `dateInitial`, `dateFinal`) are defined in `ExportSearchDialog.java` and require a code change to modify. Contact a developer if you need to change the default selection.

## Export File Format

- Format: CSV (comma-separated by default; delimiter is read from `csv.delimiter` in `roda-core.properties`)
- Header row: field keys in the selected order
- One row per AIP
- Filename: the current search summary plus today's date, e.g. `all_2026-04-23.csv`
- The file is available as a job attachment under **Internal Actions** after the job completes

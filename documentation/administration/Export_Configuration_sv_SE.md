# Konfigurera fält för export av sökresultat

ETERNA stöder export av sökresultat till CSV via ett bakgrundsjobb. Vilka fält som visas i exportdialogen konfigureras i `roda-wui.properties`, vilket gör det möjligt att lägga till egna indexerade fält utan kodändringar.

## Hur det fungerar

När en användare klickar på exportknappen i en sökresultatlista öppnas en dialog med en lista över tillgängliga fält. Användaren väljer vilka fält som ska ingå i CSV-filen och klickar på **Starta export**. Exporten körs som ett bakgrundsjobb (synligt under **Interna åtgärder**) och den färdiga CSV-filen finns tillgänglig som en jobbilaga när jobbet är klart.

Fälten som visas i dialogen läses från `roda-wui.properties` vid uppstart.

## Vilka listor stöder export

Exportdialogen finns tillgänglig på följande ställen i systemet:

| Vy | Konfigurationsprefix |
|---|---|
| AIP-sökning | `ui.export.aip` |
| Arkivvårdsjobb | `ui.export.job` |
| Arkivvårdsjobb → Processer | `ui.export.report` |
| Interna loggar | `ui.export.logentry` |
| Loggar | `ui.export.logentry` |

## Standardkonfiguration

### AIP-sökning

```properties
ui.export.aip.fields=uuid,title,level,dateInitial,dateFinal,parentId,ingestSIPIds,createdOn,updatedOn
ui.export.aip.defaultCheckedFields=uuid,title,level,dateInitial,dateFinal

ui.export.aip.fields.uuid.label=Identifierare
ui.export.aip.fields.title.label=Titel
ui.export.aip.fields.level.label=Beskrivningsnivå
ui.export.aip.fields.dateInitial.label=Startdatum
ui.export.aip.fields.dateFinal.label=Slutdatum
ui.export.aip.fields.parentId.label=Förälder-ID
ui.export.aip.fields.ingestSIPIds.label=SIP-ID
ui.export.aip.fields.createdOn.label=Skapad
ui.export.aip.fields.updatedOn.label=Uppdaterad
```

### Arkivvårdsjobb

```properties
ui.export.job.fields=id,name,username,startDate,endDate,state,priority,pluginType,plugin
ui.export.job.defaultCheckedFields=id,name,state

ui.export.job.fields.id.label=ID
ui.export.job.fields.name.label=Namn
ui.export.job.fields.username.label=Användare
ui.export.job.fields.startDate.label=Startdatum
ui.export.job.fields.endDate.label=Slutdatum
ui.export.job.fields.state.label=Status
ui.export.job.fields.priority.label=Prioritet
ui.export.job.fields.pluginType.label=Typ
ui.export.job.fields.plugin.label=Plugin
```

### Processer (jobbrapporter)

```properties
ui.export.report.fields=id,jobId,jobName,sourceObjectId,sourceObjectOriginalName,outcomeObjectId,pluginState,dateCreated,plugin,pluginDetails
ui.export.report.defaultCheckedFields=id,jobId,pluginState

ui.export.report.fields.id.label=ID
ui.export.report.fields.jobId.label=Jobb-ID
ui.export.report.fields.jobName.label=Jobbnamn
ui.export.report.fields.sourceObjectId.label=Källobjekt-ID
ui.export.report.fields.sourceObjectOriginalName.label=Källobjektets namn
ui.export.report.fields.outcomeObjectId.label=Resultatobjekt-ID
ui.export.report.fields.pluginState.label=Status
ui.export.report.fields.dateCreated.label=Skapad
ui.export.report.fields.plugin.label=Plugin
ui.export.report.fields.pluginDetails.label=Detaljer
```

### Loggar

```properties
ui.export.logentry.fields=uuid,datetime,username,actionComponent,actionMethod,address,relatedObjectID,duration,state
ui.export.logentry.defaultCheckedFields=datetime,username,actionComponent,actionMethod

ui.export.logentry.fields.uuid.label=ID
ui.export.logentry.fields.datetime.label=Datum/tid
ui.export.logentry.fields.username.label=Användare
ui.export.logentry.fields.actionComponent.label=Komponent
ui.export.logentry.fields.actionMethod.label=Metod
ui.export.logentry.fields.address.label=IP-adress
ui.export.logentry.fields.relatedObjectID.label=Relaterat objekt
ui.export.logentry.fields.duration.label=Varaktighet (ms)
ui.export.logentry.fields.state.label=Status
```

## Lägga till ett nytt fält

För att lägga till ett fält i exportdialogen krävs två saker:

### 1. Fältet måste vara indexerat i Solr

Fältet måste finnas på den aktuella indexklassen och vara populerat vid indexering.

### 2. Lägg till fältet i `roda-wui.properties`

Öppna `roda-wui.properties` (i `~/.roda/config/roda-wui.properties` för en körande instans, eller i `roda-ui/roda-wui/src/main/resources/config/roda-wui.properties` i källkoden) och:

**Steg 1** — lägg till fältnyckeln i den kommaseparerade listan för aktuell vy:

```properties
ui.export.job.fields=id,name,username,startDate,endDate,state,priority,pluginType,plugin,mitt_eget_falt
```

**Steg 2** — lägg till en etikett för fältet:

```properties
ui.export.job.fields.mitt_eget_falt.label=Mitt eget fält
```

Det nya fältet visas nu i exportdialogen som ett oförkryssat alternativ.

## Ändra vilka fält som är förkryssade som standard

De förkryssade standardfälten styrs av `defaultCheckedFields` för respektive vy:

```properties
ui.export.job.defaultCheckedFields=id,name,state,mitt_eget_falt
```

## Exportfilens format

- Format: CSV (kommaseparerat som standard; avgränsare läses från `csv.delimiter` i `roda-core.properties`)
- Rubrikrad: fältnycklar i vald ordning
- En rad per objekt
- Filen finns tillgänglig som en jobbilaga under **Interna åtgärder** när jobbet är klart

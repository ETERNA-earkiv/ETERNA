# Konfigurera fält för export av sökresultat

ETERNA stöder export av AIP-sökresultat till CSV via ett bakgrundsjobb. Vilka fält som visas i exportdialogen konfigureras i `roda-wui.properties`, vilket gör det möjligt att lägga till egna indexerade fält utan kodändringar.

## Hur det fungerar

När en användare klickar på exportknappen i en sökresultatlista öppnas en dialog med en lista över tillgängliga fält. Användaren väljer vilka fält som ska ingå i CSV-filen och klickar på **Starta export**. Exporten körs som ett bakgrundsjobb (synligt under **Interna åtgärder**) och den färdiga CSV-filen finns tillgänglig som en jobbilagor när jobbet är klart.

Fälten som visas i dialogen läses från `roda-wui.properties` vid uppstart.

## Standardkonfiguration

Följande fält ingår som standard:

```properties
###############################################################################
# Konfigurerbar AIP-export (issue #160)
###############################################################################
ui.export.aip.fields=uuid,title,level,dateInitial,dateFinal,parentId,ingestSIPIds,createdOn,updatedOn

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

De fem första fälten (`uuid`, `title`, `level`, `dateInitial`, `dateFinal`) är förkryssade i dialogen som standard. Övriga är avkryssade.

## Lägga till ett nytt fält

För att lägga till ett fält i exportdialogen krävs två saker:

### 1. Fältet måste vara indexerat i Solr

Fältet måste finnas på `IndexedAIP` och vara populerat vid indexering. Fält från EAD-deskriptiv metadata, egna schematillägg och RODA:s inbyggda AIP-egenskaper stöds alla, så länge de indexeras.

Följande inbyggda fält är tillgängliga direkt:

| Fältnyckel | Beskrivning |
|---|---|
| `uuid` | AIP-identifierare |
| `title` | Titel (från deskriptiv metadata) |
| `level` | Beskrivningsnivå |
| `dateInitial` | Startdatum |
| `dateFinal` | Slutdatum |
| `parentId` | Förälder-AIP:s identifierare |
| `ingestSIPIds` | SIP-identifierare (semikolonseparerade vid flera) |
| `createdOn` | Skapad (tidsstämpel) |
| `updatedOn` | Senast uppdaterad (tidsstämpel) |

### 2. Lägg till fältet i `roda-wui.properties`

Öppna `roda-wui.properties` (i `~/.roda/config/roda-wui.properties` för en körande instans, eller i `roda-ui/roda-wui/src/main/resources/config/roda-wui.properties` i källkoden) och:

**Steg 1** — lägg till fältnyckeln i den kommaseparerade listan:

```properties
ui.export.aip.fields=uuid,title,level,dateInitial,dateFinal,parentId,ingestSIPIds,createdOn,updatedOn,mitt_eget_falt
```

**Steg 2** — lägg till en etikett för fältet:

```properties
ui.export.aip.fields.mitt_eget_falt.label=Mitt eget fält
```

**Steg 3** — starta om RODA (eller ladda om konfigurationen om hot-reload stöds).

Det nya fältet visas nu i exportdialogen som ett avkryssat alternativ.


## Ändra vilka fält som är förkryssade som standard

De fem fält som är förkryssade i exportdialogen som standard (`uuid`, `title`, `level`, `dateInitial`, `dateFinal`) är definierade i `ExportSearchDialog.java` och kräver en kodändring för att ändras. Kontakta en utvecklare om du behöver ändra standardvalet.

## Exportfilens format

- Format: CSV (kommaseparerat som standard; avgränsare läses från `csv.delimiter` i `roda-core.properties`)
- Rubrikrad: fältnycklar i vald ordning
- En rad per AIP
- Filnamn: aktuell söksummering plus dagens datum, t.ex. `allt_2026-04-23.csv`
- Filen finns tillgänglig som en jobbilagor under **Interna åtgärder** när jobbet är klart

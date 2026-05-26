# Konfiguration av datum- och tidvisning

ETERNA lagrar alltid tidsstämplar i UTC internt, men kan visa dem för användaren antingen i UTC eller i serverns lokala tid. Inställningen styrs via `roda-wui.properties`.

## Konfigurationsinställning

```properties
# Visa datum och tid i UTC (true) eller lokal tid (false)
ui.dateTime.format.UTC=false
```

Standardvärdet är `false` — datum och tider visas i serverns lokala tidszon. Sätt värdet till `true` för att alltid visa UTC.

Filen finns under `~/.roda/config/roda-wui.properties` på en körande instans, eller i `roda-ui/roda-wui/src/main/resources/config/roda-wui.properties` i källkoden.

## Vad inställningen påverkar

| Del av systemet | Beteende |
|-----------------|----------|
| Datum och tider i alla listor och detaljvyer | Följer inställningen |
| Genererade rapporter (t.ex. Disposal Confirmation) | Följer inställningen |
| Datum och tider i Solr-index (intern lagring) | Alltid UTC — påverkas inte |
| Schemalagda jobb (`@once`-scheman) | Alltid UTC — påverkas inte |

## Vad inställningen inte påverkar

**Intern lagring** sker alltid i UTC oavsett inställning. Inställningen styr enbart hur datum *presenteras* för användaren.

**Schemalagda engångsjobb** visar alltid UTC i sina beskrivningar. Schemaläggning är alltid UTC oavsett visningsinställning.

## Övriga datumformat

Ytterligare inställningar styr datumformatens utseende:

```properties
# Kort datumformat (används på ställen som enbart visar datum)
ui.date.format.simple = yyyy-MM-dd

# Långt datumformat (används i titlar och rubriker)
ui.date.format.title  = predef:DATE_LONG

# Datum- och tidsformat (används där både datum och tid visas)
ui.dateTime.format.simple = yyyy-MM-dd HH:mm:ss z
```

## Loggar

Systemets loggar (applikationsloggar på servern) styrs **inte** av `ui.dateTime.format.UTC`. Loggtidsstämplar följer JVM:ens standardtidszon, som i sin tur beror på servermiljöns inställningar. För att säkerställa att loggar skrivs i UTC behöver servern startas med JVM-parametern `-Duser.timezone=UTC`.

# Loggning i ETERNA – Översikt

## Innehåll

- [Hur loggning fungerar](#hur-loggning-fungerar)
- [Loggtyper och var de lagras](#loggtyper-och-var-de-lagras)
- [Mappstruktur](#mappstruktur)
- [Rotation och rensning](#rotation-och-rensning)
- [Risker](#risker)
- [Åtgärdsförslag](#åtgärdsförslag)
- [Kontrollera diskanvändning](#kontrollera-diskanvändning)

---

## Hur loggning fungerar

Systemet använder tre separata loggmekanismer:

| Mekanism | Syfte |
|----------|-------|
| **Logback** | Applikationsloggar – tekniska händelser, fel, debug-info |
| **Action log (audit)** | Användaraktivitet och systemhändelser för revision |
| **Job reports** | Resultat och status från körda processer/jobb |

Konfigurationsfiler för Logback:
- Kärna: `roda-core/roda-core/src/main/resources/config/logback.xml`
- WUI: `roda-ui/roda-wui/src/main/resources/config/logback_wui.xml`

---

## Loggtyper och var de lagras

### 1. Applikationsloggar (`log/`)

Tekniska loggar från applikationen, skrivna av Logback.

| Fil | Innehåll |
|-----|----------|
| `roda-core.log` | Kärnsystemets händelser, fel, debug |
| `roda-core-pekko.log` | Pekko actor-system (intern meddelandehantering) |
| `roda-wui.log` | Webb-UI:ns händelser och API-anrop |

- Rullar dagligen samt vid 1 GB
- Arkiveras som komprimerade filer med namnmönster:
  - `roda-core-YYYY-MM-DD.N.log.gz`
  - `roda-core-pekko-YYYY-MM-DD.N.log.gz`
  - `roda-wui-YYYY-MM-DD.N.log.gz`
- **Ingen maximal historik konfigurerad** – gamla filer raderas aldrig automatiskt

---

### 2. Audit-loggar / Action logs (`data/log/` och `data/storage/action-log/`)

Spårning av användaråtgärder och systemhändelser för revision. Lagras på **två ställen parallellt**:

| Plats | Format | Syfte |
|-------|--------|-------|
| `data/log/` | JSON-filer organiserade per datum | Primär fillagring |
| `data/storage/action-log/` | Storage-container (hanterad av systemet) | Transaktionell lagring |
| Solr-index `ActionLog` | Sökbart index | Sökning och filtrering i UI |

Varje audit-post innehåller: tidsstämpel, användare, åtgärd, objekt, resultat.

---

### 3. Job-rapporter (`data/storage/job-report/`)

Resultat från alla körda jobb och processer (ingest, konvertering, validering m.m.).

- Lagras som JSON-filer: `job-report/{jobId}/{reportId}.json`
- Sparas för varje enskilt jobb som körs i systemet
- Sökbart via Solr-index `JobReport`

---

### 4. Report-output (`data/reports/`)

Exporterade rapporter genererade av användare eller automatiska processer.

- Statiska filer (PDF, CSV, o.d.) utan strukturerad organisation
- Ingen koppling till något index

---

## Mappstruktur

```
${RODA_HOME}/
├── log/                            ← Applikationsloggar (Logback)
│   ├── roda-core.log
│   ├── roda-core-pekko.log
│   ├── roda-wui.log
│   └── roda-core-YYYY-MM-DD.N.log.gz   ← Komprimerade arkiv (ackumuleras)
└── data/
    ├── log/                        ← Audit-loggar (JSON per datum)
    ├── reports/                    ← Exporterade rapporter
    └── storage/
        ├── action-log/             ← Audit storage-container
        └── job-report/             ← Job-rapporter (JSON per jobb)
```


## Rotation och rensning

| Loggtyp | Rotation | Automatisk rensning |
|---------|----------|-------------------|
| Applikationsloggar (`log/`) | Daglig + vid 1 GB | **Nej** – ackumuleras för evigt |
| Audit-loggar (filer) | Per datum | **Nej** – filer tas aldrig bort |
| Audit-loggar (Solr-index) | — | **Delvis** – via `ActionLogCleanerPlugin` (manuellt) |
| Job-rapporter | — | **Nej** – inga rensningsregler |
| Report-output | — | **Nej** – inga rensningsregler |

### Granskningsloggsrensare

Det finns ett inbyggt arkivvårdsjobb för att rensa audit-loggarnas Solr-index:

- **Arkivvårdsjobb:** Granskningsloggsrensare (`ActionLogCleanerPlugin`)
- **Parameter:** Radera äldre än X dagar (standard: 90 dagar)
- **Effekt:** Raderar poster från Solr-index – **inte** de fysiska filerna
- **Aktivering:** Måste köras manuellt som ett arkivvårdsjobb i systemet

# Simple Portal

Sökportal för arkivobjekt (AIP) via RODA/ETERNA V2 API.

Byggd med **Astro 6**, **React 19** och **ren CSS**. Digi Design System (Arbetsförmedlingens designsystem) används för formulärkomponenter och UI-feedback. Ingen separat backend krävs — all konfiguration sparas i en enda `config.json`.

---

## Snabbstart

```bash
# 1. Installera beroenden
bun install

# 2. Konfigurera miljövariabler
cp .env.example .env
# Redigera .env med rätt RODA-credentials och URL

# 3. Starta dev-server
bun dev
```

Portalen startar på `http://localhost:4321`.

### Miljövariabler

| Variabel | Beskrivning | Default |
|---|---|---|
| `PORTAL_SERVICE_USER` | RODA-användare för anonym sökning (obligatorisk) | — |
| `PORTAL_SERVICE_PASSWORD` | Lösenord (stödjer åäö/UTF-8) (obligatorisk) | — |
| `RODA_API_URL` | RODA/ETERNA backend-URL | `http://localhost:8080` |

Credentials lagras **aldrig** i kod eller config.json — enbart i `.env` (gitignored).

---

## Kommandon

| Kommando | Beskrivning |
|---|---|
| `bun dev` | Starta dev-server (port 4321) |
| `bun run build` | Bygg för produktion |
| `bun run preview` | Förhandsgranska produktionsbygge |
| `bun run check` | Typkontroll (Astro + TypeScript) |
| `bun run test` | Kör testsvit (Vitest) |

---

## Funktioner

### Sökning (utan inloggning)
- Fulltextsökning mot RODA V2 API via portal service account
- Besökare behöver **inte** logga in — portalen autentiserar server-side
- Expanderbara resultatkort med metadata och filer inline (lazy-loaded)
- Avancerade filter: beskrivningsnivå, typ, datum, fritext
- Filgrid med thumbnails, hover-overlay (förhandsgranska / ladda ner)
- Filvisare med stöd för bild, PDF, text, video, audio (lazy-loaded)
- Navigering mellan filer (piltangenter + pilar)
- Paginering
- Sökfält som inte finns i Solr-indexet hanteras gracefully (0 träffar, inte krasch)

### Paketnedladdning
- "Ladda ner komplett paket (.zip)" skapar en ZIP med:
  - `{titel}-metadata.pdf` — alla metadatafält i snygg 2-kolumns PDF (prefix undviker namnkrock med filer i paketet)
  - Alla filer från representationerna (med bevarad mappstruktur)
- Metadata filtreras enligt admin-konfigurerade synlighetsfält
- Fallback: `{titel}-metadata.html` om PDF-generering misslyckas
- Partiella filfel: ZIP laddas ner med `_misslyckade_filer.txt` + varning (inte krasch)
- jsPDF + JSZip lazy-loaded — laddas först vid nedladdning

### Inloggning (för admin)
- Basic Auth mot RODA (stödjer UTF-8 i lösenord)
- JSESSIONID-sessionscookie
- Automatisk redirect vid session-expired (enbart admin-sidor)

### Admin — Metadata och synlighet (`/admin/metadata`)
- Välj tillåtna beskrivningsnivåer (svenska labels: Arkivbestånd, Volym, etc.)
- Konfigurera sökfilter (aktivera/inaktivera, namnge, ordna, typ)
- Synliga metadatafält per standard (EAD 2002, EAD 3, Dublin Core)
  - Hämta fält från referens-AIP:er — nya fält plusas ihop
  - Alla kända fält sparas för framtida val (`knownMetadataFields`)
- XPath-regeleditor för metadata-baserad synlighet
  - Tre synlighetsnivåer: visa allt / enbart titel / dölj helt
  - Testa regler mot ETERNA i realtid

### Admin — Tema (`/admin/tema`)
- Byt portalnamn (visas i header, footer, sidtitel)
- Ladda upp logotyp
- Välj varumärkesfärg (10 WCAG AA-godkända presets)
- Förhandsvisning med knapp + länk + badge

### Admin — Konfiguration (`/admin/konfiguration`)
- Exportera/importera config.json

---

## Säkerhet

- **Proxy-härdning:** Anonyma requests (service account) begränsas till GET + vitlistade POST `/find`-endpoints. PUT/PATCH/DELETE kräver inloggning.
- **Middleware fail-closed:** Om RODA inte svarar returneras 503 (inte open access till admin).
- **XSS-skydd:** HTML från RODA saneras med DOMPurify innan rendering.
- **Request timeout:** 30s timeout på alla API-anrop via AbortController.
- **Användarvänliga felmeddelanden:** Alla HTTP-fel mappas till svenska meddelanden — inga råa statuskoder eller JSON i UI.

---

## Tester

Testsvit med **Vitest** (38 tester):

- **Proxy:** Auth mode detection, metod-begränsning för service account, vitlistade endpoints
- **Middleware:** Fail-closed beteende, session-validering, redirect vid expired
- **Sök-store:** Offset-reset vid ny query/filter, paginering, state management

```bash
bun run test
```

---

## Arkitektur

```text
Browser
  │
  ├── Publika sidor (sök, filvisning)
  │     └── /api/v2/* → Astro catch-all proxy → RODA
  │           Ingen JSESSIONID? → inject service account (server-side)
  │
  └── Admin-sidor (/admin/*)
        └── Kräver inloggning (RODA-konto)
              Har JSESSIONID → forward user session
```

### Autentisering — tre nivåer

| Besökare | Hur | Session |
|---|---|---|
| Anonym (sök) | Portal service account | Server-side, exponeras aldrig |
| Inloggad admin | Eget RODA-konto via login | Browser JSESSIONID |
| Login-försök | Basic Auth header | Forward direkt till RODA |

### En enda konfigurationskälla

All konfiguration (tema, synlighet, sökfält) sparas i `public/assets/config/config.json`.

- **Läsning:** Astro läser server-side. React hämtar via centraliserad `loadConfig()` (cachad).
- **Skrivning:** Admin sparar via `PUT /api/config` (kräver RODA-session) + `invalidateConfigCache()`.
- **Resultat:** Alla besökare ser samma tema och synlighetsregler.

### API-proxy

| API | Mål | Syfte |
|---|---|---|
| `/api/v2/*` | Astro catch-all → RODA | Sök, metadata, filer (med auth-injection) |
| `/api/config` | Astro API-endpoint | Läs/skriv config.json |

---

## Digi Design System

Portalen använder [Arbetsförmedlingens Digi designsystem](https://designsystem.arbetsformedlingen.se/) (`@designsystem-se/af-react` v35.2.0) för formulärkomponenter.

### Används via Portal UI-wrappers (`src/components/portal-ui/`)
- `PortalButton` — knappar (primary, secondary, function)
- `PortalInput` — textfält, lösenord, datum, e-post
- `PortalSelect` — dropdown-menyer
- `PortalCheckbox` — kryssrutor
- `PortalAccordion` — expanderbara sektioner (metadata-standarder)
- `PortalAlert` — felmeddelanden, varningar, status
- `PortalSpinner` — laddningsindikatorer
- `PortalPagination` — sidnavigering
- `PortalLink` — länkar
- `PortalErrorMessage`, `PortalEmptyState` — feedback

### Digi CSS
- Laddas via `<link>` i `<head>` (**inte** via frontmatter-import — kraschar Vite HMR)
- Sätter `--digi--*` tokens som Shadow DOM-komponenterna kräver
- Portalens `--brand1` mappas till `--digi--natthimmel--*` för brand color

### Native HTML där Digi inte passar
- Sökfältet (custom layout med ikon + clear)
- Inline tabell-inputs i SearchFieldConfig (Shadow DOM-labels spränger kompakt grid)
- Chip-editor i XpathRuleEditor
- File-inputs (hidden, triggas via PortalButton)

---

## i18n — Svenska labels

Portalen har inbyggda svenska översättningar för:
- Beskrivningsnivåer: fonds → Arkivbestånd, file → Volym, item → Handling, etc.
- Sökfält: Title → Titel, Description → Beskrivning, Level → Beskrivningsnivå
- Metadata-standarder: ead_2002.xml → Encoded Archival Description 2002

ETERNA kan overrida via `i18n.level.*` i shared-properties. Svenska inbyggda labels har prioritet över engelska remote-labels.

---

## Projektstruktur

```text
src/
  layouts/
    PortalLayout.astro         Huvudlayout (config server-side, Digi CSS via <link>)
    AdminLayout.astro          Admin med sidebar

  pages/
    index.astro                / → redirect till /sok
    sok.astro                  Söksida
    logga-in.astro             Inloggning
    admin/index.astro          → redirect till /admin/metadata
    admin/metadata.astro       Metadata + synlighet + sökfält
    admin/tema.astro           Tema (namn, logga, färg)
    admin/konfiguration.astro  Export/import config
    api/config.ts              GET/PUT config.json
    api/v2/[...path].ts        Catch-all proxy till RODA (auth-injection)

  components/
    portal-ui/                 Digi-wrappers (PortalButton, PortalInput, etc.)
    layout/                    Header, Footer, AdminSidebar (Astro)
    search/                    SearchPage, SearchResultCard, FileGrid, FileViewer
    admin/                     ThemeSettings, MetadataConfig, LevelSelector, etc.
    login/                     LoginForm

  lib/
    server/                    Server-only (env.ts, service-session.ts)
    api/                       Fetch-klient + API-funktioner
    stores/                    Nanostores (search, config, user)
    theme/                     Tema-logik
    types/                     TypeScript-typer (RODA V2 API)
    utils/                     metadata-parser, xpath, i18n, package

  styles/
    global.css                 Reset, fokus, skip-link, a11y
    tokens.css                 CSS custom properties (--portal-* + Digi overrides)

  middleware.ts                Auth guard för /admin

public/
  assets/config/config.json    All konfiguration
  digi/                        Digi CSS + fonts
```

---

## Styling

- **Ren CSS** — ingen Tailwind, ingen SCSS, inget CSS-in-JS
- **CSS custom properties** i `tokens.css` (prefixade `--portal-*`)
- **`--brand1`** styr alla accentfärger + mappas till Digi-tokens
- **Digi Shadow DOM** — komponenterna stylar sig själva, extern CSS påverkar dem inte
- **WCAG 2.1 AA** — alla färgpresets har >=4.5:1 kontrast

---

## Tillgänglighet (WCAG 2.1 AA)

- Skip-link ("Hoppa till huvudinnehåll")
- Landmarks: `banner`, `main`, `contentinfo`, `nav` med `aria-label`
- Rubrikhierarki utan hopp (h1 -> h2 -> h3)
- Alla inputs har labels, alla knappar har `aria-label` vid behov
- `focus-visible` med 3px outline
- `prefers-reduced-motion: reduce`
- Felmeddelanden med `role="alert"`, status med `role="status"` + `aria-live`
- Touch targets >= 44x44px

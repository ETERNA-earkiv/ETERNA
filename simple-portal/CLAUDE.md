# Simple Portal — Projektkontext för AI-assistenter

## Vad detta är

Enkel sökportal för arkivobjekt (AIP) via RODA/ETERNA V2 API.
Byggd med Astro 6 + React 19 + ren CSS. Pakethanterare: Bun.

## Arkitektur

```
Astro (SSR, routing, layout) → läser config.json server-side
  └── React islands (client:only="react") → söksida, admin, login
```

- **Ingen separat backend** — all config i `public/assets/config/config.json`
- **API-proxy:** `/api/v2/*` → ETERNA på `localhost:8080`
- **Config API:** `GET/PUT /api/config` (Astro API-endpoint som läser/skriver config.json)

## Viktiga regler

### Digi CSS
- Importera ALDRIG `digi-arbetsformedlingen.css` via Astro frontmatter-import — det kraschar Vite HMR
- Ladda istället via `<link rel="stylesheet" href="/digi/digi-arbetsformedlingen.css" />` i `<head>`
- CSS:en sätter `--digi--*` tokens som Shadow DOM-komponenterna KRÄVER för att rendera korrekt
- Fonter laddas via `<link>` från `public/digi/fonts/`
- Digi events: `e.detail` innehåller native DOM-event, hämta värde via `e.detail.target.value`

### React islands
- Använd alltid `client:only="react"` — ALDRIG `client:load`
- Digi Web Components kraschar vid SSR hydration (Shadow DOM mismatch)
- Alla interaktiva komponenter är ren HTML/CSS, inte Digi-wrappers

### CSS
- Ren CSS — ingen Tailwind, ingen SCSS, inget CSS-in-JS
- Inputs: `border-radius: 0`, `border: 1px solid #333` (Digi-stil)
- Knappar: `border-radius: 6px`
- Kort/sektioner: `border-radius: 0`
- `--brand1` styr alla accentfärger
- Portalens tokens prefixas `--portal-*` (undviker kollision med Digi)

### WCAG 2.1 AA
Bygg in från start i varje komponent:
- Labels på alla inputs
- `aria-label` på knappar utan text
- `focus-visible` med 3px outline
- `role="alert"` på fel, `role="status"` + `aria-live` på status
- `prefers-reduced-motion: reduce`
- Touch targets ≥44×44px

### Config
- En enda källa: `public/assets/config/config.json`
- Ingen localStorage för delad konfiguration
- PUT /api/config kräver RODA-session (JSESSIONID)

### Dev-server
- Port 4321, `strictPort: true`
- Starta aldrig två instanser
- `hmr: false` i astro.config.mjs

## Kommandon

```bash
bun install        # Installera beroenden
bun dev            # Starta dev-server
bun run build      # Bygg för produktion
bun run check      # Typkontroll
```

## Nyckelvägar

| Sökväg | Syfte |
|---|---|
| `src/layouts/PortalLayout.astro` | Huvudlayout — läser config.json server-side |
| `src/pages/api/config.ts` | Config API (GET/PUT) |
| `src/components/search/` | Söksida, kort, filgrid, filvisare |
| `src/components/admin/` | Admin-inställningar |
| `src/lib/api/` | Fetch-klient + API-funktioner |
| `src/lib/utils/metadata-parser.ts` | XML → fält (EAD, Dublin Core) |
| `src/lib/theme/theme.ts` | Tema-logik |
| `src/lib/utils/i18n.ts` | Svenska labels + ETERNA overlay |
| `public/assets/config/config.json` | All konfiguration |

## Vanliga misstag att undvika

1. Importera Digi CSS via frontmatter
2. Använda `client:load` istället för `client:only="react"`
3. Spara config i localStorage istället för config.json
4. Nästla `<button>` inuti `<button>`
5. Glömma WCAG (labels, aria, fokus)
6. Hårdkoda färger istället för `var(--brand1)`
7. Använda `document.querySelectorAll('[data-site-name]')` istället för `document.body.querySelectorAll`

# ETERNA visuell facelift — design

**Datum:** 2026-04-22
**Status:** Design godkänd av Oscar, inväntar implementationsplan
**Branch-mål:** `eterna-v1-alpha`
**Ambition:** Maximal facelift — ETERNA ska kännas som en egen produkt, inte som "en RODA-fork"

## 1. Syfte och scope

ETERNA är en fork av RODA v5. Målet med den här faceliften är att ge ETERNA ett visuellt uttryck som tydligt skiljer den från RODA, förankrat i WhiteReds grafiska profil — utan att byta ut GWT eller bygga om användargränssnittet i grunden.

Ändringarna omfattar fyra kategorier:

1. **CSS-tema** (`theme.css` m.fl.) — ny palett, typografi, komponenter
2. **Statiska HTML-mallar** (`Welcome`, `Banner`, `Footer`, error-sidor) — fullt ersättbara
3. **Texter** (`ClientMessages_sv_SE.properties`, `ServerMessages_sv_SE.properties`) — vokabulärbyte till svensk arkivton
4. **Brand-assets** (logotyper, favicon, webfonts) — läggs till / ersätts

Plus mindre ingrepp i två GWT-komponenter: login-sidan (struktur för split-screen) och top-shell (header-lockup).

## 2. Designprinciper

1. **Kontinuitet med WhiteReds grafiska profil.** Palett, logotyp och signalflaggan återanvänds direkt från `Grafisk_manual_20191115.pdf` (2019-11-15).
2. **Skärmoptimerad typografi.** WhiteReds primärtypsnitt Neue Helvetica byts ut mot Barlow Condensed + Inter — optimerade för skärm, fria att bundla (OFL).
3. **Svenskt arkivspråk.** Allt UI är på svenska. Vokabulär hämtas från svensk arkivtradition (Leverans, Arkivpaket, Bevarande) snarare än direktöversatt RODA-terminologi.
4. **WCAG 2.1 AA som minimum.** Alla text-/bakgrundskombinationer klarar 4.5:1 (eller 3:1 för stor text). Logotypgrå #9C9E9F används aldrig som textfärg.
5. **Minimal GWT-påverkan.** Så mycket som möjligt görs via CSS, statiska HTML-filer och `.properties`. GWT-koden rörs endast där struktur tvingar oss.

## 3. Varumärkesgrund

### 3.1 Färgpalett

| Roll | Namn | Hex | Kontrast mot vitt | Användning |
|------|------|-----|---------------------|------------|
| Primär accent | ETERNA Red | `#E2001A` | 4.62:1 | Topp-list, CTA-knappar, länkar, aktiv state |
| Hover-röd | Red Deep | `#B8001A` | 5.54:1 | Hover på röda knappar |
| Brödtext | Ink | `#1F2937` | 14.68:1 | Brödtext, rubriker på ljus bakgrund |
| Sekundär text | Slate | `#4B5563` | 7.56:1 | Labels, metadata |
| Svag text | Muted | `#6B7280` | 5.73:1 | Hjälptext, placeholders |
| Logotyp-grå | Brand Gray | `#9C9E9F` | 2.50:1 | **Endast** logotyp/dekor. Får EJ vara textfärg. |
| Ram | Border | `#E5E7EB` | — | Skiljelinjer, input-ramar |
| Yta | Surface | `#F9FAFB` | — | Kort-bakgrund, panel |
| Paper | White | `#FFFFFF` | — | Huvudbakgrund |
| Mörk accent | Dark | `#111827` | — | Brand-side på login, "Arkivet i siffror"-kort |
| Red Soft | Red Soft | `#FEE2E5` | — | Aktiv nav-flik, ikon-bakgrund |
| Funktionell grön | OK | `#059669` | 4.54:1 | Success-indikatorer |
| Funktionell orange | Warn | `#D97706` | 4.52:1 | Varningar |

### 3.2 Typografi

- **Display:** Barlow Condensed — weights 500, 600, 700. Rubriker, hero-text, wordmarks, section-headers.
- **Body/UI:** Inter — weights 400, 500, 600, 700. Allt annat.
- **Fallback:** Arial Narrow / system-sans (för Barlow), -apple-system / Segoe UI (för Inter).

**Motivering:** Barlow Condensed har samma kondenserade geometri som HelveticaNeueLT MdCn i grafiska profilen, men är OFL-licensierad och fungerar som web font. Inter är standard för UI-text och klarar WCAG i alla vikter.

Båda typsnitten bundlas i repot som woff2 (av hänsyn till air-gapped-installationer), inte lästa från Google Fonts.

### 3.3 Signaturelement

1. **Röd topp-list 6 px** — på varje sida, direkt arv från grafiska profilens horisontella röda band.
2. **Signalflaggan** (från `eterna-logo.svg`) — som favicon, som inline-accent bredvid ETERNA-wordmark, som dekor i tomma vyer.
3. **Cirkel-dekoration** — stor transparent röd cirkel-kontur i mörka brand-ytor (login brand-side, "Arkivet i siffror"-kort).
4. **Vertikal röd indikator (3 px)** till vänster om section-headers.

### 3.4 Ton — svensk arkivton (RODA → ETERNA)

| RODA | ETERNA |
|------|--------|
| Dashboard | Översikt |
| Ingest | Leverans |
| Submit / Upload | Lämna in / Leverera |
| AIP / Intellectual Entity | Arkivpaket |
| Welcome to RODA | Välkommen till ETERNA |
| Manage users | Användare och behörigheter |
| Preservation events | Bevarandehändelser |
| Disposal schedules | Gallringsplaner |
| Catalogue | Katalog |
| Representations | Representationer |
| Search | Sök |
| Logout | Logga ut |

Tekniska termer som är standardiserade (METS, PREMIS, SIP, AIP, DIP, OAIS, E-ARK, FGS) behålls som de är.

### 3.5 Logotyp-varianter

- `eterna-logo.svg` — **befintlig**, ljusa bakgrunder (ETERNA-text i grått #9C9E9F). Behålls oförändrad.
- `eterna-logo-reversed.svg` — **ny**, mörka bakgrunder (ETERNA-text i vitt, flagga oförändrad).
- `eterna-flag.svg` — **ny**, endast signalflaggan (94×94 viewBox). För favicon, inline-accent, dekor.

## 4. Sidodesign

### 4.1 Login-sida (före inloggning)

**Struktur:** split-screen 50/50 med röd topp-list 6 px ovan.

**Vänsterhalva — brand-side (mörk, `#111827`):**
- Reversed ETERNA-logotyp med signalflaggan (62 px wordmark)
- Byline: "ett e-arkiv från WhiteRed" (uppercase, letter-spaced)
- Tagline: "Digitalt bevarande som **står sig** över tid." (Barlow Condensed 500, 34 px, "står sig" i rött)
- Kort lead-paragraf om öppen källkod + standarder
- Standards-rad längst ner: "OAIS · E-ARK · FGS · PREMIS · METS"
- Subtil röd cirkel-kontur i hörnet som dekoration
- Footer-rad: version + WhiteRed-attribution

**Högerhalva — form-side (vit):**
- Rubrik "Logga in" (Barlow Condensed 600, 36 px)
- Underrubrik: "Välkommen tillbaka till arkivet."
- Användarnamn + lösenord (Inter, etikett 13 px)
- Primär-knapp "LOGGA IN" (röd, uppercase, letter-spaced, full bredd)
- Hjälptext: "Har du glömt lösenordet? [Återställ här]"

### 4.2 Startsida (efter inloggning)

**Struktur:** traditionell app-shell — topp-shell → hero → tvåkolumns main → footer. Röd topp-list 6 px ovan.

**Top app shell:**
- Signalflaggan (24 px) + ETERNA-wordmark (Barlow Condensed 700, 22 px)
- Huvudnav: Översikt · Katalog · Leverans · Bevarande · Administration (aktiv flik i red-soft `#FEE2E5`)
- Notisikon med röd prick (om olästa)
- User chip (avatar-initial + namn)

**Hero-sektion (vit med subtil gradient till surface):**
- "God eftermiddag" (uppercase label, muted)
- "Välkommen tillbaka, {förnamn}" (Barlow Condensed 600, 44 px)
- Kort status-sammanfattning i brödtext (max 56 ch)
- Global sökruta: scope-selector ("I arkivet ▾") + input + röd "Sök"-knapp

**Main, vänster kolumn (2fr):**
- Section-header "Snabbåtgärder" (med vertikal röd indikator)
- 4 åtgärdskort (2×2 grid): Lämna in material · Granska leveranser · Sök i katalogen · Administrera åtkomst
  - Varje kort: röd-soft ikonruta + rubrik (Barlow Condensed) + kort beskrivning + metadata-rad med aktuell siffra
- Section-header "Senaste händelser"
- Aktivitetsström: rad per händelse med statusfärg-prick (grön/orange/röd), rubrik, beskrivning, tidsstämpel

**Main, höger kolumn (1fr):**
- Kort "Arkivet i siffror" (mörk `#111827`, röd cirkel-dekor):
  - Arkivpaket totalt, Levererat denna månad (röd accent), Total lagring, Pågående bevarandejobb
- Kort "Arkivet uppfyller":
  - Kort beskrivning
  - Standard-chips: OAIS ISO 14721, E-ARK CSIP 2.2, FGS 2.1, PREMIS 3.0, METS 1.12, EAD 3

**Footer:**
- Vänster: "ETERNA v1.0-alpha · © WhiteRed · [Dokumentation]"
- Höger: systemstatus-indikator ("Alla tjänster fungerar normalt" med grön prick)

## 5. Implementationsyter

### 5.1 Helt utan GWT (~70%)

**CSS:**
| Fil | Ändring |
|-----|---------|
| `roda-ui/roda-wui/src/main/resources/config/theme/theme.css` | Skriv om core-sektioner: CSS-variabler för palett, typografi, knappar, inputs, navigation, kort, tabeller, kort-komponenter |
| `roda-ui/roda-wui/src/main/resources/config/theme/fonts/fonts.css` | Lägg till `@font-face`-regler för Inter + Barlow Condensed |
| `roda-ui/roda-wui/src/main/resources/config/theme/spinner.css` | Justera spinner-färg till ETERNA Red |

**Nya webfont-filer:**
- `config/theme/fonts/inter/Inter-Regular.woff2`
- `config/theme/fonts/inter/Inter-Medium.woff2`
- `config/theme/fonts/inter/Inter-SemiBold.woff2`
- `config/theme/fonts/inter/Inter-Bold.woff2`
- `config/theme/fonts/barlow-condensed/BarlowCondensed-Medium.woff2`
- `config/theme/fonts/barlow-condensed/BarlowCondensed-SemiBold.woff2`
- `config/theme/fonts/barlow-condensed/BarlowCondensed-Bold.woff2`

**Statiska HTML-mallar (full ersättning):**
- `config/theme/Welcome_sv_SE.html` — ny startsida enligt avsnitt 4.2
- `config/theme/Welcome.html` — engelsk fallback (samma struktur, engelska texter där det finns, svenska kvar för produkt-specifika termer)
- `config/theme/Banner_sv_SE.html` — ny lockup (flagga + ETERNA, används i top-shell)
- `config/theme/Banner.html` — samma som sv
- `config/theme/Footer_sv_SE.html` — ny footer
- `config/theme/Error401_sv_SE.html`, `Error404_sv_SE.html`, `Error500_sv_SE.html` — restyled med brand-uttryck
- `config/theme/static/Main.html` — justera head/meta om det behövs (webfont preload)

**Texter — `.properties`:**
- `config/i18n/client/ClientMessages_sv_SE.properties` — ersätt UI-strängar enligt vokabulärtabellen i 3.4
- `config/i18n/ServerMessages_sv_SE.properties` — ersätt server-side meddelanden som är synliga i UI

**Brand-assets:**
- `config/theme/eterna-logo-reversed.svg` — ny (ETERNA-text i vitt, flagga oförändrad)
- `config/theme/eterna-flag.svg` — ny (endast signalflaggan, 94×94 viewBox)
- `config/theme/favicon/favicon-16x16.png` — ny (genererad från signalflaggan)
- `config/theme/favicon/favicon-32x32.png` — ny (genererad från signalflaggan)
- `config/theme/favicon/favicon.ico` — ny (multi-res från signalflaggan)
- `config/theme/favicon.ico`, `config/theme/favicon.png` — samma som ovan

### 5.2 Med lätt GWT-touch (~25%)

**Login-sidan (split-screen kräver strukturändring):**
- Identifiera och redigera login-widget: troligen en klass under `src/main/java/org/roda/wui/client/common/UserLogin.java` + ev. motsvarande `.ui.xml`
- Wrap befintlig form i en `FlowPanel`/`HTMLPanel` med nya semantiska CSS-klasser (`.login-brand-side` / `.login-form-side`)
- Ingen ändring av inloggningslogik — bara struktur och klassnamn

**Top-shell (header med nav):**
- Hitta header-klassen (trol. under `src/main/java/org/roda/wui/client/main/`)
- Byt logotyp-element från enkelt `<img>` till lockup med flagga + wordmark (alt: gör det i `Banner_sv_SE.html` om den används här — kolla rendering-sökvägen)
- Lägg till CSS-klasser för ny layout

### 5.3 Bonus (~5%)

- **Empty states:** byt placeholder-ikoner till signalflaggan-motivet i tomma vyer (CSS-only, bakgrundsbild).
- **Fokus-ring:** säkerställ att alla interaktiva element har synlig röd fokus-outline (tillägg i `theme.css`).
- **PDF/A-preview och konverteringsvyer:** adoptera den nya paletten på progress-indikatorer.

## 6. Tillgänglighet (WCAG 2.1 AA)

| Krav | Hur vi uppfyller |
|------|------------------|
| Textkontrast ≥ 4.5:1 (≥ 3:1 stor text) | Brödtext = Ink (14.68:1), sekundär = Slate (7.56:1). Primär röd används som textfärg endast på ≥ 18 px. |
| Logotypgrå #9C9E9F aldrig för text | Reserveras till logotyp och dekorativa element |
| Fokus synligt på alla interaktiva element | 2 px röd outline med 1 px offset, aldrig `outline: none` utan ersättning |
| Färg ej ensam informationsbärare | Statusprickar kombineras alltid med ikon eller text ("Leverans godkänd" ≠ bara grön prick) |
| Tangentbordsnavigering | GWT klarar detta; vi säkerställer att CSS inte bryter tab-order eller skymmer fokus |
| Skärmläsare | `alt`-text på alla logotyper och ikoner; dekorativa element får `aria-hidden="true"` |
| Textstorlek | Allt i `rem` eller relativa enheter där möjligt, så zoom upp till 200 % fungerar |

## 7. Ej i scope (explicit)

- Byt inte ut GWT eller introducera React / Vue / Web Components
- Skriv inte om katalog/ingest/admin-panelernas layout eller logik
- Ändra inte datamodell, API:er, plugins eller OAIS-funktioner
- Ändra inte användarflöden — alla arbetsflöden förblir identiska
- Ändra inte översättningsfiler för andra språk än `sv_SE` (engelska fallbacks uppdateras endast när de blockerar sv-flödet)
- Ersätt inte `eterna-logo.svg` — den befintliga behålls som primär
- Ändra inte portalen (`eterna-portal`, separat Next.js-app) — den har egen rebrand-process

## 8. Öppna frågor

1. **Webfont-bundling:** bundla woff2 i repot (förslag) eller lita på system-fonts? Air-gapped-installationer gör bundling till standardval.
2. **Engelsk fallback:** ska engelska sidorna också få faceliften visuellt, eller lämnas som idag? Förslag: samma CSS/visuella behandling, texter behålls engelska (RODA-termer ersätts inte utan svensk ekvivalent).
3. **Community-RFC:** ska faceliften hanteras som RFC till ETERNA-community (Issue First-regeln) innan PR? Oscar bedömer — det är en stor visuell ändring som bidragsgivare vill ha synpunkter på.
4. **Portal-synkning:** ska `eterna-portal` (Next.js/Tailwind) uppdateras i samma PR eller separat? Förslag: separat — den har egen stack.

## 9. Referensmaterial

- WhiteRed grafiska manual: `Grafisk_manual_20191115.pdf` (2019-11-15), primär källa för palett och logotyp
- Befintlig ETERNA-logotyp: `roda-ui/roda-wui/src/main/resources/config/theme/eterna-logo.svg`
- Befintlig WhiteRed-logotyp: `roda-ui/roda-wui/src/main/resources/config/theme/whitered-logo.svg`
- Mockups från brainstorming-sessionen (2026-04-22): `.superpowers/brainstorm/737-1776862599/content/` (login + dashboard HTML, inte committade)
- Inter typeface: https://rsms.me/inter/ (OFL 1.1)
- Barlow Condensed typeface: https://github.com/jpt/barlow (OFL 1.1)

---

**Nästa steg:** skriv implementationsplan som bryter ner detta i ordnade, körbara tasks med verifieringskriterier per steg.

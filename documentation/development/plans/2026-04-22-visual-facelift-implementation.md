# ETERNA visuell facelift — implementationsplan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implementera den visuella faceliften som beskrivs i `documentation/development/design-proposals/2026-04-22-visual-facelift.md` — ny palett, typografi, statiska mallar, svensk arkivvokabulär och minimala GWT-strukturtweaks — på branch `eterna-v1-alpha`.

**Architecture:** Additiva CSS/HTML-ändringar styrda via CSS-variabler, plus två smala GWT-strukturtweaks (login split-screen och header-lockup). Ingen ny JS, inga frameworks, ingen förändring av affärs- eller layoutlogik. Cirka 70 % rent HTML/CSS/.properties, 25 % tunn GWT-wrap, 5 % polish.

**Tech Stack:** GWT-klient (Java → JS-kompilering), Java 21, Spring Boot 4, Maven. Typografi: Inter (finns redan som variable TTF i `fonts/Inter/`), Barlow Condensed (ny, woff2). Plain CSS med `@font-face`, SVG. Dev-miljö: WSL Ubuntu 24.04 med Docker Solr + Zookeeper.

---

## File Structure

**Modified:**
- `roda-ui/roda-wui/src/main/resources/config/theme/theme.css` — brand tokens och komponentstilar (740 rader idag; utökas, inte skrivs om)
- `roda-ui/roda-wui/src/main/resources/config/theme/fonts/fonts.css` — nya `@font-face`-regler för Inter (variable) och Barlow Condensed
- `roda-ui/roda-wui/src/main/resources/config/theme/spinner.css` — spinnerfärg → ETERNA Red
- `roda-ui/roda-wui/src/main/resources/config/theme/Welcome_sv_SE.html` — ny startsida (avsnitt 4.2 i specen)
- `roda-ui/roda-wui/src/main/resources/config/theme/Welcome.html` — samma struktur, engelska texter
- `roda-ui/roda-wui/src/main/resources/config/theme/Banner_sv_SE.html` — lockup (flagga + ETERNA)
- `roda-ui/roda-wui/src/main/resources/config/theme/Banner.html` — samma som sv
- `roda-ui/roda-wui/src/main/resources/config/theme/Footer_sv_SE.html` — ny footer-markup
- `roda-ui/roda-wui/src/main/resources/config/theme/Footer.html` — samma
- `roda-ui/roda-wui/src/main/resources/config/theme/Error404_sv_SE.html` — restyled
- `roda-ui/roda-wui/src/main/resources/config/theme/Error500_sv_SE.html` — restyled
- `roda-ui/roda-wui/src/main/resources/config/theme/ErrorInactiveAccount_sv_SE.html` — restyled
- `roda-ui/roda-wui/src/main/resources/config/i18n/client/ClientMessages_sv_SE.properties` — vokabulärbyte (1758 rader, ~40 nyckelbyten)
- `roda-ui/roda-wui/src/main/resources/config/i18n/ServerMessages_sv_SE.properties` — vokabulärbyte
- `roda-ui/roda-wui/src/main/java/org/roda/wui/client/common/UserLogin.java` — wrap i split-screen struktur (ingen logikändring)

**Created:**
- `roda-ui/roda-wui/src/main/resources/config/theme/eterna-logo-reversed.svg`
- `roda-ui/roda-wui/src/main/resources/config/theme/eterna-flag.svg`
- `roda-ui/roda-wui/src/main/resources/config/theme/Error401_sv_SE.html` (finns inte idag)
- `roda-ui/roda-wui/src/main/resources/config/theme/Error401.html`
- `roda-ui/roda-wui/src/main/resources/config/theme/favicon/favicon-16x16.png`
- `roda-ui/roda-wui/src/main/resources/config/theme/favicon/favicon-32x32.png`
- `roda-ui/roda-wui/src/main/resources/config/theme/favicon/favicon.ico`
- `roda-ui/roda-wui/src/main/resources/config/theme/fonts/BarlowCondensed/BarlowCondensed-Medium.woff2`
- `roda-ui/roda-wui/src/main/resources/config/theme/fonts/BarlowCondensed/BarlowCondensed-SemiBold.woff2`
- `roda-ui/roda-wui/src/main/resources/config/theme/fonts/BarlowCondensed/BarlowCondensed-Bold.woff2`
- `roda-ui/roda-wui/src/main/resources/config/theme/fonts/BarlowCondensed/OFL.txt`
- `roda-ui/roda-wui/src/main/resources/config/theme/fonts/BarlowCondensed/README.txt`

**Ej rörda** (explicit ur scope):
- `eterna-logo.svg` — bevaras oförändrad
- Alla `.properties`-filer för andra språk än sv_SE
- Alla `.ui.xml`, Java-klasser, plugins, datamodell, API
- `eterna-portal/` (separat Next.js-app)

---

## Verifikationsnoter

**Bygg:** `mvn clean install -DskipTests` tar ~15 min. För iterativ utveckling används GWT SuperDevMode eller bara resource-reload i browser eftersom ändringar under `src/main/resources/config/theme/` och `config/i18n/` laddas dynamiskt av servern på `/api/v2/themes`.

**Dev-start** (körs före task 2):
```bash
cd ~/.eterna-dev && sudo service docker start && docker-compose up -d
cd ~/ETERNA && mvn -pl roda-ui/roda-wui spring-boot:run -Pstandalone
```

**Browser-verifikation:** http://localhost:8080 → manuell kontroll per task. Inga automatiska frontend-tester finns idag.

**Tester som finns:** Junit-tester under `roda-ui/roda-wui/src/test/java/`. För visuellt arbete kompletteras de med manuella browsertester och, för GWT-ändringar, minst en full `mvn clean install` innan PR.

---

## Task 0: Förberedelser — branch, baseline, dev-miljö

**Files:**
- Create: branch `facelift/visual-v1`
- Capture: baseline-screenshots av login-sida och Welcome-sida

- [ ] **Step 1: Skapa och checka ut feature-branch från `eterna-v1-alpha`**

```bash
cd ~/ETERNA
git fetch origin
git checkout eterna-v1-alpha
git pull --ff-only origin eterna-v1-alpha
git checkout -b facelift/visual-v1
```
Expected: `On branch facelift/visual-v1`

- [ ] **Step 2: Starta dev-stacken och verifiera att appen startar**

```bash
cd ~/.eterna-dev && sudo service docker start && docker-compose up -d
cd ~/ETERNA && mvn -pl roda-ui/roda-wui spring-boot:run -Pstandalone
```
Expected: Appen svarar på http://localhost:8080.

- [ ] **Step 3: Ta baseline-screenshots för före/efter-dokumentation**

Öppna i browser och spara som PNG under `documentation/development/design-proposals/baseline-2026-04-22/`:
- `login-before.png` (http://localhost:8080/#login)
- `welcome-before.png` (http://localhost:8080/#welcome efter inloggning)
- `error404-before.png` (http://localhost:8080/#error/404)

Expected: Tre PNG-filer på angiven plats.

- [ ] **Step 4: Commit baseline**

```bash
git add documentation/development/design-proposals/baseline-2026-04-22/
git commit -m "docs(facelift): capture visual baseline before facelift"
```

---

## Task 1: Brand tokens — CSS-variabler för palett och typografi

**Files:**
- Modify: `roda-ui/roda-wui/src/main/resources/config/theme/theme.css` (lägg till block högst upp, ingen borttagning)

- [ ] **Step 1: Läs de första 40 raderna av nuvarande `theme.css`**

Syfte: identifiera var `:root`-block (om det finns) eller var det är lämpligt att lägga nya tokens så att de får företräde i kaskaden.

```bash
sed -n '1,40p' roda-ui/roda-wui/src/main/resources/config/theme/theme.css
```

- [ ] **Step 2: Lägg till ETERNA brand tokens som första `:root`-block i `theme.css`**

Placera följande direkt efter ev. befintlig `@import` men före första selektor. Om `:root` redan finns — lägg de nya variablerna inom samma block.

```css
/* === ETERNA brand tokens (facelift 2026-04) === */
:root {
  --eterna-red: #E2001A;
  --eterna-red-deep: #B8001A;
  --eterna-red-soft: #FEE2E5;
  --eterna-ink: #1F2937;
  --eterna-slate: #4B5563;
  --eterna-muted: #6B7280;
  --eterna-brand-gray: #9C9E9F;
  --eterna-border: #E5E7EB;
  --eterna-surface: #F9FAFB;
  --eterna-paper: #FFFFFF;
  --eterna-dark: #111827;
  --eterna-ok: #059669;
  --eterna-warn: #D97706;

  --eterna-font-display: 'Barlow Condensed', 'Arial Narrow', 'Helvetica Neue', Arial, sans-serif;
  --eterna-font-body: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif;

  --eterna-radius-sm: 4px;
  --eterna-radius-md: 6px;
  --eterna-radius-lg: 10px;
  --eterna-shadow-card: 0 2px 8px rgba(0, 0, 0, 0.04);
  --eterna-shadow-elevated: 0 4px 16px rgba(0, 0, 0, 0.06);

  --eterna-focus-ring: 0 0 0 2px var(--eterna-paper), 0 0 0 4px var(--eterna-red);
}
```

- [ ] **Step 3: Verifiera att CSS parsar (inget syntaxfel)**

```bash
npx stylelint roda-ui/roda-wui/src/main/resources/config/theme/theme.css --no-config --report-needless-disables 2>&1 | head -20
```
Om stylelint ej är tillgänglig: kör `node -e 'require("css").parse(require("fs").readFileSync("roda-ui/roda-wui/src/main/resources/config/theme/theme.css","utf8"))'` — acceptera tyst exit (exit 0).

- [ ] **Step 4: Reload `theme.css` i browser och verifiera att nuvarande vyer inte har regressat**

Öppna http://localhost:8080, hård-reload (Ctrl+F5). Welcome och login ska se exakt ut som förut (inga klasser har ändrats ännu — bara variabler lagts till som inte används).

Expected: Ingen visuell förändring jämfört med baseline.

- [ ] **Step 5: Commit**

```bash
git add roda-ui/roda-wui/src/main/resources/config/theme/theme.css
git commit -m "feat(theme): add ETERNA brand token CSS variables"
```

---

## Task 2: Webfonts — Barlow Condensed och Inter via fonts.css

**Files:**
- Create: `roda-ui/roda-wui/src/main/resources/config/theme/fonts/BarlowCondensed/BarlowCondensed-Medium.woff2`
- Create: `roda-ui/roda-wui/src/main/resources/config/theme/fonts/BarlowCondensed/BarlowCondensed-SemiBold.woff2`
- Create: `roda-ui/roda-wui/src/main/resources/config/theme/fonts/BarlowCondensed/BarlowCondensed-Bold.woff2`
- Create: `roda-ui/roda-wui/src/main/resources/config/theme/fonts/BarlowCondensed/OFL.txt`
- Create: `roda-ui/roda-wui/src/main/resources/config/theme/fonts/BarlowCondensed/README.txt`
- Modify: `roda-ui/roda-wui/src/main/resources/config/theme/fonts/fonts.css`

- [ ] **Step 1: Ladda ner Barlow Condensed från officiell källa**

```bash
mkdir -p /tmp/barlow-dl && cd /tmp/barlow-dl
curl -L -o barlow.zip "https://github.com/jpt/barlow/archive/refs/heads/master.zip"
unzip -o barlow.zip
ls barlow-master/fonts/barlow-condensed/webfonts/ | head -20
```
Expected: En listning som innehåller `BarlowCondensed-Medium.woff2`, `BarlowCondensed-SemiBold.woff2`, `BarlowCondensed-Bold.woff2`.

- [ ] **Step 2: Kopiera de tre vikterna + OFL-licens till repo**

```bash
DEST=~/ETERNA/roda-ui/roda-wui/src/main/resources/config/theme/fonts/BarlowCondensed
mkdir -p "$DEST"
cp /tmp/barlow-dl/barlow-master/fonts/barlow-condensed/webfonts/BarlowCondensed-Medium.woff2 "$DEST/"
cp /tmp/barlow-dl/barlow-master/fonts/barlow-condensed/webfonts/BarlowCondensed-SemiBold.woff2 "$DEST/"
cp /tmp/barlow-dl/barlow-master/fonts/barlow-condensed/webfonts/BarlowCondensed-Bold.woff2 "$DEST/"
cp /tmp/barlow-dl/barlow-master/OFL.txt "$DEST/OFL.txt"
ls -la "$DEST"
```
Expected: Tre `.woff2`-filer och `OFL.txt` finns i destinationen.

- [ ] **Step 3: Skriv en README i font-katalogen för proveniens**

Skapa `roda-ui/roda-wui/src/main/resources/config/theme/fonts/BarlowCondensed/README.txt`:

```
Barlow Condensed
Licens: SIL Open Font License 1.1 (se OFL.txt)
Källa: https://github.com/jpt/barlow
Hämtad: 2026-04-22
Användning: Display-typsnitt för ETERNA (rubriker, wordmark, hero-text)
Vikter bundlade: 500 (Medium), 600 (SemiBold), 700 (Bold)
```

- [ ] **Step 4: Lägg till `@font-face`-regler för Barlow Condensed och Inter i `fonts.css`**

Redigera `roda-ui/roda-wui/src/main/resources/config/theme/fonts/fonts.css`. Lägg blocket längst ner, efter sista befintliga `@font-face`:

```css
/* === ETERNA facelift: Barlow Condensed (display) + Inter (body) === */
@font-face {
  font-family: 'Barlow Condensed';
  font-style: normal;
  font-weight: 500;
  font-display: swap;
  src: url(../../api/v2/themes?resource-id=fonts/BarlowCondensed/BarlowCondensed-Medium.woff2) format('woff2');
}
@font-face {
  font-family: 'Barlow Condensed';
  font-style: normal;
  font-weight: 600;
  font-display: swap;
  src: url(../../api/v2/themes?resource-id=fonts/BarlowCondensed/BarlowCondensed-SemiBold.woff2) format('woff2');
}
@font-face {
  font-family: 'Barlow Condensed';
  font-style: normal;
  font-weight: 700;
  font-display: swap;
  src: url(../../api/v2/themes?resource-id=fonts/BarlowCondensed/BarlowCondensed-Bold.woff2) format('woff2');
}
@font-face {
  font-family: 'Inter';
  font-style: normal;
  font-weight: 100 900;
  font-display: swap;
  src: url(../../api/v2/themes?resource-id=fonts/Inter/Inter-VariableFont_opsz,wght.ttf) format('truetype-variations');
}
@font-face {
  font-family: 'Inter';
  font-style: italic;
  font-weight: 100 900;
  font-display: swap;
  src: url(../../api/v2/themes?resource-id=fonts/Inter/Inter-Italic-VariableFont_opsz,wght.ttf) format('truetype-variations');
}
```

- [ ] **Step 5: Verifiera font-loading i browser**

Reload appen. Öppna DevTools → Network → filtrera "font". Expected: tre `BarlowCondensed-*.woff2` och två `Inter-*.ttf` laddas 200 OK när typsnitten används (de laddas lazy — kan kräva att task 3 är klar för att visas).

Alternativ-kontroll i DevTools Console:
```js
document.fonts.check('16px "Barlow Condensed"')
```
Expected: returnerar `true` efter att task 3 applicerat typsnittet någonstans. Nu räcker det med 200 OK-svar på request-panelen när man navigerar direkt till URL:en.

- [ ] **Step 6: Commit**

```bash
git add roda-ui/roda-wui/src/main/resources/config/theme/fonts/BarlowCondensed
git add roda-ui/roda-wui/src/main/resources/config/theme/fonts/fonts.css
git commit -m "feat(theme): bundle Barlow Condensed and register Inter variable font"
```

---

## Task 3: Global typografi + röd topp-list

**Files:**
- Modify: `roda-ui/roda-wui/src/main/resources/config/theme/theme.css`

- [ ] **Step 1: Lägg till globala typografiregler efter `:root`-blocket**

Syfte: sätta Inter som default body-font och Barlow Condensed på rubriker. Behåll bakåtkompat via progressiv override — använd `.eterna-ui` eller `body` beroende på skop. Vi väljer `body` för att slå igenom överallt.

```css
/* === ETERNA typography === */
body,
.gwt-root,
.wui-body {
  font-family: var(--eterna-font-body);
  color: var(--eterna-ink);
}

h1, h2, h3, h4, h5, h6,
.eterna-display,
.hero .title {
  font-family: var(--eterna-font-display);
  font-weight: 600;
  letter-spacing: 0.01em;
  color: var(--eterna-ink);
}

/* 6 px röd topp-list — global */
body::before {
  content: '';
  display: block;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 6px;
  background: var(--eterna-red);
  z-index: 9999;
  pointer-events: none;
}

body {
  padding-top: 6px;
}
```

- [ ] **Step 2: Reload browser, verifiera röd topp-list + typografi**

Förväntade observationer:
- 6 px röd linje överst på varje sida
- Brödtext renderas i Inter (tester via DevTools: `getComputedStyle(document.body).fontFamily` innehåller "Inter")
- H1/H2 renderas i Barlow Condensed

Om röd topp-list täcks av app-shell: sänk `z-index` eller injicera en absolut-positionerad div i `Main.html` istället. Testa båda vyerna: login (`/#login`) och Welcome.

- [ ] **Step 3: Verifiera att inga layouter kraschat**

Klicka igenom: login → logga in → Welcome → Katalog → Administration. Inga felmeddelanden i konsol, inget innehåll skymt. Ta skärmdump `welcome-after-task3.png` för senare jämförelse.

- [ ] **Step 4: Commit**

```bash
git add roda-ui/roda-wui/src/main/resources/config/theme/theme.css
git commit -m "feat(theme): apply Inter+Barlow typography and red top bar globally"
```

---

## Task 4: Brand-assets — reversed logo, standalone flag, favicons

**Files:**
- Create: `roda-ui/roda-wui/src/main/resources/config/theme/eterna-logo-reversed.svg`
- Create: `roda-ui/roda-wui/src/main/resources/config/theme/eterna-flag.svg`
- Create: `roda-ui/roda-wui/src/main/resources/config/theme/favicon/favicon-16x16.png`
- Create: `roda-ui/roda-wui/src/main/resources/config/theme/favicon/favicon-32x32.png`
- Create: `roda-ui/roda-wui/src/main/resources/config/theme/favicon/favicon.ico`

- [ ] **Step 1: Läs `eterna-logo.svg` för att förstå nuvarande struktur**

```bash
cat roda-ui/roda-wui/src/main/resources/config/theme/eterna-logo.svg
```
Notera: flaggpaths och koordinater för ETERNA-wordmarkens textpaths. Kopiera dessa exakt till nästa steg.

- [ ] **Step 2: Skapa `eterna-logo-reversed.svg`**

Samma SVG som `eterna-logo.svg` men med ETERNA-wordmarken i `#FFFFFF` istället för `#9C9E9F`. Flaggpaths (vit+röd signalflaggan-H) lämnas oförändrade.

Skapa filen med identisk `viewBox` och identiska path-definitioner som i originalet. Byt exakt varje `fill="#9C9E9F"` på wordmark-paths till `fill="#FFFFFF"`. Lämna flaggans `fill="#fff"` och `fill="#E2001A"` oförändrade.

- [ ] **Step 3: Skapa `eterna-flag.svg` (endast signalflaggan)**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 94 94" aria-hidden="true">
  <title>ETERNA signalflagga</title>
  <path d="m0.33337 1.3192h50.877v91.191h-50.877v-91.191z" fill="#fff"/>
  <path d="m51.544 0.9858h-51.544v91.857h51.544v-91.857zm-0.6666 91.191h-50.211v-90.524h50.211v90.524z" fill="#E2001A"/>
  <path d="m92.849 92.843h-46.603v-91.857h46.603v91.857z" fill="#E2001A"/>
</svg>
```

- [ ] **Step 4: Generera favicons från signalflaggan**

```bash
cd ~/ETERNA/roda-ui/roda-wui/src/main/resources/config/theme
mkdir -p favicon
# rsvg-convert kan behöva installeras: sudo apt install -y librsvg2-bin imagemagick
rsvg-convert -w 16 -h 16 eterna-flag.svg -o favicon/favicon-16x16.png
rsvg-convert -w 32 -h 32 eterna-flag.svg -o favicon/favicon-32x32.png
rsvg-convert -w 48 -h 48 eterna-flag.svg -o favicon/favicon-48x48.png
convert favicon/favicon-16x16.png favicon/favicon-32x32.png favicon/favicon-48x48.png favicon/favicon.ico
ls -la favicon/
```
Expected: `.png` och `.ico` finns och är > 0 bytes.

- [ ] **Step 5: Verifiera SVG:er i browser**

Navigera direkt till:
- http://localhost:8080/api/v2/themes?resource-id=eterna-logo-reversed.svg
- http://localhost:8080/api/v2/themes?resource-id=eterna-flag.svg

Expected: båda renderas. Reversed har vit text; flag visar bara vitt + rött H.

- [ ] **Step 6: Commit**

```bash
git add roda-ui/roda-wui/src/main/resources/config/theme/eterna-logo-reversed.svg
git add roda-ui/roda-wui/src/main/resources/config/theme/eterna-flag.svg
git add roda-ui/roda-wui/src/main/resources/config/theme/favicon/
git commit -m "feat(theme): add reversed ETERNA logo, standalone flag, favicons"
```

---

## Task 5: Komponentstilar — knappar, inputs, kort, fokus

**Files:**
- Modify: `roda-ui/roda-wui/src/main/resources/config/theme/theme.css`

- [ ] **Step 1: Lägg till komponentblock efter typografin (från Task 3)**

```css
/* === ETERNA buttons === */
.btn-eterna-primary,
button.btn-primary,
.gwt-Button.btn-primary {
  background: var(--eterna-red);
  color: var(--eterna-paper);
  border: 0;
  padding: 12px 20px;
  border-radius: var(--eterna-radius-md);
  font-family: var(--eterna-font-body);
  font-weight: 600;
  font-size: 14px;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  cursor: pointer;
  transition: background 120ms ease;
}
.btn-eterna-primary:hover,
button.btn-primary:hover,
.gwt-Button.btn-primary:hover {
  background: var(--eterna-red-deep);
}
.btn-eterna-secondary {
  background: transparent;
  color: var(--eterna-ink);
  border: 1px solid var(--eterna-border);
  padding: 11px 19px;
  border-radius: var(--eterna-radius-md);
  font-family: var(--eterna-font-body);
  font-weight: 500;
  font-size: 14px;
  cursor: pointer;
}

/* === ETERNA inputs === */
.form-eterna-input,
input.gwt-TextBox,
input.gwt-PasswordTextBox {
  width: 100%;
  padding: 11px 13px;
  border: 1px solid var(--eterna-border);
  border-radius: var(--eterna-radius-md);
  font-family: inherit;
  font-size: 14px;
  box-sizing: border-box;
  background: var(--eterna-paper);
  color: var(--eterna-ink);
}
.form-eterna-label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: var(--eterna-slate);
  margin: 18px 0 6px;
}

/* === ETERNA cards === */
.eterna-card {
  background: var(--eterna-paper);
  border: 1px solid var(--eterna-border);
  border-radius: var(--eterna-radius-lg);
  padding: 24px;
  box-shadow: var(--eterna-shadow-card);
}
.eterna-card--dark {
  background: var(--eterna-dark);
  color: var(--eterna-paper);
  border: 0;
  position: relative;
  overflow: hidden;
}

/* === ETERNA focus ring (WCAG 2.4.7) === */
a:focus-visible,
button:focus-visible,
input:focus-visible,
select:focus-visible,
textarea:focus-visible,
[tabindex]:focus-visible {
  outline: none;
  box-shadow: var(--eterna-focus-ring);
  border-radius: var(--eterna-radius-sm);
}

/* === ETERNA section header === */
.eterna-section-header {
  font-family: var(--eterna-font-display);
  font-size: 22px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--eterna-ink);
  padding-left: 12px;
  border-left: 3px solid var(--eterna-red);
  margin: 32px 0 16px;
}
```

- [ ] **Step 2: Reload och verifiera att nuvarande knappar/inputs inte regressar**

Eftersom vi enbart lagt till nya klasser (och selektorer på `.gwt-Button.btn-primary`) ska inga existerande knappar på Welcome/Katalog påverkas visuellt — såvida de inte redan har klassen `btn-primary`. Klicka igenom och ta skärmdumpar.

Om existerande knappar fick oönskad styling (exempelvis röda där de tidigare varit grå), notera klassnamnet och backa ut selektorn — lägg i så fall ändringen som separat class `.btn-eterna-primary` och applicera den explicit i kommande tasks istället.

- [ ] **Step 3: Verifiera fokus-ring med tangentbord**

Navigera med Tab i login-formen. Expected: varje fält visar tydlig röd outline på fokus.

- [ ] **Step 4: Commit**

```bash
git add roda-ui/roda-wui/src/main/resources/config/theme/theme.css
git commit -m "feat(theme): add ETERNA button, input, card and focus-ring components"
```

---

## Task 6: Banner.html — lockup med signalflagga + wordmark

**Files:**
- Modify: `roda-ui/roda-wui/src/main/resources/config/theme/Banner_sv_SE.html`
- Modify: `roda-ui/roda-wui/src/main/resources/config/theme/Banner.html`

- [ ] **Step 1: Läs nuvarande Banner-innehåll**

```bash
cat roda-ui/roda-wui/src/main/resources/config/theme/Banner_sv_SE.html
cat roda-ui/roda-wui/src/main/resources/config/theme/Banner.html
```
Expected: Minimalt innehåll (filerna är 105 bytes idag).

- [ ] **Step 2: Ersätt `Banner_sv_SE.html` med lockup**

```html
<div class="eterna-banner">
  <img src="api/v2/themes?resource-id=eterna-flag.svg"
       alt=""
       aria-hidden="true"
       class="eterna-banner__flag" />
  <span class="eterna-banner__wordmark">ETERNA</span>
  <span class="eterna-banner__byline">ett e-arkiv från WhiteRed</span>
</div>
```

- [ ] **Step 3: Ersätt `Banner.html` med samma struktur (engelsk byline)**

```html
<div class="eterna-banner">
  <img src="api/v2/themes?resource-id=eterna-flag.svg"
       alt=""
       aria-hidden="true"
       class="eterna-banner__flag" />
  <span class="eterna-banner__wordmark">ETERNA</span>
  <span class="eterna-banner__byline">an e-archive from WhiteRed</span>
</div>
```

- [ ] **Step 4: Lägg till banner-styling i `theme.css`**

```css
/* === ETERNA banner lockup === */
.eterna-banner {
  display: inline-flex;
  align-items: center;
  gap: 12px;
}
.eterna-banner__flag {
  width: 28px;
  height: 28px;
  flex-shrink: 0;
}
.eterna-banner__wordmark {
  font-family: var(--eterna-font-display);
  font-weight: 700;
  font-size: 22px;
  letter-spacing: 0.05em;
  color: var(--eterna-ink);
}
.eterna-banner__byline {
  font-family: var(--eterna-font-body);
  font-size: 12px;
  color: var(--eterna-muted);
  letter-spacing: 0.02em;
}
```

- [ ] **Step 5: Reload och verifiera bannern**

Banner renderas troligen i top-shellet. Navigera till Welcome → kontrollera att flaggan visas till vänster om "ETERNA" med byline. Ta screenshot.

- [ ] **Step 6: Commit**

```bash
git add roda-ui/roda-wui/src/main/resources/config/theme/Banner.html \
        roda-ui/roda-wui/src/main/resources/config/theme/Banner_sv_SE.html \
        roda-ui/roda-wui/src/main/resources/config/theme/theme.css
git commit -m "feat(theme): new ETERNA banner lockup with flag and byline"
```

---

## Task 7: Welcome_sv_SE.html — ny startsida efter inloggning

**Files:**
- Modify: `roda-ui/roda-wui/src/main/resources/config/theme/Welcome_sv_SE.html` (ersätts helt)
- Modify: `roda-ui/roda-wui/src/main/resources/config/theme/theme.css` (Welcome-specifika klasser)

- [ ] **Step 1: Säkerhetskopiera nuvarande Welcome och läs den**

```bash
cp roda-ui/roda-wui/src/main/resources/config/theme/Welcome_sv_SE.html /tmp/welcome-before.html
wc -l roda-ui/roda-wui/src/main/resources/config/theme/Welcome_sv_SE.html
```

- [ ] **Step 2: Ersätt `Welcome_sv_SE.html` med ny dashboardstruktur**

```html
<div class="eterna-welcome">
  <section class="eterna-welcome__hero">
    <p class="eterna-welcome__greeting-label">God dag</p>
    <h1 class="eterna-welcome__greeting">Välkommen till ETERNA</h1>
    <p class="eterna-welcome__lead">
      Ett öppet svenskt e-arkiv för långsiktigt digitalt bevarande. Byggt på OAIS,
      E-ARK, FGS, PREMIS och METS.
    </p>
    <form class="eterna-welcome__search" action="#search" method="get">
      <label class="eterna-welcome__search-scope" for="eterna-welcome-scope">
        <span>I arkivet</span>
        <select id="eterna-welcome-scope" name="scope">
          <option value="all">Allt</option>
          <option value="catalogue">Katalog</option>
          <option value="deliveries">Leveranser</option>
        </select>
      </label>
      <input class="eterna-welcome__search-input" type="search" name="q"
             placeholder="Sök i arkivet…" aria-label="Sök" />
      <button class="btn-eterna-primary" type="submit">Sök</button>
    </form>
  </section>

  <div class="eterna-welcome__grid">
    <section class="eterna-welcome__main">
      <h2 class="eterna-section-header">Snabbåtgärder</h2>
      <div class="eterna-actions">
        <a class="eterna-action" href="#ingest">
          <span class="eterna-action__icon" aria-hidden="true">↥</span>
          <span class="eterna-action__title">Lämna in material</span>
          <span class="eterna-action__desc">Skapa en ny leverans till arkivet</span>
        </a>
        <a class="eterna-action" href="#ingest/transfer">
          <span class="eterna-action__icon" aria-hidden="true">⎘</span>
          <span class="eterna-action__title">Granska leveranser</span>
          <span class="eterna-action__desc">Pågående och väntande leveranser</span>
        </a>
        <a class="eterna-action" href="#search">
          <span class="eterna-action__icon" aria-hidden="true">⌕</span>
          <span class="eterna-action__title">Sök i katalogen</span>
          <span class="eterna-action__desc">Hitta arkivpaket och beskrivningar</span>
        </a>
        <a class="eterna-action" href="#administration">
          <span class="eterna-action__icon" aria-hidden="true">⚙</span>
          <span class="eterna-action__title">Administrera åtkomst</span>
          <span class="eterna-action__desc">Användare, grupper och behörigheter</span>
        </a>
      </div>

      <h2 class="eterna-section-header">Senaste händelser</h2>
      <ul class="eterna-activity">
        <li class="eterna-activity__item">
          <span class="eterna-status-dot eterna-status-dot--ok" aria-hidden="true"></span>
          <div class="eterna-activity__body">
            <p class="eterna-activity__title">Leverans godkänd</p>
            <p class="eterna-activity__desc">Arkivpaket från Region Öst validerades utan fel.</p>
          </div>
          <span class="eterna-activity__time">—</span>
        </li>
        <li class="eterna-activity__item">
          <span class="eterna-status-dot eterna-status-dot--warn" aria-hidden="true"></span>
          <div class="eterna-activity__body">
            <p class="eterna-activity__title">Bevarandejobb väntar</p>
            <p class="eterna-activity__desc">Formatkontroll avvaktar manuell granskning.</p>
          </div>
          <span class="eterna-activity__time">—</span>
        </li>
        <li class="eterna-activity__item">
          <span class="eterna-status-dot eterna-status-dot--err" aria-hidden="true"></span>
          <div class="eterna-activity__body">
            <p class="eterna-activity__title">Leverans avvisad</p>
            <p class="eterna-activity__desc">SIP-paket saknar METS-manifest.</p>
          </div>
          <span class="eterna-activity__time">—</span>
        </li>
      </ul>
      <p class="eterna-activity__note">
        Visar senaste händelser när logg-kopplingen aktiveras. Tills dess renderas exempeldata.
      </p>
    </section>

    <aside class="eterna-welcome__aside">
      <div class="eterna-card eterna-card--dark">
        <h3 class="eterna-aside__title">Arkivet i siffror</h3>
        <dl class="eterna-stats">
          <div class="eterna-stat">
            <dt>Arkivpaket totalt</dt>
            <dd>—</dd>
          </div>
          <div class="eterna-stat">
            <dt>Levererat denna månad</dt>
            <dd class="eterna-stat--accent">—</dd>
          </div>
          <div class="eterna-stat">
            <dt>Total lagring</dt>
            <dd>—</dd>
          </div>
          <div class="eterna-stat">
            <dt>Pågående bevarandejobb</dt>
            <dd>—</dd>
          </div>
        </dl>
      </div>
      <div class="eterna-card eterna-card--dark">
        <h3 class="eterna-aside__title">Arkivet uppfyller</h3>
        <ul class="eterna-standards">
          <li>OAIS — ISO 14721</li>
          <li>E-ARK CSIP 2.2</li>
          <li>FGS 2.1</li>
          <li>PREMIS 3.0</li>
          <li>METS 1.12</li>
          <li>EAD 3</li>
        </ul>
      </div>
      <div class="eterna-card">
        <h3 class="eterna-aside__title">Dokumentation</h3>
        <p>
          Handböcker, standarder och utvecklardokumentation finns på
          <a href="https://github.com/ETERNA-earkiv/ETERNA">GitHub</a>.
        </p>
      </div>
    </aside>
  </div>
</div>
```

- [ ] **Step 3: Lägg till Welcome-specifika stilar i `theme.css`**

```css
/* === ETERNA Welcome (facelift 2026-04) === */
.eterna-welcome {
  max-width: 1180px;
  margin: 0 auto;
  padding: 40px 32px 72px;
}
.eterna-welcome__hero {
  padding: 32px 0 48px;
  border-bottom: 1px solid var(--eterna-border);
  margin-bottom: 40px;
}
.eterna-welcome__greeting-label {
  font-size: 12px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--eterna-muted);
  margin: 0 0 8px;
}
.eterna-welcome__greeting {
  font-family: var(--eterna-font-display);
  font-weight: 600;
  font-size: 44px;
  line-height: 1.1;
  color: var(--eterna-ink);
  margin: 0 0 12px;
}
.eterna-welcome__lead {
  font-size: 15px;
  color: var(--eterna-slate);
  max-width: 56ch;
  line-height: 1.55;
  margin: 0 0 28px;
}
.eterna-welcome__search {
  display: flex;
  gap: 8px;
  align-items: stretch;
  max-width: 640px;
  border: 1px solid var(--eterna-border);
  border-radius: var(--eterna-radius-md);
  padding: 4px;
  background: var(--eterna-paper);
}
.eterna-welcome__search-scope {
  display: inline-flex;
  align-items: center;
  padding: 0 10px;
  border-right: 1px solid var(--eterna-border);
  font-size: 13px;
  color: var(--eterna-slate);
  gap: 6px;
}
.eterna-welcome__search-scope select {
  border: 0;
  background: transparent;
  font: inherit;
  color: inherit;
}
.eterna-welcome__search-input {
  flex: 1;
  border: 0;
  padding: 10px 12px;
  font: inherit;
  background: transparent;
  color: var(--eterna-ink);
}
.eterna-welcome__search-input:focus-visible {
  outline: none;
  box-shadow: var(--eterna-focus-ring);
  border-radius: var(--eterna-radius-sm);
}

.eterna-welcome__grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 40px;
}
@media (max-width: 900px) {
  .eterna-welcome__grid { grid-template-columns: 1fr; }
}

.eterna-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
@media (max-width: 600px) {
  .eterna-actions { grid-template-columns: 1fr; }
}
.eterna-action {
  display: grid;
  grid-template-rows: auto auto 1fr;
  gap: 8px;
  padding: 20px;
  border: 1px solid var(--eterna-border);
  border-radius: var(--eterna-radius-lg);
  text-decoration: none;
  color: inherit;
  background: var(--eterna-paper);
  transition: border-color 120ms ease, box-shadow 120ms ease;
}
.eterna-action:hover {
  border-color: var(--eterna-red);
  box-shadow: var(--eterna-shadow-elevated);
}
.eterna-action__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 999px;
  background: var(--eterna-red-soft);
  color: var(--eterna-red);
  font-size: 20px;
}
.eterna-action__title {
  font-family: var(--eterna-font-display);
  font-weight: 600;
  font-size: 18px;
  color: var(--eterna-ink);
}
.eterna-action__desc {
  font-size: 13px;
  color: var(--eterna-slate);
}

.eterna-welcome__aside {
  display: grid;
  gap: 16px;
  align-content: start;
}
.eterna-aside__title {
  font-family: var(--eterna-font-display);
  font-weight: 600;
  font-size: 18px;
  margin: 0 0 12px;
  letter-spacing: 0.03em;
  text-transform: uppercase;
}
.eterna-standards {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 6px;
  font-size: 13px;
  letter-spacing: 0.04em;
  color: rgba(255, 255, 255, 0.85);
}
.eterna-card--dark::before {
  content: '';
  position: absolute;
  right: -80px;
  top: -80px;
  width: 240px;
  height: 240px;
  border: 40px solid rgba(226, 0, 26, 0.12);
  border-radius: 50%;
  pointer-events: none;
}

/* === ETERNA aktivitetsström === */
.eterna-activity {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 2px;
  border: 1px solid var(--eterna-border);
  border-radius: var(--eterna-radius-lg);
  overflow: hidden;
  background: var(--eterna-paper);
}
.eterna-activity__item {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 12px;
  align-items: start;
  padding: 14px 18px;
  border-bottom: 1px solid var(--eterna-border);
}
.eterna-activity__item:last-child { border-bottom: 0; }
.eterna-activity__item .eterna-status-dot { margin-top: 7px; }
.eterna-activity__body { min-width: 0; }
.eterna-activity__title {
  font-weight: 600;
  font-size: 14px;
  color: var(--eterna-ink);
  margin: 0 0 2px;
}
.eterna-activity__desc {
  font-size: 13px;
  color: var(--eterna-slate);
  margin: 0;
}
.eterna-activity__time {
  font-size: 12px;
  color: var(--eterna-muted);
  font-variant-numeric: tabular-nums;
}
.eterna-activity__note {
  font-size: 12px;
  color: var(--eterna-muted);
  margin: 10px 0 0;
  font-style: italic;
}

/* === ETERNA statistik ("Arkivet i siffror") === */
.eterna-stats {
  display: grid;
  gap: 18px;
  margin: 0;
}
.eterna-stat {
  display: grid;
  gap: 4px;
}
.eterna-stat dt {
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.6);
  margin: 0;
}
.eterna-stat dd {
  font-family: var(--eterna-font-display);
  font-weight: 600;
  font-size: 28px;
  color: var(--eterna-paper);
  margin: 0;
  line-height: 1;
}
.eterna-stat--accent { color: var(--eterna-red) !important; }
```

- [ ] **Step 4: Reload Welcome-sidan och verifiera**

Öppna http://localhost:8080/#welcome efter inloggning.

Expected:
- Hero med "God dag" + "Välkommen till ETERNA" i Barlow Condensed 44 px
- Sökruta med scope-selector
- 2×2 grid av åtgärdskort till vänster, med röd-soft ikoner
- Aktivitetsström "Senaste händelser" med tre statusprickar (grön/orange/röd) och exempeldata
- Mörkt kort "Arkivet i siffror" med fyra statistik-slots (alla `—` tills logg-koppling aktiveras)
- Mörkt kort "Arkivet uppfyller" med standards
- Röd topp-list kvar

- [ ] **Step 5: Ta efter-screenshot för dokumentation**

```bash
# screenshot manuellt → sparas som:
# documentation/development/design-proposals/baseline-2026-04-22/welcome-after.png
```

- [ ] **Step 6: Commit**

```bash
git add roda-ui/roda-wui/src/main/resources/config/theme/Welcome_sv_SE.html \
        roda-ui/roda-wui/src/main/resources/config/theme/theme.css \
        documentation/development/design-proposals/baseline-2026-04-22/welcome-after.png
git commit -m "feat(welcome): replace Welcome_sv_SE with ETERNA facelift dashboard"
```

---

## Task 8: Welcome.html (engelsk fallback med samma struktur)

**Files:**
- Modify: `roda-ui/roda-wui/src/main/resources/config/theme/Welcome.html`

- [ ] **Step 1: Läs och säkerhetskopiera nuvarande engelska Welcome**

```bash
cp roda-ui/roda-wui/src/main/resources/config/theme/Welcome.html /tmp/welcome-en-before.html
```

- [ ] **Step 2: Ersätt `Welcome.html` med samma struktur som svenska, engelska texter**

Kopiera markup från Task 7 Step 2 men ersätt alla svenska strängar:
- "God dag" → "Good day"
- "Välkommen till ETERNA" → "Welcome to ETERNA"
- Lead: "An open-source Swedish e-archive for long-term digital preservation. Built on OAIS, E-ARK, FGS, PREMIS and METS."
- Scope-label "I arkivet" → "In the archive"
- Options: "Allt"→"All", "Katalog"→"Catalogue", "Leveranser"→"Deliveries"
- Placeholder "Sök i arkivet…" → "Search the archive…"
- Button "Sök" → "Search"
- "Snabbåtgärder" → "Quick actions"
- "Lämna in material"→"Deliver material", desc "Skapa en ny leverans till arkivet"→"Create a new delivery to the archive"
- "Granska leveranser"→"Review deliveries", desc "Pågående och väntande leveranser"→"Ongoing and pending deliveries"
- "Sök i katalogen"→"Search the catalogue", desc "Hitta arkivpaket och beskrivningar"→"Find archival packages and descriptions"
- "Administrera åtkomst"→"Manage access", desc "Användare, grupper och behörigheter"→"Users, groups and permissions"
- "Senaste händelser" → "Recent activity"
- Aktivitetsrader: "Leverans godkänd"→"Delivery accepted", desc "Arkivpaket från Region Öst validerades utan fel."→"Archival package from Region East validated without errors."
- "Bevarandejobb väntar"→"Preservation job pending", desc "Formatkontroll avvaktar manuell granskning."→"Format check awaiting manual review."
- "Leverans avvisad"→"Delivery rejected", desc "SIP-paket saknar METS-manifest."→"SIP package is missing METS manifest."
- Note: "Visar senaste händelser när logg-kopplingen aktiveras. Tills dess renderas exempeldata."→"Shows recent activity once the log integration is enabled. Sample data until then."
- "Arkivet i siffror"→"Archive in numbers"
- Statistikrubriker: "Arkivpaket totalt"→"Archival packages", "Levererat denna månad"→"Delivered this month", "Total lagring"→"Total storage", "Pågående bevarandejobb"→"Active preservation jobs"
- "Arkivet uppfyller"→"Standards compliance"
- "Dokumentation"→"Documentation"
- Dokumentations-paragraf: "Handbooks, standards and developer documentation are available on [GitHub]."

- [ ] **Step 3: Verifiera i browser med engelska som locale**

Byt UI-språk till English (om det går via användarprofil eller `?locale=en` i URL) och kontrollera att engelska Welcome renderas identiskt i layout men med engelska strängar.

- [ ] **Step 4: Commit**

```bash
git add roda-ui/roda-wui/src/main/resources/config/theme/Welcome.html
git commit -m "feat(welcome): mirror facelift structure in English Welcome fallback"
```

---

## Task 9: Footer — svensk + engelsk

**Files:**
- Modify: `roda-ui/roda-wui/src/main/resources/config/theme/Footer_sv_SE.html`
- Modify: `roda-ui/roda-wui/src/main/resources/config/theme/Footer.html`

- [ ] **Step 1: Läs nuvarande footrar**

```bash
cat roda-ui/roda-wui/src/main/resources/config/theme/Footer_sv_SE.html
cat roda-ui/roda-wui/src/main/resources/config/theme/Footer.html
```

- [ ] **Step 2: Ersätt `Footer_sv_SE.html`**

```html
<footer class="eterna-footer">
  <div class="eterna-footer__left">
    <span class="eterna-footer__product">ETERNA v1.0-alpha</span>
    <span class="eterna-footer__sep">·</span>
    <span class="eterna-footer__attr">© WhiteRed</span>
    <span class="eterna-footer__sep">·</span>
    <a href="https://github.com/ETERNA-earkiv/ETERNA">Dokumentation</a>
  </div>
  <div class="eterna-footer__right">
    <span class="eterna-status-dot eterna-status-dot--ok" aria-hidden="true"></span>
    <span>Alla tjänster fungerar normalt</span>
  </div>
</footer>
```

- [ ] **Step 3: Ersätt `Footer.html` (engelska)**

Samma struktur, engelska strängar: "Documentation", "All systems operational".

- [ ] **Step 4: Lägg till footer-stilar i `theme.css`**

```css
/* === ETERNA footer === */
.eterna-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 32px;
  border-top: 1px solid var(--eterna-border);
  background: var(--eterna-surface);
  font-size: 13px;
  color: var(--eterna-slate);
}
.eterna-footer__sep { opacity: 0.5; margin: 0 4px; }
.eterna-footer a { color: var(--eterna-red); text-decoration: none; }
.eterna-footer a:hover { text-decoration: underline; }
.eterna-status-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 8px;
  vertical-align: middle;
}
.eterna-status-dot--ok { background: var(--eterna-ok); }
.eterna-status-dot--warn { background: var(--eterna-warn); }
.eterna-status-dot--err { background: var(--eterna-red); }
```

- [ ] **Step 5: Reload och kontrollera footer på Welcome**

Expected: footer längst ner med produktrad vänster och statusindikator höger.

- [ ] **Step 6: Commit**

```bash
git add roda-ui/roda-wui/src/main/resources/config/theme/Footer_sv_SE.html \
        roda-ui/roda-wui/src/main/resources/config/theme/Footer.html \
        roda-ui/roda-wui/src/main/resources/config/theme/theme.css
git commit -m "feat(theme): new ETERNA footer with version, attribution and status"
```

---

## Task 10: Error-sidor — 401, 404, 500, InactiveAccount

**Files:**
- Create: `roda-ui/roda-wui/src/main/resources/config/theme/Error401_sv_SE.html`
- Create: `roda-ui/roda-wui/src/main/resources/config/theme/Error401.html`
- Modify: `roda-ui/roda-wui/src/main/resources/config/theme/Error404_sv_SE.html`
- Modify: `roda-ui/roda-wui/src/main/resources/config/theme/Error500_sv_SE.html`
- Modify: `roda-ui/roda-wui/src/main/resources/config/theme/ErrorInactiveAccount_sv_SE.html`
- Modify: `roda-ui/roda-wui/src/main/resources/config/theme/theme.css`

- [ ] **Step 1: Läs befintliga error-sidor som mall**

```bash
cat roda-ui/roda-wui/src/main/resources/config/theme/Error404_sv_SE.html
cat roda-ui/roda-wui/src/main/resources/config/theme/Error500_sv_SE.html
cat roda-ui/roda-wui/src/main/resources/config/theme/ErrorInactiveAccount_sv_SE.html
```

- [ ] **Step 2: Ersätt Error404_sv_SE.html**

```html
<div class="eterna-error">
  <img class="eterna-error__flag"
       src="api/v2/themes?resource-id=eterna-flag.svg"
       alt="" aria-hidden="true" />
  <p class="eterna-error__code">404</p>
  <h1 class="eterna-error__title">Sidan finns inte</h1>
  <p class="eterna-error__desc">
    Sidan du letar efter finns inte eller har flyttats. Gå tillbaka till
    <a href="#welcome">Översikten</a> eller sök i arkivet.
  </p>
</div>
```

- [ ] **Step 3: Ersätt Error500_sv_SE.html**

Samma struktur, men:
- code "500"
- title "Ett fel uppstod i tjänsten"
- desc: "Ett internt fel inträffade. Försök igen om en stund eller kontakta din administratör om problemet kvarstår."

- [ ] **Step 4: Skapa Error401_sv_SE.html (finns inte idag)**

Samma struktur:
- code "401"
- title "Åtkomst nekad"
- desc: "Du har inte behörighet att se denna sida. <a href='#login'>Logga in</a> eller kontakta din administratör."

- [ ] **Step 5: Skapa Error401.html (engelska)**

Samma struktur, engelska strängar: "Access denied", "You do not have permission to view this page. [Sign in] or contact your administrator."

- [ ] **Step 6: Ersätt ErrorInactiveAccount_sv_SE.html**

- title "Kontot är inaktivt"
- desc: "Ditt användarkonto är inaktiverat. Kontakta din administratör för att återaktivera åtkomsten."

- [ ] **Step 7: Lägg till error-page-stilar i `theme.css`**

```css
/* === ETERNA error pages === */
.eterna-error {
  max-width: 520px;
  margin: 80px auto;
  padding: 48px 32px;
  text-align: center;
}
.eterna-error__flag {
  width: 72px;
  height: 72px;
  opacity: 0.9;
  margin-bottom: 24px;
}
.eterna-error__code {
  font-family: var(--eterna-font-display);
  font-weight: 700;
  font-size: 64px;
  line-height: 1;
  color: var(--eterna-red);
  letter-spacing: 0.04em;
  margin: 0 0 12px;
}
.eterna-error__title {
  font-family: var(--eterna-font-display);
  font-weight: 600;
  font-size: 28px;
  color: var(--eterna-ink);
  margin: 0 0 12px;
}
.eterna-error__desc {
  font-size: 15px;
  color: var(--eterna-slate);
  line-height: 1.55;
  margin: 0;
}
.eterna-error__desc a {
  color: var(--eterna-red);
  text-decoration: none;
  font-weight: 500;
}
.eterna-error__desc a:hover { text-decoration: underline; }
```

- [ ] **Step 8: Verifiera alla error-sidor i browser**

Öppna:
- http://localhost:8080/#theme/Error401_sv_SE.html (eller utlös 401 via direkt API-anrop)
- http://localhost:8080/#theme/Error404_sv_SE.html
- http://localhost:8080/#theme/Error500_sv_SE.html

Expected: signalflagga + stor röd sifferkod + rubrik + beskrivning, centrerad layout.

- [ ] **Step 9: Commit**

```bash
git add roda-ui/roda-wui/src/main/resources/config/theme/Error*.html \
        roda-ui/roda-wui/src/main/resources/config/theme/theme.css
git commit -m "feat(theme): restyle error pages with ETERNA brand and add 401"
```

---

## Task 11: Login-sidan — split-screen struktur (lätt GWT-touch)

**Files:**
- Modify: `roda-ui/roda-wui/src/main/java/org/roda/wui/client/common/UserLogin.java`
- Modify: `roda-ui/roda-wui/src/main/resources/config/theme/theme.css`

- [ ] **Step 1: Läs hela UserLogin.java för att hitta render-metod**

```bash
wc -l roda-ui/roda-wui/src/main/java/org/roda/wui/client/common/UserLogin.java
grep -n "onLoad\|onInitialize\|FlowPanel\|HTMLPanel\|initWidget\|add(" roda-ui/roda-wui/src/main/java/org/roda/wui/client/common/UserLogin.java | head -40
```
Expected: identifiera var top-level `FlowPanel` eller motsvarande `Composite`-root initieras.

- [ ] **Step 2: Hitta var inloggningsformen byggs**

```bash
grep -n "username\|password\|loginButton\|TextBox\|PasswordTextBox" roda-ui/roda-wui/src/main/java/org/roda/wui/client/common/UserLogin.java | head -30
```
Notera radnummer för form-konstruktion.

- [ ] **Step 3: Wrap befintlig form i split-screen struktur utan att ändra logik**

Lokalisera där root-widgeten byggs (troligen `FlowPanel root = new FlowPanel();` eller `initWidget(...)`). Omslut ALLT existerande innehåll med två syskon-paneler:

```java
// --- ETERNA facelift split-screen wrap ---
FlowPanel splitRoot = new FlowPanel();
splitRoot.addStyleName("eterna-login");

FlowPanel brandSide = new FlowPanel();
brandSide.addStyleName("eterna-login__brand-side");
brandSide.add(new HTML(
  "<img src=\"api/v2/themes?resource-id=eterna-logo-reversed.svg\" " +
  "alt=\"ETERNA\" class=\"eterna-login__logo\">" +
  "<p class=\"eterna-login__byline\">ett e-arkiv från WhiteRed</p>" +
  "<p class=\"eterna-login__tagline\">Digitalt bevarande som " +
  "<span class=\"eterna-accent\">står sig</span> över tid.</p>" +
  "<p class=\"eterna-login__lead\">Öppen källkod, svensk dokumentation, " +
  "byggd på internationella standarder för arkivering.</p>" +
  "<p class=\"eterna-login__standards\">OAIS · E-ARK · FGS · PREMIS · METS</p>"
));

FlowPanel formSide = new FlowPanel();
formSide.addStyleName("eterna-login__form-side");
// Flytta ALLA existerande children från gamla root-panelen hit:
// for each existing widget in old root: formSide.add(widget);

splitRoot.add(brandSide);
splitRoot.add(formSide);
// Ersätt gamla root med splitRoot i initWidget(...) / add(...)
```

**Viktigt:** Ingen inloggningslogik, ingen event-handler, ingen fält-konstruktion ändras — endast den omslutande DOM-strukturen.

- [ ] **Step 4: Lägg till login-stilar i `theme.css`**

```css
/* === ETERNA login (facelift 2026-04) === */
.eterna-login {
  display: grid;
  grid-template-columns: 1fr 1fr;
  min-height: 100vh;
}
@media (max-width: 780px) {
  .eterna-login { grid-template-columns: 1fr; }
  .eterna-login__brand-side { min-height: 240px; }
}
.eterna-login__brand-side {
  background: var(--eterna-dark);
  color: var(--eterna-paper);
  padding: 64px 48px;
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}
.eterna-login__brand-side::before {
  content: '';
  position: absolute;
  right: -140px;
  top: -140px;
  width: 420px;
  height: 420px;
  border: 60px solid rgba(226, 0, 26, 0.10);
  border-radius: 50%;
  pointer-events: none;
}
.eterna-login__logo {
  width: 240px;
  height: auto;
  display: block;
  margin-bottom: 24px;
}
.eterna-login__byline {
  font-size: 12px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.6);
  margin: 0 0 32px;
}
.eterna-login__tagline {
  font-family: var(--eterna-font-display);
  font-weight: 500;
  font-size: 34px;
  line-height: 1.15;
  max-width: 340px;
  color: var(--eterna-paper);
  margin: 0 0 14px;
}
.eterna-login__tagline .eterna-accent { color: var(--eterna-red); }
.eterna-login__lead {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
  max-width: 360px;
  line-height: 1.55;
  margin: 0 0 24px;
}
.eterna-login__standards {
  font-size: 11px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.5);
  margin: 0;
  position: relative;
  z-index: 1;
}
.eterna-login__form-side {
  background: var(--eterna-paper);
  padding: 64px 56px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
```

- [ ] **Step 5: GWT-kompilera och verifiera login**

```bash
cd ~/ETERNA && mvn -pl roda-ui/roda-wui clean install -DskipTests -Pstandalone
```

Sedan starta om dev-servern och navigera till http://localhost:8080 (utloggad).

Expected:
- Split-screen 50/50
- Mörk vänstersida med reversed ETERNA-logo, byline, tagline med röd accent, standards-rad
- Vit högersida med formulärfält (username/password) och "Logga in"-knapp
- Röd topp-list 6 px ovan
- Röd cirkel-dekor subtilt i vänstersidans hörn

- [ ] **Step 6: Testa login-funktionalitet**

Logga in med giltigt konto. Expected: fortfarande fungerar, redirectar till Welcome.
Logga in med fel lösenord. Expected: felmeddelande visas precis som tidigare.

- [ ] **Step 7: Ta after-screenshot `login-after.png`**

- [ ] **Step 8: Commit**

```bash
git add roda-ui/roda-wui/src/main/java/org/roda/wui/client/common/UserLogin.java \
        roda-ui/roda-wui/src/main/resources/config/theme/theme.css \
        documentation/development/design-proposals/baseline-2026-04-22/login-after.png
git commit -m "feat(login): split-screen ETERNA brand-side + form-side layout"
```

---

## Task 12: Svenska texter — vokabulärbyte i .properties

**Files:**
- Modify: `roda-ui/roda-wui/src/main/resources/config/i18n/client/ClientMessages_sv_SE.properties`
- Modify: `roda-ui/roda-wui/src/main/resources/config/i18n/ServerMessages_sv_SE.properties`

Notera: Orden ska bytas enligt vokabulärtabellen i specens avsnitt 3.4. Eftersom `.properties` har ~1758 rader görs bytet stegvis med verifierade sökträffar — inte blind sed-replace.

- [ ] **Step 1: Identifiera nuvarande nyckel/värde-par som matchar vokabulärtabellen**

```bash
cd ~/ETERNA/roda-ui/roda-wui/src/main/resources/config/i18n/client
for term in "Översikt" "Dashboard" "Ingest" "Leverans" "AIP" "Arkivpaket" "Uppladdning" "Leverera" "Lämna in" "Administration" "Preservation" "Bevarande" "Disposal" "Gallring" "Katalog" "Catalogue" "Sök" "Logout" "Logga ut"; do
  echo "=== $term ==="
  grep -n "$term" ClientMessages_sv_SE.properties | head -5
done
```

- [ ] **Step 2: Gör punktersättningar för kändra RODA-RODA-ord**

Öppna filen i editor. För varje träff nedan, ersätt värde (höger om `=`) enligt tabell:

| Hitta (svensk text idag) | Byt till |
|--------------------------|----------|
| `Instrumentpanel` eller `Dashboard` | `Översikt` |
| `Intag` eller `Ingest` i rubrik | `Leverans` |
| `Lämna in` — behåll |
| `AIP` i brödtext (ej tekniska ID-fält) | `Arkivpaket` |
| `Katalog` — behåll |
| `Bevarande` — behåll (kolla att PreservationEvents → Bevarandehändelser) |
| `Gallringsplaner` — behåll (kolla Disposal schedules) |
| `Sök` — behåll |
| `Logga ut` — behåll |
| `Välkommen till RODA` | `Välkommen till ETERNA` |

**Regel:** ändra endast värden som är synliga UI-strängar. Ändra aldrig nyckelnamnet (vänster om `=`). Ändra inte tekniska termer (OAIS, METS, PREMIS, SIP, AIP, DIP, FGS, E-ARK).

- [ ] **Step 3: Kör diff mot tidigare och granska**

```bash
git diff --stat roda-ui/roda-wui/src/main/resources/config/i18n/client/ClientMessages_sv_SE.properties
git diff roda-ui/roda-wui/src/main/resources/config/i18n/client/ClientMessages_sv_SE.properties | head -80
```
Expected: ~20-40 rader ändrade. Endast värden efter `=`.

- [ ] **Step 4: Upprepa för `ServerMessages_sv_SE.properties`**

```bash
grep -n "RODA\|Ingest\|Dashboard\|AIP" roda-ui/roda-wui/src/main/resources/config/i18n/ServerMessages_sv_SE.properties | head -30
```
Byt enligt samma regler.

- [ ] **Step 5: Bygg och verifiera**

```bash
cd ~/ETERNA && mvn -pl roda-ui/roda-wui clean install -DskipTests -Pstandalone
```
Starta om dev-servern. Navigera till Welcome, top-nav-flikar och catalogue. Expected: svenska strängar reflekterar ny vokabulär.

- [ ] **Step 6: Commit**

```bash
git add roda-ui/roda-wui/src/main/resources/config/i18n/client/ClientMessages_sv_SE.properties \
        roda-ui/roda-wui/src/main/resources/config/i18n/ServerMessages_sv_SE.properties
git commit -m "feat(i18n): swap vocabulary to Swedish archival tone (Leverans, Arkivpaket, Översikt)"
```

---

## Task 13: Top-shell header — flagga + wordmark-lockup (lätt GWT-touch)

**Files:**
- Identify: header-klassen under `roda-ui/roda-wui/src/main/java/org/roda/wui/client/main/` eller liknande
- Modify: identifierad header-klass
- Modify: `roda-ui/roda-wui/src/main/resources/config/theme/theme.css`

- [ ] **Step 1: Hitta header-klassen**

```bash
cd ~/ETERNA
grep -rn "Banner\|whitered-logo\|eterna-logo" roda-ui/roda-wui/src/main/java/ | head -20
find roda-ui/roda-wui/src/main/java -iname "*Header*" -o -iname "*TopBar*" -o -iname "*NavBar*" | head -10
```
Expected: identifiera Java-klass som injicerar Banner.html eller logo-img.

- [ ] **Step 2: Om header hämtar Banner.html dynamiskt — ingen Java-ändring krävs**

Om grep visar att header redan använder `theme/Banner_sv_SE.html` (exempelvis via `ThemeResource` eller `HTMLResource.fetchTheme`), är vår Banner-lockup från Task 6 redan aktiv. Hoppa till Step 4.

- [ ] **Step 3: Om header har hårdkodat `<img src="eterna-logo.svg">` — ersätt med lockup**

Lokalisera den kodrad som sätter logo-img. Ersätt med en HTMLPanel som laddar `Banner_sv_SE.html` via befintlig theme-resolver, ELLER bygg lockup inline:

```java
// Gammal rad (exempel):
// Image logo = new Image("api/v2/themes?resource-id=eterna-logo.svg");
// logo.addStyleName("app-logo");

// Ny rad:
HTML lockup = new HTML(
  "<span class=\"eterna-banner\">" +
  "<img src=\"api/v2/themes?resource-id=eterna-flag.svg\" alt=\"\" aria-hidden=\"true\" class=\"eterna-banner__flag\" />" +
  "<span class=\"eterna-banner__wordmark\">ETERNA</span>" +
  "</span>"
);
lockup.addStyleName("app-logo");
```

Ingen annan navigations- eller menylogik ändras.

- [ ] **Step 4: Kompilera, reload, verifiera header**

```bash
cd ~/ETERNA && mvn -pl roda-ui/roda-wui clean install -DskipTests -Pstandalone
```
Logga in. Navigera till Welcome. Expected: header visar signalflagga till vänster om "ETERNA" i top-shell. Navigationslänkar (Översikt, Katalog osv.) är oförändrade.

- [ ] **Step 5: Commit**

```bash
git add roda-ui/roda-wui/src/main/java/... # exakt fil från Step 1
git add roda-ui/roda-wui/src/main/resources/config/theme/theme.css
git commit -m "feat(top-shell): replace logo img with ETERNA flag+wordmark lockup"
```

---

## Task 14: Spinner-färg och polish

**Files:**
- Modify: `roda-ui/roda-wui/src/main/resources/config/theme/spinner.css`

- [ ] **Step 1: Läs nuvarande spinner.css**

```bash
cat roda-ui/roda-wui/src/main/resources/config/theme/spinner.css
```

- [ ] **Step 2: Byt spinner-färgen till ETERNA Red**

Identifiera CSS-property som styr stroke/border-färgen (troligen `border-top-color` eller `stroke`). Ersätt värdet med `var(--eterna-red)`. Om variabeln inte är tillgänglig i spinner.css (importeras separat), använd literalvärdet `#E2001A`.

- [ ] **Step 3: Verifiera spinner i browser**

Utlös en laddning (exempelvis ladda om catalogue med en tung sökning). Expected: spinner renderas i ETERNA-röd istället för RODA-grått.

- [ ] **Step 4: Commit**

```bash
git add roda-ui/roda-wui/src/main/resources/config/theme/spinner.css
git commit -m "feat(theme): spinner uses ETERNA Red"
```

---

## Task 15: Main.html head — favicon och webfont preload

**Files:**
- Modify: `roda-ui/roda-wui/src/main/resources/config/theme/static/Main.html`

- [ ] **Step 1: Läs Main.html**

```bash
cat roda-ui/roda-wui/src/main/resources/config/theme/static/Main.html
```

- [ ] **Step 2: Uppdatera `<head>` med nya favicon-referenser och webfont-preload**

Hitta `<head>` och ersätt favicon-raden + lägg till preload:

```html
<link rel="icon" type="image/png" sizes="16x16"
      href="api/v2/themes?resource-id=favicon/favicon-16x16.png">
<link rel="icon" type="image/png" sizes="32x32"
      href="api/v2/themes?resource-id=favicon/favicon-32x32.png">
<link rel="shortcut icon"
      href="api/v2/themes?resource-id=favicon/favicon.ico">

<link rel="preload"
      href="api/v2/themes?resource-id=fonts/BarlowCondensed/BarlowCondensed-SemiBold.woff2"
      as="font" type="font/woff2" crossorigin>
<link rel="preload"
      href="api/v2/themes?resource-id=fonts/Inter/Inter-VariableFont_opsz,wght.ttf"
      as="font" type="font/ttf" crossorigin>
```

- [ ] **Step 3: Verifiera favicon visas i browser-tabben**

Hård-reload. Expected: signalflagge-favicon i tab-ikonen.

- [ ] **Step 4: Verifiera i DevTools att webfonts preloadas**

DevTools → Network → filter "font". Expected: Barlow SemiBold och Inter laddas tidigt (FCP-timing).

- [ ] **Step 5: Commit**

```bash
git add roda-ui/roda-wui/src/main/resources/config/theme/static/Main.html
git commit -m "feat(theme): update favicon to ETERNA flag and preload core webfonts"
```

---

## Task 16: Bonus — empty state med signalflagga

**Files:**
- Modify: `roda-ui/roda-wui/src/main/resources/config/theme/theme.css`

- [ ] **Step 1: Lägg till generisk empty-state-klass**

```css
/* === ETERNA empty state === */
.eterna-empty {
  text-align: center;
  padding: 64px 24px;
  color: var(--eterna-muted);
}
.eterna-empty::before {
  content: '';
  display: block;
  width: 80px;
  height: 80px;
  margin: 0 auto 20px;
  background: url('api/v2/themes?resource-id=eterna-flag.svg') center/contain no-repeat;
  opacity: 0.35;
}
.eterna-empty__title {
  font-family: var(--eterna-font-display);
  font-weight: 600;
  font-size: 20px;
  color: var(--eterna-slate);
  margin: 0 0 6px;
}
.eterna-empty__desc {
  font-size: 14px;
  margin: 0;
}
```

- [ ] **Step 2: Kontrollera att klassen inte krockar med befintliga RODA empty states**

```bash
grep -rn "empty\|no-results" roda-ui/roda-wui/src/main/resources/config/theme/ | head -20
```
Om klassen `.empty` eller `.no-results` redan finns i RODA — använd `.eterna-empty` som namespace och tillämpa den endast i våra nya mallar. Ingen rebrand av befintliga empty states krävs i denna iteration.

- [ ] **Step 3: Commit**

```bash
git add roda-ui/roda-wui/src/main/resources/config/theme/theme.css
git commit -m "feat(theme): add ETERNA empty-state with flag motif"
```

---

## Task 17: WCAG-audit och regressionskontroll

**Files:** inga — endast verifikation.

- [ ] **Step 1: Kontrastmätning av alla textkombinationer**

Gå igenom specens tabell i avsnitt 6:

| Kombination | Förväntad kontrast |
|-------------|--------------------|
| Ink på vit | ≥ 14.6:1 |
| Slate på vit | ≥ 7.5:1 |
| Muted på vit | ≥ 5.7:1 |
| Red på vit (≥18px) | ≥ 4.6:1 |
| Vit på Dark (#111827) | > 15:1 |
| Red på Dark | ≥ 4.5:1 |

Använd https://webaim.org/resources/contrastchecker/ eller DevTools "Inspect" → Accessibility. Dokumentera resultat i en kort tabell i PR-beskrivningen.

- [ ] **Step 2: Verifiera att Brand Gray `#9C9E9F` aldrig används som textfärg**

```bash
grep -rn "9C9E9F\|#9c9e9f" roda-ui/roda-wui/src/main/resources/config/theme/*.css roda-ui/roda-wui/src/main/resources/config/theme/*.html | grep -v "logo\|svg\|decor\|border"
```
Expected: endast träffar i logo-sammanhang eller tom output. Om en text-related träff finns → byt färg.

- [ ] **Step 3: Tangentbordsnavigering — login + Welcome**

Starta från login. Tab genom formen. Expected: fokus-ring synlig på varje fält, tab-ordning logisk.

Logga in. Tab genom top-nav, sökruta, action cards. Expected: fokus-ring synlig överallt.

- [ ] **Step 4: Screen reader smoke test**

Starta NVDA (Windows) eller VoiceOver (macOS). Ladda Welcome. Expected: "ETERNA ett e-arkiv från WhiteRed" läses upp för banner; rubrik "Välkommen till ETERNA" läses; "Sök i arkivet sökfält" läses.

Om signalflaggan läses upp som bild — kontrollera att `alt=""` + `aria-hidden="true"` är satt i Banner.

- [ ] **Step 5: Zoom-test**

Browser → zoom till 200 %. Expected: allt innehåll förblir läsbart, ingen horisontell scroll på vanliga vyer.

- [ ] **Step 6: Kör full mvn-build och alla junit-tester**

```bash
cd ~/ETERNA && mvn clean install
```
Expected: BUILD SUCCESS. Om någon GWT-test failar på grund av vår UserLogin-wrap → fixa testet eller backa strukturen.

- [ ] **Step 7: Commit audit-rapport om något hittats**

Om audit triggade fixes — commit dem med prefix `fix(a11y):`.

---

## Task 18: PR-förberedelser — screenshots, dokumentation, PR-beskrivning

**Files:**
- Create: `documentation/development/design-proposals/baseline-2026-04-22/compare.md` (före/efter-tabell)

- [ ] **Step 1: Samla alla after-screenshots**

Säkerställ att följande finns:
- `login-before.png` + `login-after.png`
- `welcome-before.png` + `welcome-after.png`
- `error404-before.png` + `error404-after.png`

```bash
ls documentation/development/design-proposals/baseline-2026-04-22/
```

- [ ] **Step 2: Skriv en kort jämförelse-md**

Skapa `documentation/development/design-proposals/baseline-2026-04-22/compare.md`:

```markdown
# Visuell facelift 2026-04 — före/efter

Nedan visas de tre nyckelvyer som fått faceliften. Skärmdumpar togs i Chrome på localhost:8080 med standardzoom.

## Login
| Före | Efter |
|------|-------|
| ![](login-before.png) | ![](login-after.png) |

## Welcome (startsida)
| Före | Efter |
|------|-------|
| ![](welcome-before.png) | ![](welcome-after.png) |

## Error 404
| Före | Efter |
|------|-------|
| ![](error404-before.png) | ![](error404-after.png) |
```

- [ ] **Step 3: Sammanställ commit-historik för PR-beskrivning**

```bash
git log --oneline eterna-v1-alpha..HEAD
```
Expected: ordnad lista av commits från Task 0 till Task 17.

- [ ] **Step 4: Pusha branch och öppna PR mot `eterna-v1-alpha`**

```bash
git push -u origin facelift/visual-v1
gh pr create --base eterna-v1-alpha --title "feat: ETERNA visuell facelift v1" --body "$(cat <<'EOF'
## Sammanfattning
Visuell facelift av ETERNA enligt godkänd spec i `documentation/development/design-proposals/2026-04-22-visual-facelift.md`.

Ändringarna är additiva och omfattar:
- Ny färgpalett + typografi (Barlow Condensed + Inter) via CSS-variabler
- Split-screen login med reversed ETERNA-logotyp och signalflagga
- Ny Welcome-startsida (hero, sök, snabbåtgärder, standards-kort)
- Banner-lockup flagga + wordmark i top-shell
- Rebrandade error-sidor (401, 404, 500)
- Svensk arkivvokabulär (Översikt, Leverans, Arkivpaket) i `.properties`
- Ny favicon från signalflaggan

Ingen ändring av inloggningslogik, navigation, API, datamodell eller plugins.

## Teststeg
- [ ] Logga in som testanvändare → Welcome renderar ny hero + åtgärdsgrid
- [ ] Navigera till Katalog, Leverans, Administration → oförändrad layout men nya texter
- [ ] Forcera 404 → ny error-sida med flagga och röd sifferkod
- [ ] Tab genom login → fokus-ring röd synlig
- [ ] Zoom 200 % → inget innehåll faller av

Full spec: `documentation/development/design-proposals/2026-04-22-visual-facelift.md`
Implementationsplan: `documentation/development/plans/2026-04-22-visual-facelift-implementation.md`
Före/efter: `documentation/development/design-proposals/baseline-2026-04-22/compare.md`
EOF
)"
```

- [ ] **Step 5: Notera PR-URL och dela med community enligt ETERNA Issue First-regeln**

Om detta ska RFC-stämplas före merge: öppna ett GitHub issue med länk till PR och bjud in bidragsgivare att kommentera innan merge (Oscar bedömer per community-process).

---

## Sammanfattning av antal tasks

| # | Task | Fokus | GWT-touch |
|---|------|-------|-----------|
| 0 | Förberedelser | branch + baseline | nej |
| 1 | Brand tokens | CSS-variabler | nej |
| 2 | Webfonts | Barlow + Inter | nej |
| 3 | Typografi + röd topp-list | global | nej |
| 4 | Brand-assets | logo, flag, favicon | nej |
| 5 | Komponenter | knapp, input, card, fokus | nej |
| 6 | Banner | lockup | nej |
| 7 | Welcome sv | dashboard | nej |
| 8 | Welcome en | fallback | nej |
| 9 | Footer | sv + en | nej |
| 10 | Error-sidor | 401, 404, 500 | nej |
| 11 | Login | split-screen | ja (struktur) |
| 12 | Texter | vokabulär | nej |
| 13 | Top-shell | header-lockup | ja (struktur) |
| 14 | Spinner | färg | nej |
| 15 | Main.html head | favicon + preload | nej |
| 16 | Empty state | bonus | nej |
| 17 | WCAG-audit | verifikation | nej |
| 18 | PR | screenshots + PR | nej |

Totalt 19 tasks (0–18). Kritiska GWT-touches: endast Task 11 (login) och Task 13 (top-shell, om hårdkodat).

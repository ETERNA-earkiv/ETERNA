# Designspec: Katalogträd i ETERNA

**Datum:** 2026-04-17
**Status:** Godkänd

---

## Bakgrund

Katalogstrukturen i ETERNA visas idag bara som en breadcrumb-banner längst upp i gränssnittet. Användare saknar möjlighet att se och navigera hela arkivhierarkin utan att klicka sig fram nod för nod.

## Mål

Lägga till en alltid synlig vänster sidopanel med en trädvy över AIP-hierarkin i katalogvyn. Trädet ska automatiskt synkroniseras med navigeringen — när en användare navigerar till ett AIP markeras och expanderas rätt nod i trädet.

## Scope

- Trädet visar **enbart AIP-hierarkin** — representationer och filer visas inte
- Trädet är synligt **i katalogstartsidan** (`BrowseTop`) och **AIP-sidan** (`BrowseAIP`)
- Arkitekturen ska göra det möjligt att aktivera trädet på fler sidor utan omskrivning

## Layout

Ny treledad layout i katalogvyn:

```text
[ Katalogträd (vänster) | Huvudinnehåll (mitten) | Sidopanel (höger) ]
```

- **Katalogträd:** ny `FlowPanel` med klassen `catalogTreePanel`, vit bakgrund, röd header (`#e2001a`), standardbredd 260 px
- **Huvudinnehåll:** oförändrat
- **Höger sidopanel** (`browseSidePanel`, `#343A3F`): oförändrad
- En 3 px bred resize-handle separerar trädet från huvudinnehållet; röd vid hover
- Minsta trädbredd: 150 px, maximal: 480 px

## Komponenter (frontend, GWT)

### `CatalogTreePanel` (Composite)

Toppnivå-widget. Ansvarar för:
- Rendera header, filterinput och scrollbar trädvy
- Ta emot navigationshändelser (vilket AIP är aktivt) och trigga auto-synk
- Exponera `void revealAip(String aipId)` som anropas av `BrowseAIP` vid navigering

Intern struktur:
```text
CatalogTreePanel
├── Header ("Katalog")
├── Filterinput (lokal filtrering av laddade noder)
└── CatalogTreeBody (ScrollPanel)
    └── CatalogTreeNode (rekursiv, en per AIP-nod)
```

### `CatalogTreeNode` (Composite)

En nod i trädet. Varje nod ansvarar för:
- Visa toggle-pil, ikon, label och barn-badge
- Lazy-ladda barn via `AsyncCallback` när noden expanderas första gången
- Visa spinner under laddning, felmeddelande vid misslyckande

**Nodtillstånd:**

| Tillstånd | Ikon | Toggle |
|---|---|---|
| Stängd, har barn | 📁 | ▶ |
| Öppen, har barn | 📂 | ▼ |
| Löv (inga barn) | 📄 | — |
| Laddar barn | 📁 | spinner |

**Felfall vid laddning:** Texten "Kunde inte hämta innehåll" visas under noden med en "Försök igen"-länk. Noden förblir stängd.

### CSS-klasser (nya)

| Klass | Syfte |
|---|---|
| `.catalogTreePanel` | Yttre container, vit bakgrund, border-right |
| `.catalogTreePanelHeader` | Mörkgrå header (`#343A3F`), röd ikon |
| `.catalogTreeNode` | En nod-rad, hover `#EDEDED` |
| `.catalogTreeNode.selected` | Markerad nod, bakgrund `#e2001a`, vit text |
| `.catalogTreeNodeChildren` | Container för barn, dold tills öppnad |

Alla klasser läggs till i `main.gss`.

## Auto-synk

När `BrowseAIP` laddar ett AIP anropar den `CatalogTreePanel.revealAip(aipId)`:

1. `CatalogTreePanel` anropar `GET /api/v2/aips/{id}/ancestors`
2. Svaret är en ordnad lista `[rot, ..., direkt förälder]` med `{id, title}` per nod
3. Trädet expanderar varje nod i listan uppifrån och ned, med lazy loading om noden inte redan är laddad
4. Slutnoden (det aktiva AIP:et) markeras med klassen `selected` och scrollas in i vyn

**Felfall vid auto-synk:** Om ancestor-anropet misslyckas uteblir synken tyst — trädet förblir i sitt nuvarande tillstånd. Ingen krasch, inget felmeddelande till användaren (synken är ett hjälpmedel, inte kritisk funktionalitet).

## Backend

### Initial laddning av rotnoder

Vid initialisering anropar `CatalogTreePanel`:

```http
GET /api/v2/aips?parentId=&fields=id,title,hasChildren
```

Tom `parentId` returnerar AIP:er utan förälder (rotnivån). Om det finns flera parallella katalogstrukturer visas alla som separata rotnoder.

### Befintlig endpoint (används för lazy loading av barn)

```http
GET /api/v2/aips?parentId={id}&fields=id,title,hasChildren
```

Returnerar direkta barn. Används när en nod expanderas. **Obs:** Fältet `hasChildren` kan behöva läggas till i svaret om det inte redan finns — det behövs för att avgöra om en nod ska visas som löv eller expanderbar.

### Ny endpoint

```http
GET /api/v2/aips/{id}/ancestors
```

Returnerar den ordnade föräldrakedjan från rotnod till direkt förälder:

```json
[
  { "id": "root-uuid", "title": "Kommunarkivet" },
  { "id": "node-uuid", "title": "21. Ansökan om skolkort" }
]
```

- Rotnoden inkluderas, det begärda AIP:et inkluderas **inte**
- Om AIP:et är ett rotnod returneras tom lista `[]`
- Om AIP:et inte finns returneras `404`

Implementeras i `roda-core` (service-lager) och exponeras via `roda-ui` REST-kontroller. Får inte bryta lagerintegriteten — `roda-ui` anropar service, aldrig Solr direkt.

## i18n

Alla strängar i trädet definieras i:
- `ClientMessages.properties`
- `ClientMessages_sv_SE.properties`

Inga hårdkodade strängar i Java-filer.

Nya nycklar:

| Nyckel | Värde (sv) |
|---|---|
| `catalogTreeTitle` | Katalog |
| `catalogTreeFilterPlaceholder` | Filtrera... |
| `catalogTreeLoadingLabel` | Laddar... |
| `catalogTreeLoadError` | Kunde inte hämta innehåll |
| `catalogTreeRetry` | Försök igen |

## Felhantering (GWT AsyncCallback)

Alla `onFailure`-implementationer:
- Loggar felet via `LOGGER`
- Visar ett användarvänligt meddelande i trädet (ej popup/toast)
- Lämnar trädet i ett konsistent tillstånd

## Testning

### Enhetstester (JUnit, ingen Docker)

- `CatalogTreeNode`: toggle expanderar/kollapsar, löv-nod visar ingen toggle-pil
- Ancestor-service: returnerar korrekt ordnad kedja, tom lista för rotnod

### Integrationstester

- `GET /api/v2/aips/{id}/ancestors`: rätt kedja för känt AIP, tom lista för rotnod, 404 för okänt AIP
- `GET /api/v2/aips?parentId={id}`: oförändrat beteende (regressionstest)

### Manuell verifiering (GWT dev mode)

- Lazy loading: barn hämtas inte förrän nod expanderas
- Auto-synk: navigera till djupt AIP via sökning → trädet expanderar och markerar rätt nod
- Felfall: simulera nätverksfel vid expansion → felmeddelande visas, trädet kraschar inte
- Resize: dra resize-handle → trädbredd justeras, layout håller

## Arkitekturregler som gäller

- `roda-ui` importerar aldrig implementationsklasser från `roda-core` direkt
- Ancestor-logiken implementeras i `roda-core` service-lager
- `CatalogTreePanel` är en fristående widget utan beroenden till övriga browse-komponenter, vilket möjliggör framtida placering på andra sidor

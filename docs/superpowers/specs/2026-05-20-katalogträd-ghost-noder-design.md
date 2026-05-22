# Design: Ghost-noder i katalogträdet för användare med begränsad behörighet

**Datum:** 2026-05-20
**Issues:** [#480](https://github.com/ETERNA-earkiv/ETERNA/issues/480), [#481](https://github.com/ETERNA-earkiv/ETERNA/issues/481)
**Branch:** skapas från `eterna-v1-alpha`

---

## Problembeskrivning

En användare som enbart har behörighet till ett objekt längre ner i katalogstrukturen ser ett tomt katalogträd. Objektet syns i sökvyn men inte i trädet. Orsaken är att `CatalogTreePanel.loadRootNodes()` filtrerar på `EmptyKeyFilterParameter(AIP_PARENT_ID)` — rotnoder utan förälder — och backend filtrerar bort de rotnoder användaren saknar behörighet till. Resultatet blir en tom lista.

Breadcrumb-menyn hanterar redan detta korrekt: förfäder utan behörighet renderas som "spöknoder" (null-värden i ancestor-listan). Trädet saknar motsvarande logik.

---

## Lösning: Fallback-flöde med ghost-noder (Alternativ A)

### Övergripande arkitektur

Tre komponenter berörs:

| Komponent | Förändring |
|---|---|
| `CatalogTreePanel` | Nytt fallback-flöde i `loadRootNodes()` |
| `CatalogTreeNode` | Nytt ghost-tillstånd: grå, ej klickbar, alltid utfälld |
| Backend (`AIPController`, `retrieveAncestors`) | Ingen förändring — returnerar redan `null` för noder utan behörighet |

---

### Dataflöde

```
loadRootNodes()
  └─ Sökning: EmptyKeyFilterParameter(AIP_PARENT_ID)
       ├─ Resultat > 0  →  normalt träd (oförändrat beteende)
       └─ Resultat = 0  →  FALLBACK:
            Sökning utan parent-filter, max 200 träffar, sorterat på titel
            För varje träff:
              getAncestors(aipId) → List<IndexedAIP>  (null = ej behörighet)

            Bygg träd med merge-logik (uppifrån och ned):
              - Nod redan i trädet → återanvänd (delade förfäder slås ihop)
              - null-ancestor       → ghost-nod ("Åtkomst saknas")
              - Leaf-nod (AIP med behörighet) → normal nod

            Ghost-noder renderas:
              - Text: "Åtkomst saknas" (grå/uttonad)
              - Utfälld från start, toggle inaktiverad
              - Ej klickbar
```

---

### Ghost-nodernas utseende

- Text: `"Åtkomst saknas"` (hämtas från `ClientMessages`)
- Visuell stil: grå/uttonad, skiljer sig tydligt från vanliga noder
- Toggle-knapp: inaktiverad eller borttagen (noden kan inte fällas in)
- Klick: ingen action — noden är inte navigerbar
- Utfälld från start: underliggande behöriga noder alltid synliga

Konsekvent med befintlig ghost-node-logik i `BreadcrumbUtils` (rad 123–132).

---

### Merge-logik för delade förfäder

Om två AIPs delar en gemensam förfader (med eller utan behörighet) ska förfadern visas en gång med båda barnen under sig. Implementeras via en `Map<String, CatalogTreeNode>` indexerad på AIP-ID som byggs upp under fallback-flödet.

---

### Felhantering

| Scenario | Hantering |
|---|---|
| Fallback-sökning returnerar 0 resultat | Tom vy — samma beteende som idag |
| `getAncestors()` misslyckas för enskilt AIP | Hoppa över det AIP:t, logga, fortsätt med övriga |
| Fler än 200 behöriga AIPs | Visa de 200 första (sorterade på titel); ingen UI-notis i detta skede |
| Cykliska ancestors | Kan inte uppstå (träd-invariant garanteras av backend) |

---

### Prestanda

Fallback-flödet aktiveras **enbart** om root-sökningen returnerar 0 resultat — alltså bara för användare med starkt begränsad behörighet. Normalflödet (admin och användare med breda behörigheter) påverkas inte.

---

### Berörda filer

| Fil | Förändring |
|---|---|
| `roda-ui/roda-wui/src/main/java/org/roda/wui/client/browse/CatalogTreePanel.java` | Fallback-flöde i `loadRootNodes()`, merge-logik |
| `roda-ui/roda-wui/src/main/java/org/roda/wui/client/browse/CatalogTreeNode.java` | Ghost-tillstånd: konstruktor/factory-metod, rendering, inaktiverad toggle |
| `roda-ui/roda-wui/src/main/resources/config/i18n/ClientMessages.properties` | Ny nyckel: `catalogTree.ghostNode.label = Access denied` |
| `roda-ui/roda-wui/src/main/resources/config/i18n/ClientMessages_sv_SE.properties` | Ny nyckel: `catalogTree.ghostNode.label = Åtkomst saknas` |
| `roda-ui/roda-wui/src/main/webapp/WEB-INF/static/css/main.gss` | Ny CSS-klass `.catalogTreeGhostNode`: grå text, reducerad opacity, cursor default |

---

### Acceptanskriterier (#481)

- [x] Objekt med behörighet visas i trädet även om toppnoden saknar behörighet
- [x] Förfäder utan behörighet renderas som ghost-noder ("Åtkomst saknas")
- [x] Ghost-noder är inte klickbara
- [x] Ghost-noder är utfällda från start och kan inte fällas in
- [x] Beteendet är konsekvent med befintlig ghost-logik i breadcrumb-menyn

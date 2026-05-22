# Katalogträd Ghost-noder — Implementationsplan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Visa accessible AIPs i katalogträdet även när användaren saknar behörighet till rotnoderna, med otillgängliga förfäder renderade som "ghost-noder" (grå, ej klickbara, alltid utfällda).

**Architecture:** `loadRootNodes()` i `CatalogTreePanel` kör ett fallback-flöde om root-sökningen returnerar 0 resultat: hämta alla AIPs användaren har tillgång till (max 200), bygg ancestor-kedjor via `getAncestors()`, och konstruera ett ghost-träd. `CatalogTreeNode` utökas med en ghost-konstruktor och `addPrebuiltChild()`. Noderna kopplas uppifrån och ned; deduplicering sker för reella rotnoder (depth 0) men ej för ghost-noder.

**Tech Stack:** GWT (Java → JS), Spring Boot REST, Solr-index, GWT i18n (ClientMessages), GSS (CSS-in-GWT)

---

## Berörda filer

| Fil | Förändring |
|---|---|
| `roda-ui/roda-wui/src/main/java/config/i18n/client/ClientMessages.java` | Ny metod `catalogTreeGhostNodeLabel()` |
| `roda-ui/roda-wui/src/main/resources/config/i18n/client/ClientMessages.properties` | Ny nyckel `catalogTreeGhostNodeLabel` |
| `roda-ui/roda-wui/src/main/resources/config/i18n/client/ClientMessages_sv_SE.properties` | Ny nyckel `catalogTreeGhostNodeLabel` |
| `roda-ui/roda-wui/src/main/java/org/roda/wui/client/common/resources/main.gss` | Ny CSS-klass `.catalogTreeNode.ghost` |
| `roda-ui/roda-wui/src/main/java/org/roda/wui/client/browse/CatalogTreeNode.java` | Ghost-konstruktor, `addPrebuiltChild()`, `isGhostNode()`, skydda `toggle()`/`collapse()` |
| `roda-ui/roda-wui/src/main/java/org/roda/wui/client/browse/CatalogTreePanel.java` | Fallback i `loadRootNodes()`, `loadFallbackGhostTree()`, `insertFallbackChain()`, `finalizeFallbackTree()`, uppdaterad `applyFilter()` |

---

## Task 0: Skapa feature branch

**Files:** (inga ändringar, bara git)

- [ ] **Steg 1: Skapa och checka ut feature branch**

Kör i WSL från worktree-katalogen:

```bash
wsl -d Ubuntu -- bash -c "cd ~/ETERNA/.worktrees/eterna-v1-alpha-2026-05-20 && git checkout -b feat/480-katalogträd-ghost-noder"
```

Förväntat output: `Switched to a new branch 'feat/480-katalogträd-ghost-noder'`

---

## Task 1: i18n — Ghost-nyckel

**Files:**
- Modify: `roda-ui/roda-wui/src/main/java/config/i18n/client/ClientMessages.java:2728`
- Modify: `roda-ui/roda-wui/src/main/resources/config/i18n/client/ClientMessages.properties:1883`
- Modify: `roda-ui/roda-wui/src/main/resources/config/i18n/client/ClientMessages_sv_SE.properties:1800`

- [ ] **Steg 1: Lägg till metod i ClientMessages.java efter rad 2728**

Befintlig kod (rad 2728–2729):
```java
  String catalogTreeReasonRetrieveAIP();
}
```

Ersätt med:
```java
  String catalogTreeReasonRetrieveAIP();

  String catalogTreeGhostNodeLabel();
}
```

- [ ] **Steg 2: Lägg till engelsk nyckel i ClientMessages.properties efter rad 1883**

Befintlig rad 1883:
```
catalogTreeReasonRetrieveAIP: Retrieve AIP
```

Lägg till efter raden:
```
catalogTreeGhostNodeLabel: Access denied
```

- [ ] **Steg 3: Lägg till svensk nyckel i ClientMessages_sv_SE.properties efter rad 1800**

Befintlig rad 1800:
```
catalogTreeReasonRetrieveAIP: Hämta AIP
```

Lägg till efter raden:
```
catalogTreeGhostNodeLabel: Åtkomst saknas
```

- [ ] **Steg 4: Commit**

```bash
wsl -d Ubuntu -- bash -c "cd ~/ETERNA/.worktrees/eterna-v1-alpha-2026-05-20 && git add roda-ui/roda-wui/src/main/java/config/i18n/client/ClientMessages.java roda-ui/roda-wui/src/main/resources/config/i18n/client/ClientMessages.properties roda-ui/roda-wui/src/main/resources/config/i18n/client/ClientMessages_sv_SE.properties && git commit -m 'feat: i18n-nyckel för ghost-nod i katalogträdet'"
```

---

## Task 2: CSS — Ghost-nodstil

**Files:**
- Modify: `roda-ui/roda-wui/src/main/java/org/roda/wui/client/common/resources/main.gss:3570`

- [ ] **Steg 1: Lägg till ghost-CSS efter `.catalogTreeNodeError`-blocket (rad 3570)**

Befintlig kod (rad 3563–3571):
```css
.catalogTreeNodeError {
    padding: 4px 8px 4px 40px;
    font-size: 11px;
    color: COLOR_PRIMARY;
    display: flex;
    gap: 8px;
    align-items: center;
}

```

Ersätt med:
```css
.catalogTreeNodeError {
    padding: 4px 8px 4px 40px;
    font-size: 11px;
    color: COLOR_PRIMARY;
    display: flex;
    gap: 8px;
    align-items: center;
}

.catalogTreeNode.ghost {
    cursor: default;
    color: #aaaaaa;
    font-style: italic;
}

.catalogTreeNode.ghost:hover {
    background: transparent;
}

.catalogTreeNode.ghost .catalogTreeIcon {
    color: #cccccc;
}

```

- [ ] **Steg 2: Commit**

```bash
wsl -d Ubuntu -- bash -c "cd ~/ETERNA/.worktrees/eterna-v1-alpha-2026-05-20 && git add roda-ui/roda-wui/src/main/java/org/roda/wui/client/common/resources/main.gss && git commit -m 'feat: CSS-stil för ghost-noder i katalogträdet'"
```

---

## Task 3: CatalogTreeNode — Ghost-stöd

**Files:**
- Modify: `roda-ui/roda-wui/src/main/java/org/roda/wui/client/browse/CatalogTreeNode.java`

- [ ] **Steg 1: Lägg till `ghost`-fält och `ghostCounter` efter befintliga fält (rad 65)**

Befintlig kod (rad 63–66):
```java
  private boolean expanded = false;
  private boolean loaded = false;
  private boolean isLeaf = false;
  private Command pendingOnComplete = null;
```

Ersätt med:
```java
  private boolean expanded = false;
  private boolean loaded = false;
  private boolean isLeaf = false;
  private Command pendingOnComplete = null;
  private final boolean ghost;
  private static int ghostCounter = 0;
```

- [ ] **Steg 2: Lägg till `this.ghost = false;` i befintlig konstruktor (rad 68)**

Befintlig konstruktor (rad 68–71):
```java
  public CatalogTreeNode(String aipId, String title, String level, int depth) {
    this.aipId = aipId;
    this.title = title;
    this.depth = depth;
```

Ersätt med:
```java
  public CatalogTreeNode(String aipId, String title, String level, int depth) {
    this.ghost = false;
    this.aipId = aipId;
    this.title = title;
    this.depth = depth;
```

- [ ] **Steg 3: Lägg till ghost-fabriksmetod och privat ghost-konstruktor efter befintlig konstruktor (efter rad 119)**

Befintlig kod (rad 117–119):
```java
    rootPanel.add(rowPanel);
    rootPanel.add(childrenPanel);

    initWidget(rootPanel);
  }
```

Ersätt med:
```java
    rootPanel.add(rowPanel);
    rootPanel.add(childrenPanel);

    initWidget(rootPanel);
  }

  /** Skapar en ghost-nod som representerar ett AIP utan behörighet. */
  public static CatalogTreeNode createGhostNode(int depth) {
    return new CatalogTreeNode(depth);
  }

  private CatalogTreeNode(int depth) {
    this.ghost = true;
    this.aipId = "__ghost__" + (ghostCounter++);
    this.title = messages.catalogTreeGhostNodeLabel();
    this.depth = depth;

    rootPanel = new FlowPanel();

    rowPanel = new FlowPanel();
    rowPanel.setStyleName("catalogTreeNode ghost");

    for (int i = 0; i < depth; i++) {
      FlowPanel indent = new FlowPanel();
      indent.setStyleName("catalogTreeIndent");
      rowPanel.add(indent);
    }

    toggleHtml = new HTML("");
    toggleHtml.setStyleName("catalogTreeToggle");
    rowPanel.add(toggleHtml);

    iconHtml = new HTML(DescriptionLevelUtils.getElementLevelIconSafeHtml(RodaConstants.AIP_GHOST, false));
    iconHtml.setStyleName("catalogTreeIcon");
    rowPanel.add(iconHtml);

    titleLabel = new Label(this.title);
    titleLabel.setStyleName("catalogTreeLabel");
    rowPanel.add(titleLabel);

    childrenPanel = new FlowPanel();
    childrenPanel.setStyleName("catalogTreeNodeChildren");
    childrenPanel.setVisible(true);

    rootPanel.add(rowPanel);
    rootPanel.add(childrenPanel);

    loaded = true;
    expanded = true;

    initWidget(rootPanel);
  }
```

- [ ] **Steg 4: Skydda `toggle()` och `collapse()` mot ghost-noder**

Befintlig `toggle()` (rad 121–128):
```java
  public void toggle() {
    if (isLeaf) return;
    if (expanded) {
      collapse();
    } else {
      expand(null);
    }
  }
```

Ersätt med:
```java
  public void toggle() {
    if (ghost || isLeaf) return;
    if (expanded) {
      collapse();
    } else {
      expand(null);
    }
  }
```

Befintlig `collapse()` (rad 210–214):
```java
  public void collapse() {
    childrenPanel.setVisible(false);
    expanded = false;
    toggleHtml.setHTML(ICON_TOGGLE_COLLAPSED);
  }
```

Ersätt med:
```java
  public void collapse() {
    if (ghost) return;
    childrenPanel.setVisible(false);
    expanded = false;
    toggleHtml.setHTML(ICON_TOGGLE_COLLAPSED);
  }
```

- [ ] **Steg 5: Lägg till `addPrebuiltChild()` och `isGhostNode()` efter `collapse()` (rad 214)**

Befintlig kod (rad 215–218):
```java
  public void select() {
    rowPanel.addStyleName("selected");
  }
```

Ersätt med:
```java
  /**
   * Lägger till ett förkonstruerat barn (används vid fallback ghost-träd).
   * Sätter noden som laddad och utfälld.
   */
  public void addPrebuiltChild(CatalogTreeNode child) {
    if (childNodes.containsKey(child.getAipId())) {
      return;
    }
    childNodes.put(child.getAipId(), child);
    childrenPanel.add(child);
    if (!ghost) {
      loaded = true;
      expanded = true;
      childrenPanel.setVisible(true);
      toggleHtml.setHTML(ICON_TOGGLE_EXPANDED);
    }
  }

  public boolean isGhostNode() {
    return ghost;
  }

  public void select() {
    rowPanel.addStyleName("selected");
  }
```

- [ ] **Steg 6: Verifiera att koden kompilerar**

```bash
wsl -d Ubuntu -- bash -c "cd ~/ETERNA/.worktrees/eterna-v1-alpha-2026-05-20 && mvn install -pl roda-ui/roda-wui -am -DskipTests -q 2>&1 | tail -20"
```

Förväntat: `BUILD SUCCESS`

- [ ] **Steg 7: Commit**

```bash
wsl -d Ubuntu -- bash -c "cd ~/ETERNA/.worktrees/eterna-v1-alpha-2026-05-20 && git add roda-ui/roda-wui/src/main/java/org/roda/wui/client/browse/CatalogTreeNode.java && git commit -m 'feat: ghost-konstruktor och addPrebuiltChild i CatalogTreeNode (#480)'"
```

---

## Task 4: CatalogTreePanel — Fallback ghost-träd

**Files:**
- Modify: `roda-ui/roda-wui/src/main/java/org/roda/wui/client/browse/CatalogTreePanel.java`

- [ ] **Steg 1: Lägg till saknade imports**

Befintlig import-sektion (rad 10–13):
```java
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
```

Ersätt med:
```java
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
```

- [ ] **Steg 2: Aktivera fallback i `loadRootNodes()` om sökningen returnerar 0 resultat**

Befintlig kod i `loadRootNodes()` (rad 186–197):
```java
        for (IndexedAIP aip : result.getResults()) {
          CatalogTreeNode node = new CatalogTreeNode(aip.getId(), aip.getTitle(), aip.getLevel(), 0);
          rootNodes.put(aip.getId(), node);
          treeBody.add(node);
        }
        rootsLoaded = true;
        if (pendingRevealAipId != null) {
          String id = pendingRevealAipId;
          pendingRevealAipId = null;
          doRevealAip(id);
        }
```

Ersätt med:
```java
        if (result.getResults().isEmpty()) {
          loadFallbackGhostTree();
          return;
        }
        for (IndexedAIP aip : result.getResults()) {
          CatalogTreeNode node = new CatalogTreeNode(aip.getId(), aip.getTitle(), aip.getLevel(), 0);
          rootNodes.put(aip.getId(), node);
          treeBody.add(node);
        }
        rootsLoaded = true;
        if (pendingRevealAipId != null) {
          String id = pendingRevealAipId;
          pendingRevealAipId = null;
          doRevealAip(id);
        }
```

- [ ] **Steg 3: Lägg till `loadFallbackGhostTree()` efter `loadRootNodes()` (efter rad 198)**

Befintlig kod (rad 199–209):
```java
  public void revealAip(String aipId) {
    if (!rootsLoaded) {
      pendingRevealAipId = aipId;
      if (!rootsLoading) {
        loadRootNodes();
      }
      return;
    }
    doRevealAip(aipId);
  }
```

Ersätt med:
```java
  private void loadFallbackGhostTree() {
    FindRequest findRequest = new FindRequest.FindRequestBuilder(
      new Filter(new NotSimpleFilterParameter(RodaConstants.AIP_LEVEL, "file")),
      false)
      .withSorter(new Sorter(new SortParameter(RodaConstants.AIP_TITLE_SORT, false)))
      .withSublist(new Sublist(0, 200))
      .build();

    Services service = new Services(messages.catalogTreeReasonListRoots(), "get");
    service.rodaEntityRestService(
      s -> s.find(findRequest, LocaleInfo.getCurrentLocale().getLocaleName()),
      IndexedAIP.class)
      .whenComplete((result, error) -> {
        if (error != null) {
          LOGGER.error("Fallback ghost tree query failed", error);
          rootsLoaded = true;
          return;
        }
        List<IndexedAIP> aips = result.getResults();
        if (aips.isEmpty()) {
          rootsLoaded = true;
          return;
        }

        Map<String, CatalogTreeNode> nodeMap = new LinkedHashMap<>();
        List<CatalogTreeNode> roots = new ArrayList<>();
        int[] remaining = {aips.size()};

        for (IndexedAIP aip : aips) {
          Services s2 = new Services(messages.catalogTreeReasonGetAncestors(), "get");
          s2.aipResource(srv -> srv.getAncestors(aip.getId()))
            .whenComplete((ancestors, err) -> {
              if (err != null) {
                LOGGER.warn("Could not get ancestors for fallback AIP " + aip.getId() + ", skipping");
              } else {
                Collections.reverse(ancestors);
                insertFallbackChain(ancestors, aip, nodeMap, roots);
              }
              remaining[0]--;
              if (remaining[0] == 0) {
                finalizeFallbackTree(roots);
              }
            });
        }
      });
  }

  private void insertFallbackChain(List<IndexedAIP> ancestors, IndexedAIP targetAip,
      Map<String, CatalogTreeNode> nodeMap, List<CatalogTreeNode> roots) {
    CatalogTreeNode parent = null;

    for (int i = 0; i < ancestors.size(); i++) {
      IndexedAIP anc = ancestors.get(i);
      CatalogTreeNode node;

      if (anc == null) {
        node = CatalogTreeNode.createGhostNode(i);
      } else if (i == 0 && nodeMap.containsKey(anc.getId())) {
        parent = nodeMap.get(anc.getId());
        continue;
      } else {
        node = new CatalogTreeNode(anc.getId(), anc.getTitle(), anc.getLevel(), i);
        if (i == 0) {
          nodeMap.put(anc.getId(), node);
        }
      }

      if (parent == null) {
        roots.add(node);
      } else {
        parent.addPrebuiltChild(node);
      }
      parent = node;
    }

    if (nodeMap.containsKey(targetAip.getId())) {
      return;
    }
    CatalogTreeNode targetNode = new CatalogTreeNode(
      targetAip.getId(), targetAip.getTitle(), targetAip.getLevel(), ancestors.size());
    nodeMap.put(targetAip.getId(), targetNode);
    if (parent == null) {
      roots.add(targetNode);
    } else {
      parent.addPrebuiltChild(targetNode);
    }
  }

  private void finalizeFallbackTree(List<CatalogTreeNode> roots) {
    for (CatalogTreeNode root : roots) {
      rootNodes.put(root.getAipId(), root);
      treeBody.add(root);
    }
    rootsLoaded = true;
    if (pendingRevealAipId != null) {
      String id = pendingRevealAipId;
      pendingRevealAipId = null;
      doRevealAip(id);
    }
  }

  public void revealAip(String aipId) {
    if (!rootsLoaded) {
      pendingRevealAipId = aipId;
      if (!rootsLoading) {
        loadRootNodes();
      }
      return;
    }
    doRevealAip(aipId);
  }
```

- [ ] **Steg 4: Null-säkra `expandChain()` för ghost-förfäder**

`getAncestors()` kan returnera `null`-värden för otillgängliga förfäder. Befintlig `expandChain()` anropar `.getId()` utan null-kontroll — NPE i fallback-scenariot.

Befintlig `expandChain()` (rad 224–240):
```java
  private void expandChain(List<IndexedAIP> ancestors, int index, String targetId) {
    if (index >= ancestors.size()) {
      selectNode(targetId);
      return;
    }
    CatalogTreeNode node = findNode(ancestors.get(index).getId(), rootNodes);
    if (node == null) {
      selectNode(targetId);
      return;
    }
    node.expand(new com.google.gwt.user.client.Command() {
      @Override
      public void execute() {
        expandChain(ancestors, index + 1, targetId);
      }
    });
  }
```

Ersätt med:
```java
  private void expandChain(List<IndexedAIP> ancestors, int index, String targetId) {
    if (index >= ancestors.size()) {
      selectNode(targetId);
      return;
    }
    IndexedAIP ancestor = ancestors.get(index);
    if (ancestor == null) {
      // Ghost-förfader: redan utfälld, fortsätt till nästa
      expandChain(ancestors, index + 1, targetId);
      return;
    }
    CatalogTreeNode node = findNode(ancestor.getId(), rootNodes);
    if (node == null) {
      selectNode(targetId);
      return;
    }
    node.expand(new com.google.gwt.user.client.Command() {
      @Override
      public void execute() {
        expandChain(ancestors, index + 1, targetId);
      }
    });
  }
```

- [ ] **Steg 6: Uppdatera `applyFilter()` för ghost-noder**

Befintlig `applyFilter()` (rad 143–151):
```java
  private boolean applyFilter(CatalogTreeNode node, String query) {
    boolean selfMatches = query.isEmpty() || (node.getTitle() != null && node.getTitle().toLowerCase().contains(query));
    boolean childMatches = false;
    for (CatalogTreeNode child : node.getChildNodes().values()) {
      if (applyFilter(child, query)) childMatches = true;
    }
    node.setVisible(selfMatches || childMatches);
    return selfMatches || childMatches;
  }
```

Ersätt med:
```java
  private boolean applyFilter(CatalogTreeNode node, String query) {
    boolean childMatches = false;
    for (CatalogTreeNode child : node.getChildNodes().values()) {
      if (applyFilter(child, query)) childMatches = true;
    }
    if (node.isGhostNode()) {
      node.setVisible(query.isEmpty() || childMatches);
      return query.isEmpty() || childMatches;
    }
    boolean selfMatches = query.isEmpty() || (node.getTitle() != null && node.getTitle().toLowerCase().contains(query));
    node.setVisible(selfMatches || childMatches);
    return selfMatches || childMatches;
  }
```

- [ ] **Steg 7: Verifiera att koden kompilerar**

```bash
wsl -d Ubuntu -- bash -c "cd ~/ETERNA/.worktrees/eterna-v1-alpha-2026-05-20 && mvn install -pl roda-ui/roda-wui -am -DskipTests -q 2>&1 | tail -20"
```

Förväntat: `BUILD SUCCESS`

- [ ] **Steg 8: Commit**

```bash
wsl -d Ubuntu -- bash -c "cd ~/ETERNA/.worktrees/eterna-v1-alpha-2026-05-20 && git add roda-ui/roda-wui/src/main/java/org/roda/wui/client/browse/CatalogTreePanel.java && git commit -m 'feat: fallback ghost-träd i katalogträdet för användare utan rot-behörighet (#480, #481)'"
```

---

## Task 5: GWT-kompilering och manuell testning

**Files:** (inga ändringar)

- [ ] **Steg 1: GWT-kompilera**

```bash
wsl -d Ubuntu -- bash -c "cd ~/ETERNA/.worktrees/eterna-v1-alpha-2026-05-20 && mvn -pl roda-ui/roda-wui -am gwt:compile -Pdebug-main -Dscope.gwt-dev=compile -q 2>&1 | tail -20"
```

Förväntat: `BUILD SUCCESS` (tar 20–30 min)

- [ ] **Steg 2: Kopiera GWT RPC-filer**

```bash
wsl -d Ubuntu -- bash -c "cd ~/ETERNA/.worktrees/eterna-v1-alpha-2026-05-20 && ./roda-ui/roda-wui/copy_gwt_rpc.sh"
```

- [ ] **Steg 3: Starta om Spring Boot**

```bash
wsl -d Ubuntu -- bash -c "fuser -k 8080/tcp 5005/tcp 2>/dev/null; sleep 1; cd ~/ETERNA/.worktrees/eterna-v1-alpha-2026-05-20 && tmux new-session -d -s eterna-test-ghost 'PATH=\"$HOME/.local/bin:$PATH\" SIEGFRIED_MODE=server SIEGFRIED_SERVER_URL=http://localhost:5138 mvn -pl roda-ui/roda-wui -am spring-boot:run -Pdebug-main 2>&1 | tee /tmp/eterna-ghost-test.log'"
```

- [ ] **Steg 4: Vänta tills Spring Boot är uppe**

Kontrollera loggen:
```bash
wsl -d Ubuntu -- bash -c "tail -5 /tmp/eterna-ghost-test.log"
```

Förväntat: raden `Started RODA in X seconds` syns.

- [ ] **Steg 5: Manuell testning — Testscenario 1 (admin)**

1. Logga in som admin-användare på http://localhost:8080
2. Navigera till Katalog
3. Verifiera: Katalogträdet visar root-noder som vanligt (normalflödet är oförändrat)

- [ ] **Steg 6: Manuell testning — Testscenario 2 (begränsad användare)**

1. Logga in som en användare som bara har behörighet till ett AIP längre ner i strukturen (INTE till rotnoden)
2. Navigera till Katalog
3. Verifiera:
   - Trädet är INTE tomt
   - Förfäder utan behörighet visas som grå kursiva noder med texten "Åtkomst saknas"
   - Ghost-noder är utfällda och kan inte fällas in
   - Noden som användaren har behörighet till visas som normal klickbar nod
   - Klick på ghost-nod händer ingenting (ingen navigering)

- [ ] **Steg 7: Manuell testning — Testscenario 3 (sökfilter)**

1. Logga in som den begränsade användaren
2. Skriv in en söksträng i trädets filterruta
3. Verifiera:
   - Ghost-noder döljs om inga av deras barn matchar filtret
   - Ghost-noder visas om ett eller flera barn matchar filtret
   - Ghost-noder visas inte om filtret matchar deras text ("åtkomst") — de ska inte vara sökbara på sin labeltext

---

## Task 6: Slutcommit och städning

**Files:** (inga ändringar)

- [ ] **Steg 1: Kontrollera git-status**

```bash
wsl -d Ubuntu -- bash -c "cd ~/ETERNA/.worktrees/eterna-v1-alpha-2026-05-20 && git log --oneline feat/480-katalogträd-ghost-noder ^eterna-v1-alpha-2026-05-20 2>/dev/null | head -10"
```

Förväntat: 4 commits syns (i18n, CSS, CatalogTreeNode, CatalogTreePanel)

- [ ] **Steg 2: Kontrollera att inga oavsiktliga filer är ändrade**

```bash
wsl -d Ubuntu -- bash -c "cd ~/ETERNA/.worktrees/eterna-v1-alpha-2026-05-20 && git status --short"
```

Förväntat: tom output (inga ostageade ändringar)
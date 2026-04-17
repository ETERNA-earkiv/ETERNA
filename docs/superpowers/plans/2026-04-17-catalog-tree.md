# Katalogträd i ETERNA — Implementationsplan

> **För agentiska implementatörer:** OBLIGATORISK SUBSKILL: Använd `superpowers:subagent-driven-development` (rekommenderas) eller `superpowers:executing-plans` för att implementera uppgift för uppgift.

**Mål:** Lägga till en permanent vänster sidopanel med trädvy över AIP-hierarkin i `BrowseAIP`, med automatisk synkronisering när användaren navigerar till ett AIP.

**Arkitektur:** Ny GWT Composite-widget `CatalogTreePanel` placeras till vänster i `BrowseAIP`:s flexbox-layout. Trädet laddar rotnoder vid start och barn lazy via `Services.rodaEntityRestService()`. Auto-synk triggas av `BrowseAIP` via `revealAip(aipId)` som hämtar föräldrakedjans via den befintliga `GET /api/v2/aips/{id}/ancestors`-endpointen och expanderar trädet nod för nod.

**Tech stack:** Java 21, GWT (UiBinder + programmatiska widgets), Spring Boot REST (befintlig), Apache Solr via `IndexService`, CSS/GSS

---

## Filstruktur

### Nya filer
| Fil | Ansvar |
|---|---|
| `roda-ui/roda-wui/src/main/java/org/roda/wui/client/browse/CatalogTreeNode.java` | En nod i trädet — toggle, lazy load, select, felhantering |
| `roda-ui/roda-wui/src/main/java/org/roda/wui/client/browse/CatalogTreePanel.java` | Träd-container — laddar rotnoder, `revealAip()`, filterinput |
| `roda-ui/roda-wui/src/main/java/org/roda/wui/client/browse/CatalogTreePanel.ui.xml` | UiBinder-template för panelens yttre struktur |

### Modifierade filer
| Fil | Förändring |
|---|---|
| `roda-ui/roda-wui/src/main/java/org/roda/wui/client/common/resources/main.gss` | Nya CSS-klasser för trädet |
| `roda-ui/roda-wui/src/main/java/config/i18n/client/ClientMessages.java` | Nya i18n-metoder |
| `roda-ui/roda-wui/src/main/resources/config/i18n/client/ClientMessages.properties` | Engelska texter |
| `roda-ui/roda-wui/src/main/resources/config/i18n/client/ClientMessages_sv_SE.properties` | Svenska texter |
| `roda-ui/roda-wui/src/main/java/org/roda/wui/client/browse/BrowseAIP.ui.xml` | Trädet läggs till som första barn i `contentWithSidePanel` |
| `roda-ui/roda-wui/src/main/java/org/roda/wui/client/browse/BrowseAIP.java` | `@UiField` för trädet + anrop till `revealAip()` |

---

### Uppgift 1: Skapa feature-branch

**Filer:** Inga

- [ ] **Steg 1: Skapa branch från `eterna-v1-alpha`**

```bash
wsl -d Ubuntu -- bash -c "cd ~/ETERNA && git checkout eterna-v1-alpha && git pull && git checkout -b feat/catalog-tree"
```

Förväntat: `Switched to a new branch 'feat/catalog-tree'`

---

### Uppgift 2: CSS-klasser i main.gss

**Filer:**
- Modify: `roda-ui/roda-wui/src/main/java/org/roda/wui/client/common/resources/main.gss`

- [ ] **Steg 1: Lägg till CSS-klasser direkt efter `.browseSidePanel`-blocket**

Hitta raden med `.browseSidePanel {` i `main.gss` och lägg till följande **efter** hela `.browseSidePanel`-blockets avslutande `}`:

```css
/* ── Catalog Tree Panel ── */
.catalogTreePanel {
    width: 260px;
    min-width: 150px;
    flex-shrink: 0;
    background: white;
    display: flex;
    flex-direction: column;
    overflow: hidden;
}

.catalogTreeResizeHandle {
    width: 3px;
    background: COLOR_GREY_LIGHT;
    cursor: col-resize;
    flex-shrink: 0;
    transition: background 0.15s;
}

.catalogTreeResizeHandle:hover,
.catalogTreeResizeHandle.resizing {
    background: COLOR_PRIMARY;
}

.catalogTreeFilter {
    padding: 6px 8px;
    border-bottom: 1px solid COLOR_GREY_VERYLIGHT;
    flex-shrink: 0;
    background: #fafafa;
}

.catalogTreeFilterInput {
    width: 100%;
    padding: 5px 8px;
    border: 1px solid COLOR_GREY_LIGHT;
    border-radius: 3px;
    font-size: 12px;
    background: white;
    color: #333333;
    box-sizing: border-box;
}

.catalogTreePanelHeader {
    background: COLOR_GREY_DARK;
    color: COLOR_GREY_LIGHT;
    padding: 10px 14px;
    font-size: 11px;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.8px;
    flex-shrink: 0;
}

.catalogTreeBodyScroll {
    flex: 1;
    overflow-y: auto;
    padding: 4px 0;
}

.catalogTreeNode {
    display: flex;
    align-items: center;
    height: 27px;
    cursor: pointer;
    color: #333333;
    padding-right: 8px;
}

.catalogTreeNode:hover {
    background: COLOR_GREY_BGDARKER;
}

.catalogTreeNode.selected {
    background: COLOR_PRIMARY;
    color: white;
}

.catalogTreeNode.selected .catalogTreeToggle {
    color: rgba(255, 255, 255, 0.7);
}

.catalogTreeIndent {
    display: inline-block;
    width: 16px;
    flex-shrink: 0;
}

.catalogTreeToggle {
    width: 16px;
    text-align: center;
    font-size: 9px;
    color: #999999;
    flex-shrink: 0;
    margin-right: 2px;
}

.catalogTreeLabel {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: 12.5px;
}

.catalogTreeNodeChildren {
    /* Synlighet styrs via setVisible() */
}

.catalogTreeNodeError {
    padding: 4px 8px 4px 40px;
    font-size: 11px;
    color: COLOR_PRIMARY;
    display: flex;
    gap: 8px;
    align-items: center;
}
```

- [ ] **Steg 2: Commit**

```bash
wsl -d Ubuntu -- bash -c "cd ~/ETERNA && git add roda-ui/roda-wui/src/main/java/org/roda/wui/client/common/resources/main.gss && git commit -m 'feat: CSS-klasser för katalogträd'"
```

---

### Uppgift 3: i18n-nycklar

**Filer:**
- Modify: `roda-ui/roda-wui/src/main/java/config/i18n/client/ClientMessages.java`
- Modify: `roda-ui/roda-wui/src/main/resources/config/i18n/client/ClientMessages.properties`
- Modify: `roda-ui/roda-wui/src/main/resources/config/i18n/client/ClientMessages_sv_SE.properties`

- [ ] **Steg 1: Lägg till metoder i `ClientMessages.java` — direkt innan den avslutande `}`**

```java
  String catalogTreeTitle();

  String catalogTreeFilterPlaceholder();

  String catalogTreeLoadingLabel();

  String catalogTreeLoadError();

  String catalogTreeRetry();
```

- [ ] **Steg 2: Lägg till engelska texter sist i `ClientMessages.properties`**

```properties
catalogTreeTitle: Catalogue
catalogTreeFilterPlaceholder: Filter...
catalogTreeLoadingLabel: Loading...
catalogTreeLoadError: Could not load content
catalogTreeRetry: Try again
```

- [ ] **Steg 3: Lägg till svenska texter sist i `ClientMessages_sv_SE.properties`**

```properties
catalogTreeTitle: Katalog
catalogTreeFilterPlaceholder: Filtrera...
catalogTreeLoadingLabel: Laddar...
catalogTreeLoadError: Kunde inte hämta innehåll
catalogTreeRetry: Försök igen
```

- [ ] **Steg 4: Commit**

```bash
wsl -d Ubuntu -- bash -c "cd ~/ETERNA && git add roda-ui/roda-wui/src/main/java/config/i18n/client/ClientMessages.java roda-ui/roda-wui/src/main/resources/config/i18n/client/ClientMessages.properties roda-ui/roda-wui/src/main/resources/config/i18n/client/ClientMessages_sv_SE.properties && git commit -m 'feat: i18n-nycklar för katalogträd'"
```

---

### Uppgift 4: `CatalogTreeNode.java`

**Filer:**
- Create: `roda-ui/roda-wui/src/main/java/org/roda/wui/client/browse/CatalogTreeNode.java`

En programmatisk GWT-widget (inget UiBinder — strukturen är dynamisk och rekursiv). Hanterar en enstaka nod: toggle, lazy loading av barn, felvisning, select/deselect.

- [ ] **Steg 1: Skapa filen med hela implementationen**

```java
package org.roda.wui.client.browse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.v2.index.FindRequest;
import org.roda.core.data.v2.index.filter.Filter;
import org.roda.core.data.v2.index.filter.SimpleFilterParameter;
import org.roda.core.data.v2.ip.IndexedAIP;
import org.roda.wui.client.services.Services;

import com.google.gwt.core.client.Command;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import config.i18n.client.ClientMessages;

public class CatalogTreeNode extends Composite {

  private static final Logger LOGGER = LoggerFactory.getLogger(CatalogTreeNode.class);
  private static final ClientMessages messages = GWT.create(ClientMessages.class);

  private static final String TOGGLE_COLLAPSED = "▶";
  private static final String TOGGLE_EXPANDED = "▼";
  private static final String TOGGLE_LEAF = "—";
  private static final String TOGGLE_LOADING = "○";

  private final String aipId;
  private final String title;
  private final int depth;
  private final FlowPanel rootPanel;
  private final FlowPanel rowPanel;
  private final FlowPanel childrenPanel;
  private final Label toggleLabel;
  private final Map<String, CatalogTreeNode> childNodes = new HashMap<>();

  private boolean expanded = false;
  private boolean loaded = false;
  private boolean isLeaf = false;

  public CatalogTreeNode(String aipId, String title, int depth) {
    this.aipId = aipId;
    this.title = title;
    this.depth = depth;

    rootPanel = new FlowPanel();

    rowPanel = new FlowPanel();
    rowPanel.setStyleName("catalogTreeNode");

    for (int i = 0; i < depth; i++) {
      FlowPanel indent = new FlowPanel();
      indent.setStyleName("catalogTreeIndent");
      rowPanel.add(indent);
    }

    toggleLabel = new Label(TOGGLE_COLLAPSED);
    toggleLabel.setStyleName("catalogTreeToggle");
    rowPanel.add(toggleLabel);

    Label labelWidget = new Label(title);
    labelWidget.setStyleName("catalogTreeLabel");
    rowPanel.add(labelWidget);

    childrenPanel = new FlowPanel();
    childrenPanel.setStyleName("catalogTreeNodeChildren");
    childrenPanel.setVisible(false);

    rowPanel.addDomHandler(event -> toggle(), ClickEvent.getType());

    rootPanel.add(rowPanel);
    rootPanel.add(childrenPanel);

    initWidget(rootPanel);
  }

  public void toggle() {
    if (isLeaf) return;
    if (expanded) {
      collapse();
    } else {
      expand(null);
    }
  }

  public void expand(Command onComplete) {
    if (isLeaf) {
      if (onComplete != null) onComplete.execute();
      return;
    }
    if (loaded) {
      childrenPanel.setVisible(true);
      expanded = true;
      toggleLabel.setText(TOGGLE_EXPANDED);
      if (onComplete != null) onComplete.execute();
    } else {
      loadChildren(onComplete);
    }
  }

  private void loadChildren(Command onComplete) {
    toggleLabel.setText(TOGGLE_LOADING);

    FindRequest findRequest = new FindRequest.FindRequestBuilder(
      new Filter(new SimpleFilterParameter(RodaConstants.AIP_PARENT_ID, aipId)), false)
      .build();

    Services service = new Services(messages.catalogTreeLoadingLabel(), "get");
    service.rodaEntityRestService(
      s -> s.find(findRequest, LocaleInfo.getCurrentLocale().getLocaleName()),
      IndexedAIP.class)
      .whenComplete((result, error) -> {
        if (error != null) {
          LOGGER.error("Failed to load children for AIP {}", aipId, error);
          toggleLabel.setText(TOGGLE_COLLAPSED);
          showLoadError();
          return;
        }
        loaded = true;
        List<IndexedAIP> children = result.getResults();
        if (children.isEmpty()) {
          markAsLeaf();
        } else {
          for (IndexedAIP child : children) {
            CatalogTreeNode childNode = new CatalogTreeNode(child.getId(), child.getTitle(), depth + 1);
            childNodes.put(child.getId(), childNode);
            childrenPanel.add(childNode);
          }
          childrenPanel.setVisible(true);
          expanded = true;
          toggleLabel.setText(TOGGLE_EXPANDED);
        }
        if (onComplete != null) onComplete.execute();
      });
  }

  private void markAsLeaf() {
    isLeaf = true;
    toggleLabel.setText(TOGGLE_LEAF);
  }

  private void showLoadError() {
    FlowPanel errorPanel = new FlowPanel();
    errorPanel.setStyleName("catalogTreeNodeError");
    Label errorLabel = new Label(messages.catalogTreeLoadError());
    Anchor retryLink = new Anchor(messages.catalogTreeRetry());
    retryLink.addClickHandler(e -> {
      errorPanel.removeFromParent();
      loaded = false;
      loadChildren(null);
    });
    errorPanel.add(errorLabel);
    errorPanel.add(retryLink);
    rootPanel.insert(errorPanel, 1);
  }

  public void collapse() {
    childrenPanel.setVisible(false);
    expanded = false;
    toggleLabel.setText(TOGGLE_COLLAPSED);
  }

  public void select() {
    rowPanel.addStyleName("selected");
    rowPanel.getElement().scrollIntoView();
  }

  public void deselect() {
    rowPanel.removeStyleName("selected");
  }

  public String getAipId() {
    return aipId;
  }

  public String getTitle() {
    return title;
  }

  public Map<String, CatalogTreeNode> getChildNodes() {
    return childNodes;
  }
}
```

- [ ] **Steg 2: Commit**

```bash
wsl -d Ubuntu -- bash -c "cd ~/ETERNA && git add roda-ui/roda-wui/src/main/java/org/roda/wui/client/browse/CatalogTreeNode.java && git commit -m 'feat: CatalogTreeNode — lazy-loading trädnod för AIP-hierarkin'"
```

---

### Uppgift 5: `CatalogTreePanel.java` + `CatalogTreePanel.ui.xml`

**Filer:**
- Create: `roda-ui/roda-wui/src/main/java/org/roda/wui/client/browse/CatalogTreePanel.java`
- Create: `roda-ui/roda-wui/src/main/java/org/roda/wui/client/browse/CatalogTreePanel.ui.xml`

Container-widgeten. UiBinder för statisk yttre struktur (header, scroll-yta). Laddar rotnoder vid init. `revealAip()` hämtar föräldrakedjans via befintlig endpoint och expanderar trädet sekventiellt.

- [ ] **Steg 1: Skapa `CatalogTreePanel.ui.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE ui:UiBinder SYSTEM "http://dl.google.com/gwt/DTD/xhtml.ent">
<ui:UiBinder xmlns:ui="urn:ui:com.google.gwt.uibinder"
    xmlns:g="urn:import:com.google.gwt.user.client.ui">

    <ui:with field='messages' type='config.i18n.client.ClientMessages' />

    <g:FlowPanel styleName="catalogTreePanel">
        <g:FlowPanel styleName="catalogTreePanelHeader">
            <g:Label>{messages.catalogTreeTitle}</g:Label>
        </g:FlowPanel>
        <g:FlowPanel styleName="catalogTreeFilter">
            <g:TextBox ui:field="filterInput" styleName="catalogTreeFilterInput" />
        </g:FlowPanel>
        <g:FlowPanel styleName="catalogTreeBodyScroll">
            <g:FlowPanel ui:field="treeBody" />
        </g:FlowPanel>
    </g:FlowPanel>
</ui:UiBinder>
```

- [ ] **Steg 2: Skapa `CatalogTreePanel.java`**

```java
package org.roda.wui.client.browse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.v2.index.FindRequest;
import org.roda.core.data.v2.index.filter.EmptyKeyFilterParameter;
import org.roda.core.data.v2.index.filter.Filter;
import org.roda.core.data.v2.ip.IndexedAIP;
import org.roda.wui.client.services.Services;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import config.i18n.client.ClientMessages;

public class CatalogTreePanel extends Composite {

  private static final Logger LOGGER = LoggerFactory.getLogger(CatalogTreePanel.class);
  private static final ClientMessages messages = GWT.create(ClientMessages.class);
  private static final MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

  interface MyUiBinder extends UiBinder<Widget, CatalogTreePanel> {}

  @UiField
  FlowPanel treeBody;

  @UiField
  TextBox filterInput;

  private final Map<String, CatalogTreeNode> rootNodes = new HashMap<>();
  private CatalogTreeNode selectedNode = null;

  public CatalogTreePanel() {
    initWidget(uiBinder.createAndBindUi(this));
    filterInput.getElement().setAttribute("placeholder", messages.catalogTreeFilterPlaceholder());
    filterInput.addKeyUpHandler(this::onFilterChanged);
    loadRootNodes();
  }

  private void onFilterChanged(KeyUpEvent event) {
    String query = filterInput.getText().trim().toLowerCase();
    for (CatalogTreeNode node : rootNodes.values()) {
      applyFilter(node, query);
    }
  }

  private boolean applyFilter(CatalogTreeNode node, String query) {
    boolean selfMatches = query.isEmpty() || node.getTitle().toLowerCase().contains(query);
    boolean childMatches = false;
    for (CatalogTreeNode child : node.getChildNodes().values()) {
      if (applyFilter(child, query)) childMatches = true;
    }
    node.setVisible(selfMatches || childMatches);
    return selfMatches || childMatches;
  }

  private void loadRootNodes() {
    FindRequest findRequest = new FindRequest.FindRequestBuilder(
      new Filter(new EmptyKeyFilterParameter(RodaConstants.AIP_PARENT_ID)), false)
      .build();

    Services service = new Services(messages.catalogTreeLoadingLabel(), "get");
    service.rodaEntityRestService(
      s -> s.find(findRequest, LocaleInfo.getCurrentLocale().getLocaleName()),
      IndexedAIP.class)
      .whenComplete((result, error) -> {
        if (error != null) {
          LOGGER.error("Failed to load catalog tree root nodes", error);
          return;
        }
        for (IndexedAIP aip : result.getResults()) {
          CatalogTreeNode node = new CatalogTreeNode(aip.getId(), aip.getTitle(), 0);
          rootNodes.put(aip.getId(), node);
          treeBody.add(node);
        }
      });
  }

  public void revealAip(String aipId) {
    Services service = new Services(messages.catalogTreeLoadingLabel(), "get");
    service.aipResource(s -> s.getAncestors(aipId))
      .whenComplete((ancestors, error) -> {
        if (error != null) {
          LOGGER.warn("Could not fetch ancestors for AIP {}, auto-sync skipped", aipId);
          return;
        }
        expandChain(ancestors, 0, aipId);
      });
  }

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
    node.expand(() -> expandChain(ancestors, index + 1, targetId));
  }

  private void selectNode(String aipId) {
    if (selectedNode != null) selectedNode.deselect();
    CatalogTreeNode node = findNode(aipId, rootNodes);
    if (node != null) {
      selectedNode = node;
      node.select();
    }
  }

  private CatalogTreeNode findNode(String aipId, Map<String, CatalogTreeNode> nodes) {
    if (nodes.containsKey(aipId)) return nodes.get(aipId);
    for (CatalogTreeNode node : nodes.values()) {
      CatalogTreeNode found = findNode(aipId, node.getChildNodes());
      if (found != null) return found;
    }
    return null;
  }
}
```

- [ ] **Steg 3: Commit**

```bash
wsl -d Ubuntu -- bash -c "cd ~/ETERNA && git add roda-ui/roda-wui/src/main/java/org/roda/wui/client/browse/CatalogTreePanel.java roda-ui/roda-wui/src/main/java/org/roda/wui/client/browse/CatalogTreePanel.ui.xml && git commit -m 'feat: CatalogTreePanel — träd-container med lazy loading och auto-synk'"
```

---

### Uppgift 6: `BrowseAIP.ui.xml` — lägg till trädet i layouten

**Filer:**
- Modify: `roda-ui/roda-wui/src/main/java/org/roda/wui/client/browse/BrowseAIP.ui.xml`

- [ ] **Steg 1: Lägg till `browse`-namespace i öppnings-taggen**

Befintlig rad 3–5 ser ut så här:
```xml
<ui:UiBinder xmlns:ui="urn:ui:com.google.gwt.uibinder" xmlns:g="urn:import:com.google.gwt.user.client.ui"
	xmlns:common="urn:import:org.roda.wui.client.common" xmlns:commoncards="urn:import:org.roda.wui.client.common.cards"
	xmlns:tabs="urn:import:org.roda.wui.client.browse.tabs" xmlns:labels="urn:import:org.roda.wui.client.common.labels">
```

Lägg till `xmlns:browse="urn:import:org.roda.wui.client.browse"` på slutet av rad 5, så att det blir:
```xml
<ui:UiBinder xmlns:ui="urn:ui:com.google.gwt.uibinder" xmlns:g="urn:import:com.google.gwt.user.client.ui"
	xmlns:common="urn:import:org.roda.wui.client.common" xmlns:commoncards="urn:import:org.roda.wui.client.common.cards"
	xmlns:tabs="urn:import:org.roda.wui.client.browse.tabs" xmlns:labels="urn:import:org.roda.wui.client.common.labels"
	xmlns:browse="urn:import:org.roda.wui.client.browse">
```

- [ ] **Steg 2: Lägg till `CatalogTreePanel` som första barn i `contentWithSidePanel`**

Befintlig struktur av `contentWithSidePanel`:
```xml
<g:FlowPanel styleName="contentWithSidePanel">
    <g:FocusPanel ui:field="keyboardFocus">
```

Ändra till:
```xml
<g:FlowPanel styleName="contentWithSidePanel">
    <browse:CatalogTreePanel ui:field="catalogTreePanel" />
    <g:FlowPanel ui:field="treeResizeHandle" styleName="catalogTreeResizeHandle" />
    <g:FocusPanel ui:field="keyboardFocus">
```

- [ ] **Steg 3: Lägg till `@UiField` och resize-logik i `BrowseAIP.java`**

Direkt under `@UiField CatalogTreePanel catalogTreePanel;` lägg till:

```java
@UiField
FlowPanel treeResizeHandle;
```

I konstruktorn, direkt efter `catalogTreePanel.revealAip(aipId);`, lägg till:

```java
initTreeResize();
```

Lägg till privat metod i klassen (utanför konstruktorn):

```java
private void initTreeResize() {
  treeResizeHandle.addDomHandler(event -> {
    event.preventDefault();
    final int startX = event.getClientX();
    final int startWidth = catalogTreePanel.getOffsetWidth();
    com.google.gwt.user.client.Event.setCapture(treeResizeHandle.getElement());
    treeResizeHandle.addStyleName("resizing");

    com.google.gwt.user.client.EventListener mouseMoveListener = nativeEvent -> {
      int dx = nativeEvent.getClientX() - startX;
      int newWidth = Math.max(150, Math.min(480, startWidth + dx));
      catalogTreePanel.getElement().getStyle().setPropertyPx("width", newWidth);
    };

    com.google.gwt.user.client.EventListener mouseUpListener = nativeEvent -> {
      com.google.gwt.user.client.Event.releaseCapture(treeResizeHandle.getElement());
      treeResizeHandle.removeStyleName("resizing");
      com.google.gwt.user.client.DOM.setEventListener(treeResizeHandle.getElement(), null);
    };

    com.google.gwt.user.client.DOM.setEventListener(treeResizeHandle.getElement(), nativeEvent -> {
      if (nativeEvent.getType().equals("mousemove")) mouseMoveListener.onBrowserEvent(nativeEvent);
      else if (nativeEvent.getType().equals("mouseup")) mouseUpListener.onBrowserEvent(nativeEvent);
    });
    com.google.gwt.user.client.DOM.sinkEvents(treeResizeHandle.getElement(),
      com.google.gwt.user.client.Event.ONMOUSEMOVE | com.google.gwt.user.client.Event.ONMOUSEUP);
  }, com.google.gwt.event.dom.client.MouseDownEvent.getType());
}
```

- [ ] **Steg 4: Commit**

```bash
wsl -d Ubuntu -- bash -c "cd ~/ETERNA && git add roda-ui/roda-wui/src/main/java/org/roda/wui/client/browse/BrowseAIP.ui.xml && git commit -m 'feat: lägg till CatalogTreePanel och resize-handle i BrowseAIP-layouten'"
```

---

### Uppgift 7: `BrowseAIP.java` — koppla ihop med trädet

**Filer:**
- Modify: `roda-ui/roda-wui/src/main/java/org/roda/wui/client/browse/BrowseAIP.java`

- [ ] **Steg 1: Lägg till `@UiField`-fält**

Direkt efter befintlig rad `@UiField FocusPanel keyboardFocus;` lägg till:

```java
@UiField
CatalogTreePanel catalogTreePanel;

@UiField
FlowPanel treeResizeHandle;
```

Ingen extra import krävs — alla klasser är i samma paket eller redan importerade.

- [ ] **Steg 2: Anropa `revealAip()` och `initTreeResize()` i konstruktorn**

Direkt efter `initWidget(uiBinder.createAndBindUi(this));` lägg till:

```java
catalogTreePanel.revealAip(aipId);
initTreeResize();
```

Konstruktorns topp ser ut så här efter ändringen:
```java
private BrowseAIP(BrowseAIPResponse response) {
  aip = response.getIndexedAIP();
  aipId = aip.getId();
  boolean justActive = AIPState.ACTIVE.equals(aip.getState());

  AipSearchWrapperActions aipActions = AipSearchWrapperActions.get(aip.getId(), aip.getState(), aip.getPermissions());

  initWidget(uiBinder.createAndBindUi(this));
  catalogTreePanel.revealAip(aipId);
  initTreeResize();
  // ... resten oförändrat
```

- [ ] **Steg 3: Lägg till `initTreeResize()`-metod i `BrowseAIP.java`**

Lägg till som privat metod i klassen:

```java
private void initTreeResize() {
  treeResizeHandle.addDomHandler(event -> {
    event.preventDefault();
    final int startX = event.getClientX();
    final int startWidth = catalogTreePanel.getOffsetWidth();
    com.google.gwt.user.client.Event.setCapture(treeResizeHandle.getElement());
    treeResizeHandle.addStyleName("resizing");

    com.google.gwt.user.client.DOM.setEventListener(treeResizeHandle.getElement(), nativeEvent -> {
      if (nativeEvent.getType().equals("mousemove")) {
        int newWidth = Math.max(150, Math.min(480, startWidth + nativeEvent.getClientX() - startX));
        catalogTreePanel.getElement().getStyle().setPropertyPx("width", newWidth);
      } else if (nativeEvent.getType().equals("mouseup")) {
        com.google.gwt.user.client.Event.releaseCapture(treeResizeHandle.getElement());
        treeResizeHandle.removeStyleName("resizing");
        com.google.gwt.user.client.DOM.setEventListener(treeResizeHandle.getElement(), null);
      }
    });
    com.google.gwt.user.client.DOM.sinkEvents(treeResizeHandle.getElement(),
      com.google.gwt.user.client.Event.ONMOUSEMOVE | com.google.gwt.user.client.Event.ONMOUSEUP);
  }, com.google.gwt.event.dom.client.MouseDownEvent.getType());
}
```

- [ ] **Steg 4: Commit**

```bash
wsl -d Ubuntu -- bash -c "cd ~/ETERNA && git add roda-ui/roda-wui/src/main/java/org/roda/wui/client/browse/BrowseAIP.java && git commit -m 'feat: koppla BrowseAIP till CatalogTreePanel via revealAip och resize'"
```

---

### Uppgift 8: Bygg och verifiera manuellt

**Filer:** Inga

- [ ] **Steg 1: Bygg backend och installera sources**

```bash
wsl -d Ubuntu -- bash -c "cd ~/ETERNA && mvn source:jar install -DskipTests 2>&1 | tail -10"
```

Förväntat: `BUILD SUCCESS`

- [ ] **Steg 2: Starta utvecklingsmiljön**

I en WSL-terminal:
```bash
cd ~/ETERNA && ./start-dev.sh
```

I en annan WSL-terminal:
```bash
cd ~/ETERNA && mvn -f dev/codeserver gwt:codeserver -DrodaPath=$(pwd)
```

- [ ] **Steg 3: Aktivera GWT Dev Mode**

1. Gå till http://127.0.0.1:9876/ och spara bokmärkena
2. Gå till http://localhost:8080
3. Klicka bokmärket **Dev Mode On**

- [ ] **Steg 4: Verifiera — rotnoder laddas**

Öppna katalogen. Kontrollera att trädet syns till vänster med rotnoder laddade. Trädet ska ha mörk header med texten "Katalog".

- [ ] **Steg 5: Verifiera — lazy loading**

Klicka på en nod med barn. Kontrollera att ▶ ändras till ○ (laddar) och sedan ▼ när barn laddats. Klicka igen — noden kollapsar till ▶.

- [ ] **Steg 6: Verifiera — löv-noder**

Expandera en nod som saknar barn. Kontrollera att toggle ändras till — och att ingen spinner hänger sig.

- [ ] **Steg 7: Verifiera — auto-synk**

Navigera till ett AIP via sökfunktionen (inte via trädet). Kontrollera att:
- Rätt föräldernoder expanderas automatiskt
- Det navigerade AIP:et markeras i rött i trädet
- Trädet scrollar till den markerade noden

- [ ] **Steg 8: Verifiera — fel vid laddning (valfritt)**

Stoppa Spring Boot tillfälligt och expandera en nod. Kontrollera att felmeddelandet "Kunde inte hämta innehåll" och "Försök igen"-länken visas. Starta om och klicka "Försök igen" — barnen ska laddas.

- [ ] **Steg 9: Verifiera — layout**

Kontrollera att:
- Den höger sidopanelen (representationer, disseminationer) är oförändrad
- Breadcrumben finns kvar
- Layouten håller vid smalare fönster

- [ ] **Steg 10: Push branchen**

```bash
wsl -d Ubuntu -- bash -c "cd ~/ETERNA && git push -u origin feat/catalog-tree"
```

---

## Kända kantsteg att bevaka

**`getAncestors` returnerar ordning:** Endpointen returnerar `[rot, ..., direkt förälder]` — **inte** inkl. target-AIP:et självt. `expandChain` hanterar detta korrekt.

**Root-noder utan parentId:** `EmptyKeyFilterParameter(AIP_PARENT_ID)` filtrerar AIP:er utan förälder. Om en kund har flera parallella toppnivåstrukturer visas alla som separata rotnoder.

**`revealAip` anropas innan rotnoder är laddade:** `loadRootNodes()` och `revealAip()` är båda asynkrona. Om ancestor-svaret kommer innan rotnoderna är renderade hittar inte `findNode` rätt nod och auto-synken uteblir tyst. I praktiken är detta osannolikt (ancestor-anropet är ett extra rundtrip), men om det är ett problem i produktion kan det lösas med en enkel `Scheduler.get().scheduleDeferred()` runt `expandChain`.

**GWT-kompilering krävs för produktionsbuild:** `mvn -pl roda-ui/roda-wui -am gwt:compile -Pdebug-main -Dscope.gwt-dev=compile` — krävs inte för dev mode-test.

/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.client.browse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.v2.index.FindRequest;
import org.roda.core.data.v2.index.filter.Filter;
import org.roda.core.data.v2.index.filter.NotSimpleFilterParameter;
import org.roda.core.data.v2.index.filter.OneOfManyFilterParameter;
import org.roda.core.data.v2.index.filter.SimpleFilterParameter;
import org.roda.core.data.v2.index.sort.SortParameter;
import org.roda.core.data.v2.index.sort.Sorter;
import org.roda.core.data.v2.index.sublist.Sublist;
import org.roda.core.data.v2.ip.IndexedAIP;
import org.roda.wui.client.services.Services;
import org.roda.wui.common.client.ClientLogger;
import org.roda.wui.common.client.tools.DescriptionLevelUtils;
import org.roda.wui.common.client.tools.HistoryUtils;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;

import config.i18n.client.ClientMessages;

public class CatalogTreeNode extends Composite {

  private static final ClientLogger LOGGER = new ClientLogger(CatalogTreeNode.class.getName());
  private static final ClientMessages messages = GWT.create(ClientMessages.class);
  /** Maximum number of child nodes to load per level in the catalog tree. */
  private static final int TREE_MAX_CHILDREN = 10_000;

  private static final String ICON_TOGGLE_COLLAPSED = "<span class='fas fa-chevron-right'></span>";
  private static final String ICON_TOGGLE_EXPANDED = "<span class='fas fa-chevron-down'></span>";
  private static final String ICON_TOGGLE_LOADING = "<span class='fas fa-circle-notch fa-spin'></span>";

  private final String aipId;
  private String title;
  private final int depth;
  private final FlowPanel rootPanel;
  private final FlowPanel rowPanel;
  private final FlowPanel childrenPanel;
  private final HTML toggleHtml;
  private HTML iconHtml;
  private final Map<String, CatalogTreeNode> childNodes = new HashMap<>();
  private Label titleLabel;

  private boolean expanded = false;
  private boolean loaded = false;
  private boolean isLeaf = false;
  private Command pendingOnComplete = null;
  private final boolean ghost;
  private static int ghostCounter = 0;

  public CatalogTreeNode(String aipId, String title, String level, int depth) {
    this.ghost = false;
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

    toggleHtml = new HTML(ICON_TOGGLE_COLLAPSED);
    toggleHtml.setStyleName("catalogTreeToggle");
    rowPanel.add(toggleHtml);

    iconHtml = new HTML(DescriptionLevelUtils.getElementLevelIconSafeHtml(level, false));
    iconHtml.setStyleName("catalogTreeIcon");
    rowPanel.add(iconHtml);

    titleLabel = new Label(title);
    titleLabel.setStyleName("catalogTreeLabel");
    rowPanel.add(titleLabel);

    childrenPanel = new FlowPanel();
    childrenPanel.setStyleName("catalogTreeNodeChildren");
    childrenPanel.setVisible(false);

    toggleHtml.addDomHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        event.stopPropagation();
        toggle();
      }
    }, ClickEvent.getType());

    rowPanel.addDomHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        HistoryUtils.newHistory(BrowseTop.RESOLVER, aipId);
      }
    }, ClickEvent.getType());

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

    // Ghost-noder är alltid utfällda — visa nedåtpil precis som expanderade riktiga noder.
    toggleHtml = new HTML(ICON_TOGGLE_EXPANDED);
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

  public void toggle() {
    if (ghost || isLeaf) return;
    if (expanded) {
      collapse();
    } else {
      expand(null);
    }
  }

  public void expand(Command onComplete) {
    if (ghost) {
      if (onComplete != null) onComplete.execute();
      return;
    }
    if (isLeaf) {
      if (onComplete != null) onComplete.execute();
      return;
    }
    if (loaded) {
      childrenPanel.setVisible(true);
      expanded = true;
      toggleHtml.setHTML(ICON_TOGGLE_EXPANDED);
      if (onComplete != null) onComplete.execute();
    } else {
      loadChildren(onComplete);
    }
  }

  private void loadChildren(Command onComplete) {
    this.pendingOnComplete = onComplete;
    toggleHtml.setHTML(ICON_TOGGLE_LOADING);

    FindRequest findRequest = new FindRequest.FindRequestBuilder(
      new Filter(
        new SimpleFilterParameter(RodaConstants.AIP_PARENT_ID, aipId),
        new NotSimpleFilterParameter(RodaConstants.AIP_LEVEL, "file")),
      false)
      .withSorter(new Sorter(new SortParameter(RodaConstants.AIP_TITLE_SORT, false)))
      .withSublist(new Sublist(0, TREE_MAX_CHILDREN))
      .build();

    Services service = new Services(messages.catalogTreeReasonListChildren(), "get");
    service.rodaEntityRestService(
      s -> s.find(findRequest, LocaleInfo.getCurrentLocale().getLocaleName()),
      IndexedAIP.class)
      .whenComplete((result, error) -> {
        if (error != null) {
          LOGGER.error("Failed to load children for AIP " + aipId, error);
          toggleHtml.setHTML(ICON_TOGGLE_COLLAPSED);
          showLoadError();
          return;
        }
        List<IndexedAIP> children = result.getResults();
        if (children.isEmpty()) {
          loadGhostChildrenFallback(onComplete);
          return;
        }
        loaded = true;
        for (IndexedAIP child : children) {
          CatalogTreeNode childNode = new CatalogTreeNode(child.getId(), child.getTitle(), child.getLevel(), depth + 1);
          childNodes.put(child.getId(), childNode);
          childrenPanel.add(childNode);
        }
        childrenPanel.setVisible(true);
        expanded = true;
        toggleHtml.setHTML(ICON_TOGGLE_EXPANDED);
        if (onComplete != null) onComplete.execute();
      });
  }

  /**
   * Fallback: inga direkt tillgängliga barn hittades. Söker tillgängliga ättlingar
   * och bygger ghost-noder för otillgängliga mellannivåer. Använder bara 2 nätverksanrop
   * oavsett antalet ättlingar.
   */
  private void loadGhostChildrenFallback(final Command onComplete) {
    FindRequest findRequest = new FindRequest.FindRequestBuilder(
      new Filter(
        new SimpleFilterParameter(RodaConstants.AIP_ANCESTORS, aipId),
        new NotSimpleFilterParameter(RodaConstants.AIP_LEVEL, "file")),
      false)
      .withSorter(new Sorter(new SortParameter(RodaConstants.AIP_TITLE_SORT, false)))
      .withSublist(new Sublist(0, TREE_MAX_CHILDREN))
      .build();

    Services service = new Services(messages.catalogTreeReasonListChildren(), "get");
    service.rodaEntityRestService(
      s -> s.find(findRequest, LocaleInfo.getCurrentLocale().getLocaleName()),
      IndexedAIP.class)
      .whenComplete((result, error) -> {
        if (error != null) {
          LOGGER.error("Ghost fallback: failed to query descendants for AIP " + aipId, error);
          markAsLeaf();
          if (onComplete != null) onComplete.execute();
          return;
        }
        List<IndexedAIP> descendants = result.getResults();
        if (descendants.isEmpty()) {
          markAsLeaf();
          if (onComplete != null) onComplete.execute();
          return;
        }

        // Bygg resolvedAncestors: börja med alla tillgängliga ättlingar
        final Map<String, IndexedAIP> resolvedAncestors = new HashMap<>();
        for (IndexedAIP desc : descendants) {
          resolvedAncestors.put(desc.getId(), desc);
        }

        // Samla okända förfäder-ID:n (exklusive denna nod och redan kända)
        Set<String> ancestorIdsToResolve = new LinkedHashSet<>();
        for (IndexedAIP desc : descendants) {
          if (desc.getAncestors() != null) {
            for (String ancId : desc.getAncestors()) {
              if (!aipId.equals(ancId) && !resolvedAncestors.containsKey(ancId)) {
                ancestorIdsToResolve.add(ancId);
              }
            }
          }
        }

        if (ancestorIdsToResolve.isEmpty()) {
          buildGhostChildrenFromDescendants(descendants, resolvedAncestors, onComplete);
          return;
        }

        // Hämta data för okända förfäder i ett enda batch-anrop
        FindRequest ancestorRequest = new FindRequest.FindRequestBuilder(
          new Filter(new OneOfManyFilterParameter(RodaConstants.INDEX_UUID,
            new ArrayList<>(ancestorIdsToResolve))),
          false)
          .withSublist(new Sublist(0, ancestorIdsToResolve.size()))
          .build();

        Services s2 = new Services(messages.catalogTreeReasonGetAncestors(), "get");
        s2.rodaEntityRestService(
          s -> s.find(ancestorRequest, LocaleInfo.getCurrentLocale().getLocaleName()),
          IndexedAIP.class)
          .whenComplete((ancResult, err) -> {
            if (err == null) {
              for (IndexedAIP anc : ancResult.getResults()) {
                resolvedAncestors.put(anc.getId(), anc);
              }
            } else {
              LOGGER.warn("Ghost fallback: could not resolve ancestors for AIP " + aipId
                + "; inaccessible intermediates become ghost nodes");
            }
            buildGhostChildrenFromDescendants(descendants, resolvedAncestors, onComplete);
          });
      });
  }

  private void buildGhostChildrenFromDescendants(List<IndexedAIP> descendants,
      Map<String, IndexedAIP> resolvedAncestors, Command onComplete) {
    Map<String, CatalogTreeNode> localNodeMap = new LinkedHashMap<>();
    List<CatalogTreeNode> directChildren = new ArrayList<>();
    for (IndexedAIP desc : descendants) {
      List<String> ancestorIds = desc.getAncestors() != null
        ? new ArrayList<>(desc.getAncestors())
        : new ArrayList<>();
      Collections.reverse(ancestorIds);
      insertGhostChainUnderNode(ancestorIds, desc, resolvedAncestors, localNodeMap, directChildren);
    }
    for (CatalogTreeNode child : directChildren) {
      addPrebuiltChild(child);
    }
    if (directChildren.isEmpty()) {
      markAsLeaf();
    }
    if (onComplete != null) onComplete.execute();
  }

  /**
   * Bygger ghost-kedja under denna nod baserat på förfäderslistan (top-till-bottom).
   * Förfäder vars ID finns i resolvedAncestors får riktiga noder; övriga blir ghost-noder.
   * Noder dedupliceras via localNodeMap med faktiska ID:n som nyckel.
   */
  private void insertGhostChainUnderNode(List<String> ancestorIds, IndexedAIP targetAip,
      Map<String, IndexedAIP> resolvedAncestors, Map<String, CatalogTreeNode> localNodeMap,
      List<CatalogTreeNode> directChildren) {

    int currentNodeIndex = -1;
    for (int i = 0; i < ancestorIds.size(); i++) {
      if (aipId.equals(ancestorIds.get(i))) {
        currentNodeIndex = i;
        break;
      }
    }
    if (currentNodeIndex == -1) return;

    CatalogTreeNode parent = null;
    for (int i = currentNodeIndex + 1; i < ancestorIds.size(); i++) {
      String ancId = ancestorIds.get(i);
      int nodeDepth = depth + (i - currentNodeIndex);
      if (localNodeMap.containsKey(ancId)) {
        parent = localNodeMap.get(ancId);
        continue;
      }
      CatalogTreeNode node;
      if (resolvedAncestors.containsKey(ancId)) {
        IndexedAIP anc = resolvedAncestors.get(ancId);
        node = new CatalogTreeNode(anc.getId(), anc.getTitle(), anc.getLevel(), nodeDepth);
      } else {
        node = CatalogTreeNode.createGhostNode(nodeDepth);
      }
      localNodeMap.put(ancId, node);
      if (parent == null) directChildren.add(node);
      else parent.addPrebuiltChild(node);
      parent = node;
    }

    if (!localNodeMap.containsKey(targetAip.getId())) {
      int targetDepth = depth + (ancestorIds.size() - currentNodeIndex);
      CatalogTreeNode targetNode = new CatalogTreeNode(
        targetAip.getId(), targetAip.getTitle(), targetAip.getLevel(), targetDepth);
      localNodeMap.put(targetAip.getId(), targetNode);
      if (parent == null) directChildren.add(targetNode);
      else parent.addPrebuiltChild(targetNode);
    }
  }

  private void markAsLeaf() {
    isLeaf = true;
    toggleHtml.setHTML("");
  }

  private void showLoadError() {
    FlowPanel errorPanel = new FlowPanel();
    errorPanel.setStyleName("catalogTreeNodeError");
    Label errorLabel = new Label(messages.catalogTreeLoadError());
    Anchor retryLink = new Anchor(messages.catalogTreeRetry());
    retryLink.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        errorPanel.removeFromParent();
        loaded = false;
        loadChildren(pendingOnComplete);
      }
    });
    errorPanel.add(errorLabel);
    errorPanel.add(retryLink);
    rootPanel.insert(errorPanel, 1);
  }

  public void collapse() {
    if (ghost) return;
    childrenPanel.setVisible(false);
    expanded = false;
    toggleHtml.setHTML(ICON_TOGGLE_COLLAPSED);
  }

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

  public void updateTitle(String newTitle) {
    title = newTitle != null ? newTitle : "";
    titleLabel.setText(title);
  }

  public void updateLevel(String level) {
    iconHtml.setHTML(DescriptionLevelUtils.getElementLevelIconSafeHtml(level, false));
  }

  public void removeChild(String aipId) {
    CatalogTreeNode child = childNodes.remove(aipId);
    if (child != null) {
      child.removeFromParent();
    }
    if (childNodes.isEmpty() && loaded) {
      markAsLeaf();
    }
  }

  public void invalidateChildren() {
    if (ghost) return;
    loaded = false;
    expanded = false;
    isLeaf = false;
    childNodes.clear();
    childrenPanel.clear();
    childrenPanel.setVisible(false);
    toggleHtml.setHTML(ICON_TOGGLE_COLLAPSED);
  }
}

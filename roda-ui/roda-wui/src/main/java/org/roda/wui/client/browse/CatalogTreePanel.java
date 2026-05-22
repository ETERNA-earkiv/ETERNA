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
import org.roda.core.data.v2.index.filter.EmptyKeyFilterParameter;
import org.roda.core.data.v2.index.filter.Filter;
import org.roda.core.data.v2.index.filter.NotSimpleFilterParameter;
import org.roda.core.data.v2.index.filter.OneOfManyFilterParameter;
import org.roda.core.data.v2.index.sort.SortParameter;
import org.roda.core.data.v2.index.sort.Sorter;
import org.roda.core.data.v2.index.sublist.Sublist;
import org.roda.core.data.v2.ip.IndexedAIP;
import org.roda.wui.client.services.Services;

import org.roda.wui.common.client.ClientLogger;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import config.i18n.client.ClientMessages;

public class CatalogTreePanel extends Composite {

  private static final ClientLogger LOGGER = new ClientLogger(CatalogTreePanel.class.getName());
  private static final ClientMessages messages = GWT.create(ClientMessages.class);
  private static final MyUiBinder uiBinder = GWT.create(MyUiBinder.class);
  /** Maximum number of root nodes to load in the catalog tree. */
  private static final int TREE_MAX_CHILDREN = 10_000;

  private static final String ICON_COLLAPSE = "<i class='fas fa-angle-double-left'></i>";
  private static final String ICON_EXPAND = "<i class='fas fa-angle-double-right'></i>";

  interface MyUiBinder extends UiBinder<Widget, CatalogTreePanel> {}

  @UiField
  FlowPanel treeBody;

  @UiField
  TextBox filterInput;

  @UiField
  Button collapseToggle;

  @UiField
  Button expandButton;

  @UiField
  Anchor headerTitle;

  private static CatalogTreePanel instance = null;

  public static CatalogTreePanel getInstance() {
    if (instance == null) {
      instance = new CatalogTreePanel();
    }
    return instance;
  }

  private final Map<String, CatalogTreeNode> rootNodes = new HashMap<>();
  private CatalogTreeNode selectedNode = null;
  private boolean rootsLoaded = false;
  private boolean rootsLoading = false;
  private int loadGeneration = 0;
  private int revealGeneration = 0;
  private String pendingRevealAipId = null;

  public CatalogTreePanel() {
    initWidget(uiBinder.createAndBindUi(this));
    filterInput.getElement().setAttribute("placeholder", messages.catalogTreeFilterPlaceholder());
    filterInput.addKeyUpHandler(new KeyUpHandler() {
      @Override
      public void onKeyUp(KeyUpEvent event) {
        onFilterChanged(event);
      }
    });

    collapseToggle.setHTML(ICON_COLLAPSE);
    collapseToggle.setTitle(messages.catalogTreeCollapse());
    collapseToggle.getElement().setAttribute("aria-label", messages.catalogTreeCollapse());
    collapseToggle.getElement().setAttribute("aria-expanded", "true");
    collapseToggle.addClickHandler(e -> {
      e.stopPropagation();
      setCollapsed(true);
    });

    expandButton.setHTML(ICON_EXPAND);
    expandButton.setTitle(messages.catalogTreeExpand());
    expandButton.getElement().setAttribute("aria-label", messages.catalogTreeExpand());
    expandButton.getElement().setAttribute("aria-expanded", "false");
    expandButton.addClickHandler(e -> setCollapsed(false));

    headerTitle.setTitle(messages.catalogTreeTitle());
    headerTitle.setHref(org.roda.wui.common.client.tools.HistoryUtils.createHistoryHashLink(BrowseTop.RESOLVER));

    loadRootNodes();
  }

  private void setCollapsed(boolean collapse) {
    addStyleName("animatingCollapse");
    if (collapse) {
      addStyleName("collapsed");
      getElement().getStyle().clearWidth();
    } else {
      removeStyleName("collapsed");
    }
    collapseToggle.getElement().setAttribute("aria-expanded", collapse ? "false" : "true");
    expandButton.getElement().setAttribute("aria-expanded", collapse ? "false" : "true");
    new Timer() {
      @Override
      public void run() {
        removeStyleName("animatingCollapse");
      }
    }.schedule(220);
  }

  private void onFilterChanged(KeyUpEvent event) {
    String query = filterInput.getText().trim().toLowerCase();
    for (CatalogTreeNode node : rootNodes.values()) {
      applyFilter(node, query);
    }
  }

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

  private void loadRootNodes() {
    loadGeneration++;
    clearSelection();
    treeBody.clear();
    rootNodes.clear();
    rootsLoaded = false;
    rootsLoading = true;
    FindRequest findRequest = new FindRequest.FindRequestBuilder(
      new Filter(
        new EmptyKeyFilterParameter(RodaConstants.AIP_PARENT_ID),
        new NotSimpleFilterParameter(RodaConstants.AIP_LEVEL, "file")),
      false)
      .withSorter(new Sorter(new SortParameter(RodaConstants.AIP_TITLE_SORT, false)))
      .withSublist(new Sublist(0, TREE_MAX_CHILDREN))
      .build();

    Services service = new Services(messages.catalogTreeReasonListRoots(), "get");
    service.rodaEntityRestService(
      s -> s.find(findRequest, LocaleInfo.getCurrentLocale().getLocaleName()),
      IndexedAIP.class)
      .whenComplete((result, error) -> {
        rootsLoading = false;
        if (error != null) {
          LOGGER.error("Failed to load catalog tree root nodes", error);
          treeBody.clear();
          FlowPanel errorPanel = new FlowPanel();
          errorPanel.addStyleName("catalogTreeNodeError");
          errorPanel.add(new com.google.gwt.user.client.ui.Label(messages.catalogTreeLoadError()));
          com.google.gwt.user.client.ui.Anchor retry = new com.google.gwt.user.client.ui.Anchor(messages.catalogTreeRetry());
          retry.addClickHandler(event -> loadRootNodes());
          errorPanel.add(retry);
          treeBody.add(errorPanel);
          return;
        }
        if (result.getResults().isEmpty()) {
          loadFallbackGhostTree();
          return;
        }
        final int myGeneration = loadGeneration;
        Set<String> accessibleRootIds = new HashSet<>();
        for (IndexedAIP aip : result.getResults()) {
          CatalogTreeNode node = new CatalogTreeNode(aip.getId(), aip.getTitle(), aip.getLevel(), 0);
          rootNodes.put(aip.getId(), node);
          treeBody.add(node);
          accessibleRootIds.add(aip.getId());
        }
        // Stay in loading state until supplementary ghost-root check completes.
        rootsLoading = true;
        loadSupplementaryGhostRoots(accessibleRootIds, myGeneration);
      });
  }

  /**
   * Körs efter att loadRootNodes() hittat tillgängliga toppnoder.
   * Söker tillgängliga AIP:er vars rot-förfader INTE finns bland de tillgängliga toppnoderna
   * och bygger ghost-kedjor för dessa så att de syns i trädet.
   * Använder max 2 nätverksanrop oavsett antalet AIP:er.
   */
  private void loadSupplementaryGhostRoots(Set<String> accessibleRootIds, int myGeneration) {
    FindRequest findRequest = new FindRequest.FindRequestBuilder(
      new Filter(new NotSimpleFilterParameter(RodaConstants.AIP_LEVEL, "file")),
      false)
      .withSorter(new Sorter(new SortParameter(RodaConstants.AIP_TITLE_SORT, false)))
      .withSublist(new Sublist(0, TREE_MAX_CHILDREN))
      .build();

    Services service = new Services(messages.catalogTreeReasonListRoots(), "get");
    service.rodaEntityRestService(
      s -> s.find(findRequest, LocaleInfo.getCurrentLocale().getLocaleName()),
      IndexedAIP.class)
      .whenComplete((result, error) -> {
        if (myGeneration != loadGeneration) return;
        if (error != null) {
          LOGGER.warn("Supplementary ghost-root query failed; tree may be incomplete: " + error.getMessage());
          finalizeFallbackTree(new ArrayList<>(), myGeneration);
          return;
        }
        List<IndexedAIP> allAccessible = result.getResults();

        // Hitta AIP:er vars yttersta förfader INTE finns bland tillgängliga toppnoder
        List<IndexedAIP> needsGhostRoot = new ArrayList<>();
        for (IndexedAIP aip : allAccessible) {
          List<String> ancestors = aip.getAncestors();
          if (ancestors == null || ancestors.isEmpty()) {
            // AIP:et är självt en toppnod — redan hanterat av loadRootNodes()
            continue;
          }
          // ancestors är bottom-to-top: sista elementet är yttersta förfadern (roten)
          String rootAncestorId = ancestors.get(ancestors.size() - 1);
          if (!accessibleRootIds.contains(rootAncestorId)) {
            needsGhostRoot.add(aip);
          }
        }

        if (needsGhostRoot.isEmpty()) {
          finalizeFallbackTree(new ArrayList<>(), myGeneration);
          return;
        }

        // Bygg resolvedAncestors: börja med alla tillgängliga AIP:er
        final Map<String, IndexedAIP> resolvedAncestors = new HashMap<>();
        for (IndexedAIP aip : allAccessible) {
          resolvedAncestors.put(aip.getId(), aip);
        }

        // Samla förfäder-ID:n som ännu inte är kända
        Set<String> ancestorIdsToResolve = new LinkedHashSet<>();
        for (IndexedAIP aip : needsGhostRoot) {
          if (aip.getAncestors() != null) {
            for (String ancId : aip.getAncestors()) {
              if (!resolvedAncestors.containsKey(ancId)) {
                ancestorIdsToResolve.add(ancId);
              }
            }
          }
        }

        if (ancestorIdsToResolve.isEmpty()) {
          buildAndMergeGhostRoots(needsGhostRoot, resolvedAncestors, myGeneration);
          return;
        }

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
            if (myGeneration != loadGeneration) return;
            if (err == null) {
              for (IndexedAIP anc : ancResult.getResults()) {
                resolvedAncestors.put(anc.getId(), anc);
              }
            } else {
              LOGGER.warn("Could not batch-resolve ghost ancestors; inaccessible intermediates become ghost nodes");
            }
            buildAndMergeGhostRoots(needsGhostRoot, resolvedAncestors, myGeneration);
          });
      });
  }

  private void buildAndMergeGhostRoots(List<IndexedAIP> aips,
      Map<String, IndexedAIP> resolvedAncestors, int expectedGeneration) {
    Map<String, CatalogTreeNode> nodeMap = new LinkedHashMap<>();
    List<CatalogTreeNode> newRoots = new ArrayList<>();
    for (IndexedAIP aip : aips) {
      List<String> ancestorIds = aip.getAncestors() != null
        ? new ArrayList<>(aip.getAncestors())
        : new ArrayList<>();
      Collections.reverse(ancestorIds);
      insertChain(ancestorIds, aip, resolvedAncestors, nodeMap, newRoots);
    }
    finalizeFallbackTree(newRoots, expectedGeneration);
  }

  private void loadFallbackGhostTree() {
    final int myGeneration = loadGeneration;
    rootsLoading = true;
    FindRequest findRequest = new FindRequest.FindRequestBuilder(
      new Filter(new NotSimpleFilterParameter(RodaConstants.AIP_LEVEL, "file")),
      false)
      .withSorter(new Sorter(new SortParameter(RodaConstants.AIP_TITLE_SORT, false)))
      .withSublist(new Sublist(0, TREE_MAX_CHILDREN))
      .build();

    Services service = new Services(messages.catalogTreeReasonListRoots(), "get");
    service.rodaEntityRestService(
      s -> s.find(findRequest, LocaleInfo.getCurrentLocale().getLocaleName()),
      IndexedAIP.class)
      .whenComplete((result, error) -> {
        if (error != null) {
          LOGGER.error("Fallback ghost tree query failed", error);
          if (myGeneration != loadGeneration) return;
          rootsLoading = false;
          rootsLoaded = true;
          return;
        }
        List<IndexedAIP> aips = result.getResults();
        if (aips.isEmpty()) {
          if (myGeneration != loadGeneration) return;
          rootsLoading = false;
          rootsLoaded = true;
          return;
        }
        if (aips.size() == TREE_MAX_CHILDREN) {
          LOGGER.warn("Fallback ghost tree capped at " + TREE_MAX_CHILDREN + " AIPs; some accessible objects may not be shown");
        }

        // Bygg resolvedAncestors: börja med alla tillgängliga AIP:er
        final Map<String, IndexedAIP> resolvedAncestors = new HashMap<>();
        for (IndexedAIP aip : aips) {
          resolvedAncestors.put(aip.getId(), aip);
        }

        // Samla förfäder-ID:n som ännu inte är kända
        Set<String> ancestorIdsToResolve = new LinkedHashSet<>();
        for (IndexedAIP aip : aips) {
          if (aip.getAncestors() != null) {
            for (String ancId : aip.getAncestors()) {
              if (!resolvedAncestors.containsKey(ancId)) {
                ancestorIdsToResolve.add(ancId);
              }
            }
          }
        }

        if (ancestorIdsToResolve.isEmpty()) {
          buildAndFinalizeFallbackTree(aips, resolvedAncestors, myGeneration);
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
          .whenComplete((ancestorResult, err) -> {
            if (myGeneration != loadGeneration) return;
            if (err == null) {
              for (IndexedAIP anc : ancestorResult.getResults()) {
                resolvedAncestors.put(anc.getId(), anc);
              }
            } else {
              LOGGER.warn("Could not batch-resolve ancestors; inaccessible intermediates become ghost nodes");
            }
            buildAndFinalizeFallbackTree(aips, resolvedAncestors, myGeneration);
          });
      });
  }

  private void buildAndFinalizeFallbackTree(List<IndexedAIP> aips,
      Map<String, IndexedAIP> resolvedAncestors, int expectedGeneration) {
    Map<String, CatalogTreeNode> nodeMap = new LinkedHashMap<>();
    List<CatalogTreeNode> roots = new ArrayList<>();
    for (IndexedAIP aip : aips) {
      List<String> ancestorIds = aip.getAncestors() != null
        ? new ArrayList<>(aip.getAncestors())
        : new ArrayList<>();
      Collections.reverse(ancestorIds);
      insertChain(ancestorIds, aip, resolvedAncestors, nodeMap, roots);
    }
    finalizeFallbackTree(roots, expectedGeneration);
  }

  /**
   * Bygger en nod-kedja top-till-bottom. Förfäder vars ID finns i resolvedAncestors
   * får riktiga noder; övriga blir ghost-noder. Noder dedupliceras via nodeMap.
   */
  private void insertChain(List<String> ancestorIds, IndexedAIP targetAip,
      Map<String, IndexedAIP> resolvedAncestors, Map<String, CatalogTreeNode> nodeMap,
      List<CatalogTreeNode> roots) {
    CatalogTreeNode parent = null;
    for (int i = 0; i < ancestorIds.size(); i++) {
      String ancId = ancestorIds.get(i);
      if (nodeMap.containsKey(ancId)) {
        parent = nodeMap.get(ancId);
        continue;
      }
      CatalogTreeNode node;
      if (resolvedAncestors.containsKey(ancId)) {
        IndexedAIP anc = resolvedAncestors.get(ancId);
        node = new CatalogTreeNode(anc.getId(), anc.getTitle(), anc.getLevel(), i);
      } else {
        node = CatalogTreeNode.createGhostNode(i);
      }
      nodeMap.put(ancId, node);
      if (parent == null) roots.add(node);
      else parent.addPrebuiltChild(node);
      parent = node;
    }
    if (nodeMap.containsKey(targetAip.getId())) return;
    CatalogTreeNode targetNode = new CatalogTreeNode(
      targetAip.getId(), targetAip.getTitle(), targetAip.getLevel(), ancestorIds.size());
    nodeMap.put(targetAip.getId(), targetNode);
    if (parent == null) roots.add(targetNode);
    else parent.addPrebuiltChild(targetNode);
  }

  private void finalizeFallbackTree(List<CatalogTreeNode> roots, int expectedGeneration) {
    if (expectedGeneration != loadGeneration) return;
    for (CatalogTreeNode root : roots) {
      rootNodes.put(root.getAipId(), root);
      treeBody.add(root);
    }
    rootsLoading = false;
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

  private void doRevealAip(String aipId) {
    final int myRevealGen = ++revealGeneration;
    // Hämta AIP:et för att få Solr-fältets förfäder-ID:n (fullständig kedja, inklusive otillgängliga)
    Services service = new Services(messages.catalogTreeReasonGetAncestors(), "get");
    service.rodaEntityRestService(
      s -> s.findByUuid(aipId, LocaleInfo.getCurrentLocale().getLocaleName()),
      IndexedAIP.class)
      .whenComplete((aip, error) -> {
        if (myRevealGen != revealGeneration) return;
        if (error != null) {
          LOGGER.warn("Could not fetch AIP " + aipId + " for tree sync, skipping");
          return;
        }
        List<String> ancestorIds = aip.getAncestors(); // bottom-to-top från Solr
        if (ancestorIds == null || ancestorIds.isEmpty()) {
          selectNode(aipId);
          return;
        }
        List<String> topToBottom = new ArrayList<>(ancestorIds);
        Collections.reverse(topToBottom);
        expandChainByIds(topToBottom, 0, aipId);
      });
  }

  /**
   * Expanderar förfäder i trädet baserat på ID-lista (top-till-bottom).
   * Otillgängliga förfäder (ghost-noder) hoppas över — de är alltid expanderade.
   * Reala noder expanderas i ordning; när sista förfadern är klar väljs målnoden.
   */
  private void expandChainByIds(List<String> ancestorIds, int index, String targetId) {
    if (index >= ancestorIds.size()) {
      selectNode(targetId);
      return;
    }
    String ancestorId = ancestorIds.get(index);
    CatalogTreeNode node = findNode(ancestorId, rootNodes);
    if (node == null || node.isGhostNode()) {
      // Antingen otillgänglig (ghost, redan expanderad) eller ännu inte synlig — fortsätt
      expandChainByIds(ancestorIds, index + 1, targetId);
      return;
    }
    node.expand(new com.google.gwt.user.client.Command() {
      @Override
      public void execute() {
        expandChainByIds(ancestorIds, index + 1, targetId);
      }
    });
  }

  public void clearSelection() {
    if (selectedNode != null) {
      selectedNode.deselect();
      selectedNode = null;
    }
    headerTitle.addStyleName("active");
  }

  private void selectNode(String aipId) {
    headerTitle.removeStyleName("active");
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

  public void removeNodeAnywhere(String aipId) {
    CatalogTreeNode rootNode = rootNodes.remove(aipId);
    if (rootNode != null) {
      rootNode.removeFromParent();
    } else {
      removeNodeFromSubtree(aipId, rootNodes);
    }
    if (selectedNode != null && aipId.equals(selectedNode.getAipId())) {
      clearSelection();
    }
  }

  private boolean removeNodeFromSubtree(String aipId, Map<String, CatalogTreeNode> nodes) {
    for (CatalogTreeNode node : nodes.values()) {
      if (node.getChildNodes().containsKey(aipId)) {
        node.removeChild(aipId);
        return true;
      }
      if (removeNodeFromSubtree(aipId, node.getChildNodes())) {
        return true;
      }
    }
    return false;
  }

  public void removeNode(String aipId, String parentAipId) {
    if (parentAipId == null || parentAipId.isEmpty()) {
      CatalogTreeNode node = rootNodes.remove(aipId);
      if (node != null) {
        node.removeFromParent();
      }
    } else {
      CatalogTreeNode parent = findNode(parentAipId, rootNodes);
      if (parent != null) {
        parent.removeChild(aipId);
      }
    }
    if (selectedNode != null && aipId.equals(selectedNode.getAipId())) {
      clearSelection();
    }
  }

  public void refreshNodeTitle(String aipId) {
    CatalogTreeNode node = findNode(aipId, rootNodes);
    if (node == null) return;
    Services service = new Services(messages.catalogTreeReasonRetrieveAIP(), "get");
    service.rodaEntityRestService(
      s -> s.findByUuid(aipId, LocaleInfo.getCurrentLocale().getLocaleName()),
      IndexedAIP.class)
      .whenComplete((aip, error) -> {
        if (error != null) {
          // Node keeps its current title as fallback — non-critical background refresh
          LOGGER.error("Could not refresh title for AIP " + aipId + ": " + error.getMessage(), error);
          return;
        }
        node.updateTitle(aip.getTitle());
        node.updateLevel(aip.getLevel());
      });
  }

  public void reloadRootNodes() {
    loadRootNodes();
  }

  public void refreshAfterMove(String oldParentId, String newParentId) {
    boolean rootInvolved = (oldParentId == null || oldParentId.isEmpty())
      || (newParentId == null || newParentId.isEmpty());
    if (rootInvolved) {
      loadRootNodes();
    } else {
      refreshSubtree(oldParentId);
      refreshSubtree(newParentId);
    }
  }

  public void refreshSubtree(String parentAipId) {
    if (parentAipId == null || parentAipId.isEmpty()) {
      // Mark stale without clearing the visible tree. loadRootNodes() is triggered
      // lazily by the next revealAip() call (e.g. after saving metadata in BrowseAIP).
      rootsLoaded = false;
    } else {
      CatalogTreeNode parent = findNode(parentAipId, rootNodes);
      if (parent != null) {
        parent.invalidateChildren();
      }
    }
  }
}

/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.client.browse;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.v2.index.FindRequest;
import org.roda.core.data.v2.index.filter.EmptyKeyFilterParameter;
import org.roda.core.data.v2.index.filter.Filter;
import org.roda.core.data.v2.index.filter.NotSimpleFilterParameter;
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

  interface MyUiBinder extends UiBinder<Widget, CatalogTreePanel> {}

  @UiField
  FlowPanel treeBody;

  @UiField
  TextBox filterInput;

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
    loadRootNodes();
  }

  private void onFilterChanged(KeyUpEvent event) {
    String query = filterInput.getText().trim().toLowerCase();
    for (CatalogTreeNode node : rootNodes.values()) {
      applyFilter(node, query);
    }
  }

  private boolean applyFilter(CatalogTreeNode node, String query) {
    boolean selfMatches = query.isEmpty() || (node.getTitle() != null && node.getTitle().toLowerCase().contains(query));
    boolean childMatches = false;
    for (CatalogTreeNode child : node.getChildNodes().values()) {
      if (applyFilter(child, query)) childMatches = true;
    }
    node.setVisible(selfMatches || childMatches);
    return selfMatches || childMatches;
  }

  private void loadRootNodes() {
    rootsLoading = true;
    FindRequest findRequest = new FindRequest.FindRequestBuilder(
      new Filter(
        new EmptyKeyFilterParameter(RodaConstants.AIP_PARENT_ID),
        new NotSimpleFilterParameter(RodaConstants.AIP_LEVEL, "file"),
        new NotSimpleFilterParameter(RodaConstants.AIP_LEVEL, "item")),
      false)
      .withSorter(new Sorter(new SortParameter(RodaConstants.AIP_TITLE_SORT, false)))
      .withSublist(new Sublist(0, TREE_MAX_CHILDREN))
      .build();

    Services service = new Services(messages.catalogTreeLoadingLabel(), "get");
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
        for (IndexedAIP aip : result.getResults()) {
          CatalogTreeNode node = new CatalogTreeNode(aip.getId(), aip.getTitle(), 0);
          rootNodes.put(aip.getId(), node);
          treeBody.add(node);
        }
        rootsLoaded = true;
        if (pendingRevealAipId != null) {
          String id = pendingRevealAipId;
          pendingRevealAipId = null;
          doRevealAip(id);
        }
      });
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
    Services service = new Services(messages.catalogTreeLoadingLabel(), "get");
    service.aipResource(s -> s.getAncestors(aipId))
      .whenComplete((ancestors, error) -> {
        if (error != null) {
          LOGGER.warn("Could not fetch ancestors for AIP " + aipId + ", auto-sync skipped");
          return;
        }
        Collections.reverse(ancestors);
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
    node.expand(new com.google.gwt.user.client.Command() {
      @Override
      public void execute() {
        expandChain(ancestors, index + 1, targetId);
      }
    });
  }

  public void clearSelection() {
    if (selectedNode != null) {
      selectedNode.deselect();
      selectedNode = null;
    }
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

  public void refreshSubtree(String parentAipId) {
    if (parentAipId == null || parentAipId.isEmpty()) {
      rootNodes.clear();
      treeBody.clear();
      rootsLoaded = false;
      rootsLoading = false;
      pendingRevealAipId = null;
      // Reload triggered lazily by next revealAip call (after metadata is saved)
    } else {
      CatalogTreeNode parent = findNode(parentAipId, rootNodes);
      if (parent != null) {
        parent.invalidateChildren();
      }
    }
  }
}

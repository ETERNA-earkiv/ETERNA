/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/keeps/roda
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
import org.roda.core.data.v2.ip.IndexedAIP;
import org.roda.wui.client.services.Services;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

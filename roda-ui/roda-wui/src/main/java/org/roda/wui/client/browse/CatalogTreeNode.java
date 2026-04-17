/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/keeps/roda
 */
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
import com.google.gwt.event.dom.client.ClickHandler;
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
  private Command pendingOnComplete = null;

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

    rowPanel.addDomHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        toggle();
      }
    }, ClickEvent.getType());

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
    this.pendingOnComplete = onComplete;
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

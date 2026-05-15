/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.client.browse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.v2.index.FindRequest;
import org.roda.core.data.v2.index.filter.Filter;
import org.roda.core.data.v2.index.filter.NotSimpleFilterParameter;
import org.roda.core.data.v2.index.filter.SimpleFilterParameter;
import org.roda.core.data.v2.index.sort.SortParameter;
import org.roda.core.data.v2.index.sort.Sorter;
import org.roda.core.data.v2.index.sublist.Sublist;
import org.roda.core.data.v2.ip.IndexedAIP;
import org.roda.wui.client.services.Services;
import org.roda.wui.common.client.ClientLogger;
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
  private static final String ICON_FOLDER_CLOSED = "<span class='fas fa-folder'></span>";
  private static final String ICON_FOLDER_OPEN_STR = "<span class='fas fa-folder-open'></span>";
  private static final String ICON_FILE_LEAF = "<span class='fas fa-folder'></span>";

  private final String aipId;
  private String title;
  private final int depth;
  private final FlowPanel rootPanel;
  private final FlowPanel rowPanel;
  private final FlowPanel childrenPanel;
  private final HTML toggleHtml;
  private final HTML iconHtml;
  private final Map<String, CatalogTreeNode> childNodes = new HashMap<>();
  private Label titleLabel;

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

    toggleHtml = new HTML(ICON_TOGGLE_COLLAPSED);
    toggleHtml.setStyleName("catalogTreeToggle");
    rowPanel.add(toggleHtml);

    iconHtml = new HTML(ICON_FOLDER_CLOSED);
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
      toggleHtml.setHTML(ICON_TOGGLE_EXPANDED);
      iconHtml.setHTML(ICON_FOLDER_OPEN_STR);
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

    Services service = new Services(messages.catalogTreeLoadingLabel(), "get");
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
          toggleHtml.setHTML(ICON_TOGGLE_EXPANDED);
          iconHtml.setHTML(ICON_FOLDER_OPEN_STR);
        }
        if (onComplete != null) onComplete.execute();
      });
  }

  private void markAsLeaf() {
    isLeaf = true;
    toggleHtml.setHTML("");
    iconHtml.setHTML(ICON_FILE_LEAF);
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
    toggleHtml.setHTML(ICON_TOGGLE_COLLAPSED);
    iconHtml.setHTML(ICON_FOLDER_CLOSED);
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
    loaded = false;
    expanded = false;
    isLeaf = false;
    childNodes.clear();
    childrenPanel.clear();
    childrenPanel.setVisible(false);
    toggleHtml.setHTML(ICON_TOGGLE_COLLAPSED);
    iconHtml.setHTML(ICON_FOLDER_CLOSED);
  }
}

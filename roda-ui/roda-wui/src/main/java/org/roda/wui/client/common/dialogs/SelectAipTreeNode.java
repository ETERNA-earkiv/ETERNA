/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.client.common.dialogs;

import java.util.List;
import java.util.Set;

import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.v2.index.FindRequest;
import org.roda.core.data.v2.index.filter.Filter;
import org.roda.core.data.v2.index.filter.SimpleFilterParameter;
import org.roda.core.data.v2.index.sort.SortParameter;
import org.roda.core.data.v2.index.sort.Sorter;
import org.roda.core.data.v2.index.sublist.Sublist;
import org.roda.core.data.v2.ip.IndexedAIP;
import org.roda.wui.client.services.Services;
import org.roda.wui.common.client.ClientLogger;
import org.roda.wui.common.client.tools.DescriptionLevelUtils;

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

/**
 * Selectable, lazy-loading tree node for the parent-node selector ({@link SelectAipTreeDialog}).
 *
 * <p>
 * Independent from the catalog tree's {@link org.roda.wui.client.browse.CatalogTreeNode} per ADR 0001:
 * a row click selects the node (instead of navigating), nodes can be greyed out (non-selectable) and
 * the whole {@link IndexedAIP} object is carried so the value contract towards the call sites can be
 * honored. Only logical units are loaded — the {@code file} and {@code item} levels are excluded. No
 * ghost-node handling.
 * </p>
 */
public class SelectAipTreeNode extends Composite implements Selectable {

  /** Listener notified when a (selectable) node is selected. */
  public interface SelectionListener {
    void onSelect(IndexedAIP aip, Selectable widget);
  }

  private static final ClientLogger LOGGER = new ClientLogger(SelectAipTreeNode.class.getName());
  private static final ClientMessages messages = GWT.create(ClientMessages.class);
  /** Maximum number of child nodes to load per level. */
  private static final int TREE_MAX_CHILDREN = 10_000;

  private static final String ICON_TOGGLE_COLLAPSED = "<span class='fas fa-chevron-right'></span>";
  private static final String ICON_TOGGLE_EXPANDED = "<span class='fas fa-chevron-down'></span>";
  private static final String ICON_TOGGLE_LOADING = "<span class='fas fa-circle-notch fa-spin'></span>";

  private final IndexedAIP aip;
  private final int depth;
  private final SelectionListener listener;
  private final Filter baseFilter;
  private final boolean justActive;
  private final Set<String> disabledSubtreeIds;
  private final boolean disabled;

  private final FlowPanel rootPanel;
  private final FlowPanel rowPanel;
  private final FlowPanel childrenPanel;
  private final HTML toggleHtml;
  private final Label titleLabel;

  private boolean expanded = false;
  private boolean loaded = false;
  private boolean isLeaf = false;
  private Command pendingOnComplete = null;

  public SelectAipTreeNode(IndexedAIP aip, int depth, SelectionListener listener, Filter baseFilter,
    boolean justActive, Set<String> disabledSubtreeIds, boolean parentDisabled) {
    this.aip = aip;
    this.depth = depth;
    this.listener = listener;
    this.baseFilter = baseFilter;
    this.justActive = justActive;
    this.disabledSubtreeIds = disabledSubtreeIds;
    this.disabled = parentDisabled || (disabledSubtreeIds != null && disabledSubtreeIds.contains(aip.getId()));

    rootPanel = new FlowPanel();

    rowPanel = new FlowPanel();
    rowPanel.setStyleName("catalogTreeNode");
    if (disabled) {
      rowPanel.addStyleName("disabled");
    }

    for (int i = 0; i < depth; i++) {
      FlowPanel indent = new FlowPanel();
      indent.setStyleName("catalogTreeIndent");
      rowPanel.add(indent);
    }

    toggleHtml = new HTML(ICON_TOGGLE_COLLAPSED);
    toggleHtml.setStyleName("catalogTreeToggle");
    rowPanel.add(toggleHtml);

    HTML iconHtml = new HTML(DescriptionLevelUtils.getElementLevelIconSafeHtml(aip.getLevel(), false));
    iconHtml.setStyleName("catalogTreeIcon");
    rowPanel.add(iconHtml);

    titleLabel = new Label(aip.getTitle());
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
        if (!disabled && SelectAipTreeNode.this.listener != null) {
          SelectAipTreeNode.this.listener.onSelect(aip, SelectAipTreeNode.this);
        }
      }
    }, ClickEvent.getType());

    rootPanel.add(rowPanel);
    rootPanel.add(childrenPanel);

    initWidget(rootPanel);
  }

  public void toggle() {
    if (isLeaf) {
      return;
    }
    if (expanded) {
      collapse();
    } else {
      expand(null);
    }
  }

  public void expand(Command onComplete) {
    if (isLeaf) {
      if (onComplete != null) {
        onComplete.execute();
      }
      return;
    }
    if (loaded) {
      childrenPanel.setVisible(true);
      expanded = true;
      toggleHtml.setHTML(ICON_TOGGLE_EXPANDED);
      if (onComplete != null) {
        onComplete.execute();
      }
    } else {
      loadChildren(onComplete);
    }
  }

  public void collapse() {
    childrenPanel.setVisible(false);
    expanded = false;
    toggleHtml.setHTML(ICON_TOGGLE_COLLAPSED);
  }

  private void loadChildren(final Command onComplete) {
    this.pendingOnComplete = onComplete;
    toggleHtml.setHTML(ICON_TOGGLE_LOADING);

    Filter filter = SelectAipTreeDialog.buildTreeFilter(baseFilter,
      new SimpleFilterParameter(RodaConstants.AIP_PARENT_ID, aip.getId()));

    FindRequest findRequest = new FindRequest.FindRequestBuilder(filter, justActive)
      .withSorter(new Sorter(new SortParameter(RodaConstants.AIP_TITLE_SORT, false)))
      .withSublist(new Sublist(0, TREE_MAX_CHILDREN))
      .build();

    Services service = new Services(messages.catalogTreeReasonListChildren(), "get");
    service.rodaEntityRestService(s -> s.find(findRequest, LocaleInfo.getCurrentLocale().getLocaleName()),
      IndexedAIP.class).whenComplete((result, error) -> {
        if (error != null) {
          LOGGER.error("Failed to load children for AIP " + aip.getId(), error);
          toggleHtml.setHTML(ICON_TOGGLE_COLLAPSED);
          showLoadError();
          return;
        }
        List<IndexedAIP> children = result.getResults();
        if (children.isEmpty()) {
          markAsLeaf();
          if (onComplete != null) {
            onComplete.execute();
          }
          return;
        }
        loaded = true;
        for (IndexedAIP child : children) {
          SelectAipTreeNode childNode = new SelectAipTreeNode(child, depth + 1, listener, baseFilter, justActive,
            disabledSubtreeIds, disabled);
          childrenPanel.add(childNode);
        }
        childrenPanel.setVisible(true);
        expanded = true;
        toggleHtml.setHTML(ICON_TOGGLE_EXPANDED);
        if (onComplete != null) {
          onComplete.execute();
        }
      });
  }

  private void markAsLeaf() {
    isLeaf = true;
    loaded = true;
    toggleHtml.setHTML("");
  }

  private void showLoadError() {
    final FlowPanel errorPanel = new FlowPanel();
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

  @Override
  public void setSelected(boolean selected) {
    if (selected) {
      rowPanel.addStyleName("selected");
    } else {
      rowPanel.removeStyleName("selected");
    }
  }

  public IndexedAIP getAip() {
    return aip;
  }
}

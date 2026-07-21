/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.client.common.dialogs;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.v2.index.FindRequest;
import org.roda.core.data.v2.index.filter.EmptyKeyFilterParameter;
import org.roda.core.data.v2.index.filter.Filter;
import org.roda.core.data.v2.index.filter.FilterParameter;
import org.roda.core.data.v2.index.filter.NotSimpleFilterParameter;
import org.roda.core.data.v2.index.sort.SortParameter;
import org.roda.core.data.v2.index.sort.Sorter;
import org.roda.core.data.v2.index.sublist.Sublist;
import org.roda.core.data.v2.ip.IndexedAIP;
import org.roda.wui.client.services.Services;
import org.roda.wui.common.client.ClientLogger;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import config.i18n.client.ClientMessages;

/**
 * Tree-based parent-node selector. Replaces the flat list of {@link SelectAipDialog} with a filterable,
 * lazy-loading tree of logical units (levels {@code file}/{@code item} excluded), used when picking a
 * parent node during ingest, move and disposal-rule editing (#301).
 *
 * <p>
 * Built as a standalone dialog per ADR 0001 — the catalog tree is left untouched. Exposes the same value
 * contract the call sites already rely on: {@link #showAndCenter()}, {@link #addValueChangeHandler} (fires
 * with the selected {@link IndexedAIP}, or {@code null} for "no parent"), {@code addCloseHandler} (inherited
 * from {@link DialogBox}), {@link #setEmptyParentButtonVisible} and {@link #setSingleSelectionMode}.
 * </p>
 */
public class SelectAipTreeDialog extends DialogBox
  implements SelectDialog<IndexedAIP>, SelectAipTreeNode.SelectionListener {

  private static final Binder binder = GWT.create(Binder.class);

  interface Binder extends UiBinder<Widget, SelectAipTreeDialog> {
  }

  private static final ClientLogger LOGGER = new ClientLogger(SelectAipTreeDialog.class.getName());
  private static final ClientMessages messages = GWT.create(ClientMessages.class);
  /** Maximum number of root nodes to load. */
  private static final int TREE_MAX_CHILDREN = 10_000;

  @UiField
  TextBox filterInput;

  @UiField
  FlowPanel treeBody;

  @UiField
  Button cancelButton;

  @UiField
  Button emptyParentButton;

  @UiField
  Button selectButton;

  private final Filter baseFilter;
  private final boolean justActive;
  private final Map<String, SelectAipTreeNode> rootNodes = new HashMap<>();
  private Set<String> disabledSubtreeIds = new HashSet<>();

  private SelectAipTreeNode selectedNode = null;
  private IndexedAIP selectedAip = null;
  private boolean rootsLoaded = false;
  private int loadGeneration = 0;

  public SelectAipTreeDialog(String title) {
    this(title, null, true);
  }

  public SelectAipTreeDialog(String title, Filter filter, boolean justActive) {
    this(title, filter, justActive, true);
  }

  /**
   * @param exportCsvVisible
   *          accepted for signature parity with {@link SelectAipDialog}; the tree has no CSV export, so it
   *          is ignored.
   */
  public SelectAipTreeDialog(String title, Filter filter, boolean justActive, boolean exportCsvVisible) {
    this.baseFilter = filter;
    this.justActive = justActive;

    setWidget(binder.createAndBindUi(this));

    filterInput.getElement().setAttribute("placeholder", messages.catalogTreeFilterPlaceholder());
    filterInput.addKeyUpHandler(new KeyUpHandler() {
      @Override
      public void onKeyUp(KeyUpEvent event) {
        String query = filterInput.getText().trim().toLowerCase();
        for (SelectAipTreeNode node : rootNodes.values()) {
          node.applyFilter(query);
        }
        if (selectedNode != null && !selectedNode.isVisible()) {
          clearSelection();
        }
      }
    });

    setAutoHideEnabled(false);
    setModal(true);
    setGlassEnabled(true);
    setAnimationEnabled(false);
    setText(title);

    emptyParentButton.setVisible(false);
    selectButton.setEnabled(false);

    center();
  }

  @Override
  public void showAndCenter() {
    if (Window.getClientWidth() < 800) {
      this.setWidth(Window.getClientWidth() + "px");
    }
    if (!rootsLoaded) {
      loadRootNodes();
    }
    show();
    center();
  }

  private void loadRootNodes() {
    final int myGeneration = ++loadGeneration;
    rootsLoaded = true;
    treeBody.clear();
    rootNodes.clear();
    clearSelection();

    Filter filter = buildTreeFilter(baseFilter, new EmptyKeyFilterParameter(RodaConstants.AIP_PARENT_ID));
    FindRequest findRequest = new FindRequest.FindRequestBuilder(filter, justActive)
      .withSorter(new Sorter(new SortParameter(RodaConstants.AIP_TITLE_SORT, false)))
      .withSublist(new Sublist(0, TREE_MAX_CHILDREN))
      .build();

    Services service = new Services(messages.catalogTreeReasonListRoots(), "get");
    service.rodaEntityRestService(s -> s.find(findRequest, LocaleInfo.getCurrentLocale().getLocaleName()),
      IndexedAIP.class).whenComplete((result, error) -> {
        if (myGeneration != loadGeneration) {
          return;
        }
        if (error != null) {
          LOGGER.error("Failed to load parent-node selector root nodes", error);
          showRootLoadError();
          return;
        }
        List<IndexedAIP> roots = result.getResults();
        if (roots.isEmpty()) {
          Label emptyLabel = new Label(messages.selectAipTreeNoResults());
          emptyLabel.addStyleName("catalogTreeNodeError");
          treeBody.add(emptyLabel);
          return;
        }
        for (IndexedAIP aip : roots) {
          SelectAipTreeNode node = new SelectAipTreeNode(aip, 0, this, baseFilter, justActive, disabledSubtreeIds,
            false);
          rootNodes.put(aip.getId(), node);
          treeBody.add(node);
        }
      });
  }

  private void showRootLoadError() {
    treeBody.clear();
    FlowPanel errorPanel = new FlowPanel();
    errorPanel.addStyleName("catalogTreeNodeError");
    errorPanel.add(new Label(messages.catalogTreeLoadError()));
    Anchor retry = new Anchor(messages.catalogTreeRetry());
    retry.addClickHandler(event -> loadRootNodes());
    errorPanel.add(retry);
    treeBody.add(errorPanel);
  }

  @Override
  public void onSelect(SelectAipTreeNode node) {
    if (selectedNode != null) {
      selectedNode.deselect();
    }
    selectedNode = node;
    selectedAip = node.getAip();
    node.select();
    selectButton.setEnabled(true);
  }

  private void clearSelection() {
    if (selectedNode != null) {
      selectedNode.deselect();
    }
    selectedNode = null;
    selectedAip = null;
    selectButton.setEnabled(false);
  }

  @UiHandler("cancelButton")
  void buttonCancelHandler(ClickEvent e) {
    hide();
  }

  @UiHandler("selectButton")
  void buttonSelectHandler(ClickEvent e) {
    ValueChangeEvent.fire(this, selectedAip);
    hide();
  }

  @UiHandler("emptyParentButton")
  void buttonEmptyParentHandler(ClickEvent e) {
    ValueChangeEvent.fire(this, null);
    hide();
  }

  @Override
  public HandlerRegistration addValueChangeHandler(ValueChangeHandler<IndexedAIP> handler) {
    return addHandler(handler, ValueChangeEvent.getType());
  }

  public void setEmptyParentButtonVisible(boolean visible) {
    emptyParentButton.setVisible(visible);
  }

  /**
   * Single selection is the only supported mode; kept for signature parity with {@link SelectAipDialog}.
   */
  public void setSingleSelectionMode() {
    // no-op
  }

  /**
   * Marks the given AIPs and their whole subtrees as non-selectable (greyed out). Used by move so a node
   * cannot be moved into itself or one of its descendants. Must be called before {@link #showAndCenter()}.
   */
  public void setDisabledSubtreeIds(Collection<String> ids) {
    this.disabledSubtreeIds = ids != null ? new HashSet<>(ids) : new HashSet<>();
    if (rootsLoaded) {
      loadRootNodes();
    }
  }

  /**
   * Builds a tree query filter: the caller's base filter (if any), the given extra parameters, plus the
   * always-present exclusion of the {@code file} and {@code item} levels so only logical units are shown.
   */
  static Filter buildTreeFilter(Filter baseFilter, FilterParameter... extraParameters) {
    Filter filter = baseFilter != null ? new Filter(baseFilter) : new Filter();
    for (FilterParameter parameter : extraParameters) {
      filter.add(parameter);
    }
    filter.add(new NotSimpleFilterParameter(RodaConstants.AIP_LEVEL, "file"));
    filter.add(new NotSimpleFilterParameter(RodaConstants.AIP_LEVEL, "item"));
    return filter;
  }
}

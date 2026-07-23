/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.client.common.dialogs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.v2.index.FindRequest;
import org.roda.core.data.v2.index.filter.BasicSearchFilterParameter;
import org.roda.core.data.v2.index.filter.EmptyKeyFilterParameter;
import org.roda.core.data.v2.index.filter.Filter;
import org.roda.core.data.v2.index.filter.FilterParameter;
import org.roda.core.data.v2.index.filter.NotSimpleFilterParameter;
import org.roda.core.data.v2.index.filter.OneOfManyFilterParameter;
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
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import config.i18n.client.ClientMessages;

/**
 * Tree-based parent-node selector. Replaces the flat list of {@link SelectAipDialog} with a lazy-loading
 * tree of logical units (levels {@code file}/{@code item} excluded), used when picking a parent node during
 * ingest, move and disposal-rule editing (#301).
 *
 * <p>
 * The filter field switches the body from the browse tree to a flat, server-side search of matching logical
 * units, each shown with its ancestor breadcrumb (so nodes that share a title can be told apart). This works
 * around lazy-loading: a client-side filter could only match already-loaded nodes. Clearing the field
 * restores the tree.
 * </p>
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
  /** Maximum number of matches shown in search mode. */
  private static final int MAX_SEARCH_RESULTS = 200;
  /** Minimum query length before a search fires. */
  private static final int MIN_QUERY_LENGTH = 2;
  /** Debounce before a search fires, in milliseconds. */
  private static final int SEARCH_DEBOUNCE_MS = 300;
  /** Breadcrumb separator between ancestor titles. */
  private static final String BREADCRUMB_SEPARATOR = " › ";

  @UiField
  TextBox filterInput;

  @UiField
  FlowPanel treeBody;

  @UiField
  FlowPanel searchResults;

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

  private Selectable selectedWidget = null;
  private IndexedAIP selectedAip = null;
  private boolean rootsLoaded = false;
  private int loadGeneration = 0;
  private int searchGeneration = 0;

  private final Timer searchTimer = new Timer() {
    @Override
    public void run() {
      runSearch(filterInput.getText().trim());
    }
  };

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
        onFilterChanged();
      }
    });

    searchResults.setVisible(false);

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

  private void onFilterChanged() {
    String query = filterInput.getText().trim();
    if (query.length() < MIN_QUERY_LENGTH) {
      searchTimer.cancel();
      // Bump the generation so a slower in-flight search cannot repaint the results after we return.
      searchGeneration++;
      showBrowseTree();
    } else {
      searchTimer.cancel();
      searchTimer.schedule(SEARCH_DEBOUNCE_MS);
    }
  }

  private void showBrowseTree() {
    searchResults.clear();
    searchResults.setVisible(false);
    treeBody.setVisible(true);
    // Reset the selection: a search-result row may have been selected and is now gone, so a stale
    // (invisible) selection must not stay armed on the select button.
    clearSelection();
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

  /**
   * Runs a server-side search for logical units matching the query and shows the matches as a flat list,
   * each annotated with its ancestor breadcrumb. Replaces the browse tree until the query is cleared.
   */
  private void runSearch(String query) {
    final int myGeneration = ++searchGeneration;
    // Switching to search mode hides the browse tree, so a tree-node selection would become invisible;
    // drop it so it cannot be submitted from behind the search results.
    clearSelection();
    treeBody.setVisible(false);
    searchResults.setVisible(true);
    searchResults.clear();
    searchResults.add(infoLabel(messages.catalogTreeLoadingLabel()));

    Filter filter = buildTreeFilter(baseFilter,
      new BasicSearchFilterParameter(RodaConstants.INDEX_SEARCH, toSubstringQuery(query)));
    FindRequest findRequest = new FindRequest.FindRequestBuilder(filter, justActive)
      .withSorter(new Sorter(new SortParameter(RodaConstants.AIP_TITLE_SORT, false)))
      .withSublist(new Sublist(0, MAX_SEARCH_RESULTS))
      .build();

    Services service = new Services(messages.catalogTreeReasonListRoots(), "get");
    service.rodaEntityRestService(s -> s.find(findRequest, LocaleInfo.getCurrentLocale().getLocaleName()),
      IndexedAIP.class).whenComplete((result, error) -> {
        if (myGeneration != searchGeneration) {
          return;
        }
        searchResults.clear();
        if (error != null) {
          LOGGER.error("Parent-node selector search failed", error);
          searchResults.add(infoLabel(messages.catalogTreeLoadError()));
          return;
        }
        List<IndexedAIP> matches = result.getResults();
        if (matches.isEmpty()) {
          searchResults.add(infoLabel(messages.selectAipTreeNoResults()));
          return;
        }
        resolveAncestorsAndRender(matches, myGeneration);
      });
  }

  /**
   * Resolves every ancestor id referenced by the matches to a title (one batch call for the unknown ones),
   * then renders the result rows. Inaccessible ancestors fall back to the ghost-node label.
   */
  private void resolveAncestorsAndRender(List<IndexedAIP> matches, int expectedGeneration) {
    Map<String, String> idToTitle = new HashMap<>();
    for (IndexedAIP match : matches) {
      idToTitle.put(match.getId(), match.getTitle());
    }
    Set<String> unknownAncestorIds = new LinkedHashSet<>();
    for (IndexedAIP match : matches) {
      if (match.getAncestors() != null) {
        for (String ancestorId : match.getAncestors()) {
          if (!idToTitle.containsKey(ancestorId)) {
            unknownAncestorIds.add(ancestorId);
          }
        }
      }
    }

    if (unknownAncestorIds.isEmpty()) {
      renderSearchResults(matches, idToTitle);
      return;
    }

    FindRequest ancestorRequest = new FindRequest.FindRequestBuilder(
      new Filter(new OneOfManyFilterParameter(RodaConstants.INDEX_UUID, new ArrayList<>(unknownAncestorIds))), false)
        .withSublist(new Sublist(0, unknownAncestorIds.size())).build();

    Services service = new Services(messages.catalogTreeReasonGetAncestors(), "get");
    service.rodaEntityRestService(s -> s.find(ancestorRequest, LocaleInfo.getCurrentLocale().getLocaleName()),
      IndexedAIP.class).whenComplete((ancestorResult, error) -> {
        if (expectedGeneration != searchGeneration) {
          return;
        }
        if (error == null) {
          for (IndexedAIP ancestor : ancestorResult.getResults()) {
            idToTitle.put(ancestor.getId(), ancestor.getTitle());
          }
        } else {
          LOGGER.warn("Could not resolve ancestor titles for search breadcrumbs; using placeholder");
        }
        renderSearchResults(matches, idToTitle);
      });
  }

  private void renderSearchResults(List<IndexedAIP> matches, Map<String, String> idToTitle) {
    searchResults.clear();
    for (IndexedAIP match : matches) {
      boolean disabled = isInDisabledSubtree(match);
      searchResults.add(new SearchResultRow(match, buildBreadcrumb(match, idToTitle), disabled));
    }
  }

  private boolean isInDisabledSubtree(IndexedAIP aip) {
    if (disabledSubtreeIds.contains(aip.getId())) {
      return true;
    }
    if (aip.getAncestors() != null) {
      for (String ancestorId : aip.getAncestors()) {
        if (disabledSubtreeIds.contains(ancestorId)) {
          return true;
        }
      }
    }
    return false;
  }

  /** Builds a top-to-bottom breadcrumb of ancestor titles; {@code ancestors} from Solr is bottom-to-top. */
  private String buildBreadcrumb(IndexedAIP aip, Map<String, String> idToTitle) {
    List<String> ancestors = aip.getAncestors();
    if (ancestors == null || ancestors.isEmpty()) {
      return "";
    }
    List<String> topToBottom = new ArrayList<>(ancestors);
    Collections.reverse(topToBottom);
    StringBuilder breadcrumb = new StringBuilder();
    for (String ancestorId : topToBottom) {
      String title = idToTitle.get(ancestorId);
      if (title == null || title.isEmpty()) {
        title = messages.catalogTreeGhostNodeLabel();
      }
      if (breadcrumb.length() > 0) {
        breadcrumb.append(BREADCRUMB_SEPARATOR);
      }
      breadcrumb.append(title);
    }
    return breadcrumb.toString();
  }

  /**
   * Widens matching by wrapping each whitespace-separated token in wildcards ({@code *token*}) so a partial
   * word matches (e.g. "userie 1" finds "userie 1.1", where the plain token "1" would not match "1.1").
   * Tokens the user already made into wildcards, and boolean operators, are left untouched. Solr's special
   * characters are still escaped server-side; {@code *}/{@code ?} are not, so they pass through.
   */
  private static String toSubstringQuery(String query) {
    StringBuilder sb = new StringBuilder();
    for (String token : query.trim().split("\\s+")) {
      if (token.isEmpty()) {
        continue;
      }
      if (sb.length() > 0) {
        sb.append(' ');
      }
      if (token.contains("*") || token.contains("?") || "AND".equals(token) || "OR".equals(token)
        || "NOT".equals(token)) {
        sb.append(token);
      } else {
        sb.append('*').append(token).append('*');
      }
    }
    return sb.toString();
  }

  private Label infoLabel(String text) {
    Label label = new Label(text);
    label.addStyleName("catalogTreeNodeError");
    return label;
  }

  @Override
  public void onSelect(IndexedAIP aip, Selectable widget) {
    if (selectedWidget != null) {
      selectedWidget.setSelected(false);
    }
    selectedWidget = widget;
    selectedAip = aip;
    widget.setSelected(true);
    selectButton.setEnabled(true);
  }

  private void clearSelection() {
    if (selectedWidget != null) {
      selectedWidget.setSelected(false);
    }
    selectedWidget = null;
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
   * Builds a query filter: the caller's base filter (if any), the given extra parameters, plus the
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

  /** A flat search-result row: level icon, title, and an ancestor breadcrumb. */
  private class SearchResultRow extends Composite implements Selectable {
    private final FlowPanel row;

    SearchResultRow(final IndexedAIP aip, String breadcrumb, boolean disabled) {
      row = new FlowPanel();
      row.setStyleName("selectAipTreeSearchResult");
      if (disabled) {
        row.addStyleName("disabled");
      }

      HTML icon = new HTML(DescriptionLevelUtils.getElementLevelIconSafeHtml(aip.getLevel(), false));
      icon.setStyleName("catalogTreeIcon");
      row.add(icon);

      FlowPanel text = new FlowPanel();
      text.setStyleName("selectAipTreeSearchResultText");
      Label title = new Label(aip.getTitle() != null ? aip.getTitle() : aip.getId());
      title.setStyleName("catalogTreeLabel");
      text.add(title);
      if (!breadcrumb.isEmpty()) {
        Label crumb = new Label(breadcrumb);
        crumb.setStyleName("selectAipTreeBreadcrumb");
        text.add(crumb);
      }
      row.add(text);

      if (!disabled) {
        row.addDomHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            onSelect(aip, SearchResultRow.this);
          }
        }, ClickEvent.getType());
      }

      initWidget(row);
    }

    @Override
    public void setSelected(boolean selected) {
      if (selected) {
        row.addStyleName("selected");
      } else {
        row.removeStyleName("selected");
      }
    }
  }
}

/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.client.management;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.MissingResourceException;
import java.util.function.Supplier;

import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.v2.accessrules.AccessRule;
import org.roda.core.data.v2.index.filter.Filter;
import org.roda.core.data.v2.index.filter.OneOfManyFilterParameter;
import org.roda.core.data.v2.ip.IndexedAIP;
import org.roda.core.data.v2.user.GroupOverview;
import org.roda.core.data.v2.user.RoleArea;
import org.roda.core.data.v2.user.UserOverview;
import org.roda.wui.client.browse.tabs.Tabs;
import org.roda.wui.client.common.TitlePanel;
import org.roda.wui.client.common.UserLogin;
import org.roda.wui.client.common.lists.utils.AsyncTableCellOptions;
import org.roda.wui.client.common.lists.utils.BasicTablePanel;
import org.roda.wui.client.common.lists.utils.ConfigurableAsyncTableCell;
import org.roda.wui.client.common.lists.utils.ListBuilder;
import org.roda.wui.client.common.search.SearchWrapper;
import org.roda.wui.client.common.utils.AsyncCallbackUtils;
import org.roda.wui.client.common.utils.HtmlSnippetUtils;
import org.roda.wui.client.services.Services;
import org.roda.wui.common.client.HistoryResolver;
import org.roda.wui.common.client.tools.ConfigurationManager;
import org.roda.wui.common.client.tools.HistoryUtils;
import org.roda.wui.common.client.tools.ListUtils;
import org.roda.wui.common.client.tools.StringUtils;
import org.roda.wui.common.client.widgets.HTMLWidgetWrapper;

import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.URL;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import config.i18n.client.ClientMessages;

/**
 * Behörighetsöversikt under Administration. Visar
 * rättigheter per grupp, användare, strukturenheternas behörigheter och
 * aktiva åtkomstregler. Underlaget hämtas från
 * {@code GET /api/v2/permission-overview}; rapporterna (HTML/CSV) öppnas som
 * vanliga länkar mot {@code /report/html} och {@code /report/csv}. Sidan är
 * enbart en läsvy.
 */
public class PermissionOverview extends Composite {
  private static final ClientMessages messages = GWT.create(ClientMessages.class);

  private static final String LIST_ID_STRUCTURE_NODES = "PermissionOverview_structureNodes";
  private static final String REPORT_BASE_PATH = "api/v2/permission-overview/report/";
  private static final String REPORT_FORMAT_HTML = "html";
  private static final String REPORT_FORMAT_CSV = "csv";

  /** Bredd (em) för gruppkolumnerna i matrisen, så att rubrikerna blir kompakta. */
  private static final double GROUP_COLUMN_WIDTH_EM = 5;

  private static final String CELL_STYLE_NOWRAP = "nowrap";
  private static final String CELL_STYLE_MATRIX = "permissionOverviewMatrixCell";
  private static final String CELL_STYLE_ADMIN_COLUMN = "permissionOverviewAdminColumn";
  private static final String STYLE_MATRIX = "permissionOverviewMatrix";
  private static final String STYLE_LOADING = "permissionOverviewLoading";
  private static final String OPEN_SPAN_LABEL_SUCCESS = "<span class='label-success'>";
  private static final String OPEN_SPAN_LABEL_DEFAULT = "<span class='label-default'>";
  private static final String OPEN_SPAN_SR_ONLY = "<span class='permissionOverviewSrOnly'>";
  private static final String CLOSE_SPAN = "</span>";
  private static final String CHECK_ICON = "<i class=\"fa fa-check permissionOverviewCheck\" aria-hidden=\"true\"></i>";

  public static final HistoryResolver RESOLVER = new HistoryResolver() {

    @Override
    public void resolve(List<String> historyTokens, AsyncCallback<Widget> callback) {
      getInstance().resolve(historyTokens, callback);
    }

    @Override
    public void isCurrentUserPermitted(AsyncCallback<Boolean> callback) {
      UserLogin.getInstance().checkRole(this, callback);
    }

    @Override
    public List<String> getHistoryPath() {
      return ListUtils.concat(Management.RESOLVER.getHistoryPath(), getHistoryToken());
    }

    @Override
    public String getHistoryToken() {
      return "permission_overview";
    }
  };

  private static PermissionOverview instance = null;

  /**
   * Get the singleton instance
   *
   * @return the instance
   */
  public static PermissionOverview getInstance() {
    if (instance == null) {
      instance = new PermissionOverview();
    } else {
      instance.refresh();
    }
    return instance;
  }

  interface MyUiBinder extends UiBinder<Widget, PermissionOverview> {
  }

  private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

  @UiField
  TitlePanel title;
  @UiField
  FlowPanel permissionOverviewDescription;
  @UiField
  FlowPanel warningsPanel;
  @UiField
  Button buttonHtml;
  @UiField
  Button buttonCsv;
  @UiField
  Label generatedLabel;
  @UiField
  FlowPanel tabsContainer;

  private org.roda.core.data.v2.user.PermissionOverview overview = null;

  /** Vald flik (index i flikraden), så att samma flik visas igen efter refresh(). */
  private int selectedTabIndex = 0;

  /** Senaste filtertext på fliken Användare, så att den överlever refresh(). */
  private String usersFilterText = "";

  public PermissionOverview() {
    initWidget(uiBinder.createAndBindUi(this));

    title.setIcon("fa fa-shield-alt");

    permissionOverviewDescription.add(new HTMLWidgetWrapper("PermissionOverviewDescription.html"));
    permissionOverviewDescription.setVisible(false);
    Anchor infoIconTitle = new Anchor();
    infoIconTitle.getElement().setInnerHTML("<i class=\"fa fa-info-circle\"></i>");
    infoIconTitle.addStyleName("description-toggle-icon");
    infoIconTitle.setTitle(messages.descriptionToggleTitle());
    infoIconTitle
      .addClickHandler(event -> permissionOverviewDescription.setVisible(!permissionOverviewDescription.isVisible()));
    title.add(infoIconTitle);

    warningsPanel.setVisible(false);
    generatedLabel.setVisible(false);

    buttonHtml.addClickHandler(event -> openReport(REPORT_FORMAT_HTML));
    buttonCsv.addClickHandler(event -> openReport(REPORT_FORMAT_CSV));

    load();
  }

  private void refresh() {
    load();
  }

  public void resolve(List<String> historyTokens, AsyncCallback<Widget> callback) {
    if (historyTokens.isEmpty()) {
      callback.onSuccess(this);
    } else {
      HistoryUtils.newHistory(RESOLVER);
      callback.onSuccess(null);
    }
  }

  private static String getLocaleName() {
    return LocaleInfo.getCurrentLocale().getLocaleName();
  }

  private void openReport(String format) {
    String url = GWT.getHostPageBaseURL() + REPORT_BASE_PATH + format + "?" + RodaConstants.API_QUERY_KEY_LANG + "="
      + URL.encodeQueryString(getLocaleName());
    if (REPORT_FORMAT_CSV.equals(format)) {
      // CSV levereras som Content-Disposition: attachment; samma mönster som
      // övriga nedladdningar (t.ex. AipActions)
      Window.Location.assign(url);
    } else {
      Window.open(url, "_blank", "");
    }
  }

  private void load() {
    showLoading();
    String locale = getLocaleName();
    Services services = new Services("Retrieve permission overview", "get");
    services.permissionOverviewResource(s -> s.retrieveOverview(locale)).whenComplete((result, error) -> {
      if (error != null) {
        showLoadError();
        AsyncCallbackUtils.defaultFailureTreatment(error);
      } else if (result != null) {
        overview = result;
        render();
      } else {
        showLoadError();
      }
    });
  }

  /**
   * Visas medan underlaget hämtas (servern går igenom alla användare, grupper
   * och strukturenheter) och ersätter vid refresh() det gamla innehållet.
   */
  private void showLoading() {
    tabsContainer.clear();
    FlowPanel loading = new FlowPanel();
    loading.addStyleName(STYLE_LOADING);
    loading.add(new HTML(HtmlSnippetUtils.LOADING));
    tabsContainer.add(loading);
  }

  private void showLoadError() {
    tabsContainer.clear();
    tabsContainer.add(createEmptyLabel(messages.permissionOverviewLoadError()));
    Button retry = new Button(messages.permissionOverviewRetry());
    retry.addStyleName("btn btn-default btn-refresh");
    retry.addClickHandler(event -> load());
    tabsContainer.add(retry);
  }

  private void render() {
    renderWarnings(overview.getWarnings());
    renderGeneratedInfo();

    // createAndAddTab väljer första fliken och nollställer därmed indexet
    int tabToSelect = selectedTabIndex;

    tabsContainer.clear();
    RestorableTabs tabs = new RestorableTabs();
    tabs.createAndAddTab(SafeHtmlUtils.fromString(messages.permissionOverviewTabRoles()),
      cached(this::createRolesTab));
    tabs.createAndAddTab(SafeHtmlUtils.fromString(messages.permissionOverviewTabUsers()),
      cached(this::createUsersTab));
    tabs.createAndAddTab(SafeHtmlUtils.fromString(messages.permissionOverviewTabNodes()),
      cached(this::createNodesTab));
    tabs.createAndAddTab(SafeHtmlUtils.fromString(messages.permissionOverviewTabRules()),
      cached(this::createRulesTab));
    tabs.selectTabAt(tabToSelect);
    tabsContainer.add(tabs);
  }

  /**
   * {@link Tabs} bygger flikinnehållet om varje gång fliken väljs; cacha
   * widgeten så att listor och tabeller inte skapas på nytt vid varje klick.
   */
  private static Tabs.TabContentBuilder cached(Supplier<Widget> supplier) {
    return new Tabs.TabContentBuilder() {
      private Widget widget = null;

      @Override
      public Widget buildTabWidget() {
        if (widget == null) {
          widget = supplier.get();
        }
        return widget;
      }
    };
  }

  /**
   * {@link Tabs} som kommer ihåg vald flik (index i flikraden) så att samma
   * flik kan väljas igen när sidan byggs om efter refresh(), t.ex. när
   * användaren går tillbaka från en användar- eller gruppsida.
   */
  private class RestorableTabs extends Tabs {
    private FlowPanel tabButtonRow = null;

    @Override
    protected void selectTab(Widget tabButtonContainer) {
      super.selectTab(tabButtonContainer);
      // anropas även från Tabs-konstruktorn med null, innan fälten finns
      if (tabButtonContainer != null && tabButtonContainer.getParent() instanceof FlowPanel) {
        tabButtonRow = (FlowPanel) tabButtonContainer.getParent();
        selectedTabIndex = tabButtonRow.getWidgetIndex(tabButtonContainer);
      }
    }

    private void selectTabAt(int index) {
      if (tabButtonRow != null && index >= 0 && index < tabButtonRow.getWidgetCount()) {
        selectTab(tabButtonRow.getWidget(index));
      }
    }
  }

  private void renderWarnings(List<String> warnings) {
    warningsPanel.clear();
    if (warnings == null || warnings.isEmpty()) {
      warningsPanel.setVisible(false);
      return;
    }

    SafeHtmlBuilder b = new SafeHtmlBuilder();
    b.appendHtmlConstant("<p><i class=\"fa fa-exclamation-triangle\" aria-hidden=\"true\"></i> <strong>");
    b.appendEscaped(messages.permissionOverviewWarnings());
    b.appendHtmlConstant("</strong></p><ul>");
    for (String warning : warnings) {
      b.appendHtmlConstant("<li>");
      b.appendEscaped(warning != null ? warning : "");
      b.appendHtmlConstant("</li>");
    }
    b.appendHtmlConstant("</ul>");
    warningsPanel.add(new HTML(b.toSafeHtml()));
    warningsPanel.setVisible(true);
  }

  private void renderGeneratedInfo() {
    String generatedAt = overview.getGeneratedAt();
    if (generatedAt == null || generatedAt.isEmpty()) {
      generatedLabel.setVisible(false);
    } else {
      String generatedBy = overview.getGeneratedBy() != null ? overview.getGeneratedBy() : "";
      generatedLabel.setText(messages.permissionOverviewGeneratedAt(generatedAt, generatedBy));
      generatedLabel.setVisible(true);
    }
  }

  // ------------------------------------------------------------------ roles

  private Widget createRolesTab() {
    List<RoleRow> rows = new ArrayList<>();
    for (RoleArea area : safe(overview.getRoleAreas())) {
      String areaTitle = roleTitle(area.getKey());
      for (String role : safe(area.getRoles())) {
        rows.add(new RoleRow(areaTitle, role, roleDescription(role)));
      }
    }

    if (rows.isEmpty()) {
      return createEmptyLabel(messages.permissionOverviewNoRoles());
    }

    final SafeHtml granted = createMatrixCell(true);
    final SafeHtml notGranted = createMatrixCell(false);

    List<BasicTablePanel.ColumnInfo<RoleRow>> columns = new ArrayList<>();
    // Första kolumnen (rättighet + beskrivning) ligger fast vid horisontell
    // scroll, se .permissionOverviewMatrix i main.gss
    columns.add(new BasicTablePanel.ColumnInfo<RoleRow>(messages.permissionOverviewColRole(), 0,
      new Column<RoleRow, SafeHtml>(new SafeHtmlCell()) {
        @Override
        public SafeHtml getValue(RoleRow row) {
          return createRoleCell(row);
        }
      }));
    columns.add(new BasicTablePanel.ColumnInfo<RoleRow>(messages.permissionOverviewColArea(), 0,
      new TextColumn<RoleRow>() {
        @Override
        public String getValue(RoleRow row) {
          return row.area;
        }
      }, CELL_STYLE_NOWRAP));

    for (GroupOverview group : safe(overview.getGroups())) {
      final List<String> allRoles = safe(group.getAllRoles());
      Column<RoleRow, SafeHtml> column = new Column<RoleRow, SafeHtml>(new SafeHtmlCell()) {
        @Override
        public SafeHtml getValue(RoleRow row) {
          return allRoles.contains(row.role) ? granted : notGranted;
        }
      };
      if (RodaConstants.ADMINISTRATORS.equals(group.getName())) {
        columns.add(new BasicTablePanel.ColumnInfo<RoleRow>(createGroupHeader(group), false, GROUP_COLUMN_WIDTH_EM,
          column, CELL_STYLE_MATRIX, CELL_STYLE_ADMIN_COLUMN));
      } else {
        columns.add(new BasicTablePanel.ColumnInfo<RoleRow>(createGroupHeader(group), false, GROUP_COLUMN_WIDTH_EM,
          column, CELL_STYLE_MATRIX));
      }
    }

    Widget table = wrapTable(new BasicTablePanel<RoleRow>(rows.iterator(), toArray(columns)));
    table.addStyleName(STYLE_MATRIX);
    return table;
  }

  /**
   * Bock för tilldelad rättighet, annars tom cell. Båda får dold text (Ja/Nej)
   * så att skärmläsare kan skilja cellerna åt (WCAG 1.1.1/1.3.1).
   */
  private static SafeHtml createMatrixCell(boolean granted) {
    SafeHtmlBuilder b = new SafeHtmlBuilder();
    if (granted) {
      b.appendHtmlConstant(CHECK_ICON);
    }
    b.appendHtmlConstant(OPEN_SPAN_SR_ONLY);
    b.appendEscaped(granted ? messages.permissionOverviewYes() : messages.permissionOverviewNo());
    b.appendHtmlConstant(CLOSE_SPAN);
    return b.toSafeHtml();
  }

  /** Rättighetens namn med beskrivningen som en andra rad under namnet. */
  private static SafeHtml createRoleCell(RoleRow row) {
    SafeHtmlBuilder b = new SafeHtmlBuilder();
    b.appendHtmlConstant("<div class=\"permissionOverviewRoleCell\"><span class=\"permissionOverviewRoleName\">");
    b.appendEscaped(row.role);
    b.appendHtmlConstant("</span>");
    if (!row.description.isEmpty() && !row.description.equals(row.role)) {
      b.appendHtmlConstant("<span class=\"permissionOverviewRoleDescription\">");
      b.appendEscaped(row.description);
      b.appendHtmlConstant("</span>");
    }
    b.appendHtmlConstant("</div>");
    return b.toSafeHtml();
  }

  private static SafeHtml createGroupHeader(GroupOverview group) {
    String name = group.getName() != null ? group.getName() : "";
    String fullName = group.getFullName() != null && !group.getFullName().isEmpty() ? group.getFullName() : name;
    String href = HistoryUtils.createHistoryHashLink(ShowGroup.RESOLVER, name);

    StringBuilder b = new StringBuilder();
    b.append("<a href=\"").append(SafeHtmlUtils.htmlEscape(href)).append("\" title=\"")
      .append(SafeHtmlUtils.htmlEscape(fullName)).append("\">").append(SafeHtmlUtils.htmlEscape(name)).append("</a>");
    if (RodaConstants.ADMINISTRATORS.equals(name)) {
      // role/aria-label gör att texten även läses upp av skärmläsare; title
      // behålls för mus
      String adminTitle = SafeHtmlUtils.htmlEscape(messages.permissionOverviewAdminGroup());
      b.append(" <i class=\"fa fa-shield-alt\" role=\"img\" aria-label=\"").append(adminTitle).append("\" title=\"")
        .append(adminTitle).append("\"></i>");
    }
    return SafeHtmlUtils.fromTrustedString(b.toString());
  }

  private static String roleTitle(String key) {
    if (key == null) {
      return "";
    }
    try {
      return messages.roleTitle(key);
    } catch (MissingResourceException e) {
      return key;
    }
  }

  private static String roleDescription(String role) {
    if (role == null) {
      return "";
    }
    try {
      return messages.role(role);
    } catch (MissingResourceException e) {
      return role;
    }
  }

  // ------------------------------------------------------------------ users

  private Widget createUsersTab() {
    FlowPanel panel = new FlowPanel();

    TextBox filterBox = new TextBox();
    filterBox.addStyleName("form-textbox");
    filterBox.addStyleName("permissionOverviewFilter");
    filterBox.setTitle(messages.permissionOverviewFilterPlaceholder());
    filterBox.getElement().setAttribute("placeholder", messages.permissionOverviewFilterPlaceholder());
    filterBox.setValue(usersFilterText);

    SimplePanel tableHolder = new SimplePanel();
    renderUsersTable(tableHolder, usersFilterText);
    // keyup täcker skrivning; ValueChange (change/blur) täcker inklistring via
    // meny, autofyll och IME-inmatning som inte ger keyup
    filterBox.addKeyUpHandler(event -> applyUsersFilter(tableHolder, filterBox.getValue()));
    filterBox.addValueChangeHandler(event -> applyUsersFilter(tableHolder, event.getValue()));

    panel.add(filterBox);
    panel.add(tableHolder);
    return panel;
  }

  /**
   * Bygger bara om tabellen när filtret faktiskt ändrats (inte vid Shift,
   * piltangenter m.m.).
   */
  private void applyUsersFilter(SimplePanel holder, String value) {
    String text = value != null ? value : "";
    boolean changed = !normalizeFilter(text).equals(normalizeFilter(usersFilterText));
    usersFilterText = text;
    if (changed) {
      renderUsersTable(holder, text);
    }
  }

  private static String normalizeFilter(String filter) {
    return filter != null ? filter.trim().toLowerCase() : "";
  }

  private void renderUsersTable(SimplePanel holder, String filter) {
    String needle = normalizeFilter(filter);
    List<UserOverview> rows = new ArrayList<>();
    for (UserOverview user : safe(overview.getUsers())) {
      if (matchesFilter(user, needle)) {
        rows.add(user);
      }
    }

    if (rows.isEmpty()) {
      holder.setWidget(createEmptyLabel(messages.permissionOverviewNoUsers()));
      return;
    }

    List<BasicTablePanel.ColumnInfo<UserOverview>> columns = new ArrayList<>();
    columns.add(new BasicTablePanel.ColumnInfo<UserOverview>(messages.permissionOverviewColUser(), 0,
      new Column<UserOverview, SafeHtml>(new SafeHtmlCell()) {
        @Override
        public SafeHtml getValue(UserOverview user) {
          return createUserLink(user);
        }
      }, CELL_STYLE_NOWRAP));
    columns.add(new BasicTablePanel.ColumnInfo<UserOverview>(messages.permissionOverviewColName(), 0,
      new TextColumn<UserOverview>() {
        @Override
        public String getValue(UserOverview user) {
          return user.getFullName() != null ? user.getFullName() : "";
        }
      }));
    columns.add(new BasicTablePanel.ColumnInfo<UserOverview>(messages.permissionOverviewColEmail(), 0,
      new TextColumn<UserOverview>() {
        @Override
        public String getValue(UserOverview user) {
          return user.getEmail() != null ? user.getEmail() : "";
        }
      }));
    columns.add(new BasicTablePanel.ColumnInfo<UserOverview>(messages.permissionOverviewColActive(), 0,
      new Column<UserOverview, SafeHtml>(new SafeHtmlCell()) {
        @Override
        public SafeHtml getValue(UserOverview user) {
          return createActiveHtml(user.isActive(), messages.showUserActivated(), messages.showUserDeactivated());
        }
      }, CELL_STYLE_NOWRAP));
    columns.add(new BasicTablePanel.ColumnInfo<UserOverview>(messages.permissionOverviewColGroups(), 0,
      new TextColumn<UserOverview>() {
        @Override
        public String getValue(UserOverview user) {
          return StringUtils.prettyPrint(safe(user.getGroups()));
        }
      }));
    columns.add(new BasicTablePanel.ColumnInfo<UserOverview>(messages.permissionOverviewColDirectRoles(), 0,
      new Column<UserOverview, SafeHtml>(new SafeHtmlCell()) {
        @Override
        public SafeHtml getValue(UserOverview user) {
          return createRoleList(user.getDirectRoles());
        }
      }));
    columns.add(new BasicTablePanel.ColumnInfo<UserOverview>(messages.permissionOverviewColAllRoles(), 0,
      new Column<UserOverview, SafeHtml>(new SafeHtmlCell()) {
        @Override
        public SafeHtml getValue(UserOverview user) {
          return createRoleList(user.getAllRoles());
        }
      }));

    holder.setWidget(wrapTable(new BasicTablePanel<UserOverview>(rows.iterator(), toArray(columns))));
  }

  private static boolean matchesFilter(UserOverview user, String needle) {
    if (needle.isEmpty()) {
      return true;
    }
    if (containsIgnoreCase(user.getName(), needle) || containsIgnoreCase(user.getFullName(), needle)
      || containsIgnoreCase(user.getEmail(), needle)) {
      return true;
    }
    for (String group : safe(user.getGroups())) {
      if (containsIgnoreCase(group, needle)) {
        return true;
      }
    }
    return false;
  }

  private static boolean containsIgnoreCase(String value, String needle) {
    return value != null && value.toLowerCase().contains(needle);
  }

  private static SafeHtml createUserLink(UserOverview user) {
    String name = user.getName() != null ? user.getName() : "";
    String href = HistoryUtils.createHistoryHashLink(ShowUser.RESOLVER, name);
    StringBuilder b = new StringBuilder();
    b.append("<a href=\"").append(SafeHtmlUtils.htmlEscape(href)).append("\">")
      .append(SafeHtmlUtils.htmlEscape(name)).append("</a>");
    return SafeHtmlUtils.fromTrustedString(b.toString());
  }

  /**
   * Antalet rättigheter som en utfällbar kontroll (details/summary) med hela
   * listan som text. Nås med tangentbord och pekskärm, till skillnad från en
   * title-tooltip, och texten går att söka i och markera.
   */
  private static SafeHtml createRoleList(List<String> roles) {
    List<String> safeRoles = safe(roles);
    if (safeRoles.isEmpty()) {
      return SafeHtmlUtils.fromString("0");
    }
    SafeHtmlBuilder b = new SafeHtmlBuilder();
    b.appendHtmlConstant(
      "<details class=\"permissionOverviewRoleList\"><summary class=\"permissionOverviewRoleListToggle\">");
    b.append(safeRoles.size());
    b.appendHtmlConstant("</summary><div class=\"permissionOverviewRoleListItems\">");
    b.appendEscaped(StringUtils.prettyPrint(safeRoles));
    b.appendHtmlConstant("</div></details>");
    return b.toSafeHtml();
  }

  /**
   * Samma utseende som status på användarsidan
   * (HtmlSnippetUtils.getUserStateHtml).
   */
  private static SafeHtml createActiveHtml(boolean active, String activeText, String inactiveText) {
    SafeHtmlBuilder b = new SafeHtmlBuilder();
    if (active) {
      b.append(SafeHtmlUtils.fromSafeConstant(OPEN_SPAN_LABEL_SUCCESS));
      b.append(SafeHtmlUtils.fromString(activeText));
    } else {
      b.append(SafeHtmlUtils.fromSafeConstant(OPEN_SPAN_LABEL_DEFAULT));
      b.append(SafeHtmlUtils.fromString(inactiveText));
    }
    b.append(SafeHtmlUtils.fromSafeConstant(CLOSE_SPAN));
    return b.toSafeHtml();
  }

  // ------------------------------------------------------------------ nodes

  private Widget createNodesTab() {
    Filter filter = new Filter(new OneOfManyFilterParameter(RodaConstants.AIP_LEVEL, getConfiguredLevels()));
    ListBuilder<IndexedAIP> listBuilder = new ListBuilder<>(() -> new ConfigurableAsyncTableCell<>(),
      new AsyncTableCellOptions<>(IndexedAIP.class, LIST_ID_STRUCTURE_NODES).withFilter(filter)
        .withSummary(messages.permissionOverviewTabNodes()).withCsvDownloadButtonVisibility(true).bindOpener());
    return new SearchWrapper(false).createListAndSearchPanel(listBuilder);
  }

  /**
   * Nivåerna från ui.permissionOverview.levels (delad konfiguration). Värdet
   * får vara kommaseparerat eller upprepat per rad, precis som på serversidan
   * (PermissionOverviewBuilder); saknas nyckeln används samma standardnivåer
   * som servern, {@link RodaConstants#PERMISSION_OVERVIEW_DEFAULT_LEVELS}.
   */
  private static List<String> getConfiguredLevels() {
    List<String> levels = new ArrayList<>();
    for (String entry : ConfigurationManager.getStringList(RodaConstants.UI_PERMISSION_OVERVIEW_LEVELS)) {
      if (entry == null) {
        continue;
      }
      for (String part : entry.split(",")) {
        String level = part.trim();
        if (!level.isEmpty() && !levels.contains(level)) {
          levels.add(level);
        }
      }
    }
    return levels.isEmpty() ? RodaConstants.PERMISSION_OVERVIEW_DEFAULT_LEVELS : levels;
  }

  // ------------------------------------------------------------------ rules

  private Widget createRulesTab() {
    if (!overview.isRulesEnabled()) {
      return createEmptyLabel(messages.permissionOverviewRulesDisabled());
    }

    List<AccessRule> rules = safe(overview.getRules());
    if (rules.isEmpty()) {
      return createEmptyLabel(messages.permissionOverviewRulesEmpty());
    }

    List<BasicTablePanel.ColumnInfo<AccessRule>> columns = new ArrayList<>();
    columns.add(new BasicTablePanel.ColumnInfo<AccessRule>(messages.permissionOverviewRuleGroup(), 0,
      new TextColumn<AccessRule>() {
        @Override
        public String getValue(AccessRule rule) {
          return rule.getGroup() != null ? rule.getGroup() : "";
        }
      }, CELL_STYLE_NOWRAP));
    columns.add(new BasicTablePanel.ColumnInfo<AccessRule>(messages.permissionOverviewRuleEnabled(), 0,
      new Column<AccessRule, SafeHtml>(new SafeHtmlCell()) {
        @Override
        public SafeHtml getValue(AccessRule rule) {
          return createActiveHtml(rule.isEnabled(), messages.permissionOverviewYes(), messages.permissionOverviewNo());
        }
      }, CELL_STYLE_NOWRAP));
    columns.add(new BasicTablePanel.ColumnInfo<AccessRule>(messages.permissionOverviewRuleSekretess(), 0,
      new TextColumn<AccessRule>() {
        @Override
        public String getValue(AccessRule rule) {
          return StringUtils.prettyPrint(safe(rule.getSekretess()));
        }
      }));
    columns.add(new BasicTablePanel.ColumnInfo<AccessRule>(messages.permissionOverviewRuleLagrum(), 0,
      new TextColumn<AccessRule>() {
        @Override
        public String getValue(AccessRule rule) {
          return StringUtils.prettyPrint(safe(rule.getLagrum()));
        }
      }));
    columns.add(new BasicTablePanel.ColumnInfo<AccessRule>(messages.permissionOverviewRulePersonuppgifter(), 0,
      new TextColumn<AccessRule>() {
        @Override
        public String getValue(AccessRule rule) {
          return StringUtils.prettyPrint(safe(rule.getPersonuppgifter()));
        }
      }));
    columns.add(new BasicTablePanel.ColumnInfo<AccessRule>(messages.permissionOverviewRuleNiva(), 0,
      new TextColumn<AccessRule>() {
        @Override
        public String getValue(AccessRule rule) {
          return StringUtils.prettyPrint(safe(rule.getPersonuppgiftsniva()));
        }
      }));
    columns.add(new BasicTablePanel.ColumnInfo<AccessRule>(messages.permissionOverviewRuleNote(), 0,
      new TextColumn<AccessRule>() {
        @Override
        public String getValue(AccessRule rule) {
          return rule.getNote() != null ? rule.getNote() : "";
        }
      }));

    return wrapTable(new BasicTablePanel<AccessRule>(rules.iterator(), toArray(columns)));
  }

  // ---------------------------------------------------------------- helpers

  private static <T> List<T> safe(List<T> list) {
    return list != null ? list : Collections.<T> emptyList();
  }

  private static Widget createEmptyLabel(String text) {
    Label label = new Label(text);
    label.addStyleName("basicTableEmpty");
    return label;
  }

  private static Widget wrapTable(Widget table) {
    FlowPanel tablePanel = new FlowPanel();
    tablePanel.addStyleName("basicTable");
    tablePanel.add(table);
    return tablePanel;
  }

  @SuppressWarnings("unchecked")
  private static <C> BasicTablePanel.ColumnInfo<C>[] toArray(List<BasicTablePanel.ColumnInfo<C>> columns) {
    return columns.toArray(new BasicTablePanel.ColumnInfo[columns.size()]);
  }

  /** En rad i matrisen "Rättigheter per grupp". */
  private static class RoleRow {
    private final String area;
    private final String role;
    private final String description;

    private RoleRow(String area, String role, String description) {
      this.area = area;
      this.role = role;
      this.description = description;
    }
  }
}

/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.client.management.accessrules;

import java.util.List;

import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.v2.accessrules.AccessRule;
import org.roda.core.data.v2.accessrules.AccessRuleSet;
import org.roda.core.data.v2.accessrules.AccessRulesStatus;
import org.roda.core.data.v2.accessrules.AccessRulesVocabulary;
import org.roda.core.data.v2.index.filter.Filter;
import org.roda.core.data.v2.index.filter.SimpleFilterParameter;
import org.roda.core.data.v2.user.RODAMember;
import org.roda.wui.client.common.NoAsyncCallback;
import org.roda.wui.client.common.TitlePanel;
import org.roda.wui.client.common.UserLogin;
import org.roda.wui.client.common.dialogs.Dialogs;
import org.roda.wui.client.common.dialogs.MemberSelectDialog;
import org.roda.wui.client.common.lists.utils.BasicTablePanel;
import org.roda.wui.client.common.utils.AsyncCallbackUtils;
import org.roda.wui.client.common.utils.PermissionClientUtils;
import org.roda.wui.client.common.utils.SidebarUtils;
import org.roda.wui.client.ingest.process.ShowJob;
import org.roda.wui.client.management.Management;
import org.roda.wui.client.management.ShowGroup;
import org.roda.wui.client.services.AccessRulesRestService;
import org.roda.wui.client.services.Services;
import org.roda.wui.common.client.HistoryResolver;
import org.roda.wui.common.client.tools.HistoryUtils;
import org.roda.wui.common.client.tools.ListUtils;
import org.roda.wui.common.client.widgets.HTMLWidgetWrapper;
import org.roda.wui.common.client.widgets.Toast;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import config.i18n.client.ClientMessages;

/**
 * Administrationssidan för åtkomstreglerna
 * (sekretess/personuppgifter → läsbehörighet per grupp). Visar status,
 * regellistan och åtgärderna lägg till/tillämpa/torrkör.
 */
public class AccessRules extends Composite {
  private static final ClientMessages messages = GWT.create(ClientMessages.class);

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
      return "access_rules";
    }
  };

  private static AccessRules instance = null;

  /**
   * Get the singleton instance
   *
   * @return the instance
   */
  public static AccessRules getInstance() {
    if (instance == null) {
      instance = new AccessRules();
    } else {
      instance.refresh();
    }
    return instance;
  }

  interface MyUiBinder extends UiBinder<Widget, AccessRules> {
  }

  private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

  @UiField
  TitlePanel title;

  @UiField
  FlowPanel accessRulesDescription;

  @UiField
  FlowPanel statusPanel;

  @UiField
  FlowPanel rulesPanel;

  @UiField
  FlowPanel contentFlowPanel;

  @UiField
  FlowPanel sidebarFlowPanel;

  @UiField
  Button buttonAddRule;

  @UiField
  Button buttonApply;

  @UiField
  Button buttonDryRun;

  private AccessRulesVocabulary vocabulary;
  private AccessRuleSet ruleSet;

  private final boolean canManage;
  private final boolean canApply;

  public AccessRules() {
    initWidget(uiBinder.createAndBindUi(this));

    accessRulesDescription.add(new HTMLWidgetWrapper("AccessRulesDescription.html"));
    accessRulesDescription.setVisible(false);
    Anchor infoIconTitle = new Anchor();
    infoIconTitle.getElement().setInnerHTML("<i class=\"fa fa-info-circle\"></i>");
    infoIconTitle.addStyleName("description-toggle-icon");
    infoIconTitle.setTitle(messages.descriptionToggleTitle());
    infoIconTitle.addClickHandler(event -> accessRulesDescription.setVisible(!accessRulesDescription.isVisible()));
    title.add(infoIconTitle);

    canManage = PermissionClientUtils.hasPermissions(RodaConstants.PERMISSION_METHOD_UPDATE_ACCESS_RULE);
    canApply = PermissionClientUtils.hasPermissions(RodaConstants.PERMISSION_METHOD_APPLY_ACCESS_RULES);
    buttonAddRule.setVisible(canManage);
    buttonApply.setVisible(canApply);
    buttonDryRun.setVisible(canApply);
    SidebarUtils.toggleSidebar(contentFlowPanel, sidebarFlowPanel, canManage || canApply);

    refresh();
  }

  private void refresh() {
    Services services = new Services("Retrieve access rules", "get");
    services.accessRulesResource(s -> s.retrieveVocabulary(LocaleInfo.getCurrentLocale().getLocaleName()))
      .whenComplete((vocabularyResult, vocabularyError) -> {
        if (vocabularyError != null) {
          loadFailed(vocabularyError);
          return;
        }
        vocabulary = vocabularyResult;
        services.accessRulesResource(AccessRulesRestService::listAccessRules).whenComplete((set, setError) -> {
          if (setError != null) {
            loadFailed(setError);
            return;
          }
          ruleSet = set;
          services.accessRulesResource(AccessRulesRestService::retrieveStatus).whenComplete((status, statusError) -> {
            if (statusError != null) {
              loadFailed(statusError);
            } else {
              renderStatus(status);
              renderRules();
            }
          });
        });
      });
  }

  private void loadFailed(Throwable error) {
    statusPanel.clear();
    rulesPanel.clear();
    Label label = new Label(messages.accessRulesLoadError());
    label.addStyleName("basicTableEmpty");
    rulesPanel.add(label);
    AsyncCallbackUtils.defaultFailureTreatment(error);
  }

  private void renderStatus(AccessRulesStatus status) {
    statusPanel.clear();

    statusPanel.add(field(messages.accessRulesStatus(),
      new Label(status.isEnabled() ? messages.accessRulesStatusActive() : messages.accessRulesStatusDisabled())));
    statusPanel.add(field(messages.accessRulesVersion(), new Label(String.valueOf(status.getVersion()))));

    Widget lastApplied;
    if (status.getLastAppliedOn() != null && !status.getLastAppliedOn().isEmpty()) {
      FlowPanel panel = new FlowPanel();
      panel.add(new InlineLabel(AccessRulesFormat.dateTime(status.getLastAppliedOn())));
      if (status.getLastJobId() != null && !status.getLastJobId().isEmpty()) {
        panel.add(new InlineLabel(" "));
        panel.add(new Anchor(messages.accessRulesShowJob(),
          HistoryUtils.createHistoryHashLink(ShowJob.RESOLVER, status.getLastJobId())));
      }
      lastApplied = panel;
    } else {
      lastApplied = new Label(messages.accessRulesNeverApplied());
    }
    statusPanel.add(field(messages.accessRulesLastApplied(), lastApplied));

    if (ruleSet.getUpdatedOn() != null && !ruleSet.getUpdatedOn().isEmpty()) {
      String updatedBy = ruleSet.getUpdatedBy() != null ? ruleSet.getUpdatedBy() : "";
      statusPanel.add(field(messages.accessRulesLastChanged(),
        new Label(messages.dateCreatedOrUpdated(AccessRulesFormat.dateTime(ruleSet.getUpdatedOn()), updatedBy))));
    }
  }

  private FlowPanel field(String label, Widget value) {
    FlowPanel field = new FlowPanel();
    field.addStyleName("field");
    Label labelWidget = new Label(label);
    labelWidget.addStyleName("label");
    value.addStyleName("value");
    field.add(labelWidget);
    field.add(value);
    return field;
  }

  private void renderRules() {
    rulesPanel.clear();
    List<AccessRule> rules = ruleSet.getRules();

    if (rules.isEmpty()) {
      Label empty = new Label(messages.accessRulesNoRules());
      empty.addStyleName("basicTableEmpty");
      rulesPanel.add(empty);
      return;
    }

    Column<AccessRule, SafeHtml> groupColumn = new Column<AccessRule, SafeHtml>(new SafeHtmlCell()) {
      @Override
      public SafeHtml getValue(AccessRule rule) {
        return link(HistoryUtils.createHistoryHashLink(ShowGroup.RESOLVER, rule.getGroup()), rule.getGroup());
      }
    };

    // riktiga länkar, inte klickbar text: nåbara med Tab/Enter (WCAG 2.1.1)
    Column<AccessRule, SafeHtml> editColumn = new Column<AccessRule, SafeHtml>(new SafeHtmlCell()) {
      @Override
      public SafeHtml getValue(AccessRule rule) {
        return link(HistoryUtils.createHistoryHashLink(EditAccessRule.RESOLVER, rule.getGroup()),
          messages.editButton());
      }
    };

    Column<AccessRule, String> removeColumn = new Column<AccessRule, String>(new ActionLinkCell()) {
      @Override
      public String getValue(AccessRule rule) {
        return messages.removeButton();
      }
    };
    removeColumn.setFieldUpdater((index, rule, value) -> removeRule(rule));

    BasicTablePanel<AccessRule> table = new BasicTablePanel<AccessRule>(rules.iterator(),
      new BasicTablePanel.ColumnInfo<>(messages.accessRuleGroup(), 12, groupColumn),
      new BasicTablePanel.ColumnInfo<>(messages.accessRuleEnabled(), 5, new TextColumn<AccessRule>() {
        @Override
        public String getValue(AccessRule rule) {
          return AccessRulesFormat.enabled(rule.isEnabled());
        }
      }), new BasicTablePanel.ColumnInfo<>(messages.accessRuleSekretess(), 0, new TextColumn<AccessRule>() {
        @Override
        public String getValue(AccessRule rule) {
          return AccessRulesFormat.labels(rule.getSekretess(), vocabulary);
        }
      }), new BasicTablePanel.ColumnInfo<>(messages.accessRuleLagrum(), 0, new TextColumn<AccessRule>() {
        @Override
        public String getValue(AccessRule rule) {
          return AccessRulesFormat.lagrum(rule.getLagrum());
        }
      }), new BasicTablePanel.ColumnInfo<>(messages.accessRulePersonuppgifter(), 0, new TextColumn<AccessRule>() {
        @Override
        public String getValue(AccessRule rule) {
          return AccessRulesFormat.labels(rule.getPersonuppgifter(), vocabulary);
        }
      }), new BasicTablePanel.ColumnInfo<>(messages.accessRulePersonuppgiftsniva(), 0, new TextColumn<AccessRule>() {
        @Override
        public String getValue(AccessRule rule) {
          return AccessRulesFormat.labels(rule.getPersonuppgiftsniva(), vocabulary);
        }
      }), new BasicTablePanel.ColumnInfo<>(messages.editButton(), !canManage, 6, editColumn),
      new BasicTablePanel.ColumnInfo<>(messages.removeButton(), !canManage, 6, removeColumn));

    rulesPanel.add(table);
  }

  private static SafeHtml link(String href, String text) {
    SafeHtmlBuilder builder = new SafeHtmlBuilder();
    builder.append(SafeHtmlUtils.fromTrustedString("<a href=\"" + SafeHtmlUtils.htmlEscape(href) + "\">"));
    builder.appendEscaped(text);
    builder.append(SafeHtmlUtils.fromSafeConstant("</a>"));
    return builder.toSafeHtml();
  }

  /**
   * Tabellåtgärd som renderas som en länk (fokuserbar med Tab, aktiveras med
   * Enter) i stället för klickbar text, så att den nås med tangentbord. Klicket
   * går till kolumnens FieldUpdater och navigeringen till "#" stoppas.
   */
  private static class ActionLinkCell extends AbstractCell<String> {
    ActionLinkCell() {
      super(BrowserEvents.CLICK);
    }

    @Override
    public void render(Context context, String value, SafeHtmlBuilder sb) {
      if (value != null) {
        sb.append(SafeHtmlUtils.fromSafeConstant("<a href=\"#\" role=\"button\">"));
        sb.appendEscaped(value);
        sb.append(SafeHtmlUtils.fromSafeConstant("</a>"));
      }
    }

    @Override
    public void onBrowserEvent(Context context, Element parent, String value, NativeEvent event,
      ValueUpdater<String> valueUpdater) {
      super.onBrowserEvent(context, parent, value, event, valueUpdater);
      if (BrowserEvents.CLICK.equals(event.getType())) {
        event.preventDefault();
        if (valueUpdater != null) {
          valueUpdater.update(value);
        }
      }
    }
  }

  private void removeRule(AccessRule rule) {
    Dialogs.showConfirmDialog(messages.accessRuleRemoveConfirmTitle(),
      messages.accessRuleRemoveConfirmMessage(rule.getGroup()), messages.dialogNo(), messages.dialogYes(),
      new NoAsyncCallback<Boolean>() {
        @Override
        public void onSuccess(Boolean confirmed) {
          if (confirmed) {
            Services services = new Services("Delete access rule", "delete");
            services.accessRulesResource(s -> s.deleteAccessRule(rule.getGroup())).whenComplete((result, error) -> {
              if (error != null) {
                AsyncCallbackUtils.defaultFailureTreatment(error);
              } else {
                Toast.showInfo(messages.accessRulesTitle(), messages.accessRuleRemoved(rule.getGroup()));
                refresh();
              }
            });
          }
        }
      });
  }

  @UiHandler("buttonAddRule")
  void buttonAddRuleHandler(ClickEvent e) {
    Filter filter = new Filter(new SimpleFilterParameter(RodaConstants.MEMBERS_IS_USER, "false"));
    MemberSelectDialog selectDialog = new MemberSelectDialog(messages.accessRulesSelectGroup(), filter);
    selectDialog.showAndCenter();
    selectDialog.addValueChangeHandler(event -> {
      RODAMember selected = event.getValue();
      if (selected != null && !selected.isUser()) {
        HistoryUtils.newHistory(EditAccessRule.RESOLVER, selected.getName());
      }
    });
  }

  @UiHandler("buttonApply")
  void buttonApplyHandler(ClickEvent e) {
    apply(false);
  }

  @UiHandler("buttonDryRun")
  void buttonDryRunHandler(ClickEvent e) {
    apply(true);
  }

  private void apply(boolean dryRun) {
    String dialogTitle = dryRun ? messages.accessRulesDryRunConfirmTitle() : messages.accessRulesApplyConfirmTitle();
    String dialogMessage = dryRun ? messages.accessRulesDryRunConfirmMessage()
      : messages.accessRulesApplyConfirmMessage();

    Dialogs.showConfirmDialog(dialogTitle, dialogMessage, messages.dialogNo(), messages.dialogYes(),
      new NoAsyncCallback<Boolean>() {
        @Override
        public void onSuccess(Boolean confirmed) {
          if (confirmed) {
            Services services = new Services(dryRun ? "Dry run access rules" : "Apply access rules", "job");
            services.accessRulesResource(s -> s.applyAccessRules(dryRun)).whenComplete((job, error) -> {
              if (error != null) {
                AsyncCallbackUtils.defaultFailureTreatment(error);
              } else {
                Toast.showInfo(messages.accessRulesJobStartedTitle(), messages.accessRulesJobStartedMessage());
                Dialogs.showJobRedirectDialog(messages.jobCreatedMessage(), new AsyncCallback<Void>() {
                  @Override
                  public void onFailure(Throwable caught) {
                    refresh();
                  }

                  @Override
                  public void onSuccess(final Void nothing) {
                    HistoryUtils.newHistory(ShowJob.RESOLVER, job.getId());
                  }
                });
              }
            });
          }
        }
      });
  }

  public void resolve(List<String> historyTokens, AsyncCallback<Widget> callback) {
    if (historyTokens.isEmpty()) {
      callback.onSuccess(this);
    } else if (historyTokens.get(0).equals(EditAccessRule.RESOLVER.getHistoryToken())) {
      EditAccessRule.RESOLVER.resolve(HistoryUtils.tail(historyTokens), callback);
    } else {
      HistoryUtils.newHistory(RESOLVER);
      callback.onSuccess(null);
    }
  }
}

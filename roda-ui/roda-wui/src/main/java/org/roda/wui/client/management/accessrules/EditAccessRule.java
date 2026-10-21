/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.client.management.accessrules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.exceptions.NotFoundException;
import org.roda.core.data.v2.accessrules.AccessRule;
import org.roda.core.data.v2.accessrules.AccessRulesVocabulary;
import org.roda.core.data.v2.accessrules.LagrumNormalizeRequest;
import org.roda.wui.client.common.NoAsyncCallback;
import org.roda.wui.client.common.UserLogin;
import org.roda.wui.client.common.dialogs.Dialogs;
import org.roda.wui.client.common.utils.AsyncCallbackUtils;
import org.roda.wui.client.common.utils.PermissionClientUtils;
import org.roda.wui.client.services.Services;
import org.roda.wui.common.client.HistoryResolver;
import org.roda.wui.common.client.tools.HistoryUtils;
import org.roda.wui.common.client.tools.ListUtils;
import org.roda.wui.common.client.tools.StringUtils;
import org.roda.wui.common.client.widgets.Toast;
import org.roda.wui.common.client.widgets.wcag.WCAGUtilities;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import config.i18n.client.ClientMessages;

/**
 * Formulär för en grupps åtkomstregel. Sökvägen är
 * administration/access_rules/edit/{grupp}; saknar gruppen regel (404) öppnas
 * ett formulär med "Alla (*)" förvalt i varje grupp (en regel tillåter bara
 * det den listar) som skapar regeln vid sparning. Sidan kräver
 * access_rules.manage – listsidans läsroll räcker inte.
 */
public class EditAccessRule extends Composite {

  public static final HistoryResolver RESOLVER = new HistoryResolver() {

    @Override
    public void resolve(List<String> historyTokens, final AsyncCallback<Widget> callback) {
      if (historyTokens.size() == 1) {
        String group = historyTokens.get(0);
        Services services = new Services("Retrieve access rule", "get");
        services.accessRulesResource(s -> s.retrieveVocabulary(LocaleInfo.getCurrentLocale().getLocaleName()))
          .whenComplete((vocabulary, vocabularyError) -> {
            if (vocabularyError != null) {
              callback.onFailure(vocabularyError);
              return;
            }
            services.accessRulesResource(s -> s.retrieveAccessRule(group)).whenComplete((rule, ruleError) -> {
              if (rule != null) {
                callback.onSuccess(new EditAccessRule(group, rule, vocabulary, true));
              } else if (ruleError instanceof NotFoundException) {
                callback.onSuccess(new EditAccessRule(group, new AccessRule(group), vocabulary, false));
              } else if (ruleError != null) {
                callback.onFailure(ruleError);
              }
            });
          });
      } else {
        HistoryUtils.newHistory(AccessRules.RESOLVER);
        callback.onSuccess(null);
      }
    }

    @Override
    public void isCurrentUserPermitted(AsyncCallback<Boolean> callback) {
      // listsidans roll (access_rules.read) räcker inte: redigering kräver
      // access_rules.manage, annars visas ett formulär som bara kan ge 403
      UserLogin.getInstance().checkRoles(new HistoryResolver[] {AccessRules.RESOLVER}, false,
        new AsyncCallback<Boolean>() {
          @Override
          public void onFailure(Throwable caught) {
            callback.onFailure(caught);
          }

          @Override
          public void onSuccess(Boolean permitted) {
            callback.onSuccess(Boolean.TRUE.equals(permitted)
              && PermissionClientUtils.hasPermissions(RodaConstants.PERMISSION_METHOD_UPDATE_ACCESS_RULE));
          }
        });
    }

    @Override
    public List<String> getHistoryPath() {
      return ListUtils.concat(AccessRules.RESOLVER.getHistoryPath(), getHistoryToken());
    }

    @Override
    public String getHistoryToken() {
      return "edit";
    }
  };

  interface MyUiBinder extends UiBinder<Widget, EditAccessRule> {
  }

  private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

  private static final ClientMessages messages = GWT.create(ClientMessages.class);

  /**
   * Kryssrutegrupp med vokabulärens värden plus "Alla (*)". Markeras "*" blir
   * de övriga rutorna irrelevanta och låses; getValues() ger då bara "*".
   * Hjälptexten påminner om att inga markerade = inget tillåtet.
   */
  private class ValueGroup extends FlowPanel {
    private final Map<String, CheckBox> checkboxes = new LinkedHashMap<>();
    private final CheckBox allCheckbox;

    ValueGroup(String groupTitle, List<String> values, List<String> selected, String help) {
      this.addStyleName("permissions");

      FlowPanel container = new FlowPanel();
      container.addStyleName("permission-role-container");

      FlowPanel header = new FlowPanel();
      header.addStyleName("permission-role-container-title");
      header.add(new Label(groupTitle));
      container.add(header);

      Label helpLabel = new Label(help);
      helpLabel.addStyleName("form-help");
      container.add(helpLabel);

      allCheckbox = addOption(container, AccessRulesFormat.ALL, messages.accessRuleAll(),
        selected.contains(AccessRulesFormat.ALL));
      for (String value : values) {
        addOption(container, value, vocabulary.labelOf(value), selected.contains(value));
      }

      allCheckbox.addValueChangeHandler(event -> updateEnabledState());
      updateEnabledState();
      this.add(container);
    }

    private CheckBox addOption(FlowPanel container, String value, String label, boolean checked) {
      HorizontalPanel row = new HorizontalPanel();
      row.addStyleName("permission");

      CheckBox checkbox = new CheckBox();
      checkbox.addStyleName("permission-checkbox");
      checkbox.setValue(checked);
      WCAGUtilities.addTitleToCheckbox(checkbox, label);

      Label description = new Label(label);
      description.setStylePrimaryName("permission-description");
      description.addClickHandler(event -> {
        if (checkbox.isEnabled()) {
          checkbox.setValue(!checkbox.getValue(), true);
        }
      });

      row.add(checkbox);
      row.add(description);
      container.add(row);
      checkboxes.put(value, checkbox);
      return checkbox;
    }

    private void updateEnabledState() {
      boolean all = Boolean.TRUE.equals(allCheckbox.getValue());
      for (Map.Entry<String, CheckBox> entry : checkboxes.entrySet()) {
        if (!AccessRulesFormat.ALL.equals(entry.getKey())) {
          entry.getValue().setEnabled(!all);
        }
      }
    }

    /** Låser "Alla (*)" markerad – när det inte finns någon vokabulär att välja ur. */
    void lockAll() {
      allCheckbox.setValue(true);
      allCheckbox.setEnabled(false);
      updateEnabledState();
    }

    List<String> getValues() {
      List<String> result = new ArrayList<>();
      if (Boolean.TRUE.equals(allCheckbox.getValue())) {
        result.add(AccessRulesFormat.ALL);
        return result;
      }
      for (Map.Entry<String, CheckBox> entry : checkboxes.entrySet()) {
        if (!AccessRulesFormat.ALL.equals(entry.getKey()) && Boolean.TRUE.equals(entry.getValue().getValue())) {
          result.add(entry.getKey());
        }
      }
      return result;
    }
  }

  private final String group;
  private final AccessRulesVocabulary vocabulary;

  private final List<TextBox> lagrumBoxes = new ArrayList<>();
  private ValueGroup sekretessGroup;
  private ValueGroup personuppgifterGroup;
  private ValueGroup personuppgiftsnivaGroup;
  private CheckBox enabledCheckbox;
  private CheckBox applyCheckbox;

  @UiField
  FlowPanel formPanel;

  @UiField
  Label groupValue;

  @UiField
  FlowPanel enabledPanel;

  @UiField
  FlowPanel sekretessPanel;

  @UiField
  FlowPanel lagrumRows;

  @UiField
  Button buttonLagrumAdd;

  @UiField
  Button buttonLagrumNormalize;

  @UiField
  FlowPanel lagrumResult;

  @UiField
  FlowPanel personuppgifterPanel;

  @UiField
  FlowPanel personuppgiftsnivaPanel;

  @UiField
  TextArea noteTextArea;

  @UiField
  FlowPanel applyPanel;

  @UiField
  Button buttonSave;

  @UiField
  Button buttonRemove;

  @UiField
  Button buttonCancel;

  /**
   * Create a new panel to edit an access rule
   *
   * @param group
   *          the group the rule applies to (from the history path)
   * @param rule
   *          the stored rule, or an empty rule when the group has none
   * @param vocabulary
   *          allowed values and their labels
   * @param existing
   *          whether the rule is stored (enables remove)
   */
  public EditAccessRule(String group, AccessRule rule, AccessRulesVocabulary vocabulary, boolean existing) {
    this.group = group;
    this.vocabulary = vocabulary;

    initWidget(uiBinder.createAndBindUi(this));

    groupValue.setText(group);

    // sparning/borttagning kräver access_rules.manage; resolvern släpper
    // normalt inte in andra, men formuläret visar ändå inga knappar som
    // bara skulle ge 403
    final boolean canManage = PermissionClientUtils
      .hasPermissions(RodaConstants.PERMISSION_METHOD_UPDATE_ACCESS_RULE);
    // ny regel: "Alla (*)" förvalt i varje grupp – en tom lista tillåter
    // inget, och en regel som sparas orörd ska inte spärra gruppen från hela
    // arkivet
    final List<String> all = Collections.singletonList(AccessRulesFormat.ALL);

    enabledCheckbox = new CheckBox(messages.accessRuleEnabled());
    enabledCheckbox.addStyleName("form-checkbox");
    enabledCheckbox.setValue(rule.isEnabled());
    WCAGUtilities.addTitleToCheckbox(enabledCheckbox, messages.accessRuleEnabled());
    enabledPanel.add(enabledCheckbox);
    Label enabledHelp = new Label(messages.accessRuleEnabledHelp());
    enabledHelp.addStyleName("form-help");
    enabledPanel.add(enabledHelp);

    sekretessGroup = new ValueGroup(messages.accessRuleSekretess(), vocabulary.getSekretess(),
      existing ? rule.getSekretess() : all, messages.accessRuleValuesHelp());
    sekretessPanel.add(sekretessGroup);

    if (rule.getLagrum().isEmpty()) {
      addLagrumRow(existing ? "" : AccessRulesFormat.ALL);
    } else {
      for (String lagrum : rule.getLagrum()) {
        addLagrumRow(lagrum);
      }
    }

    personuppgifterGroup = new ValueGroup(messages.accessRulePersonuppgifter(), vocabulary.getPersonuppgifter(),
      existing ? rule.getPersonuppgifter() : all, messages.accessRuleValuesHelp());
    personuppgifterPanel.add(personuppgifterGroup);

    boolean nivaVocabularyEmpty = vocabulary.getPersonuppgiftsniva().isEmpty();
    personuppgiftsnivaGroup = new ValueGroup(messages.accessRulePersonuppgiftsniva(),
      vocabulary.getPersonuppgiftsniva(), existing ? rule.getPersonuppgiftsniva() : all,
      nivaVocabularyEmpty ? messages.accessRulePersonuppgiftsnivaNoVocabulary() : messages.accessRuleValuesHelp());
    if (nivaVocabularyEmpty) {
      // utan vokabulär finns bara "Alla (*)" att välja; lämnad omarkerad
      // skulle varje arkivobjekt med ett nivåvärde spärras
      personuppgiftsnivaGroup.lockAll();
    }
    personuppgiftsnivaPanel.add(personuppgiftsnivaGroup);

    noteTextArea.setText(rule.getNote() != null ? rule.getNote() : "");

    applyCheckbox = new CheckBox(messages.accessRuleApplyAfterSave());
    applyCheckbox.addStyleName("form-checkbox");
    WCAGUtilities.addTitleToCheckbox(applyCheckbox, messages.accessRuleApplyAfterSave());
    applyPanel.add(applyCheckbox);
    applyPanel.setVisible(canManage);

    buttonSave.setVisible(canManage);
    buttonRemove.setVisible(existing && canManage);

    WCAGUtilities.getInstance().makeAccessible(formPanel.getElement());
  }

  private void addLagrumRow(String value) {
    HorizontalPanel row = new HorizontalPanel();
    row.addStyleName("permission");

    TextBox textBox = new TextBox();
    textBox.addStyleName("form-textbox");
    textBox.setTitle(messages.accessRuleLagrum());
    textBox.setText(value);

    // normaliseringsresultatet gäller bara fälten som de såg ut när det togs fram
    textBox.addKeyUpHandler(event -> lagrumResult.clear());
    textBox.addValueChangeHandler(event -> lagrumResult.clear());

    Button remove = new Button(messages.accessRuleLagrumRemove());
    remove.addStyleName("btn btn-link btn-danger");
    remove.addClickHandler(event -> {
      lagrumRows.remove(row);
      lagrumBoxes.remove(textBox);
      lagrumResult.clear();
    });

    row.add(textBox);
    row.add(remove);
    lagrumRows.add(row);
    lagrumBoxes.add(textBox);
  }

  private List<String> getLagrumInput() {
    List<String> result = new ArrayList<>();
    for (TextBox textBox : lagrumBoxes) {
      String text = textBox.getText().trim();
      if (!text.isEmpty()) {
        result.add(text);
      }
    }
    return result;
  }

  @UiHandler("buttonLagrumAdd")
  void buttonLagrumAddHandler(ClickEvent e) {
    lagrumResult.clear();
    addLagrumRow("");
  }

  @UiHandler("buttonLagrumNormalize")
  void buttonLagrumNormalizeHandler(ClickEvent e) {
    lagrumResult.clear();
    List<String> input = getLagrumInput();
    if (input.isEmpty()) {
      lagrumResult.add(resultLine(messages.accessRuleLagrumNormalized() + ": " + messages.accessRuleNone(), false));
      return;
    }

    Services services = new Services("Normalize legal references", "get");
    services.accessRulesResource(s -> s.normalizeLagrum(new LagrumNormalizeRequest(input)))
      .whenComplete((result, error) -> {
        if (error != null) {
          AsyncCallbackUtils.defaultRestErrorTreatment(error);
        } else {
          lagrumResult.add(resultLine(messages.accessRuleLagrumNormalized() + ": "
            + (result.getNormalized().isEmpty() ? messages.accessRuleNone()
              : StringUtils.join(result.getNormalized(), ", ")), false));

          if (!result.getWarnings().isEmpty()) {
            lagrumResult.add(resultLine(
              messages.accessRuleLagrumWarnings() + ": " + StringUtils.join(result.getWarnings(), "; "), true));
          }

          if (!result.getNormalized().isEmpty()) {
            // det som sparas ska kunna vara exakt det som förhandsvisades (en per rad)
            Button useNormalized = new Button(messages.accessRuleLagrumUseNormalized());
            useNormalized.addStyleName("btn btn-link");
            useNormalized.addClickHandler(event -> replaceLagrumRows(result.getNormalized()));
            lagrumResult.add(useNormalized);
          }
        }
      });
  }

  private static Label resultLine(String text, boolean error) {
    Label label = new Label(text);
    label.addStyleName(error ? "form-help error" : "form-help");
    return label;
  }

  /** Ersätter lagrumsraderna med de normaliserade formerna, en per rad. */
  private void replaceLagrumRows(List<String> normalized) {
    lagrumRows.clear();
    lagrumBoxes.clear();
    for (String value : normalized) {
      addLagrumRow(value);
    }
    lagrumResult.clear();
  }

  @UiHandler("buttonSave")
  void buttonSaveHandler(ClickEvent e) {
    AccessRule toSave = new AccessRule(group);
    toSave.setEnabled(Boolean.TRUE.equals(enabledCheckbox.getValue()));
    toSave.setSekretess(sekretessGroup.getValues());
    toSave.setLagrum(getLagrumInput());
    toSave.setPersonuppgifter(personuppgifterGroup.getValues());
    toSave.setPersonuppgiftsniva(personuppgiftsnivaGroup.getValues());
    String note = noteTextArea.getText().trim();
    toSave.setNote(note.isEmpty() ? null : note);
    final boolean applyNow = Boolean.TRUE.equals(applyCheckbox.getValue());

    buttonSave.setEnabled(false);
    Services services = new Services("Update access rule", "update");
    services.accessRulesResource(s -> s.updateAccessRule(group, toSave, applyNow)).whenComplete((saved, error) -> {
      buttonSave.setEnabled(true);
      if (error != null) {
        AsyncCallbackUtils.defaultRestErrorTreatment(error);
      } else {
        Toast.showInfo(messages.accessRuleSavedTitle(),
          applyNow ? messages.accessRuleSavedAndApplyMessage() : messages.accessRuleSavedMessage());
        HistoryUtils.newHistory(AccessRules.RESOLVER);
      }
    });
  }

  @UiHandler("buttonRemove")
  void buttonRemoveHandler(ClickEvent e) {
    Dialogs.showConfirmDialog(messages.accessRuleRemoveConfirmTitle(), messages.accessRuleRemoveConfirmMessage(group),
      messages.dialogNo(), messages.dialogYes(), new NoAsyncCallback<Boolean>() {
        @Override
        public void onSuccess(Boolean confirmed) {
          if (confirmed) {
            Services services = new Services("Delete access rule", "delete");
            services.accessRulesResource(s -> s.deleteAccessRule(group)).whenComplete((result, error) -> {
              if (error != null) {
                AsyncCallbackUtils.defaultFailureTreatment(error);
              } else {
                Toast.showInfo(messages.accessRulesTitle(), messages.accessRuleRemoved(group));
                HistoryUtils.newHistory(AccessRules.RESOLVER);
              }
            });
          }
        }
      });
  }

  @UiHandler("buttonCancel")
  void buttonCancelHandler(ClickEvent e) {
    HistoryUtils.newHistory(AccessRules.RESOLVER);
  }
}

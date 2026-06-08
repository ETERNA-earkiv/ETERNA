/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.client.disposal.rule;

import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;

import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.v2.common.Pair;
import org.roda.core.data.v2.disposal.rule.DisposalRuleCondition;
import org.roda.core.data.v2.ip.IndexedAIP;
import org.roda.wui.common.client.tools.ConfigurationManager;
import org.roda.wui.common.client.tools.StringUtils;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import config.i18n.client.ClientMessages;

/**
 * Lets the user define one or more metadata-field conditions for a disposal rule. Conditions are ANDed together: an
 * AIP matches only if all of them hold. Each condition is one row (field dropdown + value), and rows can be added or
 * removed.
 *
 * @author Tiago Fraga <tfraga@keep.pt>
 */
public class MetadataFieldsPanel extends Composite implements HasValueChangeHandlers<List<DisposalRuleCondition>> {

  public static final String IS_WRONG = "isWrong";
  private static final ClientMessages messages = GWT.create(ClientMessages.class);
  private static MetadataFieldsPanel.MyUiBinder uiBinder = GWT.create(MetadataFieldsPanel.MyUiBinder.class);

  @UiField
  Label metadataFieldLabel;

  @UiField
  FlowPanel conditionsPanel;

  @UiField
  Button addConditionButton;

  @UiField
  Label metadataFieldError;

  private final List<Pair<String, String>> fieldOptions;
  private final List<ConditionRow> rows = new ArrayList<>();
  private boolean changed = false;
  private boolean checked = false;

  public MetadataFieldsPanel(List<DisposalRuleCondition> conditions, boolean editMode) {
    initWidget(uiBinder.createAndBindUi(this));

    this.fieldOptions = getElementsFromConfig();

    addConditionButton.setText(messages.disposalRuleAddCondition());
    addConditionButton.addClickHandler(event -> {
      addRow(null);
      onChange();
    });

    if (editMode && conditions != null && !conditions.isEmpty()) {
      for (DisposalRuleCondition condition : conditions) {
        addRow(condition);
      }
    } else {
      addRow(null);
    }
  }

  private static List<Pair<String, String>> getElementsFromConfig() {
    List<Pair<String, String>> elements = new ArrayList<>();
    String classSimpleName = IndexedAIP.class.getSimpleName();
    List<String> fields = ConfigurationManager.getStringList(RodaConstants.SEARCH_FIELD_PREFIX, classSimpleName);

    for (String field : fields) {
      String fieldPrefix = RodaConstants.SEARCH_FIELD_PREFIX + '.' + classSimpleName + '.' + field;
      String fieldType = ConfigurationManager.getString(fieldPrefix, RodaConstants.SEARCH_FIELD_TYPE);
      String fieldsName = ConfigurationManager.getString(fieldPrefix, RodaConstants.SEARCH_FIELD_FIELDS);

      if (RodaConstants.SEARCH_FIELD_TYPE_TEXT.equals(fieldType) && showField(field)) {
        String fieldLabelI18N = ConfigurationManager.getString(fieldPrefix, RodaConstants.SEARCH_FIELD_I18N);
        String translation = fieldLabelI18N;
        try {
          translation = ConfigurationManager.getTranslation(fieldLabelI18N);
        } catch (MissingResourceException e) {
          // do nothing
        }

        elements.add(new Pair<>(fieldsName, translation));
      }
    }
    return elements;
  }

  private static boolean showField(String field) {
    List<String> blackList = ConfigurationManager.getStringList(RodaConstants.DISPOSAL_RULE_BLACKLIST_CONDITION);
    return !blackList.contains(field);
  }

  private void addRow(DisposalRuleCondition condition) {
    ConditionRow row = new ConditionRow(condition);
    rows.add(row);
    conditionsPanel.add(row.panel);
    updateRemoveButtons();
  }

  private void removeRow(ConditionRow row) {
    rows.remove(row);
    conditionsPanel.remove(row.panel);
    updateRemoveButtons();
    onChange();
  }

  private void updateRemoveButtons() {
    boolean canRemove = rows.size() > 1;
    for (ConditionRow row : rows) {
      row.removeButton.setVisible(canRemove);
    }
  }

  public boolean isValid() {
    boolean valid = true;
    for (ConditionRow row : rows) {
      boolean fieldOk = row.fieldsList.getSelectedIndex() > 0;
      boolean valueOk = StringUtils.isNotBlank(row.valueBox.getText());

      row.fieldsList.removeStyleName(IS_WRONG);
      row.valueBox.removeStyleName(IS_WRONG);
      if (!fieldOk) {
        row.fieldsList.addStyleName(IS_WRONG);
        valid = false;
      }
      if (!valueOk) {
        row.valueBox.addStyleName(IS_WRONG);
        valid = false;
      }
    }

    metadataFieldError.setText(messages.mandatoryField());
    metadataFieldError.setVisible(!valid);
    checked = true;
    return valid;
  }

  public boolean isChanged() {
    return changed;
  }

  @Override
  public HandlerRegistration addValueChangeHandler(ValueChangeHandler<List<DisposalRuleCondition>> handler) {
    return addHandler(handler, ValueChangeEvent.getType());
  }

  protected void onChange() {
    changed = true;
    if (checked) {
      isValid();
    }
    ValueChangeEvent.fire(this, getValue());
  }

  public List<DisposalRuleCondition> getValue() {
    List<DisposalRuleCondition> conditions = new ArrayList<>();
    for (ConditionRow row : rows) {
      conditions.add(new DisposalRuleCondition(row.fieldsList.getSelectedValue(), row.valueBox.getText()));
    }
    return conditions;
  }

  interface MyUiBinder extends UiBinder<Widget, MetadataFieldsPanel> {
  }

  /**
   * A single condition row: field dropdown + operator + value + a remove button (hidden when only one row remains).
   */
  private class ConditionRow {
    private final FlowPanel panel = new FlowPanel();
    private final ListBox fieldsList = new ListBox();
    private final TextBox valueBox = new TextBox();
    private final Button removeButton = new Button();

    private ConditionRow(DisposalRuleCondition condition) {
      panel.getElement().getStyle().setProperty("display", "flex");
      panel.getElement().getStyle().setProperty("marginBottom", "4px");

      FlowPanel fieldWrapper = new FlowPanel();
      fieldWrapper.addStyleName("col_2");
      fieldsList.addStyleName("form-textbox");
      fieldWrapper.add(fieldsList);

      FlowPanel operatorWrapper = new FlowPanel();
      operatorWrapper.addStyleName("disposalRuleOperator col_1");
      Label operator = new Label(messages.disposalRuleConditionOperator());
      operator.addStyleName("form-label");
      operatorWrapper.add(operator);

      FlowPanel valueWrapper = new FlowPanel();
      valueWrapper.addStyleName("col_11");
      valueBox.addStyleName("form-textbox");
      valueWrapper.add(valueBox);

      FlowPanel removeWrapper = new FlowPanel();
      removeWrapper.addStyleName("col_1");
      removeButton.setText("×");
      removeButton.setTitle(messages.disposalRuleRemoveCondition());
      removeButton.addStyleName("btn btn-link");
      removeButton.addClickHandler(event -> removeRow(this));
      removeWrapper.add(removeButton);

      panel.add(fieldWrapper);
      panel.add(operatorWrapper);
      panel.add(valueWrapper);
      panel.add(removeWrapper);

      fieldsList.addItem("", "");
      int index = 1;
      int selected = 0;
      for (Pair<String, String> option : fieldOptions) {
        fieldsList.addItem(option.getSecond(), option.getFirst());
        if (condition != null && option.getFirst().equals(condition.getKey())) {
          selected = index;
        }
        index++;
      }
      fieldsList.setSelectedIndex(selected);
      if (condition != null && condition.getValue() != null) {
        valueBox.setText(condition.getValue());
      }

      fieldsList.addChangeHandler(event -> onChange());
      valueBox.addChangeHandler(event -> onChange());
      valueBox.addKeyUpHandler(event -> onChange());
    }
  }
}

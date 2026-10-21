/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.client.common.dialogs;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.roda.core.data.v2.generics.MetadataValue;
import org.roda.core.data.v2.ip.IndexedAIP;
import org.roda.core.data.v2.ip.metadata.BulkUpdateDescriptiveMetadataRequest;
import org.roda.core.data.v2.ip.metadata.ConfiguredDescriptiveMetadata;
import org.roda.core.data.v2.index.select.SelectedItems;
import org.roda.core.data.utils.SelectedItemsUtils;
import org.roda.wui.client.common.utils.AsyncCallbackUtils;
import org.roda.wui.client.common.utils.FormUtilities;
import org.roda.wui.client.ingest.process.ShowJob;
import org.roda.wui.client.services.Services;
import org.roda.wui.common.client.tools.HistoryUtils;
import org.roda.wui.common.client.widgets.Toast;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;

import config.i18n.client.ClientMessages;

/**
 * Dialog för massredigering av ett fält i beskrivande
 * metadata för de valda strukturenheterna. Fältlistan läses dynamiskt ur
 * formulärmallen för vald metadatatyp (samma sanningskälla som pluginets
 * xpath-vitlista), och värde-widgeten anpassar sig efter fältets typ:
 * rullgardin för kontrollerade vokabulärer, datumväljare för datumfält,
 * annars textfält.
 */
public class BulkEditMetadataDialog {

  private static final ClientMessages messages = GWT.create(ClientMessages.class);

  private static final String MODE_OVERWRITE = "overwrite";
  private static final String MODE_COMPLETE = "complete";

  private final DialogBox dialogBox;
  private final ListBox typeList = new ListBox();
  private final ListBox fieldList = new ListBox();
  private final FlowPanel valuePanel = new FlowPanel();
  private final ListBox modeList = new ListBox();
  private final TextBox expectedValueBox = new TextBox();
  private final Label noFieldsLabel = new Label();
  private final Button startButton;

  private final SelectedItems<IndexedAIP> selected;
  private final Map<String, MetadataValue> fieldsById = new LinkedHashMap<>();
  private MetadataValue currentField = null;
  private boolean jobInProgress = false;

  public BulkEditMetadataDialog(SelectedItems<IndexedAIP> selected, long totalCount) {
    this.selected = selected;

    dialogBox = new DialogBox(false, true);
    dialogBox.setText(messages.bulkEditMetadataDialogTitle());
    dialogBox.setGlassEnabled(true);
    dialogBox.setAnimationEnabled(false);
    dialogBox.addStyleName("wui-dialog-information");

    FlowPanel content = new FlowPanel();
    content.addStyleName("wui-dialog-layout");

    Label countLabel = new Label(messages.bulkEditMetadataSelectedCount(totalCount));
    countLabel.addStyleName("form-help");
    content.add(countLabel);

    // Metadatatyp
    Label typeLabel = new Label(messages.bulkEditMetadataTypeLabel());
    typeLabel.addStyleName("form-label");
    content.add(typeLabel);
    typeList.addStyleName("form-textbox");
    typeList.addChangeHandler(event -> loadFields(typeList.getSelectedValue()));
    content.add(typeList);

    // Fält
    Label fieldLabel = new Label(messages.bulkEditMetadataFieldLabel());
    fieldLabel.addStyleName("form-label");
    content.add(fieldLabel);
    fieldList.addStyleName("form-textbox");
    fieldList.addChangeHandler(event -> selectField(fieldList.getSelectedValue()));
    content.add(fieldList);

    noFieldsLabel.setText(messages.bulkEditMetadataNoFields());
    noFieldsLabel.addStyleName("form-help");
    noFieldsLabel.setVisible(false);
    content.add(noFieldsLabel);

    // Nytt värde — widget byggs per valt fält av FormUtilities
    content.add(valuePanel);

    // Läge
    Label modeLabel = new Label(messages.bulkEditMetadataModeLabel());
    modeLabel.addStyleName("form-label");
    content.add(modeLabel);
    modeList.addStyleName("form-textbox");
    modeList.addItem(messages.bulkEditMetadataModeOverwrite(), MODE_OVERWRITE);
    modeList.addItem(messages.bulkEditMetadataModeComplete(), MODE_COMPLETE);
    modeList.addChangeHandler(event -> expectedValueBox.setEnabled(MODE_OVERWRITE.equals(modeList.getSelectedValue())));
    content.add(modeList);

    // Villkor (valfritt)
    Label expectedLabel = new Label(messages.bulkEditMetadataExpectedValueLabel());
    expectedLabel.addStyleName("form-label");
    content.add(expectedLabel);
    expectedValueBox.addStyleName("form-textbox");
    content.add(expectedValueBox);

    // Knappar
    FlowPanel buttonsPanel = new FlowPanel();
    buttonsPanel.addStyleName("wui-dialog-layout-footer");

    Button cancelButton = new Button(messages.cancelButton());
    cancelButton.addStyleName("btn btn-default btn-times-circle");
    cancelButton.addClickHandler(event -> dialogBox.hide());
    buttonsPanel.add(cancelButton);

    startButton = new Button(messages.bulkEditMetadataStartButton());
    startButton.addStyleName("btn btn-play");
    startButton.setEnabled(false);
    startButton.addClickHandler(event -> onStart());
    buttonsPanel.add(startButton);

    content.add(buttonsPanel);
    dialogBox.setWidget(content);

    loadTypes();
  }

  private void loadTypes() {
    Services services = new Services("Retrieve configured metadata types", "get");
    services.aipResource(s -> s.retrieveSupportedMetadataTypes(LocaleInfo.getCurrentLocale().getLocaleName()))
      .whenComplete((types, throwable) -> {
        if (throwable != null) {
          AsyncCallbackUtils.defaultFailureTreatment(throwable);
        } else {
          typeList.clear();
          for (ConfiguredDescriptiveMetadata type : types.getList()) {
            typeList.addItem(type.getLabel(), type.getId());
          }
          if (typeList.getItemCount() > 0) {
            typeList.setSelectedIndex(0);
            loadFields(typeList.getSelectedValue());
          }
        }
      });
  }

  private void loadFields(String metadataType) {
    fieldList.clear();
    fieldsById.clear();
    currentField = null;
    valuePanel.clear();
    noFieldsLabel.setVisible(false);
    startButton.setEnabled(false);
    if (metadataType == null || metadataType.isEmpty()) {
      return;
    }

    Services services = new Services("Retrieve metadata template fields", "get");
    services
      .aipResource(
        s -> s.retrieveConfiguredMetadataFields(metadataType, LocaleInfo.getCurrentLocale().getLocaleName()))
      .whenComplete((supportedMetadata, throwable) -> {
        if (throwable != null) {
          AsyncCallbackUtils.defaultFailureTreatment(throwable);
        } else {
          for (MetadataValue mv : supportedMetadata.getValue()) {
            fieldsById.put(mv.getId(), mv);
            fieldList.addItem(FormUtilities.getFieldLabel(mv), mv.getId());
          }
          if (fieldList.getItemCount() > 0) {
            fieldList.setSelectedIndex(0);
            selectField(fieldList.getSelectedValue());
          } else {
            noFieldsLabel.setVisible(true);
          }
        }
      });
  }

  private void selectField(String fieldName) {
    valuePanel.clear();
    currentField = fieldName != null ? fieldsById.get(fieldName) : null;
    startButton.setEnabled(currentField != null);
    if (currentField == null) {
      return;
    }

    // Nollställ mallens defaultvärde och sätt widgetens etikett till
    // "Nytt värde" i stället för fältets namn (fältet visas redan i
    // fältrullgardinen ovanför)
    currentField.set("value", "");
    currentField.set("label",
      "{\"" + LocaleInfo.getCurrentLocale().getLocaleName() + "\":\"" + messages.bulkEditMetadataNewValueLabel()
        + "\"}");
    currentField.set("mandatory", "true");
    currentField.set("description", null);

    FormUtilities.create(valuePanel, Collections.singleton(currentField), true);
  }

  private void onStart() {
    if (jobInProgress || currentField == null) {
      return;
    }
    String newValue = currentField.get("value");
    if (newValue == null || newValue.trim().isEmpty()) {
      Toast.showError(messages.bulkEditMetadataDialogTitle(), messages.bulkEditMetadataMissingValue());
      return;
    }

    jobInProgress = true;
    startButton.setEnabled(false);

    BulkUpdateDescriptiveMetadataRequest request = new BulkUpdateDescriptiveMetadataRequest();
    request.setItems(SelectedItemsUtils.convertToRESTRequest(selected));
    request.setMetadataType(typeList.getSelectedValue());
    request.setFieldName(currentField.getId());
    request.setNewValue(newValue.trim());
    request.setMode(modeList.getSelectedValue());
    String expected = expectedValueBox.getValue();
    if (MODE_OVERWRITE.equals(modeList.getSelectedValue()) && expected != null && !expected.trim().isEmpty()) {
      request.setExpectedValue(expected.trim());
    }

    Services services = new Services("Bulk update descriptive metadata", "update");
    services.aipResource(s -> s.bulkUpdateDescriptiveMetadata(request)).whenComplete((job, throwable) -> {
      jobInProgress = false;
      startButton.setEnabled(true);
      if (throwable != null) {
        AsyncCallbackUtils.defaultFailureTreatment(throwable);
      } else {
        dialogBox.hide();
        Toast.showInfo(messages.runningInBackgroundTitle(), messages.runningInBackgroundDescription());
        Dialogs.showJobRedirectDialog(messages.jobCreatedMessage(), new AsyncCallback<Void>() {
          @Override
          public void onFailure(Throwable caught) {
            // stanna kvar i katalogen
          }

          @Override
          public void onSuccess(Void nothing) {
            HistoryUtils.newHistory(ShowJob.RESOLVER, job.getId());
          }
        });
      }
    });
  }

  public void show() {
    dialogBox.center();
    dialogBox.show();
  }
}

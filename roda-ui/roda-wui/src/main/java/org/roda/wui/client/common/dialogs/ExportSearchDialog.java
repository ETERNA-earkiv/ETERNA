/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.client.common.dialogs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.roda.core.data.v2.generics.select.SelectedItemsNoneRequest;
import org.roda.core.data.v2.index.filter.Filter;
import org.roda.core.data.v2.jobs.CreateJobRequest;
import org.roda.wui.client.common.utils.AsyncCallbackUtils;
import org.roda.wui.client.services.Services;
import org.roda.wui.common.client.tools.ConfigurationManager;
import org.roda.wui.common.client.widgets.Toast;

import com.github.nmorel.gwtjackson.client.ObjectMapper;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

import config.i18n.client.ClientMessages;

/**
 * Dialog that lets users select fields and start a search export job.
 *
 * @author ETERNA Development Team
 */
public class ExportSearchDialog {

  interface FilterMapper extends ObjectMapper<Filter> {
  }

  private static final ClientMessages messages = GWT.create(ClientMessages.class);

  private static final String PLUGIN_ID = "org.roda.core.plugins.base.preservation.SearchExportPlugin";
  private static final String PARAM_FILTER = "exportFilter";
  private static final String PARAM_FIELDS = "exportFields";
  private static final String PARAM_FILENAME = "exportFilename";
  private static final String PARAM_CLASS = "exportClass";

  private final DialogBox dialogBox;
  private final List<CheckBox> fieldCheckboxes = new ArrayList<>();
  private final Map<CheckBox, String> checkboxFieldMap = new HashMap<>();
  private Button startButton;
  private boolean exportInProgress = false;

  public ExportSearchDialog(Filter filter, long totalCount, String exportFilename, String configKeyPrefix,
    String exportClass) {
    dialogBox = new DialogBox(false, true);
    dialogBox.setText(messages.exportSearchDialogTitle());
    dialogBox.setGlassEnabled(true);
    dialogBox.setAnimationEnabled(false);
    dialogBox.addStyleName("wui-dialog-information");

    FlowPanel content = new FlowPanel();
    content.addStyleName("export-search-dialog");

    // Hit count label
    Label hitCountLabel = new Label(messages.exportSearchDialogHitCount(totalCount));
    hitCountLabel.addStyleName("export-search-dialog-hitcount");
    content.add(hitCountLabel);

    // Fields section label
    Label fieldsLabel = new Label(messages.exportSearchDialogFieldsLabel());
    fieldsLabel.addStyleName("export-search-dialog-fields-label");
    content.add(fieldsLabel);

    // Fields checkboxes
    FlowPanel fieldsPanel = new FlowPanel();
    fieldsPanel.addStyleName("export-search-dialog-fields");

    String defaultCheckedConfig = ConfigurationManager.getString(configKeyPrefix + ".defaultCheckedFields");
    List<String> defaultCheckedFields = (defaultCheckedConfig != null && !defaultCheckedConfig.trim().isEmpty())
      ? Arrays.asList(defaultCheckedConfig.split(","))
      : Arrays.asList("uuid");

    String fieldsConfig = ConfigurationManager.getString(configKeyPrefix + ".fields");
    if (fieldsConfig != null && !fieldsConfig.trim().isEmpty()) {
      String[] fields = fieldsConfig.split(",");
      for (String rawField : fields) {
        String field = rawField.trim();
        if (field.isEmpty()) {
          continue;
        }
        String labelText = ConfigurationManager.getStringWithDefault(field,
          configKeyPrefix + ".fields." + field + ".label");
        CheckBox cb = new CheckBox(labelText);
        cb.setValue(defaultCheckedFields.contains(field));
        cb.addValueChangeHandler(event -> updateStartButtonState());
        fieldCheckboxes.add(cb);
        checkboxFieldMap.put(cb, field);
        fieldsPanel.add(cb);
      }
    }
    content.add(fieldsPanel);

    // Buttons panel
    FlowPanel buttonsPanel = new FlowPanel();
    buttonsPanel.addStyleName("export-search-dialog-buttons");

    startButton = new Button(messages.exportSearchDialogStartButton());
    startButton.addStyleName("btn btn-primary");
    startButton.addClickHandler(event -> onStartExport(filter, exportFilename, exportClass));
    updateStartButtonState();
    buttonsPanel.add(startButton);

    Button cancelButton = new Button(messages.dialogCancel());
    cancelButton.addStyleName("btn btn-default");
    cancelButton.addClickHandler(event -> dialogBox.hide());
    buttonsPanel.add(cancelButton);

    content.add(buttonsPanel);
    dialogBox.setWidget(content);
  }

  private void updateStartButtonState() {
    boolean anySelected = false;
    for (CheckBox cb : fieldCheckboxes) {
      if (Boolean.TRUE.equals(cb.getValue())) {
        anySelected = true;
        break;
      }
    }
    startButton.setEnabled(anySelected);
  }

  private void onStartExport(Filter filter, String exportFilename, String exportClass) {
    if (exportInProgress) {
      return;
    }
    exportInProgress = true;
    startButton.setEnabled(false);

    // Collect selected fields as comma-separated string
    List<String> selectedFields = new ArrayList<>();
    for (CheckBox cb : fieldCheckboxes) {
      if (Boolean.TRUE.equals(cb.getValue())) {
        selectedFields.add(checkboxFieldMap.get(cb));
      }
    }
    String fieldsParam = String.join(",", selectedFields);

    // Serialize Filter to JSON via gwt-jackson
    FilterMapper mapper = GWT.create(FilterMapper.class);
    String filterJson = mapper.write(filter);

    // Build CreateJobRequest
    Map<String, String> pluginParameters = new HashMap<>();
    pluginParameters.put(PARAM_FILTER, filterJson);
    pluginParameters.put(PARAM_FIELDS, fieldsParam);
    pluginParameters.put(PARAM_FILENAME, exportFilename);
    pluginParameters.put(PARAM_CLASS, exportClass);

    CreateJobRequest jobRequest = new CreateJobRequest();
    jobRequest.setName(messages.exportSearchDialogTitle());
    jobRequest.setPlugin(PLUGIN_ID);
    jobRequest.setPluginParameters(pluginParameters);
    jobRequest.setSourceObjects(new SelectedItemsNoneRequest());
    jobRequest.setSourceObjectsClass("org.roda.core.data.v2.Void");
    jobRequest.setPriority("MEDIUM");
    jobRequest.setParallelism("NORMAL");

    Services services = new Services("Create export search job", "create");
    services.jobsResource(s -> s.createJob(jobRequest)).whenComplete((job, throwable) -> {
      exportInProgress = false;
      startButton.setEnabled(true);
      if (throwable != null) {
        AsyncCallbackUtils.defaultFailureTreatment(throwable);
      } else {
        Toast.showInfo(messages.exportListTitle(), messages.exportSearchJobStarted());
        dialogBox.hide();
      }
    });
  }

  public void show() {
    dialogBox.center();
    dialogBox.show();
  }
}

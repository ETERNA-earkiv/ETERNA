/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.client.process;

import org.roda.wui.client.services.Services;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;

import config.i18n.client.ClientMessages;

/**
 * A user-friendly dialog for selecting a recurring job schedule without
 * requiring knowledge of cron syntax.
 */
public class ScheduleJobDialog extends DialogBox {

  public interface ScheduleCallback {
    void onSchedule(String cronExpression);
  }

  private static final ClientMessages messages = GWT.create(ClientMessages.class);

  private static final int FREQ_HOURLY = 0;
  private static final int FREQ_DAILY = 1;
  private static final int FREQ_WEEKLY = 2;
  private static final int FREQ_MONTHLY = 3;

  private final ListBox frequencyList = new ListBox();
  private final ListBox hourList = new ListBox();
  private final ListBox minuteList = new ListBox();
  private final ListBox dayOfWeekList = new ListBox();
  private final ListBox dayOfMonthList = new ListBox();
  private final Label previewLabel = new Label();
  private final FlowPanel timeRow = new FlowPanel();
  private final FlowPanel dowRow = new FlowPanel();
  private final FlowPanel domRow = new FlowPanel();

  private final ScheduleCallback callback;

  public ScheduleJobDialog(ScheduleCallback callback) {
    super(false, true);
    this.callback = callback;

    setText(messages.createJobScheduleTitle());
    setGlassEnabled(true);
    setAnimationEnabled(false);

    FlowPanel root = new FlowPanel();
    root.addStyleName("wui-dialog-layout");

    // --- Frequency row ---
    FlowPanel freqRow = new FlowPanel();
    freqRow.addStyleName("form-row");
    Label freqLabel = new Label(messages.scheduleDialogFrequencyLabel());
    freqLabel.addStyleName("form-label");
    frequencyList.addStyleName("form-listbox");
    frequencyList.addItem(messages.scheduleDialogFrequencyHourly());
    frequencyList.addItem(messages.scheduleDialogFrequencyDaily());
    frequencyList.addItem(messages.scheduleDialogFrequencyWeekly());
    frequencyList.addItem(messages.scheduleDialogFrequencyMonthly());
    frequencyList.setSelectedIndex(FREQ_DAILY);
    freqRow.add(freqLabel);
    freqRow.add(frequencyList);
    root.add(freqRow);

    // --- Time row (hour + minute) ---
    timeRow.addStyleName("form-row");
    Label timeLabel = new Label(messages.scheduleDialogTimeLabel());
    timeLabel.addStyleName("form-label");
    hourList.addStyleName("form-listbox schedule-time-select");
    minuteList.addStyleName("form-listbox schedule-time-select");
    for (int h = 0; h < 24; h++) {
      hourList.addItem(String.format("%02d", h), String.valueOf(h));
    }
    for (int m = 0; m < 60; m += 5) {
      minuteList.addItem(String.format("%02d", m), String.valueOf(m));
    }
    hourList.setSelectedIndex(2); // default 02:00
    timeRow.add(timeLabel);
    timeRow.add(hourList);
    Label colon = new Label(":");
    colon.addStyleName("schedule-time-colon");
    timeRow.add(colon);
    timeRow.add(minuteList);
    root.add(timeRow);

    // --- Day of week row ---
    dowRow.addStyleName("form-row");
    Label dowLabel = new Label(messages.scheduleDialogDayOfWeekLabel());
    dowLabel.addStyleName("form-label");
    dayOfWeekList.addStyleName("form-listbox");
    String[] weekdays = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
    int[] weekdayCron = {1, 2, 3, 4, 5, 6, 0}; // cron: 0=Sun, 1=Mon, ..., 6=Sat
    for (int i = 0; i < weekdays.length; i++) {
      dayOfWeekList.addItem(weekdays[i], String.valueOf(weekdayCron[i]));
    }
    dowRow.add(dowLabel);
    dowRow.add(dayOfWeekList);
    dowRow.setVisible(false);
    root.add(dowRow);

    // --- Day of month row ---
    domRow.addStyleName("form-row");
    Label domLabel = new Label(messages.scheduleDialogDayOfMonthLabel());
    domLabel.addStyleName("form-label");
    dayOfMonthList.addStyleName("form-listbox");
    for (int d = 1; d <= 28; d++) {
      dayOfMonthList.addItem(String.valueOf(d), String.valueOf(d));
    }
    domRow.add(domLabel);
    domRow.add(dayOfMonthList);
    domRow.setVisible(false);
    root.add(domRow);

    // --- Preview row ---
    FlowPanel previewRow = new FlowPanel();
    previewRow.addStyleName("form-row schedule-preview-row");
    Label previewTitleLabel = new Label(messages.scheduleDialogPreviewLabel() + ":");
    previewTitleLabel.addStyleName("form-label");
    previewLabel.addStyleName("value schedule-preview-text");
    previewRow.add(previewTitleLabel);
    previewRow.add(previewLabel);
    root.add(previewRow);

    // --- Footer buttons ---
    FlowPanel footer = new FlowPanel();
    footer.addStyleName("wui-dialog-footer");
    Button cancelButton = new Button(messages.cancelButton());
    cancelButton.addStyleName("btn btn-default btn-times-circle");
    Button confirmButton = new Button(messages.scheduleDialogConfirmButton());
    confirmButton.addStyleName("btn btn-play");
    footer.add(cancelButton);
    footer.add(confirmButton);
    root.add(footer);

    setWidget(root);

    // --- Event wiring ---
    ChangeHandler updateHandler = new ChangeHandler() {
      @Override
      public void onChange(ChangeEvent event) {
        updateVisibility();
        refreshPreview();
      }
    };

    frequencyList.addChangeHandler(updateHandler);
    hourList.addChangeHandler(updateHandler);
    minuteList.addChangeHandler(updateHandler);
    dayOfWeekList.addChangeHandler(updateHandler);
    dayOfMonthList.addChangeHandler(updateHandler);

    cancelButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        hide();
        callback.onSchedule(null);
      }
    });

    confirmButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        hide();
        callback.onSchedule(buildCronExpression());
      }
    });

    // Initial state
    updateVisibility();
    refreshPreview();
  }

  private void updateVisibility() {
    int freq = frequencyList.getSelectedIndex();
    timeRow.setVisible(freq != FREQ_HOURLY);
    dowRow.setVisible(freq == FREQ_WEEKLY);
    domRow.setVisible(freq == FREQ_MONTHLY);
  }

  private String buildCronExpression() {
    int freq = frequencyList.getSelectedIndex();
    String hour = hourList.getSelectedValue();
    String minute = minuteList.getSelectedValue();
    switch (freq) {
      case FREQ_HOURLY:
        return "0 * * * *";
      case FREQ_DAILY:
        return minute + " " + hour + " * * *";
      case FREQ_WEEKLY:
        return minute + " " + hour + " * * " + dayOfWeekList.getSelectedValue();
      case FREQ_MONTHLY:
        return minute + " " + hour + " " + dayOfMonthList.getSelectedValue() + " * *";
      default:
        return "0 * * * *";
    }
  }

  private void refreshPreview() {
    String cron = buildCronExpression();
    Services services = new Services("Describe cron expression", "retrieve");
    services
      .configurationsResource(
        s -> s.describeCronExpression(cron, com.google.gwt.i18n.client.LocaleInfo.getCurrentLocale().getLocaleName()))
      .whenComplete((response, error) -> {
        if (error == null && response != null && response.getValue() != null) {
          previewLabel.setText(response.getValue());
        } else {
          previewLabel.setText(cron);
        }
      });
  }
}

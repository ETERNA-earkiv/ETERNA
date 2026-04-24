/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.client.process;

import java.util.Date;

import org.roda.wui.client.services.Services;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;

import config.i18n.client.ClientMessages;

/**
 * A user-friendly dialog for selecting a job schedule without requiring
 * knowledge of cron syntax. Supports one-shot and recurring schedules.
 */
public class ScheduleJobDialog extends DialogBox {

  public interface ScheduleCallback {
    void onSchedule(String scheduleExpression);
  }

  /** Prefix for one-shot schedule expressions: {@code @once:YYYY-MM-DDTHH:MM} */
  public static final String ONCE_PREFIX = "@once:";

  private static final ClientMessages messages = GWT.create(ClientMessages.class);

  private static final int FREQ_ONCE = 0;
  private static final int FREQ_HOURLY = 1;
  private static final int FREQ_DAILY = 2;
  private static final int FREQ_WEEKLY = 3;
  private static final int FREQ_MONTHLY = 4;

  private final ListBox frequencyList = new ListBox();

  // Date selectors (shown for Once)
  private final ListBox yearList = new ListBox();
  private final ListBox monthList = new ListBox();
  private final ListBox onceDayList = new ListBox();
  private final FlowPanel dateRow = new FlowPanel();

  // Time selectors (shown for everything except Hourly)
  private final ListBox hourList = new ListBox();
  private final ListBox minuteList = new ListBox();
  private final FlowPanel timeRow = new FlowPanel();

  // Recurring-only rows
  private final ListBox dayOfWeekList = new ListBox();
  private final ListBox dayOfMonthList = new ListBox();
  private final FlowPanel dowRow = new FlowPanel();
  private final FlowPanel domRow = new FlowPanel();

  private final Label previewLabel = new Label();
  private final ScheduleCallback callback;
  private int previewSequence = 0;

  @SuppressWarnings("deprecation")
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
    frequencyList.addItem(messages.scheduleDialogFrequencyOnce());
    frequencyList.addItem(messages.scheduleDialogFrequencyHourly());
    frequencyList.addItem(messages.scheduleDialogFrequencyDaily());
    frequencyList.addItem(messages.scheduleDialogFrequencyWeekly());
    frequencyList.addItem(messages.scheduleDialogFrequencyMonthly());
    frequencyList.setSelectedIndex(FREQ_DAILY);
    freqRow.add(freqLabel);
    freqRow.add(frequencyList);
    root.add(freqRow);

    // --- Date row (Once only) ---
    Date now = new Date();
    int currentYear = 1900 + now.getYear();
    int currentMonth = 1 + now.getMonth();
    int currentDay = now.getDate();

    dateRow.addStyleName("form-row");
    Label dateLabel = new Label(messages.scheduleDialogDateLabel());
    dateLabel.addStyleName("form-label");

    FlowPanel dateInputsGroup = buildInlineGroup();
    for (int y = currentYear; y <= currentYear + 5; y++) {
      yearList.addItem(String.valueOf(y), String.valueOf(y));
    }
    DateTimeFormat monthFmt = DateTimeFormat.getFormat("MMM");
    Date monthRef = new Date(2000 - 1900, 0, 1, 12, 0, 0);
    for (int m = 1; m <= 12; m++) {
      monthRef.setMonth(m - 1);
      monthList.addItem(monthFmt.format(monthRef), String.valueOf(m));
    }
    for (int d = 1; d <= 31; d++) {
      onceDayList.addItem(String.valueOf(d), String.valueOf(d));
    }
    monthList.setSelectedIndex(currentMonth - 1);
    onceDayList.setSelectedIndex(currentDay - 1);
    yearList.getElement().getStyle().setWidth(70, Style.Unit.PX);
    monthList.getElement().getStyle().setWidth(60, Style.Unit.PX);
    onceDayList.getElement().getStyle().setWidth(55, Style.Unit.PX);

    dateInputsGroup.add(yearList);
    Label dashM = new Label("-");
    dashM.addStyleName("schedule-time-colon");
    dateInputsGroup.add(dashM);
    dateInputsGroup.add(monthList);
    Label dashD = new Label("-");
    dashD.addStyleName("schedule-time-colon");
    dateInputsGroup.add(dashD);
    dateInputsGroup.add(onceDayList);

    dateRow.add(dateLabel);
    dateRow.add(dateInputsGroup);
    root.add(dateRow);

    // --- Time row (hour : minute) ---
    timeRow.addStyleName("form-row");
    Label timeLabel = new Label(messages.scheduleDialogTimeLabel());
    timeLabel.addStyleName("form-label");

    FlowPanel timeInputsGroup = buildInlineGroup();
    for (int h = 0; h < 24; h++) {
      hourList.addItem(pad2(h), String.valueOf(h));
    }
    for (int m = 0; m < 60; m += 5) {
      minuteList.addItem(pad2(m), String.valueOf(m));
    }
    hourList.setSelectedIndex(8); // default 08:00
    hourList.getElement().getStyle().setWidth(65, Style.Unit.PX);
    minuteList.getElement().getStyle().setWidth(65, Style.Unit.PX);

    timeInputsGroup.add(hourList);
    Label colon = new Label(":");
    colon.addStyleName("schedule-time-colon");
    timeInputsGroup.add(colon);
    timeInputsGroup.add(minuteList);

    timeRow.add(timeLabel);
    timeRow.add(timeInputsGroup);
    root.add(timeRow);

    // --- Day of week row ---
    dowRow.addStyleName("form-row");
    Label dowLabel = new Label(messages.scheduleDialogDayOfWeekLabel());
    dowLabel.addStyleName("form-label");
    dayOfWeekList.addStyleName("form-listbox");
    DateTimeFormat dowFmt = DateTimeFormat.getFormat("EEEE");
    int[] weekdayCron = {1, 2, 3, 4, 5, 6, 0};
    // Jan 3, 2000 is a Monday; iterate Mon–Sun using epoch offsets
    Date monday = new Date(2000 - 1900, 0, 3, 12, 0, 0);
    for (int i = 0; i < 7; i++) {
      Date weekday = new Date(monday.getTime() + (long) i * 86400000L);
      dayOfWeekList.addItem(dowFmt.format(weekday), String.valueOf(weekdayCron[i]));
    }
    dowRow.add(dowLabel);
    dowRow.add(dayOfWeekList);
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

    // --- Footer ---
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
    yearList.addChangeHandler(updateHandler);
    monthList.addChangeHandler(updateHandler);
    onceDayList.addChangeHandler(updateHandler);

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
        if (frequencyList.getSelectedIndex() == FREQ_ONCE) {
          Date selected = buildOnceDate();
          if (selected == null || !selected.after(new Date())) {
            previewLabel.setText(messages.scheduleDialogPastTimeError());
            return;
          }
        }
        hide();
        callback.onSchedule(buildScheduleExpression());
      }
    });

    updateVisibility();
    refreshPreview();
  }

  /** Creates an inline-flex container for grouping inputs on one line. */
  private FlowPanel buildInlineGroup() {
    FlowPanel panel = new FlowPanel();
    panel.getElement().getStyle().setDisplay(Style.Display.INLINE_FLEX);
    panel.getElement().getStyle().setProperty("alignItems", "center");
    panel.getElement().getStyle().setProperty("gap", "4px");
    return panel;
  }

  private void updateVisibility() {
    int freq = frequencyList.getSelectedIndex();
    dateRow.setVisible(freq == FREQ_ONCE);
    timeRow.setVisible(freq != FREQ_HOURLY);
    dowRow.setVisible(freq == FREQ_WEEKLY);
    domRow.setVisible(freq == FREQ_MONTHLY);
  }

  @SuppressWarnings("deprecation")
  private Date buildOnceDate() {
    int year = Integer.parseInt(yearList.getSelectedValue());
    int month = Integer.parseInt(monthList.getSelectedValue());
    int day = Integer.parseInt(onceDayList.getSelectedValue());
    int hour = Integer.parseInt(hourList.getSelectedValue());
    int minute = Integer.parseInt(minuteList.getSelectedValue());
    Date d = new Date(year - 1900, month - 1, day, hour, minute, 0);
    // JS Date normalizes out-of-range values (e.g. Feb 31 → Mar 3).
    // Return null to signal an invalid day selection.
    if (d.getYear() + 1900 != year || d.getMonth() + 1 != month || d.getDate() != day) {
      return null;
    }
    return d;
  }

  private String buildScheduleExpression() {
    int freq = frequencyList.getSelectedIndex();
    String hour = hourList.getSelectedValue();
    String minute = minuteList.getSelectedValue();
    switch (freq) {
      case FREQ_ONCE:
        Date onceDate = buildOnceDate();
        if (onceDate == null) {
          return "";
        }
        return ONCE_PREFIX + onceDate.getTime();
      case FREQ_HOURLY:
        return "0 0 * * * *";
      case FREQ_DAILY:
        return "0 " + minute + " " + hour + " * * *";
      case FREQ_WEEKLY:
        return "0 " + minute + " " + hour + " * * " + dayOfWeekList.getSelectedValue();
      case FREQ_MONTHLY:
        return "0 " + minute + " " + hour + " " + dayOfMonthList.getSelectedValue() + " * *";
      default:
        return "0 0 * * * *";
    }
  }

  private void refreshPreview() {
    int freq = frequencyList.getSelectedIndex();
    if (freq == FREQ_ONCE) {
      String year = yearList.getSelectedValue();
      String month = pad2(Integer.parseInt(monthList.getSelectedValue()));
      String day = pad2(Integer.parseInt(onceDayList.getSelectedValue()));
      String hh = pad2(Integer.parseInt(hourList.getSelectedValue()));
      String mm = pad2(Integer.parseInt(minuteList.getSelectedValue()));
      previewLabel.setText(messages.scheduleDialogOnceSummary(year + "-" + month + "-" + day, hh + ":" + mm));
      return;
    }
    String cron = buildScheduleExpression();
    final int seq = ++previewSequence;
    Services services = new Services("Describe cron expression", "retrieve");
    services
      .configurationsResource(
        s -> s.describeCronExpression(cron, com.google.gwt.i18n.client.LocaleInfo.getCurrentLocale().getLocaleName()))
      .whenComplete((response, error) -> {
        if (seq != previewSequence) {
          return; // a newer request is already in flight; discard this stale response
        }
        if (error == null && response != null && response.getValue() != null) {
          previewLabel.setText(response.getValue());
        } else {
          previewLabel.setText(cron);
        }
      });
  }

  private static String pad2(int n) {
    return (n < 10 ? "0" : "") + n;
  }
}

/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.client.common.lists.utils;

import java.util.Date;

import org.roda.wui.common.client.tools.Humanize;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

/**
 * A GWT cell that renders a {@link Date} using {@link Humanize}, which
 * respects the {@code ui.dateTime.format.UTC} configuration property.
 * Use this instead of {@code DateCell} to ensure timezone consistency.
 */
public class HumanizeDateCell extends AbstractCell<Date> {

  private final boolean dateOnly;

  /**
   * Creates a cell that renders date and time (formatDateTime).
   */
  public HumanizeDateCell() {
    this(false);
  }

  /**
   * @param dateOnly {@code true} to render date only (formatDate),
   *                 {@code false} to render date and time (formatDateTime)
   */
  public HumanizeDateCell(boolean dateOnly) {
    this.dateOnly = dateOnly;
  }

  @Override
  public void render(Context context, Date value, SafeHtmlBuilder sb) {
    if (value != null) {
      String formatted = dateOnly
        ? Humanize.formatDate(value)
        : Humanize.formatDateTime(value);
      sb.appendEscaped(formatted);
    }
  }
}

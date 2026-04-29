/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.client.common.lists.utils;

import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.user.cellview.client.AbstractPager;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.view.client.HasRows;


/**
 * A simple pager that controls the page size.
 *
 * NOTE: GWT 2.7 PageSizePager doesn't have the ShowMore and ShowLess button
 * localized.
 */
public class RodaPageSizePager extends AbstractPager {

  /**
   * The increment by which to grow or shrink the page size.
   */
  private final int increment;

  /**
   * The main layout widget.
   */
  private final FlexTable layout = new FlexTable();


  /**
   * Construct a PageSizePager with a given increment.
   *
   * @param increment
   *          the amount by which to increase the page size
   */
  @UiConstructor
  public RodaPageSizePager(final int increment) {
    this.increment = increment;
    initWidget(layout);
    layout.setCellPadding(0);
    layout.setCellSpacing(0);

    layout.setText(0, 1, " | ");

    // Hide the buttons by default.
    setDisplay(null);
  }

  @Override
  public void setDisplay(HasRows display) {
    super.setDisplay(display);
  }

  @Override
  public void setPageSize(int pageSize) {
    super.setPageSize(pageSize);
  }

  @Override
  protected void onRangeOrRowCountChanged() {
    // Assumes a page start index of 0.
    HasRows display = getDisplay();
    int pageSize = display.getVisibleRange().getLength();
    boolean hasLess = pageSize > increment;
    boolean hasMore = !display.isRowCountExact() || pageSize < display.getRowCount();
    layout.setText(0, 1, (hasLess && hasMore) ? " | " : "");
  }

}
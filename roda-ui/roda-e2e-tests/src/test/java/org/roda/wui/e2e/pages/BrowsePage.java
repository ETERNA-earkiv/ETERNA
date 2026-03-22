/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;

public class BrowsePage {

  private final Page page;
  private final String baseUrl;

  public BrowsePage(Page page, String baseUrl) {
    this.page = page;
    this.baseUrl = baseUrl;
  }

  public void navigate() {
    page.navigate(baseUrl + "/#browse");
    page.waitForLoadState(LoadState.NETWORKIDLE);
  }

  public boolean isLoaded() {
    return page.locator(".browse").count() > 0 || page.locator(".contentFlowPanel").count() > 0;
  }

  public boolean hasSearchBar() {
    return page.locator("input[type=text]").count() > 0;
  }

  public int getAIPRowCount() {
    return page.locator("table tr").count();
  }

  public void clickFirstAIP() {
    Locator rows = page.locator("table tr").filter(new Locator.FilterOptions().setHasText(""));
    if (rows.count() > 1) {
      rows.nth(1).click();
      page.waitForLoadState(LoadState.NETWORKIDLE);
    }
  }
}

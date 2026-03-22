/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.e2e.pages;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;

public class SearchPage {

  private final Page page;
  private final String baseUrl;

  public SearchPage(Page page, String baseUrl) {
    this.page = page;
    this.baseUrl = baseUrl;
  }

  public void navigate() {
    page.navigate(baseUrl + "/#search");
    page.waitForLoadState(LoadState.NETWORKIDLE);
  }

  public boolean isLoaded() {
    return page.locator(".search").count() > 0 || page.locator("input[type=text]").count() > 0;
  }

  public void search(String term) {
    page.locator("input[type=text]").first().fill(term);
    page.keyboard().press("Enter");
    page.waitForLoadState(LoadState.NETWORKIDLE);
  }

  public boolean hasResults() {
    // Results container or zero-results message is visible
    return page.locator(".searchResults, .emptyPanel, table").count() > 0;
  }
}

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

public class ProcessPage {

  private final Page page;
  private final String baseUrl;

  public ProcessPage(Page page, String baseUrl) {
    this.page = page;
    this.baseUrl = baseUrl;
  }

  public void navigate() {
    page.navigate(baseUrl + "/#process");
    page.waitForLoadState(LoadState.NETWORKIDLE);
  }

  public boolean isLoaded() {
    return page.locator(".contentFlowPanel, table, .emptyPanel").count() > 0;
  }
}

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

public class PlanningPage {

  private final Page page;
  private final String baseUrl;

  public PlanningPage(Page page, String baseUrl) {
    this.page = page;
    this.baseUrl = baseUrl;
  }

  public void navigate() {
    // /#planning renders an empty HTML placeholder; navigate directly to
    // /#planning/riskregister which renders the RiskRegister widget.
    // (RiskRegister.RESOLVER.getHistoryToken() == "riskregister")
    page.navigate(baseUrl + "/#planning/riskregister");
    page.waitForLoadState(LoadState.NETWORKIDLE);
  }

  public boolean isLoaded() {
    try {
      page.locator(".wui-risk-register").waitFor(new Locator.WaitForOptions().setTimeout(10000));
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public void navigateToRisks() {
    page.navigate(baseUrl + "/#planning/riskregister");
    page.waitForLoadState(LoadState.NETWORKIDLE);
  }

  public boolean isRiskRegisterLoaded() {
    // Use the specific widget class rather than generic table/emptyPanel to
    // avoid false positives from the Welcome page.
    try {
      page.locator(".wui-risk-register").waitFor(new Locator.WaitForOptions().setTimeout(10000));
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public void clickNewRisk() {
    page.locator(".btn-plus, button:has(.fa-plus)").first().click();
    page.waitForLoadState(LoadState.NETWORKIDLE);
  }

  public boolean isNewRiskFormVisible() {
    return page.locator("input[type=text]").count() > 0;
  }
}

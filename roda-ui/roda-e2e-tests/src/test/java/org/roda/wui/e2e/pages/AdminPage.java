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

public class AdminPage {

  private final Page page;
  private final String baseUrl;

  public AdminPage(Page page, String baseUrl) {
    this.page = page;
    this.baseUrl = baseUrl;
  }

  public void navigate() {
    // /#administration renders an empty HTML placeholder; navigate directly to the
    // user management sub-page which renders the MemberManagement widget.
    page.navigate(baseUrl + "/#administration/user");
    page.waitForLoadState(LoadState.NETWORKIDLE);
  }

  public boolean isLoaded() {
    try {
      page.locator(".wui-management-user").waitFor(new Locator.WaitForOptions().setTimeout(10000));
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public void navigateToUsers() {
    page.navigate(baseUrl + "/#administration/user");
    page.waitForLoadState(LoadState.NETWORKIDLE);
  }

  public boolean isAdminUserVisible() {
    return page.locator("table").locator("text=admin").count() > 0;
  }

  public void clickNewUser() {
    page.locator(".btn-plus, button:has(.fa-plus)").first().click();
    page.waitForLoadState(LoadState.NETWORKIDLE);
  }

  public boolean isNewUserFormVisible() {
    return page.locator("input[type=text]").count() > 0;
  }
}

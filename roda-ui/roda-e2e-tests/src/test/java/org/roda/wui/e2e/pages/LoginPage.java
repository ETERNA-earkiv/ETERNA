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
import com.microsoft.playwright.options.WaitForSelectorState;

public class LoginPage {

  private final Page page;
  private final String baseUrl;

  public LoginPage(Page page, String baseUrl) {
    this.page = page;
    this.baseUrl = baseUrl;
  }

  public void navigate() {
    page.navigate(baseUrl + "/#login");
    page.waitForLoadState(LoadState.NETWORKIDLE);
  }

  public void login(String username, String password) {
    page.locator(".fieldTextBox").first().fill(username);
    page.locator("input[type=password]").fill(password);
    page.locator(".login-button").first().click();
    // On a successful login GWT redirects away from #login. Wait up to 3 s for
    // that URL change. If it doesn't happen (wrong password) we fall through to
    // a plain NETWORKIDLE wait — the error message will then be visible.
    try {
      page.waitForURL(url -> !url.contains("/#login"), new Page.WaitForURLOptions().setTimeout(3000));
    } catch (Exception ignored) {
      // login failed — URL stayed on #login
    }
    page.waitForLoadState(LoadState.NETWORKIDLE);
  }

  public boolean isLoggedIn() {
    // The Login widget only exists in the DOM on the /#login page.
    // Navigate there first. GWT's Login.resolve() calls getAuthenticatedUser()
    // asynchronously, so we must waitFor the loggedInPanel to become VISIBLE
    // rather than checking isVisible() immediately.
    navigate();
    try {
      page.locator(".loginPanel").nth(1)
        .waitFor(new Locator.WaitForOptions().setTimeout(5000).setState(WaitForSelectorState.VISIBLE));
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public boolean isLoginFormVisible() {
    // Ensure we are on the login page; after logout GWT may redirect to #welcome.
    navigate();
    try {
      page.locator(".loginPanel").first()
        .waitFor(new Locator.WaitForOptions().setTimeout(5000).setState(WaitForSelectorState.VISIBLE));
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public void logout() {
    navigate();
    page.waitForLoadState(LoadState.NETWORKIDLE);
    // Click logout link (only visible when logged in panel is shown)
    Locator logoutBtn = page.locator(".login-link").filter(new Locator.FilterOptions().setHasText("logout"));
    if (logoutBtn.count() > 0) {
      logoutBtn.first().click();
      page.waitForLoadState(LoadState.NETWORKIDLE);
    }
  }
}

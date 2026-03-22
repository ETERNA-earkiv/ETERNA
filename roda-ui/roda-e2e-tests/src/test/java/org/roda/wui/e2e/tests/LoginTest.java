/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.e2e.tests;

import org.roda.wui.e2e.base.BaseTest;
import org.roda.wui.e2e.pages.LoginPage;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = {"e2e"})
public class LoginTest extends BaseTest {

  @Test
  public void loginAndLogout() {
    LoginPage loginPage = new LoginPage(page, BASE_URL);
    loginPage.navigate();
    Assert.assertTrue(loginPage.isLoginFormVisible(), "Login form should be visible");

    loginPage.login(ADMIN_USER, ADMIN_PASSWORD);
    Assert.assertTrue(loginPage.isLoggedIn(), "Logged-in panel should be visible after login");

    loginPage.logout();
    Assert.assertTrue(loginPage.isLoginFormVisible(), "Login form should be visible after logout");
  }

  @Test
  public void loginWithWrongPasswordShowsError() {
    LoginPage loginPage = new LoginPage(page, BASE_URL);
    loginPage.navigate();
    loginPage.login(ADMIN_USER, "wrongpassword");
    // Should stay on login page
    Assert.assertTrue(loginPage.isLoginFormVisible(), "Login form should still be visible after failed login");
    Assert.assertFalse(loginPage.isLoggedIn(), "Should not be logged in with wrong password");
  }
}

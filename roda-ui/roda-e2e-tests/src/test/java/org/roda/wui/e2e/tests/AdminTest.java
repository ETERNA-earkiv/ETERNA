/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.e2e.tests;

import org.roda.wui.e2e.base.BaseTest;
import org.roda.wui.e2e.pages.AdminPage;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = {"e2e"})
public class AdminTest extends BaseTest {

  @Test
  public void administrationPageLoads() {
    loginAsAdmin();
    AdminPage adminPage = new AdminPage(page, BASE_URL);
    adminPage.navigate();
    Assert.assertTrue(adminPage.isLoaded(), "Administration page should load");
  }

  @Test
  public void usersListShowsAdminUser() {
    loginAsAdmin();
    AdminPage adminPage = new AdminPage(page, BASE_URL);
    adminPage.navigateToUsers();
    Assert.assertTrue(adminPage.isAdminUserVisible(), "Admin user should be visible in the users list");
  }
}

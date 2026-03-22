/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.e2e.tests;

import org.roda.wui.e2e.base.BaseTest;
import org.roda.wui.e2e.pages.BrowsePage;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = {"e2e"})
public class BrowseAIPTest extends BaseTest {

  @Test
  public void browsePageLoads() {
    loginAsAdmin();
    BrowsePage browsePage = new BrowsePage(page, BASE_URL);
    browsePage.navigate();
    Assert.assertTrue(browsePage.isLoaded(), "Browse page should load");
    Assert.assertTrue(browsePage.hasSearchBar(), "Browse page should have a search bar");
  }
}

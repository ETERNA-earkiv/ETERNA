/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.e2e.tests;

import org.roda.wui.e2e.base.BaseTest;
import org.roda.wui.e2e.pages.SearchPage;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = {"e2e"})
public class SearchTest extends BaseTest {

  @Test
  public void searchPageLoads() {
    loginAsAdmin();
    SearchPage searchPage = new SearchPage(page, BASE_URL);
    searchPage.navigate();
    Assert.assertTrue(searchPage.isLoaded(), "Search page should load with a search input");
  }

  @Test
  public void searchReturnsResults() {
    loginAsAdmin();
    SearchPage searchPage = new SearchPage(page, BASE_URL);
    searchPage.navigate();
    searchPage.search("*");
    Assert.assertTrue(searchPage.hasResults(), "Search should render results panel or empty message");
  }
}

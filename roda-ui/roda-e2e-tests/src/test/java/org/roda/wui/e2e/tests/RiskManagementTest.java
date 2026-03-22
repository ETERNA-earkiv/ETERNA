/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.e2e.tests;

import org.roda.wui.e2e.base.BaseTest;
import org.roda.wui.e2e.pages.PlanningPage;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = {"e2e"})
public class RiskManagementTest extends BaseTest {

  @Test
  public void planningPageLoads() {
    loginAsAdmin();
    PlanningPage planningPage = new PlanningPage(page, BASE_URL);
    planningPage.navigate();
    Assert.assertTrue(planningPage.isLoaded(), "Planning page should load");
  }

  @Test
  public void riskRegisterLoads() {
    loginAsAdmin();
    PlanningPage planningPage = new PlanningPage(page, BASE_URL);
    planningPage.navigateToRisks();
    Assert.assertTrue(planningPage.isRiskRegisterLoaded(), "Risk register should render (table or empty message)");
  }
}

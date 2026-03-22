/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.e2e.tests;

import org.roda.wui.e2e.base.BaseTest;
import org.roda.wui.e2e.pages.ProcessPage;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = {"e2e"})
public class ProcessTest extends BaseTest {

  @Test
  public void processPageLoads() {
    loginAsAdmin();
    ProcessPage processPage = new ProcessPage(page, BASE_URL);
    processPage.navigate();
    Assert.assertTrue(processPage.isLoaded(), "Process/jobs page should load");
  }
}

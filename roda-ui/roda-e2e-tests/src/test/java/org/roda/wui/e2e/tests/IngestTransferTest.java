/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.e2e.tests;

import java.net.URL;
import java.nio.file.Paths;

import org.roda.wui.e2e.base.BaseTest;
import org.roda.wui.e2e.pages.IngestPage;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = {"e2e"})
public class IngestTransferTest extends BaseTest {

  @Test
  public void ingestTransferPageLoads() {
    loginAsAdmin();
    IngestPage ingestPage = new IngestPage(page, BASE_URL);
    ingestPage.navigateToTransfer();
    Assert.assertTrue(ingestPage.isTransferPageLoaded(), "Ingest transfer page should load");
  }

  @Test
  public void uploadButtonIsVisible() {
    loginAsAdmin();
    IngestPage ingestPage = new IngestPage(page, BASE_URL);
    ingestPage.navigateToTransfer();
    Assert.assertTrue(ingestPage.hasUploadButton(), "Upload button should be visible on the ingest transfer page");
  }

  @Test
  public void uploadSIP() {
    loginAsAdmin();
    URL resource = getClass().getResource("/fixtures/sample.zip");
    Assert.assertNotNull(resource, "sample.zip fixture must exist in src/test/resources/fixtures/");

    IngestPage ingestPage = new IngestPage(page, BASE_URL);
    ingestPage.navigateToTransfer();
    ingestPage.uploadFile(Paths.get(resource.getPath()));

    Assert.assertTrue(ingestPage.isFileInList("sample.zip"), "Uploaded file should appear in the transfer list");
  }
}

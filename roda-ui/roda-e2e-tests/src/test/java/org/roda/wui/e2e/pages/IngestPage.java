/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.e2e.pages;

import java.nio.file.Path;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;

public class IngestPage {

  private final Page page;
  private final String baseUrl;

  public IngestPage(Page page, String baseUrl) {
    this.page = page;
    this.baseUrl = baseUrl;
  }

  public void navigateToTransfer() {
    page.navigate(baseUrl + "/#ingest/transfer");
    page.waitForLoadState(LoadState.NETWORKIDLE);
  }

  public boolean isTransferPageLoaded() {
    return page.locator(".wui-ingest-transfer").count() > 0 || page.locator(".contentFlowPanel").count() > 0;
  }

  public boolean hasUploadButton() {
    // ActionButton renders "btn-upload" as icon class "fa fa-upload" on an <i>
    // inside an .actionable-button container — NOT as a CSS class on the button.
    return page.locator(".fa-upload").count() > 0;
  }

  /**
   * Uploads a file by navigating directly to the upload page
   * (/#ingest/transfer/upload) and setting the file on the hidden file input.
   *
   * <p>
   * The file input name is RodaConstants.API_PARAM_UPLOAD ("upl"). jQuery
   * fileupload hides the native input and renders a stylised drop-zone;
   * Playwright's setInputFiles bypasses visibility checks so no force flag is
   * needed.
   */
  public void uploadFile(Path filePath) {
    // Navigate directly to the upload page (equivalent to clicking UPLOAD button)
    page.navigate(baseUrl + "/#ingest/transfer/upload");
    page.waitForLoadState(LoadState.NETWORKIDLE);
    // Wait for the drop-zone container rendered by
    // TransferUpload.updateUploadForm()
    page.locator("#drop").waitFor(new Locator.WaitForOptions().setTimeout(10000));
    // Set the file on the hidden native input (name =
    // RodaConstants.API_PARAM_UPLOAD)
    page.locator("input[name='upl']").setInputFiles(filePath);
    page.waitForLoadState(LoadState.NETWORKIDLE);
  }

  public boolean isFileInList(String filename) {
    // After upload, the file name appears in #upload-list on the upload page
    return page.locator("#upload-list").locator("text=" + filename).count() > 0
      || page.locator("table").locator("text=" + filename).count() > 0;
  }
}

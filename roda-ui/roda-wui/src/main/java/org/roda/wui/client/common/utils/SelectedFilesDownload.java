/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.client.common.utils;

import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.exceptions.AuthorizationDeniedException;
import org.roda.core.data.utils.SelectedItemsUtils;
import org.roda.core.data.v2.file.DownloadRefusal;
import org.roda.core.data.v2.file.DownloadRefusalReason;
import org.roda.core.data.v2.file.PreparedDownloadResponse;
import org.roda.core.data.v2.index.select.SelectedItems;
import org.roda.core.data.v2.ip.IndexedFile;
import org.roda.wui.client.common.NoAsyncCallback;
import org.roda.wui.client.common.actions.Actionable.ActionImpact;
import org.roda.wui.client.common.dialogs.Dialogs;
import org.roda.wui.client.services.Services;
import org.roda.wui.common.client.tools.ConfigurationManager;
import org.roda.wui.common.client.tools.Humanize;
import org.roda.wui.common.client.tools.RestUtils;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

import config.i18n.client.ClientMessages;

/**
 * Downloads a selection of files as a zip, in the two steps that a disclosure
 * ("utlämnande") needs: first ask the server to expand and validate the
 * selection, then fetch the result.
 * <p>
 * The second step is a plain navigation to the download URL, so the browser's
 * own download handling applies — a progress indicator, a place on disk, and a
 * resumable transfer that survives leaving the page. See
 * {@code docs/adr/0002-prepared-download-via-token-for-selection-based-downloads.md}.
 */
public final class SelectedFilesDownload {

  private static final ClientMessages messages = GWT.create(ClientMessages.class);

  private SelectedFilesDownload() {
    // do nothing
  }

  /**
   * Starts the flow. The callback is always answered, and every path that ends
   * without a download says why.
   */
  public static void start(SelectedItems<IndexedFile> selection, AsyncCallback<ActionImpact> callback) {
    Services services = new Services(messages.filesReasonDownloadSelectedFiles(), "download");
    services.fileResource(s -> s.requestSelectedFilesDownload(SelectedItemsUtils.convertToRESTRequest(selection)))
      .whenComplete((response, throwable) -> {
        // the action itself changes nothing, whatever the outcome: the dialogs
        // below are what informs the user
        callback.onSuccess(ActionImpact.NONE);

        if (throwable != null) {
          showFailure(throwable);
        } else if (response.getRefusal() != null) {
          showRefusal(response.getRefusal());
        } else {
          confirmIfLarge(response);
        }
      });
  }

  /**
   * Warns before a download that will take a while, but does not stand in the
   * way: an archivist with a legitimately large disclosure confirms and
   * proceeds.
   */
  private static void confirmIfLarge(PreparedDownloadResponse response) {
    // the two thresholds are independent: either one on its own brings up the
    // dialog
    boolean manyFiles = response.getFileCount() > ConfigurationManager.getInt(
      RodaConstants.DEFAULT_UI_DOWNLOAD_CONFIRMATION_FILES_THRESHOLD,
      RodaConstants.UI_DOWNLOAD_CONFIRMATION_FILES_THRESHOLD);
    boolean large = response.getTotalSize() > ConfigurationManager.getLong(
      RodaConstants.DEFAULT_UI_DOWNLOAD_CONFIRMATION_SIZE_THRESHOLD,
      RodaConstants.UI_DOWNLOAD_CONFIRMATION_SIZE_THRESHOLD);

    if (!manyFiles && !large) {
      startDownload(response.getToken());
      return;
    }

    Dialogs.showConfirmDialog(messages.downloadSelectedFilesConfirmTitle(),
      messages.downloadSelectedFilesConfirmMessage(response.getFileCount(),
        Humanize.readableFileSize(response.getTotalSize())),
      messages.dialogCancel(), messages.dialogYes(), new NoAsyncCallback<Boolean>() {
        @Override
        public void onSuccess(Boolean confirmed) {
          if (Boolean.TRUE.equals(confirmed)) {
            startDownload(response.getToken());
          }
        }
      });
  }

  private static void startDownload(String token) {
    Window.Location.assign(RestUtils.createPreparedDownloadUri(token).asString());
  }

  private static void showRefusal(DownloadRefusal refusal) {
    String message = messages.downloadSelectedFilesFailed();

    if (DownloadRefusalReason.NO_FILES.equals(refusal.getReason())) {
      message = messages.downloadSelectedFilesNoFiles();
    } else if (DownloadRefusalReason.TOO_MANY_FILES.equals(refusal.getReason())) {
      message = messages.downloadSelectedFilesTooManyFiles(refusal.getFileCount(), refusal.getFileLimit());
    } else if (DownloadRefusalReason.UNDELIVERABLE_CONTENT.equals(refusal.getReason())) {
      message = messages.downloadSelectedFilesUndeliverableContent(refusal.getUndeliverableFileCount(),
        refusal.getFileCount());
    }

    Dialogs.showInformationDialog(messages.downloadSelectedFilesRefusedTitle(), message, messages.dialogOk(), false);
  }

  /**
   * A missing read permission on any of the selected files arrives as a plain
   * 403, like every other permission failure in the API, so it is recognised
   * here rather than carried in the refusal.
   */
  private static void showFailure(Throwable throwable) {
    if (throwable instanceof AuthorizationDeniedException) {
      Dialogs.showInformationDialog(messages.downloadSelectedFilesRefusedTitle(),
        messages.downloadSelectedFilesNoPermission(), messages.dialogOk(), false);
    } else {
      Dialogs.showInformationDialog(messages.downloadSelectedFilesRefusedTitle(),
        messages.downloadSelectedFilesFailed(), messages.dialogOk(), false);
    }
  }

}

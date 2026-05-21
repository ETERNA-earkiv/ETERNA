/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.client.browse;

import org.roda.wui.common.client.tools.RestUtils;
import org.roda.wui.common.client.widgets.Toast;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Frame;

import config.i18n.client.ClientMessages;

import com.google.gwt.core.client.GWT;
import org.apache.http.HttpStatus;

/**
 * Handles the HTTP orchestration for server-side XSLT previews: builds the
 * preview URL, performs the GET, writes the response to the iframe srcdoc only
 * on HTTP 200, and surfaces errors via {@link Toast} without clobbering any
 * working preview already in the iframe.
 * <p>
 * Extracted out of {@link BitstreamPreview} to keep the view free of async
 * request orchestration. The JSNI helpers ({@code applyCustomXslt},
 * {@code getCachedXslt}, {@code applyCachedXslt}) stay on
 * {@link BitstreamPreview} because they manipulate the host element directly
 * and rely on instance-bound natives.
 */
final class XsltPreviewService {

  private static final ClientMessages messages = GWT.create(ClientMessages.class);

  private XsltPreviewService() {
    // utility class
  }

  /** Builds the GET URL for a server-side XSLT preview. */
  static String buildPreviewUrl(String fileUuid, String locale, String xsltId) {
    return RestUtils.createRepresentationFileHtmlPreviewUri(fileUuid, locale, xsltId).asString();
  }

  /**
   * Issues the preview GET and writes the response into {@code frame}'s
   * {@code srcdoc} on HTTP 200, then runs {@code onSuccess} if non-null. On any
   * non-200/exception path the iframe and raw-XML view are left untouched and
   * a toast describes the failure.
   */
  static void loadPreview(String url, Frame frame, Command onSuccess) {
    RequestBuilder request = new RequestBuilder(RequestBuilder.GET, url);
    try {
      request.sendRequest(null, new RequestCallback() {
        @Override
        public void onResponseReceived(Request req, Response response) {
          if (response.getStatusCode() == HttpStatus.SC_OK) {
            frame.getElement().setAttribute("srcdoc", response.getText());
            if (onSuccess != null) {
              onSuccess.execute();
            }
          } else {
            Toast.showError(messages.xsltTransformFailed() + response.getStatusCode());
          }
        }

        @Override
        public void onError(Request req, Throwable exception) {
          Toast.showError(messages.xsltTransformFailed() + exception.getMessage());
        }
      });
    } catch (RequestException e) {
      Toast.showError(messages.xsltTransformFailed() + e.getMessage());
    }
  }
}

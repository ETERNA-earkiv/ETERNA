package org.roda.wui.client.redact;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import elemental2.dom.Blob;
import elemental2.dom.FormData;
import elemental2.dom.RequestInit;
import elemental2.promise.Promise;
import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.v2.index.IndexResult;
import org.roda.core.data.v2.index.facet.Facets;
import org.roda.core.data.v2.index.filter.Filter;
import org.roda.core.data.v2.index.filter.SimpleFilterParameter;
import org.roda.core.data.v2.index.select.SelectedItemsList;
import org.roda.core.data.v2.index.sort.SortParameter;
import org.roda.core.data.v2.index.sort.Sorter;
import org.roda.core.data.v2.index.sublist.Sublist;
import org.roda.core.data.v2.ip.IndexedFile;
import org.roda.core.data.v2.ip.IndexedRepresentation;
import org.roda.core.data.v2.jobs.Job;
import org.roda.wui.client.browse.BrowserService;
import org.roda.wui.client.browse.bundle.BrowseFileBundle;
import org.roda.wui.client.common.PromiseAsyncCallback;
import org.roda.wui.client.common.PromiseWrapper;
import org.roda.wui.client.common.UserLogin;
import org.roda.wui.client.common.utils.AsyncCallbackUtils;
import org.roda.wui.client.common.utils.CssFileInjector;
import org.roda.wui.client.common.utils.ScriptModuleInjector;
import org.roda.wui.client.main.Theme;
import org.roda.wui.common.client.HistoryResolver;
import org.roda.wui.common.client.tools.HistoryUtils;
import org.roda.wui.common.client.tools.RestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static elemental2.dom.DomGlobal.fetch;

public class PDFRedactor extends Composite {
  public static final String JS_PATH = "pdf-redactor/assets/index.js";
  public static final String CSS_PATH = "pdf-redactor/assets/index.css";
  public static String[] requiredRoles = new String[]{"representation.view", "representation.read",
          "representation.create", "representation.update"};
  private static PDFRedactor instance = null;
  public static final HistoryResolver RESOLVER = new HistoryResolver() {

    @Override
    public void resolve(List<String> historyTokens, AsyncCallback<Widget> callback) {
      getInstance().resolve(historyTokens, callback);
    }

    @Override
    public void isCurrentUserPermitted(AsyncCallback<Boolean> callback) {
      UserLogin.getInstance().checkRole(Arrays.asList(requiredRoles), callback);
    }

    @Override
    public String getHistoryToken() {
      return "redactor";
    }

    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    @Override
    public List<String> getHistoryPath() {
      return Arrays.asList(getHistoryToken());
    }
  };

  private static final List<String> findRedactedRepresentationFieldsToReturn = new ArrayList<>();

  static {
    findRedactedRepresentationFieldsToReturn.addAll(
            Arrays.asList(RodaConstants.INDEX_ID, RodaConstants.INDEX_UUID, RodaConstants.REPRESENTATION_TITLE, RodaConstants.REPRESENTATION_TYPE));
  }

  private static final Sorter findRedactedRepresentationSorter = new Sorter(
          new SortParameter(RodaConstants.REPRESENTATION_ID, false)
  );

  private boolean initialized;
  private PDFRedactorPanel pdfRedactorPanel;

  private PDFRedactor() {
    initialized = false;
  }

  public static native void consoleLog(String text) /*-{
    console.log(text);
  }-*/;

  public static native void consoleLog(List<String> obj) /*-{
    console.log(obj);
  }-*/;

  public static native void consoleLog(JavaScriptObject obj) /*-{
    console.log(obj);
  }-*/;

  /**
   * Get the singleton instance
   *
   * @return the instance
   */
  public static PDFRedactor getInstance() {
    if (instance == null) {
      instance = new PDFRedactor();
    }
    return instance;
  }

  private void init(final IndexedFile file) {
    if (!initialized) {
      initialized = true;

      String downloadUrl = RestUtils.createRepresentationFileDownloadUri(file.getUUID()).asString();
      //String uploadUrl = RestUtils.createFileUploadUri()
      //BrowserService.Util.getInstance().upl

      String aipId = file.getAipId();


      new CssFileInjector(RestUtils.createThemeResourceUri(CSS_PATH, CSS_PATH, false).asString())
              .setWindow(CssFileInjector.TOP_WINDOW).inject();

      pdfRedactorPanel = new PDFRedactorPanel();

      ScriptModuleInjector scriptModuleInjector = new ScriptModuleInjector(
              RestUtils.createThemeResourceUri(JS_PATH, JS_PATH, false).asString());

      scriptModuleInjector.setWindow(ScriptModuleInjector.TOP_WINDOW);
      scriptModuleInjector.setCallback(new Callback<Void, Exception>() {
        @Override
        public void onFailure(Exception e) {
          AsyncCallbackUtils.defaultFailureTreatment(e);
        }

        @Override
        public void onSuccess(Void unused) {
          pdfRedactorPanel.setUrl(downloadUrl);
          pdfRedactorPanel.render();
          pdfRedactorPanel.setSaveCallback((Blob pdfData) -> {
            getOrCreateRedactedRepresentation(aipId).then(
                    (representation) -> {
                      List<String> path = new ArrayList<>();
                      path.addAll(file.getPath());

                      consoleLog("path:");
                      consoleLog(path);

                      String uploadUri = RestUtils.createFileUploadUri(
                              aipId,
                              representation.getId(),
                              path,
                              "Saved redacted version"
                      );

                      consoleLog("uploadUri: " + uploadUri);

                      FormData formData = new FormData();
                      formData.append("upl", pdfData, file.getOriginalName());

                      RequestInit requestInit = RequestInit.create();
                      requestInit.setMethod("POST");
                      requestInit.setBody(formData);

                      fetch(uploadUri, requestInit);

                      return null;
                    }
            );

            return true;
          });
        }
      });

      scriptModuleInjector.inject();
    }
  }

  private static Promise<IndexedRepresentation> getOrCreateRedactedRepresentation(String aipId) {
    Filter findRedactedRepresentationFilter = new Filter(
            new SimpleFilterParameter(RodaConstants.REPRESENTATION_AIP_ID, aipId),

            // Make type configurable
            new SimpleFilterParameter(RodaConstants.REPRESENTATION_TYPE, "Redacted")
    );

    final PromiseWrapper<IndexedRepresentation> repPromise = new PromiseWrapper<>();
    final PromiseAsyncCallback<IndexResult<IndexedRepresentation>> findRepPromise = new PromiseAsyncCallback<>();
    findRepPromise.getPromise().<Void>then(
            (result) -> {
              if (result.getTotalCount() > 0) {
                repPromise.resolve(result.getResults().get(0));
              } else {
                createRedactedRepresentation(aipId).then(repPromise::resolve).catch_(repPromise::reject);
              }
              return null;
            }
    ).catch_(repPromise::reject);

    BrowserService.Util.getInstance().find(
            IndexedRepresentation.class.getName(),
            findRedactedRepresentationFilter,
            findRedactedRepresentationSorter,
            new Sublist(0, 1),
            Facets.NONE,
            LocaleInfo.getCurrentLocale().getLocaleName(),
            true,
            findRedactedRepresentationFieldsToReturn,
            findRepPromise
    );

    return repPromise.getPromise();
  }

  private static Promise<IndexedRepresentation> getRepresentationById(String aipId, String representationId) {
    final Filter findRedactedRepresentationFilter = new Filter(
            new SimpleFilterParameter(RodaConstants.REPRESENTATION_AIP_ID, aipId),
            new SimpleFilterParameter(RodaConstants.REPRESENTATION_ID, representationId)
    );

    final PromiseWrapper<IndexedRepresentation> repPromise = new PromiseWrapper<>();
    final PromiseAsyncCallback<IndexResult<IndexedRepresentation>> findRepPromise = new PromiseAsyncCallback<>();
    findRepPromise.getPromise().then(
            (result) -> {
              if (result.getTotalCount() > 0) {
                return repPromise.resolve(result.getResults().get(0));
              } else {
                return repPromise.reject(new Exception("Could not find created representation for redacted files."));
              }
            }
    ).catch_(repPromise::reject);

    BrowserService.Util.getInstance().find(
            IndexedRepresentation.class.getName(),
            findRedactedRepresentationFilter,
            findRedactedRepresentationSorter,
            new Sublist(0, 1),
            Facets.NONE,
            LocaleInfo.getCurrentLocale().getLocaleName(),
            true,
            findRedactedRepresentationFieldsToReturn,
            findRepPromise
    );

    return repPromise.getPromise();
  }

  private static Promise<IndexedRepresentation> createRedactedRepresentation(String aipId) {
    final PromiseWrapper<IndexedRepresentation> repPromise = new PromiseWrapper<>();
    final PromiseAsyncCallback<String> createRepPromise = new PromiseAsyncCallback<>();
    createRepPromise.getPromise().then(
            (representationId) -> {
              getRepresentationById(aipId, representationId).then(
                      (representation) -> {
                        setRepresentationType(representation).then(
                                (Void) -> repPromise.resolve(representation)
                        ).catch_(repPromise::reject);

                        return null;
                      }
              );

              return null;
            }
    ).catch_(repPromise::reject);

    BrowserService.Util.getInstance().createRepresentation(aipId, "Creating representation for redacted files", createRepPromise);

    return repPromise.getPromise();
  }

  private static Promise<Job> setRepresentationType(IndexedRepresentation representation) {
    final SelectedItemsList<IndexedRepresentation> selectedItemsList = new SelectedItemsList<>(
            Collections.singletonList(representation.getUUID()),
            IndexedRepresentation.class.getName()
    );

    final PromiseAsyncCallback<Job> changeRepTypeJobCallback = new PromiseAsyncCallback<>();
    BrowserService.Util.getInstance().changeRepresentationType(
            selectedItemsList,
            "Redacted",
            "Setting representation type to \"Redacted\"",
            changeRepTypeJobCallback
    );

    return changeRepTypeJobCallback.getPromise();
  }

  public void resolve(List<String> historyTokens, AsyncCallback<Widget> callback) {
    if (historyTokens.size() > 2) {
      final String aipId = historyTokens.get(0);
      final String representationId = historyTokens.get(1);
      final List<String> filePath = new ArrayList<>(historyTokens.subList(2, historyTokens.size() - 1));
      final String fileId = historyTokens.get(historyTokens.size() - 1);

      BrowserService.Util.getInstance().retrieveBrowseFileBundle(aipId, representationId, filePath, fileId,
              Collections.emptyList(), new AsyncCallback<BrowseFileBundle>() {
                @Override
                public void onFailure(Throwable caught) {
                  AsyncCallbackUtils.defaultFailureTreatment(caught);
                }

                @Override
                public void onSuccess(final BrowseFileBundle bundle) {
                  init(bundle.getFile());
                  callback.onSuccess(pdfRedactorPanel);
                }
              });
    } else {
      HistoryUtils.newHistory(Theme.RESOLVER, "Error404.html");
      callback.onSuccess(null);
    }
  }
}

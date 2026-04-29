/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.client.redact;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;
import config.i18n.client.ClientMessages;
import org.roda.core.data.v2.index.IndexedRepresentationRequest;
import org.roda.core.data.v2.ip.redaction.SaveRedactionRequest;
import org.roda.wui.client.common.NavigationToolbar;
import org.roda.wui.client.common.dialogs.Dialogs;
import org.roda.wui.common.client.tools.ConfigurationManager;
import org.roda.wui.common.client.widgets.Toast;
import org.roda.wui.common.client.widgets.wcag.AccessibleFocusPanel;
import elemental2.dom.AbortSignal;
import elemental2.dom.Blob;
import elemental2.dom.FormData;
import elemental2.dom.RequestInit;
import elemental2.promise.Promise;
import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.v2.index.FindRequest;
import org.roda.core.data.v2.index.facet.Facets;
import org.roda.core.data.v2.index.filter.Filter;
import org.roda.core.data.v2.index.filter.SimpleFilterParameter;
import org.roda.core.data.v2.index.sort.SortParameter;
import org.roda.core.data.v2.index.sort.Sorter;
import org.roda.core.data.v2.index.sublist.Sublist;
import org.roda.core.data.v2.generics.select.SelectedItemsListRequest;
import org.roda.core.data.v2.representation.ChangeTypeRequest;
import org.roda.core.data.v2.index.IndexedFileRequest;
import org.roda.core.data.v2.ip.IndexedAIP;
import org.roda.core.data.v2.ip.IndexedFile;
import org.roda.core.data.v2.ip.IndexedRepresentation;
import org.roda.core.data.v2.jobs.Job;
import org.roda.wui.client.common.PromiseAsyncCallback;
import org.roda.wui.client.services.Services;
import org.roda.wui.client.common.PromiseWrapper;
import org.roda.wui.client.common.UserLogin;
import org.roda.wui.client.common.utils.AsyncCallbackUtils;
import org.roda.wui.client.common.utils.CssFileInjector;
import org.roda.wui.client.common.utils.ScriptModuleInjector;
import org.roda.wui.client.main.Theme;
import org.roda.wui.common.client.HistoryResolver;
import org.roda.wui.common.client.tools.HistoryUtils;
import org.roda.wui.common.client.tools.RestUtils;

import java.util.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static elemental2.dom.DomGlobal.fetch;

public class PDFRedactor extends Composite {
  interface MyUiBinder extends UiBinder<Widget, PDFRedactor> {
  }

  private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

  public static final String JS_PATH = "webjars/pdf-redactor/pdf-redactor.js";
  public static final String CSS_PATH = "webjars/pdf-redactor/pdf-redactor.css";
  private static final ClientMessages messages = GWT.create(ClientMessages.class);
  public static String[] requiredRoles = new String[]{"representation.view", "representation.read", "representation.create", "representation.update"};
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
    findRedactedRepresentationFieldsToReturn.addAll(Arrays.asList(RodaConstants.INDEX_ID, RodaConstants.INDEX_UUID, RodaConstants.REPRESENTATION_TITLE, RodaConstants.REPRESENTATION_TYPE));
  }

  private static final Sorter findRedactedRepresentationSorter = new Sorter(new SortParameter(RodaConstants.REPRESENTATION_ID, false));

  private static String generateTimestamp() {
    DateTimeFormat fmt = DateTimeFormat.getFormat("yyyy-MM-dd'T'HH-mm");
    return fmt.format(new Date());
  }

  private static String buildFilename(String originalId, String suffix) {
    String effectiveSuffix = (suffix == null || suffix.isEmpty())
        ? generateTimestamp()
        : suffix;
    int lastDot = originalId.lastIndexOf('.');
    String base = (lastDot >= 0) ? originalId.substring(0, lastDot) : originalId;
    String ext  = (lastDot >= 0) ? originalId.substring(lastDot)    : "";
    return base + "_" + effectiveSuffix + ext;
  }

  private boolean initialized;

  @UiField
  AccessibleFocusPanel keyboardFocus;

  @UiField
  NavigationToolbar<IndexedFile> navigationToolbar;

  @UiField
  FlowPanel center;

  @UiField
  PDFRedactorPanel pdfRedactorPanel;

  private PDFRedactor() {
    initialized = false;
    initWidget(uiBinder.createAndBindUi(this));
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
    String downloadUrl = RestUtils.createRepresentationFileDownloadUri(file.getUUID()).asString();
    String aipId = file.getAipId();

    if (initialized) {
      pdfRedactorPanel.unmount();
      initPdfRedactorPanel(aipId, file, downloadUrl);
    } else {
      initialized = true;

      new CssFileInjector(CSS_PATH).setWindow(CssFileInjector.TOP_WINDOW).inject();

      ScriptModuleInjector scriptModuleInjector = new ScriptModuleInjector(JS_PATH);

      scriptModuleInjector.setWindow(ScriptModuleInjector.TOP_WINDOW);
      scriptModuleInjector.setCallback(new Callback<Void, Exception>() {
        @Override
        public void onFailure(Exception e) {
          AsyncCallbackUtils.defaultFailureTreatment(e);
        }

        @Override
        public void onSuccess(Void unused) {
          initPdfRedactorPanel(aipId, file, downloadUrl);
        }
      });

      scriptModuleInjector.inject();
    }
  }

  private void initPdfRedactorPanel(final String aipId, final IndexedFile file, final String downloadUrl) {
    pdfRedactorPanel.setUrl(downloadUrl);
    pdfRedactorPanel.mount();

    pdfRedactorPanel.setPreSaveCallback(() -> {
      PromiseWrapper<Void> preSavePromise = new PromiseWrapper<>();
      boolean mandatory = ConfigurationManager.getBoolean(true, RodaConstants.UI_REDACTION_REASON_MANDATORY);

      Dialogs.showPromptDialog(messages.redactPdfReasonTitle(), null, null,
        messages.redactPdfReasonPlaceholder(), RegExp.compile(".*"),
        messages.cancelButton(), messages.confirmButton(), mandatory, true,
        new AsyncCallback<String>() {
          @Override
          public void onFailure(Throwable caught) {
            preSavePromise.reject(caught);
          }

          @Override
          public void onSuccess(String details) {
            SaveRedactionRequest request = new SaveRedactionRequest(
              aipId, file.getRepresentationId(), file.getId(), details);
            Services services = new Services("Log redaction save", "post");
            services.redactionResource(s -> s.logRedactionSave(request))
              .whenComplete((result, throwable) -> {
                if (throwable != null) {
                  preSavePromise.reject(throwable);
                } else {
                  preSavePromise.resolve(null);
                }
              });
          }
        });
      return preSavePromise.getPromise();
    });

    pdfRedactorPanel.setSaveCallback((Blob pdfData, AbortSignal signal, String suffix) ->
      getOrCreateRedactedRepresentation(aipId).then((representation) -> {
        List<String> path = new ArrayList<>(file.getPath());

        String uploadUrl = RestUtils.createFileUploadUri(aipId, representation.getId(), path, "Maskerad PDF sparad");

          String newFilename = buildFilename(file.getId(), suffix);

          FormData formData = new FormData();
          formData.append("resource", pdfData, newFilename);

          RequestInit requestInit = RequestInit.create();
          requestInit.setMethod("POST");
          requestInit.setBody(formData);
          requestInit.setSignal(signal);

          return fetch(uploadUrl, requestInit).then(response -> {
            if (response.ok) {
              Toast.showInfo(messages.redactPdfToastTitle(), messages.redactPdfSaveSuccessDescription());
              return Promise.resolve(response);
            } else if (response.status == RodaConstants.HTTP_RESPONSE_CODE_REQUEST_CONFLICT) {
              // Resolve (inte reject) — React visar inline-feltext vid 409, ingen Toast
              return Promise.resolve(response);
            } else {
              return Promise.reject(response);
            }
          }).catch_(error -> {
            Toast.showError(messages.redactPdfToastTitle(), messages.redactPdfSaveErrorDescription());
            return Promise.reject(error);
          });
        })
    );
  }

  private static Promise<IndexedRepresentation> getOrCreateRedactedRepresentation(String aipId) {
    Filter findRedactedRepresentationFilter = new Filter(new SimpleFilterParameter(RodaConstants.REPRESENTATION_AIP_ID, aipId),

            // Make type configurable
            new SimpleFilterParameter(RodaConstants.REPRESENTATION_TYPE, "Redacted"));

    final PromiseWrapper<IndexedRepresentation> repPromise = new PromiseWrapper<>();
    Services services = new Services("Find redacted representation", "get");
    FindRequest findRequest = new FindRequest.FindRequestBuilder(findRedactedRepresentationFilter, true)
      .withSorter(findRedactedRepresentationSorter)
      .withSublist(new Sublist(0, 1))
      .withFacets(Facets.NONE)
      .withFieldsToReturn(findRedactedRepresentationFieldsToReturn)
      .build();

    services.rodaEntityRestService(s -> s.find(findRequest, LocaleInfo.getCurrentLocale().getLocaleName()), IndexedRepresentation.class)
      .whenComplete((result, throwable) -> {
        if (throwable != null) {
          repPromise.reject(throwable);
        } else if (result.getTotalCount() > 0) {
          repPromise.resolve(result.getResults().get(0));
        } else {
          createRedactedRepresentation(aipId).then(repPromise::resolve).catch_(repPromise::reject);
        }
      });

    return repPromise.getPromise();
  }

  private static Promise<IndexedRepresentation> getRepresentationById(String aipId, String representationId) {
    final Filter findRedactedRepresentationFilter = new Filter(new SimpleFilterParameter(RodaConstants.REPRESENTATION_AIP_ID, aipId), new SimpleFilterParameter(RodaConstants.REPRESENTATION_ID, representationId));

    final PromiseWrapper<IndexedRepresentation> repPromise = new PromiseWrapper<>();
    Services services = new Services("Find representation by ID", "get");
    FindRequest findRequest = new FindRequest.FindRequestBuilder(findRedactedRepresentationFilter, true)
      .withSorter(findRedactedRepresentationSorter)
      .withSublist(new Sublist(0, 1))
      .withFacets(Facets.NONE)
      .withFieldsToReturn(findRedactedRepresentationFieldsToReturn)
      .build();

    services.rodaEntityRestService(s -> s.find(findRequest, LocaleInfo.getCurrentLocale().getLocaleName()), IndexedRepresentation.class)
      .whenComplete((result, throwable) -> {
        if (throwable != null) {
          repPromise.reject(throwable);
        } else if (result.getTotalCount() > 0) {
          repPromise.resolve(result.getResults().get(0));
        } else {
          repPromise.reject(new Exception("Could not find created representation for redacted files."));
        }
      });

    return repPromise.getPromise();
  }

  private static Promise<IndexedRepresentation> createRedactedRepresentation(String aipId) {
    final PromiseWrapper<IndexedRepresentation> repPromise = new PromiseWrapper<>();
    Services services = new Services("Create redacted representation", "post");

    services.representationResource(s -> s.createRepresentation(aipId, "MIXED", "Creating representation for redacted files"))
      .whenComplete((representation, throwable) -> {
        if (throwable != null) {
          repPromise.reject(throwable);
        } else {
          // After creating the representation, get it as IndexedRepresentation, then set its type
          getRepresentationById(aipId, representation.getId()).then((indexedRepresentation) -> {
            setRepresentationType(indexedRepresentation).then((job) -> {
              repPromise.resolve(indexedRepresentation);
              return null;
            }).catch_(repPromise::reject);
            return null;
          }).catch_(repPromise::reject);
        }
      });

    return repPromise.getPromise();
  }

  private static Promise<Job> setRepresentationType(IndexedRepresentation representation) {
    final PromiseAsyncCallback<Job> changeRepTypeJobCallback = new PromiseAsyncCallback<>();
    Services services = new Services("Change representation type", "put");
    SelectedItemsListRequest selectedItemsRequest = new SelectedItemsListRequest(Collections.singletonList(representation.getUUID()));
    ChangeTypeRequest changeTypeRequest = new ChangeTypeRequest(selectedItemsRequest, "Redacted", "Setting representation type to \"Redacted\"");

    services.representationResource(s -> s.changeRepresentationType(changeTypeRequest))
      .whenComplete((job, throwable) -> {
        if (throwable != null) {
          changeRepTypeJobCallback.onFailure(throwable);
        } else {
          changeRepTypeJobCallback.onSuccess(job);
        }
      });

    return changeRepTypeJobCallback.getPromise();
  }

  private void setupNavigation(IndexedFile indexedFile, IndexedAIP indexedAIP, IndexedRepresentation indexedRepresentation) {
    navigationToolbar.withObject(indexedFile)
      .withPermissions(indexedAIP.getPermissions())
      .build();
    navigationToolbar.updateBreadcrumb(indexedAIP, indexedRepresentation, indexedFile);
    keyboardFocus.setFocus(true);
  }

  public void resolve(List<String> historyTokens, AsyncCallback<Widget> callback) {
    if (historyTokens.size() > 2) {
      final String aipId = historyTokens.get(0);
      final String representationId = historyTokens.get(1);
      final List<String> filePath = new ArrayList<>(historyTokens.subList(2, historyTokens.size() - 1));
      final String fileId = historyTokens.get(historyTokens.size() - 1);

      Services services = new Services("Retrieve file for PDF redactor", "get");
      IndexedRepresentationRequest repFindRequest = new IndexedRepresentationRequest(aipId, representationId);
      services.representationResource(
        s -> s.retrieveIndexedRepresentationViaRequest(repFindRequest))
        .whenComplete((indexedRepresentation, throwableRep) -> {
          if (throwableRep != null) {
            AsyncCallbackUtils.defaultFailureTreatment(throwableRep);
          } else {
            IndexedFileRequest request = new IndexedFileRequest();
            request.setAipId(aipId);
            request.setRepresentationId(indexedRepresentation.getId());
            request.setDirectoryPaths(filePath);
            request.setFileId(fileId);

            CompletableFuture<IndexedFile> retrieveFileFuture = services.fileResource(s -> s.retrieveIndexedFileViaRequest(request));
            CompletableFuture<IndexedAIP> retrieveAIPFuture = services.rodaEntityRestService(
              s -> s.findByUuid(aipId, LocaleInfo.getCurrentLocale().getLocaleName()), IndexedAIP.class);

            CompletableFuture.allOf(retrieveFileFuture, retrieveAIPFuture)
              .whenComplete((unused, throwable) -> {
                if (throwable != null) {
                  AsyncCallbackUtils.defaultFailureTreatment(throwable);
                } else {
                  PDFRedactor pdfRedactor = getInstance();
                  pdfRedactor.init(retrieveFileFuture.join());
                  pdfRedactor.setupNavigation(retrieveFileFuture.join(), retrieveAIPFuture.join(), indexedRepresentation);
                  callback.onSuccess(pdfRedactor);
                }
              });
          }
        });
    } else {
      HistoryUtils.newHistory(Theme.RESOLVER, "Error404.html");
      callback.onSuccess(null);
    }
  }
}

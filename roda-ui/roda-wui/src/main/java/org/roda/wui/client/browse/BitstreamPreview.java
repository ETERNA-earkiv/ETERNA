/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.client.browse;

import org.apache.http.HttpStatus;
import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.v2.index.IsIndexed;
import org.roda.core.data.v2.ip.AIPState;
import org.roda.core.data.v2.ip.IndexedDIP;
import org.roda.core.data.v2.ip.IndexedFile;
import org.roda.core.data.v2.ip.Permissions;
import org.roda.core.data.v2.ip.metadata.FileFormat;
import org.roda.wui.client.common.utils.IndexedDIPUtils;
import org.roda.wui.client.common.utils.JavascriptUtils;
import org.roda.wui.client.common.UserLogin;
import org.roda.wui.common.client.tools.ConfigurationManager;
import org.roda.wui.common.client.tools.RestUtils;
import org.roda.wui.common.client.tools.StringUtils;

import java.util.Arrays;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.logical.shared.AttachEvent.Handler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.media.client.Audio;
import com.google.gwt.media.client.Video;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import config.i18n.client.ClientMessages;

public class BitstreamPreview<T extends IsIndexed> extends Composite {

  private static final String VIEWER_TYPE_VIDEO = "video";
  private static final String VIEWER_TYPE_AUDIO = "audio";
  private static final String VIEWER_TYPE_TEXT = "text";
  private static final String VIEWER_TYPE_HTML = "html";
  private static final String VIEWER_TYPE_PDF = "pdf";
  private static final String VIEWER_TYPE_IMAGE = "image";
  private static final String VIEWER_TYPE_XML = "xml";
  private static final String VIEWER_TYPE_TIFF = "tiff";
  private static final String VIEWER_TYPE_WEBARCHIVE = "webarchive";

  private static final ClientMessages messages = GWT.create(ClientMessages.class);

  // interface
  private final FlowPanel panel;

  // system properties
  private final Viewers viewers;

  // bitstream properties
  private final SafeUri bitstreamDownloadUri;
  private final FileFormat format;
  private final String filename;
  private final long size;
  private final boolean isDirectory;
  private final boolean isAvailable;

  // other
  private final Command onPreviewFailure;

  private boolean justActive;
  private Permissions permissions;
  private final AIPState state;

  private final T object;
  private boolean fileIsFromDistributedInstance;

  public BitstreamPreview(Viewers viewers, SafeUri bitstreamDownloadUri, FileFormat format, String filename, long size,
    boolean isDirectory, T object) {
    this(viewers, bitstreamDownloadUri, format, filename, size, isDirectory, new Command() {

      @Override
      public void execute() {
        // do nothing
      }
    }, object);
  }

  public BitstreamPreview(Viewers viewers, SafeUri bitstreamDownloadUri, FileFormat format, String filename, long size,
    boolean isDirectory, Command onPreviewFailure, T object) {
    this(viewers, bitstreamDownloadUri, format, filename, size, isDirectory, onPreviewFailure, object, false, null);
  }

  public BitstreamPreview(Viewers viewers, SafeUri bitstreamDownloadUri, FileFormat format, String filename, long size,
    boolean isDirectory, Command onPreviewFailure, T object, boolean justActive, Permissions permissions) {
    this(viewers, bitstreamDownloadUri, format, filename, size, isDirectory, true, onPreviewFailure, object, justActive,
      null, permissions);
  }

  public BitstreamPreview(Viewers viewers, SafeUri bitstreamDownloadUri, FileFormat format, String filename, long size,
    boolean isDirectory, boolean isAvailable, Command onPreviewFailure, T object, boolean justActive, AIPState aipState,
    Permissions permissions) {
    super();
    this.object = object;
    this.panel = new FlowPanel();

    this.viewers = viewers;

    this.bitstreamDownloadUri = bitstreamDownloadUri;
    this.format = format;
    this.filename = filename;
    this.size = size;
    this.isDirectory = isDirectory;
    this.isAvailable = isAvailable;

    this.onPreviewFailure = onPreviewFailure;

    this.state = aipState;
    this.justActive = justActive;
    this.permissions = permissions;

    initWidget(panel);

    setStyleName("bitstreamPreview");
    if (isDirectory) {
      addStyleDependentName("directory");
    }

    init();
  }

  public SafeUri getBitstreamDownloadUri() {
    return bitstreamDownloadUri;
  }

  public FileFormat getFormat() {
    return format;
  }

  public String getFilename() {
    return filename;
  }

  public long getSize() {
    return size;
  }

  public boolean isDirectory() {
    return isDirectory;
  }

  private void init() {
    if (!isDirectory) {
      fileIsFromDistributedInstance = isFileFromDistributedInstance();
      if (fileIsFromDistributedInstance) {
        notSupportedPreviewDistributedInstance();
      } else if (!isAvailable) {
        notSupportedShallowFilePreview();
      } else {
        preview();
      }
    } else {
      panel.add(directoryPreview());
    }
  }

  private void preview() {
    String type = viewerType();
    if (type != null) {
      if (type.equals(VIEWER_TYPE_IMAGE)) {
        imagePreview();
      } else if (type.equals(VIEWER_TYPE_TIFF)) {
        tiffCanvasPreview();
      } else if (type.equals(VIEWER_TYPE_PDF)) {
        pdfPreview();
      } else if (type.equals(VIEWER_TYPE_WEBARCHIVE)) {
        webarchivePreview();
      } else if (type.equals(VIEWER_TYPE_TEXT)) {
        if (isXmlFile()) {
          xmlHtmlPreview();
        } else {
          textPreview();
        }
      } else if (type.equals(VIEWER_TYPE_HTML)) {
        htmlPreview();
      } else if (type.equals(VIEWER_TYPE_AUDIO)) {
        audioPreview();
      } else if (type.equals(VIEWER_TYPE_VIDEO)) {
        videoPreview();
      } else {
        notSupportedPreview();
      }
    } else if (isXmlFile()) {
      xmlHtmlPreview();
    } else if (object instanceof IndexedDIP) {
      IndexedDIP dip = (IndexedDIP) object;
      dipUrlPreview(dip);
    } else {
      notSupportedPreview();
    }
  }

  private String viewerType() {
    String type = null;
    if (format != null) {
      if (format.getPronom() != null) {
        type = viewers.getPronoms().get(format.getPronom());
      }

      if (format.getMimeType() != null && type == null) {
        type = viewers.getMimetypes().get(format.getMimeType());
      }
    }

    if (type == null && filename.lastIndexOf('.') != -1) {
      String extension = getFileNameExtension();
      type = viewers.getExtensions().get(extension);
    }

    return type;
  }

  private String getFileNameExtension() {
    return filename.substring(filename.lastIndexOf('.')).toLowerCase();
  }

  private void imagePreview() {
    final SimplePanel imageContainer = new SimplePanel();

    panel.add(imageContainer);
    imageContainer.setStyleName("viewRepresentationImageFilePreview");

    imageContainer.addAttachHandler(new Handler() {

      private JavaScriptObject imageViewer = null;

      @Override
      public void onAttachOrDetach(AttachEvent event) {
        if (event.isAttached()) {
          // load image
          imageViewer = JavascriptUtils.runImageViewerOn(imageContainer.getElement(), bitstreamDownloadUri.asString());
        } else {
          // destroy
          if (imageViewer != null) {
            JavascriptUtils.stopImageViewer(imageViewer);
          }
        }

      }
    });

  }

  private void tiffCanvasPreview() {
    final SimplePanel canvasContainer = new SimplePanel();
    canvasContainer.setStyleName("viewRepresentationTiffFilePreview");
    panel.add(canvasContainer);

    canvasContainer.addAttachHandler(new Handler() {
      @Override
      public void onAttachOrDetach(AttachEvent event) {
        if (event.isAttached()) {
          JavascriptUtils.runTiffCanvasViewerOn(canvasContainer.getElement(), bitstreamDownloadUri.asString(),
            messages.viewRepresentationTiffLoading(), messages.viewRepresentationTiffError());
        }
      }
    });
  }

  private void pdfPreview() {

    String viewerPdf = GWT.getHostPageBaseURL() + "webjars/pdf-js/web/viewer.html" + "?file="
      + URL.encodeQueryString(GWT.getHostPageBaseURL() + bitstreamDownloadUri.asString()) + "#" + viewers.getOptions();

    final Frame frame = new Frame(viewerPdf);
    frame.addLoadHandler(ev -> JavascriptUtils.runIframeResizer(frame.getElement()));

    panel.add(frame);
    frame.setStyleName("viewRepresentationPDFFilePreview");
  }

  private void webarchivePreview() {
    String sourceUrl = GWT.getHostPageBaseURL() + bitstreamDownloadUri.asString();

    String viewerUrl = GWT.getHostPageBaseURL() + "replay-viewer.html?source=" + URL.encodeQueryString(sourceUrl);

    final Frame frame = new Frame(viewerUrl);
    frame.getElement().setAttribute("title", filename);
    frame.addLoadHandler(ev -> JavascriptUtils.runIframeResizer(frame.getElement()));

    panel.add(frame);
    frame.setStyleName("viewRepresentationWebArchiveFilePreview");
  }

  private void textPreview() {
    textPreview(panel);
  }

  private void textPreview(ComplexPanel target) {
    if (StringUtils.isBlank(viewers.getTextLimit()) || size <= Long.parseLong(viewers.getTextLimit())) {
      RequestBuilder request = new RequestBuilder(RequestBuilder.GET, bitstreamDownloadUri.asString());
      try {
        request.sendRequest(null, new RequestCallback() {

          @Override
          public void onResponseReceived(Request request, Response response) {
            if (response.getStatusCode() == HttpStatus.SC_OK) {
              HTML html = new HTML("<pre><code>" + SafeHtmlUtils.htmlEscape(response.getText()) + "</code></pre>");
              FlowPanel frame = new FlowPanel();
              frame.add(html);

              target.add(frame);
              frame.setStyleName("viewRepresentationTextFilePreview");
              JavascriptUtils.runHighlighter(html.getElement());
            } else {
              errorPreview();
            }
          }

          @Override
          public void onError(Request request, Throwable exception) {
            errorPreview();
          }
        });
      } catch (RequestException e) {
        errorPreview();
      }
    } else {
      errorPreview(messages.viewRepresentationTooLargeErrorPreview());
    }
  }

  private void htmlPreview() {
    Frame frame = new Frame();
    frame.setUrl(bitstreamDownloadUri);
    panel.add(frame);
    frame.setStyleName("viewRepresentationHtmlFilePreview");
  }

  private void audioPreview() {
    Audio audioPlayer = Audio.createIfSupported();
    if (audioPlayer != null) {
      HTML html = new HTML();
      SafeHtmlBuilder b = new SafeHtmlBuilder();
      b.append(SafeHtmlUtils.fromSafeConstant("<i class='fa fa-headphones fa-5'></i>"));
      html.setHTML(b.toSafeHtml());

      // TODO check if audio source type needs to be transformed
      // TODO check if audio player supports provided file format
      audioPlayer.addSource(bitstreamDownloadUri.asString(), getAudioSourceType());
      audioPlayer.setControls(true);
      panel.add(html);
      panel.add(audioPlayer);
      audioPlayer.addStyleName("viewRepresentationAudioFilePreview");
      html.addStyleName("viewRepresentationAudioFilePreviewHTML");
    } else {
      notSupportedPreview();
    }
  }

  private void videoPreview() {
    Video videoPlayer = Video.createIfSupported();
    if (videoPlayer != null) {
      videoPlayer.addSource(bitstreamDownloadUri.asString(), getVideoSourceType());
      videoPlayer.setControls(true);
      panel.add(videoPlayer);
      videoPlayer.addStyleName("viewRepresentationVideoFilePreview");
    } else {
      notSupportedPreview();
    }
  }

  private String getVideoSourceType() {
    String ret;

    if (format != null && StringUtils.isNotBlank(format.getMimeType())) {
      String mimetype = format.getMimeType();
      if ("application/mp4".equals(mimetype)) {
        ret = "video/mp4";
      } else if ("application/ogg".equals(mimetype)) {
        ret = "video/ogg";
      } else {
        ret = mimetype;
      }
    } else {
      String extension = getFileNameExtension();

      if (".ogg".equals(extension)) {
        ret = "video/ogg";
      } else {
        ret = "video/mp4";
      }
    }

    // TODO video player might not support provided file format
    return ret;
  }

  private String getAudioSourceType() {
    String ret;

    if (format != null && StringUtils.isNotBlank(format.getMimeType())) {
      String mimetype = format.getMimeType();
      ret = mimetype;
    } else {
      String extension = getFileNameExtension();

      if (".ogg".equals(extension)) {
        ret = "audio/ogg";
      } else {
        ret = "audio/mpeg";
      }
    }

    // TODO audio player might not support provided file format
    return ret;
  }

  private void dipUrlPreview(IndexedDIP dip) {
    String url = IndexedDIPUtils.interpolateOpenExternalURL(dip, LocaleInfo.getCurrentLocale().getLocaleName());
    final Frame frame = new Frame(url);
    frame.setStyleName("viewDIPPreview");
    frame.setTitle(dip.getTitle());
    frame.getElement().setAttribute("scrolling", "auto");

    if(isSameOrigin(url)){
        frame.addLoadHandler(ev -> JavascriptUtils.runIframeResizer(frame.getElement()));
    }
    panel.add(frame);
  }

  private void errorPreview() {
    errorPreview(messages.viewRepresentationErrorPreview());
  }

  private void errorPreview(String errorPreview) {
    HTML html = new HTML();
    SafeHtmlBuilder b = new SafeHtmlBuilder();

    b.append(SafeHtmlUtils.fromSafeConstant("<i class='fa fa-download fa-5'></i>"));
    b.append(SafeHtmlUtils.fromSafeConstant("<h4 class='errormessage'>"));
    b.append(SafeHtmlUtils.fromString(errorPreview));
    b.append(SafeHtmlUtils.fromSafeConstant("</h4>"));

    Button downloadButton = new Button(messages.viewRepresentationDownloadFileButton());
    downloadButton.addClickHandler(new ClickHandler() {

      @Override
      public void onClick(ClickEvent event) {
        downloadFile();
      }
    });

    html.setHTML(b.toSafeHtml());
    panel.add(html);
    panel.add(downloadButton);
    html.setStyleName("viewRepresentationErrorPreview");
    downloadButton.setStyleName("btn btn-donwload viewRepresentationNotSupportedDownloadButton");

    onPreviewFailure.execute();
  }

  private void notSupportedPreview() {
    HTML html = new HTML();
    SafeHtmlBuilder b = new SafeHtmlBuilder();

    b.append(SafeHtmlUtils.fromSafeConstant("<i class='fa fa-picture-o fa-5'></i>"));
    b.append(SafeHtmlUtils.fromSafeConstant("<h4 class='errormessage'>"));
    b.append(SafeHtmlUtils.fromString(messages.viewRepresentationNotSupportedPreview()));
    b.append(SafeHtmlUtils.fromSafeConstant("</h4>"));

    Button downloadButton = new Button(messages.viewRepresentationDownloadFileButton());
    downloadButton.addClickHandler(new ClickHandler() {

      @Override
      public void onClick(ClickEvent event) {
        downloadFile();
      }
    });

    html.setHTML(b.toSafeHtml());
    panel.add(html);
    panel.add(downloadButton);
    html.setStyleName("viewRepresentationNotSupportedPreview");
    downloadButton.setStyleName("btn btn-download viewRepresentationNotSupportedDownloadButton");

    onPreviewFailure.execute();

  }

  private void notSupportedPreviewDistributedInstance() {
    HTML html = new HTML();
    SafeHtmlBuilder b = new SafeHtmlBuilder();

    b.append(SafeHtmlUtils.fromSafeConstant("<i class='fa fa-picture-o fa-5'></i>"));
    b.append(SafeHtmlUtils.fromSafeConstant("<h4 class='errormessage'>"));
    b.append(SafeHtmlUtils.fromString(messages.viewRepresentationNotSupportedPreviewCentralInstance()));
    b.append(SafeHtmlUtils.fromSafeConstant("</h4>"));

    html.setHTML(b.toSafeHtml());
    panel.add(html);
    html.setStyleName("viewRepresentationNotSupportedPreview");

    onPreviewFailure.execute();
  }

  private void notSupportedShallowFilePreview() {
    HTML html = new HTML();
    SafeHtmlBuilder b = new SafeHtmlBuilder();

    b.append(SafeHtmlUtils.fromSafeConstant("<i class='fa fa-picture-o fa-5'></i>"));
    b.append(SafeHtmlUtils.fromSafeConstant("<h4 class='errormessage'>"));
    b.append(SafeHtmlUtils.fromString(messages.viewRepresentationNotSupportedPreviewShallowFile()));
    b.append(SafeHtmlUtils.fromSafeConstant("</h4>"));

    html.setHTML(b.toSafeHtml());
    panel.add(html);
    html.setStyleName("viewRepresentationNotSupportedPreview");

    onPreviewFailure.execute();
  }

  protected Widget directoryPreview() {
    HTML html = new HTML();
    SafeHtmlBuilder b = new SafeHtmlBuilder();

    b.append(SafeHtmlUtils.fromSafeConstant("<i class='fa fa-folder-open fa-5'></i>"));
    b.append(SafeHtmlUtils.fromSafeConstant("<h4 class='emptymessage'>"));
    b.append(SafeHtmlUtils.fromString(filename + " /"));
    b.append(SafeHtmlUtils.fromSafeConstant("</h4>"));

    html.setHTML(b.toSafeHtml());
    html.setStyleName("viewRepresentationEmptyPreview");
    return html;
  }


  private boolean isXmlFile() {
    if (format != null && format.getMimeType() != null) {
      String mime = format.getMimeType().toLowerCase();
      if (mime.equals("text/xml") || mime.equals("application/xml") || mime.endsWith("+xml")) {
        return true;
      }
    }
    if (filename != null && filename.toLowerCase().endsWith(".xml")) {
      return true;
    }
    return false;
  }

  private void xmlHtmlPreview() {
    if (object instanceof IndexedFile) {
      IndexedFile indexedFile = (IndexedFile) object;
      String fileUuid = indexedFile.getUUID();
      String locale = LocaleInfo.getCurrentLocale().getLocaleName();
      String xsltsUrl = RestUtils.createRepresentationFileXsltsUri(fileUuid).asString();
      String transformUrl = RestUtils.createRepresentationFileTransformUri(fileUuid, locale).asString();

      // Print button + toggle XML/rendered - always visible
      FlowPanel printToolbar = new FlowPanel();
      printToolbar.setStyleName("xmlPreviewToolbar");
      Button printButton = new Button(messages.xsltPrintButton());
      printButton.setStyleName("btn btn-play xmlPreviewPrintButton");
      Button toggleXmlButton = new Button(messages.xsltViewOriginalButton());
      toggleXmlButton.setStyleName("btn btn-play xmlPreviewToggleButton");
      printToolbar.add(printButton);
      printToolbar.add(toggleXmlButton);
      panel.add(printToolbar);

      // Dropdown toolbar - populated after XSLT list is loaded (hidden until needed)
      FlowPanel dropdownToolbar = new FlowPanel();
      dropdownToolbar.setStyleName("xmlPreviewToolbar");
      dropdownToolbar.setVisible(false);
      Label dropdownLabel = new Label(messages.xsltSelectLabel());
      dropdownLabel.setStyleName("xmlPreviewSelectLabel");
      ListBox xsltDropdown = new ListBox();
      xsltDropdown.setStyleName("xmlPreviewSelectDropdown");
      dropdownToolbar.add(dropdownLabel);
      dropdownToolbar.add(xsltDropdown);
      panel.add(dropdownToolbar);

      // Upload toolbar - only visible if user has representation.apply_xslt role
      FlowPanel toolbar = new FlowPanel();
      toolbar.setStyleName("xmlPreviewToolbar");
      toolbar.setVisible(false);
      FileUpload xsltUpload = new FileUpload();
      xsltUpload.setName("xslt");
      xsltUpload.getElement().setAttribute("accept", ".xslt,.xsl");
      xsltUpload.setStyleName("xmlPreviewFileInput");
      Button applyButton = new Button(messages.applyXsltButton());
      applyButton.setStyleName("btn btn-play xmlPreviewApplyButton");
      applyButton.setEnabled(false);
      xsltUpload.addChangeHandler(event -> applyButton.setEnabled(true));
      toolbar.add(xsltUpload);
      toolbar.add(applyButton);
      panel.add(toolbar);

      UserLogin.getInstance().checkRole(Arrays.asList("representation.apply_xslt"), new AsyncCallback<Boolean>() {
        @Override
        public void onSuccess(Boolean hasRole) {
          toolbar.setVisible(Boolean.TRUE.equals(hasRole));
        }

        @Override
        public void onFailure(Throwable caught) {
          toolbar.setVisible(false);
        }
      });

      // Iframe for rendered HTML
      Frame frame = new Frame();
      frame.setStyleName("viewRepresentationHtmlFilePreview");
      frame.getElement().setAttribute("sandbox", "allow-same-origin");

      applyButton.addClickHandler(event -> {
        applyButton.setEnabled(false);
        applyButton.setText(messages.xsltLoading());
        applyCustomXslt(xsltUpload.getElement(), transformUrl, frame.getElement(), applyButton.getElement(),
          messages.applyXsltButton(), messages.xsltFileTooLarge(), messages.xsltTransformFailed(),
          messages.xsltUploadError(), messages.xsltTransformTimeout());
      });

      printButton.addClickHandler(event -> printIframeContent(frame.getElement(), messages.xsltPrintError()));

      // Toggle between XSLT-rendered view and raw XML
      final boolean[] showingRawXml = {false};
      final String[] currentXsltId = {null};

      toggleXmlButton.addClickHandler(event -> {
        if (!showingRawXml[0]) {
          // Switch to raw XML view
          showingRawXml[0] = true;
          toggleXmlButton.setText(messages.xsltViewRenderedButton());
          dropdownToolbar.setVisible(false);
          RequestBuilder rawRequest = new RequestBuilder(RequestBuilder.GET, bitstreamDownloadUri.asString());
          try {
            rawRequest.sendRequest(null, new RequestCallback() {
              @Override
              public void onResponseReceived(Request req, Response response) {
                if (response.getStatusCode() == HttpStatus.SC_OK) {
                  String escaped = SafeHtmlUtils.htmlEscape(response.getText());
                  String xmlHtml = "<html><body style=\"margin:0;font-family:monospace;font-size:13px;\">"
                    + "<pre style=\"white-space:pre-wrap;word-break:break-all;padding:16px;\">"
                    + escaped + "</pre></body></html>";
                  frame.getElement().setAttribute("srcdoc", xmlHtml);
                }
              }
              @Override
              public void onError(Request req, Throwable exception) {}
            });
          } catch (RequestException e) {}
        } else {
          // Switch back to rendered view
          showingRawXml[0] = false;
          toggleXmlButton.setText(messages.xsltViewOriginalButton());
          if (xsltDropdown.getItemCount() > 1) {
            dropdownToolbar.setVisible(true);
          }
          loadXsltPreview(buildPreviewUrl(fileUuid, locale, currentXsltId[0]), frame);
        }
      });

      // Step 1: fetch available XSLTs, then render
      RequestBuilder xsltsRequest = new RequestBuilder(RequestBuilder.GET, xsltsUrl);
      try {
        xsltsRequest.sendRequest(null, new RequestCallback() {
          @Override
          public void onResponseReceived(Request req, Response response) {
            if (response.getStatusCode() == HttpStatus.SC_OK) {
              try {
                JSONValue parsed = JSONParser.parseStrict(response.getText());
                JSONArray arr = parsed.isArray();
                if (arr != null && arr.size() > 1) {
                  // Populate dropdown
                  for (int i = 0; i < arr.size(); i++) {
                    JSONObject obj = arr.get(i).isObject();
                    if (obj != null) {
                      String id = obj.get("id").isString().stringValue();
                      String label = obj.get("label").isString().stringValue();
                      xsltDropdown.addItem(label, id);
                    }
                  }
                  dropdownToolbar.setVisible(true);

                  // Load with first XSLT (already default, but be explicit)
                  currentXsltId[0] = xsltDropdown.getValue(0);
                  loadXsltPreview(buildPreviewUrl(fileUuid, locale, currentXsltId[0]), frame);

                  // On selection change, reload
                  xsltDropdown.addChangeHandler(event -> {
                    String selectedId = xsltDropdown.getValue(xsltDropdown.getSelectedIndex());
                    currentXsltId[0] = selectedId;
                    loadXsltPreview(buildPreviewUrl(fileUuid, locale, selectedId), frame);
                  });
                } else {
                  // Only one or zero XSLTs — load default, no dropdown
                  loadXsltPreview(buildPreviewUrl(fileUuid, locale, null), frame);
                }
              } catch (Exception e) {
                loadXsltPreview(buildPreviewUrl(fileUuid, locale, null), frame);
              }
            } else {
              loadXsltPreview(buildPreviewUrl(fileUuid, locale, null), frame);
            }
            panel.add(frame);
          }

          @Override
          public void onError(Request req, Throwable exception) {
            loadXsltPreview(buildPreviewUrl(fileUuid, locale, null), frame);
            panel.add(frame);
          }
        });
      } catch (RequestException e) {
        textPreview();
      }
    } else {
      textPreview();
    }
  }

  private static String buildPreviewUrl(String fileUuid, String locale, String xsltId) {
    return RestUtils.createRepresentationFileHtmlPreviewUri(fileUuid, locale, xsltId).asString();
  }

  private void loadXsltPreview(String url, Frame frame) {
    RequestBuilder request = new RequestBuilder(RequestBuilder.GET, url);
    try {
      request.sendRequest(null, new RequestCallback() {
        @Override
        public void onResponseReceived(Request req, Response response) {
          if (response.getStatusCode() == HttpStatus.SC_OK) {
            frame.getElement().setAttribute("srcdoc", response.getText());
          } else {
            textPreview();
          }
        }

        @Override
        public void onError(Request req, Throwable exception) {
          textPreview();
        }
      });
    } catch (RequestException e) {
      textPreview();
    }
  }

  private native void printIframeContent(com.google.gwt.dom.client.Element iframe, String errorMsg) /*-{
    try {
      var iframeDoc = iframe.contentDocument || iframe.contentWindow.document;
      var html = iframeDoc.documentElement.outerHTML;
      var printWin = $wnd.open('', '_blank');
      printWin.document.write(html);
      // Inject print styles to suppress headers/footers
      var style = printWin.document.createElement('style');
      style.textContent = '@page { margin: 15mm; size: auto; } @media print { body { margin: 0; } }';
      printWin.document.head.appendChild(style);
      printWin.document.close();
      printWin.focus();
      // Delay to let content render before printing
      setTimeout(function() {
        printWin.print();
        printWin.close();
      }, 500);
    } catch (e) {
      $wnd.alert(errorMsg + e.message);
    }
  }-*/;

  private native void applyCustomXslt(
    com.google.gwt.dom.client.Element fileInput, String url,
    com.google.gwt.dom.client.Element iframe, com.google.gwt.dom.client.Element button,
    String buttonLabel, String fileTooLargeMsg, String transformFailedMsg,
    String uploadErrorMsg, String timeoutMsg) /*-{
    var files = fileInput.files;
    if (!files || files.length === 0) {
      button.innerText = buttonLabel;
      button.disabled = false;
      return;
    }
    var file = files[0];
    var maxSize = 1024 * 1024;
    if (file.size > maxSize) {
      $wnd.alert("XSLT-filen är för stor (max 1 MB)");
      button.innerText = buttonLabel;
      button.disabled = false;
      return;
    }
    var formData = new FormData();
    formData.append("xslt", file);
    var xhr = new XMLHttpRequest();
    xhr.open("POST", url, true);
    xhr.timeout = 30000;
    xhr.onload = function() {
      if (xhr.status === 200) {
        iframe.setAttribute("srcdoc", xhr.responseText);
      } else {
        $wnd.alert(transformFailedMsg + xhr.statusText);
      }
      button.innerText = buttonLabel;
      button.disabled = false;
    };
    xhr.onerror = function() {
      $wnd.alert(uploadErrorMsg);
      button.innerText = buttonLabel;
      button.disabled = false;
    };
    xhr.ontimeout = function() {
      $wnd.alert("XSLT-transformeringen tog för lång tid (timeout 30s)");
      button.innerText = buttonLabel;
      button.disabled = false;
    };
    xhr.send(formData);
  }-*/;

  private void downloadFile() {
    if (bitstreamDownloadUri != null) {
      Window.Location.assign(bitstreamDownloadUri.asString());
    }
  }

  public T getObject() {
    return object;
  }

  public AIPState getState() {
    return this.state;
  }

  public boolean getJustActive() {
    return justActive;
  }

  public Permissions getPermissions() {
    return permissions;
  }

  public boolean isFileFromDistributedInstance() {
    String distributedMode = ConfigurationManager.getStringWithDefault(
      RodaConstants.DEFAULT_DISTRIBUTED_MODE_TYPE.name(), RodaConstants.DISTRIBUTED_MODE_TYPE_PROPERTY);

    if (this.object instanceof IndexedFile) {
      IndexedFile file = (IndexedFile) this.object;
      if (distributedMode.equals(RodaConstants.DistributedModeType.CENTRAL.name()) && file.isReference()) {
        return UriUtils.extractScheme(file.getReferenceURL()).equals("roda");
      }
    }
    return false;
  }

  public boolean isSameOrigin(String url){
      String base = GWT.getHostPageBaseURL();
      return url !=null && base != null && url.startsWith(base);
  }
}

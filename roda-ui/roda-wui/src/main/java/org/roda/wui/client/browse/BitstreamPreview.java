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
  private static final String LOCAL_VALUE_PREFIX = "__local__:";

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
    // XSL/XSLT files: show raw XML, never apply a stylesheet
    if (isXsltOrXslFile()) {
      textPreview();
      return;
    }
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
              errorPreview(target);
            }
          }

          @Override
          public void onError(Request request, Throwable exception) {
            errorPreview(target);
          }
        });
      } catch (RequestException e) {
        errorPreview(target);
      }
    } else {
      errorPreview(messages.viewRepresentationTooLargeErrorPreview(), target);
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
    errorPreview(messages.viewRepresentationErrorPreview(), panel);
  }

  private void errorPreview(String errorPreview) {
    errorPreview(errorPreview, panel);
  }

  private void errorPreview(ComplexPanel target) {
    errorPreview(messages.viewRepresentationErrorPreview(), target);
  }

  private void errorPreview(String errorPreview, ComplexPanel target) {
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
    target.add(html);
    target.add(downloadButton);
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


  private boolean isXsltOrXslFile() {
    if (filename != null) {
      String lower = filename.toLowerCase();
      if (lower.endsWith(".xsl") || lower.endsWith(".xslt")) {
        return true;
      }
    }
    if (format != null && format.getMimeType() != null) {
      String mime = format.getMimeType().toLowerCase();
      if (mime.equals("application/xslt+xml") || mime.equals("text/xsl") || mime.equals("application/xsl+xml")) {
        return true;
      }
    }
    return false;
  }

  private boolean isXmlFile() {
    if (format != null && format.getMimeType() != null) {
      String mime = format.getMimeType().toLowerCase();
      if (mime.equals("text/xml") || mime.equals("application/xml") || mime.endsWith("+xml")) {
        return true;
      }
    }
    if (filename != null) {
      String lower = filename.toLowerCase();
      if (lower.endsWith(".xml") || lower.endsWith(".xsl") || lower.endsWith(".xslt")) {
        return true;
      }
    }
    return false;
  }

  private void xmlHtmlPreview() {
    if (!(object instanceof IndexedFile)) {
      textPreview();
      return;
    }

    if (isXsltOrXslFile()) {
      textPreview();
      return;
    }

    final IndexedFile indexedFile = (IndexedFile) object;
    final String fileUuid = indexedFile.getUUID();
    final String locale = LocaleInfo.getCurrentLocale().getLocaleName();
    String xsltsUrl = RestUtils.createRepresentationFileXsltsUri(fileUuid).asString();

    // Fetch the available-XSLT list FIRST, then build the unified viewer regardless
    // of whether anything was found. The toolbar always shows the same controls; an
    // empty list just means the stylesheet dropdown shows "(None)" until a user
    // with the apply_xslt role uploads a local one.
    RequestBuilder xsltsRequest = new RequestBuilder(RequestBuilder.GET, xsltsUrl);
    try {
      xsltsRequest.sendRequest(null, new RequestCallback() {
        @Override
        public void onResponseReceived(Request req, Response response) {
          JSONArray xslts = parseXsltArray(response);
          buildXsltViewer(indexedFile, fileUuid, locale, xslts != null ? xslts : new JSONArray());
        }

        @Override
        public void onError(Request req, Throwable exception) {
          buildXsltViewer(indexedFile, fileUuid, locale, new JSONArray());
        }
      });
    } catch (RequestException e) {
      buildXsltViewer(indexedFile, fileUuid, locale, new JSONArray());
    }
  }

  private static JSONArray parseXsltArray(Response response) {
    if (response.getStatusCode() != HttpStatus.SC_OK) {
      return null;
    }
    try {
      JSONValue parsed = JSONParser.parseStrict(response.getText());
      return parsed.isArray();
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Unified XSLT viewer. Toolbar has the same controls regardless of whether any
   * server-side stylesheets were found:
   *
   *   [Toggle XML/render] [Print]   "Stilmall:" [dropdown]   "Använd lokal stilmall:" [Välj fil]   [flagga]
   *
   * The dropdown lists every server-side stylesheet plus — once a privileged user
   * picks one — a single "Local: filename" entry. Changing the dropdown applies
   * the new stylesheet immediately; there is no separate Apply button.
   *
   * Local-upload widgets (label + file button + flag) are visible only to users
   * with the representation.apply_xslt role. Everything else is always visible.
   */
  private void buildXsltViewer(IndexedFile indexedFile, String fileUuid, String locale, JSONArray xslts) {
    String transformUrl = RestUtils.createRepresentationFileTransformUri(fileUuid, locale).asString();

    final FlowPanel toolbar = new FlowPanel();
    toolbar.setStyleName("xmlPreviewToolbar");

    // 1. Toggle (left-most) — switches iframe ↔ raw-XML
    final Button toggleXmlButton = new Button(messages.xsltViewOriginalButton());
    toggleXmlButton.setStyleName("btn btn-play xmlPreviewToggleButton");
    toolbar.add(toggleXmlButton);

    // 2. Print — prints iframe content
    final Button printButton = new Button(messages.xsltPrintButton());
    printButton.setStyleName("btn btn-play xmlPreviewPrintButton");
    toolbar.add(printButton);

    // 3-4. Stylesheet dropdown — applies on change. Empty list → single "(None)"
    // placeholder with empty id, treated as "do nothing" by the change handler.
    Label dropdownLabel = new Label(messages.xsltSelectLabel());
    dropdownLabel.setStyleName("xmlPreviewSelectLabel");
    final ListBox xsltDropdown = new ListBox();
    xsltDropdown.setStyleName("xmlPreviewSelectDropdown");
    for (int i = 0; i < xslts.size(); i++) {
      JSONObject obj = xslts.get(i).isObject();
      if (obj != null) {
        String id = obj.get("id").isString().stringValue();
        String label = obj.get("label").isString().stringValue();
        xsltDropdown.addItem(label, id);
      }
    }
    if (xsltDropdown.getItemCount() == 0) {
      xsltDropdown.addItem(messages.xsltDropdownEmpty(), "");
    }
    toolbar.add(dropdownLabel);
    toolbar.add(xsltDropdown);

    // 5-6. Local upload — hidden <input type="file"> + styled "Välj fil" button.
    // Visible only if the user has the apply_xslt role.
    Label localLabel = new Label(messages.xsltUseLocalLabel());
    localLabel.setStyleName("xmlPreviewLocalLabel");
    localLabel.setVisible(false);
    final FileUpload xsltUpload = new FileUpload();
    xsltUpload.setName("xslt");
    xsltUpload.getElement().setAttribute("accept", ".xslt,.xsl");
    xsltUpload.setStyleName("xmlPreviewFileInput");
    Button selectFileButton = new Button(messages.xsltSelectFileButton());
    selectFileButton.setStyleName("btn-eterna-secondary xmlPreviewSelectFileButton");
    selectFileButton.addClickHandler(event -> triggerClick(xsltUpload.getElement()));
    selectFileButton.setVisible(false);
    toolbar.add(localLabel);
    toolbar.add(selectFileButton);
    toolbar.add(xsltUpload);

    // 7. Flag (right-most) — informs that local uploads are session-only
    Label flagLabel = new Label(messages.xsltLocalNotSaved());
    flagLabel.setStyleName("xmlPreviewFlag");
    flagLabel.setVisible(false);
    toolbar.add(flagLabel);

    panel.add(toolbar);

    UserLogin.getInstance().checkRole(Arrays.asList("representation.apply_xslt"), new AsyncCallback<Boolean>() {
      @Override
      public void onSuccess(Boolean hasRole) {
        boolean show = Boolean.TRUE.equals(hasRole);
        localLabel.setVisible(show);
        selectFileButton.setVisible(show);
        flagLabel.setVisible(show);
      }

      @Override
      public void onFailure(Throwable caught) {
        localLabel.setVisible(false);
        selectFileButton.setVisible(false);
        flagLabel.setVisible(false);
      }
    });

    // XSLT-rendered iframe — primary view when something is selected
    final Frame xsltFrame = new Frame();
    xsltFrame.setStyleName("viewRepresentationHtmlFilePreview");
    xsltFrame.getElement().setAttribute("sandbox", "allow-same-origin");
    panel.add(xsltFrame);

    // Raw XML container — lazily populated on first toggle, then shown/hidden
    final FlowPanel rawXmlContainer = new FlowPanel();
    rawXmlContainer.setVisible(false);
    panel.add(rawXmlContainer);

    final boolean[] rawXmlLoaded = {false};
    final boolean noServerXslts = xslts.size() == 0;
    // No stylesheets → start in raw-XML mode; toggle label reads "Show rendered view"
    final boolean[] showingRawXml = {noServerXslts};
    // Tracks whether the iframe holds anything renderable. Toggle and print are
    // disabled when false — there is no rendered view to switch to/print.
    final boolean[] iframeHasContent = {!noServerXslts};
    if (noServerXslts) {
      xsltFrame.setVisible(false);
      rawXmlContainer.setVisible(true);
      rawXmlLoaded[0] = true;
      textPreview(rawXmlContainer);
      toggleXmlButton.setText(messages.xsltViewRenderedButton());
      toggleXmlButton.setEnabled(false);
      printButton.setEnabled(false);
    }

    // Applies the currently-selected dropdown entry. Empty id = "(None)" = no-op.
    // For local entries we hit the JS cache first (populated by applyCustomXslt on
    // a previous successful POST) so revisiting a local stylesheet does not re-upload.
    Runnable applyCurrentSelection = () -> {
      int idx = xsltDropdown.getSelectedIndex();
      if (idx < 0) return;
      String selectedId = xsltDropdown.getValue(idx);
      if (selectedId == null || selectedId.isEmpty()) return;
      if (selectedId.startsWith(LOCAL_VALUE_PREFIX)) {
        String fn = selectedId.substring(LOCAL_VALUE_PREFIX.length());
        String cached = getCachedXslt(fn);
        if (cached != null) {
          applyCachedXslt(xsltFrame.getElement(), cached);
        }
        // No cache + the file isn't currently in xsltUpload.files → can't re-POST.
        // This only happens for stale local entries from older session state; in
        // practice the cache is populated the moment the user uploads the file.
      } else {
        loadXsltPreview(buildPreviewUrl(fileUuid, locale, selectedId), xsltFrame);
      }
      iframeHasContent[0] = true;
      toggleXmlButton.setEnabled(true);
      if (showingRawXml[0]) {
        showingRawXml[0] = false;
        toggleXmlButton.setText(messages.xsltViewOriginalButton());
        rawXmlContainer.setVisible(false);
        xsltFrame.setVisible(true);
        printButton.setEnabled(true);
      }
    };

    xsltDropdown.addChangeHandler(event -> applyCurrentSelection.run());

    xsltUpload.addChangeHandler(event -> {
      String filename = xsltUpload.getFilename();
      int slash = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
      String shortName = slash >= 0 ? filename.substring(slash + 1) : filename;
      String localValue = LOCAL_VALUE_PREFIX + shortName;

      // Duplicate check: if this filename is already in the dropdown, just select it
      // and let the dropdown change handler (cache-backed) re-apply. No new POST.
      for (int i = 0; i < xsltDropdown.getItemCount(); i++) {
        if (localValue.equals(xsltDropdown.getValue(i))) {
          xsltDropdown.setSelectedIndex(i);
          applyCurrentSelection.run();
          return;
        }
      }

      // New local file: replace "(None)" placeholder if present, else append
      if (xsltDropdown.getItemCount() == 1 && "".equals(xsltDropdown.getValue(0))) {
        xsltDropdown.removeItem(0);
      }
      xsltDropdown.addItem(messages.xsltLocalPrefix() + " " + shortName, localValue);
      xsltDropdown.setSelectedIndex(xsltDropdown.getItemCount() - 1);
      // POST + cache. applyCustomXslt JSNI populates the cache on HTTP 200, keyed
      // by filename, so a later visit to the same dropdown entry resolves locally.
      applyCustomXslt(xsltUpload.getElement(), transformUrl, xsltFrame.getElement(),
        messages.xsltFileTooLarge(), messages.xsltTransformFailed(),
        messages.xsltUploadError(), messages.xsltTransformTimeout());
      iframeHasContent[0] = true;
      toggleXmlButton.setEnabled(true);
      if (showingRawXml[0]) {
        showingRawXml[0] = false;
        toggleXmlButton.setText(messages.xsltViewOriginalButton());
        rawXmlContainer.setVisible(false);
        xsltFrame.setVisible(true);
        printButton.setEnabled(true);
      }
    });

    printButton.addClickHandler(event -> printIframeContent(xsltFrame.getElement(), messages.xsltPrintError()));

    toggleXmlButton.addClickHandler(event -> {
      if (!showingRawXml[0]) {
        showingRawXml[0] = true;
        toggleXmlButton.setText(messages.xsltViewRenderedButton());
        printButton.setEnabled(false);
        xsltFrame.setVisible(false);
        rawXmlContainer.setVisible(true);
        if (!rawXmlLoaded[0]) {
          rawXmlLoaded[0] = true;
          textPreview(rawXmlContainer);
        }
      } else {
        showingRawXml[0] = false;
        toggleXmlButton.setText(messages.xsltViewOriginalButton());
        rawXmlContainer.setVisible(false);
        xsltFrame.setVisible(true);
        printButton.setEnabled(true);
      }
    });

    // Initial render: if any server-side XSLT exists, load the first one
    if (!noServerXslts) {
      loadXsltPreview(buildPreviewUrl(fileUuid, locale, xsltDropdown.getValue(0)), xsltFrame);
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
            showXsltErrorInFrame(frame, messages.xsltTransformFailed() + response.getStatusCode());
          }
        }

        @Override
        public void onError(Request req, Throwable exception) {
          showXsltErrorInFrame(frame, messages.xsltTransformFailed() + exception.getMessage());
        }
      });
    } catch (RequestException e) {
      showXsltErrorInFrame(frame, messages.xsltTransformFailed() + e.getMessage());
    }
  }

  private static void showXsltErrorInFrame(Frame frame, String message) {
    String escaped = SafeHtmlUtils.htmlEscape(message);
    String errorHtml = "<html><body style=\"margin:0;padding:16px;font-family:sans-serif;color:#a33;\">"
      + escaped + "</body></html>";
    frame.getElement().setAttribute("srcdoc", errorHtml);
  }

  private static native void triggerClick(com.google.gwt.dom.client.Element element) /*-{
    element.click();
  }-*/;

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
    com.google.gwt.dom.client.Element fileInput, String url, com.google.gwt.dom.client.Element iframe,
    String fileTooLargeMsg, String transformFailedMsg, String uploadErrorMsg, String timeoutMsg) /*-{
    var files = fileInput.files;
    if (!files || files.length === 0) {
      return;
    }
    var file = files[0];
    var maxSize = 1024 * 1024;
    if (file.size > maxSize) {
      $wnd.alert(fileTooLargeMsg);
      return;
    }
    var self = this;
    var formData = new FormData();
    formData.append("xslt", file);
    var xhr = new XMLHttpRequest();
    xhr.open("POST", url, true);
    xhr.timeout = 30000;
    xhr.onload = function() {
      if (xhr.status === 200) {
        iframe.setAttribute("srcdoc", xhr.responseText);
        // Cache the rendered HTML keyed by filename so re-selecting this entry
        // in the dropdown avoids a second POST.
        self.__xsltCache = self.__xsltCache || {};
        self.__xsltCache[file.name] = xhr.responseText;
      } else {
        $wnd.alert(transformFailedMsg + xhr.statusText);
      }
    };
    xhr.onerror = function() {
      $wnd.alert(uploadErrorMsg);
    };
    xhr.ontimeout = function() {
      $wnd.alert(timeoutMsg);
    };
    xhr.send(formData);
  }-*/;

  private native String getCachedXslt(String filename) /*-{
    return (this.__xsltCache && this.__xsltCache[filename]) || null;
  }-*/;

  private native void applyCachedXslt(com.google.gwt.dom.client.Element iframe, String html) /*-{
    iframe.setAttribute("srcdoc", html);
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

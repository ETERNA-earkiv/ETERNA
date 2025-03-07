package org.roda.wui.client.redact;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import elemental2.dom.Blob;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

@JsType(name="PDFRedactor", namespace=JsPackage.GLOBAL)
public class PDFRedactorObject {
  @JsFunction
  public interface SaveCallback {
    public abstract boolean onInvoke(Blob pdfData);
  }

  @JsMethod
  public static native void render(Element root);

  @JsMethod
  public static native void setUrl(String url);

  @JsMethod
  public static native void setSaveCallback(SaveCallback callback);

  /*
  public static void test() {
    BrowserService.Util.getInstance().createRepresentation();
    BrowserService.Util.getInstance().createFolder();
    BrowserService.Util.getInstance().find();
    RestUtils.createFileUploadUri()
  }
  */
}


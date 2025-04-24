package org.roda.wui.client.redact;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

public class PDFRedactorPanel extends Composite {
  private static final Binder uiBinder = GWT.create(Binder.class);

  interface Binder extends UiBinder<Widget, PDFRedactorPanel> {
  }

  @UiField
  SimplePanel content;

  public PDFRedactorPanel() {
    initWidget(uiBinder.createAndBindUi(this));
  }

  public void setUrl(String url) {
    PDFRedactorObject.setUrl(url);
  }

  public void mount() {
    PDFRedactorObject.mount(content.getElement());
  }

  public void unmount() {
    PDFRedactorObject.unmount();
  }

  public void setSaveCallback(PDFRedactorObject.SaveCallback callback) { PDFRedactorObject.setSaveCallback(callback); }
}

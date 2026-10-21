/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.client.main;

import org.roda.core.data.common.RodaConstants;
import org.roda.wui.common.client.tools.ConfigurationManager;
import org.roda.wui.common.client.widgets.HTMLWidgetWrapper;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;

public class Footer extends Composite {

  private static final String VERSION_CLASS = "eterna-footer__version";

  private FlowPanel panel;
  private HTMLWidgetWrapper layout;

  public Footer() {
    super();

    panel = new FlowPanel();
    layout = new HTMLWidgetWrapper("Footer.html", null, RodaConstants.ResourcesTypes.INTERNAL,
      new AsyncCallback<Void>() {
        @Override
        public void onFailure(Throwable caught) {
          // do nothing
        }

        @Override
        public void onSuccess(Void result) {
          showVersion();
        }
      });

    panel.add(layout);
    initWidget(layout);
  }

  private void showVersion() {
    String version = ConfigurationManager.getString("ui", "version");
    if (version == null) {
      return;
    }
    NodeList<Element> spans = layout.getElement().getElementsByTagName("span");
    for (int i = 0; i < spans.getLength(); i++) {
      Element span = spans.getItem(i);
      if (span.hasClassName(VERSION_CLASS)) {
        span.setInnerText("v" + version);
      }
    }
  }
}

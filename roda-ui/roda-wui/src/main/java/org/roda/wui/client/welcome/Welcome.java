/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/keeps/roda
 */
/**
 *
 */
package org.roda.wui.client.welcome;

import java.util.Arrays;
import java.util.List;

import org.roda.core.data.v2.user.User;
import org.roda.wui.client.common.UserLogin;
import org.roda.wui.client.main.Login;
import org.roda.wui.common.client.HistoryResolver;
import org.roda.wui.common.client.tools.ConfigurationManager;
import org.roda.wui.common.client.tools.HistoryUtils;
import org.roda.wui.common.client.widgets.HTMLWidgetWrapper;

import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author Luis Faria
 *
 */
public class Welcome {

  public static final HistoryResolver RESOLVER = new HistoryResolver() {

    @Override
    public void resolve(List<String> historyTokens, AsyncCallback<Widget> callback) {
      getInstance().resolve(historyTokens, callback);
    }

    @Override
    public void isCurrentUserPermitted(AsyncCallback<Boolean> callback) {
      callback.onSuccess(Boolean.TRUE);
    }

    @Override
    public String getHistoryToken() {
      return "welcome";
    }

    @Override
    public List<String> getHistoryPath() {
      return Arrays.asList(getHistoryToken());
    }
  };

  private static Welcome instance = null;

  /**
   * Get the singleton instance
   *
   * @return the instance
   */
  public static Welcome getInstance() {
    if (instance == null) {
      instance = new Welcome();
    }
    return instance;
  }

  private boolean initialized;

  private FlowPanel layout;

  private Welcome() {
    initialized = false;
  }

  private void init() {
    if (!initialized) {
      initialized = true;

      layout = new FlowPanel();
      layout.addStyleName("wui-home");

      FlowPanel outer = new FlowPanel();
      outer.addStyleName("eterna-welcome");

      // Hero section (static HTML, locale-aware)
      outer.add(new HTMLWidgetWrapper("WelcomeHero.html"));

      // Quick actions header — placed above the grid so aside aligns with cards
      Label actionsHeader = new Label("Snabbåtgärder");
      actionsHeader.addStyleName("eterna-section-header");
      outer.add(actionsHeader);

      // Grid: main column + aside
      FlowPanel grid = new FlowPanel();
      grid.addStyleName("eterna-welcome__grid");

      FlowPanel main = new FlowPanel();
      main.addStyleName("eterna-welcome__main");

      // Action cards built from roda-wui.properties
      main.add(buildActionCards());

      // Activity section (static HTML, locale-aware)
      main.add(new HTMLWidgetWrapper("WelcomeActivity.html"));

      grid.add(main);

      // Aside (static HTML, locale-aware)
      FlowPanel aside = new FlowPanel();
      aside.addStyleName("eterna-welcome__aside");
      aside.add(new HTMLWidgetWrapper("WelcomeAside.html"));
      grid.add(aside);

      outer.add(grid);
      layout.add(outer);
    }
  }

  private FlowPanel buildActionCards() {
    FlowPanel container = new FlowPanel();
    container.addStyleName("eterna-actions");

    List<String> actionIds = ConfigurationManager.getStringList("ui", "welcome", "actions");
    for (String id : actionIds) {
      String title = ConfigurationManager.getString("ui", "welcome", "action", id, "title");
      String href = ConfigurationManager.getString("ui", "welcome", "action", id, "href");
      String icon = ConfigurationManager.getString("ui", "welcome", "action", id, "icon");
      String desc = ConfigurationManager.getString("ui", "welcome", "action", id, "description");

      if (title == null || href == null) {
        continue;
      }

      Anchor card = new Anchor();
      card.addStyleName("eterna-action");
      card.setHref(href);

      SafeHtmlBuilder sb = new SafeHtmlBuilder();
      if (icon != null && !icon.isEmpty()) {
        sb.appendHtmlConstant("<span class=\"eterna-action__icon\" aria-hidden=\"true\">");
        sb.appendEscaped(icon);
        sb.appendHtmlConstant("</span>");
      }
      sb.appendHtmlConstant("<span class=\"eterna-action__title\">");
      sb.appendEscaped(title);
      sb.appendHtmlConstant("</span>");
      if (desc != null && !desc.isEmpty()) {
        sb.appendHtmlConstant("<span class=\"eterna-action__desc\">");
        sb.appendEscaped(desc);
        sb.appendHtmlConstant("</span>");
      }
      card.setHTML(sb.toSafeHtml());
      container.add(card);
    }

    return container;
  }

  public void resolve(List<String> historyTokens, AsyncCallback<Widget> callback) {
    if (historyTokens.isEmpty()) {
      UserLogin.getInstance().getAuthenticatedUser(new AsyncCallback<User>() {
        @Override
        public void onFailure(Throwable caught) {
          HistoryUtils.newHistory(Login.RESOLVER);
          callback.onSuccess(null);
        }

        @Override
        public void onSuccess(User user) {
          if (user.isGuest()) {
            HistoryUtils.newHistory(Login.RESOLVER);
            callback.onSuccess(null);
          } else {
            init();
            callback.onSuccess(layout);
          }
        }
      });
    } else {
      HistoryUtils.newHistory(Welcome.RESOLVER);
      callback.onSuccess(null);
    }
  }

}

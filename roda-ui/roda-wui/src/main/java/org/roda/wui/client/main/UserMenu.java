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
package org.roda.wui.client.main;

import java.util.ArrayList;
import java.util.List;

import org.roda.core.data.v2.user.User;
import org.roda.wui.client.common.UserLogin;
import org.roda.wui.client.management.Profile;
import org.roda.wui.client.management.Register;
import org.roda.wui.common.client.ClientLogger;
import org.roda.wui.common.client.tools.HistoryUtils;
import org.roda.wui.common.client.widgets.wcag.AcessibleMenuBar;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.Widget;

import config.i18n.client.ClientMessages;

/**
 * @author Alexandre Flores
 *
 */
public class UserMenu extends Composite {

  private ClientLogger logger = new ClientLogger(getClass().getName());
  private static final ClientMessages messages = GWT.create(ClientMessages.class);
  private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

  interface MyUiBinder extends UiBinder<Widget, UserMenu> {
  }

  private AcessibleMenuBar profileDropdown;
  private final List<MenuItem> activeItems = new ArrayList<>();

  /**
   * User menu constructor
   *
   */
  public UserMenu() {
    initWidget(uiBinder.createAndBindUi(this));
  }

  public void init(AcessibleMenuBar navMenu) {
    profileDropdown = new AcessibleMenuBar(true);
    profileDropdown.addStyleName("userMenuColors");
    MenuItem profile = profileDropdown.addItem(messages.loginProfile(), createCommand(Profile.RESOLVER.getHistoryPath()));
    profile.addStyleName("profile_user_item");
    MenuItem logout = profileDropdown.addItem(messages.loginLogout(), () -> UserLogin.getInstance().logout());
    logout.addStyleName("login_user_item");

    UserLogin.getInstance().getAuthenticatedUser(new AsyncCallback<User>() {

      @Override
      public void onFailure(Throwable caught) {
        logger.fatal("Error getting Authenticated user", caught);
      }

      @Override
      public void onSuccess(User user) {
        updateVisibles(navMenu, user);
      }
    });

    UserLogin.getInstance().addLoginStatusListener(user -> updateVisibles(navMenu, user));
  }

  private ScheduledCommand createCommand(final List<String> path) {
    return () -> HistoryUtils.newHistory(path);
  }

  private ScheduledCommand createLoginCommand() {
    return () -> UserLogin.getInstance().login();
  }

  private void updateVisibles(AcessibleMenuBar navMenu, User user) {
    for (MenuItem item : activeItems) {
      navMenu.removeItem(item);
    }
    activeItems.clear();

    if (user.isGuest()) {
      MenuItem loginItem = customMenuItem("fa fa-user", messages.loginLogin(), "navigationMenu-item-label", null,
        createLoginCommand());
      loginItem.addStyleName("user_menu_item");
      navMenu.addItem(loginItem);
      activeItems.add(loginItem);

      MenuItem registerItem = customMenuItem("fa fa-user-plus", messages.loginRegister(),
        "navigationMenu-item-label navigationMenu-register", null, createCommand(Register.RESOLVER.getHistoryPath()));
      registerItem.addStyleName("user_menu_item_register");
      navMenu.addItem(registerItem);
      activeItems.add(registerItem);
    } else {
      String name = user.getName();
      String displayName = name.isEmpty() ? name
        : Character.toUpperCase(name.charAt(0)) + name.substring(1);
      SafeHtmlBuilder b = new SafeHtmlBuilder();
      b.append(SafeHtmlUtils.fromSafeConstant("<i class='fa fa-user'></i>"));
      b.appendEscaped(displayName);
      MenuItem userItem = new MenuItem(b.toSafeHtml(), profileDropdown);
      userItem.addStyleName("navigationMenu-item");
      userItem.addStyleName("navigationMenu-item-label");
      userItem.addStyleName("user_menu_item");
      navMenu.addItem(userItem);
      activeItems.add(userItem);
    }
  }

  private MenuItem customMenuItem(String icon, String label, String styleNames, MenuBar subMenu,
    ScheduledCommand command) {
    SafeHtmlBuilder b = new SafeHtmlBuilder();
    b.append(SafeHtmlUtils.fromSafeConstant("<i class='" + icon + "'></i>"));
    if (label != null) {
      b.append(SafeHtmlUtils.fromSafeConstant(label));
    }

    MenuItem menuItem;
    if (subMenu != null) {
      menuItem = new MenuItem(b.toSafeHtml(), subMenu);
    } else if (command != null) {
      menuItem = new MenuItem(b.toSafeHtml(), command);
    } else {
      menuItem = new MenuItem(b.toSafeHtml());
    }
    menuItem.addStyleName("navigationMenu-item");
    menuItem.addStyleName(styleNames);

    return menuItem;
  }

}

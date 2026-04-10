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
package org.roda.wui.client.management;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;

import org.roda.wui.common.client.ClientLogger;
import org.roda.wui.common.client.tools.ConfigurationManager;
import org.roda.wui.common.client.widgets.LoadingPopup;
import org.roda.wui.common.client.widgets.wcag.WCAGUtilities;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import config.i18n.client.ClientMessages;

/**
 * @author Luis Faria
 *
 */
public class PermissionsPanel extends FlowPanel implements HasValueChangeHandlers<List<String>> {
  public enum PermissionsMode {
    EDIT,
    READ_ONLY
  }
  private PermissionsMode mode = PermissionsMode.EDIT;

  private class Permission extends HorizontalPanel implements HasValueChangeHandlers<String> {

    private final String role;
    private boolean locked;
    private final CheckBox checkbox;
    private final Label descriptionLabel;

    public Permission(String role, String description) {
      this.role = role;
      this.checkbox = new CheckBox();
      this.descriptionLabel = new Label(description);
      this.add(checkbox);
      this.add(descriptionLabel);
      this.locked = false;
      descriptionLabel.addClickHandler(event -> {
        if (isEditable()) {
          checkbox.setValue(!checkbox.getValue(), true);
        }
      });

      checkbox.addValueChangeHandler(event -> {
        if (isEditable()) {
          onChange();
        }
      });

      this.addStyleName("permission");
      checkbox.addStyleName("permission-checkbox");
      descriptionLabel.setStylePrimaryName("permission-description");
      WCAGUtilities.addTitleToCheckbox(checkbox, description);
    }

    private boolean isEditable() {
      return mode == PermissionsMode.EDIT && !locked;
    }

    public void setLocked(boolean locked) {
      this.locked = locked;
      updateEnabledState();
    }

    public boolean isLocked() {
      return locked;
    }

    public boolean isChecked() {
      return checkbox.getValue();
    }

    public void setChecked(boolean checked) {
      checkbox.setValue(checked, false);
    }

    public void updateEnabledState() {
      checkbox.setEnabled(isEditable());
    }

    public String getRole() {
      return role;
    }

    @Override
    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> handler) {
      return addHandler(handler, ValueChangeEvent.getType());
    }

    protected void onChange() {
      ValueChangeEvent.fire(this, getRole());
    }
  }

  private static final ClientMessages messages = GWT.create(ClientMessages.class);
  private final ClientLogger logger = new ClientLogger(getClass().getName());

  private final List<Permission> permissions = new ArrayList<>();
  private final Map<String, List<Permission>> permissionGroups = new LinkedHashMap<>();
  private final Map<String, CheckBox> selectAllCheckboxes = new LinkedHashMap<>();

  private final List<String> userSelections = new ArrayList<>();
  private final LoadingPopup loading;
  private final Label readOnlyBadge;

  public PermissionsPanel() {
    loading = new LoadingPopup(this);
    loading.show();
    this.addStyleName("permissions");
    readOnlyBadge = new Label(messages.permissionsReadOnly());
    readOnlyBadge.addStyleName("permissions-readonly-badge");
    readOnlyBadge.setVisible(false);
    this.add(readOnlyBadge);
  }

  public void setMode(PermissionsMode mode) {
    this.mode = mode;
    boolean isReadOnly = (mode == PermissionsMode.READ_ONLY);
    if (isReadOnly) {
      this.addStyleName("permissions--readonly");
    } else {
      this.removeStyleName("permissions--readonly");
    }
    readOnlyBadge.setVisible(isReadOnly);
    for (Permission p : permissions) {
      p.updateEnabledState();
    }
    for (CheckBox cb : selectAllCheckboxes.values()) {
      cb.setEnabled(!isReadOnly);
    }
  }

  public void init(final AsyncCallback<Boolean> callback) {
    Map<String, FlowPanel> rolePanels = new LinkedHashMap<>();
    List<String> roleTitleKeys = ConfigurationManager.getStringList("ui.roleTitle");
    List<String> roles = ConfigurationManager.getStringList("ui.role");

    for (String roleKey : roleTitleKeys) {

      FlowPanel rolePanel = new FlowPanel();
      rolePanel.addStyleName("permission-role-container");

      String roleTitle;
      try {
        roleTitle = messages.roleTitle(roleKey);
      } catch (MissingResourceException e) {
        roleTitle = roleKey;
      }

      FlowPanel headerPanel = new FlowPanel();
      headerPanel.addStyleName("permission-role-container-title");
      headerPanel.addStyleName("permission-header-flex");

      CheckBox selectAllCheckbox = new CheckBox();
      selectAllCheckbox.addStyleName("permission-select-all-checkbox");
      WCAGUtilities.addTitleToCheckbox(selectAllCheckbox, roleTitle + " \u2013 select all");

      Label roleLabel = new Label(roleTitle);

      headerPanel.add(selectAllCheckbox);
      headerPanel.add(roleLabel);
      rolePanel.add(headerPanel);

      selectAllCheckboxes.put(roleKey, selectAllCheckbox);
      rolePanels.put(roleKey, rolePanel);
    }

    for (String role : roles) {
      String description;
      try {
        description = messages.role(role);
      } catch (MissingResourceException e) {
        description = role;
      }

      Permission permission = new Permission(role, description);
      permissions.add(permission);

      String roleKey = role.contains(".")
              ? role.substring(0, role.indexOf('.'))
              : "other";
      FlowPanel rolePanel = rolePanels.get(roleKey);
      if (rolePanel == null) {
        rolePanel = new FlowPanel();
        rolePanel.addStyleName("permission-role-container");
        rolePanels.put(roleKey, rolePanel);
        this.add(rolePanel);
      }
      rolePanel.add(permission);
      permissionGroups.computeIfAbsent(roleKey, k -> new ArrayList<>()).add(permission);

      permission.addValueChangeHandler(event -> {

        if (permission.isChecked()) {
          if (!userSelections.contains(permission.getRole())) {
            userSelections.add(permission.getRole());
          }
        } else {
          userSelections.remove(permission.getRole());
        }
        updateSelectAllCheckboxState(roleKey);
        onChange();
      });
    }

    for (String key : roleTitleKeys) {
      FlowPanel rp = rolePanels.get(key);
      if (rp != null) this.add(rp);
    }
    for (Map.Entry<String, List<Permission>> entry : permissionGroups.entrySet()) {
      String roleKey = entry.getKey();
      List<Permission> groupPermissions = entry.getValue();
      CheckBox headerCheckbox = selectAllCheckboxes.get(roleKey);
      if (headerCheckbox == null) continue;   // unknown group — no select-all header

      headerCheckbox.addValueChangeHandler(event -> {
        if (mode != PermissionsMode.EDIT) return;
        boolean newState = Boolean.TRUE.equals(event.getValue());
        for (Permission p : groupPermissions) {
          if (!p.isLocked()) {
            p.setChecked(newState);
            if (newState) {
              if (!userSelections.contains(p.getRole())) {
                userSelections.add(p.getRole());
              }
            } else {
              userSelections.remove(p.getRole());
            }
          }
        }
        updateSelectAllCheckboxState(roleKey);
        onChange();
      });
    }

    loading.hide();
    callback.onSuccess(true);
  }

  private void updateSelectAllCheckboxState(String roleKey) {

    List<Permission> group = permissionGroups.get(roleKey);
    CheckBox headerCheckbox = selectAllCheckboxes.get(roleKey);

    if (group == null || headerCheckbox == null) return;

    long total = group.size();
    long selected = group.stream().filter(Permission::isChecked).count();

    boolean allLocked = group.stream().allMatch(Permission::isLocked);
    boolean allChecked = selected == total && total > 0;

    com.google.gwt.dom.client.Node firstChildNode = headerCheckbox.getElement().getFirstChild();
    if (firstChildNode == null) return;
    InputElement input = firstChildNode.cast();
    input.setPropertyBoolean("indeterminate", false);

    if (allChecked) {
      headerCheckbox.setValue(true, false);
    } else if (selected == 0) {
      headerCheckbox.setValue(false, false);
    } else {
      headerCheckbox.setValue(false, false);
      input.setPropertyBoolean("indeterminate", true);
    }

    if (allChecked && allLocked) {
      headerCheckbox.setEnabled(false);
    } else {
      headerCheckbox.setEnabled(mode == PermissionsMode.EDIT);
    }
  }

  public void checkPermissions(Set<String> roles) {
    checkPermissions(roles, false);
  }

  public void checkPermissions(Set<String> roles, boolean lock) {
    for (Permission p : permissions) {
      if (roles.contains(p.getRole())) {
        p.setChecked(true);
        p.setLocked(lock);
        if (!lock && !userSelections.contains(p.getRole())) {
          userSelections.add(p.getRole());
        }
      }
    }
    for (String roleKey : permissionGroups.keySet()) {
      updateSelectAllCheckboxState(roleKey);
    }
  }

  public void clear() {
    for (Permission p : permissions) {
      p.setChecked(false);
      p.setLocked(false);
    }
  }

  public Set<String> getDirectRoles() {
    Set<String> result = new HashSet<>();
    for (Permission p : permissions) {
      if (p.isChecked() && !p.isLocked()) {
        result.add(p.getRole());
      }
    }
    return result;
  }

  public List<String> getUserSelections() {
    return userSelections;
  }

  @Override
  public HandlerRegistration addValueChangeHandler(ValueChangeHandler<List<String>> handler) {
    return addHandler(handler, ValueChangeEvent.getType());
  }

  protected void onChange() {
    ValueChangeEvent.fire(this, userSelections);
  }
}

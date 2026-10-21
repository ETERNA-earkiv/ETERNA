/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.data.v2.user;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Ett område i rollkatalogen (ui.roleTitle) med de
 * rättigheter (ui.role) som hör dit, t.ex. {@code aip} → {@code aip.read},
 * {@code aip.update}. Används som radgruppering i behörighetsöversiktens
 * matris. GWT-säker (bara java.util).
 */
public class RoleArea implements Serializable {

  @Serial
  private static final long serialVersionUID = 6467350711458016137L;

  private String key;
  private List<String> roles = new ArrayList<>();

  public RoleArea() {
    super();
  }

  public RoleArea(String key, List<String> roles) {
    this();
    this.key = key;
    setRoles(roles);
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public List<String> getRoles() {
    return roles;
  }

  public void setRoles(List<String> roles) {
    this.roles = roles != null ? roles : new ArrayList<>();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    RoleArea that = (RoleArea) o;
    return Objects.equals(key, that.key) && Objects.equals(roles, that.roles);
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, roles);
  }

  @Override
  public String toString() {
    return "RoleArea{" + "key='" + key + '\'' + ", roles=" + roles + '}';
  }
}

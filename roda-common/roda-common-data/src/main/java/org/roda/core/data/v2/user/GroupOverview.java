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
 * En grupp i behörighetsöversikten med direkta och
 * effektiva rättigheter samt medlemmar. Alla listor är sorterade. GWT-säker
 * (bara java.util).
 */
public class GroupOverview implements Serializable {

  @Serial
  private static final long serialVersionUID = -2359013367130598744L;

  private String name;
  private String fullName;
  private boolean active;
  private List<String> directRoles = new ArrayList<>();
  private List<String> allRoles = new ArrayList<>();
  private List<String> users = new ArrayList<>();

  public GroupOverview() {
    super();
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public List<String> getDirectRoles() {
    return directRoles;
  }

  public void setDirectRoles(List<String> directRoles) {
    this.directRoles = directRoles != null ? directRoles : new ArrayList<>();
  }

  public List<String> getAllRoles() {
    return allRoles;
  }

  public void setAllRoles(List<String> allRoles) {
    this.allRoles = allRoles != null ? allRoles : new ArrayList<>();
  }

  public List<String> getUsers() {
    return users;
  }

  public void setUsers(List<String> users) {
    this.users = users != null ? users : new ArrayList<>();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    GroupOverview that = (GroupOverview) o;
    return active == that.active && Objects.equals(name, that.name) && Objects.equals(fullName, that.fullName)
      && Objects.equals(directRoles, that.directRoles) && Objects.equals(allRoles, that.allRoles)
      && Objects.equals(users, that.users);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, fullName, active, directRoles, allRoles, users);
  }

  @Override
  public String toString() {
    return "GroupOverview{" + "name='" + name + '\'' + ", fullName='" + fullName + '\'' + ", active=" + active
      + ", directRoles=" + directRoles + ", allRoles=" + allRoles + ", users=" + users + '}';
  }
}

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
 * En användare i behörighetsöversikten med grupper,
 * direkta och effektiva rättigheter. Alla listor är sorterade. GWT-säker
 * (bara java.util).
 */
public class UserOverview implements Serializable {

  @Serial
  private static final long serialVersionUID = 1372449830185930212L;

  private String name;
  private String fullName;
  private String email;
  private boolean active;
  private List<String> groups = new ArrayList<>();
  private List<String> directRoles = new ArrayList<>();
  private List<String> allRoles = new ArrayList<>();

  public UserOverview() {
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

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public List<String> getGroups() {
    return groups;
  }

  public void setGroups(List<String> groups) {
    this.groups = groups != null ? groups : new ArrayList<>();
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

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    UserOverview that = (UserOverview) o;
    return active == that.active && Objects.equals(name, that.name) && Objects.equals(fullName, that.fullName)
      && Objects.equals(email, that.email) && Objects.equals(groups, that.groups)
      && Objects.equals(directRoles, that.directRoles) && Objects.equals(allRoles, that.allRoles);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, fullName, email, active, groups, directRoles, allRoles);
  }

  @Override
  public String toString() {
    return "UserOverview{" + "name='" + name + '\'' + ", fullName='" + fullName + '\'' + ", email='" + email + '\''
      + ", active=" + active + ", groups=" + groups + ", directRoles=" + directRoles + ", allRoles=" + allRoles + '}';
  }
}

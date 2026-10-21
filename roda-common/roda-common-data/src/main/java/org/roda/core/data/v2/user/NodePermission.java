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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Objektbehörigheterna för en strukturenhet (AIP på
 * nivå fonds/class/series …) i behörighetsöversikten. Nycklarna i
 * {@code groupsByType}/{@code usersByType} är
 * {@code Permissions.PermissionType.name()} (CREATE, READ, UPDATE, DELETE,
 * GRANT); värdena är sorterade namnlistor. {@code restrictedGroups} är de
 * grupper som åtkomstreglerna spärrat (AccessRuleState.restrictedGroups),
 * tom när reglerna är avstängda eller inget spärrats. GWT-säker (bara
 * java.util).
 */
public class NodePermission implements Serializable {

  @Serial
  private static final long serialVersionUID = 7705384416126934019L;

  private String aipId;
  private String level;
  private String title;
  private String parentId;
  private Map<String, List<String>> groupsByType = new LinkedHashMap<>();
  private Map<String, List<String>> usersByType = new LinkedHashMap<>();
  private List<String> restrictedGroups = new ArrayList<>();

  public NodePermission() {
    super();
  }

  public String getAipId() {
    return aipId;
  }

  public void setAipId(String aipId) {
    this.aipId = aipId;
  }

  public String getLevel() {
    return level;
  }

  public void setLevel(String level) {
    this.level = level;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getParentId() {
    return parentId;
  }

  public void setParentId(String parentId) {
    this.parentId = parentId;
  }

  public Map<String, List<String>> getGroupsByType() {
    return groupsByType;
  }

  public void setGroupsByType(Map<String, List<String>> groupsByType) {
    this.groupsByType = groupsByType != null ? groupsByType : new LinkedHashMap<>();
  }

  public Map<String, List<String>> getUsersByType() {
    return usersByType;
  }

  public void setUsersByType(Map<String, List<String>> usersByType) {
    this.usersByType = usersByType != null ? usersByType : new LinkedHashMap<>();
  }

  public List<String> getRestrictedGroups() {
    return restrictedGroups;
  }

  public void setRestrictedGroups(List<String> restrictedGroups) {
    this.restrictedGroups = restrictedGroups != null ? restrictedGroups : new ArrayList<>();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    NodePermission that = (NodePermission) o;
    return Objects.equals(aipId, that.aipId) && Objects.equals(level, that.level)
      && Objects.equals(title, that.title) && Objects.equals(parentId, that.parentId)
      && Objects.equals(groupsByType, that.groupsByType) && Objects.equals(usersByType, that.usersByType)
      && Objects.equals(restrictedGroups, that.restrictedGroups);
  }

  @Override
  public int hashCode() {
    return Objects.hash(aipId, level, title, parentId, groupsByType, usersByType, restrictedGroups);
  }

  @Override
  public String toString() {
    return "NodePermission{" + "aipId='" + aipId + '\'' + ", level='" + level + '\'' + ", title='" + title + '\''
      + ", parentId='" + parentId + '\'' + ", groupsByType=" + groupsByType + ", usersByType=" + usersByType
      + ", restrictedGroups=" + restrictedGroups + '}';
  }
}

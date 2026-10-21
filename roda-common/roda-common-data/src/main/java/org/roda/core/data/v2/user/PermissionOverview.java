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

import org.roda.core.data.v2.accessrules.AccessRule;

/**
 * Behörighetsöversikten – en ögonblicksbild av
 * rollkatalogen ({@link RoleArea}), grupper ({@link GroupOverview}),
 * användare ({@link UserOverview}), objektbehörigheter på strukturenheter
 * ({@link NodePermission}) och åtkomstreglerna ({@link AccessRule}).
 * {@code generatedAt} är en ISO-8601-sträng eftersom klassen
 * GWT-kompileras (java.time saknas). Alla listor är sorterade av byggaren.
 * {@code warnings} är tom när översikten är komplett; annars en läsbar rad
 * per avsnitt som inte kunde hämtas (t.ex. LDAP eller Solr nere) – rapporten
 * ska då inte användas som revisionsunderlag utan att det framgår.
 */
public class PermissionOverview implements Serializable {

  @Serial
  private static final long serialVersionUID = -8450931727193384063L;

  private String generatedAt;
  private String generatedBy;
  private List<RoleArea> roleAreas = new ArrayList<>();
  private List<GroupOverview> groups = new ArrayList<>();
  private List<UserOverview> users = new ArrayList<>();
  private List<NodePermission> nodes = new ArrayList<>();
  private List<AccessRule> rules = new ArrayList<>();
  private boolean rulesEnabled;
  private List<String> warnings = new ArrayList<>();

  public PermissionOverview() {
    super();
  }

  public String getGeneratedAt() {
    return generatedAt;
  }

  public void setGeneratedAt(String generatedAt) {
    this.generatedAt = generatedAt;
  }

  public String getGeneratedBy() {
    return generatedBy;
  }

  public void setGeneratedBy(String generatedBy) {
    this.generatedBy = generatedBy;
  }

  public List<RoleArea> getRoleAreas() {
    return roleAreas;
  }

  public void setRoleAreas(List<RoleArea> roleAreas) {
    this.roleAreas = roleAreas != null ? roleAreas : new ArrayList<>();
  }

  public List<GroupOverview> getGroups() {
    return groups;
  }

  public void setGroups(List<GroupOverview> groups) {
    this.groups = groups != null ? groups : new ArrayList<>();
  }

  public List<UserOverview> getUsers() {
    return users;
  }

  public void setUsers(List<UserOverview> users) {
    this.users = users != null ? users : new ArrayList<>();
  }

  public List<NodePermission> getNodes() {
    return nodes;
  }

  public void setNodes(List<NodePermission> nodes) {
    this.nodes = nodes != null ? nodes : new ArrayList<>();
  }

  public List<AccessRule> getRules() {
    return rules;
  }

  public void setRules(List<AccessRule> rules) {
    this.rules = rules != null ? rules : new ArrayList<>();
  }

  public boolean isRulesEnabled() {
    return rulesEnabled;
  }

  public void setRulesEnabled(boolean rulesEnabled) {
    this.rulesEnabled = rulesEnabled;
  }

  /** Läsbara varningar om avsnitt som inte kunde hämtas; tom när översikten är komplett. */
  public List<String> getWarnings() {
    return warnings;
  }

  public void setWarnings(List<String> warnings) {
    this.warnings = warnings != null ? warnings : new ArrayList<>();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    PermissionOverview that = (PermissionOverview) o;
    return rulesEnabled == that.rulesEnabled && Objects.equals(generatedAt, that.generatedAt)
      && Objects.equals(generatedBy, that.generatedBy) && Objects.equals(roleAreas, that.roleAreas)
      && Objects.equals(groups, that.groups) && Objects.equals(users, that.users) && Objects.equals(nodes, that.nodes)
      && Objects.equals(rules, that.rules) && Objects.equals(warnings, that.warnings);
  }

  @Override
  public int hashCode() {
    return Objects.hash(generatedAt, generatedBy, roleAreas, groups, users, nodes, rules, rulesEnabled, warnings);
  }

  @Override
  public String toString() {
    return "PermissionOverview{" + "generatedAt='" + generatedAt + '\'' + ", generatedBy='" + generatedBy + '\''
      + ", roleAreas=" + roleAreas.size() + ", groups=" + groups.size() + ", users=" + users.size() + ", nodes="
      + nodes.size() + ", rules=" + rules.size() + ", rulesEnabled=" + rulesEnabled + ", warnings=" + warnings + '}';
  }
}

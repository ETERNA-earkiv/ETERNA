/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.data.v2.accessrules;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Hela regeluppsättningen, dvs. innehållet i
 * konfigurationsfilen access-rules.json. Består av policyval
 * ({@link AccessRuleDefaults}) och en lista med regler per grupp
 * ({@link AccessRule}). {@code version} räknas upp vid varje sparning och
 * stämplas på AIP:er när reglerna tillämpas, så att "drift" (AIP:er som
 * utvärderats mot en äldre version) kan upptäckas.
 *
 * Tidsstämplar lagras som ISO-8601-strängar eftersom klassen GWT-kompileras
 * (java.time är inte tillgängligt).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccessRuleSet implements Serializable {

  @Serial
  private static final long serialVersionUID = 3059172118296640711L;

  private int version;
  private boolean enabled;
  private AccessRuleDefaults defaults = new AccessRuleDefaults();
  private List<AccessRule> rules = new ArrayList<>();
  private String updatedBy;
  private String updatedOn;

  public AccessRuleSet() {
    super();
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public AccessRuleDefaults getDefaults() {
    return defaults;
  }

  public void setDefaults(AccessRuleDefaults defaults) {
    this.defaults = defaults != null ? defaults : new AccessRuleDefaults();
  }

  public List<AccessRule> getRules() {
    return rules;
  }

  public void setRules(List<AccessRule> rules) {
    this.rules = rules != null ? rules : new ArrayList<>();
  }

  public String getUpdatedBy() {
    return updatedBy;
  }

  public void setUpdatedBy(String updatedBy) {
    this.updatedBy = updatedBy;
  }

  public String getUpdatedOn() {
    return updatedOn;
  }

  public void setUpdatedOn(String updatedOn) {
    this.updatedOn = updatedOn;
  }

  /**
   * Hittar regeln för en grupp (även om regeln är avstängd).
   *
   * @param group
   *          gruppnamn
   * @return regeln eller {@code null} om gruppen saknar regel
   */
  public AccessRule findRule(String group) {
    if (group != null && rules != null) {
      for (AccessRule rule : rules) {
        if (group.equals(rule.getGroup())) {
          return rule;
        }
      }
    }
    return null;
  }

  /**
   * Alla grupper som reglerna hanterar, dvs. {@code rules[].group} inklusive
   * avstängda regler (en avstängd regel gör fortfarande gruppen hanterad men
   * tillåter aldrig något). Ordningen följer regellistan.
   *
   * @return hanterade gruppnamn (aldrig {@code null})
   */
  @JsonIgnore
  public Set<String> getManagedGroups() {
    Set<String> managed = new LinkedHashSet<>();
    if (rules != null) {
      for (AccessRule rule : rules) {
        if (rule.getGroup() != null && !rule.getGroup().isEmpty()) {
          managed.add(rule.getGroup());
        }
      }
    }
    return managed;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    AccessRuleSet that = (AccessRuleSet) o;
    return version == that.version && enabled == that.enabled && Objects.equals(defaults, that.defaults)
      && Objects.equals(rules, that.rules) && Objects.equals(updatedBy, that.updatedBy)
      && Objects.equals(updatedOn, that.updatedOn);
  }

  @Override
  public int hashCode() {
    return Objects.hash(version, enabled, defaults, rules, updatedBy, updatedOn);
  }

  @Override
  public String toString() {
    return "AccessRuleSet{" + "version=" + version + ", enabled=" + enabled + ", defaults=" + defaults + ", rules="
      + rules + ", updatedBy='" + updatedBy + '\'' + ", updatedOn='" + updatedOn + '\'' + '}';
  }
}

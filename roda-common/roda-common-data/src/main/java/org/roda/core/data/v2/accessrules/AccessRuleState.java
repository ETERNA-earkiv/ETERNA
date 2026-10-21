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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Per-AIP-tillstånd som regelmotorn lämnar efter
 * sig i AIP-JSON:en.
 *
 * <ul>
 * <li>{@code restrictedGroups}: hanterade grupper som fått READ borttagen av
 * reglerna – de kandiderar för att få READ tillbaka om metadata eller regler
 * ändras så att gruppen tillåts igen (reglerna ger aldrig behörighet till
 * grupper som inte haft READ eller inte är restricted).</li>
 * <li>{@code removedPermissions}: per hanterad grupp de skrivbehörigheter
 * (UPDATE/DELETE/GRANT) som reglerna tagit bort utöver READ, så att de kan
 * återställas när gruppen åter tillåts. Tom när inget tagits bort (eller när
 * core.access_rules.strip_write_on_deny = false).</li>
 * <li>{@code rulesVersion}/{@code appliedOn}: vilken regelversion AIP:n
 * senast utvärderades mot och när; {@code factsSource} anger varifrån fakta
 * hämtades ("self", förfaders AIP-id eller "none").</li>
 * </ul>
 *
 * Tidsstämplar lagras som ISO-8601-strängar eftersom klassen GWT-kompileras.
 * Mängderna hålls som LinkedHashSet/LinkedHashMap så att utskriften blir
 * stabil; ordningen garanteras dock inte efter JSON-inläsning. Okända fält
 * ignoreras vid inläsning så att en aip.json skriven av ett nyare bygge kan
 * läsas av detta.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccessRuleState implements Serializable {

  @Serial
  private static final long serialVersionUID = 1849036258071446615L;

  private Set<String> restrictedGroups = new LinkedHashSet<>();
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private Map<String, Set<String>> removedPermissions = new LinkedHashMap<>();
  private String rulesVersion;
  private String appliedOn;
  private String factsSource;

  public AccessRuleState() {
    super();
  }

  public Set<String> getRestrictedGroups() {
    return restrictedGroups;
  }

  public void setRestrictedGroups(Set<String> restrictedGroups) {
    this.restrictedGroups = restrictedGroups != null ? new LinkedHashSet<>(restrictedGroups) : new LinkedHashSet<>();
  }

  /** Grupp → borttagna skrivbehörigheter (namn på PermissionType). Aldrig null. */
  public Map<String, Set<String>> getRemovedPermissions() {
    return removedPermissions;
  }

  public void setRemovedPermissions(Map<String, Set<String>> removedPermissions) {
    this.removedPermissions = copyRemoved(removedPermissions);
  }

  public String getRulesVersion() {
    return rulesVersion;
  }

  public void setRulesVersion(String rulesVersion) {
    this.rulesVersion = rulesVersion;
  }

  public String getAppliedOn() {
    return appliedOn;
  }

  public void setAppliedOn(String appliedOn) {
    this.appliedOn = appliedOn;
  }

  public String getFactsSource() {
    return factsSource;
  }

  public void setFactsSource(String factsSource) {
    this.factsSource = factsSource;
  }

  /** Djup kopia (nya mängder) av en grupp→behörigheter-map; null ger tom map. */
  public static Map<String, Set<String>> copyRemoved(Map<String, Set<String>> source) {
    Map<String, Set<String>> copy = new LinkedHashMap<>();
    if (source != null) {
      for (Map.Entry<String, Set<String>> entry : source.entrySet()) {
        if (entry.getKey() != null && entry.getValue() != null && !entry.getValue().isEmpty()) {
          copy.put(entry.getKey(), new LinkedHashSet<>(entry.getValue()));
        }
      }
    }
    return copy;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    AccessRuleState that = (AccessRuleState) o;
    return Objects.equals(restrictedGroups, that.restrictedGroups)
      && Objects.equals(removedPermissions, that.removedPermissions) && Objects.equals(rulesVersion, that.rulesVersion)
      && Objects.equals(appliedOn, that.appliedOn) && Objects.equals(factsSource, that.factsSource);
  }

  @Override
  public int hashCode() {
    return Objects.hash(restrictedGroups, removedPermissions, rulesVersion, appliedOn, factsSource);
  }

  @Override
  public String toString() {
    return "AccessRuleState{" + "restrictedGroups=" + restrictedGroups + ", removedPermissions=" + removedPermissions
      + ", rulesVersion='" + rulesVersion + '\'' + ", appliedOn='" + appliedOn + '\'' + ", factsSource='" + factsSource
      + '\'' + '}';
  }
}

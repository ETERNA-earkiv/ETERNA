/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.common.access;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Resultatet av att {@link AccessRuleEngine}
 * utvärderat en regeluppsättning mot ett AIP:s {@link EternaAdminFacts}.
 *
 * <ul>
 * <li>{@code managedGroups}: alla grupper som regelfilen nämner (även
 * avstängda regler). Bara dessa grupper får röras av
 * {@link PermissionsMerger}.</li>
 * <li>{@code allowedGroups}: delmängd av de hanterade grupperna som enligt
 * reglerna får läsa objektet.</li>
 * <li>{@code explanation}: en rad per hanterad grupp, i regelordning, med
 * svensk motivering ("socialtjanst: tillåten" / "handlaggare: nekad – ...").
 * Används i torrkörningsrapporter och PREMIS-händelser.</li>
 * </ul>
 *
 * Mängderna hålls som LinkedHashSet så att ordningen följer regellistan.
 */
public class AccessDecision implements Serializable {

  private static final long serialVersionUID = 1L;

  private Set<String> allowedGroups = new LinkedHashSet<>();
  private Set<String> managedGroups = new LinkedHashSet<>();
  private List<String> explanation = new ArrayList<>();

  public AccessDecision() {
    // bean
  }

  /** Hanterade grupper som får READ enligt reglerna (aldrig null). */
  public Set<String> getAllowedGroups() {
    return allowedGroups;
  }

  public void setAllowedGroups(Set<String> allowedGroups) {
    this.allowedGroups = allowedGroups == null ? new LinkedHashSet<>() : new LinkedHashSet<>(allowedGroups);
  }

  /** Alla grupper som regelfilen hanterar, även avstängda regler (aldrig null). */
  public Set<String> getManagedGroups() {
    return managedGroups;
  }

  public void setManagedGroups(Set<String> managedGroups) {
    this.managedGroups = managedGroups == null ? new LinkedHashSet<>() : new LinkedHashSet<>(managedGroups);
  }

  /** En motiveringsrad per hanterad grupp (aldrig null). */
  public List<String> getExplanation() {
    return explanation;
  }

  public void setExplanation(List<String> explanation) {
    this.explanation = explanation == null ? new ArrayList<>() : new ArrayList<>(explanation);
  }

  /** true om gruppen är hanterad och tillåten. */
  public boolean isAllowed(String group) {
    return group != null && allowedGroups.contains(group);
  }

  /** true om gruppen nämns i regelfilen. */
  public boolean isManaged(String group) {
    return group != null && managedGroups.contains(group);
  }

  @Override
  public String toString() {
    return "AccessDecision{allowedGroups=" + allowedGroups + ", managedGroups=" + managedGroups + ", explanation="
      + explanation + "}";
  }
}

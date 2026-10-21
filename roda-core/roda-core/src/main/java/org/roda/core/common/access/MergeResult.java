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

import org.roda.core.data.v2.accessrules.AccessRuleState;
import org.roda.core.data.v2.ip.Permissions;

/**
 * Resultatet av {@link PermissionsMerger#apply}.
 *
 * <ul>
 * <li>{@code permissions}: NY kopia av AIP:ns behörigheter med reglernas
 * ändringar införda (indata rörs aldrig).</li>
 * <li>{@code state}: nytt {@link AccessRuleState} att skriva på AIP:n —
 * uppdaterade restrictedGroups/removedPermissions samt alltid
 * rulesVersion/appliedOn.</li>
 * <li>{@code added}/{@code removed}: grupper som fått READ tillagd respektive
 * borttagen i denna körning.</li>
 * <li>{@code writeStripped}/{@code writeRestored}: grupper som fått
 * skrivbehörigheter (UPDATE/DELETE/GRANT) borttagna respektive återställda i
 * denna körning (bara när mergern körs med stripWriteOnDeny).</li>
 * <li>{@code changed}: true om behörigheterna, restrictedGroups eller
 * removedPermissions ändrats (inte bara stämpeln rulesVersion/appliedOn).</li>
 * <li>{@code warnings}: t.ex. grupp som förlorat READ men behåller
 * UPDATE/DELETE/GRANT, eller restricted grupp som inte längre har regel.</li>
 * </ul>
 */
public class MergeResult implements Serializable {

  private static final long serialVersionUID = 1L;

  private Permissions permissions;
  private AccessRuleState state;
  private Set<String> added = new LinkedHashSet<>();
  private Set<String> removed = new LinkedHashSet<>();
  private Set<String> writeStripped = new LinkedHashSet<>();
  private Set<String> writeRestored = new LinkedHashSet<>();
  private boolean changed;
  private List<String> warnings = new ArrayList<>();

  public MergeResult() {
    // bean
  }

  public Permissions getPermissions() {
    return permissions;
  }

  public void setPermissions(Permissions permissions) {
    this.permissions = permissions;
  }

  public AccessRuleState getState() {
    return state;
  }

  public void setState(AccessRuleState state) {
    this.state = state;
  }

  /** Grupper som fått READ tillagd (aldrig null). */
  public Set<String> getAdded() {
    return added;
  }

  public void setAdded(Set<String> added) {
    this.added = added == null ? new LinkedHashSet<>() : new LinkedHashSet<>(added);
  }

  /** Grupper som fått READ borttagen (aldrig null). */
  public Set<String> getRemoved() {
    return removed;
  }

  public void setRemoved(Set<String> removed) {
    this.removed = removed == null ? new LinkedHashSet<>() : new LinkedHashSet<>(removed);
  }

  /** Grupper som fått skrivbehörigheter borttagna (aldrig null). */
  public Set<String> getWriteStripped() {
    return writeStripped;
  }

  public void setWriteStripped(Set<String> writeStripped) {
    this.writeStripped = writeStripped == null ? new LinkedHashSet<>() : new LinkedHashSet<>(writeStripped);
  }

  /** Grupper som fått skrivbehörigheter återställda (aldrig null). */
  public Set<String> getWriteRestored() {
    return writeRestored;
  }

  public void setWriteRestored(Set<String> writeRestored) {
    this.writeRestored = writeRestored == null ? new LinkedHashSet<>() : new LinkedHashSet<>(writeRestored);
  }

  public boolean isChanged() {
    return changed;
  }

  public void setChanged(boolean changed) {
    this.changed = changed;
  }

  /** Varningar från sammanslagningen (aldrig null). */
  public List<String> getWarnings() {
    return warnings;
  }

  public void setWarnings(List<String> warnings) {
    this.warnings = warnings == null ? new ArrayList<>() : new ArrayList<>(warnings);
  }

  @Override
  public String toString() {
    return "MergeResult{changed=" + changed + ", added=" + added + ", removed=" + removed + ", writeStripped="
      + writeStripped + ", writeRestored=" + writeRestored + ", state=" + state + ", warnings=" + warnings + "}";
  }
}

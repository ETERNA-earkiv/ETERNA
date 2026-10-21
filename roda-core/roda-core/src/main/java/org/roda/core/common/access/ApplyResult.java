/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.common.access;

/**
 * Utfallet av {@link AccessRulesApplier#applyToAip}
 * för en AIP — hämtade fakta, motorns beslut, sammanslagningen mot
 * befintliga behörigheter samt om något skrevs.
 *
 * <ul>
 * <li>{@code skipped}: reglerna tillämpades inte alls (avstängda eller inga
 * regler); {@code skipReason} förklarar. Övriga fält är då null.</li>
 * <li>{@code writeNeeded}: behörigheter eller regeltillstånd skiljer sig
 * från det lagrade (även vid torrkörning).</li>
 * <li>{@code stateOnly}: bara regeltillståndet (rulesVersion m.m.) behöver
 * skrivas, inga behörigheter ändras.</li>
 * <li>{@code written}: AIP:n skrevs till modellen (alltid false vid
 * torrkörning).</li>
 * </ul>
 */
public class ApplyResult {

  private EternaAdminFacts facts;
  private AccessDecision decision;
  private MergeResult merge;
  private boolean skipped;
  private String skipReason;
  private boolean writeNeeded;
  private boolean stateOnly;
  private boolean written;

  public ApplyResult() {
    // bean
  }

  public EternaAdminFacts getFacts() {
    return facts;
  }

  void setFacts(EternaAdminFacts facts) {
    this.facts = facts;
  }

  public AccessDecision getDecision() {
    return decision;
  }

  void setDecision(AccessDecision decision) {
    this.decision = decision;
  }

  public MergeResult getMerge() {
    return merge;
  }

  void setMerge(MergeResult merge) {
    this.merge = merge;
  }

  public boolean isSkipped() {
    return skipped;
  }

  void setSkipped(boolean skipped) {
    this.skipped = skipped;
  }

  public String getSkipReason() {
    return skipReason;
  }

  void setSkipReason(String skipReason) {
    this.skipReason = skipReason;
  }

  public boolean isWriteNeeded() {
    return writeNeeded;
  }

  void setWriteNeeded(boolean writeNeeded) {
    this.writeNeeded = writeNeeded;
  }

  public boolean isStateOnly() {
    return stateOnly;
  }

  void setStateOnly(boolean stateOnly) {
    this.stateOnly = stateOnly;
  }

  public boolean isWritten() {
    return written;
  }

  void setWritten(boolean written) {
    this.written = written;
  }

  /** true om behörigheter (READ för hanterade grupper) ändrades/skulle ändras. */
  public boolean isPermissionsChanged() {
    return merge != null && merge.isChanged();
  }

  /**
   * Kort svensk sammanfattning för jobbrapporter och loggar, t.ex.
   * "fakta: egen; tillåtna: [socialtjanst]; READ borttagen: [handlaggare];
   * skrivet".
   */
  public String summary() {
    if (skipped) {
      return "hoppades över: " + skipReason;
    }
    StringBuilder sb = new StringBuilder();
    sb.append("fakta: ").append(describeFacts());
    if (decision != null) {
      sb.append("; tillåtna: ").append(decision.getAllowedGroups());
    }
    if (merge != null) {
      if (merge.getAdded() != null && !merge.getAdded().isEmpty()) {
        sb.append("; READ tillagd: ").append(merge.getAdded());
      }
      if (merge.getRemoved() != null && !merge.getRemoved().isEmpty()) {
        sb.append("; READ borttagen: ").append(merge.getRemoved());
      }
      if (merge.getWarnings() != null && !merge.getWarnings().isEmpty()) {
        sb.append("; varningar: ").append(merge.getWarnings());
      }
    }
    if (written) {
      sb.append(stateOnly ? "; regeltillstånd skrivet" : "; skrivet");
    } else if (writeNeeded) {
      sb.append(stateOnly ? "; regeltillstånd skulle skrivas" : "; skulle skrivas");
    } else {
      sb.append("; oförändrad");
    }
    return sb.toString();
  }

  private String describeFacts() {
    if (facts == null || !facts.isPresent()) {
      return "saknas";
    }
    if (facts.isInherited()) {
      return "ärvd från " + facts.getSourceAipId();
    }
    return "egen";
  }

  @Override
  public String toString() {
    return "ApplyResult{" + summary() + "}";
  }
}

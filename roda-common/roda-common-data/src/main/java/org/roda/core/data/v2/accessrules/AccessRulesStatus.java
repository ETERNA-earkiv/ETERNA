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
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Statusöversikt för behörighetsreglerna, avsedd
 * för administrationsgränssnittet/API:t. Visar om reglerna är aktiva, aktuell
 * version, när och av vilket jobb de senast tillämpades, vilka grupper som
 * hanteras samt hur många AIP:er som har "drift" (utvärderade mot en annan
 * regelversion än den aktuella). {@code driftCount} är {@code null} när
 * värdet inte beräknats.
 *
 * Tidsstämplar lagras som ISO-8601-strängar eftersom klassen GWT-kompileras.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccessRulesStatus implements Serializable {

  @Serial
  private static final long serialVersionUID = -7723045198115460934L;

  private boolean enabled;
  private int version;
  private String lastAppliedOn;
  private String lastJobId;
  private List<String> managedGroups = new ArrayList<>();
  private Integer driftCount;

  public AccessRulesStatus() {
    super();
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  public String getLastAppliedOn() {
    return lastAppliedOn;
  }

  public void setLastAppliedOn(String lastAppliedOn) {
    this.lastAppliedOn = lastAppliedOn;
  }

  public String getLastJobId() {
    return lastJobId;
  }

  public void setLastJobId(String lastJobId) {
    this.lastJobId = lastJobId;
  }

  public List<String> getManagedGroups() {
    return managedGroups;
  }

  public void setManagedGroups(List<String> managedGroups) {
    this.managedGroups = managedGroups != null ? managedGroups : new ArrayList<>();
  }

  public Integer getDriftCount() {
    return driftCount;
  }

  public void setDriftCount(Integer driftCount) {
    this.driftCount = driftCount;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    AccessRulesStatus that = (AccessRulesStatus) o;
    return enabled == that.enabled && version == that.version && Objects.equals(lastAppliedOn, that.lastAppliedOn)
      && Objects.equals(lastJobId, that.lastJobId) && Objects.equals(managedGroups, that.managedGroups)
      && Objects.equals(driftCount, that.driftCount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(enabled, version, lastAppliedOn, lastJobId, managedGroups, driftCount);
  }

  @Override
  public String toString() {
    return "AccessRulesStatus{" + "enabled=" + enabled + ", version=" + version + ", lastAppliedOn='" + lastAppliedOn
      + '\'' + ", lastJobId='" + lastJobId + '\'' + ", managedGroups=" + managedGroups + ", driftCount=" + driftCount
      + '}';
  }
}

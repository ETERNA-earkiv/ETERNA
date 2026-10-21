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
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Standardval (policy) för behörighetsregler som
 * styr hur regelmotorn hanterar ofullständig eller saknad eterna_admin-
 * metadata.
 *
 * <ul>
 * <li>{@code ejBedomd}: "closed" (standard) ⇒ sekretess/personuppgifter som
 * inte bedömts kräver att "ej_bedomd" listas uttryckligen i regeln; "open" ⇒
 * ej bedömda objekt tillåts alltid.</li>
 * <li>{@code missingMetadata}: vad som gäller när objektet helt saknar
 * eterna_admin. "inherit" (standard): ärv närmaste förfaders bedömning;
 * finns ingen bedömd förfader lämnas objektet öppet (obedömd struktur).
 * "inherit-closed": som inherit men ett objekt utan egen eller ärvd
 * bedömning behandlas som ej bedömt (stängt). "open"/"closed": ingen
 * arvslogik, alltid öppet respektive stängt.</li>
 * <li>{@code missingLagrum}: "closed" (standard) ⇒ sekretessbelagt objekt
 * utan tolkbart lagrum tillåts bara för regler med lagrum "*"; "open" ⇒
 * lagrumskravet hoppas över.</li>
 * <li>{@code multipleLagrum}: "all" (standard) ⇒ varje lagrum på objektet
 * måste täckas av regeln; "any" ⇒ det räcker att ett täcks.</li>
 * <li>{@code defaultAct}: lag som antas när lagrumstexten saknar lagnamn
 * ("21 kap. 7 §" tolkas som OSL 21:7).</li>
 * </ul>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccessRuleDefaults implements Serializable {

  @Serial
  private static final long serialVersionUID = 6235117460921358821L;

  private String ejBedomd = "closed";
  private String missingMetadata = "inherit";
  private String missingLagrum = "closed";
  private String multipleLagrum = "all";
  private String defaultAct = "OSL";

  public AccessRuleDefaults() {
    super();
  }

  public String getEjBedomd() {
    return ejBedomd;
  }

  public void setEjBedomd(String ejBedomd) {
    this.ejBedomd = ejBedomd;
  }

  public String getMissingMetadata() {
    return missingMetadata;
  }

  public void setMissingMetadata(String missingMetadata) {
    this.missingMetadata = missingMetadata;
  }

  public String getMissingLagrum() {
    return missingLagrum;
  }

  public void setMissingLagrum(String missingLagrum) {
    this.missingLagrum = missingLagrum;
  }

  public String getMultipleLagrum() {
    return multipleLagrum;
  }

  public void setMultipleLagrum(String multipleLagrum) {
    this.multipleLagrum = multipleLagrum;
  }

  public String getDefaultAct() {
    return defaultAct;
  }

  public void setDefaultAct(String defaultAct) {
    this.defaultAct = defaultAct;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    AccessRuleDefaults that = (AccessRuleDefaults) o;
    return Objects.equals(ejBedomd, that.ejBedomd) && Objects.equals(missingMetadata, that.missingMetadata)
      && Objects.equals(missingLagrum, that.missingLagrum) && Objects.equals(multipleLagrum, that.multipleLagrum)
      && Objects.equals(defaultAct, that.defaultAct);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ejBedomd, missingMetadata, missingLagrum, multipleLagrum, defaultAct);
  }

  @Override
  public String toString() {
    return "AccessRuleDefaults{" + "ejBedomd='" + ejBedomd + '\'' + ", missingMetadata='" + missingMetadata + '\''
      + ", missingLagrum='" + missingLagrum + '\'' + ", multipleLagrum='" + multipleLagrum + '\'' + ", defaultAct='"
      + defaultAct + '\'' + '}';
  }
}

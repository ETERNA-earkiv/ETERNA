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
import java.util.List;

/**
 * De fakta ur ett AIP:s eterna_admin-metadata som
 * behörighetsmotorn beslutar på — sekretessbedömning, tolkade lagrum,
 * personuppgiftsbedömning och personuppgiftsnivå. Fylls av
 * {@link EternaAdminParser}; {@link #absent()} representerar ett AIP utan
 * (egen eller ärvd) eterna_admin.
 *
 * <p>
 * {@code warnings} innehåller en rad per del av sekretessgrunden som inte
 * kunde tolkas till lagrum. Motorn behandlar en varning som ett otäckt lagrum
 * (om regeln inte har "*"), dvs. otolkbar text stänger hellre än öppnar.
 * </p>
 *
 * <p>
 * {@code inherited}/{@code sourceAipId} sätts av applikatorn när fakta hämtats
 * från en förfader (defaults.missingMetadata = inherit eller inherit-closed).
 * </p>
 */
public class EternaAdminFacts implements Serializable {

  private static final long serialVersionUID = 1L;

  private boolean present;
  private String sekretess;
  private String sekretessgrund;
  private List<Lagrum> lagrum = new ArrayList<>();
  private List<String> warnings = new ArrayList<>();
  private String personuppgifter;
  private String personuppgiftsniva;
  private String sourceAipId;
  private boolean inherited;

  public EternaAdminFacts() {
    // bean
  }

  /** Fakta för ett AIP som saknar eterna_admin (present = false). */
  public static EternaAdminFacts absent() {
    EternaAdminFacts facts = new EternaAdminFacts();
    facts.setPresent(false);
    return facts;
  }

  /** true om ett eterna_admin-dokument hittades (eget eller ärvt). */
  public boolean isPresent() {
    return present;
  }

  public void setPresent(boolean present) {
    this.present = present;
  }

  /** Vokabulärvärde ur &lt;sekretess&gt;, t.ex. "sekretess_forekommer"; null om saknas. */
  public String getSekretess() {
    return sekretess;
  }

  public void setSekretess(String sekretess) {
    this.sekretess = sekretess;
  }

  /** Råtexten ur &lt;sekretessgrund&gt;; null om saknas/tom. */
  public String getSekretessgrund() {
    return sekretessgrund;
  }

  public void setSekretessgrund(String sekretessgrund) {
    this.sekretessgrund = sekretessgrund;
  }

  /** Tolkade lagrum ur sekretessgrunden (aldrig null). */
  public List<Lagrum> getLagrum() {
    return lagrum;
  }

  public void setLagrum(List<Lagrum> lagrum) {
    this.lagrum = lagrum == null ? new ArrayList<>() : lagrum;
  }

  /** Varningar från lagrumstolkningen (aldrig null). */
  public List<String> getWarnings() {
    return warnings;
  }

  public void setWarnings(List<String> warnings) {
    this.warnings = warnings == null ? new ArrayList<>() : warnings;
  }

  /** Vokabulärvärde ur &lt;personuppgifter&gt;; null om saknas. */
  public String getPersonuppgifter() {
    return personuppgifter;
  }

  public void setPersonuppgifter(String personuppgifter) {
    this.personuppgifter = personuppgifter;
  }

  /** Värde ur &lt;personuppgiftsniva&gt; (t.ex. "niva_2"); null om saknas. */
  public String getPersonuppgiftsniva() {
    return personuppgiftsniva;
  }

  public void setPersonuppgiftsniva(String personuppgiftsniva) {
    this.personuppgiftsniva = personuppgiftsniva;
  }

  /** Id på det AIP vars eterna_admin faktan kommer ifrån (vid arv: förfadern). */
  public String getSourceAipId() {
    return sourceAipId;
  }

  public void setSourceAipId(String sourceAipId) {
    this.sourceAipId = sourceAipId;
  }

  /** true om faktan ärvts från en förfader. */
  public boolean isInherited() {
    return inherited;
  }

  public void setInherited(boolean inherited) {
    this.inherited = inherited;
  }

  @Override
  public String toString() {
    return "EternaAdminFacts{present=" + present + ", sekretess=" + sekretess + ", sekretessgrund=" + sekretessgrund
      + ", lagrum=" + lagrum + ", warnings=" + warnings + ", personuppgifter=" + personuppgifter
      + ", personuppgiftsniva=" + personuppgiftsniva + ", sourceAipId=" + sourceAipId + ", inherited=" + inherited
      + "}";
  }
}

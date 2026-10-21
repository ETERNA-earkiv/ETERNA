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
 * En behörighetsregel för en grupp. Regeln anger
 * vilka värden i eterna_admin-metadatan (sekretess, lagrum, personuppgifter
 * och personuppgiftsnivå) som gruppen får läsa. Alla villkor måste vara
 * uppfyllda för att gruppen ska tillåtas; "*" i en lista tillåter alla
 * värden.
 *
 * En regel med {@code enabled = false} gör fortfarande gruppen "hanterad"
 * (regelmotorn tar då bort READ) men tillåter aldrig något.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccessRule implements Serializable {

  @Serial
  private static final long serialVersionUID = -4480366210553173462L;

  private String group;
  private boolean enabled = true;
  private List<String> sekretess = new ArrayList<>();
  private List<String> lagrum = new ArrayList<>();
  private List<String> personuppgifter = new ArrayList<>();
  private List<String> personuppgiftsniva = new ArrayList<>();
  private String note;

  public AccessRule() {
    super();
  }

  public AccessRule(String group) {
    this();
    this.group = group;
  }

  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public List<String> getSekretess() {
    return sekretess;
  }

  public void setSekretess(List<String> sekretess) {
    this.sekretess = sekretess != null ? sekretess : new ArrayList<>();
  }

  public List<String> getLagrum() {
    return lagrum;
  }

  public void setLagrum(List<String> lagrum) {
    this.lagrum = lagrum != null ? lagrum : new ArrayList<>();
  }

  public List<String> getPersonuppgifter() {
    return personuppgifter;
  }

  public void setPersonuppgifter(List<String> personuppgifter) {
    this.personuppgifter = personuppgifter != null ? personuppgifter : new ArrayList<>();
  }

  public List<String> getPersonuppgiftsniva() {
    return personuppgiftsniva;
  }

  public void setPersonuppgiftsniva(List<String> personuppgiftsniva) {
    this.personuppgiftsniva = personuppgiftsniva != null ? personuppgiftsniva : new ArrayList<>();
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    AccessRule that = (AccessRule) o;
    return enabled == that.enabled && Objects.equals(group, that.group) && Objects.equals(sekretess, that.sekretess)
      && Objects.equals(lagrum, that.lagrum) && Objects.equals(personuppgifter, that.personuppgifter)
      && Objects.equals(personuppgiftsniva, that.personuppgiftsniva) && Objects.equals(note, that.note);
  }

  @Override
  public int hashCode() {
    return Objects.hash(group, enabled, sekretess, lagrum, personuppgifter, personuppgiftsniva, note);
  }

  @Override
  public String toString() {
    return "AccessRule{" + "group='" + group + '\'' + ", enabled=" + enabled + ", sekretess=" + sekretess
      + ", lagrum=" + lagrum + ", personuppgifter=" + personuppgifter + ", personuppgiftsniva=" + personuppgiftsniva
      + ", note='" + note + '\'' + '}';
  }
}

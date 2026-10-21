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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Vokabulären som regelredigeraren behöver — de
 * tillåtna värdena för sekretess, personuppgifter och personuppgiftsnivå
 * (plus jokertecknet "*", som inte listas här) samt etiketter på begärt
 * språk. Gruppkatalogen ingår inte (den kräver member.read; gruppen väljs
 * i redigeraren via MemberSelectDialog).
 *
 * <ul>
 * <li>{@code sekretess}/{@code personuppgifter}: värden enligt
 * eterna_admin.xsd i den ordning schemat anger (ej bedömd först).</li>
 * <li>{@code personuppgiftsniva}: värden ur
 * core.access_rules.personuppgiftsniva.values[] (tom lista när ingen
 * vokabulär är konfigurerad — då valideras inte nivåer).</li>
 * <li>{@code labels}: värde → etikett (ServerMessages-nycklarna
 * eterna_admin.sekretess.&lt;v&gt;, eterna_admin.personuppgifter.&lt;v&gt;
 * och eterna_admin.personuppgiftsniva.&lt;v&gt;); saknas översättning
 * används värdet självt. Ett värde som förekommer i flera vokabulärer
 * ("ej_bedomd") har samma etikett i alla.</li>
 * </ul>
 *
 * Klassen GWT-kompileras (bara java.util-samlingar).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccessRulesVocabulary implements Serializable {

  @Serial
  private static final long serialVersionUID = 5188216030957274413L;

  private List<String> sekretess = new ArrayList<>();
  private List<String> personuppgifter = new ArrayList<>();
  private List<String> personuppgiftsniva = new ArrayList<>();
  private Map<String, String> labels = new HashMap<>();

  public AccessRulesVocabulary() {
    super();
  }

  public List<String> getSekretess() {
    return sekretess;
  }

  public void setSekretess(List<String> sekretess) {
    this.sekretess = sekretess != null ? sekretess : new ArrayList<>();
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

  public Map<String, String> getLabels() {
    return labels;
  }

  public void setLabels(Map<String, String> labels) {
    this.labels = labels != null ? labels : new HashMap<>();
  }

  /** Etiketten för ett värde, eller värdet självt om ingen etikett finns. */
  public String labelOf(String value) {
    if (value == null) {
      return null;
    }
    String label = labels.get(value);
    return label != null && !label.isEmpty() ? label : value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    AccessRulesVocabulary that = (AccessRulesVocabulary) o;
    return Objects.equals(sekretess, that.sekretess) && Objects.equals(personuppgifter, that.personuppgifter)
      && Objects.equals(personuppgiftsniva, that.personuppgiftsniva) && Objects.equals(labels, that.labels);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sekretess, personuppgifter, personuppgiftsniva, labels);
  }

  @Override
  public String toString() {
    return "AccessRulesVocabulary{" + "sekretess=" + sekretess + ", personuppgifter=" + personuppgifter
      + ", personuppgiftsniva=" + personuppgiftsniva + ", labels=" + labels + '}';
  }
}

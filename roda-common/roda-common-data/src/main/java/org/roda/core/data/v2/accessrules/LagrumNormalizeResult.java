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
 * Resultatet av en lagrumsnormalisering.
 *
 * <ul>
 * <li>{@code input}: texterna som skickades in (oförändrade).</li>
 * <li>{@code normalized}: de tolkade lagrummen i mönsterform ("*",
 * "OSL 21", "OSL 21:7", "OSL 21:7a"), avdubblerade och i textordning — en
 * inmatad text med flera lagrum ger flera poster. Varje post kan användas
 * direkt i {@code AccessRule.lagrum}.</li>
 * <li>{@code warnings}: en varning per del som inte kunde tolkas (sådana
 * delar ger inget mönster). Tom lista = allt tolkades.</li>
 * </ul>
 *
 * Klassen GWT-kompileras.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LagrumNormalizeResult implements Serializable {

  @Serial
  private static final long serialVersionUID = 8042718113250995178L;

  private List<String> input = new ArrayList<>();
  private List<String> normalized = new ArrayList<>();
  private List<String> warnings = new ArrayList<>();

  public LagrumNormalizeResult() {
    super();
  }

  public List<String> getInput() {
    return input;
  }

  public void setInput(List<String> input) {
    this.input = input != null ? input : new ArrayList<>();
  }

  public List<String> getNormalized() {
    return normalized;
  }

  public void setNormalized(List<String> normalized) {
    this.normalized = normalized != null ? normalized : new ArrayList<>();
  }

  public List<String> getWarnings() {
    return warnings;
  }

  public void setWarnings(List<String> warnings) {
    this.warnings = warnings != null ? warnings : new ArrayList<>();
  }

  public boolean hasWarnings() {
    return warnings != null && !warnings.isEmpty();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    LagrumNormalizeResult that = (LagrumNormalizeResult) o;
    return Objects.equals(input, that.input) && Objects.equals(normalized, that.normalized)
      && Objects.equals(warnings, that.warnings);
  }

  @Override
  public int hashCode() {
    return Objects.hash(input, normalized, warnings);
  }

  @Override
  public String toString() {
    return "LagrumNormalizeResult{" + "input=" + input + ", normalized=" + normalized + ", warnings=" + warnings
      + '}';
  }
}

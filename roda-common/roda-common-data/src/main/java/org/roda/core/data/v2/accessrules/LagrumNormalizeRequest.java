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
 * Begäran om att normalisera lagrumstexter till
 * regelfilens mönsterform (se {@link LagrumNormalizeResult}). Varje post i
 * {@code input} är en fritext som handläggaren skrivit, t.ex.
 * "OSL 21 kap. 7 §" eller "OSL 26 kap. 1 § och 21 kap. 7 §"; en text kan ge
 * flera mönster.
 *
 * Klassen GWT-kompileras.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LagrumNormalizeRequest implements Serializable {

  @Serial
  private static final long serialVersionUID = -2645817702336141935L;

  private List<String> input = new ArrayList<>();

  public LagrumNormalizeRequest() {
    super();
  }

  public LagrumNormalizeRequest(List<String> input) {
    this();
    setInput(input);
  }

  public List<String> getInput() {
    return input;
  }

  public void setInput(List<String> input) {
    this.input = input != null ? input : new ArrayList<>();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    LagrumNormalizeRequest that = (LagrumNormalizeRequest) o;
    return Objects.equals(input, that.input);
  }

  @Override
  public int hashCode() {
    return Objects.hash(input);
  }

  @Override
  public String toString() {
    return "LagrumNormalizeRequest{" + "input=" + input + '}';
  }
}

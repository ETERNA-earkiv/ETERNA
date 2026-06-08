/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.data.v2.disposal.rule;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A single metadata-field condition of a {@link DisposalRule} (a field key and the value it must match). A
 * METADATA_FIELD rule may hold several conditions, which are ANDed together when the rule is evaluated.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DisposalRuleCondition implements Serializable {

  @Serial
  private static final long serialVersionUID = 7716321340335265337L;

  private String key;
  private String value;

  public DisposalRuleCondition() {
    super();
  }

  public DisposalRuleCondition(String key, String value) {
    this.key = key;
    this.value = value;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DisposalRuleCondition that = (DisposalRuleCondition) o;
    return Objects.equals(key, that.key) && Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, value);
  }

  @Override
  public String toString() {
    return "DisposalRuleCondition{key='" + key + '\'' + ", value='" + value + '\'' + '}';
  }
}

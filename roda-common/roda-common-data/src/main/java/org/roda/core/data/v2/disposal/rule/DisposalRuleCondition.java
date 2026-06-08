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
  // The operator joining this condition to the previous one (ignored for the first condition). Defaults to AND so that
  // rules stored before per-condition operators were introduced keep behaving as an all-conditions-must-match (AND).
  private DisposalRuleConditionOperator operator = DisposalRuleConditionOperator.AND;

  public DisposalRuleCondition() {
    super();
  }

  public DisposalRuleCondition(String key, String value) {
    this.key = key;
    this.value = value;
  }

  public DisposalRuleCondition(String key, String value, DisposalRuleConditionOperator operator) {
    this.key = key;
    this.value = value;
    this.operator = operator;
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

  public DisposalRuleConditionOperator getOperator() {
    return operator;
  }

  public void setOperator(DisposalRuleConditionOperator operator) {
    this.operator = operator;
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
    return Objects.equals(key, that.key) && Objects.equals(value, that.value) && operator == that.operator;
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, value, operator);
  }

  @Override
  public String toString() {
    return "DisposalRuleCondition{key='" + key + '\'' + ", value='" + value + '\'' + ", operator=" + operator + '}';
  }
}

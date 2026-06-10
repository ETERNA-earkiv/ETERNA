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
 * METADATA_FIELD rule may hold several conditions; each carries an {@link DisposalRuleConditionOperator} that joins it
 * to the previous one, so conditions are combined with AND or OR (folded left to right) when the rule is evaluated.
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

  /** Creates an empty condition (operator defaults to AND). Required for serialization. */
  public DisposalRuleCondition() {
    super();
  }

  /**
   * Creates a condition with the default AND operator.
   *
   * @param key
   *          the index field key
   * @param value
   *          the value the field must match
   */
  public DisposalRuleCondition(String key, String value) {
    this.key = key;
    this.value = value;
  }

  /**
   * Creates a condition with an explicit operator joining it to the previous condition.
   *
   * @param key
   *          the index field key
   * @param value
   *          the value the field must match
   * @param operator
   *          the operator (AND/OR) joining this condition to the previous one
   */
  public DisposalRuleCondition(String key, String value, DisposalRuleConditionOperator operator) {
    this.key = key;
    this.value = value;
    this.operator = operator;
  }

  /** @return the index field key */
  public String getKey() {
    return key;
  }

  /** @param key the index field key */
  public void setKey(String key) {
    this.key = key;
  }

  /** @return the value the field must match */
  public String getValue() {
    return value;
  }

  /** @param value the value the field must match */
  public void setValue(String value) {
    this.value = value;
  }

  /** @return the operator joining this condition to the previous one (ignored for the first condition) */
  public DisposalRuleConditionOperator getOperator() {
    return operator;
  }

  /** @param operator the operator joining this condition to the previous one */
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

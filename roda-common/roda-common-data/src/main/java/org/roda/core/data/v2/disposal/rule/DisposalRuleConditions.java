/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.data.v2.disposal.rule;

import java.util.ArrayList;
import java.util.List;

import org.roda.core.data.v2.index.filter.AndFiltersParameters;
import org.roda.core.data.v2.index.filter.FilterParameter;
import org.roda.core.data.v2.index.filter.OrFiltersParameters;
import org.roda.core.data.v2.index.filter.SimpleFilterParameter;

/**
 * Builds the index {@link FilterParameter} for a METADATA_FIELD disposal rule's conditions. Shared by the apply job
 * (roda-core) and the rule preview (GWT client) so both produce identical queries.
 *
 * <p>
 * Each condition becomes a {@link SimpleFilterParameter}; consecutive conditions are folded left to right using the
 * per-condition operator, reusing {@link AndFiltersParameters}/{@link OrFiltersParameters} (the same building blocks
 * advanced search uses). So {@code a AND b OR c} yields {@code (a AND b) OR c}.
 */
public final class DisposalRuleConditions {

  private DisposalRuleConditions() {
  }

  /**
   * Folds the conditions into a single {@link FilterParameter}.
   *
   * @param conditions
   *          the conditions to combine; must not be empty
   * @return the combined filter parameter
   */
  public static FilterParameter toFilterParameter(List<DisposalRuleCondition> conditions) {
    FilterParameter accumulated = new SimpleFilterParameter(conditions.get(0).getKey(), conditions.get(0).getValue());
    for (int i = 1; i < conditions.size(); i++) {
      DisposalRuleCondition condition = conditions.get(i);
      List<FilterParameter> pair = new ArrayList<>();
      pair.add(accumulated);
      pair.add(new SimpleFilterParameter(condition.getKey(), condition.getValue()));
      accumulated = DisposalRuleConditionOperator.OR.equals(condition.getOperator()) ? new OrFiltersParameters(pair)
        : new AndFiltersParameters(pair);
    }
    return accumulated;
  }
}

/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.data.v2.disposal.rule;

/**
 * The boolean operator that joins a {@link DisposalRuleCondition} to the preceding one when a METADATA_FIELD disposal
 * rule has several conditions. Conditions are folded left to right, so {@code a AND b OR c} evaluates as
 * {@code (a AND b) OR c}.
 */
public enum DisposalRuleConditionOperator {
  AND, OR
}

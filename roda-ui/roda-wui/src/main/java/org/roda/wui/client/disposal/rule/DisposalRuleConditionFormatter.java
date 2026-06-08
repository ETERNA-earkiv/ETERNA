/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.client.disposal.rule;

import java.util.List;

import org.roda.core.data.v2.disposal.rule.DisposalRule;
import org.roda.core.data.v2.disposal.rule.DisposalRuleCondition;

import com.google.gwt.core.client.GWT;

import config.i18n.client.ClientMessages;

/**
 * Formats the metadata-field conditions of a disposal rule for display, e.g. {@code title is Krank and description is
 * PMO}. Shared by the rule detail view and the rule listings so they render multi-condition rules consistently.
 */
public final class DisposalRuleConditionFormatter {

  private static final ClientMessages messages = GWT.create(ClientMessages.class);

  private DisposalRuleConditionFormatter() {
  }

  public static String describeMetadataConditions(DisposalRule rule) {
    StringBuilder builder = new StringBuilder();
    List<DisposalRuleCondition> conditions = rule.getMetadataConditions();
    for (int i = 0; i < conditions.size(); i++) {
      if (i > 0) {
        builder.append(' ').append(messages.disposalRuleConditionConjunction()).append(' ');
      }
      DisposalRuleCondition condition = conditions.get(i);
      builder.append(condition.getKey()).append(' ').append(messages.disposalRuleConditionOperator()).append(' ')
        .append(condition.getValue());
    }
    return builder.toString();
  }
}

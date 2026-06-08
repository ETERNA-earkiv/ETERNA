/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.plugins.base.disposal.rules;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.exceptions.GenericException;
import org.roda.core.data.exceptions.RequestNotValidException;
import org.roda.core.data.v2.disposal.metadata.DisposalAIPMetadata;
import org.roda.core.data.v2.disposal.metadata.DisposalScheduleAIPMetadata;
import org.roda.core.data.v2.disposal.rule.ConditionType;
import org.roda.core.data.v2.disposal.rule.DisposalRule;
import org.roda.core.data.v2.disposal.rule.DisposalRuleCondition;
import org.roda.core.data.v2.disposal.rule.DisposalRuleConditions;
import org.roda.core.data.v2.disposal.rule.DisposalRules;
import org.roda.core.data.v2.index.filter.Filter;
import org.roda.core.data.v2.index.filter.SimpleFilterParameter;
import org.roda.core.data.v2.ip.AIP;
import org.roda.core.data.v2.ip.AIPDisposalScheduleAssociationType;
import org.roda.core.data.v2.ip.AIPState;
import org.roda.core.data.v2.ip.IndexedAIP;
import org.roda.core.index.IndexService;

/**
 * @author Miguel Guimarães <mguimaraes@keep.pt>
 */
public class ApplyDisposalRulesPluginUtils {

  private ApplyDisposalRulesPluginUtils() {
  }

  /**
   * Applies the first matching disposal rule to the given AIP, setting its disposal metadata in place.
   *
   * @param aip
   *          the AIP to evaluate
   * @param disposalRules
   *          the ordered disposal rules to evaluate
   * @param index
   *          the index service used for metadata-field matching
   * @param allowedConditionFields
   *          the whitelist of allowed metadata condition fields (see {@link #allowedMetadataConditionFields()})
   * @return the first matching rule, or {@link Optional#empty()} if none matched
   * @throws GenericException
   *           if rule evaluation against the index fails
   */
  public static Optional<DisposalRule> applyRule(AIP aip, DisposalRules disposalRules, IndexService index,
    Set<String> allowedConditionFields) throws GenericException {

    for (DisposalRule rule : disposalRules.getObjects()) {
      Optional<DisposalRule> used = Optional.empty();
      if (ConditionType.IS_CHILD_OF.equals(rule.getType())) {
        used = conditionTypeChildOf(aip, rule);
      } else if (ConditionType.METADATA_FIELD.equals(rule.getType())) {
        used = conditionTypeMetadataValue(aip, rule, index, allowedConditionFields);
      }

      if (used.isPresent()) {
        return used;
      }
    }

    return Optional.empty();
  }

  private static Optional<DisposalRule> conditionTypeChildOf(AIP aip, DisposalRule rule) {
    if (aip.getParentId() != null && aip.getParentId().equals(rule.getConditionKey())) {
      DisposalAIPMetadata disposal = getDisposalAipMetadata(aip, rule);
      aip.setDisposal(disposal);
      return Optional.of(rule);
    }

    return Optional.empty();
  }

  private static Optional<DisposalRule> conditionTypeMetadataValue(AIP aip, DisposalRule rule, IndexService index,
    Set<String> allowedConditionFields) throws GenericException {

    List<DisposalRuleCondition> conditions = rule.getMetadataConditions();
    if (conditions.isEmpty()) {
      return Optional.empty();
    }

    // Skip incomplete/unsafe rules: a blank value would NPE in SolrUtils, and a non-whitelisted key is written raw
    // into the Solr query (appendExactMatch does not escape the field name). The whitelist is shared with the API.
    for (DisposalRuleCondition condition : conditions) {
      if (StringUtils.isBlank(condition.getKey()) || StringUtils.isBlank(condition.getValue())
        || !allowedConditionFields.contains(condition.getKey())) {
        return Optional.empty();
      }
    }

    // Match exactly like the preview (DisposalRuleDataPanel#refreshPreviewAIPList): a Solr filter scoped to this AIP
    // (AIP_STATE=ACTIVE) plus the conditions folded with their per-condition AND/OR operators (shared helper).
    Filter filter = new Filter(new SimpleFilterParameter(RodaConstants.INDEX_UUID, aip.getId()),
      new SimpleFilterParameter(RodaConstants.AIP_STATE, AIPState.ACTIVE.name()));
    filter.add(DisposalRuleConditions.toFilterParameter(conditions));

    try {
      if (index.count(IndexedAIP.class, filter) > 0) {
        DisposalAIPMetadata disposal = getDisposalAipMetadata(aip, rule);
        aip.setDisposal(disposal);
        return Optional.of(rule);
      }
    } catch (RequestNotValidException e) {
      throw new GenericException(
        "Unable to evaluate disposal rule '" + rule.getId() + "' for AIP '" + aip.getId() + "'", e);
    }

    return Optional.empty();
  }

  private static DisposalAIPMetadata getDisposalAipMetadata(AIP aip, DisposalRule rule) {
    DisposalAIPMetadata disposal = aip.getDisposal();
    if (disposal == null) {
      disposal = new DisposalAIPMetadata();
      disposal.setSchedule(new DisposalScheduleAIPMetadata());
    } else if (disposal.getSchedule() == null) {
      disposal.setSchedule(new DisposalScheduleAIPMetadata());
    }
    disposal.getSchedule().setId(rule.getDisposalScheduleId());
    disposal.getSchedule().setAssociationType(AIPDisposalScheduleAssociationType.RULES);

    return disposal;
  }
}

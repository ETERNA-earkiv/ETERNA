/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.plugins.base.disposal.rules;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.roda.core.RodaCoreFactory;
import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.exceptions.GenericException;
import org.roda.core.data.exceptions.RequestNotValidException;
import org.roda.core.data.v2.disposal.metadata.DisposalAIPMetadata;
import org.roda.core.data.v2.disposal.metadata.DisposalScheduleAIPMetadata;
import org.roda.core.data.v2.disposal.rule.ConditionType;
import org.roda.core.data.v2.disposal.rule.DisposalRule;
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

    // Skip incomplete/unsafe rules: a blank value would NPE in SolrUtils, and a non-whitelisted key is written raw
    // into the Solr query (appendExactMatch does not escape the field name). The whitelist is shared with the API.
    if (StringUtils.isBlank(rule.getConditionKey()) || StringUtils.isBlank(rule.getConditionValue())
      || !allowedConditionFields.contains(rule.getConditionKey())) {
      return Optional.empty();
    }

    // Match exactly like the preview (DisposalRuleDataPanel#refreshPreviewAIPList): a Solr filter on the condition
    // field plus AIP_STATE=ACTIVE, scoped to this AIP — so apply associates the same AIPs the preview shows.
    Filter filter = new Filter(new SimpleFilterParameter(RodaConstants.INDEX_UUID, aip.getId()),
      new SimpleFilterParameter(RodaConstants.AIP_STATE, AIPState.ACTIVE.name()),
      new SimpleFilterParameter(rule.getConditionKey(), rule.getConditionValue()));

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

  /**
   * The set of Solr field names a {@link ConditionType#METADATA_FIELD} rule is allowed to target. Mirrors the fields
   * the UI offers in MetadataFieldsPanel: text-typed IndexedAIP search fields whose configuration key is not in the
   * disposal rule condition blacklist. Shared by the apply job and the API validation (DisposalRuleService) so both
   * enforce the same whitelist.
   *
   * <p>
   * Note: the blacklist is matched against the configuration key (e.g. {@code reference}), not the resolved Solr field
   * (e.g. {@code unitId_txt}), exactly as the UI does.
   */
  public static Set<String> allowedMetadataConditionFields() {
    List<String> blacklist = RodaCoreFactory.getRodaConfigurationAsList(RodaConstants.DISPOSAL_RULE_BLACKLIST_CONDITION);
    String classSimpleName = IndexedAIP.class.getSimpleName();

    Set<String> allowedFields = new HashSet<>();
    for (String field : RodaCoreFactory.getRodaConfigurationAsList(RodaConstants.SEARCH_FIELD_PREFIX, classSimpleName)) {
      String fieldPrefix = RodaConstants.SEARCH_FIELD_PREFIX + '.' + classSimpleName + '.' + field;
      String fieldType = RodaCoreFactory.getRodaConfigurationAsString(fieldPrefix, RodaConstants.SEARCH_FIELD_TYPE);
      String fieldName = RodaCoreFactory.getRodaConfigurationAsString(fieldPrefix, RodaConstants.SEARCH_FIELD_FIELDS);

      if (RodaConstants.SEARCH_FIELD_TYPE_TEXT.equals(fieldType) && StringUtils.isNotBlank(fieldName)
        && !blacklist.contains(field)) {
        allowedFields.add(fieldName);
      }
    }
    return allowedFields;
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

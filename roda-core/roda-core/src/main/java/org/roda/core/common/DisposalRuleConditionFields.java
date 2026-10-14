/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.common;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.roda.core.RodaCoreFactory;
import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.v2.ip.IndexedAIP;

/**
 * Shared, layer-neutral source of truth for which metadata fields a disposal rule condition may target. Kept in
 * {@code org.roda.core.common} (not in a plugin package) so both the apply job (roda-core) and the API validation
 * (roda-ui {@code DisposalRuleService}) can reuse the exact same whitelist without the UI layer reaching into plugin
 * internals.
 */
public final class DisposalRuleConditionFields {

  private DisposalRuleConditionFields() {
  }

  /**
   * The set of Solr field names a metadata-field disposal rule condition is allowed to target. Mirrors the fields the
   * UI offers (client {@code MetadataFieldsPanel}): text-typed IndexedAIP search fields whose configuration key is not
   * in the disposal rule condition blacklist.
   *
   * <p>
   * The blacklist is matched against the configuration key (e.g. {@code reference}), not the resolved Solr field (e.g.
   * {@code unitId_txt}), exactly as the UI does.
   *
   * @return the allowed Solr field names; empty if the search-field configuration is not loaded
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
}

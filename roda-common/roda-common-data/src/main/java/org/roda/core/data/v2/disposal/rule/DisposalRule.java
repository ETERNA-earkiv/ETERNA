/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.data.v2.disposal.rule;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.v2.IsModelObject;
import org.roda.core.data.v2.ip.HasId;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @author Tiago Fraga <tfraga@keep.pt>
 */
@jakarta.xml.bind.annotation.XmlRootElement(name = RodaConstants.RODA_OBJECT_DISPOSAL_RULE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DisposalRule implements IsModelObject, HasId, Comparable<DisposalRule> {

  // Stays at 1 on purpose: the new conditions list is an optional, additive field and old rules are read transparently
  // via getMetadataConditions(), so no stored-data migration is required. Bumping this would trip RODA's model-version
  // mismatch check (MigrationManager) and abort primary-node startup for an existing installation.
  private static final int VERSION = 1;
  @Serial
  private static final long serialVersionUID = 6903251340335265336L;

  private String id;
  private String title;

  private String description;

  private ConditionType type;

  // condition
  // For IS_CHILD_OF: conditionKey holds the parent AIP id (single condition).
  // For METADATA_FIELD: conditions holds one or more field/value pairs that are ANDed together. conditionKey/
  // conditionValue are kept for backward compatibility with rules stored before multi-condition support;
  // getMetadataConditions() normalises both shapes (so no model-version migration is required).
  private String conditionKey;
  private String conditionValue;
  private List<DisposalRuleCondition> conditions;

  private String disposalScheduleId;
  private String disposalScheduleName;

  private Integer order;

  private Date createdOn = null;
  private String createdBy = null;
  private Date updatedOn = null;
  private String updatedBy = null;

  public DisposalRule() {
    super();
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public ConditionType getType() {
    return type;
  }

  public void setType(ConditionType type) {
    this.type = type;
  }

  public String getDisposalScheduleId() {
    return disposalScheduleId;
  }

  public void setDisposalScheduleId(String disposalScheduleId) {
    this.disposalScheduleId = disposalScheduleId;
  }

  public String getDisposalScheduleName() {
    return disposalScheduleName;
  }

  public void setDisposalScheduleName(String disposalScheduleName) {
    this.disposalScheduleName = disposalScheduleName;
  }

  public Integer getOrder() {
    return order;
  }

  public void setOrder(Integer order) {
    this.order = order;
  }

  public Date getCreatedOn() {
    return createdOn;
  }

  public void setCreatedOn(Date createdOn) {
    this.createdOn = createdOn;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public Date getUpdatedOn() {
    return updatedOn;
  }

  public void setUpdatedOn(Date updatedOn) {
    this.updatedOn = updatedOn;
  }

  public String getUpdatedBy() {
    return updatedBy;
  }

  public void setUpdatedBy(String updatedBy) {
    this.updatedBy = updatedBy;
  }

  public String getConditionKey() {
    return conditionKey;
  }

  public void setConditionKey(String conditionKey) {
    this.conditionKey = conditionKey;
  }

  public String getConditionValue() {
    return conditionValue;
  }

  public void setConditionValue(String conditionValue) {
    this.conditionValue = conditionValue;
  }

  public List<DisposalRuleCondition> getConditions() {
    return conditions;
  }

  public void setConditions(List<DisposalRuleCondition> conditions) {
    this.conditions = conditions;
  }

  /**
   * Returns the metadata-field conditions normalised across storage formats. Rules saved with multi-condition support
   * carry them in {@link #conditions}; rules saved before that carry a single condition in
   * {@link #conditionKey}/{@link #conditionValue}. All METADATA_FIELD evaluation and validation should go through this
   * method so both shapes behave identically.
   *
   * @return the conditions to AND together; never {@code null}
   */
  @JsonIgnore
  public List<DisposalRuleCondition> getMetadataConditions() {
    if (conditions != null && !conditions.isEmpty()) {
      return conditions;
    }
    List<DisposalRuleCondition> normalised = new ArrayList<>();
    if (conditionKey != null && !conditionKey.isEmpty()) {
      normalised.add(new DisposalRuleCondition(conditionKey, conditionValue));
    }
    return normalised;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    DisposalRule that = (DisposalRule) o;
    return Objects.equals(id, that.id) && Objects.equals(title, that.title)
      && Objects.equals(description, that.description) && type == that.type
      && Objects.equals(conditionKey, that.conditionKey) && Objects.equals(conditionValue, that.conditionValue)
      && Objects.equals(conditions, that.conditions)
      && Objects.equals(disposalScheduleId, that.disposalScheduleId)
      && Objects.equals(disposalScheduleName, that.disposalScheduleName) && Objects.equals(order, that.order)
      && Objects.equals(createdOn, that.createdOn) && Objects.equals(createdBy, that.createdBy)
      && Objects.equals(updatedOn, that.updatedOn) && Objects.equals(updatedBy, that.updatedBy);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, title, description, type, conditionKey, conditionValue, conditions, disposalScheduleId,
      disposalScheduleName, order, createdOn, createdBy, updatedOn, updatedBy);
  }

  @Override
  public String toString() {
    return "DisposalRule{" + "id='" + id + '\'' + ", title='" + title + '\'' + ", description='" + description + '\''
      + ", type=" + type + ", conditionKey='" + conditionKey + '\'' + ", conditionValue='" + conditionValue + '\''
      + ", conditions=" + conditions + ", disposalScheduleId='" + disposalScheduleId + '\'' + ", disposalScheduleName='" + disposalScheduleName + '\''
      + ", order=" + order + ", createdOn=" + createdOn + ", createdBy='" + createdBy + '\'' + ", updatedOn="
      + updatedOn + ", updatedBy='" + updatedBy + '\'' + '}';
  }

  @JsonIgnore
  @Override
  public int getClassVersion() {
    return VERSION;
  }

  @Override
  public int compareTo(DisposalRule otherRule) {
    return Integer.compare(this.getOrder(), otherRule.getOrder());
  }
}

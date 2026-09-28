/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.data.v2.ip.metadata;

import java.io.Serial;
import java.io.Serializable;

import org.roda.core.data.v2.generics.select.SelectedItemsRequest;

/**
 * ETERNA 1.H (krav 1): begäran om massredigering av ett fält i beskrivande
 * metadata för ett urval av AIP:ar. Utförs som jobb med
 * BulkUpdateDescriptiveMetadataPlugin; fältets xpath resolvas alltid
 * server-side ur formulärmallen.
 */
public class BulkUpdateDescriptiveMetadataRequest implements Serializable {

  @Serial
  private static final long serialVersionUID = 4489218852204523111L;

  private SelectedItemsRequest items;
  private String metadataType;
  private String fieldName;
  private String newValue;
  private String mode;
  private String expectedValue;

  public BulkUpdateDescriptiveMetadataRequest() {
    // empty constructor
  }

  public SelectedItemsRequest getItems() {
    return items;
  }

  public void setItems(SelectedItemsRequest items) {
    this.items = items;
  }

  public String getMetadataType() {
    return metadataType;
  }

  public void setMetadataType(String metadataType) {
    this.metadataType = metadataType;
  }

  public String getFieldName() {
    return fieldName;
  }

  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }

  public String getNewValue() {
    return newValue;
  }

  public void setNewValue(String newValue) {
    this.newValue = newValue;
  }

  public String getMode() {
    return mode;
  }

  public void setMode(String mode) {
    this.mode = mode;
  }

  public String getExpectedValue() {
    return expectedValue;
  }

  public void setExpectedValue(String expectedValue) {
    this.expectedValue = expectedValue;
  }
}

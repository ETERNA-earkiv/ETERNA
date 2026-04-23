/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/keeps/roda
 */
package org.roda.core.data.v2.ip.redaction;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author Miguel Guimarães <mguimaraes@keep.pt>
 */

public class StartRedactionRequest implements Serializable {

  @Serial
  private static final long serialVersionUID = 4891623758294756821L;

  private String aipId;
  private String representationId;
  private String fileId;
  private String details;

  public StartRedactionRequest() {
    // empty constructor
  }

  public StartRedactionRequest(String aipId, String representationId, String fileId, String details) {
    this.aipId = aipId;
    this.representationId = representationId;
    this.fileId = fileId;
    this.details = details;
  }

  public String getAipId() {
    return aipId;
  }

  public void setAipId(String aipId) {
    this.aipId = aipId;
  }

  public String getRepresentationId() {
    return representationId;
  }

  public void setRepresentationId(String representationId) {
    this.representationId = representationId;
  }

  public String getFileId() {
    return fileId;
  }

  public void setFileId(String fileId) {
    this.fileId = fileId;
  }

  public String getDetails() {
    return details;
  }

  public void setDetails(String details) {
    this.details = details;
  }
}

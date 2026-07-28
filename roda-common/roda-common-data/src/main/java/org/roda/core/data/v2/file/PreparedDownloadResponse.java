/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.data.v2.file;

import java.io.Serial;
import java.io.Serializable;

/**
 * Result of preparing a selection-based download: the token the client uses to
 * fetch the zip, plus what the client needs to decide whether to confirm first.
 */
public class PreparedDownloadResponse implements Serializable {

  @Serial
  private static final long serialVersionUID = -6008527863498374619L;

  private String token;
  private int fileCount;
  private long totalSize;

  public PreparedDownloadResponse() {
    // empty constructor
  }

  public PreparedDownloadResponse(String token, int fileCount, long totalSize) {
    this.token = token;
    this.fileCount = fileCount;
    this.totalSize = totalSize;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public int getFileCount() {
    return fileCount;
  }

  public void setFileCount(int fileCount) {
    this.fileCount = fileCount;
  }

  public long getTotalSize() {
    return totalSize;
  }

  public void setTotalSize(long totalSize) {
    this.totalSize = totalSize;
  }
}

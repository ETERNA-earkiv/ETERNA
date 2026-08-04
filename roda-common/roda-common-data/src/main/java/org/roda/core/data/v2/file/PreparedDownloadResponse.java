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
 * <p>
 * A refusal travels here too, rather than as an HTTP error, because the client
 * has to tell the reasons apart and name the numbers behind them — and the
 * error channel conveys neither: every refusal would arrive as the same
 * "Request was not valid" with the numbers buried in an English sentence.
 * The size and the count are filled in either way, so a refusal can say how
 * large the selection was that got turned down.
 */
public class PreparedDownloadResponse implements Serializable {

  @Serial
  private static final long serialVersionUID = -6008527863498374619L;

  private String token;
  private int fileCount;
  private long totalSize;
  private DownloadRefusal refusal;

  public PreparedDownloadResponse() {
    // empty constructor
  }

  public PreparedDownloadResponse(String token, int fileCount, long totalSize) {
    this.token = token;
    this.fileCount = fileCount;
    this.totalSize = totalSize;
  }

  public PreparedDownloadResponse(DownloadRefusal refusal, int fileCount, long totalSize) {
    this.refusal = refusal;
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

  /** {@code null} when the download was prepared and a token was issued. */
  public DownloadRefusal getRefusal() {
    return refusal;
  }

  public void setRefusal(DownloadRefusal refusal) {
    this.refusal = refusal;
  }
}

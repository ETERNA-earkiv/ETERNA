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
 * A refused prepared download, carrying everything the client needs to say why
 * in its own language. The numbers are part of the refusal rather than left for
 * the client to look up, so that no message can end up naming a count the
 * server never applied.
 */
public class DownloadRefusal implements Serializable {

  @Serial
  private static final long serialVersionUID = 3382013297158457851L;

  private DownloadRefusalReason reason;
  private int fileCount;
  private int fileLimit;
  private int undeliverableFileCount;

  public DownloadRefusal() {
    // empty constructor
  }

  public static DownloadRefusal noFiles() {
    DownloadRefusal refusal = new DownloadRefusal();
    refusal.reason = DownloadRefusalReason.NO_FILES;
    return refusal;
  }

  public static DownloadRefusal tooManyFiles(int fileCount, int fileLimit) {
    DownloadRefusal refusal = new DownloadRefusal();
    refusal.reason = DownloadRefusalReason.TOO_MANY_FILES;
    refusal.fileCount = fileCount;
    refusal.fileLimit = fileLimit;
    return refusal;
  }

  public static DownloadRefusal undeliverableContent(int undeliverableFileCount, int fileCount) {
    DownloadRefusal refusal = new DownloadRefusal();
    refusal.reason = DownloadRefusalReason.UNDELIVERABLE_CONTENT;
    refusal.undeliverableFileCount = undeliverableFileCount;
    refusal.fileCount = fileCount;
    return refusal;
  }

  public DownloadRefusalReason getReason() {
    return reason;
  }

  public void setReason(DownloadRefusalReason reason) {
    this.reason = reason;
  }

  /** How many files the selection expanded to. */
  public int getFileCount() {
    return fileCount;
  }

  public void setFileCount(int fileCount) {
    this.fileCount = fileCount;
  }

  /** The configured maximum; only meaningful for {@code TOO_MANY_FILES}. */
  public int getFileLimit() {
    return fileLimit;
  }

  public void setFileLimit(int fileLimit) {
    this.fileLimit = fileLimit;
  }

  /**
   * How many files cannot be fetched; only meaningful for
   * {@code UNDELIVERABLE_CONTENT}.
   */
  public int getUndeliverableFileCount() {
    return undeliverableFileCount;
  }

  public void setUndeliverableFileCount(int undeliverableFileCount) {
    this.undeliverableFileCount = undeliverableFileCount;
  }
}

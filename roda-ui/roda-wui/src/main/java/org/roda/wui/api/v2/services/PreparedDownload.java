/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.api.v2.services;

import java.util.List;

import org.roda.core.data.v2.ip.IndexedFile;

/**
 * A validated, materialized file list bound to the user that requested it.
 * <p>
 * The binding is mandatory: without it the download token would be a way
 * around the permission check. The list is materialized rather than kept as a
 * selection so that what is delivered is demonstrably what was validated and
 * audited, even if the index changes in between.
 */
public record PreparedDownload(String username, List<IndexedFile> files) {

  public long totalSize() {
    return files.stream().mapToLong(IndexedFile::getSize).sum();
  }

  public List<String> fileUUIDs() {
    return files.stream().map(IndexedFile::getUUID).toList();
  }
}

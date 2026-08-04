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
 * <p>
 * The reason travels along because the download itself is fetched by plain
 * browser navigation, which cannot carry the {@code x-request-reason} header.
 * Reusing the reason the client sent when preparing keeps both audit entries in
 * the user's own language instead of leaving the one that records the actual
 * disclosure without a reason.
 * <p>
 * Note that this is the reason as already translated by the client, so it is
 * frozen in whatever language the session had when the download was prepared
 * and cannot be rendered in another one later. The action itself is legible
 * regardless: the audit log translates {@code actionMethod} at render time from
 * a stable identifier, and both endpoints have a key for it.
 */
public record PreparedDownload(String username, String reason, List<IndexedFile> files) {

  public long totalSize() {
    return files.stream().mapToLong(IndexedFile::getSize).sum();
  }

  public List<String> fileUUIDs() {
    return files.stream().map(IndexedFile::getUUID).toList();
  }
}

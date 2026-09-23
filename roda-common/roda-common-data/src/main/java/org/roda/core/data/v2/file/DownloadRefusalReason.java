/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.data.v2.file;

/**
 * Why a prepared download was refused. The client needs its own wording for
 * each, so the reason travels as a value rather than as prose.
 * <p>
 * A refusal for lack of permission is not here: that one is an
 * {@code AuthorizationDeniedException} and reaches the client as HTTP 403,
 * along with every other permission failure in the API.
 */
public enum DownloadRefusalReason {
  /** The selection expanded to no files at all, so there is nothing to zip. */
  NO_FILES,

  /** The selection is larger than {@code core.download.max_files} allows. */
  TOO_MANY_FILES,

  /** Some of the selected files reference content that cannot be fetched. */
  UNDELIVERABLE_CONTENT
}

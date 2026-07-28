/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.api.v2.services;

import java.util.concurrent.TimeUnit;

import org.roda.core.data.exceptions.NotFoundException;
import org.roda.core.util.IdUtils;
import org.springframework.stereotype.Service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Holds the tokens of the two-step, prepared download of a file selection: a
 * POST expands and validates the selection and issues a token, a GET on that
 * token streams the zip. See
 * {@code docs/adr/0002-prepared-download-via-token-for-selection-based-downloads.md}.
 * <p>
 * The cache is what makes this a bean rather than a static: it has to survive
 * between the two requests. The expansion, validation and zipping themselves
 * live in {@code org.roda.core.common.DownloadSelection}.
 */
@Service
public class DownloadSelectionService {

  private static final long TOKEN_TTL_MINUTES = 10;

  /**
   * Token to prepared download. Per node and in memory, following the Guava
   * pattern already used for schemas, disposal rules and i18n in core. A token
   * stays valid for its whole TTL so that browser resumption works.
   */
  private final Cache<String, PreparedDownload> preparedDownloads = CacheBuilder.newBuilder()
    .expireAfterWrite(TOKEN_TTL_MINUTES, TimeUnit.MINUTES).build();

  /**
   * Caches a validated file list under a fresh token and returns the token.
   */
  public String prepareDownload(PreparedDownload prepared) {
    String token = IdUtils.createUUID();
    preparedDownloads.put(token, prepared);
    return token;
  }

  /**
   * Resolves a token for the user presenting it. An unknown token, an expired
   * one and one issued to somebody else are all indistinguishable to the
   * caller, so a token cannot be used to probe for other users' downloads.
   */
  public PreparedDownload retrievePreparedDownload(String token, String username) throws NotFoundException {
    PreparedDownload prepared = token == null ? null : preparedDownloads.getIfPresent(token);

    if (prepared == null || !prepared.username().equals(username)) {
      throw new NotFoundException("There is no prepared download for the given token");
    }

    return prepared;
  }
}

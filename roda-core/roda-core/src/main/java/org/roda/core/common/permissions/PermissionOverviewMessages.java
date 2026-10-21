/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.common.permissions;

import org.roda.core.common.Messages;

/**
 * Minimal översättningskälla för
 * behörighetsöversiktens renderare, så att de kan testas utan
 * RodaCoreFactory. Nycklarna ligger under {@code permission.overview.*} i
 * ServerMessages(_sv_SE).properties; saknas en nyckel används fallback.
 */
@FunctionalInterface
public interface PermissionOverviewMessages {

  /**
   * Översätter en nyckel.
   *
   * @param key
   *          i18n-nyckel
   * @param fallback
   *          text som returneras när nyckeln saknas
   * @return översättningen eller fallback (aldrig null om fallback inte är
   *         null)
   */
  String t(String key, String fallback);

  /** Källa som alltid svarar med fallback (används när inga meddelanden laddats). */
  static PermissionOverviewMessages fallbackOnly() {
    return (key, fallback) -> fallback;
  }

  /**
   * Adapter mot RODA:s serverside-meddelanden. {@code null} ger
   * {@link #fallbackOnly()}.
   */
  static PermissionOverviewMessages of(Messages messages) {
    if (messages == null) {
      return fallbackOnly();
    }
    return (key, fallback) -> {
      String value = messages.getTranslation(key, fallback);
      return value != null ? value : fallback;
    };
  }
}

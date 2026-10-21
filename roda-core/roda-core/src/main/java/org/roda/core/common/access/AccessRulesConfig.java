/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.common.access;

import org.roda.core.RodaCoreFactory;

/**
 * Enda stället som läser konfignycklarna
 * {@code core.access_rules.*} ur roda-core.properties (via
 * {@link RodaCoreFactory}). Plugin, observer, applikator och IndexService
 * anropar hjälparna här i stället för att tolka nycklarna var för sig.
 *
 * <p>
 * Saknat eller tomt värde ger alltid leveransens standard (se
 * roda-core.properties); booleska värden tolkas med
 * {@link Boolean#parseBoolean(String)}.
 * </p>
 */
public final class AccessRulesConfig {

  /** Prefix för alla nycklar. */
  public static final String PREFIX = "core.access_rules";
  /** Hel nyckel för observerbrytaren (läses av IndexService ur den medskickade konfigurationen). */
  public static final String OBSERVER_ENABLED_PROPERTY = PREFIX + ".observer.enabled";

  /** Metadatatyp när core.access_rules.metadata_type saknas. */
  public static final String DEFAULT_METADATA_TYPE = "eterna_admin";
  /** Standard för core.access_rules.strip_write_on_deny. */
  public static final boolean DEFAULT_STRIP_WRITE_ON_DENY = true;

  static final String[] ENABLED_KEY = {"core", "access_rules", "enabled"};
  static final String[] OBSERVER_ENABLED_KEY = {"core", "access_rules", "observer", "enabled"};
  static final String[] OBSERVER_CREATE_EVENT_KEY = {"core", "access_rules", "observer", "create_event"};
  static final String[] DEFAULT_ACT_KEY = {"core", "access_rules", "default_lag"};
  static final String[] METADATA_TYPE_KEY = {"core", "access_rules", "metadata_type"};
  static final String[] STRIP_WRITE_KEY = {"core", "access_rules", "strip_write_on_deny"};
  static final String[] INGEST_CREATE_EVENT_KEY = {"core", "access_rules", "ingest", "create_event"};

  private AccessRulesConfig() {
    // statisk hjälpklass
  }

  /** core.access_rules.enabled – huvudbrytaren (standard true). */
  public static boolean isEnabled() {
    return bool(ENABLED_KEY, true);
  }

  /** core.access_rules.observer.enabled (standard true). */
  public static boolean isObserverEnabled() {
    return bool(OBSERVER_ENABLED_KEY, true);
  }

  /**
   * core.access_rules.observer.create_event (standard true): PREMIS-händelse
   * när observern ändrar behörigheter.
   */
  public static boolean isObserverEventEnabled() {
    return bool(OBSERVER_CREATE_EVENT_KEY, true);
  }

  /** core.access_rules.ingest.create_event (standard true). */
  public static boolean isIngestEventEnabled() {
    return bool(INGEST_CREATE_EVENT_KEY, true);
  }

  /** core.access_rules.strip_write_on_deny (standard {@value #DEFAULT_STRIP_WRITE_ON_DENY}). */
  public static boolean isStripWriteOnDeny() {
    return bool(STRIP_WRITE_KEY, DEFAULT_STRIP_WRITE_ON_DENY);
  }

  /** core.access_rules.default_lag, trimmad, eller null om nyckeln saknas/är tom. */
  public static String defaultAct() {
    return string(DEFAULT_ACT_KEY);
  }

  /** core.access_rules.metadata_type, trimmad; {@value #DEFAULT_METADATA_TYPE} om nyckeln saknas/är tom. */
  public static String metadataType() {
    String type = string(METADATA_TYPE_KEY);
    return type == null ? DEFAULT_METADATA_TYPE : type;
  }

  private static boolean bool(String[] key, boolean defaultValue) {
    String value = RodaCoreFactory.getRodaConfigurationAsString(key);
    return value == null || value.isBlank() ? defaultValue : Boolean.parseBoolean(value.trim());
  }

  private static String string(String[] key) {
    String value = RodaCoreFactory.getRodaConfigurationAsString(key);
    return value == null || value.isBlank() ? null : value.trim();
  }
}

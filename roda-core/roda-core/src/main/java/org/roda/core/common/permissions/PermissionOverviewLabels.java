/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.common.permissions;

import org.roda.core.data.v2.ip.Permissions.PermissionType;

/**
 * I18n-nycklar (permission.overview.*) och svenska
 * fallback-texter som HTML- och CSV-renderarna (och byggarens varningar)
 * delar. Nycklarna finns i ServerMessages.properties (engelska) och
 * ServerMessages_sv_SE.properties (svenska); fallback används bara när en
 * nyckel saknas.
 */
final class PermissionOverviewLabels {

  static final String PREFIX = "permission.overview.";

  /** En etikett: nyckel + fallback. */
  record Label(String key, String fallback) {
    String in(PermissionOverviewMessages messages) {
      String value = messages != null ? messages.t(key, fallback) : fallback;
      return value != null ? value : fallback;
    }
  }

  /** Språkkoden (BCP 47) för meddelandefilen – används i {@code <html lang>}. */
  static final Label LANG = new Label(PREFIX + "lang", "sv");

  static final Label TITLE = new Label(PREFIX + "title", "Behörighetsöversikt");
  static final Label GENERATED_AT = new Label(PREFIX + "generatedAt", "Genererad");
  static final Label GENERATED_BY = new Label(PREFIX + "generatedBy", "av");
  static final Label PRINT = new Label(PREFIX + "print", "Skriv ut");

  static final Label SECTION_WARNINGS = new Label(PREFIX + "warnings", "Varningar – rapporten är ofullständig");
  static final Label SECTION_ROLES = new Label(PREFIX + "section.roles", "Rättigheter per grupp");
  static final Label SECTION_USERS = new Label(PREFIX + "section.users", "Användare");
  static final Label SECTION_NODES = new Label(PREFIX + "section.nodes", "Strukturenheter");
  static final Label SECTION_RULES = new Label(PREFIX + "section.rules", "Åtkomstregler");

  static final Label WARNING_GROUPS = new Label(PREFIX + "warning.groups", "Kunde inte lista grupper");
  static final Label WARNING_USERS = new Label(PREFIX + "warning.users", "Kunde inte lista användare");
  static final Label WARNING_NODES = new Label(PREFIX + "warning.nodes", "Kunde inte hämta strukturenheter");

  static final Label COL_AREA = new Label(PREFIX + "column.area", "Område");
  static final Label COL_ROLE = new Label(PREFIX + "column.role", "Rättighet");
  static final Label COL_DESCRIPTION = new Label(PREFIX + "column.description", "Beskrivning");
  static final Label COL_GROUP = new Label(PREFIX + "column.group", "Grupp");
  static final Label COL_USERNAME = new Label(PREFIX + "column.username", "Användarnamn");
  static final Label COL_FULL_NAME = new Label(PREFIX + "column.fullName", "Fullständigt namn");
  static final Label COL_EMAIL = new Label(PREFIX + "column.email", "E-post");
  static final Label COL_ACTIVE = new Label(PREFIX + "column.active", "Aktiv");
  static final Label COL_GROUPS = new Label(PREFIX + "column.groups", "Grupper");
  static final Label COL_USERS = new Label(PREFIX + "column.users", "Användare");
  static final Label COL_USER_COUNT = new Label(PREFIX + "column.userCount", "Antal användare");
  static final Label COL_DIRECT_ROLES = new Label(PREFIX + "column.directRoles", "Direkta rättigheter");
  static final Label COL_ALL_ROLES = new Label(PREFIX + "column.allRoles", "Effektiva rättigheter");
  static final Label COL_LEVEL = new Label(PREFIX + "column.level", "Nivå");
  static final Label COL_TITLE = new Label(PREFIX + "column.title", "Titel");
  static final Label COL_ID = new Label(PREFIX + "column.id", "Identifierare");
  static final Label COL_PARENT = new Label(PREFIX + "column.parent", "Överordnad");
  static final Label COL_RESTRICTED = new Label(PREFIX + "column.restrictedGroups", "Spärrade grupper");
  static final Label COL_SEKRETESS = new Label(PREFIX + "column.sekretess", "Sekretess");
  static final Label COL_LAGRUM = new Label(PREFIX + "column.lagrum", "Lagrum");
  static final Label COL_PERSONUPPGIFTER = new Label(PREFIX + "column.personuppgifter", "Personuppgifter");
  static final Label COL_PERSONUPPGIFTSNIVA = new Label(PREFIX + "column.personuppgiftsniva",
    "Personuppgiftsnivå");
  static final Label COL_NOTE = new Label(PREFIX + "column.note", "Anteckning");
  static final Label COL_ENABLED = new Label(PREFIX + "column.enabled", "Aktiv");

  static final Label YES = new Label(PREFIX + "yes", "Ja");
  static final Label NO = new Label(PREFIX + "no", "Nej");
  static final Label EMPTY = new Label(PREFIX + "empty", "–");

  static final Label GROUPS_NONE = new Label(PREFIX + "groups.none", "Inga grupper.");
  static final Label USERS_NONE = new Label(PREFIX + "users.none", "Inga användare.");
  static final Label NODES_NONE = new Label(PREFIX + "nodes.none", "Inga strukturenheter på de valda nivåerna.");
  static final Label RULES_NONE = new Label(PREFIX + "rules.none", "Inga åtkomstregler är aktiva.");
  static final Label RULES_EMPTY = new Label(PREFIX + "rules.empty",
    "Åtkomstreglerna är aktiva men inga regler är definierade.");

  private PermissionOverviewLabels() {
    // static utility class
  }

  /** Områdets rubrik (permission.overview.roleTitle.&lt;key&gt;), fallback nyckeln. */
  static String roleTitle(PermissionOverviewMessages messages, String key) {
    return messages.t(PREFIX + "roleTitle." + key, key);
  }

  /** Rättighetens beskrivning (permission.overview.role.&lt;role&gt;), fallback rättigheten själv. */
  static String roleDescription(PermissionOverviewMessages messages, String role) {
    return messages.t(PREFIX + "role." + role, role);
  }

  /** Objektbehörighetens namn (permission.overview.permission.&lt;TYPE&gt;). */
  static String permissionType(PermissionOverviewMessages messages, PermissionType type) {
    String fallback = switch (type) {
      case CREATE -> "Skapa";
      case READ -> "Läsa";
      case UPDATE -> "Uppdatera";
      case DELETE -> "Ta bort";
      case GRANT -> "Tilldela";
    };
    return messages.t(PREFIX + "permission." + type.name(), fallback);
  }

  /**
   * Nivåns namn, i fallande prioritet: permission.overview.level.&lt;level&gt;,
   * facettens översättning (ui.facets.IndexedAIP.level.&lt;level&gt;; finns
   * bara för fonds/series/file/item/other), EAD-crosswalkens översättning
   * (crosswalks.dissemination.html.ead.level.&lt;level&gt;; finns för hela
   * nivåvokabulären inkl. collection, subclass, subseries, recordgrp,
   * subgrp), annars nivån själv.
   */
  static String level(PermissionOverviewMessages messages, String level) {
    if (level == null || level.isEmpty()) {
      return EMPTY.in(messages);
    }
    String crosswalk = messages.t("crosswalks.dissemination.html.ead.level." + level, level);
    String facet = messages.t("ui.facets.IndexedAIP.level." + level, crosswalk);
    return messages.t(PREFIX + "level." + level, facet);
  }

  static String yesNo(PermissionOverviewMessages messages, boolean value) {
    return value ? YES.in(messages) : NO.in(messages);
  }
}

/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.common.permissions;

import static org.roda.core.common.permissions.PermissionOverviewLabels.COL_ACTIVE;
import static org.roda.core.common.permissions.PermissionOverviewLabels.COL_ALL_ROLES;
import static org.roda.core.common.permissions.PermissionOverviewLabels.COL_AREA;
import static org.roda.core.common.permissions.PermissionOverviewLabels.COL_DESCRIPTION;
import static org.roda.core.common.permissions.PermissionOverviewLabels.COL_DIRECT_ROLES;
import static org.roda.core.common.permissions.PermissionOverviewLabels.COL_EMAIL;
import static org.roda.core.common.permissions.PermissionOverviewLabels.COL_ENABLED;
import static org.roda.core.common.permissions.PermissionOverviewLabels.COL_FULL_NAME;
import static org.roda.core.common.permissions.PermissionOverviewLabels.COL_GROUP;
import static org.roda.core.common.permissions.PermissionOverviewLabels.COL_GROUPS;
import static org.roda.core.common.permissions.PermissionOverviewLabels.COL_ID;
import static org.roda.core.common.permissions.PermissionOverviewLabels.COL_LAGRUM;
import static org.roda.core.common.permissions.PermissionOverviewLabels.COL_LEVEL;
import static org.roda.core.common.permissions.PermissionOverviewLabels.COL_NOTE;
import static org.roda.core.common.permissions.PermissionOverviewLabels.COL_PARENT;
import static org.roda.core.common.permissions.PermissionOverviewLabels.COL_PERSONUPPGIFTER;
import static org.roda.core.common.permissions.PermissionOverviewLabels.COL_PERSONUPPGIFTSNIVA;
import static org.roda.core.common.permissions.PermissionOverviewLabels.COL_RESTRICTED;
import static org.roda.core.common.permissions.PermissionOverviewLabels.COL_ROLE;
import static org.roda.core.common.permissions.PermissionOverviewLabels.COL_SEKRETESS;
import static org.roda.core.common.permissions.PermissionOverviewLabels.COL_TITLE;
import static org.roda.core.common.permissions.PermissionOverviewLabels.COL_USERNAME;
import static org.roda.core.common.permissions.PermissionOverviewLabels.COL_USERS;
import static org.roda.core.common.permissions.PermissionOverviewLabels.GROUPS_NONE;
import static org.roda.core.common.permissions.PermissionOverviewLabels.NODES_NONE;
import static org.roda.core.common.permissions.PermissionOverviewLabels.RULES_EMPTY;
import static org.roda.core.common.permissions.PermissionOverviewLabels.RULES_NONE;
import static org.roda.core.common.permissions.PermissionOverviewLabels.SECTION_NODES;
import static org.roda.core.common.permissions.PermissionOverviewLabels.SECTION_ROLES;
import static org.roda.core.common.permissions.PermissionOverviewLabels.SECTION_RULES;
import static org.roda.core.common.permissions.PermissionOverviewLabels.SECTION_USERS;
import static org.roda.core.common.permissions.PermissionOverviewLabels.SECTION_WARNINGS;
import static org.roda.core.common.permissions.PermissionOverviewLabels.TITLE;
import static org.roda.core.common.permissions.PermissionOverviewLabels.USERS_NONE;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.roda.core.data.v2.accessrules.AccessRule;
import org.roda.core.data.v2.ip.Permissions.PermissionType;
import org.roda.core.data.v2.user.GroupOverview;
import org.roda.core.data.v2.user.NodePermission;
import org.roda.core.data.v2.user.PermissionOverview;
import org.roda.core.data.v2.user.RoleArea;
import org.roda.core.data.v2.user.UserOverview;

/**
 * Renderar {@link PermissionOverview} som CSV – en
 * sektion per avsnitt (ev. varningar, rättighetsmatris, användare,
 * strukturenheter, åtkomstregler) med sektionsrubriken på egen rad, tom rad
 * mellan sektionerna och semikolon som standardavgränsare (ETERNA:s
 * dokumenterade standard för csv.delimiter; samma nyckel som
 * SearchExportPlugin när den är satt). Flervärdesfält slås ihop med ", ".
 * Citattecken och avgränsare i värden hanteras av commons-csv (RFC 4180:
 * fältet citeras, citattecken dubbleras). Texten är UTF-8 utan BOM, som
 * övriga CSV-exporter.
 *
 * <p>
 * Rapporten är avsedd att öppnas i kalkylprogram och innehåller värden som
 * styrs av leverantörer och användare (AIP-titlar, namn, e-post,
 * anteckningar). Alla värdeceller passerar därför {@link #safe(String)} som
 * neutraliserar formelinjektion (CWE-1236): ett värde som börjar med
 * {@code = + - @}, tabb eller vagnretur får en inledande apostrof, så att
 * Excel/LibreOffice visar det som text i stället för att evaluera det.
 * Rubriker och översatta etiketter skyddas inte (de kommer från
 * konfigurationen).
 * </p>
 */
public final class PermissionOverviewCsvRenderer {

  /** Standardavgränsare (svensk/europeisk Excel-konvention, som csv.delimiter i roda-wui.properties). */
  public static final char DEFAULT_DELIMITER = ';';
  /** Markering i matrisen. */
  public static final String MARK = "x";
  /** Skiljetecken i flervärdesfält. */
  public static final String MULTI_VALUE_SEPARATOR = ", ";
  /** Prefix som neutraliserar formler i kalkylprogram. */
  public static final char FORMULA_GUARD = '\'';

  private PermissionOverviewCsvRenderer() {
    // static utility class
  }

  /** Renderar med {@link #DEFAULT_DELIMITER}. */
  public static String render(PermissionOverview overview, PermissionOverviewMessages messages) {
    return render(overview, messages, DEFAULT_DELIMITER);
  }

  /**
   * Renderar översikten som CSV.
   *
   * @param overview
   *          översikten (null behandlas som tom)
   * @param messages
   *          översättningar (null ⇒ fallback-texter)
   * @param delimiter
   *          fältavgränsare
   * @return CSV-texten (radslut \n)
   */
  public static String render(PermissionOverview overview, PermissionOverviewMessages messages, char delimiter) {
    PermissionOverview o = overview != null ? overview : new PermissionOverview();
    PermissionOverviewMessages m = messages != null ? messages : PermissionOverviewMessages.fallbackOnly();

    CSVFormat format = CSVFormat.DEFAULT.withDelimiter(delimiter).withRecordSeparator("\n");
    StringBuilder out = new StringBuilder(32 * 1024);
    try (CSVPrinter printer = new CSVPrinter(out, format)) {
      printer.printRecord(TITLE.in(m), nz(o.getGeneratedAt()), nz(o.getGeneratedBy()));
      printer.println();

      if (!o.getWarnings().isEmpty()) {
        renderWarnings(printer, o, m);
        printer.println();
      }
      renderRoleMatrix(printer, o, m);
      printer.println();
      renderUsers(printer, o, m);
      printer.println();
      renderNodes(printer, o, m);
      printer.println();
      renderRules(printer, o, m);
    } catch (IOException e) {
      // StringBuilder kastar aldrig IOException
      throw new UncheckedIOException(e);
    }
    return out.toString();
  }

  /**
   * Tolkar en konfigurerad avgränsare (csv.delimiter); null/tom ⇒
   * {@link #DEFAULT_DELIMITER}. Bara första tecknet används.
   */
  public static char delimiterOf(String configured) {
    if (configured == null || configured.isBlank()) {
      return DEFAULT_DELIMITER;
    }
    return configured.trim().charAt(0);
  }

  // ------------------------------------------------------------ 0. varningar

  private static void renderWarnings(CSVPrinter printer, PermissionOverview o, PermissionOverviewMessages m)
    throws IOException {
    printer.printRecord(SECTION_WARNINGS.in(m));
    for (String warning : o.getWarnings()) {
      printer.printRecord(nz(warning));
    }
  }

  // ------------------------------------------------------------ 1. matrisen

  private static void renderRoleMatrix(CSVPrinter printer, PermissionOverview o, PermissionOverviewMessages m)
    throws IOException {
    printer.printRecord(SECTION_ROLES.in(m));
    List<GroupOverview> groups = o.getGroups();
    if (groups.isEmpty()) {
      printer.printRecord(GROUPS_NONE.in(m));
      return;
    }
    List<String> header = new ArrayList<>();
    header.add(COL_AREA.in(m));
    header.add(COL_ROLE.in(m));
    header.add(COL_DESCRIPTION.in(m));
    for (GroupOverview group : groups) {
      // gruppnamn är användarstyrda även om de står i rubrikraden
      header.add(nz(group.getName()));
    }
    printer.printRecord(header);

    List<String> activeRow = new ArrayList<>();
    activeRow.add("");
    activeRow.add(COL_ACTIVE.in(m));
    activeRow.add("");
    for (GroupOverview group : groups) {
      activeRow.add(PermissionOverviewLabels.yesNo(m, group.isActive()));
    }
    printer.printRecord(activeRow);

    for (RoleArea area : o.getRoleAreas()) {
      String areaKey = area.getKey() != null ? area.getKey() : "";
      String areaTitle = safe(PermissionOverviewLabels.roleTitle(m, areaKey));
      for (String role : area.getRoles()) {
        List<String> row = new ArrayList<>();
        row.add(areaTitle);
        row.add(safe(role));
        String description = PermissionOverviewLabels.roleDescription(m, role);
        row.add(description != null && !description.equals(role) ? description : "");
        for (GroupOverview group : groups) {
          row.add(group.getAllRoles().contains(role) ? MARK : "");
        }
        printer.printRecord(row);
      }
    }
  }

  // ------------------------------------------------------------ 2. användare

  private static void renderUsers(CSVPrinter printer, PermissionOverview o, PermissionOverviewMessages m)
    throws IOException {
    printer.printRecord(SECTION_USERS.in(m));
    if (o.getUsers().isEmpty()) {
      printer.printRecord(USERS_NONE.in(m));
      return;
    }
    printer.printRecord(COL_USERNAME.in(m), COL_FULL_NAME.in(m), COL_EMAIL.in(m), COL_ACTIVE.in(m), COL_GROUPS.in(m),
      COL_DIRECT_ROLES.in(m), COL_ALL_ROLES.in(m));
    for (UserOverview user : o.getUsers()) {
      printer.printRecord(nz(user.getName()), nz(user.getFullName()), nz(user.getEmail()),
        PermissionOverviewLabels.yesNo(m, user.isActive()), join(user.getGroups()), join(user.getDirectRoles()),
        join(user.getAllRoles()));
    }
  }

  // ------------------------------------------------------- 3. strukturenheter

  private static void renderNodes(CSVPrinter printer, PermissionOverview o, PermissionOverviewMessages m)
    throws IOException {
    printer.printRecord(SECTION_NODES.in(m));
    if (o.getNodes().isEmpty()) {
      printer.printRecord(NODES_NONE.in(m));
      return;
    }
    List<String> header = new ArrayList<>();
    header.add(COL_LEVEL.in(m));
    header.add(COL_TITLE.in(m));
    header.add(COL_ID.in(m));
    header.add(COL_PARENT.in(m));
    String groupsLabel = COL_GROUPS.in(m);
    String usersLabel = COL_USERS.in(m);
    for (PermissionType type : PermissionType.values()) {
      String typeLabel = PermissionOverviewLabels.permissionType(m, type);
      header.add(typeLabel + " – " + lower(groupsLabel));
      header.add(typeLabel + " – " + lower(usersLabel));
    }
    header.add(COL_RESTRICTED.in(m));
    printer.printRecord(header);

    for (NodePermission node : o.getNodes()) {
      List<String> row = new ArrayList<>();
      row.add(PermissionOverviewLabels.level(m, node.getLevel()));
      row.add(nz(node.getTitle()));
      row.add(nz(node.getAipId()));
      row.add(nz(node.getParentId()));
      for (PermissionType type : PermissionType.values()) {
        row.add(join(node.getGroupsByType().get(type.name())));
        row.add(join(node.getUsersByType().get(type.name())));
      }
      row.add(join(node.getRestrictedGroups()));
      printer.printRecord(row);
    }
  }

  // --------------------------------------------------------- 4. åtkomstregler

  private static void renderRules(CSVPrinter printer, PermissionOverview o, PermissionOverviewMessages m)
    throws IOException {
    printer.printRecord(SECTION_RULES.in(m));
    if (!o.isRulesEnabled()) {
      printer.printRecord(RULES_NONE.in(m));
      return;
    }
    if (o.getRules().isEmpty()) {
      printer.printRecord(RULES_EMPTY.in(m));
      return;
    }
    printer.printRecord(COL_GROUP.in(m), COL_ENABLED.in(m), COL_SEKRETESS.in(m), COL_LAGRUM.in(m),
      COL_PERSONUPPGIFTER.in(m), COL_PERSONUPPGIFTSNIVA.in(m), COL_NOTE.in(m));
    for (AccessRule rule : o.getRules()) {
      printer.printRecord(nz(rule.getGroup()), PermissionOverviewLabels.yesNo(m, rule.isEnabled()),
        join(rule.getSekretess()), join(rule.getLagrum()), join(rule.getPersonuppgifter()),
        join(rule.getPersonuppgiftsniva()), nz(rule.getNote()));
    }
  }

  // ------------------------------------------------------------------ hjälpare

  /**
   * Neutraliserar formelinjektion i kalkylprogram: börjar värdet (efter
   * eventuella inledande blanksteg) med {@code =}, {@code +}, {@code -},
   * {@code @}, tabb eller vagnretur prefixas det med en apostrof
   * ({@link #FORMULA_GUARD}). null ⇒ tom sträng.
   */
  public static String safe(String value) {
    if (value == null || value.isEmpty()) {
      return "";
    }
    int start = 0;
    while (start < value.length() && value.charAt(start) == ' ') {
      start++;
    }
    if (start == value.length()) {
      return value;
    }
    char first = value.charAt(start);
    if (first == '=' || first == '+' || first == '-' || first == '@' || first == '\t' || first == '\r') {
      return FORMULA_GUARD + value;
    }
    return value;
  }

  /** Flervärdescell: värdena med {@link #MULTI_VALUE_SEPARATOR}, formelskyddad. */
  private static String join(Collection<String> values) {
    return values == null || values.isEmpty() ? "" : safe(String.join(MULTI_VALUE_SEPARATOR, values));
  }

  /** Värdecell: null ⇒ tom sträng, formelskyddad. */
  private static String nz(String value) {
    return safe(value);
  }

  private static String lower(String value) {
    return value != null ? value.toLowerCase() : "";
  }
}

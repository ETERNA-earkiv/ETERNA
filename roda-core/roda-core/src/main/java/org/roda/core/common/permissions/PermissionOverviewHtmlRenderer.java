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
import static org.roda.core.common.permissions.PermissionOverviewLabels.COL_PERSONUPPGIFTER;
import static org.roda.core.common.permissions.PermissionOverviewLabels.COL_PERSONUPPGIFTSNIVA;
import static org.roda.core.common.permissions.PermissionOverviewLabels.COL_RESTRICTED;
import static org.roda.core.common.permissions.PermissionOverviewLabels.COL_ROLE;
import static org.roda.core.common.permissions.PermissionOverviewLabels.COL_SEKRETESS;
import static org.roda.core.common.permissions.PermissionOverviewLabels.COL_TITLE;
import static org.roda.core.common.permissions.PermissionOverviewLabels.COL_USERNAME;
import static org.roda.core.common.permissions.PermissionOverviewLabels.COL_USERS;
import static org.roda.core.common.permissions.PermissionOverviewLabels.COL_USER_COUNT;
import static org.roda.core.common.permissions.PermissionOverviewLabels.EMPTY;
import static org.roda.core.common.permissions.PermissionOverviewLabels.GENERATED_AT;
import static org.roda.core.common.permissions.PermissionOverviewLabels.GENERATED_BY;
import static org.roda.core.common.permissions.PermissionOverviewLabels.GROUPS_NONE;
import static org.roda.core.common.permissions.PermissionOverviewLabels.LANG;
import static org.roda.core.common.permissions.PermissionOverviewLabels.NODES_NONE;
import static org.roda.core.common.permissions.PermissionOverviewLabels.PRINT;
import static org.roda.core.common.permissions.PermissionOverviewLabels.RULES_EMPTY;
import static org.roda.core.common.permissions.PermissionOverviewLabels.RULES_NONE;
import static org.roda.core.common.permissions.PermissionOverviewLabels.SECTION_NODES;
import static org.roda.core.common.permissions.PermissionOverviewLabels.SECTION_ROLES;
import static org.roda.core.common.permissions.PermissionOverviewLabels.SECTION_RULES;
import static org.roda.core.common.permissions.PermissionOverviewLabels.SECTION_USERS;
import static org.roda.core.common.permissions.PermissionOverviewLabels.SECTION_WARNINGS;
import static org.roda.core.common.permissions.PermissionOverviewLabels.TITLE;
import static org.roda.core.common.permissions.PermissionOverviewLabels.USERS_NONE;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.roda.core.data.v2.accessrules.AccessRule;
import org.roda.core.data.v2.ip.Permissions.PermissionType;
import org.roda.core.data.v2.user.GroupOverview;
import org.roda.core.data.v2.user.NodePermission;
import org.roda.core.data.v2.user.PermissionOverview;
import org.roda.core.data.v2.user.RoleArea;
import org.roda.core.data.v2.user.UserOverview;

/**
 * Renderar {@link PermissionOverview} som ett
 * fristående, utskrivbart HTML-dokument (inline-CSS, @media print, inga
 * externa resurser). Fyra avsnitt: rättighetsmatris per grupp, användare,
 * strukturenheter och åtkomstregler. Finns {@link PermissionOverview#getWarnings()}
 * visas de som en varningsbanner före första avsnittet, så att en
 * ofullständig rapport aldrig ser komplett ut. All text HTML-escapas.
 * Texterna hämtas via {@link PermissionOverviewMessages} så att renderaren
 * kan testas utan RodaCoreFactory; {@code <html lang>} följer
 * meddelandefilens språk (permission.overview.lang).
 */
public final class PermissionOverviewHtmlRenderer {

  /** Markering i matrisen (U+2713 CHECK MARK). */
  public static final String CHECK = "✓";

  private static final String STYLE = """
    body{font-family:Arial,Helvetica,sans-serif;font-size:12px;color:#222;margin:24px;background:#fff}
    h1{font-size:20px;margin:0 0 4px}
    h2{font-size:15px;margin:28px 0 8px;padding-bottom:3px;border-bottom:1px solid #8E2430;color:#8E2430;page-break-after:avoid}
    p.meta{color:#555;margin:0 0 12px}
    table{border-collapse:collapse;width:100%;margin-bottom:12px}
    tr{page-break-inside:avoid}
    th,td{border:1px solid #bbb;padding:3px 6px;text-align:left;vertical-align:top}
    th{background:#eee;font-weight:bold}
    thead th{position:sticky;top:0}
    tr.area th{background:#f3e3e5}
    tr.info th,tr.info td{color:#555;font-weight:normal;font-size:11px}
    table.matrix td.y{text-align:center;font-weight:bold;color:#1a6b2a}
    table.matrix td.n{text-align:center;color:#ccc}
    table.matrix th.role{font-weight:normal}
    code{font-size:11px;color:#555;white-space:nowrap}
    .muted{color:#777}
    .toolbar{margin-bottom:12px}
    .inactive{color:#999;font-style:italic}
    .warning{border:1px solid #b00020;background:#fdecea;color:#5a0010;padding:8px 12px;margin:0 0 16px}
    .warning ul{margin:4px 0 0;padding-left:20px}
    @media print{
      body{margin:10mm;font-size:10px}
      .toolbar{display:none}
      thead{display:table-header-group}
      thead th{position:static}
      h2{page-break-before:auto}
    }
    """;

  private PermissionOverviewHtmlRenderer() {
    // static utility class
  }

  /**
   * Renderar översikten.
   *
   * @param overview
   *          översikten (null behandlas som tom)
   * @param messages
   *          översättningar (null ⇒ fallback-texter)
   * @return komplett HTML-dokument
   */
  public static String render(PermissionOverview overview, PermissionOverviewMessages messages) {
    PermissionOverview o = overview != null ? overview : new PermissionOverview();
    PermissionOverviewMessages m = messages != null ? messages : PermissionOverviewMessages.fallbackOnly();

    String heading = TITLE.in(m) + " – " + nz(o.getGeneratedAt()) + " – " + nz(o.getGeneratedBy());

    StringBuilder html = new StringBuilder(64 * 1024);
    html.append("<!DOCTYPE html>\n<html lang=\"").append(esc(LANG.in(m))).append("\">\n<head>\n")
      .append("<meta charset=\"utf-8\">\n")
      .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n").append("<title>")
      .append(esc(heading)).append("</title>\n<style>\n").append(STYLE).append("</style>\n</head>\n<body>\n");
    html.append("<div class=\"toolbar\"><button type=\"button\" onclick=\"window.print()\">").append(esc(PRINT.in(m)))
      .append("</button></div>\n");
    html.append("<h1>").append(esc(heading)).append("</h1>\n");
    html.append("<p class=\"meta\">").append(esc(GENERATED_AT.in(m))).append(' ').append(esc(nz(o.getGeneratedAt())))
      .append(' ').append(esc(GENERATED_BY.in(m))).append(' ').append(esc(nz(o.getGeneratedBy()))).append(" · ")
      .append(o.getGroups().size()).append(' ').append(esc(lower(COL_GROUPS.in(m)))).append(", ")
      .append(o.getUsers().size()).append(' ').append(esc(lower(COL_USERS.in(m)))).append(", ")
      .append(o.getNodes().size()).append(' ').append(esc(lower(SECTION_NODES.in(m)))).append("</p>\n");

    renderWarnings(html, o, m);
    renderRoleMatrix(html, o, m);
    renderUsers(html, o, m);
    renderNodes(html, o, m);
    renderRules(html, o, m);

    html.append("</body>\n</html>\n");
    return html.toString();
  }

  // ------------------------------------------------------------ 0. varningar

  /** Varningsbanner (role="alert") före första avsnittet; inget alls när översikten är komplett. */
  private static void renderWarnings(StringBuilder html, PermissionOverview o, PermissionOverviewMessages m) {
    if (o.getWarnings().isEmpty()) {
      return;
    }
    html.append("<div class=\"warning\" role=\"alert\"><strong>").append(esc(SECTION_WARNINGS.in(m)))
      .append("</strong>\n<ul>\n");
    for (String warning : o.getWarnings()) {
      html.append("<li>").append(esc(nz(warning))).append("</li>\n");
    }
    html.append("</ul>\n</div>\n");
  }

  // ------------------------------------------------------------ 1. matrisen

  private static void renderRoleMatrix(StringBuilder html, PermissionOverview o, PermissionOverviewMessages m) {
    html.append("<h2>1. ").append(esc(SECTION_ROLES.in(m))).append("</h2>\n");
    List<GroupOverview> groups = o.getGroups();
    if (groups.isEmpty()) {
      html.append("<p>").append(esc(GROUPS_NONE.in(m))).append("</p>\n");
      return;
    }
    int columns = groups.size() + 1;
    html.append("<table class=\"matrix\">\n<thead>\n<tr><th>").append(esc(COL_ROLE.in(m))).append("</th>");
    for (GroupOverview group : groups) {
      html.append("<th").append(group.isActive() ? "" : " class=\"inactive\"").append('>')
        .append(esc(group.getName())).append("</th>");
    }
    html.append("</tr>\n</thead>\n<tbody>\n");

    // informationsrader om grupperna
    html.append("<tr class=\"info\"><th>").append(esc(COL_FULL_NAME.in(m))).append("</th>");
    for (GroupOverview group : groups) {
      html.append("<td>").append(esc(nz(group.getFullName()))).append("</td>");
    }
    html.append("</tr>\n<tr class=\"info\"><th>").append(esc(COL_ACTIVE.in(m))).append("</th>");
    for (GroupOverview group : groups) {
      html.append("<td>").append(esc(PermissionOverviewLabels.yesNo(m, group.isActive()))).append("</td>");
    }
    html.append("</tr>\n<tr class=\"info\"><th>").append(esc(COL_USER_COUNT.in(m))).append("</th>");
    for (GroupOverview group : groups) {
      html.append("<td>").append(group.getUsers().size()).append("</td>");
    }
    html.append("</tr>\n");

    for (RoleArea area : o.getRoleAreas()) {
      if (area.getRoles().isEmpty()) {
        continue;
      }
      html.append("<tr class=\"area\"><th colspan=\"").append(columns).append("\">")
        .append(esc(PermissionOverviewLabels.roleTitle(m, nz(area.getKey())))).append("</th></tr>\n");
      for (String role : area.getRoles()) {
        html.append("<tr><th scope=\"row\" class=\"role\">");
        String description = PermissionOverviewLabels.roleDescription(m, role);
        if (description != null && !description.equals(role)) {
          html.append(esc(description)).append(" <code>").append(esc(role)).append("</code>");
        } else {
          html.append("<code>").append(esc(role)).append("</code>");
        }
        html.append("</th>");
        for (GroupOverview group : groups) {
          boolean has = group.getAllRoles().contains(role);
          html.append(has ? "<td class=\"y\">" + CHECK + "</td>" : "<td class=\"n\"></td>");
        }
        html.append("</tr>\n");
      }
    }
    html.append("</tbody>\n</table>\n");
  }

  // ------------------------------------------------------------ 2. användare

  private static void renderUsers(StringBuilder html, PermissionOverview o, PermissionOverviewMessages m) {
    html.append("<h2>2. ").append(esc(SECTION_USERS.in(m))).append("</h2>\n");
    if (o.getUsers().isEmpty()) {
      html.append("<p>").append(esc(USERS_NONE.in(m))).append("</p>\n");
      return;
    }
    html.append("<table>\n<thead>\n<tr>");
    th(html, COL_USERNAME.in(m));
    th(html, COL_FULL_NAME.in(m));
    th(html, COL_EMAIL.in(m));
    th(html, COL_ACTIVE.in(m));
    th(html, COL_GROUPS.in(m));
    th(html, COL_DIRECT_ROLES.in(m));
    th(html, COL_ALL_ROLES.in(m));
    html.append("</tr>\n</thead>\n<tbody>\n");
    for (UserOverview user : o.getUsers()) {
      html.append("<tr").append(user.isActive() ? "" : " class=\"inactive\"").append('>');
      td(html, user.getName());
      td(html, user.getFullName());
      td(html, user.getEmail());
      td(html, PermissionOverviewLabels.yesNo(m, user.isActive()));
      td(html, join(user.getGroups(), m));
      td(html, join(user.getDirectRoles(), m));
      td(html, join(user.getAllRoles(), m));
      html.append("</tr>\n");
    }
    html.append("</tbody>\n</table>\n");
  }

  // ------------------------------------------------------- 3. strukturenheter

  private static void renderNodes(StringBuilder html, PermissionOverview o, PermissionOverviewMessages m) {
    html.append("<h2>3. ").append(esc(SECTION_NODES.in(m))).append("</h2>\n");
    if (o.getNodes().isEmpty()) {
      html.append("<p>").append(esc(NODES_NONE.in(m))).append("</p>\n");
      return;
    }
    Set<String> knownIds = new HashSet<>();
    for (NodePermission node : o.getNodes()) {
      knownIds.add(node.getAipId());
    }
    html.append("<table>\n<thead>\n<tr>");
    th(html, COL_LEVEL.in(m));
    th(html, COL_TITLE.in(m));
    th(html, COL_ID.in(m));
    for (PermissionType type : PermissionType.values()) {
      th(html, PermissionOverviewLabels.permissionType(m, type));
    }
    th(html, COL_RESTRICTED.in(m));
    html.append("</tr>\n</thead>\n<tbody>\n");
    String groupsLabel = COL_GROUPS.in(m);
    String usersLabel = COL_USERS.in(m);
    for (NodePermission node : o.getNodes()) {
      html.append("<tr>");
      td(html, PermissionOverviewLabels.level(m, node.getLevel()));
      html.append("<td>").append(esc(nz(node.getTitle())));
      if (node.getParentId() != null && !node.getParentId().isEmpty() && !knownIds.contains(node.getParentId())) {
        // överordnad utanför de valda nivåerna/behörigheten: visa id:t
        html.append(" <span class=\"muted\">(↑ ").append(esc(node.getParentId())).append(")</span>");
      }
      html.append("</td>");
      html.append("<td><code>").append(esc(nz(node.getAipId()))).append("</code></td>");
      for (PermissionType type : PermissionType.values()) {
        List<String> groups = node.getGroupsByType().get(type.name());
        List<String> users = node.getUsersByType().get(type.name());
        boolean hasGroups = groups != null && !groups.isEmpty();
        boolean hasUsers = users != null && !users.isEmpty();
        html.append("<td>");
        if (!hasGroups && !hasUsers) {
          html.append("<span class=\"muted\">").append(esc(EMPTY.in(m))).append("</span>");
        } else {
          if (hasGroups) {
            html.append("<div><span class=\"muted\">").append(esc(groupsLabel)).append(":</span> ")
              .append(esc(String.join(", ", groups))).append("</div>");
          }
          if (hasUsers) {
            html.append("<div><span class=\"muted\">").append(esc(usersLabel)).append(":</span> ")
              .append(esc(String.join(", ", users))).append("</div>");
          }
        }
        html.append("</td>");
      }
      td(html, join(node.getRestrictedGroups(), m));
      html.append("</tr>\n");
    }
    html.append("</tbody>\n</table>\n");
  }

  // --------------------------------------------------------- 4. åtkomstregler

  private static void renderRules(StringBuilder html, PermissionOverview o, PermissionOverviewMessages m) {
    html.append("<h2>4. ").append(esc(SECTION_RULES.in(m))).append("</h2>\n");
    if (!o.isRulesEnabled()) {
      html.append("<p>").append(esc(RULES_NONE.in(m))).append("</p>\n");
      return;
    }
    if (o.getRules().isEmpty()) {
      html.append("<p>").append(esc(RULES_EMPTY.in(m))).append("</p>\n");
      return;
    }
    html.append("<table>\n<thead>\n<tr>");
    th(html, COL_GROUP.in(m));
    th(html, COL_ENABLED.in(m));
    th(html, COL_SEKRETESS.in(m));
    th(html, COL_LAGRUM.in(m));
    th(html, COL_PERSONUPPGIFTER.in(m));
    th(html, COL_PERSONUPPGIFTSNIVA.in(m));
    th(html, COL_NOTE.in(m));
    html.append("</tr>\n</thead>\n<tbody>\n");
    for (AccessRule rule : o.getRules()) {
      html.append("<tr").append(rule.isEnabled() ? "" : " class=\"inactive\"").append('>');
      td(html, rule.getGroup());
      td(html, PermissionOverviewLabels.yesNo(m, rule.isEnabled()));
      td(html, join(rule.getSekretess(), m));
      td(html, join(rule.getLagrum(), m));
      td(html, join(rule.getPersonuppgifter(), m));
      td(html, join(rule.getPersonuppgiftsniva(), m));
      td(html, rule.getNote());
      html.append("</tr>\n");
    }
    html.append("</tbody>\n</table>\n");
  }

  // ------------------------------------------------------------------ hjälpare

  private static void th(StringBuilder html, String text) {
    html.append("<th>").append(esc(text)).append("</th>");
  }

  private static void td(StringBuilder html, String text) {
    html.append("<td>").append(esc(nz(text))).append("</td>");
  }

  private static String join(Collection<String> values, PermissionOverviewMessages m) {
    if (values == null || values.isEmpty()) {
      return EMPTY.in(m);
    }
    return String.join(", ", values);
  }

  private static String nz(String value) {
    return value != null ? value : "";
  }

  private static String lower(String value) {
    return value != null ? value.toLowerCase() : "";
  }

  /** HTML-escapar &amp; &lt; &gt; " och '. null ger tom sträng. */
  public static String esc(String value) {
    if (value == null || value.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder(value.length() + 16);
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      switch (c) {
        case '&' -> sb.append("&amp;");
        case '<' -> sb.append("&lt;");
        case '>' -> sb.append("&gt;");
        case '"' -> sb.append("&quot;");
        case '\'' -> sb.append("&#39;");
        default -> sb.append(c);
      }
    }
    return sb.toString();
  }
}

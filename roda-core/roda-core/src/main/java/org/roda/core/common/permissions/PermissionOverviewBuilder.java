/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.common.permissions;

import static org.roda.core.common.permissions.PermissionOverviewLabels.WARNING_GROUPS;
import static org.roda.core.common.permissions.PermissionOverviewLabels.WARNING_NODES;
import static org.roda.core.common.permissions.PermissionOverviewLabels.WARNING_USERS;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.roda.core.RodaCoreFactory;
import org.roda.core.common.access.AccessRuleRegistry;
import org.roda.core.common.permissions.PermissionOverviewLabels.Label;
import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.exceptions.GenericException;
import org.roda.core.data.exceptions.RODAException;
import org.roda.core.data.exceptions.RequestNotValidException;
import org.roda.core.data.v2.accessrules.AccessRule;
import org.roda.core.data.v2.accessrules.AccessRuleSet;
import org.roda.core.data.v2.accessrules.AccessRuleState;
import org.roda.core.data.v2.index.filter.Filter;
import org.roda.core.data.v2.index.filter.OneOfManyFilterParameter;
import org.roda.core.data.v2.ip.AIP;
import org.roda.core.data.v2.ip.IndexedAIP;
import org.roda.core.data.v2.ip.Permissions;
import org.roda.core.data.v2.ip.Permissions.PermissionType;
import org.roda.core.data.v2.user.Group;
import org.roda.core.data.v2.user.GroupOverview;
import org.roda.core.data.v2.user.NodePermission;
import org.roda.core.data.v2.user.PermissionOverview;
import org.roda.core.data.v2.user.RoleArea;
import org.roda.core.data.v2.user.User;
import org.roda.core.data.v2.user.UserOverview;
import org.roda.core.index.IndexService;
import org.roda.core.model.ModelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bygger {@link PermissionOverview} – en
 * ögonblicksbild av rollkatalogen, grupper, användare, objektbehörigheter
 * på strukturenheter och åtkomstreglerna.
 *
 * <p>
 * Kärnan är den rena funktionen
 * {@link #build(ModelService, NodeSource, User, List, AccessRuleSet, List, Map, PermissionOverviewMessages)}
 * som bara arbetar på injicerade indata (modellen stubbas med
 * java.lang.reflect.Proxy, noderna via {@link NodeSource}) och därför kan
 * enhetstestas utan Solr eller RodaCoreFactory. Overloaden
 * {@link #build(ModelService, IndexService, User, PermissionOverviewMessages)}
 * slår upp noderna i indexet med anroparens READ-filter (administratörer
 * ser allt) och läser dessutom konfigurationen (ui.permissionOverview.levels,
 * ui.roleTitle, ui.role, core.access_rules.enabled) och regelregistret.
 * </p>
 *
 * <p>
 * Fel vid listning av grupper, användare eller strukturenheter avbryter
 * inte bygget: avsnittet blir tomt, felet loggas och en varning (på
 * rapportens språk, via {@link PermissionOverviewMessages}) läggs i
 * {@link PermissionOverview#getWarnings()} så att rapporten aldrig ser
 * komplett ut när den inte är det.
 * </p>
 *
 * <p>
 * Rollområdena grupperas som i PermissionsPanel: rättighetens prefix före
 * första punkten matchar nyckeln i ui.roleTitle; prefix som saknar
 * ui.roleTitle läggs efter de konfigurerade områdena i den ordning de
 * påträffas, och rättigheter som finns på grupper/användare men inte i
 * ui.role hamnar under området {@value #OTHER_AREA}. Inom ett område behålls
 * konfigurationsordningen (samma som i gränssnittet); alla övriga listor är
 * sorterade.
 * </p>
 *
 * <p>
 * {@code restrictedGroups} per nod finns inte i indexet utan läses från
 * AIP:ns tillstånd via {@link ModelService#retrieveAIP(String)} – men bara
 * när reglerna är aktiva och hanterar minst en grupp, så att översikten
 * inte läser AIP-JSON i onödan.
 * </p>
 */
public final class PermissionOverviewBuilder {

  private static final Logger LOGGER = LoggerFactory.getLogger(PermissionOverviewBuilder.class);

  /** Konfigurationsnyckel (roda-wui.properties) för nivåerna som tas med; kommaseparerad eller upprepad. */
  public static final String CONFIG_LEVELS_KEY = RodaConstants.UI_PERMISSION_OVERVIEW_LEVELS;
  /** Rollkatalogens områden (roda-wui.properties). */
  public static final String CONFIG_ROLE_TITLES_KEY = "ui.roleTitle";
  /** Rollkatalogens rättigheter (roda-wui.properties). */
  public static final String CONFIG_ROLES_KEY = "ui.role";
  /** Huvudbrytaren för åtkomstreglerna (roda-core.properties). */
  public static final String CONFIG_RULES_ENABLED_KEY = "core.access_rules.enabled";
  /** Områdesnyckel för rättigheter utan konfigurerat område. */
  public static final String OTHER_AREA = "other";
  /** Nivåer som tas med när ui.permissionOverview.levels saknas. */
  public static final List<String> DEFAULT_LEVELS = RodaConstants.PERMISSION_OVERVIEW_DEFAULT_LEVELS;

  /**
   * Källa för strukturenheterna, så att indexet kan bytas ut i tester.
   * Returneras ett {@link Closeable} stängs det efter iterationen.
   */
  @FunctionalInterface
  public interface NodeSource {
    Iterable<IndexedAIP> find(Filter filter, User requester, List<String> fieldsToReturn)
      throws GenericException, RequestNotValidException;
  }

  private PermissionOverviewBuilder() {
    // static utility class
  }

  // ------------------------------------------------------------------ overloads

  /**
   * Som {@link #build(ModelService, IndexService, User, PermissionOverviewMessages)}
   * med fallback-texter (svenska) för varningarna.
   */
  public static PermissionOverview build(ModelService model, IndexService index, User requester) {
    return build(model, index, requester, PermissionOverviewMessages.fallbackOnly());
  }

  /**
   * Bekväm overload som läser konfiguration och regelregister.
   *
   * @param model
   *          modellen (användare, grupper, AIP-tillstånd)
   * @param index
   *          indexet (strukturenheter)
   * @param requester
   *          den som begär översikten; styr READ-filtret och
   *          {@code generatedBy}
   * @param messages
   *          översättningar för varningstexterna (null ⇒ fallback-texter)
   * @return översikten (aldrig null)
   */
  public static PermissionOverview build(ModelService model, IndexService index, User requester,
    PermissionOverviewMessages messages) {
    AccessRuleSet rules = AccessRuleRegistry.getInstance().get();
    if (!RodaCoreFactory.getRodaConfiguration().getBoolean(CONFIG_RULES_ENABLED_KEY, true)) {
      // huvudbrytaren av ⇒ reglerna tillämpas inte oavsett vad filen säger
      rules.setEnabled(false);
    }
    List<String> roleTitles = RodaCoreFactory.getRodaConfigurationAsList(CONFIG_ROLE_TITLES_KEY);
    List<String> roles = RodaCoreFactory.getRodaConfigurationAsList(CONFIG_ROLES_KEY);
    return build(model, nodeSource(index), requester, configuredLevels(), rules, roleTitles,
      groupRolesByPrefix(roles), messages);
  }

  /**
   * Som {@link #build(ModelService, NodeSource, User, List, AccessRuleSet, List, Map, PermissionOverviewMessages)}
   * med fallback-texter (svenska) för varningarna.
   */
  public static PermissionOverview build(ModelService model, NodeSource nodes, User requester, List<String> levels,
    AccessRuleSet rules, List<String> roleTitles, Map<String, List<String>> rolesByTitle) {
    return build(model, nodes, requester, levels, rules, roleTitles, rolesByTitle, null);
  }

  /**
   * Ren funktion över injicerade indata.
   *
   * @param model
   *          modellen; fel vid listning loggas, ger tomma listor och en
   *          varning i översikten
   * @param nodes
   *          nodkälla; fel (även under sidvis hämtning) loggas, ger tom
   *          nodlista och en varning i översikten
   * @param requester
   *          den som begär översikten (får vara null i tester)
   * @param levels
   *          nivåer som tas med, i önskad sorteringsordning; null/tom ⇒
   *          {@link #DEFAULT_LEVELS}
   * @param rules
   *          regeluppsättningen; null ⇒ inga regler
   * @param roleTitles
   *          områdesnycklar i visningsordning (ui.roleTitle)
   * @param rolesByTitle
   *          område → rättigheter (se {@link #groupRolesByPrefix(List)})
   * @param messages
   *          översättningar för varningstexterna (null ⇒ fallback-texter)
   * @return översikten (aldrig null)
   */
  public static PermissionOverview build(ModelService model, NodeSource nodes, User requester, List<String> levels,
    AccessRuleSet rules, List<String> roleTitles, Map<String, List<String>> rolesByTitle,
    PermissionOverviewMessages messages) {
    List<String> effectiveLevels = levels == null || levels.isEmpty() ? DEFAULT_LEVELS : levels;
    PermissionOverviewMessages m = messages != null ? messages : PermissionOverviewMessages.fallbackOnly();
    List<String> warnings = new ArrayList<>();

    PermissionOverview overview = new PermissionOverview();
    overview.setGeneratedAt(AccessRuleRegistry.nowIso());
    overview.setGeneratedBy(requester != null ? requester.getName() : null);
    overview.setGroups(buildGroups(model, m, warnings));
    overview.setUsers(buildUsers(model, m, warnings));
    overview.setRoleAreas(buildRoleAreas(roleTitles, rolesByTitle, overview.getGroups(), overview.getUsers()));
    overview.setNodes(buildNodes(model, nodes, requester, effectiveLevels, rules, m, warnings));
    overview.setRules(sortedRules(rules));
    overview.setRulesEnabled(rules != null && rules.isEnabled());
    overview.setWarnings(warnings);
    return overview;
  }

  /** Nodkälla över ett {@link IndexService} (bara aktiva AIP:er, anroparens READ-filter). */
  public static NodeSource nodeSource(IndexService index) {
    return (filter, user, fields) -> index.findAll(IndexedAIP.class, filter, user, true, fields);
  }

  // ------------------------------------------------------------- konfiguration

  /**
   * Nivåerna från ui.permissionOverview.levels. Värdet får vara
   * kommaseparerat ({@code fonds,class,series}) eller upprepat per rad;
   * blanksteg trimmas och dubbletter tas bort. Saknas nyckeln ⇒
   * {@link #DEFAULT_LEVELS}.
   */
  public static List<String> configuredLevels() {
    return parseLevels(RodaCoreFactory.getRodaConfigurationAsList(CONFIG_LEVELS_KEY));
  }

  /** Tolkar en konfigurerad nivålista (se {@link #configuredLevels()}). */
  public static List<String> parseLevels(List<String> configured) {
    Set<String> levels = new LinkedHashSet<>();
    if (configured != null) {
      for (String entry : configured) {
        if (entry == null) {
          continue;
        }
        for (String part : entry.split(",")) {
          String level = part.trim();
          if (!level.isEmpty()) {
            levels.add(level);
          }
        }
      }
    }
    return levels.isEmpty() ? DEFAULT_LEVELS : new ArrayList<>(levels);
  }

  /**
   * Grupperar rättigheter på prefixet före första punkten (som
   * PermissionsPanel); rättigheter utan punkt hamnar under
   * {@value #OTHER_AREA}. Ordningen inom ett område är den givna; dubbletter
   * tas bort.
   */
  public static Map<String, List<String>> groupRolesByPrefix(List<String> roles) {
    Map<String, List<String>> grouped = new LinkedHashMap<>();
    if (roles != null) {
      for (String role : roles) {
        if (role == null || role.trim().isEmpty()) {
          continue;
        }
        String trimmed = role.trim();
        int dot = trimmed.indexOf('.');
        String area = dot > 0 ? trimmed.substring(0, dot) : OTHER_AREA;
        List<String> list = grouped.computeIfAbsent(area, k -> new ArrayList<>());
        if (!list.contains(trimmed)) {
          list.add(trimmed);
        }
      }
    }
    return grouped;
  }

  // ------------------------------------------------------------------- grupper

  private static List<GroupOverview> buildGroups(ModelService model, PermissionOverviewMessages messages,
    List<String> warnings) {
    List<GroupOverview> result = new ArrayList<>();
    if (model == null) {
      return result;
    }
    List<Group> groups;
    try {
      groups = model.listGroups();
    } catch (GenericException | RuntimeException e) {
      LOGGER.error("Kunde inte lista grupper till behörighetsöversikten", e);
      warnings.add(warning(WARNING_GROUPS, messages, e));
      return result;
    }
    if (groups == null) {
      return result;
    }
    for (Group group : groups) {
      if (group == null) {
        continue;
      }
      GroupOverview item = new GroupOverview();
      item.setName(group.getName());
      item.setFullName(group.getFullName());
      item.setActive(group.isActive());
      item.setDirectRoles(sorted(group.getDirectRoles()));
      item.setAllRoles(sorted(group.getAllRoles()));
      item.setUsers(sorted(group.getUsers()));
      result.add(item);
    }
    result.sort(Comparator.comparing(GroupOverview::getName, nullSafeIgnoreCase()));
    return result;
  }

  // ----------------------------------------------------------------- användare

  private static List<UserOverview> buildUsers(ModelService model, PermissionOverviewMessages messages,
    List<String> warnings) {
    List<UserOverview> result = new ArrayList<>();
    if (model == null) {
      return result;
    }
    List<User> users;
    try {
      users = model.listUsers();
    } catch (GenericException | RuntimeException e) {
      LOGGER.error("Kunde inte lista användare till behörighetsöversikten", e);
      warnings.add(warning(WARNING_USERS, messages, e));
      return result;
    }
    if (users == null) {
      return result;
    }
    for (User user : users) {
      if (user == null) {
        continue;
      }
      UserOverview item = new UserOverview();
      item.setName(user.getName());
      item.setFullName(user.getFullName());
      item.setEmail(user.getEmail());
      item.setActive(user.isActive());
      item.setGroups(sorted(user.getGroups()));
      item.setDirectRoles(sorted(user.getDirectRoles()));
      item.setAllRoles(sorted(user.getAllRoles()));
      result.add(item);
    }
    result.sort(Comparator.comparing(UserOverview::getName, nullSafeIgnoreCase()));
    return result;
  }

  // --------------------------------------------------------------- rollområden

  private static List<RoleArea> buildRoleAreas(List<String> roleTitles, Map<String, List<String>> rolesByTitle,
    List<GroupOverview> groups, List<UserOverview> users) {
    Map<String, List<String>> areas = new LinkedHashMap<>();
    if (roleTitles != null) {
      for (String title : roleTitles) {
        if (title != null && !title.trim().isEmpty()) {
          areas.put(title.trim(), new ArrayList<>());
        }
      }
    }
    if (rolesByTitle != null) {
      for (Map.Entry<String, List<String>> entry : rolesByTitle.entrySet()) {
        if (entry.getKey() == null) {
          continue;
        }
        List<String> target = areas.computeIfAbsent(entry.getKey(), k -> new ArrayList<>());
        if (entry.getValue() != null) {
          for (String role : entry.getValue()) {
            if (role != null && !role.isEmpty() && !target.contains(role)) {
              target.add(role);
            }
          }
        }
      }
    }

    // rättigheter som används men saknas i katalogen ⇒ "other", sorterade
    Set<String> configured = new HashSet<>();
    areas.values().forEach(configured::addAll);
    Set<String> extra = new TreeSet<>();
    for (GroupOverview group : groups) {
      extra.addAll(group.getAllRoles());
    }
    for (UserOverview user : users) {
      extra.addAll(user.getAllRoles());
    }
    extra.removeAll(configured);
    if (!extra.isEmpty()) {
      List<String> other = areas.computeIfAbsent(OTHER_AREA, k -> new ArrayList<>());
      for (String role : extra) {
        if (!other.contains(role)) {
          other.add(role);
        }
      }
    }

    List<RoleArea> result = new ArrayList<>();
    for (Map.Entry<String, List<String>> entry : areas.entrySet()) {
      result.add(new RoleArea(entry.getKey(), entry.getValue()));
    }
    return result;
  }

  // --------------------------------------------------------------------- noder

  private static List<NodePermission> buildNodes(ModelService model, NodeSource nodes, User requester,
    List<String> levels, AccessRuleSet rules, PermissionOverviewMessages messages, List<String> warnings) {
    List<NodePermission> result = new ArrayList<>();
    if (nodes == null) {
      return result;
    }
    List<String> fields = new ArrayList<>(RodaConstants.AIP_PERMISSIONS_FIELDS_TO_RETURN);
    fields.add(RodaConstants.AIP_TITLE);
    fields.add(RodaConstants.AIP_LEVEL);
    fields.add(RodaConstants.AIP_PARENT_ID);
    Filter filter = new Filter(new OneOfManyFilterParameter(RodaConstants.AIP_LEVEL, new ArrayList<>(levels)));

    boolean lookupState = model != null && rules != null && rules.isEnabled() && !rules.getManagedGroups().isEmpty();

    // Hela hämtningen ligger i try/catch: IndexResultIterator hämtar sidorna
    // lat och kastar NoSuchElementException när Solr fallerar mitt i, så ett
    // fel kan komma lika gärna från loopen som från find().
    Iterable<IndexedAIP> found = null;
    try {
      found = nodes.find(filter, requester, fields);
      if (found != null) {
        for (IndexedAIP aip : found) {
          if (aip != null) {
            result.add(toNode(aip, lookupState ? model : null));
          }
        }
      }
    } catch (GenericException | RequestNotValidException | RuntimeException e) {
      LOGGER.error("Kunde inte hämta strukturenheter ({}) till behörighetsöversikten", levels, e);
      warnings.add(warning(WARNING_NODES, messages, e));
      // hellre tomt avsnitt + varning än ett avsnitt som ser komplett ut men saknar noder
      result.clear();
      return result;
    } finally {
      close(found);
    }

    result.sort(nodeOrder(levels));
    return result;
  }

  private static void close(Iterable<IndexedAIP> found) {
    if (found instanceof Closeable closeable) {
      try {
        closeable.close();
      } catch (IOException e) {
        LOGGER.debug("Kunde inte stänga indexresultatet", e);
      }
    }
  }

  private static NodePermission toNode(IndexedAIP aip, ModelService model) {
    NodePermission node = new NodePermission();
    node.setAipId(aip.getId());
    node.setLevel(aip.getLevel());
    node.setTitle(aip.getTitle());
    node.setParentId(aip.getParentID());

    Permissions permissions = aip.getPermissions();
    Map<PermissionType, Set<String>> groups = permissions != null ? permissions.getGroups() : null;
    Map<PermissionType, Set<String>> users = permissions != null ? permissions.getUsers() : null;
    for (PermissionType type : PermissionType.values()) {
      node.getGroupsByType().put(type.name(), sorted(groups != null ? groups.get(type) : null));
      node.getUsersByType().put(type.name(), sorted(users != null ? users.get(type) : null));
    }

    if (model != null && aip.getId() != null) {
      try {
        AIP full = model.retrieveAIP(aip.getId());
        AccessRuleState state = full != null ? full.getAccessRuleState() : null;
        if (state != null) {
          node.setRestrictedGroups(sorted(state.getRestrictedGroups()));
        }
      } catch (RODAException | RuntimeException e) {
        LOGGER.debug("Kunde inte läsa regeltillstånd för AIP {}", aip.getId(), e);
      }
    }
    return node;
  }

  /** Nivåordning enligt konfigurationen (okända nivåer sist), sedan titel, sedan id. */
  static Comparator<NodePermission> nodeOrder(List<String> levels) {
    Comparator<String> text = nullSafeIgnoreCase();
    return Comparator.<NodePermission>comparingInt(node -> {
      int idx = node.getLevel() != null ? levels.indexOf(node.getLevel()) : -1;
      return idx < 0 ? Integer.MAX_VALUE : idx;
    }).thenComparing(NodePermission::getTitle, text).thenComparing(NodePermission::getAipId, text);
  }

  // -------------------------------------------------------------------- regler

  private static List<AccessRule> sortedRules(AccessRuleSet rules) {
    List<AccessRule> result = new ArrayList<>();
    if (rules != null && rules.getRules() != null) {
      for (AccessRule rule : rules.getRules()) {
        if (rule != null) {
          result.add(rule);
        }
      }
    }
    result.sort(Comparator.comparing(AccessRule::getGroup, nullSafeIgnoreCase()));
    return result;
  }

  // ------------------------------------------------------------------ hjälpare

  /**
   * Varningstext på rapportens språk: etiketten följt av felets meddelande
   * (eller exceptionklassen när meddelande saknas).
   */
  private static String warning(Label label, PermissionOverviewMessages messages, Exception e) {
    String detail = e.getMessage();
    if (detail == null || detail.isBlank()) {
      detail = e.getClass().getSimpleName();
    }
    return label.in(messages) + ": " + detail;
  }

  /** Sorterad kopia (skiftlägesokänslig, sedan exakt) utan null/tomma värden; null ⇒ tom lista. */
  static List<String> sorted(Collection<String> values) {
    List<String> result = new ArrayList<>();
    if (values != null) {
      for (String value : values) {
        if (value != null && !value.isEmpty() && !result.contains(value)) {
          result.add(value);
        }
      }
    }
    result.sort(nullSafeIgnoreCase());
    return result;
  }

  static Comparator<String> nullSafeIgnoreCase() {
    return Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER.thenComparing(Comparator.naturalOrder()));
  }
}

/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.common.permissions;

import java.io.Closeable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.exceptions.GenericException;
import org.roda.core.data.exceptions.NotFoundException;
import org.roda.core.data.v2.accessrules.AccessRule;
import org.roda.core.data.v2.accessrules.AccessRuleSet;
import org.roda.core.data.v2.accessrules.AccessRuleState;
import org.roda.core.data.v2.index.filter.Filter;
import org.roda.core.data.v2.index.filter.FilterParameter;
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
import org.roda.core.model.ModelService;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tester för {@link PermissionOverviewBuilder} mot en
 * stubbad {@link ModelService} (java.lang.reflect.Proxy) och en nodkälla i
 * minnet – ingen Solr, ingen RodaCoreFactory. Två grupper, tre användare,
 * fyra strukturenheter med olika behörigheter och ett regeltillstånd.
 */
@Test(groups = {RodaConstants.TEST_GROUP_ALL, RodaConstants.TEST_GROUP_DEV, RodaConstants.TEST_GROUP_TRAVIS,
  "accessrules"})
public class PermissionOverviewBuilderTest {

  private static final String SOC = "socialtjanst";
  private static final String HANDL = "handlaggare";
  private static final List<String> LEVELS = Arrays.asList("fonds", "class", "series");
  private static final List<String> ROLE_TITLES = Arrays.asList("aip", "member", "job");
  private static final List<String> ROLES = Arrays.asList("aip.view", "aip.read", "member.read", "job.read");

  // ---------------------------------------------------------------- modellstubb

  private static final class ModelStub implements InvocationHandler {
    final List<Group> groups = new ArrayList<>();
    final List<User> users = new ArrayList<>();
    final Map<String, AIP> aips = new HashMap<>();
    final List<String> retrieved = new ArrayList<>();
    boolean failUsers;
    boolean failGroups;

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      switch (method.getName()) {
        case "listGroups":
          if (failGroups) {
            // RuntimeException utan meddelande ⇒ varningen ska visa klassnamnet
            throw new IllegalStateException();
          }
          return new ArrayList<>(groups);
        case "listUsers":
          if (failUsers) {
            throw new GenericException("LDAP nere");
          }
          return new ArrayList<>(users);
        case "retrieveAIP": {
          String id = (String) args[0];
          retrieved.add(id);
          AIP aip = aips.get(id);
          if (aip == null) {
            throw new NotFoundException("AIP " + id + " saknas");
          }
          return aip;
        }
        case "toString":
          return "ModelStub";
        case "hashCode":
          return System.identityHashCode(proxy);
        case "equals":
          return proxy == args[0];
        default:
          throw new UnsupportedOperationException("Oväntat modellanrop: " + method.getName());
      }
    }

    ModelService proxy() {
      return (ModelService) Proxy.newProxyInstance(ModelService.class.getClassLoader(),
        new Class<?>[] {ModelService.class}, this);
    }
  }

  /** Nodkälla i minnet som dessutom sparar filtret och fältlistan. */
  private static final class NodeStub implements PermissionOverviewBuilder.NodeSource {
    final List<IndexedAIP> nodes = new ArrayList<>();
    Filter filter;
    List<String> fields;
    User user;

    @Override
    public Iterable<IndexedAIP> find(Filter filter, User requester, List<String> fieldsToReturn) {
      this.filter = filter;
      this.user = requester;
      this.fields = fieldsToReturn;
      return new ArrayList<>(nodes);
    }
  }

  /**
   * Nodkälla som levererar första elementet och sedan fallerar mitt i
   * iterationen – som IndexResultIterator gör (NoSuchElementException) när
   * Solr dör mellan två sidor. Registrerar om den stängts.
   */
  private static final class BrokenIterable implements Iterable<IndexedAIP>, Closeable {
    final IndexedAIP first;
    boolean closed;

    BrokenIterable(IndexedAIP first) {
      this.first = first;
    }

    @Override
    public Iterator<IndexedAIP> iterator() {
      return new Iterator<>() {
        int served = 0;

        @Override
        public boolean hasNext() {
          return true;
        }

        @Override
        public IndexedAIP next() {
          if (served++ == 0) {
            return first;
          }
          throw new NoSuchElementException("Error getting next item in list: Solr nere");
        }
      };
    }

    @Override
    public void close() {
      closed = true;
    }
  }

  // ------------------------------------------------------------------ fixtures

  private static Group group(String name, String fullName, boolean active, Set<String> roles, String... users) {
    Group group = new Group();
    group.setId(name);
    group.setName(name);
    group.setFullName(fullName);
    group.setActive(active);
    group.setDirectRoles(new LinkedHashSet<>(roles));
    group.setAllRoles(new LinkedHashSet<>(roles));
    group.setUsers(new LinkedHashSet<>(Arrays.asList(users)));
    return group;
  }

  private static User user(String name, String fullName, String email, boolean active, Set<String> groups,
    Set<String> directRoles, Set<String> allRoles) {
    User user = new User();
    user.setId(name);
    user.setName(name);
    user.setFullName(fullName);
    user.setEmail(email);
    user.setActive(active);
    user.setGroups(new LinkedHashSet<>(groups));
    user.setDirectRoles(new LinkedHashSet<>(directRoles));
    user.setAllRoles(new LinkedHashSet<>(allRoles));
    return user;
  }

  private static IndexedAIP node(String id, String level, String title, String parentId, Permissions permissions) {
    IndexedAIP aip = new IndexedAIP();
    aip.setId(id);
    aip.setLevel(level);
    aip.setTitle(title);
    aip.setParentID(parentId);
    aip.setPermissions(permissions);
    return aip;
  }

  private static Set<String> set(String... values) {
    return new LinkedHashSet<>(Arrays.asList(values));
  }

  private static AccessRuleSet rules(boolean enabled) {
    AccessRuleSet set = new AccessRuleSet();
    set.setEnabled(enabled);
    AccessRule soc = new AccessRule(SOC);
    soc.setSekretess(Arrays.asList("*"));
    AccessRule handl = new AccessRule(HANDL);
    handl.setSekretess(Arrays.asList("ingen_sekretess"));
    handl.setLagrum(Arrays.asList("OSL 26:1"));
    // avsiktligt osorterad ordning i filen
    set.setRules(new ArrayList<>(Arrays.asList(soc, handl)));
    return set;
  }

  /** Två grupper, tre användare, fyra noder (fonds, class, 2 × series) och ett regeltillstånd på s1. */
  private static final class Fixture {
    final ModelStub model = new ModelStub();
    final NodeStub nodes = new NodeStub();
    final User requester = user("admin", "Administratör", "admin@example.org", true, set("administrators"),
      set(), set());

    Fixture() {
      // grupper i omvänd ordning för att testa sorteringen
      model.groups.add(group(SOC, "Socialtjänsten", true, set("member.read", "aip.view", "aip.read"), "bertil",
        "anna"));
      model.groups.add(group(HANDL, "Handläggare", false, set("custom.role", "aip.read"), "cecilia", "bertil"));

      model.users.add(user("cecilia", "Cecilia C", "cecilia@example.org", true, set(HANDL), set(),
        set("custom.role", "aip.read")));
      model.users.add(user("anna", "Anna A", "anna@example.org", true, set(SOC), set("job.read"),
        set("member.read", "job.read", "aip.view", "aip.read")));
      model.users.add(user("bertil", "Bertil B", null, false, set(HANDL, SOC), set(),
        set("custom.role", "aip.read", "aip.view", "member.read")));

      Permissions f1 = new Permissions();
      f1.setGroupPermissions(SOC, EnumSet.of(PermissionType.READ, PermissionType.GRANT));
      Permissions c1 = new Permissions();
      c1.setGroupPermissions(SOC, EnumSet.of(PermissionType.READ));
      c1.setUserPermissions("anna", EnumSet.of(PermissionType.READ, PermissionType.UPDATE));
      Permissions s1 = new Permissions();
      s1.setGroupPermissions(SOC, EnumSet.of(PermissionType.READ));
      s1.setGroupPermissions(HANDL, EnumSet.of(PermissionType.READ));
      s1.setUserPermissions("anna", EnumSet.of(PermissionType.READ));
      Permissions s2 = new Permissions();

      // avsiktligt blandad ordning från "indexet"
      nodes.nodes.add(node("s1", "series", "Serie B", "c1", s1));
      nodes.nodes.add(node("c1", "class", "Klass A", "f1", c1));
      nodes.nodes.add(node("s2", "series", "Serie A", "c1", s2));
      nodes.nodes.add(node("f1", "fonds", "Arkiv X", null, f1));

      AIP s1Aip = new AIP();
      s1Aip.setId("s1");
      AccessRuleState state = new AccessRuleState();
      state.setRestrictedGroups(set(HANDL));
      s1Aip.setAccessRuleState(state);
      model.aips.put("s1", s1Aip);

      AIP f1Aip = new AIP();
      f1Aip.setId("f1");
      model.aips.put("f1", f1Aip);
      // c1 och s2 saknas i modellen ⇒ NotFoundException ⇒ tom lista, inget fel
    }

    PermissionOverview build(AccessRuleSet rules) {
      return PermissionOverviewBuilder.build(model.proxy(), nodes, requester, LEVELS, rules, ROLE_TITLES,
        PermissionOverviewBuilder.groupRolesByPrefix(ROLES));
    }
  }

  private static GroupOverview group(PermissionOverview overview, String name) {
    return overview.getGroups().stream().filter(g -> name.equals(g.getName())).findFirst()
      .orElseThrow(() -> new AssertionError("gruppen " + name + " saknas"));
  }

  private static UserOverview user(PermissionOverview overview, String name) {
    return overview.getUsers().stream().filter(u -> name.equals(u.getName())).findFirst()
      .orElseThrow(() -> new AssertionError("användaren " + name + " saknas"));
  }

  private static NodePermission node(PermissionOverview overview, String id) {
    return overview.getNodes().stream().filter(n -> id.equals(n.getAipId())).findFirst()
      .orElseThrow(() -> new AssertionError("noden " + id + " saknas"));
  }

  private static RoleArea area(PermissionOverview overview, String key) {
    return overview.getRoleAreas().stream().filter(a -> key.equals(a.getKey())).findFirst()
      .orElseThrow(() -> new AssertionError("området " + key + " saknas"));
  }

  private static List<String> names(List<?> items, java.util.function.Function<Object, String> name) {
    List<String> result = new ArrayList<>();
    for (Object item : items) {
      result.add(name.apply(item));
    }
    return result;
  }

  // -------------------------------------------------------------------- tester

  @Test
  public void testHeaderAndGroupsSortedWithEffectiveRoles() {
    Fixture fx = new Fixture();
    PermissionOverview overview = fx.build(rules(true));

    Assert.assertEquals(overview.getGeneratedBy(), "admin");
    Assert.assertNotNull(overview.getGeneratedAt());
    Assert.assertTrue(overview.getGeneratedAt().matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z"),
      overview.getGeneratedAt());
    Assert.assertTrue(overview.getWarnings().isEmpty(), "komplett översikt ⇒ inga varningar");

    Assert.assertEquals(names(overview.getGroups(), g -> ((GroupOverview) g).getName()),
      Arrays.asList(HANDL, SOC));
    GroupOverview soc = group(overview, SOC);
    Assert.assertEquals(soc.getFullName(), "Socialtjänsten");
    Assert.assertTrue(soc.isActive());
    Assert.assertEquals(soc.getAllRoles(), Arrays.asList("aip.read", "aip.view", "member.read"));
    Assert.assertEquals(soc.getDirectRoles(), Arrays.asList("aip.read", "aip.view", "member.read"));
    Assert.assertEquals(soc.getUsers(), Arrays.asList("anna", "bertil"));
    GroupOverview handl = group(overview, HANDL);
    Assert.assertFalse(handl.isActive());
    Assert.assertEquals(handl.getAllRoles(), Arrays.asList("aip.read", "custom.role"));
    Assert.assertEquals(handl.getUsers(), Arrays.asList("bertil", "cecilia"));
  }

  @Test
  public void testUsersSortedWithGroupsAndRoles() {
    Fixture fx = new Fixture();
    PermissionOverview overview = fx.build(rules(true));

    Assert.assertEquals(names(overview.getUsers(), u -> ((UserOverview) u).getName()),
      Arrays.asList("anna", "bertil", "cecilia"));
    UserOverview anna = user(overview, "anna");
    Assert.assertEquals(anna.getFullName(), "Anna A");
    Assert.assertEquals(anna.getEmail(), "anna@example.org");
    Assert.assertTrue(anna.isActive());
    Assert.assertEquals(anna.getGroups(), Collections.singletonList(SOC));
    Assert.assertEquals(anna.getDirectRoles(), Collections.singletonList("job.read"));
    Assert.assertEquals(anna.getAllRoles(), Arrays.asList("aip.read", "aip.view", "job.read", "member.read"));
    UserOverview bertil = user(overview, "bertil");
    Assert.assertFalse(bertil.isActive());
    Assert.assertNull(bertil.getEmail());
    Assert.assertEquals(bertil.getGroups(), Arrays.asList(HANDL, SOC));
  }

  @Test
  public void testRoleAreasFollowCatalogueAndCollectUnknownRoles() {
    Fixture fx = new Fixture();
    PermissionOverview overview = fx.build(rules(true));

    // konfigurerade områden i ui.roleTitle-ordning, därefter "other" med rättigheter utanför katalogen
    Assert.assertEquals(names(overview.getRoleAreas(), a -> ((RoleArea) a).getKey()),
      Arrays.asList("aip", "member", "job", PermissionOverviewBuilder.OTHER_AREA));
    // konfigurationsordningen (som i PermissionsPanel) behålls inom området
    Assert.assertEquals(area(overview, "aip").getRoles(), Arrays.asList("aip.view", "aip.read"));
    Assert.assertEquals(area(overview, "member").getRoles(), Collections.singletonList("member.read"));
    Assert.assertEquals(area(overview, PermissionOverviewBuilder.OTHER_AREA).getRoles(),
      Collections.singletonList("custom.role"));

    // matrisdata: område × grupp
    GroupOverview soc = group(overview, SOC);
    GroupOverview handl = group(overview, HANDL);
    Assert.assertTrue(soc.getAllRoles().contains("aip.read"));
    Assert.assertTrue(soc.getAllRoles().contains("member.read"));
    Assert.assertFalse(soc.getAllRoles().contains("custom.role"));
    Assert.assertFalse(soc.getAllRoles().contains("job.read"));
    Assert.assertTrue(handl.getAllRoles().contains("custom.role"));
    Assert.assertFalse(handl.getAllRoles().contains("member.read"));
  }

  @Test
  public void testNodesSortedByLevelThenTitleWithPermissionsPerType() {
    Fixture fx = new Fixture();
    PermissionOverview overview = fx.build(rules(true));

    Assert.assertEquals(names(overview.getNodes(), n -> ((NodePermission) n).getAipId()),
      Arrays.asList("f1", "c1", "s2", "s1"));

    NodePermission s1 = node(overview, "s1");
    Assert.assertEquals(s1.getLevel(), "series");
    Assert.assertEquals(s1.getTitle(), "Serie B");
    Assert.assertEquals(s1.getParentId(), "c1");
    for (PermissionType type : PermissionType.values()) {
      Assert.assertTrue(s1.getGroupsByType().containsKey(type.name()), type.name());
      Assert.assertTrue(s1.getUsersByType().containsKey(type.name()), type.name());
    }
    Assert.assertEquals(s1.getGroupsByType().get("READ"), Arrays.asList(HANDL, SOC));
    Assert.assertEquals(s1.getUsersByType().get("READ"), Collections.singletonList("anna"));
    Assert.assertTrue(s1.getGroupsByType().get("GRANT").isEmpty());

    NodePermission f1 = node(overview, "f1");
    Assert.assertNull(f1.getParentId());
    Assert.assertEquals(f1.getGroupsByType().get("GRANT"), Collections.singletonList(SOC));
    Assert.assertTrue(f1.getUsersByType().get("READ").isEmpty());

    NodePermission c1 = node(overview, "c1");
    Assert.assertEquals(c1.getUsersByType().get("UPDATE"), Collections.singletonList("anna"));
    Assert.assertEquals(c1.getUsersByType().get("READ"), Collections.singletonList("anna"));

    NodePermission s2 = node(overview, "s2");
    Assert.assertTrue(s2.getGroupsByType().get("READ").isEmpty());

    // filtret och fältlistan som skickades till "indexet"
    Assert.assertSame(fx.nodes.user, fx.requester);
    Assert.assertEquals(fx.nodes.filter.getParameters().size(), 1);
    FilterParameter parameter = fx.nodes.filter.getParameters().get(0);
    Assert.assertTrue(parameter instanceof OneOfManyFilterParameter, String.valueOf(parameter));
    Assert.assertEquals(parameter.getName(), RodaConstants.AIP_LEVEL);
    Assert.assertEquals(((OneOfManyFilterParameter) parameter).getValues(), LEVELS);
    Assert.assertTrue(fx.nodes.fields.containsAll(RodaConstants.AIP_PERMISSIONS_FIELDS_TO_RETURN));
    Assert.assertTrue(fx.nodes.fields.contains(RodaConstants.AIP_TITLE));
    Assert.assertTrue(fx.nodes.fields.contains(RodaConstants.AIP_LEVEL));
    Assert.assertTrue(fx.nodes.fields.contains(RodaConstants.AIP_PARENT_ID));
  }

  @Test
  public void testRestrictedGroupsFromRuleStateWhenRulesEnabled() {
    Fixture fx = new Fixture();
    PermissionOverview overview = fx.build(rules(true));

    Assert.assertTrue(overview.isRulesEnabled());
    Assert.assertEquals(node(overview, "s1").getRestrictedGroups(), Collections.singletonList(HANDL));
    Assert.assertTrue(node(overview, "f1").getRestrictedGroups().isEmpty());
    // c1/s2 saknas i modellen (NotFound) ⇒ tom lista utan att bygget avbryts
    Assert.assertTrue(node(overview, "c1").getRestrictedGroups().isEmpty());
    Assert.assertTrue(node(overview, "s2").getRestrictedGroups().isEmpty());
    Assert.assertEquals(new LinkedHashSet<>(fx.model.retrieved), set("f1", "c1", "s2", "s1"));

    // reglerna sorterade på grupp
    Assert.assertEquals(names(overview.getRules(), r -> ((AccessRule) r).getGroup()), Arrays.asList(HANDL, SOC));
    Assert.assertEquals(overview.getRules().get(0).getLagrum(), Collections.singletonList("OSL 26:1"));
  }

  @Test
  public void testRulesDisabledSkipsStateLookup() {
    Fixture fx = new Fixture();
    PermissionOverview overview = fx.build(rules(false));

    Assert.assertFalse(overview.isRulesEnabled());
    Assert.assertEquals(overview.getRules().size(), 2, "reglerna listas även när de är avstängda");
    Assert.assertTrue(fx.model.retrieved.isEmpty(), "ingen AIP ska läsas när reglerna är avstängda");
    Assert.assertTrue(node(overview, "s1").getRestrictedGroups().isEmpty());
  }

  @Test
  public void testNullRulesAndNoManagedGroups() {
    Fixture fx = new Fixture();
    PermissionOverview overview = fx.build(null);
    Assert.assertFalse(overview.isRulesEnabled());
    Assert.assertTrue(overview.getRules().isEmpty());
    Assert.assertTrue(fx.model.retrieved.isEmpty());

    AccessRuleSet enabledButEmpty = new AccessRuleSet();
    enabledButEmpty.setEnabled(true);
    fx = new Fixture();
    overview = fx.build(enabledButEmpty);
    Assert.assertTrue(overview.isRulesEnabled());
    Assert.assertTrue(fx.model.retrieved.isEmpty(), "inga hanterade grupper ⇒ ingen AIP-läsning");
  }

  @Test
  public void testModelFailureYieldsEmptyUsersAndWarning() {
    Fixture fx = new Fixture();
    fx.model.failUsers = true;
    PermissionOverview overview = fx.build(rules(true));

    Assert.assertTrue(overview.getUsers().isEmpty());
    Assert.assertEquals(overview.getGroups().size(), 2, "grupperna påverkas inte av användarfelet");
    Assert.assertEquals(overview.getNodes().size(), 4);
    // custom.role finns fortfarande via gruppen handlaggare
    Assert.assertEquals(area(overview, PermissionOverviewBuilder.OTHER_AREA).getRoles(),
      Collections.singletonList("custom.role"));
    // rapporten får inte se komplett ut: varning med felets meddelande
    Assert.assertEquals(overview.getWarnings(), Collections.singletonList("Kunde inte lista användare: LDAP nere"));
  }

  @Test
  public void testGroupFailureWithoutMessageYieldsWarningWithExceptionClass() {
    Fixture fx = new Fixture();
    fx.model.failGroups = true;
    PermissionOverview overview = fx.build(rules(true));

    Assert.assertTrue(overview.getGroups().isEmpty());
    Assert.assertEquals(overview.getUsers().size(), 3, "användarna påverkas inte av gruppfelet");
    Assert.assertEquals(overview.getWarnings(),
      Collections.singletonList("Kunde inte lista grupper: IllegalStateException"));
  }

  @Test
  public void testWarningsFollowMessages() {
    Fixture fx = new Fixture();
    fx.model.failUsers = true;
    PermissionOverviewMessages english = (key, fallback) -> "permission.overview.warning.users".equals(key)
      ? "Could not list users"
      : fallback;
    PermissionOverview overview = PermissionOverviewBuilder.build(fx.model.proxy(), fx.nodes, fx.requester, LEVELS,
      rules(true), ROLE_TITLES, PermissionOverviewBuilder.groupRolesByPrefix(ROLES), english);
    Assert.assertEquals(overview.getWarnings(), Collections.singletonList("Could not list users: LDAP nere"));
  }

  @Test
  public void testNodeSourceFailureYieldsEmptyNodesAndWarning() {
    Fixture fx = new Fixture();
    PermissionOverviewBuilder.NodeSource broken = (filter, user, fields) -> {
      throw new GenericException("Solr nere");
    };
    PermissionOverview overview = PermissionOverviewBuilder.build(fx.model.proxy(), broken, fx.requester, LEVELS,
      rules(true), ROLE_TITLES, PermissionOverviewBuilder.groupRolesByPrefix(ROLES));
    Assert.assertTrue(overview.getNodes().isEmpty());
    Assert.assertEquals(overview.getUsers().size(), 3);
    Assert.assertEquals(overview.getWarnings(),
      Collections.singletonList("Kunde inte hämta strukturenheter: Solr nere"));
  }

  @Test
  public void testNodeIterationFailureYieldsEmptyNodesWarningAndCloses() {
    Fixture fx = new Fixture();
    BrokenIterable broken = new BrokenIterable(fx.nodes.nodes.get(0));
    PermissionOverview overview = PermissionOverviewBuilder.build(fx.model.proxy(), (filter, user, fields) -> broken,
      fx.requester, LEVELS, rules(true), ROLE_TITLES, PermissionOverviewBuilder.groupRolesByPrefix(ROLES));

    // felet kom efter första noden: ingen halv nodlista, men en varning
    Assert.assertTrue(overview.getNodes().isEmpty(), String.valueOf(overview.getNodes()));
    Assert.assertEquals(overview.getWarnings(), Collections
      .singletonList("Kunde inte hämta strukturenheter: Error getting next item in list: Solr nere"));
    Assert.assertTrue(broken.closed, "indexresultatet ska stängas även när iterationen fallerar");
    Assert.assertEquals(overview.getUsers().size(), 3, "övriga avsnitt påverkas inte");
  }

  @Test
  public void testSortingIsCaseInsensitiveThenNaturalAndNodesTieBreakOnId() {
    Fixture fx = new Fixture();
    // "Bertil" (versal) och "bertil" finns båda: skiftlägesokänsligt först, sedan exakt (versal före gemen)
    fx.model.users.add(user("Bertil", "Bertil Stor", null, true, set(SOC), set(), set("aip.read")));
    fx.model.users.add(user("Adam", "Adam A", null, true, set(), set(), set()));
    fx.model.groups.add(group("Arkivarier", "Arkivarier", true, set("Aip.view", "aip.read", "aip.Delete", "b.role"),
      "Adam"));
    // två series-noder med exakt samma titel som en befintlig ⇒ id avgör
    Permissions none = new Permissions();
    fx.nodes.nodes.add(node("s3", "series", "Serie A", "c1", none));
    fx.nodes.nodes.add(node("s0", "series", "Serie A", "c1", none));
    PermissionOverview overview = fx.build(rules(false));

    Assert.assertEquals(names(overview.getUsers(), u -> ((UserOverview) u).getName()),
      Arrays.asList("Adam", "anna", "Bertil", "bertil", "cecilia"));
    Assert.assertEquals(names(overview.getGroups(), g -> ((GroupOverview) g).getName()),
      Arrays.asList("Arkivarier", HANDL, SOC));
    Assert.assertEquals(group(overview, "Arkivarier").getAllRoles(),
      Arrays.asList("aip.Delete", "aip.read", "Aip.view", "b.role"));
    Assert.assertEquals(names(overview.getNodes(), n -> ((NodePermission) n).getAipId()),
      Arrays.asList("f1", "c1", "s0", "s2", "s3", "s1"));
  }

  @Test
  public void testUnknownLevelSortsLastAndDefaultLevels() {
    Fixture fx = new Fixture();
    Permissions none = new Permissions();
    fx.nodes.nodes.add(node("x1", "item", "Handling", "s1", none));
    PermissionOverview overview = PermissionOverviewBuilder.build(fx.model.proxy(), fx.nodes, fx.requester, null,
      rules(false), ROLE_TITLES, PermissionOverviewBuilder.groupRolesByPrefix(ROLES));
    // null ⇒ standardnivåerna fonds,class,series; okänd nivå sist
    Assert.assertEquals(((OneOfManyFilterParameter) fx.nodes.filter.getParameters().get(0)).getValues(),
      PermissionOverviewBuilder.DEFAULT_LEVELS);
    Assert.assertEquals(names(overview.getNodes(), n -> ((NodePermission) n).getAipId()),
      Arrays.asList("f1", "c1", "s2", "s1", "x1"));
  }

  @Test
  public void testParseLevelsAndGroupRolesByPrefix() {
    Assert.assertEquals(PermissionOverviewBuilder.parseLevels(Arrays.asList("fonds, class ,series")),
      Arrays.asList("fonds", "class", "series"));
    Assert.assertEquals(PermissionOverviewBuilder.parseLevels(Arrays.asList("series", "fonds", "series")),
      Arrays.asList("series", "fonds"));
    Assert.assertEquals(PermissionOverviewBuilder.parseLevels(null), PermissionOverviewBuilder.DEFAULT_LEVELS);
    Assert.assertEquals(PermissionOverviewBuilder.parseLevels(Arrays.asList(" ", "")),
      PermissionOverviewBuilder.DEFAULT_LEVELS);

    Map<String, List<String>> grouped = PermissionOverviewBuilder
      .groupRolesByPrefix(Arrays.asList("aip.read", "aip.view", "aip.read", "member.read", "norole", null, " "));
    Assert.assertEquals(new ArrayList<>(grouped.keySet()),
      Arrays.asList("aip", "member", PermissionOverviewBuilder.OTHER_AREA));
    Assert.assertEquals(grouped.get("aip"), Arrays.asList("aip.read", "aip.view"));
    Assert.assertEquals(grouped.get(PermissionOverviewBuilder.OTHER_AREA), Collections.singletonList("norole"));
  }

  @Test
  public void testRoleTitleWithoutConfiguredRolesIsKeptEmptyAndPrefixOnlyGroupingWorks() {
    Fixture fx = new Fixture();
    // inga ui.roleTitle alls ⇒ gruppering enbart på prefix, i den ordning rättigheterna kommer
    PermissionOverview overview = PermissionOverviewBuilder.build(fx.model.proxy(), fx.nodes, fx.requester, LEVELS,
      null, null, PermissionOverviewBuilder.groupRolesByPrefix(Arrays.asList("member.read", "aip.read")));
    Assert.assertEquals(names(overview.getRoleAreas(), a -> ((RoleArea) a).getKey()),
      Arrays.asList("member", "aip", PermissionOverviewBuilder.OTHER_AREA));
    // aip.view och job.read används men finns inte i katalogen ⇒ sorterade under "other"
    Assert.assertEquals(area(overview, PermissionOverviewBuilder.OTHER_AREA).getRoles(),
      Arrays.asList("aip.view", "custom.role", "job.read"));

    // ett roleTitle utan rättigheter behålls (tomt område) så att ordningen från konfigurationen syns
    overview = PermissionOverviewBuilder.build(fx.model.proxy(), fx.nodes, fx.requester, LEVELS, null,
      Arrays.asList("risk", "aip"), PermissionOverviewBuilder.groupRolesByPrefix(Arrays.asList("aip.read")));
    Assert.assertEquals(area(overview, "risk").getRoles(), Collections.emptyList());
    Assert.assertEquals(names(overview.getRoleAreas(), a -> ((RoleArea) a).getKey()),
      Arrays.asList("risk", "aip", PermissionOverviewBuilder.OTHER_AREA));
  }
}

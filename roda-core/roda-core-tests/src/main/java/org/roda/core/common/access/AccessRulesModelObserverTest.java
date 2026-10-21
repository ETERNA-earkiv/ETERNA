/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.common.access;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.exceptions.GenericException;
import org.roda.core.data.exceptions.NotFoundException;
import org.roda.core.data.exceptions.ReturnWithExceptions;
import org.roda.core.data.v2.accessrules.AccessRule;
import org.roda.core.data.v2.accessrules.AccessRuleDefaults;
import org.roda.core.data.v2.accessrules.AccessRuleSet;
import org.roda.core.data.v2.accessrules.AccessRuleState;
import org.roda.core.data.v2.ip.AIP;
import org.roda.core.data.v2.ip.Permissions;
import org.roda.core.data.v2.ip.Permissions.PermissionType;
import org.roda.core.data.v2.ip.metadata.DescriptiveMetadata;
import org.roda.core.model.ModelObserver;
import org.roda.core.model.ModelService;
import org.roda.core.model.TransactionalModelService;
import org.roda.core.storage.Binary;
import org.roda.core.storage.ContentPayload;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tester för {@link AccessRulesModelObserver} mot
 * en stubbad {@link ModelService} (java.lang.reflect.Proxy, ingen
 * RodaCoreFactory): eterna_admin-händelser (RODA:s uppdelade typ-id
 * type="eterna", version="admin") utlöser omvärdering, andra metadatatyper
 * och representationsmetadata gör det inte, borttagning utlöser omvärdering,
 * aipPermissionsUpdated loopar inte, flytt utlöser omvärdering och
 * efterkommande-jobb (utom när AIP:n har egen eterna_admin), avstängd
 * observer är no-op, fel rapporteras utan att kastas, skrivningar och
 * händelser stämplas med den utlösande användaren, pluginets vakt
 * ({@link AccessRulesModelObserver#runGuarded}) stänger av observern,
 * efterkommande-jobb kan samlas ihop per batch och skjuts upp till efter
 * commit i transaktionellt läge, samt att gruppkontrollen mot katalogen
 * görs en gång per regeluppsättning.
 */
@Test(groups = {RodaConstants.TEST_GROUP_ALL, RodaConstants.TEST_GROUP_DEV, RodaConstants.TEST_GROUP_TRAVIS,
  "accessrules"})
public class AccessRulesModelObserverTest {

  private static final String SOC = "socialtjanst";
  private static final String HANDL = "handlaggare";
  private static final String ADMIN = "administrators";
  private static final String NS = EternaAdminParser.NAMESPACE;

  // ---------------------------------------------------------------- modellstubb

  /** Minimal modell: AIP:er per id, eterna_admin-XML per AIP-id, anropslogg. */
  private static final class ModelStub implements InvocationHandler {
    final Map<String, AIP> aips = new HashMap<>();
    final Map<String, String> xml = new HashMap<>();
    final Map<String, Integer> retrieveCount = new HashMap<>();
    final List<String> permissionsUpdated = new ArrayList<>();
    /** updatedBy per updateAIPPermissions-anrop. */
    final List<String> permissionsUpdatedBy = new ArrayList<>();
    final List<String> aipUpdated = new ArrayList<>();
    /** "aipId|agent" per createUpdateAIPEvent-anrop. */
    final List<String> events = new ArrayList<>();
    /** Efter-commit-åtgärder (bara i transaktionellt läge). */
    final List<Runnable> afterCommit = new ArrayList<>();
    int listGroupsCalls;
    /** Anropas efter updateAIPPermissions – simulerar modellens notifiering. */
    Consumer<AIP> permissionsHook;
    /** Om satt kastas detta av retrieveAIP. */
    Exception retrieveFailure;

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      switch (method.getName()) {
        case "retrieveAIP": {
          String id = (String) args[0];
          retrieveCount.merge(id, 1, Integer::sum);
          if (retrieveFailure != null) {
            throw retrieveFailure;
          }
          AIP aip = aips.get(id);
          if (aip == null) {
            throw new NotFoundException("AIP " + id + " saknas");
          }
          return aip;
        }
        case "retrieveDescriptiveMetadataBinary": {
          String content = xml.get((String) args[0]);
          if (content == null) {
            throw new NotFoundException("metadata saknas för " + args[0]);
          }
          return binary(content);
        }
        case "updateAIPPermissions": {
          AIP aip = (AIP) args[0];
          permissionsUpdated.add(aip.getId());
          permissionsUpdatedBy.add((String) args[1]);
          if (permissionsHook != null) {
            permissionsHook.accept(aip);
          }
          return null;
        }
        case "updateAIP":
          aipUpdated.add(((AIP) args[0]).getId());
          return args[0];
        case "createUpdateAIPEvent":
          // (aipId, representationId, filePath, fileId, eventType, description, state, outcomeText,
          // outcomeDetail, agentName, notify, eventId)
          events.add(args[0] + "|" + args[9]);
          return null;
        case "addAfterCommitAction":
          afterCommit.add((Runnable) args[0]);
          return null;
        case "listGroups":
          listGroupsCalls++;
          return Collections.emptyList();
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

    ModelService proxy(boolean transactional) {
      Class<?> type = transactional ? TransactionalModelService.class : ModelService.class;
      return (ModelService) Proxy.newProxyInstance(ModelService.class.getClassLoader(), new Class<?>[] {type}, this);
    }

    int retrieves(String aipId) {
      return retrieveCount.getOrDefault(aipId, 0);
    }
  }

  private static Binary binary(String content) {
    byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
    ContentPayload payload = (ContentPayload) Proxy.newProxyInstance(ContentPayload.class.getClassLoader(),
      new Class<?>[] {ContentPayload.class}, (proxy, method, args) -> {
        if ("createInputStream".equals(method.getName())) {
          return new ByteArrayInputStream(bytes);
        }
        throw new UnsupportedOperationException(method.getName());
      });
    return (Binary) Proxy.newProxyInstance(Binary.class.getClassLoader(), new Class<?>[] {Binary.class},
      (proxy, method, args) -> {
        if ("getContent".equals(method.getName())) {
          return payload;
        }
        if ("getSizeInBytes".equals(method.getName())) {
          return (long) bytes.length;
        }
        throw new UnsupportedOperationException(method.getName());
      });
  }

  // ---------------------------------------------------------------- observerbygge

  /** Observer med injicerad omgivning: räknar efterkommande-kontroller och jobbstarter. */
  private static final class Harness {
    final ModelStub model = new ModelStub();
    final List<String> descendantsChecked = new ArrayList<>();
    /** Förfäder-id:n per startat jobb. */
    final List<List<String>> jobsLaunched = new ArrayList<>();
    /** Utlösande användare per startat jobb. */
    final List<String> jobUsers = new ArrayList<>();
    boolean enabled = true;
    boolean createEvents = true;
    boolean hasDescendants;
    AccessRuleSet rules = rules("inherit");
    final AccessRulesModelObserver observer;

    Harness() {
      this(false);
    }

    Harness(boolean transactional) {
      ModelService proxy = model.proxy(transactional);
      observer = new AccessRulesModelObserver(proxy, () -> enabled, () -> rules,
        rules -> new AccessRulesApplier(proxy, rules, "OSL", "eterna_admin"), "eterna_admin", aipId -> {
          descendantsChecked.add(aipId);
          return hasDescendants;
        }, (aipIds, user) -> {
          jobsLaunched.add(new ArrayList<>(aipIds));
          jobUsers.add(user);
        }, () -> createEvents);
    }

    /** Registrerar en AIP med eterna_admin-XML (eller utan, om xml == null). */
    AIP aip(String id, String parentId, Permissions permissions, String xml) {
      AIP aip = new AIP();
      aip.setId(id);
      aip.setParentId(parentId);
      aip.setPermissions(permissions);
      aip.setDescriptiveMetadata(new ArrayList<>());
      if (xml != null) {
        aip.getDescriptiveMetadata().add(adminMetadata(id));
        model.xml.put(id, xml);
      }
      model.aips.put(id, aip);
      return aip;
    }
  }

  // ---------------------------------------------------------------- hjälpare

  private static DescriptiveMetadata adminMetadata(String aipId) {
    // så som RODA lagrar det konfigurerade typ-id:t "eterna_admin"
    return new DescriptiveMetadata("eterna_admin.xml", aipId, null, "eterna", "admin");
  }

  private static Set<PermissionType> perms(PermissionType... types) {
    return new HashSet<>(Arrays.asList(types));
  }

  /** SOC: READ+UPDATE, HANDL: READ, ADMIN: allt. */
  private static Permissions basePermissions() {
    Permissions p = new Permissions();
    p.setGroupPermissions(SOC, perms(PermissionType.READ, PermissionType.UPDATE));
    p.setGroupPermissions(HANDL, perms(PermissionType.READ));
    p.setGroupPermissions(ADMIN, perms(PermissionType.CREATE, PermissionType.READ, PermissionType.UPDATE,
      PermissionType.DELETE, PermissionType.GRANT));
    return p;
  }

  private static String xml(String sekretess, String sekretessgrund, String personuppgifter) {
    StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<eternaAdmin xmlns=\"" + NS
      + "\">\n");
    if (sekretess != null) {
      sb.append("  <sekretess>").append(sekretess).append("</sekretess>\n");
    }
    if (sekretessgrund != null) {
      sb.append("  <sekretessgrund>").append(sekretessgrund).append("</sekretessgrund>\n");
    }
    if (personuppgifter != null) {
      sb.append("  <personuppgifter>").append(personuppgifter).append("</personuppgifter>\n");
    }
    return sb.append("</eternaAdmin>\n").toString();
  }

  /** Sekretess med OSL 21:7 – SOC tillåts, HANDL nekas. */
  private static String secretXml() {
    return xml("sekretess_forekommer", "OSL 21 kap. 7 §", "inga");
  }

  private static AccessRule rule(String group, List<String> sekretess, List<String> lagrum,
    List<String> personuppgifter) {
    AccessRule rule = new AccessRule(group);
    rule.setSekretess(sekretess);
    rule.setLagrum(lagrum);
    rule.setPersonuppgifter(personuppgifter);
    rule.setPersonuppgiftsniva(List.of("*"));
    return rule;
  }

  /**
   * SOC får OSL 21 vid sekretess; HANDL bara ingen_sekretess.
   *
   * @param missingMetadata
   *          policy vid saknad eterna_admin: "inherit" (standard) eller "open"
   */
  private static AccessRuleSet rules(String missingMetadata) {
    AccessRuleSet set = new AccessRuleSet();
    set.setVersion(3);
    set.setEnabled(true);
    AccessRuleDefaults defaults = new AccessRuleDefaults();
    defaults.setMissingMetadata(missingMetadata);
    set.setDefaults(defaults);
    set.getRules().add(rule(SOC, List.of("ingen_sekretess", "sekretess_forekommer"), List.of("OSL 21"),
      List.of("inga", "forekommer")));
    set.getRules().add(rule(HANDL, List.of("ingen_sekretess"), List.of(), List.of("inga")));
    return set;
  }

  private static Set<String> readGroups(AIP aip) {
    return aip.getPermissions().getGroups().get(PermissionType.READ);
  }

  // ---------------------------------------------------------------- isAdmin

  @Test
  public void testIsAdminMatchesSplitAndPlainTypeIdOnAipLevelOnly() {
    AccessRulesModelObserver observer = new Harness().observer;

    Assert.assertTrue(observer.isAdmin(new DescriptiveMetadata("eterna_admin.xml", "a", null, "eterna", "admin")));
    Assert.assertTrue(observer.isAdmin(new DescriptiveMetadata("x.xml", "a", null, "eterna_admin", null)));
    Assert.assertTrue(observer.isAdmin(new DescriptiveMetadata("x.xml", "a", null, "Eterna", "Admin")));
    Assert.assertFalse(observer.isAdmin(new DescriptiveMetadata("ead_3.xml", "a", null, "ead", "3")));
    Assert.assertFalse(observer.isAdmin(new DescriptiveMetadata("eterna_admin.xml", "a", "rep1", "eterna", "admin")));
    Assert.assertFalse(observer.isAdmin(new DescriptiveMetadata("x.xml", "a", null, null, null)));
    Assert.assertFalse(observer.isAdmin(null));

    Assert.assertTrue(observer.isAdminId(null, "eterna_admin.xml"));
    Assert.assertTrue(observer.isAdminId(null, "ETERNA_ADMIN.xml"));
    Assert.assertFalse(observer.isAdminId("rep1", "eterna_admin.xml"));
    Assert.assertFalse(observer.isAdminId(null, "ead.xml"));
    Assert.assertFalse(observer.isAdminId(null, null));
  }

  // ---------------------------------------------------------------- metadatahändelser

  @Test
  public void testAdminMetadataCreatedTriggersReevaluation() {
    Harness h = new Harness();
    AIP aip = h.aip("a1", null, basePermissions(), secretXml());

    ReturnWithExceptions<Void, ModelObserver> ret = h.observer.descriptiveMetadataCreated(adminMetadata("a1"));

    Assert.assertTrue(ret.isEmpty(), ret.toString());
    Assert.assertSame(ret.getSource(), h.observer);
    // 1 av observern + 1 färsk kopia som appliern hämtar före skrivningen
    Assert.assertEquals(h.model.retrieves("a1"), 2);
    Assert.assertEquals(h.model.permissionsUpdated, List.of("a1"));
    // okänd utlösande användare (ingen updatedBy på AIP:n) ⇒ systemanvändaren
    Assert.assertEquals(h.model.permissionsUpdatedBy, List.of(AccessRulesModelObserver.USER));
    Assert.assertTrue(h.model.aipUpdated.isEmpty());
    Assert.assertEquals(readGroups(aip), Set.of(SOC, ADMIN));
    AccessRuleState state = aip.getAccessRuleState();
    Assert.assertNotNull(state);
    Assert.assertEquals(state.getRestrictedGroups(), Set.of(HANDL));
    Assert.assertEquals(state.getFactsSource(), AccessRulesApplier.FACTS_SOURCE_SELF);
    // spårbarhet: en PREMIS-händelse med den utlösande användaren som agent
    Assert.assertEquals(h.model.events, List.of("a1|" + AccessRulesModelObserver.USER));
    // efterkommande-kontroll gjord, inga efterkommande ⇒ inget jobb
    Assert.assertEquals(h.descendantsChecked, List.of("a1"));
    Assert.assertTrue(h.jobsLaunched.isEmpty());
  }

  @Test
  public void testAdminMetadataUpdatedLaunchesDescendantsJobAsTriggeringUser() {
    Harness h = new Harness();
    h.hasDescendants = true;
    AIP aip = h.aip("a1", null, basePermissions(), secretXml());
    // modellen stämplar updatedBy med den som ändrade metadatan innan den notifierar
    aip.setUpdatedBy("anna");

    h.observer.descriptiveMetadataUpdated(adminMetadata("a1"));

    Assert.assertEquals(h.model.permissionsUpdated, List.of("a1"));
    Assert.assertEquals(h.model.permissionsUpdatedBy, List.of("anna"));
    Assert.assertEquals(h.model.events, List.of("a1|anna"));
    Assert.assertEquals(h.descendantsChecked, List.of("a1"));
    Assert.assertEquals(h.jobsLaunched, List.of(List.of("a1")));
    Assert.assertEquals(h.jobUsers, List.of("anna"));
  }

  @Test
  public void testUnchangedPermissionsAreNotWrittenAndNoStateOnlyWrite() {
    Harness h = new Harness();
    Permissions permissions = basePermissions();
    permissions.setGroupPermissions(HANDL, perms());
    AIP aip = h.aip("a1", null, permissions, secretXml());

    h.observer.descriptiveMetadataUpdated(adminMetadata("a1"));

    // HANDL hade redan ingen READ: ingen behörighetsskrivning och – eftersom
    // observern inte stämplar tillstånd – inte heller någon ren tillståndsskrivning
    Assert.assertEquals(h.model.retrieves("a1"), 1);
    Assert.assertTrue(h.model.permissionsUpdated.isEmpty());
    Assert.assertTrue(h.model.aipUpdated.isEmpty());
    Assert.assertTrue(h.model.events.isEmpty(), "ingen händelse när inget ändras");
    Assert.assertNull(aip.getAccessRuleState());
  }

  @Test
  public void testEventIsNotCreatedWhenDisabled() {
    Harness h = new Harness();
    h.createEvents = false;
    h.aip("a1", null, basePermissions(), secretXml());

    h.observer.descriptiveMetadataUpdated(adminMetadata("a1"));

    Assert.assertEquals(h.model.permissionsUpdated, List.of("a1"));
    Assert.assertTrue(h.model.events.isEmpty());
  }

  @Test
  public void testOtherMetadataTypeIsIgnored() {
    Harness h = new Harness();
    h.aip("a1", null, basePermissions(), secretXml());

    ReturnWithExceptions<Void, ModelObserver> ret = h.observer
      .descriptiveMetadataCreated(new DescriptiveMetadata("ead_3.xml", "a1", null, "ead", "3"));
    h.observer.descriptiveMetadataUpdated(new DescriptiveMetadata("dc.xml", "a1", null, "dc", null));

    Assert.assertTrue(ret.isEmpty());
    Assert.assertEquals(h.model.retrieves("a1"), 0);
    Assert.assertTrue(h.model.permissionsUpdated.isEmpty());
    Assert.assertTrue(h.descendantsChecked.isEmpty());
    Assert.assertTrue(h.jobsLaunched.isEmpty());
  }

  @Test
  public void testRepresentationLevelAdminMetadataIsIgnored() {
    Harness h = new Harness();
    h.aip("a1", null, basePermissions(), secretXml());

    h.observer.descriptiveMetadataCreated(new DescriptiveMetadata("eterna_admin.xml", "a1", "rep1", "eterna",
      "admin"));
    h.observer.descriptiveMetadataDeleted("a1", "rep1", "eterna_admin.xml");

    Assert.assertEquals(h.model.retrieves("a1"), 0);
    Assert.assertTrue(h.model.permissionsUpdated.isEmpty());
    Assert.assertTrue(h.descendantsChecked.isEmpty());
  }

  @Test
  public void testAdminMetadataDeletedReevaluatesAndRestoresRead() {
    Harness h = new Harness();
    h.hasDescendants = true;
    h.rules = rules("open");
    // tidigare nekad av reglerna; metadatan är nu borta (ingen XML, ingen post)
    Permissions permissions = basePermissions();
    permissions.setGroupPermissions(HANDL, perms());
    AIP aip = h.aip("a1", null, permissions, null);
    aip.setUpdatedBy("anna");
    AccessRuleState state = new AccessRuleState();
    state.setRestrictedGroups(new HashSet<>(Set.of(HANDL)));
    state.setRulesVersion("3");
    aip.setAccessRuleState(state);

    ReturnWithExceptions<Void, ModelObserver> ret = h.observer.descriptiveMetadataDeleted("a1", null,
      "eterna_admin.xml");

    Assert.assertTrue(ret.isEmpty(), ret.toString());
    Assert.assertEquals(h.model.retrieves("a1"), 2);
    // missingMetadata=open ⇒ HANDL får READ tillbaka
    Assert.assertEquals(h.model.permissionsUpdated, List.of("a1"));
    Assert.assertTrue(readGroups(aip).contains(HANDL));
    Assert.assertEquals(h.jobsLaunched, List.of(List.of("a1")));
    Assert.assertEquals(h.jobUsers, List.of("anna"));

    // annat id på AIP-nivå rör ingenting
    h.observer.descriptiveMetadataDeleted("a1", null, "ead_3.xml");
    Assert.assertEquals(h.model.retrieves("a1"), 2);
  }

  // ---------------------------------------------------------------- behörighetsändring

  @Test
  public void testAipPermissionsUpdatedReappliesRulesWithoutLooping() {
    Harness h = new Harness();
    // någon har gett HANDL READ tillbaka trots sekretess
    AIP aip = h.aip("a1", null, basePermissions(), secretXml());
    // modellen notifierar aipPermissionsUpdated igen när appliern skriver
    h.model.permissionsHook = written -> h.observer.aipPermissionsUpdated(written);

    ReturnWithExceptions<Void, ModelObserver> ret = h.observer.aipPermissionsUpdated(aip);

    Assert.assertTrue(ret.isEmpty(), ret.toString());
    // 1 av observern + 1 färsk kopia av appliern; ett loopande anrop hade gett fler
    Assert.assertEquals(h.model.retrieves("a1"), 2, "det återinträdande anropet ska vara en no-op");
    Assert.assertEquals(h.model.permissionsUpdated, List.of("a1"));
    Assert.assertEquals(readGroups(aip), Set.of(SOC, ADMIN));
    // behörighetsändring startar inget efterkommande-jobb
    Assert.assertTrue(h.descendantsChecked.isEmpty());

    // en ny, extern notifiering när allt redan stämmer skriver inget
    h.observer.aipPermissionsUpdated(aip);
    Assert.assertEquals(h.model.retrieves("a1"), 3);
    Assert.assertEquals(h.model.permissionsUpdated, List.of("a1"));
    Assert.assertTrue(h.model.aipUpdated.isEmpty());
  }

  @Test
  public void testRunGuardedSuppressesReevaluation() throws Exception {
    Harness h = new Harness();
    AIP aip = h.aip("a1", null, basePermissions(), secretXml());

    // pluginets egen skrivning notifierar observern i samma tråd – inom vakten är det en no-op
    String result = AccessRulesModelObserver.runGuarded(() -> {
      h.observer.aipPermissionsUpdated(aip);
      h.observer.descriptiveMetadataUpdated(adminMetadata("a1"));
      return "klar";
    });

    Assert.assertEquals(result, "klar");
    Assert.assertEquals(h.model.retrieves("a1"), 0);
    Assert.assertTrue(h.model.permissionsUpdated.isEmpty());
    Assert.assertTrue(h.descendantsChecked.isEmpty());

    // vakten är släppt efteråt
    h.observer.aipPermissionsUpdated(aip);
    Assert.assertEquals(h.model.permissionsUpdated, List.of("a1"));
  }

  // ---------------------------------------------------------------- flytt

  @Test
  public void testAipMovedReevaluatesAndLaunchesDescendantsJob() {
    Harness h = new Harness();
    h.hasDescendants = true;
    AIP parent = h.aip("p", null, new Permissions(), secretXml());
    AIP aip = h.aip("a1", "p", basePermissions(), null);
    aip.setUpdatedBy("anna");

    h.observer.aipMoved(aip, "gammal", "p");

    // ärver förälderns sekretess ⇒ HANDL nekas (a1: observer + färsk kopia; p: arv)
    Assert.assertEquals(h.model.retrieves("a1"), 2);
    Assert.assertEquals(h.model.retrieves("p"), 1);
    Assert.assertEquals(h.model.permissionsUpdated, List.of("a1"));
    Assert.assertEquals(readGroups(aip), Set.of(SOC, ADMIN));
    Assert.assertEquals(aip.getAccessRuleState().getFactsSource(), parent.getId());
    Assert.assertEquals(h.descendantsChecked, List.of("a1"));
    Assert.assertEquals(h.jobsLaunched, List.of(List.of("a1")));
    Assert.assertEquals(h.jobUsers, List.of("anna"));

    // utan efterkommande som ärver: inget jobb
    Harness h2 = new Harness();
    h2.hasDescendants = false;
    AIP leaf = h2.aip("b1", null, basePermissions(), null);
    h2.observer.aipMoved(leaf, null, "x");
    Assert.assertEquals(h2.descendantsChecked, List.of("b1"));
    Assert.assertTrue(h2.jobsLaunched.isEmpty());
  }

  @Test
  public void testAipMovedWithOwnFactsDoesNotLaunchDescendantsJob() {
    Harness h = new Harness();
    h.hasDescendants = true;
    h.aip("p", null, new Permissions(), xml("ingen_sekretess", null, "inga"));
    // den flyttade noden har egen eterna_admin: efterkommande ärver från den, inte från nya föräldern
    AIP aip = h.aip("a1", "p", basePermissions(), secretXml());

    h.observer.aipMoved(aip, "gammal", "p");

    Assert.assertEquals(h.model.permissionsUpdated, List.of("a1"));
    Assert.assertEquals(aip.getAccessRuleState().getFactsSource(), AccessRulesApplier.FACTS_SOURCE_SELF);
    Assert.assertTrue(h.descendantsChecked.isEmpty(), "efterkommande ska inte ens kontrolleras");
    Assert.assertTrue(h.jobsLaunched.isEmpty());
  }

  // ---------------------------------------------------------------- uppsamling och transaktion

  @Test
  public void testDeferDescendantsJobsLaunchesOneJobPerBatch() {
    Harness h = new Harness();
    h.hasDescendants = true;
    h.aip("a1", null, basePermissions(), secretXml()).setUpdatedBy("anna");
    h.aip("a2", null, basePermissions(), secretXml()).setUpdatedBy("anna");
    h.aip("a3", null, basePermissions(), secretXml()).setUpdatedBy("anna");

    AccessRulesModelObserver.deferDescendantsJobs(() -> {
      h.observer.descriptiveMetadataUpdated(adminMetadata("a1"));
      h.observer.descriptiveMetadataUpdated(adminMetadata("a2"));
      // nästlad uppsamling samlas i den yttersta
      AccessRulesModelObserver.deferDescendantsJobs(() -> h.observer.descriptiveMetadataUpdated(adminMetadata("a3")));
      // dubblett i samma batch
      h.observer.descriptiveMetadataUpdated(adminMetadata("a1"));
      Assert.assertTrue(h.jobsLaunched.isEmpty(), "inget jobb förrän batchen är klar");
    });

    // varje AIP utvärderades direkt ...
    Assert.assertEquals(h.model.permissionsUpdated, List.of("a1", "a2", "a3"));
    Assert.assertEquals(h.descendantsChecked, List.of("a1", "a2", "a3", "a1"));
    // ... men ett enda efterkommande-jobb startades
    Assert.assertEquals(h.jobsLaunched, List.of(List.of("a1", "a2", "a3")));
    Assert.assertEquals(h.jobUsers, List.of("anna"));

    // utanför uppsamlingen startas jobb direkt igen
    h.observer.descriptiveMetadataUpdated(adminMetadata("a2"));
    Assert.assertEquals(h.jobsLaunched.size(), 2);
    Assert.assertEquals(h.jobsLaunched.get(1), List.of("a2"));
  }

  @Test
  public void testDeferDescendantsJobsFlushesEvenWhenBodyThrows() {
    Harness h = new Harness();
    h.hasDescendants = true;
    h.aip("a1", null, basePermissions(), secretXml());

    try {
      AccessRulesModelObserver.deferDescendantsJobs(() -> {
        h.observer.descriptiveMetadataUpdated(adminMetadata("a1"));
        throw new IllegalStateException("batchen sprack");
      });
      Assert.fail("undantaget ska propageras");
    } catch (IllegalStateException e) {
      Assert.assertEquals(e.getMessage(), "batchen sprack");
    }

    // metadataändringen består, så jobbet startas ändå
    Assert.assertEquals(h.jobsLaunched, List.of(List.of("a1")));
  }

  @Test
  public void testTransactionalModelDefersJobUntilAfterCommit() {
    Harness h = new Harness(true);
    h.hasDescendants = true;
    AIP aip = h.aip("a1", null, basePermissions(), secretXml());
    aip.setUpdatedBy("anna");

    h.observer.descriptiveMetadataUpdated(adminMetadata("a1"));

    // omvärderingen görs i transaktionen (staging) direkt ...
    Assert.assertEquals(h.model.permissionsUpdated, List.of("a1"));
    // ... men jobbet (som körs mot huvudlagringen) startas först efter commit
    Assert.assertTrue(h.jobsLaunched.isEmpty());
    Assert.assertEquals(h.model.afterCommit.size(), 1);

    h.model.afterCommit.get(0).run();
    Assert.assertEquals(h.jobsLaunched, List.of(List.of("a1")));
    Assert.assertEquals(h.jobUsers, List.of("anna"));
  }

  // ---------------------------------------------------------------- avstängd

  @Test
  public void testDisabledObserverIsNoOp() {
    Harness h = new Harness();
    h.enabled = false;
    h.hasDescendants = true;
    AIP aip = h.aip("a1", null, basePermissions(), secretXml());

    h.observer.descriptiveMetadataCreated(adminMetadata("a1"));
    h.observer.descriptiveMetadataUpdated(adminMetadata("a1"));
    h.observer.descriptiveMetadataDeleted("a1", null, "eterna_admin.xml");
    h.observer.aipPermissionsUpdated(aip);
    h.observer.aipMoved(aip, null, "p");

    Assert.assertEquals(h.model.retrieves("a1"), 0);
    Assert.assertTrue(h.model.permissionsUpdated.isEmpty());
    Assert.assertTrue(h.descendantsChecked.isEmpty());
    Assert.assertTrue(h.jobsLaunched.isEmpty());
    Assert.assertEquals(readGroups(aip), Set.of(SOC, HANDL, ADMIN));
  }

  @Test
  public void testUnrelatedEventsAreNoOp() {
    Harness h = new Harness();
    AIP aip = h.aip("a1", null, basePermissions(), secretXml());

    Assert.assertTrue(h.observer.aipCreated(aip).isEmpty());
    Assert.assertTrue(h.observer.aipUpdated(aip).isEmpty());
    Assert.assertTrue(h.observer.aipStateUpdated(aip).isEmpty());
    Assert.assertTrue(h.observer.aipDeleted("a1", true).isEmpty());

    Assert.assertEquals(h.model.retrieves("a1"), 0);
    Assert.assertTrue(h.model.permissionsUpdated.isEmpty());
  }

  // ---------------------------------------------------------------- fel

  @Test
  public void testModelErrorIsReportedWithExplanationButNeverThrown() {
    Harness h = new Harness();
    h.aip("a1", null, basePermissions(), secretXml());
    h.model.retrieveFailure = new GenericException("lagringen svarar inte");

    ReturnWithExceptions<Void, ModelObserver> ret = h.observer.descriptiveMetadataUpdated(adminMetadata("a1"));

    Assert.assertEquals(ret.getExceptions().size(), 1);
    Exception reported = ret.getExceptions().get(0);
    Assert.assertTrue(reported instanceof GenericException, reported.toString());
    // fail closed: modellen kastar vidare, så texten ska förklara vad som avvisades och varför
    Assert.assertTrue(reported.getMessage().contains("Åtkomstreglerna kunde inte tillämpas"), reported.getMessage());
    Assert.assertTrue(reported.getMessage().contains("a1"), reported.getMessage());
    Assert.assertTrue(reported.getMessage().contains("lagringen svarar inte"), reported.getMessage());
    Assert.assertTrue(h.model.permissionsUpdated.isEmpty());

    // vakten är släppt efter felet: nästa händelse utvärderas igen
    h.model.retrieveFailure = null;
    ReturnWithExceptions<Void, ModelObserver> again = h.observer.descriptiveMetadataUpdated(adminMetadata("a1"));
    Assert.assertTrue(again.isEmpty(), again.toString());
    Assert.assertEquals(h.model.permissionsUpdated, List.of("a1"));
  }

  @Test
  public void testMissingAipIsIgnored() {
    Harness h = new Harness();

    ReturnWithExceptions<Void, ModelObserver> ret = h.observer.descriptiveMetadataCreated(adminMetadata("borta"));

    Assert.assertTrue(ret.isEmpty(), ret.toString());
    Assert.assertEquals(h.model.retrieves("borta"), 1);
    Assert.assertTrue(h.model.permissionsUpdated.isEmpty());
  }

  @Test
  public void testJobLaunchFailureIsLoggedNotThrown() {
    ModelStub model = new ModelStub();
    ModelService proxy = model.proxy(false);
    AccessRulesModelObserver observer = new AccessRulesModelObserver(proxy, () -> true,
      () -> rules("inherit"), rules -> new AccessRulesApplier(proxy, rules, "OSL", "eterna_admin"),
      "eterna_admin", aipId -> true, (aipIds, user) -> {
        throw new IllegalStateException("orkestratorn är nere");
      }, () -> true);
    AIP aip = new AIP();
    aip.setId("a1");
    aip.setPermissions(basePermissions());
    aip.setDescriptiveMetadata(new ArrayList<>(List.of(adminMetadata("a1"))));
    model.aips.put("a1", aip);
    model.xml.put("a1", secretXml());

    ReturnWithExceptions<Void, ModelObserver> ret = observer.descriptiveMetadataCreated(adminMetadata("a1"));

    // omvärderingen lyckades; jobbfelet påverkar inte resultatet
    Assert.assertTrue(ret.isEmpty(), ret.toString());
    Assert.assertEquals(model.permissionsUpdated, List.of("a1"));
  }

  // ---------------------------------------------------------------- gruppkontroll

  @Test
  public void testUnknownGroupCheckIsDoneOncePerRuleSetNotPerEvent() {
    AccessRulesApplier.resetGroupCheckForTests();
    Harness h = new Harness();
    h.aip("a1", null, basePermissions(), secretXml());
    h.aip("a2", null, basePermissions(), secretXml());

    // två händelser ⇒ två applikatorinstanser, men samma hanterade grupper
    h.observer.descriptiveMetadataUpdated(adminMetadata("a1"));
    h.observer.descriptiveMetadataUpdated(adminMetadata("a2"));
    Assert.assertEquals(h.model.permissionsUpdated, List.of("a1", "a2"));
    Assert.assertEquals(h.model.listGroupsCalls, 1, "katalogen ska bara frågas en gång per regeluppsättning");

    // andra hanterade grupper ⇒ ny kontroll
    AccessRuleSet other = rules("inherit");
    other.getRules().add(rule("ekonomi", List.of("ingen_sekretess"), List.of(), List.of("inga")));
    h.rules = other;
    h.observer.descriptiveMetadataUpdated(adminMetadata("a1"));
    Assert.assertEquals(h.model.listGroupsCalls, 2);
  }
}

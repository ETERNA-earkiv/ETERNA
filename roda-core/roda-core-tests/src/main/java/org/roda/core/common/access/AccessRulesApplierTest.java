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

import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.exceptions.NotFoundException;
import org.roda.core.data.v2.accessrules.AccessRule;
import org.roda.core.data.v2.accessrules.AccessRuleDefaults;
import org.roda.core.data.v2.accessrules.AccessRuleSet;
import org.roda.core.data.v2.accessrules.AccessRuleState;
import org.roda.core.data.v2.ip.AIP;
import org.roda.core.data.v2.ip.Permissions;
import org.roda.core.data.v2.ip.Permissions.PermissionType;
import org.roda.core.data.v2.ip.metadata.DescriptiveMetadata;
import org.roda.core.model.ModelService;
import org.roda.core.storage.Binary;
import org.roda.core.storage.ContentPayload;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tester för applikatorn mot en stubbad
 * {@link ModelService} (java.lang.reflect.Proxy, ingen RodaCoreFactory):
 * metadataposten hittas på RODA:s uppdelade typ-id (type="eterna",
 * version="admin"), egna/ärvda/saknade fakta med cache, cirkulär kedja,
 * saknad förfader, avstängda regler (återställning), torrkörning, ren
 * tillståndsskrivning, otolkbar metadata (stängt), färsk kopia vid skrivning
 * samt strip av skrivbehörigheter.
 */
@Test(groups = {RodaConstants.TEST_GROUP_ALL, RodaConstants.TEST_GROUP_DEV, RodaConstants.TEST_GROUP_TRAVIS,
  "accessrules"})
public class AccessRulesApplierTest {

  private static final String SOC = "socialtjanst";
  private static final String HANDL = "handlaggare";
  private static final String ADMIN = "administrators";
  private static final String USER = "anna";
  private static final String NS = EternaAdminParser.NAMESPACE;

  // ---------------------------------------------------------------- modellstubb

  /** Minimal modell: AIP:er per id, eterna_admin-XML per AIP-id, anropslogg. */
  private static final class ModelStub implements InvocationHandler {
    final Map<String, AIP> aips = new HashMap<>();
    /** AIP som returneras av retrieveAIP i stället för den i aips (färsk kopia). */
    final Map<String, AIP> fresh = new HashMap<>();
    final Map<String, String> xml = new HashMap<>();
    final Map<String, Integer> retrieveCount = new HashMap<>();
    final List<String> permissionsUpdated = new ArrayList<>();
    final List<String> aipUpdated = new ArrayList<>();
    int binaryReads;

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      switch (method.getName()) {
        case "retrieveAIP": {
          String id = (String) args[0];
          retrieveCount.merge(id, 1, Integer::sum);
          AIP aip = fresh.containsKey(id) ? fresh.get(id) : aips.get(id);
          if (aip == null) {
            throw new NotFoundException("AIP " + id + " saknas");
          }
          return aip;
        }
        case "retrieveDescriptiveMetadataBinary": {
          String aipId = (String) args[0];
          binaryReads++;
          String content = xml.get(aipId);
          if (content == null) {
            throw new NotFoundException("metadata saknas för " + aipId);
          }
          return binary(content);
        }
        case "updateAIPPermissions":
          permissionsUpdated.add(((AIP) args[0]).getId());
          return null;
        case "updateAIP":
          aipUpdated.add(((AIP) args[0]).getId());
          return args[0];
        case "listGroups":
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

    ModelService proxy() {
      return (ModelService) Proxy.newProxyInstance(ModelService.class.getClassLoader(),
        new Class<?>[] {ModelService.class}, this);
    }

    int writes() {
      return permissionsUpdated.size() + aipUpdated.size();
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

  // ---------------------------------------------------------------- hjälpare

  private static DescriptiveMetadata adminMetadata(String aipId) {
    // så som RODA lagrar det konfigurerade typ-id:t "eterna_admin"
    return new DescriptiveMetadata("eterna_admin.xml", aipId, null, "eterna", "admin");
  }

  private static AIP aip(String id, String parentId, Permissions permissions, DescriptiveMetadata... metadata) {
    AIP aip = new AIP();
    aip.setId(id);
    aip.setParentId(parentId);
    aip.setPermissions(permissions);
    aip.setDescriptiveMetadata(new ArrayList<>(Arrays.asList(metadata)));
    return aip;
  }

  private static Set<PermissionType> perms(PermissionType... types) {
    return new HashSet<>(Arrays.asList(types));
  }

  /** SOC: READ+UPDATE, HANDL: READ, ADMIN: allt, användaren anna: READ. */
  private static Permissions basePermissions() {
    Permissions p = new Permissions();
    p.setGroupPermissions(SOC, perms(PermissionType.READ, PermissionType.UPDATE));
    p.setGroupPermissions(HANDL, perms(PermissionType.READ));
    p.setGroupPermissions(ADMIN, perms(PermissionType.CREATE, PermissionType.READ, PermissionType.UPDATE,
      PermissionType.DELETE, PermissionType.GRANT));
    p.setUserPermissions(USER, perms(PermissionType.READ));
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

  private static AccessRule rule(String group, List<String> sekretess, List<String> lagrum,
    List<String> personuppgifter) {
    AccessRule rule = new AccessRule(group);
    rule.setSekretess(sekretess);
    rule.setLagrum(lagrum);
    rule.setPersonuppgifter(personuppgifter);
    rule.setPersonuppgiftsniva(List.of("*"));
    return rule;
  }

  /** SOC får OSL 21 vid sekretess; HANDL bara ingen_sekretess. */
  private static AccessRuleSet rules() {
    AccessRuleSet set = new AccessRuleSet();
    set.setVersion(3);
    set.setEnabled(true);
    set.setDefaults(new AccessRuleDefaults());
    set.getRules().add(rule(SOC, List.of("ingen_sekretess", "sekretess_forekommer"), List.of("OSL 21"),
      List.of("inga", "forekommer")));
    set.getRules().add(rule(HANDL, List.of("ingen_sekretess"), List.of(), List.of("inga")));
    return set;
  }

  private static AccessRulesApplier applier(ModelStub model, AccessRuleSet rules) {
    return new AccessRulesApplier(model.proxy(), rules, "OSL", "eterna_admin");
  }

  private static Set<String> readGroups(AIP aip) {
    return aip.getPermissions().getGroups().get(PermissionType.READ);
  }

  // ---------------------------------------------------------------- findAdminMetadata

  @Test
  public void testFindAdminMetadataMatchesSplitAndPlainTypeId() {
    DescriptiveMetadata split = new DescriptiveMetadata("eterna_admin.xml", "a", null, "eterna", "admin");
    DescriptiveMetadata plain = new DescriptiveMetadata("eterna_admin.xml", "a", null, "eterna_admin", null);
    DescriptiveMetadata emptyVersion = new DescriptiveMetadata("eterna_admin.xml", "a", null, "eterna_admin", "");
    DescriptiveMetadata mixedCase = new DescriptiveMetadata("eterna_admin.xml", "a", null, "Eterna", "Admin");
    DescriptiveMetadata ead = new DescriptiveMetadata("ead_3.xml", "a", null, "ead", "3");
    DescriptiveMetadata onRepresentation = new DescriptiveMetadata("eterna_admin.xml", "a", "rep1", "eterna",
      "admin");
    DescriptiveMetadata otherSplit = new DescriptiveMetadata("x.xml", "a", null, "eterna_admin", "2");

    Assert.assertSame(AccessRulesApplier.findAdminMetadata(aip("a", null, null, ead, split), "eterna_admin"), split);
    Assert.assertSame(AccessRulesApplier.findAdminMetadata(aip("a", null, null, plain), "eterna_admin"), plain);
    Assert.assertSame(AccessRulesApplier.findAdminMetadata(aip("a", null, null, emptyVersion), "eterna_admin"),
      emptyVersion);
    Assert.assertSame(AccessRulesApplier.findAdminMetadata(aip("a", null, null, mixedCase), "eterna_admin"),
      mixedCase);
    Assert.assertNull(AccessRulesApplier.findAdminMetadata(aip("a", null, null, ead), "eterna_admin"));
    Assert.assertNull(AccessRulesApplier.findAdminMetadata(aip("a", null, null, onRepresentation), "eterna_admin"));
    Assert.assertNull(AccessRulesApplier.findAdminMetadata(aip("a", null, null, otherSplit), "eterna_admin"));
    Assert.assertNull(AccessRulesApplier.findAdminMetadata(aip("a", null, null), "eterna_admin"));
    Assert.assertNull(AccessRulesApplier.findAdminMetadata(null, "eterna_admin"));
  }

  // ---------------------------------------------------------------- egna fakta

  @Test
  public void testOwnFactsDenyHandlaggareAndKeepSocialtjanst() throws Exception {
    ModelStub model = new ModelStub();
    AIP aip = aip("a1", null, basePermissions(), adminMetadata("a1"));
    model.aips.put("a1", aip);
    model.xml.put("a1", xml("sekretess_forekommer", "OSL 21 kap. 7 §", "forekommer"));

    ApplyResult result = applier(model, rules()).applyToAip(aip, USER, false);

    Assert.assertFalse(result.isSkipped());
    Assert.assertTrue(result.getFacts().isPresent());
    Assert.assertFalse(result.getFacts().isInherited());
    Assert.assertEquals(result.getFacts().getSourceAipId(), "a1");
    Assert.assertEquals(result.getFacts().getLagrum(), List.of(new Lagrum("OSL", 21, 7)));
    Assert.assertEquals(result.getDecision().getAllowedGroups(), Set.of(SOC));
    Assert.assertTrue(result.isWritten());
    Assert.assertFalse(result.isStateOnly());
    Assert.assertEquals(model.permissionsUpdated, List.of("a1"));
    Assert.assertTrue(model.aipUpdated.isEmpty());

    Assert.assertEquals(readGroups(aip), Set.of(SOC, ADMIN));
    Assert.assertEquals(aip.getPermissions().getGroupPermissions(SOC), perms(PermissionType.READ, PermissionType.UPDATE));
    Assert.assertTrue(aip.getPermissions().getUserPermissions(USER).contains(PermissionType.READ));
    AccessRuleState state = aip.getAccessRuleState();
    Assert.assertNotNull(state);
    Assert.assertEquals(state.getRestrictedGroups(), Set.of(HANDL));
    Assert.assertEquals(state.getRulesVersion(), "3");
    Assert.assertEquals(state.getFactsSource(), AccessRulesApplier.FACTS_SOURCE_SELF);
    Assert.assertNotNull(state.getAppliedOn());
  }

  @Test
  public void testStripWriteOnDenyIsDefaultAndCanBeTurnedOff() throws Exception {
    ModelStub model = new ModelStub();
    Permissions permissions = basePermissions();
    permissions.setGroupPermissions(HANDL, perms(PermissionType.READ, PermissionType.UPDATE, PermissionType.GRANT));
    AIP aip = aip("a1", null, permissions, adminMetadata("a1"));
    model.aips.put("a1", aip);
    model.xml.put("a1", xml("sekretess_forekommer", "OSL 21:7", "inga"));

    AccessRulesApplier applier = applier(model, rules());
    Assert.assertTrue(applier.isStripWriteOnDeny());
    ApplyResult stripped = applier.applyToAip(aip, USER, false);

    Assert.assertEquals(stripped.getMerge().getWriteStripped(), Set.of(HANDL));
    Assert.assertTrue(aip.getPermissions().getGroupPermissions(HANDL).isEmpty());
    Assert.assertEquals(aip.getAccessRuleState().getRemovedPermissions().get(HANDL), Set.of("UPDATE", "GRANT"));
    Assert.assertTrue(stripped.getMerge().getWarnings().isEmpty());

    // utan strip: bara READ rörs, varning ges
    ModelStub model2 = new ModelStub();
    AIP aip2 = aip("a2", null, PermissionsMerger.deepCopy(permissions), adminMetadata("a2"));
    model2.aips.put("a2", aip2);
    model2.xml.put("a2", xml("sekretess_forekommer", "OSL 21:7", "inga"));
    AccessRulesApplier noStrip = applier(model2, rules());
    noStrip.setStripWriteOnDeny(false);
    ApplyResult kept = noStrip.applyToAip(aip2, USER, false);

    Assert.assertTrue(kept.getMerge().getWriteStripped().isEmpty());
    Assert.assertEquals(aip2.getPermissions().getGroupPermissions(HANDL),
      perms(PermissionType.UPDATE, PermissionType.GRANT));
    Assert.assertEquals(kept.getMerge().getWarnings().size(), 1, kept.getMerge().getWarnings().toString());
  }

  // ---------------------------------------------------------------- arv

  @Test
  public void testInheritTwoLevelsWithCacheAcrossSiblings() throws Exception {
    ModelStub model = new ModelStub();
    AIP grand = aip("g", null, new Permissions(), adminMetadata("g"));
    AIP parent = aip("p", "g", new Permissions());
    AIP child = aip("c1", "p", basePermissions());
    AIP sibling = aip("c2", "p", basePermissions());
    model.aips.put("g", grand);
    model.aips.put("p", parent);
    model.aips.put("c1", child);
    model.aips.put("c2", sibling);
    model.xml.put("g", xml("helt_sekretessbelagd", "OSL 26 kap. 1 §", "kansliga_forekommer"));

    AccessRulesApplier applier = applier(model, rules());
    ApplyResult first = applier.applyToAip(child, USER, false);

    Assert.assertTrue(first.getFacts().isPresent());
    Assert.assertTrue(first.getFacts().isInherited());
    Assert.assertEquals(first.getFacts().getSourceAipId(), "g");
    Assert.assertEquals(first.getFacts().getSekretess(), "helt_sekretessbelagd");
    Assert.assertEquals(child.getAccessRuleState().getFactsSource(), "g");
    // ingen grupp tillåts (SOC saknar OSL 26, HANDL saknar sekretess) ⇒ READ bort
    Assert.assertEquals(readGroups(child), Set.of(ADMIN));
    Assert.assertEquals(child.getAccessRuleState().getRestrictedGroups(), Set.of(SOC, HANDL));
    Assert.assertEquals(model.retrieveCount.get("p"), Integer.valueOf(1));
    Assert.assertEquals(model.retrieveCount.get("g"), Integer.valueOf(1));
    Assert.assertEquals(model.binaryReads, 1);

    ApplyResult second = applier.applyToAip(sibling, USER, false);

    Assert.assertEquals(second.getFacts().getSourceAipId(), "g");
    Assert.assertTrue(second.getFacts().isInherited());
    // förfäderna hämtas inte igen tack vare cachen
    Assert.assertEquals(model.retrieveCount.get("p"), Integer.valueOf(1));
    Assert.assertEquals(model.retrieveCount.get("g"), Integer.valueOf(1));
    Assert.assertEquals(model.binaryReads, 1);
    Assert.assertEquals(readGroups(sibling), Set.of(ADMIN));
  }

  @Test
  public void testMissingMetadataClosedDoesNotInherit() throws Exception {
    ModelStub model = new ModelStub();
    AIP parent = aip("p", null, new Permissions(), adminMetadata("p"));
    AIP child = aip("c", "p", basePermissions());
    model.aips.put("p", parent);
    model.aips.put("c", child);
    model.xml.put("p", xml("ingen_sekretess", null, "inga"));
    AccessRuleSet rules = rules();
    rules.getDefaults().setMissingMetadata("closed");

    ApplyResult result = applier(model, rules).applyToAip(child, USER, false);

    Assert.assertFalse(result.getFacts().isPresent());
    Assert.assertNull(model.retrieveCount.get("p"), "förälder ska inte hämtas vid missingMetadata=closed");
    Assert.assertTrue(result.getDecision().getAllowedGroups().isEmpty());
    Assert.assertEquals(readGroups(child), Set.of(ADMIN));
    Assert.assertEquals(child.getAccessRuleState().getFactsSource(), AccessRulesApplier.FACTS_SOURCE_NONE);
  }

  @Test
  public void testInheritWithoutAnyAncestorMetadataIsClosedUnlessEjBedomdAllowed() throws Exception {
    ModelStub model = new ModelStub();
    AIP parent = aip("p", null, new Permissions());
    AIP child = aip("c", "p", basePermissions());
    model.aips.put("p", parent);
    model.aips.put("c", child);

    // standardpolicyn inherit: ingen bedömd förfader ⇒ obedömd struktur lämnas öppen, inget skrivs
    ApplyResult open = applier(model, rules()).applyToAip(child, USER, false);
    Assert.assertFalse(open.getFacts().isPresent());
    Assert.assertEquals(open.getDecision().getAllowedGroups(), open.getDecision().getManagedGroups());
    Assert.assertFalse(open.getMerge().isChanged());

    // inherit-closed: samma objekt behandlas som ej bedömt ⇒ stängt
    AccessRuleSet strict = rules();
    strict.getDefaults().setMissingMetadata("inherit-closed");
    ApplyResult closed = applier(model, strict).applyToAip(child, USER, false);

    Assert.assertFalse(closed.getFacts().isPresent());
    Assert.assertTrue(closed.getDecision().getAllowedGroups().isEmpty(), closed.getDecision().toString());
    Assert.assertEquals(readGroups(child), Set.of(ADMIN));
    Assert.assertEquals(child.getAccessRuleState().getRestrictedGroups(), Set.of(SOC, HANDL));

    // regel som listar ej_bedomd ⇒ READ tillbaka
    AccessRuleSet lenient = rules();
    lenient.getDefaults().setMissingMetadata("inherit-closed");
    lenient.setVersion(4);
    lenient.findRule(SOC).setSekretess(List.of("ej_bedomd", "ingen_sekretess"));
    lenient.findRule(SOC).setPersonuppgifter(List.of("ej_bedomd", "inga"));
    ModelStub model2 = new ModelStub();
    model2.aips.put("p", parent);
    model2.aips.put("c", child);

    ApplyResult reopened = applier(model2, lenient).applyToAip(child, USER, false);

    Assert.assertEquals(reopened.getDecision().getAllowedGroups(), Set.of(SOC));
    Assert.assertEquals(reopened.getMerge().getAdded(), Set.of(SOC));
    Assert.assertEquals(readGroups(child), Set.of(SOC, ADMIN));
    Assert.assertEquals(child.getAccessRuleState().getRestrictedGroups(), Set.of(HANDL));
    Assert.assertEquals(child.getAccessRuleState().getRulesVersion(), "4");
  }

  @Test
  public void testCircularParentChainTerminates() throws Exception {
    ModelStub model = new ModelStub();
    AIP a = aip("a", "b", basePermissions());
    AIP b = aip("b", "a", new Permissions());
    model.aips.put("a", a);
    model.aips.put("b", b);

    ApplyResult result = applier(model, rules()).applyToAip(a, USER, true);

    Assert.assertFalse(result.getFacts().isPresent());
    Assert.assertEquals(model.retrieveCount.get("b"), Integer.valueOf(1));
    Assert.assertNull(model.retrieveCount.get("a"), "torrkörning hämtar ingen färsk kopia");
  }

  @Test
  public void testMissingAncestorIsTreatedAsAbsentWithoutException() throws Exception {
    ModelStub model = new ModelStub();
    AIP child = aip("c", "ghost", basePermissions());
    model.aips.put("c", child);

    ApplyResult result = applier(model, rules()).applyToAip(child, USER, true);

    // saknad förfader ⇒ frånvarande fakta; med standardpolicyn inherit lämnas obedömd struktur öppen
    Assert.assertFalse(result.getFacts().isPresent());
    Assert.assertEquals(result.getDecision().getAllowedGroups(), result.getDecision().getManagedGroups());

    // med inherit-closed behandlas samma objekt som ej bedömt ⇒ stängt
    AccessRuleSet strict = rules();
    strict.getDefaults().setMissingMetadata("inherit-closed");
    ApplyResult closed = applier(model, strict).applyToAip(child, USER, true);
    Assert.assertFalse(closed.getFacts().isPresent());
    Assert.assertTrue(closed.getDecision().getAllowedGroups().isEmpty());
  }

  // ---------------------------------------------------------------- avstängt / hoppas över

  @Test
  public void testRulesDisabledRestoresPermissionsAndClearsState() throws Exception {
    ModelStub model = new ModelStub();
    Permissions permissions = new Permissions();
    permissions.setGroupPermissions(ADMIN, perms(PermissionType.READ));
    permissions.setGroupPermissions(SOC, perms(PermissionType.CREATE));
    AIP aip = aip("a1", null, permissions, adminMetadata("a1"));
    AccessRuleState previous = new AccessRuleState();
    previous.setRestrictedGroups(Set.of(SOC, HANDL));
    previous.getRemovedPermissions().put(SOC, Set.of("UPDATE"));
    previous.setRulesVersion("2");
    aip.setAccessRuleState(previous);
    model.aips.put("a1", aip);
    model.xml.put("a1", xml("helt_sekretessbelagd", "OSL 21:7", "kansliga_forekommer"));
    AccessRuleSet rules = rules();
    rules.setEnabled(false);

    ApplyResult result = applier(model, rules).applyToAip(aip, USER, false);

    Assert.assertFalse(result.isSkipped());
    Assert.assertTrue(result.isWritten());
    Assert.assertEquals(result.getMerge().getAdded(), Set.of(SOC, HANDL));
    Assert.assertEquals(result.getMerge().getWriteRestored(), Set.of(SOC));
    Assert.assertEquals(model.binaryReads, 0, "faktan läses inte när reglerna är avstängda");
    Assert.assertEquals(aip.getPermissions().getGroupPermissions(SOC),
      perms(PermissionType.CREATE, PermissionType.READ, PermissionType.UPDATE));
    Assert.assertEquals(aip.getPermissions().getGroupPermissions(HANDL), perms(PermissionType.READ));
    Assert.assertNull(aip.getAccessRuleState(), "tillståndet tas bort när inget återstår");

    // andra körningen: inget att göra, ingen stämpling
    ModelStub model2 = new ModelStub();
    model2.aips.put("a1", aip);
    ApplyResult again = applier(model2, rules).applyToAip(aip, USER, false);
    Assert.assertFalse(again.isWriteNeeded());
    Assert.assertFalse(again.isWritten());
    Assert.assertEquals(model2.writes(), 0);
    Assert.assertNull(aip.getAccessRuleState());
  }

  @Test
  public void testNoRulesOrNullRulesIsSkipped() throws Exception {
    ModelStub model = new ModelStub();
    AIP aip = aip("a1", null, basePermissions(), adminMetadata("a1"));
    model.aips.put("a1", aip);
    AccessRuleSet empty = new AccessRuleSet();
    empty.setEnabled(true);

    ApplyResult skipped = applier(model, empty).applyToAip(aip, USER, false);
    Assert.assertTrue(skipped.isSkipped());
    Assert.assertNotNull(skipped.getSkipReason());
    Assert.assertEquals(model.writes(), 0);
    Assert.assertEquals(model.binaryReads, 0);

    ApplyResult nullRules = applier(model, null).applyToAip(aip, USER, false);
    Assert.assertTrue(nullRules.isSkipped());
    Assert.assertEquals(model.writes(), 0);
    Assert.assertEquals(readGroups(aip), Set.of(SOC, HANDL, ADMIN));
  }

  // ---------------------------------------------------------------- skrivbeslut

  @Test
  public void testDryRunComputesButDoesNotWrite() throws Exception {
    ModelStub model = new ModelStub();
    AIP aip = aip("a1", null, basePermissions(), adminMetadata("a1"));
    model.aips.put("a1", aip);
    model.xml.put("a1", xml("sekretess_forekommer", "OSL 21:7", "inga"));

    ApplyResult result = applier(model, rules()).applyToAip(aip, USER, true);

    Assert.assertTrue(result.isWriteNeeded());
    Assert.assertFalse(result.isWritten());
    Assert.assertTrue(result.isPermissionsChanged());
    Assert.assertEquals(result.getMerge().getRemoved(), Set.of(HANDL));
    Assert.assertEquals(model.writes(), 0);
    Assert.assertNull(model.retrieveCount.get("a1"));
    // AIP:n är orörd
    Assert.assertEquals(readGroups(aip), Set.of(SOC, HANDL, ADMIN));
    Assert.assertNull(aip.getAccessRuleState());
    Assert.assertTrue(result.summary().contains("skulle skrivas"), result.summary());
  }

  @Test
  public void testStateOnlyWriteUsesUpdateAipAndCanBeDisabled() throws Exception {
    ModelStub model = new ModelStub();
    Permissions permissions = new Permissions();
    permissions.setGroupPermissions(SOC, perms(PermissionType.READ));
    AIP aip = aip("a1", null, permissions, adminMetadata("a1"));
    model.aips.put("a1", aip);
    model.xml.put("a1", xml("ingen_sekretess", null, "inga"));

    AccessRulesApplier applier = applier(model, rules());
    ApplyResult first = applier.applyToAip(aip, USER, false);

    Assert.assertFalse(first.isPermissionsChanged());
    Assert.assertTrue(first.isStateOnly());
    Assert.assertTrue(first.isWritten());
    Assert.assertEquals(model.aipUpdated, List.of("a1"));
    Assert.assertTrue(model.permissionsUpdated.isEmpty());
    Assert.assertEquals(aip.getAccessRuleState().getRulesVersion(), "3");
    Assert.assertEquals(aip.getAccessRuleState().getFactsSource(), AccessRulesApplier.FACTS_SOURCE_SELF);
    Assert.assertTrue(first.summary().contains("regeltillstånd skrivet"), first.summary());

    // samma version och källa ⇒ inget att skriva
    ApplyResult second = applier.applyToAip(aip, USER, false);
    Assert.assertFalse(second.isWriteNeeded());
    Assert.assertFalse(second.isWritten());
    Assert.assertEquals(model.writes(), 1);

    // ändrad faktakälla ⇒ tillståndet uppdateras
    aip.getAccessRuleState().setFactsSource("gammal-forfader");
    ApplyResult third = applier.applyToAip(aip, USER, false);
    Assert.assertTrue(third.isStateOnly());
    Assert.assertEquals(aip.getAccessRuleState().getFactsSource(), AccessRulesApplier.FACTS_SOURCE_SELF);

    // observer-läge: inga rena tillståndsskrivningar
    AIP untouched = aip("a2", null, PermissionsMerger.deepCopy(permissions), adminMetadata("a2"));
    model.aips.put("a2", untouched);
    model.xml.put("a2", xml("ingen_sekretess", null, "inga"));
    AccessRulesApplier observer = applier(model, rules());
    observer.setStateOnlyWrites(false);
    ApplyResult fourth = observer.applyToAip(untouched, USER, false);
    Assert.assertFalse(fourth.isWriteNeeded());
    Assert.assertNull(untouched.getAccessRuleState());
  }

  @Test
  public void testUnparsableMetadataIsClosedNotOpen() throws Exception {
    ModelStub model = new ModelStub();
    AIP aip = aip("a1", null, basePermissions(), adminMetadata("a1"));
    model.aips.put("a1", aip);
    model.xml.put("a1", "<eternaAdmin xmlns=\"" + NS + "\"><sekretess>ingen_sekretess");

    ApplyResult result = applier(model, rules()).applyToAip(aip, USER, false);

    Assert.assertTrue(result.getFacts().isPresent());
    Assert.assertNull(result.getFacts().getSekretess());
    Assert.assertFalse(result.getFacts().getWarnings().isEmpty(), "parsefelet ska synas i faktan");
    Assert.assertTrue(result.getDecision().getAllowedGroups().isEmpty(), result.getDecision().toString());
    Assert.assertEquals(readGroups(aip), Set.of(ADMIN));
    Assert.assertEquals(aip.getAccessRuleState().getRestrictedGroups(), Set.of(SOC, HANDL));
  }

  @Test
  public void testWriteUsesFreshCopyAndMirrorsResultToCaller() throws Exception {
    ModelStub model = new ModelStub();
    AIP stale = aip("a1", null, basePermissions(), adminMetadata("a1"));
    // under tiden har administratören tagit bort READ för ADMIN och gett HANDL DELETE
    Permissions changed = basePermissions();
    changed.setGroupPermissions(ADMIN, perms(PermissionType.CREATE, PermissionType.UPDATE, PermissionType.GRANT));
    changed.setGroupPermissions(HANDL, perms(PermissionType.READ, PermissionType.DELETE));
    AIP fresh = aip("a1", null, changed, adminMetadata("a1"));
    model.aips.put("a1", stale);
    model.fresh.put("a1", fresh);
    model.xml.put("a1", xml("sekretess_forekommer", "OSL 21:7", "inga"));

    ApplyResult result = applier(model, rules()).applyToAip(stale, USER, false);

    Assert.assertTrue(result.isWritten());
    Assert.assertEquals(model.permissionsUpdated, List.of("a1"));
    // den färska kopian skrevs: ADMIN:s borttagna READ rullas inte tillbaka,
    // HANDL förlorar READ och (strip) DELETE
    Assert.assertEquals(readGroups(fresh), Set.of(SOC));
    Assert.assertTrue(fresh.getPermissions().getGroupPermissions(HANDL).isEmpty());
    Assert.assertEquals(fresh.getAccessRuleState().getRemovedPermissions().get(HANDL), Set.of("DELETE"));
    // och speglas på anroparens objekt
    Assert.assertEquals(stale.getPermissions(), fresh.getPermissions());
    Assert.assertEquals(stale.getAccessRuleState(), fresh.getAccessRuleState());
    Assert.assertEquals(result.getMerge().getRemoved(), Set.of(HANDL));
  }

  @Test
  public void testNothingToWriteAfterFreshCopyAlreadyUpToDate() throws Exception {
    ModelStub model = new ModelStub();
    AIP stale = aip("a1", null, basePermissions(), adminMetadata("a1"));
    // någon annan har redan tillämpat samma regelversion på den färska kopian
    Permissions done = basePermissions();
    done.setGroupPermissions(HANDL, perms());
    AIP fresh = aip("a1", null, done, adminMetadata("a1"));
    AccessRuleState state = new AccessRuleState();
    state.setRestrictedGroups(Set.of(HANDL));
    state.setRulesVersion("3");
    state.setFactsSource(AccessRulesApplier.FACTS_SOURCE_SELF);
    fresh.setAccessRuleState(state);
    model.aips.put("a1", stale);
    model.fresh.put("a1", fresh);
    model.xml.put("a1", xml("sekretess_forekommer", "OSL 21:7", "inga"));

    ApplyResult result = applier(model, rules()).applyToAip(stale, USER, false);

    Assert.assertFalse(result.isWriteNeeded());
    Assert.assertFalse(result.isWritten());
    Assert.assertEquals(model.writes(), 0);
    Assert.assertEquals(model.retrieveCount.get("a1"), Integer.valueOf(1));
  }

  @Test
  public void testIdentifierChecks() {
    ModelStub model = new ModelStub();
    AccessRulesApplier applier = applier(model, rules());
    Assert.assertEquals(applier.getMetadataType(), "eterna_admin");
    Assert.assertEquals(applier.getDefaultAct(), "OSL");
    try {
      applier.applyToAip(new AIP(), USER, true);
      Assert.fail("AIP utan id ska avvisas");
    } catch (IllegalArgumentException e) {
      Assert.assertTrue(e.getMessage().contains("id"));
    } catch (Exception e) {
      Assert.fail("fel undantag: " + e);
    }
  }
}

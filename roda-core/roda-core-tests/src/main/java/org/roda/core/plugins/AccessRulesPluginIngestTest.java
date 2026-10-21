/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.plugins;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.roda.core.RodaCoreFactory;
import org.roda.core.TestsHelper;
import org.roda.core.common.access.AccessRuleRegistry;
import org.roda.core.common.access.AccessRulesApplier;
import org.roda.core.common.access.AccessRulesModelObserver;
import org.roda.core.common.access.EternaAdminParser;
import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.common.RodaConstants.PreservationEventType;
import org.roda.core.data.exceptions.RODAException;
import org.roda.core.data.v2.accessrules.AccessRule;
import org.roda.core.data.v2.accessrules.AccessRuleDefaults;
import org.roda.core.data.v2.accessrules.AccessRuleSet;
import org.roda.core.data.v2.accessrules.AccessRuleState;
import org.roda.core.data.v2.index.filter.Filter;
import org.roda.core.data.v2.index.filter.SimpleFilterParameter;
import org.roda.core.data.v2.index.select.SelectedItemsList;
import org.roda.core.data.v2.ip.AIP;
import org.roda.core.data.v2.ip.IndexedAIP;
import org.roda.core.data.v2.ip.Permissions;
import org.roda.core.data.v2.ip.Permissions.PermissionType;
import org.roda.core.data.v2.ip.metadata.IndexedPreservationEvent;
import org.roda.core.data.v2.jobs.Job;
import org.roda.core.data.v2.jobs.PluginParameter;
import org.roda.core.data.v2.jobs.PluginParameter.PluginParameterType;
import org.roda.core.data.v2.jobs.PluginState;
import org.roda.core.data.v2.jobs.PluginType;
import org.roda.core.data.v2.jobs.Report;
import org.roda.core.data.v2.user.Group;
import org.roda.core.index.IndexService;
import org.roda.core.index.IndexTestUtils;
import org.roda.core.model.ModelService;
import org.roda.core.plugins.base.ingest.AccessRulesPlugin;
import org.roda.core.plugins.base.ingest.v2.ConfigurableIngestPlugin;
import org.roda.core.security.LdapUtilityTestHelper;
import org.roda.core.storage.StringContentPayload;
import org.roda.core.storage.fs.FSUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Integrationstest för {@link AccessRulesPlugin}
 * och {@link AccessRulesModelObserver} mot en uppstartad kärna (Solr + LDAP
 * via Testcontainers, orkestrerare och pluginhanterare).
 *
 * <p>
 * Uppställning: två grupper i LDAP, en AIP-förälder med eterna_admin
 * (sekretess_forekommer, "OSL 26 kap. 1 §", personuppgifter forekommer) och
 * ett barn utan egen metadata; båda grupperna har READ på båda AIP:erna.
 * Regelfilen access-rules.json (i testets konfigurationskatalog) ger grupp A
 * lagrum OSL 26 (tillåten) och grupp B bara ingen_sekretess (nekad). Filen
 * skrivs först när AIP:erna är uppsatta, så att observern inte hinner
 * tillämpa reglerna innan jobbet körs.
 * </p>
 *
 * <p>
 * Kontrolleras: torrkörning skriver inget men rapporterar och bifogar CSV;
 * skarp körning tar bort READ för grupp B i modell och index på båda
 * AIP:erna, stämplar regeltillstånd (restrictedGroups, factsSource, version)
 * och skapar exakt en UPDATE-händelse per ändrad AIP (ingen ny vid omkörning);
 * samt att observern ger grupp B READ tillbaka på föräldern direkt när
 * eterna_admin uppdateras till ingen_sekretess (utan jobb) och att barnet
 * återställs av det efterkommande-jobb observern startar.
 * </p>
 */
@Test(groups = {RodaConstants.TEST_GROUP_ALL, RodaConstants.TEST_GROUP_DEV, AccessRulesPluginIngestTest.GROUP})
public class AccessRulesPluginIngestTest {
  /** Egen TestNG-grupp så att testet kan köras ensamt: -Dtestng.groups=accessrules-integration */
  public static final String GROUP = "accessrules-integration";

  private static final Logger LOGGER = LoggerFactory.getLogger(AccessRulesPluginIngestTest.class);

  private static final String GROUP_A = "arkivarier";
  private static final String GROUP_B = "handlaggare";
  private static final String ADMIN_METADATA_ID = "eterna_admin.xml";
  /** RODA delar typ-id:t "eterna_admin" i type/version vid sista understrecket. */
  private static final String ADMIN_METADATA_TYPE = "eterna";
  private static final String ADMIN_METADATA_VERSION = "admin";
  private static final int RULES_VERSION = 7;
  private static final String NS = EternaAdminParser.NAMESPACE;
  /** Väntetid på det asynkrona efterkommande-jobbet som observern startar. */
  private static final long DESCENDANTS_JOB_TIMEOUT_MS = 90_000;

  private static Path basePath;
  private static ModelService model;
  private static IndexService index;
  private static LdapUtilityTestHelper ldapUtilityTestHelper;
  private static Path rulesFile;

  @BeforeClass
  public static void setUp() throws Exception {
    basePath = TestsHelper.createBaseTempDir(AccessRulesPluginIngestTest.class, true);
    ldapUtilityTestHelper = new LdapUtilityTestHelper();

    boolean deploySolr = true;
    boolean deployLdap = true;
    boolean deployFolderMonitor = false;
    boolean deployOrchestrator = true;
    boolean deployPluginManager = true;
    boolean deployDefaultResources = false;
    RodaCoreFactory.instantiateTest(deploySolr, deployLdap, deployFolderMonitor, deployOrchestrator,
      deployPluginManager, deployDefaultResources, false, ldapUtilityTestHelper.getLdapUtility());

    model = RodaCoreFactory.getModelService();
    index = RodaCoreFactory.getIndexService();

    // regelregistret är en lat singleton; se till att det pekar på detta
    // tests konfigurationskatalog (och inte en tidigare testklass' RODA_HOME)
    rulesFile = RodaCoreFactory.getConfigPath().resolve(AccessRuleRegistry.DEFAULT_FILE_NAME).toAbsolutePath()
      .normalize();
    Files.deleteIfExists(rulesFile);
    AccessRuleRegistry.resetInstanceForTests();
    Assert.assertEquals(AccessRuleRegistry.getInstance().getFile(), rulesFile);
    // utan fil gäller klassvägsstandarden: inga regler ⇒ applikatorn rör inget
    Assert.assertTrue(AccessRuleRegistry.getInstance().get().getManagedGroups().isEmpty());

    LOGGER.debug("Running access rules integration test under storage {}", basePath);
  }

  @BeforeMethod
  public static void resetIndex() {
    IndexTestUtils.resetIndex();
  }

  @AfterClass
  public static void tearDown() throws Exception {
    IndexTestUtils.resetIndex();
    ldapUtilityTestHelper.shutdown();
    RodaCoreFactory.shutdown();
    FSUtils.deletePath(basePath);
  }

  @Test
  public void testApplyAccessRulesJobAndObserver() throws Exception {
    // 1. grupper i LDAP
    createGroup(GROUP_A, "Arkivarier");
    createGroup(GROUP_B, "Handläggare");

    // 2. AIP-träd: förälder med eterna_admin (skapas före barnet så att
    // observern inte hittar några efterkommande), barn utan metadata
    AIP parent = model.createAIP(null, "", readForBothGroups(), RodaConstants.ADMIN, null);
    model.createDescriptiveMetadata(parent.getId(), ADMIN_METADATA_ID,
      new StringContentPayload(adminXml("sekretess_forekommer", "OSL 26 kap. 1 §", "forekommer")),
      ADMIN_METADATA_TYPE, ADMIN_METADATA_VERSION, RodaConstants.ADMIN);
    AIP child = model.createAIP(parent.getId(), "", readForBothGroups(), RodaConstants.ADMIN, null);
    index.commitAIPs();

    // utgångsläge: båda grupperna läser båda AIP:erna, inget regeltillstånd
    for (String aipId : Arrays.asList(parent.getId(), child.getId())) {
      AIP aip = model.retrieveAIP(aipId);
      Assert.assertEquals(readGroups(aip), Set.of(GROUP_A, GROUP_B), aipId);
      Assert.assertNull(aip.getAccessRuleState(), aipId);
    }
    Assert.assertEquals(countIndexedWithRead(GROUP_A), 2L);
    Assert.assertEquals(countIndexedWithRead(GROUP_B), 2L);

    // 3. regelfil: A tillåten vid OSL 26, B bara vid ingen_sekretess
    writeRules(rules());
    AccessRuleSet loaded = AccessRuleRegistry.getInstance().get();
    Assert.assertTrue(loaded.isEnabled());
    Assert.assertEquals(loaded.getVersion(), RULES_VERSION);
    Assert.assertEquals(loaded.getManagedGroups(), Set.of(GROUP_A, GROUP_B));

    // 4. torrkörning: rapport och CSV men ingen skrivning, ingen händelse
    Job dryRun = runJob(true, parent.getId(), child.getId());
    List<Report> dryReports = successfulReports(dryRun, 2);
    for (Report report : dryReports) {
      Assert.assertTrue(details(report).contains("torrkörning"), details(report));
      Assert.assertTrue(details(report).contains(GROUP_B), details(report));
    }
    for (String aipId : Arrays.asList(parent.getId(), child.getId())) {
      AIP aip = model.retrieveAIP(aipId);
      Assert.assertEquals(readGroups(aip), Set.of(GROUP_A, GROUP_B), "torrkörning får inte skriva: " + aipId);
      Assert.assertNull(aip.getAccessRuleState(), "torrkörning får inte stämpla tillstånd: " + aipId);
      Assert.assertEquals(countUpdateEvents(aipId), 0L, "torrkörning får inte skapa händelser: " + aipId);
    }
    assertCsvAttachment(dryRun, parent.getId(), child.getId());

    // 5. skarp körning: B förlorar READ på båda, A behåller; tillstånd stämplas
    Job apply = runJob(false, parent.getId(), child.getId());
    successfulReports(apply, 2);

    AIP parentApplied = model.retrieveAIP(parent.getId());
    Assert.assertEquals(readGroups(parentApplied), Set.of(GROUP_A));
    AccessRuleState parentState = parentApplied.getAccessRuleState();
    Assert.assertNotNull(parentState);
    Assert.assertEquals(parentState.getRestrictedGroups(), Set.of(GROUP_B));
    Assert.assertEquals(parentState.getFactsSource(), AccessRulesApplier.FACTS_SOURCE_SELF);
    Assert.assertEquals(parentState.getRulesVersion(), String.valueOf(RULES_VERSION));
    Assert.assertEquals(parentApplied.getUpdatedBy(), RodaConstants.ADMIN);

    AIP childApplied = model.retrieveAIP(child.getId());
    Assert.assertEquals(readGroups(childApplied), Set.of(GROUP_A));
    AccessRuleState childState = childApplied.getAccessRuleState();
    Assert.assertNotNull(childState);
    Assert.assertEquals(childState.getRestrictedGroups(), Set.of(GROUP_B));
    Assert.assertEquals(childState.getFactsSource(), parent.getId(), "barnet ärver faktan från föräldern");
    Assert.assertEquals(childState.getRulesVersion(), String.valueOf(RULES_VERSION));

    index.commitAIPs();
    Assert.assertEquals(countIndexedWithRead(GROUP_A), 2L);
    Assert.assertEquals(countIndexedWithRead(GROUP_B), 0L);
    for (String aipId : Arrays.asList(parent.getId(), child.getId())) {
      Assert.assertTrue(indexedGroupPermissions(aipId, GROUP_A).contains(PermissionType.READ), aipId);
      Assert.assertFalse(indexedGroupPermissions(aipId, GROUP_B).contains(PermissionType.READ), aipId);
      Assert.assertEquals(countUpdateEvents(aipId), 1L, "en UPDATE-händelse per ändrad AIP: " + aipId);
    }
    assertCsvAttachment(apply, parent.getId(), child.getId());

    // 6. omkörning: inget ändras och inga nya händelser
    Job again = runJob(false, parent.getId(), child.getId());
    for (Report report : successfulReports(again, 2)) {
      Assert.assertTrue(details(report).contains("oförändrad"), details(report));
    }
    for (String aipId : Arrays.asList(parent.getId(), child.getId())) {
      Assert.assertEquals(readGroups(model.retrieveAIP(aipId)), Set.of(GROUP_A), aipId);
      Assert.assertEquals(countUpdateEvents(aipId), 1L, "omkörning får inte ge nya händelser: " + aipId);
    }

    // 7. observer: ingen_sekretess på föräldern ⇒ B får READ tillbaka direkt,
    // synkront i modellens notifiering (efterkommande-jobbets urval omfattar
    // bara barnet, så föräldern kan bara ha återställts av observern)
    model.updateDescriptiveMetadata(parent.getId(), ADMIN_METADATA_ID,
      new StringContentPayload(adminXml("ingen_sekretess", null, "forekommer")), ADMIN_METADATA_TYPE,
      ADMIN_METADATA_VERSION, new HashMap<>(), RodaConstants.ADMIN);

    AIP parentRestored = model.retrieveAIP(parent.getId());
    Assert.assertEquals(readGroups(parentRestored), Set.of(GROUP_A, GROUP_B));
    // observern stämplar den användare vars metadataändring utlöste skrivningen
    Assert.assertEquals(parentRestored.getUpdatedBy(), RodaConstants.ADMIN);
    Assert.assertNotNull(parentRestored.getAccessRuleState());
    Assert.assertTrue(parentRestored.getAccessRuleState().getRestrictedGroups().isEmpty(),
      parentRestored.getAccessRuleState().toString());
    // ... och lämnar ett spår: en UPDATE-händelse utöver jobbets
    Assert.assertEquals(countUpdateEvents(parent.getId()), 2L,
      "observern ska skapa en UPDATE-händelse när den återställer READ");

    // barnet ärver den nya faktan och återställs av det efterkommande-jobb
    // observern startar asynkront
    boolean childRestored = waitUntil(() -> readGroups(model.retrieveAIP(child.getId())).contains(GROUP_B),
      DESCENDANTS_JOB_TIMEOUT_MS);
    Assert.assertTrue(childRestored, "efterkommande-jobbet återställde inte barnets READ för " + GROUP_B
      + " inom " + DESCENDANTS_JOB_TIMEOUT_MS + " ms");
    AIP childRestoredAip = model.retrieveAIP(child.getId());
    Assert.assertEquals(readGroups(childRestoredAip), Set.of(GROUP_A, GROUP_B));
    Assert.assertTrue(childRestoredAip.getAccessRuleState().getRestrictedGroups().isEmpty());
    // efterkommande-jobbet körs som systemanvändaren men stämplar den utlösande användaren
    Assert.assertEquals(childRestoredAip.getUpdatedBy(), RodaConstants.ADMIN);
  }

  /**
   * ConfigurableIngestPlugin.getParameters() bygger listan via loadMap(), som
   * slår upp valfria plugin i pluginhanteraren och läser konfigurationen –
   * därför här, mot den uppstartade kärnan.
   */
  @Test
  public void testIngestParametersIncludeAccessRulesSwitch() {
    List<PluginParameter> parameters = new ConfigurableIngestPlugin().getParameters();
    int authorization = indexOfParameter(parameters, RodaConstants.PLUGIN_PARAMS_DO_PRODUCER_AUTHORIZATION_CHECK);
    int accessRules = indexOfParameter(parameters, RodaConstants.PLUGIN_PARAMS_DO_APPLY_ACCESS_RULES);
    int disposalRules = indexOfParameter(parameters, RodaConstants.PLUGIN_PARAMS_DO_APPLY_DISPOSAL_RULES);

    Assert.assertTrue(accessRules >= 0, "parametern för åtkomstregler saknas");
    Assert.assertEquals(accessRules, authorization + 1);
    Assert.assertEquals(disposalRules, accessRules + 1);
    PluginParameter parameter = parameters.get(accessRules);
    Assert.assertEquals(parameter.getType(), PluginParameterType.BOOLEAN);
    Assert.assertEquals(parameter.getDefaultValue(), "true");
    Assert.assertEquals(parameter.getName(), AccessRulesPlugin.getStaticName());
    Assert.assertEquals(parameter.getDescription(), AccessRulesPlugin.getStaticDescription());
    Assert.assertTrue(parameter.isReadonly(), "steget är obligatoriskt och brytaren skrivskyddad");
  }

  private static int indexOfParameter(List<PluginParameter> parameters, String id) {
    for (int i = 0; i < parameters.size(); i++) {
      if (parameters.get(i) != null && id.equals(parameters.get(i).getId())) {
        return i;
      }
    }
    return -1;
  }

  // ------------------------------------------------------------------ uppställning

  private static void createGroup(String name, String fullName) throws RODAException {
    Group group = new Group(name);
    group.setActive(true);
    group.setFullName(fullName);
    model.createGroup(group, true);
    Assert.assertNotNull(model.retrieveGroup(name));
  }

  private static Set<PermissionType> perms(PermissionType... types) {
    return new HashSet<>(Arrays.asList(types));
  }

  private static Permissions readForBothGroups() {
    Permissions permissions = new Permissions();
    permissions.setGroupPermissions(GROUP_A, perms(PermissionType.READ));
    permissions.setGroupPermissions(GROUP_B, perms(PermissionType.READ));
    return permissions;
  }

  private static String adminXml(String sekretess, String sekretessgrund, String personuppgifter) {
    StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<eternaAdmin xmlns=\"" + NS
      + "\">\n");
    sb.append("  <sekretess>").append(sekretess).append("</sekretess>\n");
    if (sekretessgrund != null) {
      sb.append("  <sekretessgrund>").append(sekretessgrund).append("</sekretessgrund>\n");
    }
    sb.append("  <personuppgifter>").append(personuppgifter).append("</personuppgifter>\n");
    return sb.append("</eternaAdmin>\n").toString();
  }

  /** A: sekretess tillåten vid OSL 26; B: bara ingen_sekretess. */
  private static AccessRuleSet rules() {
    AccessRuleSet set = new AccessRuleSet();
    set.setVersion(RULES_VERSION);
    set.setEnabled(true);
    AccessRuleDefaults defaults = new AccessRuleDefaults();
    defaults.setEjBedomd("closed");
    defaults.setMissingMetadata("inherit");
    defaults.setMissingLagrum("closed");
    defaults.setMultipleLagrum("all");
    defaults.setDefaultAct("OSL");
    set.setDefaults(defaults);

    AccessRule a = new AccessRule(GROUP_A);
    a.setSekretess(List.of("ingen_sekretess", "sekretess_forekommer"));
    a.setLagrum(List.of("OSL 26"));
    a.setPersonuppgifter(List.of("*"));
    a.setPersonuppgiftsniva(List.of("*"));
    set.getRules().add(a);

    AccessRule b = new AccessRule(GROUP_B);
    b.setSekretess(List.of("ingen_sekretess"));
    b.setPersonuppgifter(List.of("*"));
    b.setPersonuppgiftsniva(List.of("*"));
    set.getRules().add(b);
    return set;
  }

  private static void writeRules(AccessRuleSet set) throws Exception {
    List<String> problems = AccessRuleRegistry.validate(set);
    Assert.assertTrue(problems.isEmpty(), problems.toString());
    Files.createDirectories(rulesFile.getParent());
    Files.writeString(rulesFile, AccessRuleRegistry.toJson(set), StandardCharsets.UTF_8);
    AccessRuleRegistry.getInstance().invalidate();
  }

  // ------------------------------------------------------------------ jobb

  private static Job runJob(boolean dryRun, String... aipIds) throws RODAException {
    Map<String, String> parameters = new HashMap<>();
    parameters.put(RodaConstants.PLUGIN_PARAMS_ACCESS_RULES_DRY_RUN, Boolean.toString(dryRun));
    parameters.put(RodaConstants.PLUGIN_PARAMS_ACCESS_RULES_CREATE_EVENTS, "true");
    Job job = TestsHelper.executeJob(AccessRulesPlugin.class, parameters, PluginType.INTERNAL,
      SelectedItemsList.create(AIP.class, aipIds));
    Assert.assertEquals(job.getJobStats().getSourceObjectsCount(), aipIds.length);
    return job;
  }

  private static List<Report> successfulReports(Job job, int expected) throws RODAException {
    List<Report> reports = TestsHelper.getJobReports(index, job, true);
    Assert.assertEquals(reports.size(), expected, reports.toString());
    for (Report report : reports) {
      Assert.assertEquals(report.getPluginState(), PluginState.SUCCESS, details(report));
    }
    return reports;
  }

  /** Pluginets detaljtext ligger i jobbrapportpostens delrapport. */
  private static String details(Report report) {
    if (report.getReports() != null && !report.getReports().isEmpty()) {
      return report.getReports().get(0).getPluginDetails();
    }
    return report.getPluginDetails();
  }

  private static void assertCsvAttachment(Job job, String... aipIds) throws Exception {
    String fileName = AccessRulesPlugin.CSV_ATTACHMENT_PREFIX + job.getId() + ".csv";
    Path csv = RodaCoreFactory.getJobAttachmentsDirectoryPath().resolve(job.getId()).resolve(fileName);
    Assert.assertTrue(Files.isRegularFile(csv), "CSV-bilagan saknas: " + csv);
    List<String> lines = Files.readAllLines(csv, StandardCharsets.UTF_8);
    Assert.assertEquals(lines.size(), 1 + aipIds.length, lines.toString());
    Assert.assertTrue(lines.get(0).contains(AccessRulesPlugin.CSV_HEADER.get(0)), lines.get(0));
    Assert.assertTrue(lines.get(0).contains("utfall"), lines.get(0));
    for (String aipId : aipIds) {
      Assert.assertTrue(lines.stream().anyMatch(line -> line.startsWith(aipId)), "rad saknas för " + aipId);
    }
    Assert.assertTrue(model.retrieveJob(job.getId()).getAttachmentsList().contains(fileName),
      "bilagan är inte registrerad på jobbet: " + model.retrieveJob(job.getId()).getAttachmentsList());
  }

  // ------------------------------------------------------------------ kontroller

  private static Set<String> readGroups(AIP aip) {
    Set<String> groups = aip.getPermissions().getGroups().get(PermissionType.READ);
    return groups == null ? Set.of() : groups;
  }

  private static long countIndexedWithRead(String group) throws RODAException {
    Filter filter = new Filter(
      new SimpleFilterParameter(RodaConstants.INDEX_PERMISSION_GROUPS_PREFIX + PermissionType.READ, group));
    return index.count(IndexedAIP.class, filter);
  }

  private static Set<PermissionType> indexedGroupPermissions(String aipId, String group) throws RODAException {
    IndexedAIP indexed = index.retrieve(IndexedAIP.class, aipId, RodaConstants.AIP_PERMISSIONS_FIELDS_TO_RETURN);
    return indexed.getPermissions().getGroupPermissions(group);
  }

  private static long countUpdateEvents(String aipId) throws RODAException {
    index.commit(IndexedPreservationEvent.class);
    Filter filter = new Filter(
      new SimpleFilterParameter(RodaConstants.PRESERVATION_EVENT_TYPE, PreservationEventType.UPDATE.toString()),
      new SimpleFilterParameter(RodaConstants.PRESERVATION_EVENT_AIP_ID, aipId));
    return index.count(IndexedPreservationEvent.class, filter);
  }

  @FunctionalInterface
  private interface Condition {
    boolean holds() throws Exception;
  }

  private static boolean waitUntil(Condition condition, long timeoutMs) throws Exception {
    long deadline = System.currentTimeMillis() + timeoutMs;
    while (System.currentTimeMillis() < deadline) {
      if (condition.holds()) {
        return true;
      }
      Thread.sleep(500);
    }
    return condition.holds();
  }
}

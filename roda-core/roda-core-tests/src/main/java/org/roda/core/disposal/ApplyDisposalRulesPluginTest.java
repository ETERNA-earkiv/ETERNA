/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.disposal;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;

import org.roda.core.CorporaConstants;
import org.roda.core.RodaCoreFactory;
import org.roda.core.TestsHelper;
import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.v2.disposal.rule.ConditionType;
import org.roda.core.data.v2.disposal.rule.DisposalRule;
import org.roda.core.data.v2.disposal.schedule.DisposalActionCode;
import org.roda.core.data.v2.disposal.schedule.DisposalSchedule;
import org.roda.core.data.v2.disposal.schedule.RetentionPeriodIntervalCode;
import org.roda.core.data.v2.index.filter.Filter;
import org.roda.core.data.v2.index.filter.SimpleFilterParameter;
import org.roda.core.data.v2.index.select.SelectedItemsFilter;
import org.roda.core.data.v2.ip.AIP;
import org.roda.core.data.v2.ip.AIPDisposalScheduleAssociationType;
import org.roda.core.data.v2.ip.IndexedAIP;
import org.roda.core.data.v2.jobs.PluginType;
import org.roda.core.index.IndexService;
import org.roda.core.index.IndexServiceTest;
import org.roda.core.index.IndexTestUtils;
import org.roda.core.model.ModelService;
import org.roda.core.plugins.base.disposal.rules.ApplyDisposalRulesPlugin;
import org.roda.core.plugins.base.disposal.rules.ApplyDisposalRulesPluginUtils;
import org.roda.core.storage.DefaultStoragePath;
import org.roda.core.storage.StorageService;
import org.roda.core.storage.fs.FSUtils;
import org.roda.core.storage.fs.FileStorageService;
import org.roda.core.util.IdUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Regression test for the disposal rule matching divergence between the preview
 * (Solr query) and the apply job (previously a raw {@code String.equals}).
 *
 * <p>
 * See {@code docs/issues/disposal-rule-apply-vs-preview-matching.md}.
 */
@Test(groups = {RodaConstants.TEST_GROUP_ALL, RodaConstants.TEST_GROUP_DEV, RodaConstants.TEST_GROUP_TRAVIS})
public class ApplyDisposalRulesPluginTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(ApplyDisposalRulesPluginTest.class);

  private static StorageService corporaService;
  private Path basePath;
  private ModelService model;
  private IndexService index;

  @BeforeClass
  public void setUp() throws Exception {
    basePath = TestsHelper.createBaseTempDir(ApplyDisposalRulesPluginTest.class, true);

    boolean deploySolr = true;
    boolean deployLdap = false;
    boolean deployFolderMonitor = false;
    boolean deployOrchestrator = true;
    boolean deployPluginManager = true;
    boolean deployDefaultResources = false;
    RodaCoreFactory.instantiateTest(deploySolr, deployLdap, deployFolderMonitor, deployOrchestrator,
      deployPluginManager, deployDefaultResources, false);
    // The disposal rule condition whitelist comes from roda-wui.properties (ui.search.fields.*), which the WUI
    // registers at runtime (RodaWuiServlet) but the core test harness does not load by default.
    RodaCoreFactory.addConfiguration("roda-wui.properties");
    model = RodaCoreFactory.getModelService();
    index = RodaCoreFactory.getIndexService();

    URL corporaURL = IndexServiceTest.class.getResource("/corpora");
    corporaService = new FileStorageService(Paths.get(corporaURL.toURI()));

    LOGGER.info("Running ApplyDisposalRulesPlugin tests under storage {}", basePath);
  }

  @AfterClass
  public void tearDown() throws Exception {
    IndexTestUtils.resetIndex();
    RodaCoreFactory.shutdown();
    FSUtils.deletePath(basePath);
  }

  @AfterMethod
  public void cleanUp() throws Exception {
    // Disposal rules are global; remove them between methods so each test is isolated.
    for (DisposalRule rule : model.listDisposalRules().getObjects()) {
      model.deleteDisposalRule(rule.getId(), RodaConstants.ADMIN);
    }
    TestsHelper.releaseAllLocks();
  }

  /** "example" is only a token inside the title "My example": matches the preview's tokenized Solr query, not the old exact String.equals. */
  @Test
  public void applyRuleMatchesTokenizedMetadataFieldLikePreview() throws Exception {
    // AIP from corpora: EAD descriptive metadata with title "My example"
    final String aipId = IdUtils.createUUID();
    final DefaultStoragePath aipPath = DefaultStoragePath.parse(CorporaConstants.SOURCE_AIP_CONTAINER,
      CorporaConstants.SOURCE_AIP_ID);
    final AIP aip = model.createAIP(aipId, corporaService, aipPath, RodaConstants.ADMIN);
    index.commitAIPs();

    // Precondition: the condition matches via tokenized search, localizing any failure to the association step.
    final Filter ruleFilter = new Filter(new SimpleFilterParameter(RodaConstants.INDEX_UUID, aipId),
      new SimpleFilterParameter(RodaConstants.AIP_TITLE, "example"));
    assertEquals("Precondition: rule condition must match the AIP via tokenized search", Long.valueOf(1L),
      index.count(IndexedAIP.class, ruleFilter));

    final DisposalSchedule schedule = createDestroySchedule();

    final DisposalRule rule = new DisposalRule();
    rule.setTitle("Title contains the token 'example'");
    rule.setType(ConditionType.METADATA_FIELD);
    rule.setConditionKey(RodaConstants.AIP_TITLE);
    rule.setConditionValue("example");
    rule.setDisposalScheduleId(schedule.getId());
    rule.setDisposalScheduleName(schedule.getTitle());
    rule.setOrder(0);
    model.createDisposalRule(rule, RodaConstants.ADMIN);

    final SelectedItemsFilter<IndexedAIP> selectedItems = new SelectedItemsFilter<>(
      new Filter(new SimpleFilterParameter(RodaConstants.INDEX_UUID, aipId)), IndexedAIP.class.getName(), false);
    TestsHelper.executeJob(ApplyDisposalRulesPlugin.class, Collections.<String, String> emptyMap(),
      PluginType.AIP_TO_AIP, selectedItems);

    final AIP updated = model.retrieveAIP(aip.getId());
    assertNotNull("Disposal metadata should have been set on the AIP", updated.getDisposal());
    assertNotNull("Disposal schedule should have been associated", updated.getDisposal().getSchedule());
    assertEquals("The matching rule's schedule must be associated", schedule.getId(),
      updated.getDisposal().getSchedule().getId());
    assertEquals("Association must be flagged as coming from a rule", AIPDisposalScheduleAssociationType.RULES,
      updated.getDisposalScheduleAssociationType());
  }

  /** A rule with a blank condition value must be skipped, not crash the job (previously NPE'd in SolrUtils). */
  @Test
  public void applyRuleWithBlankConditionDoesNotCrashJob() throws Exception {
    final String aipId = IdUtils.createUUID();
    final DefaultStoragePath aipPath = DefaultStoragePath.parse(CorporaConstants.SOURCE_AIP_CONTAINER,
      CorporaConstants.SOURCE_AIP_ID);
    final AIP aip = model.createAIP(aipId, corporaService, aipPath, RodaConstants.ADMIN);
    index.commitAIPs();

    final DisposalSchedule schedule = createDestroySchedule();

    final DisposalRule rule = new DisposalRule();
    rule.setTitle("Incomplete metadata rule");
    rule.setType(ConditionType.METADATA_FIELD);
    rule.setConditionKey(RodaConstants.AIP_TITLE);
    rule.setConditionValue(null);
    rule.setDisposalScheduleId(schedule.getId());
    rule.setDisposalScheduleName(schedule.getTitle());
    rule.setOrder(0);
    model.createDisposalRule(rule, RodaConstants.ADMIN);

    final SelectedItemsFilter<IndexedAIP> selectedItems = new SelectedItemsFilter<>(
      new Filter(new SimpleFilterParameter(RodaConstants.INDEX_UUID, aipId)), IndexedAIP.class.getName(), false);
    // executeJob asserts the job reaches state COMPLETED; with a blank condition that would previously fail.
    TestsHelper.executeJob(ApplyDisposalRulesPlugin.class, Collections.<String, String> emptyMap(),
      PluginType.AIP_TO_AIP, selectedItems);

    final AIP updated = model.retrieveAIP(aip.getId());
    assertNull("No schedule should be associated for an incomplete rule",
      updated.getDisposal() == null ? null : updated.getDisposal().getSchedule());
  }

  /** The blacklist matches the config key (reference), not the resolved Solr field (unitId_txt), exactly as the UI. */
  @Test
  public void blacklistAppliesToConfigurationKeyNotSolrField() {
    Set<String> allowed = ApplyDisposalRulesPluginUtils.allowedMetadataConditionFields();
    assertTrue("A regular text search field must be allowed", allowed.contains("title"));
    assertFalse("The Solr field of a blacklisted configuration key must be excluded", allowed.contains("unitId_txt"));
  }

  private DisposalSchedule createDestroySchedule() throws Exception {
    DisposalSchedule schedule = new DisposalSchedule();
    schedule.setTitle("Destroy after retention");
    schedule.setActionCode(DisposalActionCode.DESTROY);
    schedule.setRetentionTriggerElementId("");
    schedule.setRetentionPeriodIntervalCode(RetentionPeriodIntervalCode.YEARS);
    schedule.setRetentionPeriodDuration(1);
    return model.createDisposalSchedule(schedule, RodaConstants.ADMIN);
  }
}

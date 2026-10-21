/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.common.access;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.v2.index.filter.Filter;
import org.roda.core.data.v2.index.filter.OneOfManyFilterParameter;
import org.roda.core.data.v2.index.filter.SimpleFilterParameter;
import org.roda.core.data.v2.index.select.SelectedItemsFilter;
import org.roda.core.data.v2.ip.IndexedAIP;
import org.roda.core.data.v2.jobs.Job;
import org.roda.core.data.v2.jobs.Job.JOB_STATE;
import org.roda.core.data.v2.jobs.JobParallelism;
import org.roda.core.data.v2.jobs.JobPriority;
import org.roda.core.data.v2.jobs.JobUserDetails;
import org.roda.core.data.v2.jobs.PluginType;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tester för jobbkonstruktionen och
 * dubblettkontrollen i {@link AccessRulesJobs} (utan orkestrator och utan
 * RodaCoreFactory).
 */
@Test(groups = {RodaConstants.TEST_GROUP_ALL, RodaConstants.TEST_GROUP_DEV, RodaConstants.TEST_GROUP_TRAVIS,
  "accessrules"})
public class AccessRulesJobsTest {

  @Test
  public void testPluginClassConstantPointsToAnExistingPlugin() throws ClassNotFoundException {
    // strängkonstant (inget beroende på pluginpaketet) – men klassen måste finnas
    Assert.assertEquals(AccessRulesJobs.PLUGIN_CLASS, "org.roda.core.plugins.base.ingest.AccessRulesPlugin");
    Class<?> plugin = Class.forName(AccessRulesJobs.PLUGIN_CLASS);
    Assert.assertTrue(org.roda.core.plugins.Plugin.class.isAssignableFrom(plugin), plugin.getName());
  }

  @Test
  public void testBuildJobForFilter() {
    Filter filter = new Filter(new SimpleFilterParameter(RodaConstants.AIP_ANCESTORS, "a1"));

    Job job = AccessRulesJobs.buildJob(filter, "Åtkomstregler: test", "anna", false, JobPriority.HIGH,
      JobParallelism.LIMITED);

    Assert.assertNotNull(job.getId());
    Assert.assertFalse(job.getId().isBlank());
    Assert.assertEquals(job.getName(), "Åtkomstregler: test");
    Assert.assertEquals(job.getPlugin(), AccessRulesJobs.PLUGIN_CLASS);
    Assert.assertEquals(job.getPluginType(), PluginType.INTERNAL);
    Assert.assertEquals(job.getUsername(), "anna");
    Assert.assertEquals(job.getPriority(), JobPriority.HIGH);
    Assert.assertEquals(job.getParallelism(), JobParallelism.LIMITED);
    Assert.assertEquals(job.getPluginParameters().get(AccessRulesJobs.PARAM_DRY_RUN), "false");
    Assert.assertEquals(job.getPluginParameters().get(AccessRulesJobs.PARAM_CREATE_EVENTS), "true");

    Assert.assertTrue(job.getSourceObjects() instanceof SelectedItemsFilter);
    SelectedItemsFilter<?> selected = (SelectedItemsFilter<?>) job.getSourceObjects();
    Assert.assertEquals(selected.getSelectedClass(), IndexedAIP.class.getName());
    Assert.assertEquals(selected.getFilter(), filter);
    Assert.assertEquals(selected.justActive(), Boolean.FALSE);
  }

  @Test
  public void testBuildJobDryRunDisablesEvents() {
    Job job = AccessRulesJobs.buildJob(new Filter(), "torr", "anna", true, JobPriority.LOW, JobParallelism.NORMAL);

    Assert.assertEquals(job.getPluginParameters().get(AccessRulesJobs.PARAM_DRY_RUN), "true");
    Assert.assertEquals(job.getPluginParameters().get(AccessRulesJobs.PARAM_CREATE_EVENTS), "false");
    Assert.assertEquals(job.getPriority(), JobPriority.LOW);
    Assert.assertEquals(job.getParallelism(), JobParallelism.NORMAL);
  }

  @Test
  public void testBuildJobDefaultsForMissingValues() {
    Job job = AccessRulesJobs.buildJob(new Filter(), " ", null, false, null, null);

    Assert.assertEquals(job.getName(), "Åtkomstregler");
    Assert.assertEquals(job.getUsername(), AccessRulesModelObserver.USER);
    Assert.assertEquals(job.getPriority(), JobPriority.MEDIUM);
    Assert.assertEquals(job.getParallelism(), JobParallelism.NORMAL);
  }

  @Test
  public void testTwoJobsGetDistinctIds() {
    Job first = AccessRulesJobs.buildJob(new Filter(), "a", "anna", false, JobPriority.MEDIUM, JobParallelism.NORMAL);
    Job second = AccessRulesJobs.buildJob(new Filter(), "b", "anna", false, JobPriority.MEDIUM,
      JobParallelism.NORMAL);

    Assert.assertNotEquals(first.getId(), second.getId());
  }

  // ---------------------------------------------------------------- efterkommande-jobb

  @Test
  public void testBuildDescendantsJobForOneAncestorCarriesTriggeringUser() {
    Job job = AccessRulesJobs.buildDescendantsJob(Collections.singletonList("a1"), " anna ", JobPriority.MEDIUM,
      JobParallelism.NORMAL);

    Assert.assertEquals(job.getName(), AccessRulesJobs.DESCENDANTS_JOB_NAME_PREFIX + "a1 (utlöst av anna)");
    // körs som systemanvändaren (undantagen pluginets läsbehörighetsfilter) ...
    Assert.assertEquals(job.getUsername(), AccessRulesModelObserver.USER);
    Assert.assertEquals(job.getPluginType(), PluginType.INTERNAL);
    // ... men bär den utlösande användaren som parameter och som jobbanvändare för händelseagenten
    Assert.assertEquals(job.getPluginParameters().get(AccessRulesJobs.PARAM_TRIGGERED_BY), "anna");
    Assert.assertEquals(job.getPluginParameters().get(AccessRulesJobs.PARAM_DRY_RUN), "false");
    Assert.assertEquals(job.getPluginParameters().get(AccessRulesJobs.PARAM_CREATE_EVENTS), "true");
    Assert.assertEquals(job.getJobUsersDetails().size(), 1);
    JobUserDetails details = job.getJobUsersDetails().get(0);
    Assert.assertEquals(details.getUsername(), "anna");
    Assert.assertEquals(details.getRole(), RodaConstants.PreservationAgentRole.IMPLEMENTER.toString());

    SelectedItemsFilter<?> selected = (SelectedItemsFilter<?>) job.getSourceObjects();
    Assert.assertEquals(selected.getFilter(),
      new Filter(new SimpleFilterParameter(RodaConstants.AIP_ANCESTORS, "a1")));
  }

  @Test
  public void testBuildDescendantsJobForManyAncestorsUsesOneOfManyFilter() {
    Job job = AccessRulesJobs.buildDescendantsJob(Arrays.asList("a1", "a2", "a1"), null, JobPriority.MEDIUM,
      JobParallelism.NORMAL);

    Assert.assertEquals(job.getName(), AccessRulesJobs.DESCENDANTS_JOB_NAME_PREFIX + "2 strukturenheter");
    Assert.assertEquals(job.getUsername(), AccessRulesModelObserver.USER);
    Assert.assertFalse(job.getPluginParameters().containsKey(AccessRulesJobs.PARAM_TRIGGERED_BY));
    Assert.assertTrue(job.getJobUsersDetails().isEmpty());

    SelectedItemsFilter<?> selected = (SelectedItemsFilter<?>) job.getSourceObjects();
    Assert.assertEquals(selected.getFilter(),
      new Filter(new OneOfManyFilterParameter(RodaConstants.AIP_ANCESTORS, Arrays.asList("a1", "a2"))));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testBuildDescendantsJobRejectsEmptySelection() {
    AccessRulesJobs.buildDescendantsJob(Collections.emptyList(), "anna", JobPriority.MEDIUM, JobParallelism.NORMAL);
  }

  @Test
  public void testDropCoveredByQueuedJob() {
    Map<String, String> pending = new HashMap<>();
    pending.put("p", "j-created");
    pending.put("x", "j-started");
    pending.put("y", "j-done");
    pending.put("gone", "j-missing");
    Map<String, JOB_STATE> states = new HashMap<>();
    states.put("j-created", JOB_STATE.CREATED);
    states.put("j-started", JOB_STATE.STARTED);
    states.put("j-done", JOB_STATE.COMPLETED);
    Function<String, JOB_STATE> jobState = states::get;
    Function<String, List<String>> ancestorsOf = aipId -> {
      switch (aipId) {
        case "c1":
          return Arrays.asList("mellan", "p");
        case "z":
          throw new IllegalStateException("indexet svarar inte");
        default:
          return Collections.emptyList();
      }
    };

    Set<String> remaining = AccessRulesJobs.dropCoveredByQueuedJob(
      Arrays.asList("c1", "x", "y", "z", "gone", null, " "), ancestorsOf, jobState, pending);

    // c1 täcks av det köade (CREATED) jobbet för förfadern p; ett påbörjat jobb
    // (x) räknas inte eftersom det kan ha cachat gamla fakta; avslutade (y) och
    // saknade (gone) jobb rensas; fel i förfäderuppslaget (z) ger ett nytt jobb
    Assert.assertEquals(new ArrayList<>(remaining), Arrays.asList("x", "y", "z", "gone"));
    Assert.assertEquals(pending.get("p"), "j-created");
    Assert.assertEquals(pending.get("x"), "j-started");
    Assert.assertFalse(pending.containsKey("y"));
    Assert.assertFalse(pending.containsKey("gone"));
  }
}

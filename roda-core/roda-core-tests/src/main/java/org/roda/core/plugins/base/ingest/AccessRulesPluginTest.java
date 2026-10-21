/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.plugins.base.ingest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.roda.core.common.access.AccessRulesModelObserver;
import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.common.RodaConstants.PreservationEventType;
import org.roda.core.data.exceptions.InvalidParameterException;
import org.roda.core.data.v2.ip.AIP;
import org.roda.core.data.v2.ip.Permissions;
import org.roda.core.data.v2.ip.Permissions.PermissionType;
import org.roda.core.data.v2.jobs.Job;
import org.roda.core.data.v2.jobs.JobUserDetails;
import org.roda.core.data.v2.jobs.PluginParameter;
import org.roda.core.data.v2.jobs.PluginParameter.PluginParameterType;
import org.roda.core.data.v2.jobs.PluginType;
import org.roda.core.data.v2.user.User;
import org.roda.core.plugins.Plugin;
import org.roda.core.plugins.base.disposal.rules.ApplyDisposalRulesPlugin;
import org.roda.core.plugins.base.ingest.v2.ConfigurableIngestPlugin;
import org.roda.core.plugins.base.ingest.v2.steps.IngestStep;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tester för {@link AccessRulesPlugin} utan
 * infrastruktur – pluginets metadata (typ, kategori, objektklass,
 * parametrar, namn, händelsetyp), parametertolkning, läsbehörighetsfiltret,
 * CSV-saneringen samt att {@link ConfigurableIngestPlugin} har steget som
 * obligatoriskt mellan {@link VerifyUserAuthorizationPlugin} och
 * {@link ApplyDisposalRulesPlugin} med torrkörning avstängd. Kontrollen av
 * inleveranspluginets parameterlista kräver en uppstartad kärna och ligger i
 * AccessRulesPluginIngestTest.
 */
@Test(groups = {RodaConstants.TEST_GROUP_ALL, RodaConstants.TEST_GROUP_DEV, RodaConstants.TEST_GROUP_TRAVIS,
  "accessrules"})
public class AccessRulesPluginTest {

  @Test
  public void metadataDescribesAMaintenanceAipPlugin() {
    AccessRulesPlugin plugin = new AccessRulesPlugin();

    Assert.assertEquals(plugin.getName(), "Tillämpa åtkomstregler");
    Assert.assertEquals(AccessRulesPlugin.getStaticName(), plugin.getName());
    Assert.assertEquals(AccessRulesPlugin.getStaticDescription(), plugin.getDescription());
    Assert.assertTrue(plugin.getDescription().contains("eterna_admin"), plugin.getDescription());
    Assert.assertTrue(plugin.getDescription().contains("aldrig"), plugin.getDescription());
    Assert.assertEquals(plugin.getVersion(), "1.0");
    Assert.assertEquals(plugin.getType(), PluginType.AIP_TO_AIP);
    Assert.assertEquals(plugin.getCategories(), List.of(RodaConstants.PLUGIN_CATEGORY_MAINTENANCE));
    Assert.assertEquals(plugin.getObjectClasses(), List.of(AIP.class));
    Assert.assertEquals(plugin.getPreservationEventType(), PreservationEventType.UPDATE);
    Assert.assertEquals(plugin.getPreservationEventDescription(),
      "Tillämpning av åtkomstregler (sekretess/personuppgifter) på behörigheter");
    Assert.assertFalse(plugin.getPreservationEventSuccessMessage().isBlank());
    Assert.assertFalse(plugin.getPreservationEventFailureMessage().isBlank());
    Assert.assertTrue(plugin.areParameterValuesValid());

    Plugin<AIP> clone = plugin.cloneMe();
    Assert.assertNotSame(clone, plugin);
    Assert.assertEquals(clone.getClass(), AccessRulesPlugin.class);
  }

  @Test
  public void parametersAreBooleanWithExpectedDefaults() {
    List<PluginParameter> parameters = new AccessRulesPlugin().getParameters();
    List<String> ids = new ArrayList<>();
    for (PluginParameter parameter : parameters) {
      ids.add(parameter.getId());
      Assert.assertEquals(parameter.getType(), PluginParameterType.BOOLEAN, parameter.getId());
      Assert.assertFalse(parameter.isReadonly(), parameter.getId() + " ska inte vara skrivskyddad");
      Assert.assertFalse(parameter.getName() == null || parameter.getName().isBlank(), parameter.getId());
      Assert.assertFalse(parameter.getDescription() == null || parameter.getDescription().isBlank(),
        parameter.getId());
    }
    // stegbrytaren tillhör inleveranspluginet och ska inte visas för den fristående åtgärden
    Assert.assertEquals(ids, List.of(RodaConstants.PLUGIN_PARAMS_ACCESS_RULES_DRY_RUN,
      RodaConstants.PLUGIN_PARAMS_ACCESS_RULES_CREATE_EVENTS, RodaConstants.PLUGIN_PARAMS_ACCESS_RULES_WRITE_STATE));
    Assert.assertFalse(ids.contains(RodaConstants.PLUGIN_PARAMS_DO_APPLY_ACCESS_RULES));

    Assert.assertEquals(parameters.get(0).getDefaultValue(), "false");
    Assert.assertEquals(parameters.get(1).getDefaultValue(), "true");
    Assert.assertEquals(parameters.get(2).getDefaultValue(), "false");
  }

  @Test
  public void parameterValuesAreParsedWithDefaults() throws InvalidParameterException {
    AccessRulesPlugin plugin = new AccessRulesPlugin();
    Assert.assertFalse(plugin.isDryRun());
    Assert.assertTrue(plugin.isCreateEvents());
    Assert.assertFalse(plugin.isWriteState());

    Map<String, String> values = new HashMap<>();
    values.put(RodaConstants.PLUGIN_PARAMS_ACCESS_RULES_DRY_RUN, "true");
    values.put(RodaConstants.PLUGIN_PARAMS_ACCESS_RULES_CREATE_EVENTS, "false");
    values.put(RodaConstants.PLUGIN_PARAMS_ACCESS_RULES_WRITE_STATE, "true");
    plugin.setParameterValues(values);
    Assert.assertTrue(plugin.isDryRun());
    Assert.assertFalse(plugin.isCreateEvents());
    Assert.assertTrue(plugin.isWriteState());
    Assert.assertSame(plugin.getParameterValues(), values);

    // saknade/tomma värden ⇒ standardvärden (inleveransen skickar inte med
    // händelser/tillstånd); stegbrytaren ignoreras
    Map<String, String> partial = new HashMap<>();
    partial.put(RodaConstants.PLUGIN_PARAMS_DO_APPLY_ACCESS_RULES, "false");
    partial.put(RodaConstants.PLUGIN_PARAMS_ACCESS_RULES_DRY_RUN, " ");
    plugin.setParameterValues(partial);
    Assert.assertFalse(plugin.isDryRun());
    Assert.assertTrue(plugin.isCreateEvents());
    Assert.assertFalse(plugin.isWriteState());
  }

  @Test
  public void ingestStepIsMandatoryBetweenAuthorizationAndDisposalRulesWithoutDryRun() {
    List<IngestStep> steps = new ConfigurableIngestPlugin().getIngestSteps();
    int authorization = indexOfStep(steps, VerifyUserAuthorizationPlugin.class);
    int accessRules = indexOfStep(steps, AccessRulesPlugin.class);
    int disposalRules = indexOfStep(steps, ApplyDisposalRulesPlugin.class);

    Assert.assertTrue(authorization >= 0, "VerifyUserAuthorizationPlugin saknas i steglistan");
    Assert.assertTrue(accessRules >= 0, "AccessRulesPlugin saknas i steglistan");
    Assert.assertTrue(disposalRules >= 0, "ApplyDisposalRulesPlugin saknas i steglistan");
    Assert.assertEquals(accessRules, authorization + 1,
      "AccessRulesPlugin ska följa direkt på VerifyUserAuthorizationPlugin");
    Assert.assertEquals(disposalRules, accessRules + 1,
      "ApplyDisposalRulesPlugin ska följa direkt på AccessRulesPlugin");

    IngestStep step = steps.get(accessRules);
    Assert.assertEquals(step.getParameterName(), RodaConstants.PLUGIN_PARAMS_DO_APPLY_ACCESS_RULES);
    Assert.assertTrue(step.usesCorePlugin());
    Assert.assertTrue(step.isMandatory(), "steget ska vara obligatoriskt (ingen tyst policyförbikoppling)");
    Assert.assertTrue(step.needsAips());
    Assert.assertFalse(step.removesAips());
    // stegets fasta parametrar slår inleveransjobbets: torrkörning kan inte smugglas in
    Assert.assertEquals(step.getParameters().get(RodaConstants.PLUGIN_PARAMS_ACCESS_RULES_DRY_RUN), "false");
    Assert.assertEquals(countSteps(steps, AccessRulesPlugin.class), 1);
  }

  @Test
  public void csvCellsAreProtectedAgainstFormulaInjection() {
    Assert.assertEquals(AccessRulesPlugin.sanitizeCell(null), "");
    Assert.assertEquals(AccessRulesPlugin.sanitizeCell(""), "");
    Assert.assertEquals(AccessRulesPlugin.sanitizeCell("Socialnämnden 2019"), "Socialnämnden 2019");
    Assert.assertEquals(AccessRulesPlugin.sanitizeCell("=HYPERLINK(\"http://x\")"), "'=HYPERLINK(\"http://x\")");
    Assert.assertEquals(AccessRulesPlugin.sanitizeCell("+1"), "'+1");
    Assert.assertEquals(AccessRulesPlugin.sanitizeCell("-1"), "'-1");
    Assert.assertEquals(AccessRulesPlugin.sanitizeCell("@cmd"), "'@cmd");
    Assert.assertEquals(AccessRulesPlugin.sanitizeCell("a\tb\r\nc"), "a b  c");
  }

  @Test
  public void csvHeaderReportsTheSameWritePermissionColumnsAsTheReportText() {
    Assert.assertTrue(AccessRulesPlugin.CSV_HEADER.contains("skrivbehorigheterBorttagna"));
    Assert.assertTrue(AccessRulesPlugin.CSV_HEADER.contains("skrivbehorigheterAterstallda"));
    Assert.assertFalse(AccessRulesPlugin.CSV_HEADER.contains("skrivrattigheterBorttagna"));
    Assert.assertEquals(AccessRulesPlugin.CSV_HEADER.get(0), "aipId");
    Assert.assertEquals(AccessRulesPlugin.CSV_HEADER.get(AccessRulesPlugin.CSV_HEADER.size() - 1), "utfall");
  }

  @Test
  public void userCanReadViaOwnEntryGroupOrAdministrator() {
    Permissions permissions = new Permissions();
    permissions.setUserPermissions("eva", new HashSet<>(Set.of(PermissionType.READ)));
    permissions.setGroupPermissions("arkivarier", new HashSet<>(Set.of(PermissionType.READ)));
    permissions.setGroupPermissions("handlaggare", new HashSet<>(Set.of(PermissionType.CREATE)));

    Assert.assertTrue(AccessRulesPlugin.userCanRead(user("eva"), permissions), "egen post");
    Assert.assertTrue(AccessRulesPlugin.userCanRead(user("anna", "arkivarier"), permissions), "grupp");
    Assert.assertFalse(AccessRulesPlugin.userCanRead(user("anna", "handlaggare"), permissions), "grupp utan READ");
    Assert.assertFalse(AccessRulesPlugin.userCanRead(user("anna"), permissions), "varken post eller grupp");
    Assert.assertTrue(AccessRulesPlugin.userCanRead(user(RodaConstants.ADMIN), permissions), "administratören");
    Assert.assertTrue(AccessRulesPlugin.userCanRead(user(RodaConstants.ADMIN), null));
    Assert.assertFalse(AccessRulesPlugin.userCanRead(user("anna", "arkivarier"), null));
    Assert.assertFalse(AccessRulesPlugin.userCanRead(null, permissions));
  }

  @Test
  public void triggeringUserIsOnlyTrustedOnSystemJobs() {
    Job system = new Job();
    system.setUsername(AccessRulesModelObserver.USER);
    system.setPluginParameters(new HashMap<>(Map.of(RodaConstants.PLUGIN_PARAMS_ACCESS_RULES_TRIGGERED_BY, " anna ")));
    Assert.assertTrue(AccessRulesPlugin.isSystemJob(system));
    Assert.assertEquals(AccessRulesPlugin.triggeredBy(system), "anna");

    // samma parameter på ett användarstartat jobb ignoreras (kan inte litas på)
    Job user = new Job();
    user.setUsername("bob");
    user.setPluginParameters(new HashMap<>(Map.of(RodaConstants.PLUGIN_PARAMS_ACCESS_RULES_TRIGGERED_BY, "anna")));
    Assert.assertFalse(AccessRulesPlugin.isSystemJob(user));
    Assert.assertNull(AccessRulesPlugin.triggeredBy(user));

    Assert.assertFalse(AccessRulesPlugin.isSystemJob(null));
    Assert.assertNull(AccessRulesPlugin.triggeredBy(null));
  }

  @Test
  public void eventJobCarriesTriggeringUserAsCreator() {
    Job system = new Job();
    system.setId("job-1");
    system.setUsername(AccessRulesModelObserver.USER);
    system.setPluginParameters(new HashMap<>(Map.of(RodaConstants.PLUGIN_PARAMS_ACCESS_RULES_TRIGGERED_BY, "anna")));
    JobUserDetails details = new JobUserDetails();
    details.setUsername("anna");
    system.getJobUsersDetails().add(details);

    // händelsens skapare (= AIP:ns updatedBy) blir den utlösande användaren;
    // jobbet självt och agenterna rörs inte
    Job eventJob = AccessRulesPlugin.eventJob(system, AccessRulesPlugin.triggeredBy(system));
    Assert.assertNotSame(eventJob, system);
    Assert.assertEquals(eventJob.getUsername(), "anna");
    Assert.assertEquals(eventJob.getId(), "job-1");
    Assert.assertEquals(eventJob.getJobUsersDetails(), system.getJobUsersDetails());
    Assert.assertEquals(system.getUsername(), AccessRulesModelObserver.USER);

    // utan utlösande användare: jobbet självt
    Job user = new Job();
    user.setUsername("bob");
    Assert.assertSame(AccessRulesPlugin.eventJob(user, null), user);
    Assert.assertNull(AccessRulesPlugin.eventJob(null, "anna"));
  }

  private static User user(String name, String... groups) {
    User user = new User(name);
    user.setGroups(new HashSet<>(List.of(groups)));
    return user;
  }

  private static int indexOfStep(List<IngestStep> steps, Class<?> pluginClass) {
    for (int i = 0; i < steps.size(); i++) {
      if (pluginClass.getName().equals(steps.get(i).getPluginName())) {
        return i;
      }
    }
    return -1;
  }

  private static int countSteps(List<IngestStep> steps, Class<?> pluginClass) {
    int count = 0;
    for (IngestStep step : steps) {
      if (pluginClass.getName().equals(step.getPluginName())) {
        count++;
      }
    }
    return count;
  }
}

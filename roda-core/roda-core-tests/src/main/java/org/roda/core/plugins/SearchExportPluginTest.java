/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.plugins;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.v2.ip.IndexedAIP;
import org.roda.core.data.v2.ip.Permissions;
import org.roda.core.data.v2.ip.Permissions.PermissionType;
import org.roda.core.data.v2.jobs.IndexedJob;
import org.roda.core.data.v2.jobs.IndexedReport;
import org.roda.core.data.v2.jobs.Job;
import org.roda.core.data.v2.jobs.PluginState;
import org.roda.core.data.v2.jobs.PluginType;
import org.roda.core.data.v2.log.LogEntry;
import org.roda.core.data.v2.log.LogEntryState;
import org.roda.core.data.v2.user.Group;
import org.roda.core.data.v2.user.User;
import org.roda.core.plugins.base.preservation.SearchExportPlugin;
import org.testng.annotations.Test;

@Test(groups = {RodaConstants.TEST_GROUP_ALL, RodaConstants.TEST_GROUP_DEV})
public class SearchExportPluginTest {

  @Test
  public void testParameterParsingWithAllFields() throws Exception {
    SearchExportPlugin plugin = new SearchExportPlugin();
    Map<String, String> params = new HashMap<>();
    params.put(SearchExportPlugin.PARAM_FILTER, "{\"filterParameters\":[]}");
    params.put(SearchExportPlugin.PARAM_FIELDS, "uuid,title,level");
    params.put(SearchExportPlugin.PARAM_FILENAME, "myexport");
    plugin.setParameterValues(params);

    assertEquals("myexport", plugin.getExportFilename());
    assertEquals(Arrays.asList("uuid", "title", "level"), plugin.getExportFields());
  }

  @Test
  public void testParameterParsingDefaultFilename() throws Exception {
    SearchExportPlugin plugin = new SearchExportPlugin();
    Map<String, String> params = new HashMap<>();
    params.put(SearchExportPlugin.PARAM_FILTER, "{\"filterParameters\":[]}");
    params.put(SearchExportPlugin.PARAM_FIELDS, "uuid,title");
    plugin.setParameterValues(params);

    assertTrue(plugin.getExportFilename().startsWith("export_"));
  }

  @Test
  public void testBuildCsvRow() {
    IndexedAIP aip = new IndexedAIP();
    aip.setId("aip-123");
    aip.setTitle("Testarkiv");

    List<String> row = SearchExportPlugin.buildCsvRow(aip, Arrays.asList("uuid", "title", "level"));

    assertEquals("aip-123", row.get(0));
    assertEquals("Testarkiv", row.get(1));
    assertEquals("", row.get(2)); // level är null → tom sträng
  }

  @Test
  public void testBuildCsvRowJob() {
    IndexedJob job = new IndexedJob();
    job.setId("job-456");
    job.setName("Ingest job");
    job.setUsername("admin");
    job.setState(Job.JOB_STATE.COMPLETED);
    job.setPluginType(PluginType.INGEST);

    List<String> row = SearchExportPlugin.buildCsvRowJob(job,
      Arrays.asList("id", "name", "username", "state", "pluginType", "unknown"));

    assertEquals("job-456", row.get(0));
    assertEquals("Ingest job", row.get(1));
    assertEquals("admin", row.get(2));
    assertEquals("Klar", row.get(3));       // COMPLETED → Klar
    assertEquals("Inleverans", row.get(4)); // INGEST → Inleverans
    assertEquals("", row.get(5));           // okänt fält → tomt
  }

  @Test
  public void testBuildCsvRowReport() {
    IndexedReport report = new IndexedReport();
    report.setId("report-789");
    report.setJobId("job-456");
    report.setJobName("Ingest job");
    report.setSourceObjectId("sip-001");
    report.setPluginState(PluginState.SUCCESS);

    List<String> row = SearchExportPlugin.buildCsvRowReport(report,
      Arrays.asList("id", "jobId", "jobName", "sourceObjectId", "pluginState", "unknown"));

    assertEquals("report-789", row.get(0));
    assertEquals("job-456", row.get(1));
    assertEquals("Ingest job", row.get(2));
    assertEquals("sip-001", row.get(3));
    assertEquals("Framgång", row.get(4)); // SUCCESS → Framgång
    assertEquals("", row.get(5));         // okänt fält → tomt
  }

  @Test
  public void testBuildCsvRowLogEntry() {
    LogEntry entry = new LogEntry();
    entry.setUUID("log-111");
    entry.setUsername("user1");
    entry.setActionComponent("org.roda.wui.api.SearchResource");
    entry.setActionMethod("list");
    entry.setAddress("127.0.0.1");
    entry.setRelatedObjectID("aip-123");
    entry.setDuration(42L);
    entry.setState(LogEntryState.SUCCESS);

    List<String> row = SearchExportPlugin.buildCsvRowLogEntry(entry,
      Arrays.asList("uuid", "username", "actionComponent", "actionMethod", "address",
        "relatedObjectID", "duration", "state", "unknown"));

    assertEquals("log-111", row.get(0));
    assertEquals("user1", row.get(1));
    assertEquals("org.roda.wui.api.SearchResource", row.get(2));
    assertEquals("list", row.get(3));
    assertEquals("127.0.0.1", row.get(4));
    assertEquals("aip-123", row.get(5));
    assertEquals("42", row.get(6));
    assertEquals("Framgång", row.get(7)); // SUCCESS → Framgång
    assertEquals("", row.get(8));         // okänt fält → tomt
  }

  @Test
  public void testBuildCsvRowMemberUser() {
    User user = new User();
    user.setId("anna");
    user.setName("anna");
    user.setFullName("Anna Andersson");
    user.setActive(true);
    user.setEmail("anna@example.org");
    user.setGroups(new HashSet<>(Arrays.asList("registratorer", "arkivarier")));
    user.setDirectRoles(new HashSet<>(Arrays.asList("aip.read", "aip.create")));
    user.setAllRoles(new HashSet<>(Arrays.asList("aip.update", "aip.read", "aip.create")));

    List<String> row = SearchExportPlugin.buildCsvRowMember(user, Arrays.asList("id", "fullName", "active",
      "email", "groups", "users", "directRoles", "allRoles", "unknown"));

    assertEquals("anna", row.get(0));
    assertEquals("Anna Andersson", row.get(1));
    assertEquals("Ja", row.get(2));
    assertEquals("anna@example.org", row.get(3));
    assertEquals("arkivarier; registratorer", row.get(4));      // sorterade, semikolonseparerade
    assertEquals("", row.get(5));                                // users gäller bara grupper
    assertEquals("aip.create; aip.read", row.get(6));            // direkta roller sorterade
    assertEquals("aip.create; aip.read; aip.update", row.get(7)); // alla roller sorterade
    assertEquals("", row.get(8));                                // okänt fält → tomt
  }

  @Test
  public void testBuildCsvRowMemberGroup() {
    Group group = new Group("arkivarier");
    group.setUsers(new HashSet<>(Arrays.asList("lars", "anna")));
    group.setAllRoles(new HashSet<>(Arrays.asList("aip.read")));

    List<String> row = SearchExportPlugin.buildCsvRowMember(group,
      Arrays.asList("name", "email", "groups", "users", "allRoles"));

    assertEquals("arkivarier", row.get(0));
    assertEquals("", row.get(1));           // email gäller bara användare
    assertEquals("", row.get(2));           // groups gäller bara användare
    assertEquals("anna; lars", row.get(3)); // medlemmar sorterade
    assertEquals("aip.read", row.get(4));
  }

  @Test
  public void testBuildCsvRowAipReadPermissions() {
    IndexedAIP aip = new IndexedAIP();
    aip.setId("aip-123");
    Permissions permissions = new Permissions();
    permissions.setGroupPermissions("registratorer", new HashSet<>(Arrays.asList(PermissionType.READ)));
    permissions.setGroupPermissions("arkivarier",
      new HashSet<>(Arrays.asList(PermissionType.READ, PermissionType.UPDATE)));
    permissions.setGroupPermissions("administrators", new HashSet<>(Arrays.asList(PermissionType.GRANT)));
    permissions.setUserPermissions("anna", new HashSet<>(Arrays.asList(PermissionType.READ)));
    aip.setPermissions(permissions);

    List<String> row = SearchExportPlugin.buildCsvRow(aip, Arrays.asList("uuid", "readGroups", "readUsers"));

    assertEquals("aip-123", row.get(0));
    assertEquals("arkivarier; registratorer", row.get(1)); // bara READ, sorterade
    assertEquals("anna", row.get(2));
  }

  @Test
  public void testBuildCsvRowAipWithoutPermissions() {
    IndexedAIP aip = new IndexedAIP();
    aip.setId("aip-123");

    List<String> row = SearchExportPlugin.buildCsvRow(aip, Arrays.asList("readGroups", "readUsers"));

    assertEquals("", row.get(0)); // permissions saknas → tomt
    assertEquals("", row.get(1));
  }

  @Test
  public void testSolrFieldsToReturnForMembers() {
    List<String> fields = SearchExportPlugin.getSolrFieldsToReturn("org.roda.core.data.v2.user.RODAMember",
      Arrays.asList("id", "active", "email", "directRoles", "allRoles"));

    // exportfält → Solr-fält, plus isUser som behövs för att skilja användare från grupp
    assertEquals(Arrays.asList("id", "isActive", "email", "rolesDirect", "rolesAll", "isUser"), fields);
  }

  @Test
  public void testSolrFieldsToReturnForAip() {
    List<String> fields = SearchExportPlugin.getSolrFieldsToReturn("org.roda.core.data.v2.ip.IndexedAIP",
      Arrays.asList("uuid", "title", "readGroups", "readUsers"));

    assertEquals(Arrays.asList("uuid", "title", "permission_groups_READ", "permission_users_READ"), fields);
  }

  @Test
  public void testSolrFieldsToReturnForUnknownClassIsTreatedAsAip() {
    // speglar default-grenen i afterAllExecute(): okänd klass exporteras som AIP
    List<String> fields = SearchExportPlugin.getSolrFieldsToReturn("org.example.Unknown",
      Arrays.asList("readGroups", "readUsers"));

    assertEquals(Arrays.asList("permission_groups_READ", "permission_users_READ"), fields);
  }

  @Test
  public void testSolrFieldsToReturnForNullClassIsTreatedAsAip() {
    List<String> fields = SearchExportPlugin.getSolrFieldsToReturn(null, Arrays.asList("readGroups", "readUsers"));

    assertEquals(Arrays.asList("permission_groups_READ", "permission_users_READ"), fields);
  }

  @Test
  public void testSolrFieldsToReturnForOtherClassIsNotMapped() {
    // IndexedDIP har egen export-gren och ska varken få AIP- eller medlemsmappningen
    List<String> fields = SearchExportPlugin.getSolrFieldsToReturn("org.roda.core.data.v2.ip.IndexedDIP",
      Arrays.asList("id", "readGroups", "allRoles"));

    assertEquals(Arrays.asList("id", "readGroups", "allRoles"), fields);
  }

  @Test
  public void testParamClassParsing() throws Exception {
    SearchExportPlugin plugin = new SearchExportPlugin();
    Map<String, String> params = new HashMap<>();
    params.put(SearchExportPlugin.PARAM_FILTER, "{\"filterParameters\":[]}");
    params.put(SearchExportPlugin.PARAM_FIELDS, "id,name");
    params.put(SearchExportPlugin.PARAM_CLASS, "org.roda.core.data.v2.jobs.IndexedJob");
    plugin.setParameterValues(params);

    assertTrue(plugin.areParameterValuesValid());
  }

  @Test
  public void testPluginTypeIsInternal() {
    SearchExportPlugin plugin = new SearchExportPlugin();
    assertEquals(PluginType.INTERNAL, plugin.getType());
  }
}

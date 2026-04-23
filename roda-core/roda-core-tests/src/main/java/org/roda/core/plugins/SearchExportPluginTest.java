package org.roda.core.plugins;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.v2.ip.IndexedAIP;
import org.roda.core.data.v2.jobs.IndexedJob;
import org.roda.core.data.v2.jobs.IndexedReport;
import org.roda.core.data.v2.jobs.Job;
import org.roda.core.data.v2.jobs.PluginState;
import org.roda.core.data.v2.jobs.PluginType;
import org.roda.core.data.v2.log.LogEntry;
import org.roda.core.data.v2.log.LogEntryState;
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
    assertEquals("COMPLETED", row.get(3));
    assertEquals("INGEST", row.get(4));
    assertEquals("", row.get(5)); // unknown field → empty
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
    assertEquals("SUCCESS", row.get(4));
    assertEquals("", row.get(5)); // unknown field → empty
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
    assertEquals("SUCCESS", row.get(7));
    assertEquals("", row.get(8)); // unknown field → empty
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

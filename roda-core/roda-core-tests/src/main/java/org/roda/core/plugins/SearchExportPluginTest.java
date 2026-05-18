/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.plugins;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.exceptions.GenericException;
import org.roda.core.data.exceptions.NotFoundException;
import org.roda.core.data.exceptions.RequestNotValidException;
import org.roda.core.data.v2.ip.IndexedAIP;
import org.roda.core.data.v2.jobs.IndexedJob;
import org.roda.core.data.v2.jobs.IndexedReport;
import org.roda.core.data.v2.jobs.Job;
import org.roda.core.data.v2.jobs.PluginState;
import org.roda.core.data.v2.jobs.PluginType;
import org.roda.core.data.v2.log.LogEntry;
import org.roda.core.data.v2.log.LogEntryState;
import org.roda.core.index.IndexService;
import org.roda.core.plugins.base.preservation.SearchExportPlugin;
import org.testng.annotations.Test;

@Test(groups = {RodaConstants.TEST_GROUP_ALL, RodaConstants.TEST_GROUP_DEV})
public class SearchExportPluginTest {

  // ── Parameter parsing ────────────────────────────────────────────────────

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

  // ── AIP rows ─────────────────────────────────────────────────────────────

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
  public void testBuildCsvRowLevelTranslations() {
    String[][] cases = {
      {"fonds", "Arkivbestånd"},
      {"subfonds", "Delarkiv"},
      {"series", "Serie"},
      {"subseries", "Underserie"},
      {"file", "Akt"},
      {"item", "Handling"},
      {"class", "Klass"},
      {"collection", "Samling"},
      {"recordgrp", "Volym"},
      {"subgrp", "Dossié"},
    };
    for (String[] c : cases) {
      IndexedAIP aip = new IndexedAIP();
      aip.setLevel(c[0]);
      List<String> row = SearchExportPlugin.buildCsvRow(aip, Arrays.asList("level"));
      assertEquals("Level '" + c[0] + "' borde översättas till '" + c[1] + "'", c[1], row.get(0));
    }
  }

  @Test
  public void testBuildCsvRowUnknownLevelPassthrough() {
    IndexedAIP aip = new IndexedAIP();
    aip.setLevel("okänd-nivå");
    List<String> row = SearchExportPlugin.buildCsvRow(aip, Arrays.asList("level"));
    assertEquals("okänd-nivå", row.get(0));
  }

  @Test
  public void testBuildCsvRowDateFormatting() {
    IndexedAIP aip = new IndexedAIP();
    // 2024-03-15 12:30 UTC
    aip.setDateInitial(Date.from(Instant.parse("2024-03-15T00:00:00Z")));
    aip.setDateFinal(Date.from(Instant.parse("2024-12-31T00:00:00Z")));

    List<String> row = SearchExportPlugin.buildCsvRow(aip, Arrays.asList("dateInitial", "dateFinal"));

    assertEquals("2024-03-15", row.get(0));
    assertEquals("2024-12-31", row.get(1));
  }

  @Test
  public void testBuildCsvRowDateTimeFormatting() {
    IndexedAIP aip = new IndexedAIP();
    aip.setCreatedOn(Date.from(Instant.parse("2024-06-01T08:30:00Z")));
    aip.setUpdatedOn(Date.from(Instant.parse("2024-06-15T14:05:00Z")));

    List<String> row = SearchExportPlugin.buildCsvRow(aip, Arrays.asList("createdOn", "updatedOn"));

    assertEquals("2024-06-01 08:30", row.get(0));
    assertEquals("2024-06-15 14:05", row.get(1));
  }

  @Test
  public void testBuildCsvRowNullDateReturnsEmpty() {
    IndexedAIP aip = new IndexedAIP();
    List<String> row = SearchExportPlugin.buildCsvRow(aip,
      Arrays.asList("dateInitial", "dateFinal", "createdOn", "updatedOn"));

    for (int i = 0; i < row.size(); i++) {
      assertEquals("Null datum borde ge tom sträng på index " + i, "", row.get(i));
    }
  }

  @Test
  public void testBuildCsvRowParentIdResolution()
    throws GenericException, NotFoundException, RequestNotValidException {
    IndexedAIP aip = new IndexedAIP();
    aip.setParentID("parent-uuid-001");

    IndexedAIP parent = new IndexedAIP();
    parent.setId("parent-uuid-001");
    parent.setTitle("Moderarkiv");

    IndexService mockIndex = mock(IndexService.class);
    when(mockIndex.retrieve(eq(IndexedAIP.class), eq("parent-uuid-001"), anyList()))
      .thenReturn(parent);

    List<String> row = SearchExportPlugin.buildCsvRow(aip, Arrays.asList("parentId"), mockIndex,
      new HashMap<>());

    assertEquals("Moderarkiv", row.get(0));
  }

  @Test
  public void testBuildCsvRowParentIdFallsBackToUuidWhenNotFound()
    throws GenericException, NotFoundException, RequestNotValidException {
    IndexedAIP aip = new IndexedAIP();
    aip.setParentID("missing-parent-uuid");

    IndexService mockIndex = mock(IndexService.class);
    when(mockIndex.retrieve(eq(IndexedAIP.class), eq("missing-parent-uuid"), anyList()))
      .thenThrow(new NotFoundException("not found"));

    List<String> row = SearchExportPlugin.buildCsvRow(aip, Arrays.asList("parentId"), mockIndex,
      new HashMap<>());

    assertEquals("missing-parent-uuid", row.get(0));
  }

  @Test
  public void testBuildCsvRowParentIdEmptyWhenNoParent() {
    IndexedAIP aip = new IndexedAIP();
    // parentId ej satt
    List<String> row = SearchExportPlugin.buildCsvRow(aip, Arrays.asList("parentId"));
    assertEquals("", row.get(0));
  }

  @Test
  public void testBuildCsvRowParentTitleCache() throws Exception {
    IndexedAIP aip1 = new IndexedAIP();
    aip1.setParentID("shared-parent");
    IndexedAIP aip2 = new IndexedAIP();
    aip2.setParentID("shared-parent");

    IndexedAIP parent = new IndexedAIP();
    parent.setId("shared-parent");
    parent.setTitle("Delad förälder");

    IndexService mockIndex = mock(IndexService.class);
    when(mockIndex.retrieve(eq(IndexedAIP.class), eq("shared-parent"), anyList()))
      .thenReturn(parent);

    Map<String, String> cache = new HashMap<>();
    SearchExportPlugin.buildCsvRow(aip1, Arrays.asList("parentId"), mockIndex, cache);
    SearchExportPlugin.buildCsvRow(aip2, Arrays.asList("parentId"), mockIndex, cache);

    // retrieve borde bara anropas en gång tack vare cachen
    org.mockito.Mockito.verify(mockIndex, org.mockito.Mockito.times(1))
      .retrieve(eq(IndexedAIP.class), eq("shared-parent"), anyList());
  }

  // ── Job rows ─────────────────────────────────────────────────────────────

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
  public void testBuildCsvRowJobStateTranslations() {
    Object[][] cases = {
      {Job.JOB_STATE.COMPLETED, "Klar"},
      {Job.JOB_STATE.CREATED, "Väntar på att starta"},
      {Job.JOB_STATE.FAILED_DURING_CREATION, "Misslyckades att starta"},
      {Job.JOB_STATE.PENDING_APPROVAL, "Avvaktar"},
      {Job.JOB_STATE.SCHEDULED, "Schemalagd"},
      {Job.JOB_STATE.REJECTED, "Avvisad"},
      {Job.JOB_STATE.FAILED_TO_COMPLETE, "Misslyckades"},
      {Job.JOB_STATE.STARTED, "Körs"},
      {Job.JOB_STATE.STOPPED, "Stoppad"},
      {Job.JOB_STATE.STOPPING, "Stoppar"},
    };
    for (Object[] c : cases) {
      IndexedJob job = new IndexedJob();
      job.setState((Job.JOB_STATE) c[0]);
      List<String> row = SearchExportPlugin.buildCsvRowJob(job, Arrays.asList("state"));
      assertEquals("State " + c[0] + " borde översättas", c[1], row.get(0));
    }
  }

  @Test
  public void testBuildCsvRowJobPluginTypeTranslations() {
    Object[][] cases = {
      {PluginType.AIP_TO_AIP, "AIP till AIP"},
      {PluginType.AIP_TO_SIP, "AIP till SIP"},
      {PluginType.INGEST, "Inleverans"},
      {PluginType.INTERNAL, "Intern"},
      {PluginType.MISC, "Diverse"},
      {PluginType.SIP_TO_AIP, "SIP till AIP"},
      {PluginType.MULTI, "Flera"},
    };
    for (Object[] c : cases) {
      IndexedJob job = new IndexedJob();
      job.setPluginType((PluginType) c[0]);
      List<String> row = SearchExportPlugin.buildCsvRowJob(job, Arrays.asList("pluginType"));
      assertEquals("PluginType " + c[0] + " borde översättas", c[1], row.get(0));
    }
  }

  @Test
  public void testBuildCsvRowJobDateTimeFormatting() {
    IndexedJob job = new IndexedJob();
    job.setStartDate(Date.from(Instant.parse("2024-01-10T09:00:00Z")));
    job.setEndDate(Date.from(Instant.parse("2024-01-10T09:15:30Z")));

    List<String> row = SearchExportPlugin.buildCsvRowJob(job, Arrays.asList("startDate", "endDate"));

    assertEquals("2024-01-10 09:00", row.get(0));
    assertEquals("2024-01-10 09:15", row.get(1));
  }

  // ── Report rows ──────────────────────────────────────────────────────────

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
  public void testBuildCsvRowReportPluginStateTranslations() {
    Object[][] cases = {
      {PluginState.SUCCESS, "Framgång"},
      {PluginState.FAILURE, "Misslyckad"},
      {PluginState.PARTIAL_SUCCESS, "Delvis lyckat"},
      {PluginState.RUNNING, "Körs"},
      {PluginState.SKIPPED, "Överhoppad"},
    };
    for (Object[] c : cases) {
      IndexedReport report = new IndexedReport();
      report.setPluginState((PluginState) c[0]);
      List<String> row = SearchExportPlugin.buildCsvRowReport(report, Arrays.asList("pluginState"));
      assertEquals("PluginState " + c[0] + " borde översättas", c[1], row.get(0));
    }
  }

  // ── Log entry rows ───────────────────────────────────────────────────────

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
  public void testBuildCsvRowLogEntryDateTimeFormatting() {
    LogEntry entry = new LogEntry();
    entry.setDatetime(Date.from(Instant.parse("2024-05-20T13:45:00Z")));

    List<String> row = SearchExportPlugin.buildCsvRowLogEntry(entry, Arrays.asList("datetime"));

    assertEquals("2024-05-20 13:45", row.get(0));
  }

  @Test
  public void testBuildCsvRowLogEntryStateTranslations() {
    Object[][] cases = {
      {LogEntryState.SUCCESS, "Framgång"},
      {LogEntryState.FAILURE, "Misslyckad"},
      // UNKNOWN och UNAUTHORIZED finns ej i PLUGIN_STATE_LABELS → passthrough
      {LogEntryState.UNKNOWN, "UNKNOWN"},
      {LogEntryState.UNAUTHORIZED, "UNAUTHORIZED"},
    };
    for (Object[] c : cases) {
      LogEntry entry = new LogEntry();
      entry.setState((LogEntryState) c[0]);
      List<String> row = SearchExportPlugin.buildCsvRowLogEntry(entry, Arrays.asList("state"));
      assertEquals("LogEntryState " + c[0] + " borde ge '" + c[1] + "'", c[1], row.get(0));
    }
  }
}

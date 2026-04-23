/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/keeps/roda
 */
package org.roda.core.plugins.base.preservation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.roda.core.RodaCoreFactory;
import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.common.RodaConstants.PreservationEventType;
import org.roda.core.data.exceptions.AuthorizationDeniedException;
import org.roda.core.data.exceptions.GenericException;
import org.roda.core.data.exceptions.InvalidParameterException;
import org.roda.core.data.exceptions.NotFoundException;
import org.roda.core.data.exceptions.RequestNotValidException;
import org.roda.core.data.v2.LiteOptionalWithCause;
import org.roda.core.data.v2.Void;
import org.roda.core.data.v2.index.filter.Filter;
import org.roda.core.data.v2.ip.IndexedAIP;
import org.roda.core.data.v2.jobs.IndexedJob;
import org.roda.core.data.v2.jobs.IndexedReport;
import org.roda.core.data.v2.log.LogEntry;
import org.roda.core.data.v2.jobs.Job;
import org.roda.core.data.v2.jobs.PluginParameter;
import org.roda.core.data.v2.jobs.PluginParameter.PluginParameterType;
import org.roda.core.data.v2.jobs.PluginType;
import org.roda.core.data.v2.jobs.Report;
import org.roda.core.data.v2.user.User;
import org.roda.core.index.IndexService;
import org.roda.core.index.utils.IterableIndexResult;
import org.roda.core.model.ModelService;
import org.roda.core.plugins.AbstractPlugin;
import org.roda.core.plugins.Plugin;
import org.roda.core.plugins.PluginException;
import org.roda.core.plugins.PluginHelper;
import org.roda.core.plugins.orchestrate.JobsHelper;
import org.roda.core.data.utils.JsonUtils;
import org.roda.core.util.IdUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchExportPlugin extends AbstractPlugin<Void> {
  private static final Logger LOGGER = LoggerFactory.getLogger(SearchExportPlugin.class);

  public static final String PARAM_FILTER = "exportFilter";
  public static final String PARAM_FIELDS = "exportFields";
  public static final String PARAM_FILENAME = "exportFilename";
  public static final String PARAM_CLASS = "exportClass";

  private static final String EXPORT_TEMP_FOLDER = "SearchExportCSV";

  private String filterJson = "{\"filterParameters\":[]}";
  private List<String> exportFields = Arrays.asList("uuid", "title", "level", "dateInitial", "dateFinal");
  private String exportFilename = "export_" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
  private String exportClass = "org.roda.core.data.v2.ip.IndexedAIP";

  private static final Map<String, PluginParameter> PLUGIN_PARAMETERS = new LinkedHashMap<>();

  static {
    PLUGIN_PARAMETERS.put(PARAM_FILTER,
      PluginParameter.getBuilder(PARAM_FILTER, "Search filter (JSON)", PluginParameterType.STRING)
        .withDefaultValue("{\"filterParameters\":[]}").build());
    PLUGIN_PARAMETERS.put(PARAM_FIELDS,
      PluginParameter.getBuilder(PARAM_FIELDS, "Fields to export", PluginParameterType.STRING)
        .withDefaultValue("uuid,title,level,dateInitial,dateFinal").build());
    PLUGIN_PARAMETERS.put(PARAM_FILENAME,
      PluginParameter.getBuilder(PARAM_FILENAME, "Export filename (without extension)", PluginParameterType.STRING)
        .withDefaultValue("export").build());
    PLUGIN_PARAMETERS.put(PARAM_CLASS,
      PluginParameter.getBuilder(PARAM_CLASS, "Class to export", PluginParameterType.STRING)
        .withDefaultValue("org.roda.core.data.v2.ip.IndexedAIP").build());
  }

  @Override
  public void init() throws PluginException {}

  @Override
  public void shutdown() {}

  @Override
  public String getName() {
    return "Search Export";
  }

  @Override
  public String getDescription() {
    return "Exports AIP search results to a CSV file. The result is available as a job attachment in Internal Actions.";
  }

  @Override
  public String getVersionImpl() {
    return "1.0";
  }

  @Override
  public List<PluginParameter> getParameters() {
    return new ArrayList<>(PLUGIN_PARAMETERS.values());
  }

  @Override
  public void setParameterValues(Map<String, String> parameters) throws InvalidParameterException {
    super.setParameterValues(parameters);
    if (parameters.containsKey(PARAM_FILTER) && parameters.get(PARAM_FILTER) != null) {
      filterJson = parameters.get(PARAM_FILTER);
    }
    if (parameters.containsKey(PARAM_FIELDS) && parameters.get(PARAM_FIELDS) != null) {
      String fieldsStr = parameters.get(PARAM_FIELDS).trim();
      if (!fieldsStr.isEmpty()) {
        exportFields = Arrays.asList(fieldsStr.split(","));
      }
    }
    if (parameters.containsKey(PARAM_FILENAME) && parameters.get(PARAM_FILENAME) != null) {
      String fn = parameters.get(PARAM_FILENAME).trim();
      exportFilename = fn.isEmpty()
        ? "export_" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        : fn;
    }
    if (parameters.containsKey(PARAM_CLASS) && parameters.get(PARAM_CLASS) != null) {
      String cls = parameters.get(PARAM_CLASS).trim();
      if (!cls.isEmpty()) {
        exportClass = cls;
      }
    }
  }

  public String getExportFilename() {
    return exportFilename;
  }

  public List<String> getExportFields() {
    return Collections.unmodifiableList(exportFields);
  }

  @Override
  public Report execute(IndexService index, ModelService model, List<LiteOptionalWithCause> liteList)
    throws PluginException {
    return new Report();
  }

  @Override
  public Report beforeAllExecute(IndexService index, ModelService model) throws PluginException {
    return new Report();
  }

  @Override
  public Report afterAllExecute(IndexService index, ModelService model) throws PluginException {
    String jobId = PluginHelper.getJobId(this);
    Path tempDir = RodaCoreFactory.getWorkingDirectory().resolve(EXPORT_TEMP_FOLDER).resolve(jobId);
    Path csvFile = tempDir.resolve(IdUtils.createUUID() + ".csv");

    try {
      Files.createDirectories(tempDir);
    } catch (IOException e) {
      throw new PluginException("Could not create temp directory for export", e);
    }

    Filter filter;
    try {
      filter = JsonUtils.getObjectFromJson(filterJson, Filter.class);
    } catch (Exception e) {
      throw new PluginException("Could not deserialize export filter", e);
    }

    User user = null;
    try {
      Job job = PluginHelper.getJob(this, model);
      user = model.retrieveUser(job.getUsername());
    } catch (Exception e) {
      LOGGER.warn("Could not retrieve job user — export will run without user permissions filter", e);
    }

    String csvDelimiter = RodaCoreFactory.getRodaConfiguration().getString("csv.delimiter");
    if (csvDelimiter == null || csvDelimiter.isBlank()) {
      csvDelimiter = String.valueOf(CSVFormat.DEFAULT.getDelimiter());
    }

    CSVFormat csvFormat = CSVFormat.DEFAULT.withDelimiter(csvDelimiter.charAt(0)).withRecordSeparator("\n");

    try (BufferedWriter writer = Files.newBufferedWriter(csvFile);
      CSVPrinter printer = new CSVPrinter(writer, csvFormat)) {

      printer.printRecord(exportFields);

      switch (exportClass) {
        case "org.roda.core.data.v2.jobs.IndexedJob":
          try (IterableIndexResult<IndexedJob> results = index.findAll(IndexedJob.class, filter, user, true,
            exportFields)) {
            for (IndexedJob job : results) {
              printer.printRecord(buildCsvRowJob(job, exportFields));
            }
          }
          break;
        case "org.roda.core.data.v2.jobs.IndexedReport":
          try (IterableIndexResult<IndexedReport> results = index.findAll(IndexedReport.class, filter, user, true,
            exportFields)) {
            for (IndexedReport report : results) {
              printer.printRecord(buildCsvRowReport(report, exportFields));
            }
          }
          break;
        case "org.roda.core.data.v2.log.LogEntry":
          try (IterableIndexResult<LogEntry> results = index.findAll(LogEntry.class, filter, user, true,
            exportFields)) {
            for (LogEntry entry : results) {
              printer.printRecord(buildCsvRowLogEntry(entry, exportFields));
            }
          }
          break;
        default:
          try (IterableIndexResult<IndexedAIP> results = index.findAll(IndexedAIP.class, filter, user, true,
            exportFields)) {
            for (IndexedAIP aip : results) {
              printer.printRecord(buildCsvRow(aip, exportFields));
            }
          }
          break;
      }

    } catch (IOException | GenericException | RequestNotValidException e) {
      LOGGER.error("Error writing export CSV for job {}", jobId, e);
      throw new PluginException("Error writing export CSV", e);
    }

    String safeFilename = exportFilename.replaceAll("[/\\\\:*?\"<>|]", "_");
    String attachmentName = safeFilename + ".csv";
    Path namedFile = tempDir.resolve(attachmentName);
    try {
      Files.move(csvFile, namedFile);
      JobsHelper.createJobAttachment(jobId, namedFile);
    } catch (IOException | AuthorizationDeniedException | GenericException | NotFoundException
      | RequestNotValidException e) {
      LOGGER.error("Error creating job attachment for export job {}", jobId, e);
      throw new PluginException("Error creating job attachment for export", e);
    } finally {
      try {
        Files.deleteIfExists(namedFile);
        Files.deleteIfExists(csvFile);
        Files.deleteIfExists(tempDir);
      } catch (IOException e) {
        LOGGER.warn("Could not clean up temp files for export job {}", jobId, e);
      }
    }

    return new Report();
  }

  public static List<String> buildCsvRow(IndexedAIP aip, List<String> fields) {
    List<String> row = new ArrayList<>();
    for (String field : fields) {
      row.add(getFieldValue(aip, field));
    }
    return row;
  }

  private static String getFieldValue(IndexedAIP aip, String field) {
    if (field == null) return "";
    switch (field.trim()) {
      case "uuid":
        return nullToEmpty(aip.getId());
      case "title":
        return nullToEmpty(aip.getTitle());
      case "level":
        return nullToEmpty(aip.getLevel());
      case "dateInitial":
        return aip.getDateInitial() != null ? aip.getDateInitial().toString() : "";
      case "dateFinal":
        return aip.getDateFinal() != null ? aip.getDateFinal().toString() : "";
      case "parentId":
        return nullToEmpty(aip.getParentID());
      case "ingestSIPIds":
        return aip.getIngestSIPIds() != null ? String.join(";", aip.getIngestSIPIds()) : "";
      case "createdOn":
        return aip.getCreatedOn() != null ? aip.getCreatedOn().toString() : "";
      case "updatedOn":
        return aip.getUpdatedOn() != null ? aip.getUpdatedOn().toString() : "";
      default:
        return "";
    }
  }

  public static List<String> buildCsvRowJob(IndexedJob job, List<String> fields) {
    List<String> row = new ArrayList<>();
    for (String field : fields) {
      row.add(getFieldValueJob(job, field));
    }
    return row;
  }

  private static String getFieldValueJob(IndexedJob job, String field) {
    if (field == null) return "";
    switch (field.trim()) {
      case "id": return nullToEmpty(job.getId());
      case "name": return nullToEmpty(job.getName());
      case "username": return nullToEmpty(job.getUsername());
      case "startDate": return job.getStartDate() != null ? job.getStartDate().toString() : "";
      case "endDate": return job.getEndDate() != null ? job.getEndDate().toString() : "";
      case "state": return job.getState() != null ? job.getState().toString() : "";
      case "priority": return job.getPriority() != null ? job.getPriority().toString() : "";
      case "pluginType": return job.getPluginType() != null ? job.getPluginType().toString() : "";
      case "plugin": return nullToEmpty(job.getPlugin());
      default: return "";
    }
  }

  public static List<String> buildCsvRowReport(IndexedReport report, List<String> fields) {
    List<String> row = new ArrayList<>();
    for (String field : fields) {
      row.add(getFieldValueReport(report, field));
    }
    return row;
  }

  private static String getFieldValueReport(IndexedReport report, String field) {
    if (field == null) return "";
    switch (field.trim()) {
      case "id": return nullToEmpty(report.getId());
      case "jobId": return nullToEmpty(report.getJobId());
      case "jobName": return nullToEmpty(report.getJobName());
      case "sourceObjectId": return nullToEmpty(report.getSourceObjectId());
      case "sourceObjectOriginalName": return nullToEmpty(report.getSourceObjectOriginalName());
      case "outcomeObjectId": return nullToEmpty(report.getOutcomeObjectId());
      case "pluginState": return report.getPluginState() != null ? report.getPluginState().toString() : "";
      case "dateCreated": return report.getDateCreated() != null ? report.getDateCreated().toString() : "";
      case "dateUpdated": return report.getDateUpdated() != null ? report.getDateUpdated().toString() : "";
      case "pluginDetails": return nullToEmpty(report.getPluginDetails());
      case "plugin": return nullToEmpty(report.getPlugin());
      case "pluginName": return nullToEmpty(report.getPluginName());
      default: return "";
    }
  }

  public static List<String> buildCsvRowLogEntry(LogEntry entry, List<String> fields) {
    List<String> row = new ArrayList<>();
    for (String field : fields) {
      row.add(getFieldValueLogEntry(entry, field));
    }
    return row;
  }

  private static String getFieldValueLogEntry(LogEntry entry, String field) {
    if (field == null) return "";
    switch (field.trim()) {
      case "uuid": return nullToEmpty(entry.getUUID());
      case "datetime": return entry.getDatetime() != null ? entry.getDatetime().toString() : "";
      case "username": return nullToEmpty(entry.getUsername());
      case "actionComponent": return nullToEmpty(entry.getActionComponent());
      case "actionMethod": return nullToEmpty(entry.getActionMethod());
      case "address": return nullToEmpty(entry.getAddress());
      case "relatedObjectID": return nullToEmpty(entry.getRelatedObjectID());
      case "duration": return String.valueOf(entry.getDuration());
      case "state": return entry.getState() != null ? entry.getState().toString() : "";
      default: return "";
    }
  }

  private static String nullToEmpty(String s) {
    return s != null ? s : "";
  }

  @Override
  public Plugin<Void> cloneMe() {
    return new SearchExportPlugin();
  }

  @Override
  public PluginType getType() {
    return PluginType.INTERNAL;
  }

  @Override
  public boolean areParameterValuesValid() {
    return filterJson != null && !filterJson.isBlank() && !exportFields.isEmpty();
  }

  @Override
  public PreservationEventType getPreservationEventType() {
    return PreservationEventType.NONE;
  }

  @Override
  public String getPreservationEventDescription() {
    return "";
  }

  @Override
  public String getPreservationEventSuccessMessage() {
    return "Export CSV created successfully";
  }

  @Override
  public String getPreservationEventFailureMessage() {
    return "Export CSV creation failed";
  }

  @Override
  public List<String> getCategories() {
    return Collections.singletonList(RodaConstants.PLUGIN_CATEGORY_MANAGEMENT);
  }

  @Override
  public List<Class<Void>> getObjectClasses() {
    return Collections.singletonList(Void.class);
  }
}

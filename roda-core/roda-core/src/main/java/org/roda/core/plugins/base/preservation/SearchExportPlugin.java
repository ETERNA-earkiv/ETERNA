/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.plugins.base.preservation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

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
import org.roda.core.data.v2.disposal.confirmation.DisposalConfirmation;
import org.roda.core.data.v2.ip.IndexedAIP;
import org.roda.core.data.v2.ip.IndexedDIP;
import org.roda.core.data.v2.ip.IndexedFile;
import org.roda.core.data.v2.ip.IndexedRepresentation;
import org.roda.core.data.v2.ip.TransferredResource;
import org.roda.core.data.v2.ip.metadata.IndexedPreservationAgent;
import org.roda.core.data.v2.ip.metadata.IndexedPreservationEvent;
import org.roda.core.data.v2.notifications.Notification;
import org.roda.core.data.v2.ri.RepresentationInformation;
import org.roda.core.data.v2.risks.IndexedRisk;
import org.roda.core.data.v2.risks.RiskIncidence;
import org.roda.core.data.v2.jobs.IndexedJob;
import org.roda.core.data.v2.jobs.IndexedReport;
import org.roda.core.data.v2.jobs.Job;
import org.roda.core.data.v2.jobs.PluginParameter;
import org.roda.core.data.v2.log.LogEntry;
import org.roda.core.data.v2.jobs.PluginParameter.PluginParameterType;
import org.roda.core.data.v2.jobs.PluginType;
import org.roda.core.data.v2.jobs.Report;
import org.roda.core.data.v2.user.RODAMember;
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

  private static final ZoneId UTC = ZoneId.of("UTC");
  private static final DateTimeFormatter DATE_FMT =
    DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(UTC);
  private static final DateTimeFormatter DATETIME_FMT =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(UTC);

  private static final Map<String, String> LEVEL_LABELS;
  private static final Map<String, String> PLUGIN_STATE_LABELS;
  private static final Map<String, String> JOB_STATE_LABELS;
  private static final Map<String, String> JOB_PLUGIN_TYPE_LABELS;

  static {
    LEVEL_LABELS = new LinkedHashMap<>();
    LEVEL_LABELS.put("fonds", "Arkivbestånd");
    LEVEL_LABELS.put("subfonds", "Delarkiv");
    LEVEL_LABELS.put("series", "Serie");
    LEVEL_LABELS.put("subseries", "Underserie");
    LEVEL_LABELS.put("file", "Akt");
    LEVEL_LABELS.put("item", "Handling");
    LEVEL_LABELS.put("class", "Klass");
    LEVEL_LABELS.put("collection", "Samling");
    LEVEL_LABELS.put("recordgrp", "Volym");
    LEVEL_LABELS.put("subgrp", "Dossié");

    PLUGIN_STATE_LABELS = new LinkedHashMap<>();
    PLUGIN_STATE_LABELS.put("SUCCESS", "Framgång");
    PLUGIN_STATE_LABELS.put("FAILURE", "Misslyckad");
    PLUGIN_STATE_LABELS.put("PARTIAL_SUCCESS", "Delvis lyckat");
    PLUGIN_STATE_LABELS.put("RUNNING", "Körs");
    PLUGIN_STATE_LABELS.put("SKIPPED", "Överhoppad");

    JOB_STATE_LABELS = new LinkedHashMap<>();
    JOB_STATE_LABELS.put("COMPLETED", "Klar");
    JOB_STATE_LABELS.put("CREATED", "Väntar på att starta");
    JOB_STATE_LABELS.put("FAILED_DURING_CREATION", "Misslyckades att starta");
    JOB_STATE_LABELS.put("PENDING_APPROVAL", "Avvaktar");
    JOB_STATE_LABELS.put("SCHEDULED", "Schemalagd");
    JOB_STATE_LABELS.put("REJECTED", "Avvisad");
    JOB_STATE_LABELS.put("FAILED_TO_COMPLETE", "Misslyckades");
    JOB_STATE_LABELS.put("STARTED", "Körs");
    JOB_STATE_LABELS.put("STOPPED", "Stoppad");
    JOB_STATE_LABELS.put("STOPPING", "Stoppar");

    JOB_PLUGIN_TYPE_LABELS = new LinkedHashMap<>();
    JOB_PLUGIN_TYPE_LABELS.put("AIP_TO_AIP", "AIP till AIP");
    JOB_PLUGIN_TYPE_LABELS.put("AIP_TO_SIP", "AIP till SIP");
    JOB_PLUGIN_TYPE_LABELS.put("INGEST", "Inleverans");
    JOB_PLUGIN_TYPE_LABELS.put("INTERNAL", "Intern");
    JOB_PLUGIN_TYPE_LABELS.put("MISC", "Diverse");
    JOB_PLUGIN_TYPE_LABELS.put("SIP_TO_AIP", "SIP till AIP");
    JOB_PLUGIN_TYPE_LABELS.put("MULTI", "Flera");
  }

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
    return "Exports search results to a CSV file. Supports AIP, job, report, log entry, transferred resource, and member lists. The result is available as a job attachment in Internal Actions.";
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

    String configKeyPrefix = getConfigKeyPrefix(exportClass);
    List<String> headers = exportFields.stream()
      .map(f -> getFieldLabel(configKeyPrefix, f))
      .collect(Collectors.toList());

    try (OutputStream fileOut = Files.newOutputStream(csvFile);
      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fileOut, StandardCharsets.UTF_8));
      CSVPrinter printer = new CSVPrinter(writer, csvFormat)) {
      printer.printRecord(headers);

      Map<String, String> parentTitleCache = new HashMap<>();
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
        case "org.roda.core.data.v2.ip.TransferredResource":
          try (IterableIndexResult<TransferredResource> results = index.findAll(TransferredResource.class, filter,
            user, true, exportFields)) {
            for (TransferredResource resource : results) {
              printer.printRecord(buildCsvRowTransferredResource(resource, exportFields));
            }
          }
          break;
        case "org.roda.core.data.v2.user.RODAMember":
          try (IterableIndexResult<RODAMember> results = index.findAll(RODAMember.class, filter, user, true,
            exportFields)) {
            for (RODAMember member : results) {
              printer.printRecord(buildCsvRowMember(member, exportFields));
            }
          }
          break;
        case "org.roda.core.data.v2.ip.IndexedDIP":
          try (IterableIndexResult<IndexedDIP> results = index.findAll(IndexedDIP.class, filter, user, true,
            exportFields)) {
            for (IndexedDIP dip : results) {
              printer.printRecord(buildCsvRowDIP(dip, exportFields));
            }
          }
          break;
        case "org.roda.core.data.v2.risks.IndexedRisk":
          try (IterableIndexResult<IndexedRisk> results = index.findAll(IndexedRisk.class, filter, user, true,
            exportFields)) {
            for (IndexedRisk risk : results) {
              printer.printRecord(buildCsvRowRisk(risk, exportFields));
            }
          }
          break;
        case "org.roda.core.data.v2.risks.RiskIncidence":
          try (IterableIndexResult<RiskIncidence> results = index.findAll(RiskIncidence.class, filter, user, true,
            exportFields)) {
            for (RiskIncidence incidence : results) {
              printer.printRecord(buildCsvRowRiskIncidence(incidence, exportFields));
            }
          }
          break;
        case "org.roda.core.data.v2.ip.IndexedRepresentation":
          try (IterableIndexResult<IndexedRepresentation> results = index.findAll(IndexedRepresentation.class, filter,
            user, true, exportFields)) {
            for (IndexedRepresentation rep : results) {
              printer.printRecord(buildCsvRowRepresentation(rep, exportFields));
            }
          }
          break;
        case "org.roda.core.data.v2.ip.metadata.IndexedPreservationEvent":
          try (IterableIndexResult<IndexedPreservationEvent> results = index.findAll(IndexedPreservationEvent.class,
            filter, user, true, exportFields)) {
            for (IndexedPreservationEvent event : results) {
              printer.printRecord(buildCsvRowPreservationEvent(event, exportFields));
            }
          }
          break;
        case "org.roda.core.data.v2.ip.metadata.IndexedPreservationAgent":
          try (IterableIndexResult<IndexedPreservationAgent> results = index.findAll(IndexedPreservationAgent.class,
            filter, user, true, exportFields)) {
            for (IndexedPreservationAgent agent : results) {
              printer.printRecord(buildCsvRowPreservationAgent(agent, exportFields));
            }
          }
          break;
        case "org.roda.core.data.v2.notifications.Notification":
          try (IterableIndexResult<Notification> results = index.findAll(Notification.class, filter, user, true,
            exportFields)) {
            for (Notification notification : results) {
              printer.printRecord(buildCsvRowNotification(notification, exportFields));
            }
          }
          break;
        case "org.roda.core.data.v2.disposal.confirmation.DisposalConfirmation":
          try (IterableIndexResult<DisposalConfirmation> results = index.findAll(DisposalConfirmation.class, filter,
            user, true, exportFields)) {
            for (DisposalConfirmation confirmation : results) {
              printer.printRecord(buildCsvRowDisposalConfirmation(confirmation, exportFields));
            }
          }
          break;
        case "org.roda.core.data.v2.ip.IndexedFile":
          try (IterableIndexResult<IndexedFile> results = index.findAll(IndexedFile.class, filter, user, true,
            exportFields)) {
            for (IndexedFile file : results) {
              printer.printRecord(buildCsvRowFile(file, exportFields));
            }
          }
          break;
        case "org.roda.core.data.v2.ri.RepresentationInformation":
          try (IterableIndexResult<RepresentationInformation> results = index.findAll(RepresentationInformation.class,
            filter, user, true, exportFields)) {
            for (RepresentationInformation ri : results) {
              printer.printRecord(buildCsvRowRepresentationInformation(ri, exportFields));
            }
          }
          break;
        default:
          try (IterableIndexResult<IndexedAIP> results = index.findAll(IndexedAIP.class, filter, user, true,
            exportFields)) {
            for (IndexedAIP aip : results) {
              printer.printRecord(buildCsvRow(aip, exportFields, index, parentTitleCache));
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
    return buildCsvRow(aip, fields, null, new HashMap<>());
  }

  public static List<String> buildCsvRow(IndexedAIP aip, List<String> fields, IndexService index,
    Map<String, String> parentTitleCache) {
    List<String> row = new ArrayList<>();
    for (String field : fields) {
      row.add(getFieldValue(aip, field, index, parentTitleCache));
    }
    return row;
  }

  private static String getFieldValue(IndexedAIP aip, String field, IndexService index,
    Map<String, String> parentTitleCache) {
    if (field == null) return "";
    switch (field.trim()) {
      case "uuid":
        return nullToEmpty(aip.getId());
      case "title":
        return nullToEmpty(aip.getTitle());
      case "level":
        return LEVEL_LABELS.getOrDefault(nullToEmpty(aip.getLevel()), nullToEmpty(aip.getLevel()));
      case "dateInitial":
        return formatDate(aip.getDateInitial());
      case "dateFinal":
        return formatDate(aip.getDateFinal());
      case "parentId":
        return resolveParentTitle(nullToEmpty(aip.getParentID()), index, parentTitleCache);
      case "ingestSIPIds":
        return aip.getIngestSIPIds() != null ? String.join("; ", aip.getIngestSIPIds()) : "";
      case "createdOn":
        return formatDateTime(aip.getCreatedOn());
      case "updatedOn":
        return formatDateTime(aip.getUpdatedOn());
      default:
        return "";
    }
  }

  private static String resolveParentTitle(String parentId, IndexService index,
    Map<String, String> cache) {
    if (parentId.isEmpty()) return "";
    return cache.computeIfAbsent(parentId, id -> {
      try {
        IndexedAIP parent = index.retrieve(IndexedAIP.class, id, Arrays.asList("title", "id"));
        String t = parent.getTitle();
        return (t != null && !t.isEmpty()) ? t : id;
      } catch (Exception e) {
        return id;
      }
    });
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
      case "startDate": return formatDateTime(job.getStartDate());
      case "endDate": return formatDateTime(job.getEndDate());
      case "state": {
        String s = job.getState() != null ? job.getState().toString() : "";
        return JOB_STATE_LABELS.getOrDefault(s, s);
      }
      case "priority": return job.getPriority() != null ? job.getPriority().toString() : "";
      case "pluginType": {
        String t = job.getPluginType() != null ? job.getPluginType().toString() : "";
        return JOB_PLUGIN_TYPE_LABELS.getOrDefault(t, t);
      }
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
      case "pluginState": {
        String ps = report.getPluginState() != null ? report.getPluginState().toString() : "";
        return PLUGIN_STATE_LABELS.getOrDefault(ps, ps);
      }
      case "dateCreated": return formatDateTime(report.getDateCreated());
      case "dateUpdated": return formatDateTime(report.getDateUpdated());
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
      case "datetime": return formatDateTime(entry.getDatetime());
      case "username": return nullToEmpty(entry.getUsername());
      case "actionComponent": return nullToEmpty(entry.getActionComponent());
      case "actionMethod": return nullToEmpty(entry.getActionMethod());
      case "address": return nullToEmpty(entry.getAddress());
      case "relatedObjectID": return nullToEmpty(entry.getRelatedObjectID());
      case "duration": return String.valueOf(entry.getDuration());
      case "state": {
        String ls = entry.getState() != null ? entry.getState().toString() : "";
        return PLUGIN_STATE_LABELS.getOrDefault(ls, ls);
      }
      default: return "";
    }
  }

  public static List<String> buildCsvRowTransferredResource(TransferredResource resource, List<String> fields) {
    List<String> row = new ArrayList<>();
    for (String field : fields) {
      row.add(getFieldValueTransferredResource(resource, field));
    }
    return row;
  }

  private static String getFieldValueTransferredResource(TransferredResource resource, String field) {
    if (field == null) return "";
    switch (field.trim()) {
      case "uuid": return nullToEmpty(resource.getUUID());
      case "name": return nullToEmpty(resource.getName());
      case "fullPath": return nullToEmpty(resource.getFullPath());
      case "relativePath": return nullToEmpty(resource.getRelativePath());
      case "size": return String.valueOf(resource.getSize());
      case "creationDate": return formatDateTime(resource.getCreationDate());
      case "file": return resource.isFile() ? "Ja" : "Nej";
      default: return "";
    }
  }

  public static List<String> buildCsvRowMember(RODAMember member, List<String> fields) {
    List<String> row = new ArrayList<>();
    for (String field : fields) {
      row.add(getFieldValueMember(member, field));
    }
    return row;
  }

  private static String getFieldValueMember(RODAMember member, String field) {
    if (field == null) return "";
    switch (field.trim()) {
      case "id": return nullToEmpty(member.getId());
      case "name": return nullToEmpty(member.getName());
      case "fullName": return nullToEmpty(member.getFullName());
      case "active": return member.isActive() ? "Ja" : "Nej";
      case "directRoles":
        return member.getDirectRoles() != null ? String.join("; ", member.getDirectRoles()) : "";
      default: return "";
    }
  }

  public static List<String> buildCsvRowDIP(IndexedDIP dip, List<String> fields) {
    List<String> row = new ArrayList<>();
    for (String field : fields) {
      row.add(getFieldValueDIP(dip, field));
    }
    return row;
  }

  private static String getFieldValueDIP(IndexedDIP dip, String field) {
    if (field == null) return "";
    switch (field.trim()) {
      case "id": return nullToEmpty(dip.getId());
      case "title": return nullToEmpty(dip.getTitle());
      case "description": return nullToEmpty(dip.getDescription());
      case "type": return nullToEmpty(dip.getType());
      case "dateCreated": return formatDateTime(dip.getDateCreated());
      case "lastModified": return formatDateTime(dip.getLastModified());
      default: return "";
    }
  }

  public static List<String> buildCsvRowRisk(IndexedRisk risk, List<String> fields) {
    List<String> row = new ArrayList<>();
    for (String field : fields) {
      row.add(getFieldValueRisk(risk, field));
    }
    return row;
  }

  private static String getFieldValueRisk(IndexedRisk risk, String field) {
    if (field == null) return "";
    switch (field.trim()) {
      case "id": return nullToEmpty(risk.getId());
      case "name": return nullToEmpty(risk.getName());
      case "description": return nullToEmpty(risk.getDescription());
      case "identifiedOn": return formatDate(risk.getIdentifiedOn());
      case "identifiedBy": return nullToEmpty(risk.getIdentifiedBy());
      case "preMitigationSeverityLevel":
        return risk.getPreMitigationSeverityLevel() != null ? risk.getPreMitigationSeverityLevel().toString() : "";
      case "postMitigationSeverityLevel":
        return risk.getPostMitigationSeverityLevel() != null ? risk.getPostMitigationSeverityLevel().toString() : "";
      case "incidencesCount": return String.valueOf(risk.getIncidencesCount());
      case "unmitigatedIncidencesCount": return String.valueOf(risk.getUnmitigatedIncidencesCount());
      default: return "";
    }
  }

  public static List<String> buildCsvRowRiskIncidence(RiskIncidence incidence, List<String> fields) {
    List<String> row = new ArrayList<>();
    for (String field : fields) {
      row.add(getFieldValueRiskIncidence(incidence, field));
    }
    return row;
  }

  private static String getFieldValueRiskIncidence(RiskIncidence incidence, String field) {
    if (field == null) return "";
    switch (field.trim()) {
      case "id": return nullToEmpty(incidence.getId());
      case "riskId": return nullToEmpty(incidence.getRiskId());
      case "aipId": return nullToEmpty(incidence.getAipId());
      case "status": return incidence.getStatus() != null ? incidence.getStatus().toString() : "";
      case "severity": return incidence.getSeverity() != null ? incidence.getSeverity().toString() : "";
      case "detectedOn": return formatDateTime(incidence.getDetectedOn());
      case "detectedBy": return nullToEmpty(incidence.getDetectedBy());
      case "mitigatedOn": return formatDateTime(incidence.getMitigatedOn());
      case "mitigatedBy": return nullToEmpty(incidence.getMitigatedBy());
      default: return "";
    }
  }

  public static List<String> buildCsvRowRepresentation(IndexedRepresentation rep, List<String> fields) {
    List<String> row = new ArrayList<>();
    for (String field : fields) {
      row.add(getFieldValueRepresentation(rep, field));
    }
    return row;
  }

  private static String getFieldValueRepresentation(IndexedRepresentation rep, String field) {
    if (field == null) return "";
    switch (field.trim()) {
      case "uuid": return nullToEmpty(rep.getUUID());
      case "title": return nullToEmpty(rep.getTitle());
      case "sizeInBytes": return String.valueOf(rep.getSizeInBytes());
      case "numberOfDataFiles": return String.valueOf(rep.getNumberOfDataFiles());
      case "numberOfDataFolders": return String.valueOf(rep.getNumberOfDataFolders());
      default: return "";
    }
  }

  public static List<String> buildCsvRowPreservationEvent(IndexedPreservationEvent event, List<String> fields) {
    List<String> row = new ArrayList<>();
    for (String field : fields) {
      row.add(getFieldValuePreservationEvent(event, field));
    }
    return row;
  }

  private static String getFieldValuePreservationEvent(IndexedPreservationEvent event, String field) {
    if (field == null) return "";
    switch (field.trim()) {
      case "id": return nullToEmpty(event.getId());
      case "aipID": return nullToEmpty(event.getAipID());
      case "eventDateTime": return formatDateTime(event.getEventDateTime());
      case "eventType": return nullToEmpty(event.getEventType());
      case "eventOutcome": return nullToEmpty(event.getEventOutcome());
      case "eventDetail": return nullToEmpty(event.getEventDetail());
      default: return "";
    }
  }

  public static List<String> buildCsvRowPreservationAgent(IndexedPreservationAgent agent, List<String> fields) {
    List<String> row = new ArrayList<>();
    for (String field : fields) {
      row.add(getFieldValuePreservationAgent(agent, field));
    }
    return row;
  }

  private static String getFieldValuePreservationAgent(IndexedPreservationAgent agent, String field) {
    if (field == null) return "";
    switch (field.trim()) {
      case "id": return nullToEmpty(agent.getId());
      case "name": return nullToEmpty(agent.getName());
      case "type": return nullToEmpty(agent.getType());
      case "version": return nullToEmpty(agent.getVersion());
      case "createdOn": return formatDateTime(agent.getCreatedOn());
      default: return "";
    }
  }

  public static List<String> buildCsvRowNotification(Notification notification, List<String> fields) {
    List<String> row = new ArrayList<>();
    for (String field : fields) {
      row.add(getFieldValueNotification(notification, field));
    }
    return row;
  }

  private static String getFieldValueNotification(Notification notification, String field) {
    if (field == null) return "";
    switch (field.trim()) {
      case "id": return nullToEmpty(notification.getId());
      case "subject": return nullToEmpty(notification.getSubject());
      case "sentOn": return formatDateTime(notification.getSentOn());
      case "fromUser": return nullToEmpty(notification.getFromUser());
      case "recipientUsers":
        return notification.getRecipientUsers() != null ? String.join("; ", notification.getRecipientUsers()) : "";
      case "state": return notification.getState() != null ? notification.getState().toString() : "";
      default: return "";
    }
  }

  public static List<String> buildCsvRowDisposalConfirmation(DisposalConfirmation confirmation, List<String> fields) {
    List<String> row = new ArrayList<>();
    for (String field : fields) {
      row.add(getFieldValueDisposalConfirmation(confirmation, field));
    }
    return row;
  }

  private static String getFieldValueDisposalConfirmation(DisposalConfirmation confirmation, String field) {
    if (field == null) return "";
    switch (field.trim()) {
      case "id": return nullToEmpty(confirmation.getId());
      case "title": return nullToEmpty(confirmation.getTitle());
      case "state": return confirmation.getState() != null ? confirmation.getState().toString() : "";
      case "createdOn": return formatDateTime(confirmation.getCreatedOn());
      case "createdBy": return nullToEmpty(confirmation.getCreatedBy());
      case "executedOn": return formatDateTime(confirmation.getExecutedOn());
      case "size": return confirmation.getSize() != null ? String.valueOf(confirmation.getSize()) : "";
      case "numberOfAIPs": return confirmation.getNumberOfAIPs() != null ? String.valueOf(confirmation.getNumberOfAIPs()) : "";
      default: return "";
    }
  }

  public static List<String> buildCsvRowFile(IndexedFile file, List<String> fields) {
    List<String> row = new ArrayList<>();
    for (String field : fields) {
      row.add(getFieldValueFile(file, field));
    }
    return row;
  }

  public static List<String> buildCsvRowRepresentationInformation(RepresentationInformation ri, List<String> fields) {
    List<String> row = new ArrayList<>();
    for (String field : fields) {
      row.add(getFieldValueRepresentationInformation(ri, field));
    }
    return row;
  }

  private static String getFieldValueRepresentationInformation(RepresentationInformation ri, String field) {
    if (field == null) return "";
    switch (field.trim()) {
      case "id": return nullToEmpty(ri.getId());
      case "name": return nullToEmpty(ri.getName());
      case "description": return nullToEmpty(ri.getDescription());
      case "family": return nullToEmpty(ri.getFamily());
      case "tags": return ri.getTags() != null ? String.join("; ", ri.getTags()) : "";
      case "support": return ri.getSupport() != null ? ri.getSupport().toString() : "";
      case "createdOn": return formatDateTime(ri.getCreatedOn());
      case "updatedOn": return formatDateTime(ri.getUpdatedOn());
      default: return "";
    }
  }

  private static String getFieldValueFile(IndexedFile file, String field) {
    if (field == null) return "";
    switch (field.trim()) {
      case "uuid": return nullToEmpty(file.getUUID());
      case "id": return nullToEmpty(file.getId());
      case "aipId": return nullToEmpty(file.getAipId());
      case "representationId": return nullToEmpty(file.getRepresentationId());
      case "originalName": return nullToEmpty(file.getOriginalName());
      case "size": return String.valueOf(file.getSize());
      case "isDirectory": return file.isDirectory() ? "Ja" : "Nej";
      default: return "";
    }
  }

  private static String nullToEmpty(String s) {
    return s != null ? s : "";
  }

  private static String formatDate(Date d) {
    return d != null ? DATE_FMT.format(d.toInstant()) : "";
  }

  private static String formatDateTime(Date d) {
    return d != null ? DATETIME_FMT.format(d.toInstant()) : "";
  }

  private static String getConfigKeyPrefix(String exportClass) {
    if (exportClass == null) return null;
    switch (exportClass) {
      case "org.roda.core.data.v2.ip.IndexedAIP": return "ui.export.aip";
      case "org.roda.core.data.v2.jobs.IndexedJob": return "ui.export.job";
      case "org.roda.core.data.v2.jobs.IndexedReport": return "ui.export.report";
      case "org.roda.core.data.v2.log.LogEntry": return "ui.export.logentry";
      case "org.roda.core.data.v2.ip.TransferredResource": return "ui.export.transferredresource";
      case "org.roda.core.data.v2.user.RODAMember": return "ui.export.member";
      case "org.roda.core.data.v2.ip.IndexedDIP": return "ui.export.dip";
      case "org.roda.core.data.v2.risks.IndexedRisk": return "ui.export.risk";
      case "org.roda.core.data.v2.risks.RiskIncidence": return "ui.export.riskincidence";
      case "org.roda.core.data.v2.ip.IndexedRepresentation": return "ui.export.representation";
      case "org.roda.core.data.v2.ip.metadata.IndexedPreservationEvent": return "ui.export.preservationevent";
      case "org.roda.core.data.v2.ip.metadata.IndexedPreservationAgent": return "ui.export.preservationagent";
      case "org.roda.core.data.v2.notifications.Notification": return "ui.export.notification";
      case "org.roda.core.data.v2.disposal.confirmation.DisposalConfirmation": return "ui.export.disposalconfirmation";
      case "org.roda.core.data.v2.ip.IndexedFile": return "ui.export.file";
      case "org.roda.core.data.v2.ri.RepresentationInformation": return "ui.export.representationinformation";
      default: return null;
    }
  }

  private static String getFieldLabel(String configKeyPrefix, String field) {
    String trimmed = field != null ? field.trim() : field;
    if (configKeyPrefix != null) {
      try {
        String label = RodaCoreFactory.getConfigurationManager()
          .getConfigurationString(configKeyPrefix + ".fields." + trimmed + ".label", null);
        if (label != null && !label.isEmpty()) return label;
      } catch (Exception e) {
        // fall through to field name
      }
    }
    return trimmed;
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

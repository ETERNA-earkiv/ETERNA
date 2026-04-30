/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.plugins.base.conversion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.roda.core.RodaCoreFactory;
import org.roda.core.common.iterables.CloseableIterable;
import org.roda.core.common.iterables.CloseableIterables;
import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.common.RodaConstants.PreservationEventType;
import org.roda.core.data.exceptions.AlreadyExistsException;
import org.roda.core.data.exceptions.AuthorizationDeniedException;
import org.roda.core.data.exceptions.GenericException;
import org.roda.core.data.exceptions.InvalidParameterException;
import org.roda.core.data.exceptions.NotFoundException;
import org.roda.core.data.exceptions.RODAException;
import org.roda.core.data.exceptions.RequestNotValidException;
import org.roda.core.data.v2.IsRODAObject;
import org.roda.core.data.v2.common.OptionalWithCause;
import org.roda.core.data.v2.ip.AIP;
import org.roda.core.data.v2.ip.AIPState;
import org.roda.core.data.v2.ip.DIP;
import org.roda.core.data.v2.ip.DIPFile;
import org.roda.core.data.v2.ip.File;
import org.roda.core.data.v2.ip.FileLink;
import org.roda.core.data.v2.ip.IndexedFile;
import org.roda.core.data.v2.ip.Representation;
import org.roda.core.data.v2.ip.StoragePath;
import org.roda.core.data.v2.ip.metadata.LinkingIdentifier;
import org.roda.core.data.v2.jobs.Job;
import org.roda.core.data.v2.jobs.PluginParameter;
import org.roda.core.data.v2.jobs.PluginParameter.PluginParameterType;
import org.roda.core.data.v2.jobs.PluginState;
import org.roda.core.data.v2.jobs.PluginType;
import org.roda.core.data.v2.jobs.Report;
import org.roda.core.data.v2.validation.ValidationException;
import org.roda.core.data.v2.validation.ValidationIssue;
import org.roda.core.data.v2.validation.ValidationReport;
import org.roda.core.data.v2.index.IndexResult;
import org.roda.core.data.v2.index.filter.Filter;
import org.roda.core.data.v2.index.filter.SimpleFilterParameter;
import org.roda.core.data.v2.index.sort.SortParameter;
import org.roda.core.data.v2.index.sort.Sorter;
import org.roda.core.data.v2.index.sublist.Sublist;
import org.roda.core.data.v2.ip.IndexedRepresentation;
import org.roda.core.index.IndexService;
import org.roda.core.model.ModelService;
import org.roda.core.model.utils.ModelUtils;
import org.roda.core.plugins.AbstractAIPComponentsPlugin;
import org.roda.core.plugins.PluginException;
import org.roda.core.plugins.PluginHelper;
import org.roda.core.plugins.base.characterization.PremisSkeletonPluginUtils;
import org.roda.core.plugins.orchestrate.JobPluginInfo;
import org.roda.core.storage.Binary;
import org.roda.core.storage.ContentPayload;
import org.roda.core.storage.DirectResourceAccess;
import org.roda.core.storage.StorageService;
import org.roda.core.storage.fs.FSPathContentPayload;
import org.roda.core.storage.fs.FSUtils;
import org.roda.core.util.CommandException;
import org.roda.core.util.IdUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractConvertPlugin<T extends IsRODAObject> extends AbstractAIPComponentsPlugin<T> {
  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractConvertPlugin.class);
  private static Map<String, PluginParameter> pluginParameters = new HashMap<>();

  static {
    pluginParameters.put(RodaConstants.PLUGIN_PARAMS_INPUT_FORMAT,
        PluginParameter.getBuilder(RodaConstants.PLUGIN_PARAMS_INPUT_FORMAT, "Input format", PluginParameterType.STRING)
            .withDefaultValue("")
            .isMandatory(true)
            .isReadOnly(false)
            .withDescription(
                "Input file format to be converted (check documentation for list of supported formats). If the input file format is not specified, the task will"
                    + " run on all supported formats (check roda-core-formats.properties for list of supported formats).")
            .build());

    pluginParameters.put(RodaConstants.PLUGIN_PARAMS_OUTPUT_FORMAT,
        PluginParameter.getBuilder(RodaConstants.PLUGIN_PARAMS_OUTPUT_FORMAT, "Output format", PluginParameterType.STRING)
            .withDefaultValue("")
            .isMandatory(true)
            .isReadOnly(false)
            .withDescription("Output file format to be converted (check documentation for list of supported formats).")
            .build());

    pluginParameters.put(RodaConstants.PLUGIN_PARAMS_IGNORE_OTHER_FILES,
        PluginParameter.getBuilder(RodaConstants.PLUGIN_PARAMS_IGNORE_OTHER_FILES, "Ignore other files",
            PluginParameterType.BOOLEAN)
            .withDefaultValue("true")
            .isMandatory(false)
            .isReadOnly(false)
            .withDescription("Do not process files that have a different format from the indicated.")
            .build());

    pluginParameters.put(RodaConstants.PLUGIN_PARAMS_REPRESENTATION_OR_DIP,
        PluginParameter.getBuilder(RodaConstants.PLUGIN_PARAMS_REPRESENTATION_OR_DIP, "Outcome",
            PluginParameterType.CONVERSION)
            .withDescription(
                "A conversion can create a representation or a dissemination. Please choose which option to output")
            .build());
  }

  private String inputFormat;
  private String outputFormat;
  private boolean ignoreFiles = true;
  private boolean createDIP = false;
  private boolean hasPartialSuccessOnOutcome = false;
  private String dipTitle = "";
  private String dipDescription = "";
  private String representationType = "";
  private boolean markAsPreservation = true;

  /**
   * Context object to encapsulate conversion parameters and state
   */
  private static class ConversionContext {
    final String aipId;
    final String sourceRepresentationId;
    String newRepresentationId;
    final Report reportItem;
    final List<String> newRepresentations;

    ConversionContext(String aipId, String sourceRepresentationId, String newRepresentationId,
        Report reportItem, List<String> newRepresentations) {
      this.aipId = aipId;
      this.sourceRepresentationId = sourceRepresentationId;
      this.newRepresentationId = newRepresentationId;
      this.reportItem = reportItem;
      this.newRepresentations = newRepresentations;
    }
  }

  /**
   * Result object for file processing operations
   */
  private static class ConversionResult {
    final PluginState state;
    final List<File> alteredFiles;
    final List<File> newFiles;

    ConversionResult(PluginState state, List<File> alteredFiles, List<File> newFiles) {
      this.state = state;
      this.alteredFiles = alteredFiles;
      this.newFiles = newFiles;
    }
  }

  protected AbstractConvertPlugin() {
    super();
    inputFormat = "";
    outputFormat = "";
  }

  protected Map<String, PluginParameter> getDefaultParameters() {
    return pluginParameters.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> new PluginParameter(e.getValue())));
  }

  protected List<PluginParameter> orderParameters(Map<String, PluginParameter> params) {
    List<PluginParameter> orderedList = new ArrayList<>();
    orderedList.add(params.get(RodaConstants.PLUGIN_PARAMS_INPUT_FORMAT));
    orderedList.add(params.get(RodaConstants.PLUGIN_PARAMS_OUTPUT_FORMAT));
    orderedList.add(params.get(RodaConstants.PLUGIN_PARAMS_IGNORE_OTHER_FILES));
    orderedList.add(params.get(RodaConstants.PLUGIN_PARAMS_REPRESENTATION_OR_DIP));
    orderedList.add(params.get(RodaConstants.PLUGIN_PARAMS_DISSEMINATION_TITLE));
    orderedList.add(params.get(RodaConstants.PLUGIN_PARAMS_DISSEMINATION_DESCRIPTION));
    orderedList.add(params.get(RodaConstants.PLUGIN_PARAMS_REPRESENTATION_TYPE));
    return orderedList;
  }

  @Override
  public void init() throws PluginException {
    // do nothing
  }

  @Override
  public void shutdown() {
    // do nothing
  }

  public abstract List<String> getApplicableTo();

  public abstract List<String> getConvertableTo();

  public abstract Map<String, List<String>> getPronomToExtension();

  public abstract Map<String, List<String>> getMimetypeToExtension();

  public String getInputFormat() {
    return this.inputFormat;
  }

  public void setInputFormat(String format) {
    this.inputFormat = format;
  }

  public String getOutputFormat() {
    return this.outputFormat;
  }

  public void setOutputFormat(String format) {
    this.outputFormat = format;
  }

  @Override
  public PluginType getType() {
    return PluginType.AIP_TO_AIP;
  }

  @Override
  public boolean areParameterValuesValid() {
    return true;
  }

  @Override
  public List<PluginParameter> getParameters() {
    return orderParameters(getDefaultParameters());
  }

  @Override
  public void setParameterValues(Map<String, String> parameters) throws InvalidParameterException {
    super.setParameterValues(parameters);

    if (parameters.containsKey(RodaConstants.PLUGIN_PARAMS_INPUT_FORMAT)) {
      setInputFormat(parameters.get(RodaConstants.PLUGIN_PARAMS_INPUT_FORMAT));
    }

    if (parameters.containsKey(RodaConstants.PLUGIN_PARAMS_OUTPUT_FORMAT)) {
      setOutputFormat(parameters.get(RodaConstants.PLUGIN_PARAMS_OUTPUT_FORMAT));
    }

    if (parameters.containsKey(RodaConstants.PLUGIN_PARAMS_IGNORE_OTHER_FILES)) {
      ignoreFiles = Boolean.parseBoolean(parameters.get(RodaConstants.PLUGIN_PARAMS_IGNORE_OTHER_FILES));
    }

    if (parameters.containsKey(RodaConstants.PLUGIN_PARAMS_REPRESENTATION_OR_DIP)) {
      String value = parameters.get(RodaConstants.PLUGIN_PARAMS_REPRESENTATION_OR_DIP);

      Map<String, String> map = new HashMap<>();
      String[] keyValuePairs = value.split(";");
      for (String pair : keyValuePairs) {
        String[] parts = pair.split("=");
        if (parts.length == 2) {
          map.put(parts[0].trim(), parts[1].trim());
        }
      }

      if (map.get("type").equals(RodaConstants.PLUGIN_PARAMS_CONVERSION_REPRESENTATION)) {
        createDIP = false;
        representationType = map.get("value");
        markAsPreservation = Boolean.parseBoolean(map.get("markAsPreservation"));
      } else {
        createDIP = true;
        dipTitle = map.get("title");
        dipDescription = map.get("description");
      }
    }

    hasPartialSuccessOnOutcome = Boolean.parseBoolean(RodaCoreFactory.getRodaConfigurationAsString("core", "tools",
        "convert", "allplugins", "hasPartialSuccessOnOutcome"));
  }

  /**
   * Helper method to process files for conversion
   */
  private ConversionResult processFilesForConversion(IndexService index, ModelService model, StorageService storage,
      CloseableIterable<OptionalWithCause<File>> allFiles, Job cachedJob, ConversionContext context, Report report)
      throws PluginException, AuthorizationDeniedException, RequestNotValidException, NotFoundException,
      GenericException, IOException, AlreadyExistsException {

    PluginState pluginResultState = PluginState.SUCCESS;
    ValidationReport validationReport = new ValidationReport();
    boolean hasUnacceptedFormatFiles = false;
    List<File> alteredFiles = new ArrayList<>();
    List<File> newFiles = new ArrayList<>();
    List<DIPFile> newDIPFiles = new ArrayList<>();

    for (OptionalWithCause<File> oFile : allFiles) {
      String newFileId = null;
      if (oFile.isPresent()) {
        File file = oFile.get();
        LOGGER.debug("Processing file {}", file);

        if (!file.isDirectory()) {
          if (FSUtils.isManifestOfExternalFiles(file.getId())) {
            processShallowFile(index, file, cachedJob, context.newRepresentationId, model, pluginResultState,
                context.reportItem, true, storage, newFileId, newDIPFiles, newFiles,
                pluginResultState, report);
          } else {
            IndexedFile ifile = index.retrieve(IndexedFile.class, IdUtils.getFileId(file),
                RodaConstants.FILE_FORMAT_FIELDS_TO_RETURN);
            String fileMimetype = ifile.getFileFormat().getMimeType();
            String filePronom = ifile.getFileFormat().getPronom();
            String fileFormat = ifile.getId().substring(ifile.getId().lastIndexOf('.') + 1);
            List<String> applicableTo = getApplicableTo();
            List<String> convertableTo = getConvertableTo();
            Map<String, List<String>> pronomToExtension = getPronomToExtension();
            Map<String, List<String>> mimetypeToExtension = getMimetypeToExtension();

            if (doPluginExecute(fileFormat, filePronom, fileMimetype, applicableTo, convertableTo,
                pronomToExtension, mimetypeToExtension)) {

              fileFormat = getNewFileFormat(fileFormat, filePronom, fileMimetype, applicableTo,
                  pronomToExtension, mimetypeToExtension);

              StoragePath fileStoragePath = ModelUtils.getFileStoragePath(file);
              DirectResourceAccess directAccess = storage.getDirectAccess(fileStoragePath);

              LOGGER.debug("Running a ConvertPlugin ({} to {}) on {}", fileFormat, outputFormat, file.getId());
              try {
                Path pluginResult = Files.createTempFile(getWorkingDirectory(), "converted",
                    "." + FilenameUtils.normalize(getOutputFormat()));
                String result = executePlugin(directAccess.getPath(), pluginResult, fileFormat);

                newFileId = file.getId().replaceFirst("[.][^.]+$", "." + outputFormat);

                ContentPayload payload = new FSPathContentPayload(pluginResult);

                if (createDIP) {
                  DIPFile f = model.createDIPFile(context.newRepresentationId, file.getPath(), newFileId,
                      directAccess.getPath().toFile().length(), payload, true);
                  newDIPFiles.add(f);
                } else {
                  // Create file in existing representation
                  String actualRepresentationId = context.newRepresentationId;
                  if (!context.newRepresentations.contains(actualRepresentationId)) {
                    // Determine the appropriate representation ID based on existing reps and
                    // desired type
                    actualRepresentationId = determineRepresentationId(index, model, context.aipId,
                        context.newRepresentationId,
                        representationType, cachedJob.getUsername(), markAsPreservation);

                    context.newRepresentationId = actualRepresentationId;

                    if (!context.newRepresentations.contains(actualRepresentationId)) {
                      context.newRepresentations.add(actualRepresentationId);
                    }
                  }

                  File f = model.updateFile(context.aipId, actualRepresentationId, file.getPath(), newFileId,
                      payload, true, cachedJob.getUsername(), true);
                  newFiles.add(f);
                }

                alteredFiles.add(file);
                IOUtils.closeQuietly(directAccess);

                Report fileReportItem = PluginHelper.initPluginReportItem(this, file.getId(), File.class,
                    AIPState.ACTIVE);
                fileReportItem.setPluginState(pluginResultState).setPluginDetails(result);
                context.reportItem.addReport(fileReportItem);

              } catch (CommandException e) {
                pluginResultState = PluginState.FAILURE;
                context.reportItem.setPluginState(pluginResultState).addPluginDetails(getOutputMessage(e));

                LOGGER.debug("Conversion ({} to {}) failed on file {} of representation {} from AIP {}",
                    fileFormat, outputFormat, file.getId(), context.sourceRepresentationId, context.aipId);
              }

            } else {
              if (ignoreFiles) {
                validationReport.addIssue(new ValidationIssue(ModelUtils.getFileStoragePath(file).toString()));
              } else {
                pluginResultState = PluginState.FAILURE;
                hasUnacceptedFormatFiles = true;
              }
            }
          }
        }
      } else {
        LOGGER.error("Cannot process AIP representation file", oFile.getCause());
      }
    }

    context.reportItem.setPluginState(pluginResultState);

    if (pluginResultState.equals(PluginState.SUCCESS)) {
      if (ignoreFiles && !validationReport.getIssues().isEmpty()) {
        context.reportItem.setHtmlPluginDetails(true)
            .setPluginDetails(validationReport.toHtml(false, false, false, "Ignored files"));
      }
    }

    if (hasUnacceptedFormatFiles) {
      context.reportItem.setPluginDetails(
          "Source files include formats that are not accepted by this plugin and the plugin was not set to ignore these files. The conversion was not run.");
    }

    return new ConversionResult(pluginResultState, alteredFiles, newFiles);
  }

  @Override
  protected Report executeOnAIP(IndexService index, ModelService model, Report report,
      JobPluginInfo jobPluginInfo, List<AIP> list, Job cachedJob) throws PluginException {
    StorageService storage = model.getStorage();
    for (AIP aip : list) {
      LOGGER.debug("Processing AIP {}", aip.getId());
      List<String> newRepresentations = new ArrayList<>();
      String newRepresentationID = IdUtils.createUUID();
      PluginState reportState = PluginState.SUCCESS;
      List<File> alteredFiles = new ArrayList<>();
      List<File> newFiles = new ArrayList<>();
      if (aip.getRepresentations() != null && !aip.getRepresentations().isEmpty()) {
        // Create a single "converted" representation for all files from all
        // representations
        Report aipReportItem = PluginHelper.initPluginReportItem(this, aip.getId(),
            aip.getId(), AIP.class, AIPState.ACTIVE);
        if (createDIP) {
          aipReportItem.setOutcomeObjectClass(DIP.class.getName());
        }

        // Process all representations and collect all their files
        List<OptionalWithCause<File>> allFilesFromAllRepresentations = new ArrayList<>();

        for (Representation representation : aip.getRepresentations()) {
          try (CloseableIterable<OptionalWithCause<File>> repFiles = model.listFilesUnder(aip.getId(),
              representation.getId(), true)) {
            for (OptionalWithCause<File> file : repFiles) {
              allFilesFromAllRepresentations.add(file);
            }
          } catch (Exception e) {
            LOGGER.error("Error listing files for representation {}: {}", representation.getId(), e.getMessage());
          }
        }

        ConversionContext context = new ConversionContext(aip.getId(), null, // No specific source representation
            newRepresentationID, aipReportItem, newRepresentations);

        try {
          CloseableIterable<OptionalWithCause<File>> allFiles = CloseableIterables
              .fromList(allFilesFromAllRepresentations);

          ConversionResult result = processFilesForConversion(index, model, storage, allFiles, cachedJob, context, report);

          // accumulate results for AIP-level event creation
          alteredFiles.addAll(result.alteredFiles);
          newFiles.addAll(result.newFiles);

        } catch (RuntimeException | NotFoundException | GenericException | RequestNotValidException
            | AuthorizationDeniedException | IOException | AlreadyExistsException | PluginException e) {
          LOGGER.error("Error processing AIP " + aip.getId() + ": " + e.getMessage(), e);
          reportState = PluginState.FAILURE;
          aipReportItem.setPluginState(reportState).setPluginDetails(e.getMessage());
        } finally {
          report.addReport(aipReportItem);
          PluginHelper.updatePartialJobReport(this, model, aipReportItem, true, cachedJob);

          if (!createDIP && !alteredFiles.isEmpty()) {
            try {
              Representation rep = model.retrieveRepresentation(aip.getId(), context.newRepresentationId);
              createPremisSkeletonOnRepresentation(model, aip.getId(), rep, cachedJob.getUsername());
            } catch (RequestNotValidException | GenericException | NotFoundException | AuthorizationDeniedException
                | ValidationException | IOException e) {
              LOGGER.error("Error running premis skeleton on new representation: {}", e.getMessage());
            }
          }
        }
      } else {

        final Report reportItem = PluginHelper.initPluginReportItem(this, aip.getId(), AIP.class, AIPState.ACTIVE);
        PluginHelper.updatePartialJobReport(this, model, reportItem, false, cachedJob);
        reportState = PluginState.SKIPPED;
        reportItem.setPluginState(reportState);
        reportItem.setPluginDetails("Skipped because no representation was found for this AIP");
        report.addReport(reportItem);
        PluginHelper.updatePartialJobReport(this, model, reportItem, true, cachedJob);
      }
      try {
        LOGGER.debug("Creating convert plugin event for the AIP {}", aip.getId());
        boolean notifyEvent = false;
        createEvent(model, index, aip.getId(), null, null, null, outputFormat, reportState, alteredFiles, newFiles,
            notifyEvent, cachedJob);
        model.notifyAipUpdated(aip.getId());
        jobPluginInfo.incrementObjectsProcessed(reportState);
      } catch (PluginException | RequestNotValidException | GenericException | NotFoundException
          | AuthorizationDeniedException e) {
        LOGGER.debug("Error on update AIP notify");
      }
    }

    return report;
  }

  @Override
  protected Report executeOnRepresentation(IndexService index, ModelService model, Report report,
      JobPluginInfo jobPluginInfo, List<Representation> list, Job cachedJob) throws PluginException {
    StorageService storage = model.getStorage();
    List<String> newRepresentations = new ArrayList<>();
    String aipId = null;

    for (Representation representation : list) {
      aipId = representation.getAipId();
      boolean notify = true;
      PluginState reportState = PluginState.SUCCESS;

      String newRepresentationID = IdUtils.createUUID();

      Report reportItem = PluginHelper.initPluginReportItem(this, IdUtils.getRepresentationId(representation),
          Representation.class, AIPState.ACTIVE);
      if (createDIP) {
        reportItem.setOutcomeObjectClass(DIP.class.getName());
      }

      ConversionContext context = new ConversionContext(aipId, representation.getId(),
          newRepresentationID, reportItem, newRepresentations);
      boolean filesConverted = false;

      try (CloseableIterable<OptionalWithCause<File>> allFiles = model.listFilesUnder(representation.getAipId(),
          representation.getId(), true)) {
        LOGGER.debug("Processing representation {}", representation);

        ConversionResult result = processFilesForConversion(index, model, storage, allFiles, cachedJob, context, report);

        filesConverted = !result.alteredFiles.isEmpty();

        // add unchanged files to the new representation
        if (filesConverted) {
          if (createDIP) {
            createNewFilesOnDIP(storage, model, new ArrayList<>(), context.newRepresentationId, notify);
          } else {
            createNewFilesOnRepresentation(storage, model, new ArrayList<>(), context.newRepresentationId, cachedJob.getUsername(),
                notify);
          }
        }

        report.addReport(reportItem);
        PluginHelper.updatePartialJobReport(this, model, reportItem, true, cachedJob);

        jobPluginInfo.incrementObjectsProcessed(reportState);
        LOGGER.debug("Creating convert plugin event for the representation " + representation.getId());
        boolean notifyEvent = false;
        createEvent(model, index, aipId, representation.getId(), null, null, outputFormat, result.state,
            result.alteredFiles, result.newFiles, notifyEvent, cachedJob);

      } catch (RuntimeException | NotFoundException | GenericException | RequestNotValidException
          | AuthorizationDeniedException | IOException | AlreadyExistsException e) {
        LOGGER.error("Error processing Representation {}: {}", representation.getId(), e.getMessage(), e);
        reportState = PluginState.FAILURE;

        reportItem.setPluginState(PluginState.FAILURE).setPluginDetails(e.getMessage());
        report.addReport(reportItem);
      }

      if (!createDIP && filesConverted) {
        try {
          Representation rep = model.retrieveRepresentation(representation.getAipId(), context.newRepresentationId);
          createPremisSkeletonOnRepresentation(model, representation.getAipId(), rep, cachedJob.getUsername());
        } catch (RequestNotValidException | GenericException | NotFoundException | AuthorizationDeniedException
            | ValidationException | IOException e) {
          LOGGER.error("Error running premis skeleton on new representation: {}", e.getMessage());
        }
      }
    }

    try {
      model.notifyAipUpdated(aipId);
    } catch (RODAException e) {
      LOGGER.error("Error running creating agent for AbstractConvertPlugin", e);
    }

    return report;
  }

  @Override
  protected Report executeOnFile(IndexService index, ModelService model, Report report,
      JobPluginInfo jobPluginInfo, List<File> list, Job cachedJob) throws PluginException {
    StorageService storage = model.getStorage();

    List<String> newRepresentations = new ArrayList<>();
    String newRepresentationID = IdUtils.createUUID();
    Report reportItem = null;
    PluginState reportState = PluginState.SUCCESS;

    for (File file : list) {
      try {
        if (FSUtils.isManifestOfExternalFiles(file.getId())) {
          for (OptionalWithCause<File> fileShallows : model.listExternalFilesUnder(file)) {
            executeOnFile(index, model, report, jobPluginInfo, Arrays.asList(fileShallows.get()), cachedJob);
          }
        } else {
          ConversionContext context = null;
          boolean fileConverted = false;
          try {
            LOGGER.debug("Processing file {}", file.getId());

            reportItem = PluginHelper.initPluginReportItem(this, IdUtils.getFileId(file), File.class, AIPState.ACTIVE);
            if (createDIP) {
              reportItem.setOutcomeObjectClass(DIP.class.getName());
            }

            if (!file.isDirectory()) {
              // Process single file using helper method
              CloseableIterable<OptionalWithCause<File>> files = new CloseableIterable<OptionalWithCause<File>>() {
                @Override
                public java.util.Iterator<OptionalWithCause<File>> iterator() {
                  return Arrays.asList(OptionalWithCause.of(file)).iterator();
                }

                @Override
                public void close() throws IOException {
                  // Nothing to close
                }
              };

              context = new ConversionContext(file.getAipId(), file.getRepresentationId(),
                  newRepresentationID, reportItem, newRepresentations);
              // Note: For executeOnFile, we don't set createPerFileContainer anymore since
              // containers are created lazily

              ConversionResult result = processFilesForConversion(index, model, storage, files, cachedJob, context, report);

              fileConverted = !result.alteredFiles.isEmpty();

              if (!result.state.equals(PluginState.SUCCESS)) {
                reportState = PluginState.FAILURE;
                jobPluginInfo.incrementObjectsProcessedWithFailure();
              } else {
                jobPluginInfo.incrementObjectsProcessedWithSuccess();
              }
            }

          } catch (RuntimeException | NotFoundException | GenericException | RequestNotValidException
              | AuthorizationDeniedException | IOException | AlreadyExistsException e) {
            LOGGER.error("Error processing File {}: {}", file.getId(), e.getMessage(), e);
            reportState = PluginState.FAILURE;
            if (reportItem != null) {
              reportItem.setPluginDetails(e.getMessage());
            }
            jobPluginInfo.incrementObjectsProcessedWithFailure();
          } finally {
            if (reportItem != null) {
              reportItem.setPluginState(PluginState.SUCCESS); // This should be set based on actual result
              report.addReport(reportItem);
              PluginHelper.updatePartialJobReport(this, model, reportItem, true, cachedJob);
            }
          }

          boolean notifyEvent = true;
          createEvent(model, index, file.getAipId(), file.getRepresentationId(), file.getPath(), file.getId(),
              outputFormat, reportState, Arrays.asList(file), new ArrayList<>(), notifyEvent, cachedJob);

          if (!createDIP && context != null && fileConverted) {
            try {
              Representation rep = model.retrieveRepresentation(file.getAipId(), context.newRepresentationId);
              createPremisSkeletonOnRepresentation(model, file.getAipId(), rep, cachedJob.getUsername());
            } catch (RequestNotValidException | GenericException | NotFoundException | AuthorizationDeniedException
                | ValidationException | IOException e) {
              LOGGER.error("Error running premis skeleton on new representation: {}", e.getMessage());
            }
          }
        }
      } catch (NotFoundException | GenericException | RequestNotValidException | AuthorizationDeniedException e) {
        reportState = PluginState.FAILURE;
        if (reportItem != null) {
          reportItem.setPluginDetails(e.getMessage());
        }
        jobPluginInfo.incrementObjectsProcessedWithFailure();
      }
    }

    return report;
  }

  public abstract String executePlugin(Path inputPath, Path outputPath, String fileFormat)
      throws UnsupportedOperationException, IOException, CommandException;

  private void createPremisSkeletonOnRepresentation(ModelService model, String aipId, Representation representation,
      String username)
      throws RequestNotValidException, GenericException, NotFoundException, AuthorizationDeniedException,
      ValidationException, IOException {
    List<String> algorithms = RodaCoreFactory.getFixityAlgorithms();
    PremisSkeletonPluginUtils.createPremisSkeletonOnRepresentation(model, aipId, representation.getId(), algorithms,
        username);
    model.notifyRepresentationUpdated(representation).failOnError();
  }

  private void createEvent(ModelService model, IndexService index, String aipId, String representationId,
      List<String> filePath, String fileId, String outputFormat, PluginState outcome, List<File> alteredFiles,
      List<File> newFiles, boolean notify, Job cachedJob) throws PluginException {

    List<LinkingIdentifier> premisSourceFilesIdentifiers = new ArrayList<>();
    List<LinkingIdentifier> premisTargetFilesIdentifiers = new ArrayList<>();

    // building the detail for the plugin event
    StringBuilder stringBuilder = new StringBuilder();

    if (alteredFiles.isEmpty()) {
      stringBuilder
          .append("No file was successfully converted on this representation due to plugin or command line issues.");
    } else {
      for (File file : alteredFiles) {
        premisSourceFilesIdentifiers.add(PluginHelper.getLinkingIdentifier(aipId, file.getRepresentationId(),
            file.getPath(), file.getId(), RodaConstants.PRESERVATION_LINKING_OBJECT_SOURCE));
      }

      for (File file : newFiles) {
        premisTargetFilesIdentifiers.add(PluginHelper.getLinkingIdentifier(aipId, file.getRepresentationId(),
            file.getPath(), file.getId(), RodaConstants.PRESERVATION_LINKING_OBJECT_OUTCOME));
      }

      stringBuilder.append("The source files were converted to a new format (." + outputFormat + ")");
    }

    // Conversion plugin did not run correctly
    if (PluginState.FAILURE.equals(outcome)
        || (outcome == PluginState.PARTIAL_SUCCESS && !hasPartialSuccessOnOutcome)) {
      outcome = PluginState.FAILURE;
      stringBuilder.setLength(0);
    }

    try {
      PluginHelper.createPluginEvent(this, aipId, representationId, filePath, fileId, model, index,
          premisSourceFilesIdentifiers, premisTargetFilesIdentifiers, outcome, stringBuilder.toString(), notify,
          cachedJob);
    } catch (RequestNotValidException | NotFoundException | GenericException | AuthorizationDeniedException
        | ValidationException | AlreadyExistsException e) {
      throw new PluginException(e.getMessage(), e);
    }
  }

  private void processShallowFile(IndexService index, File file, Job cachedJob, String newRepresentationID,
      ModelService model, PluginState pluginResultState, Report reportItem, boolean notify, StorageService storage,
      String newFileId, List<DIPFile> newDIPFiles, List<File> newFiles, PluginState reportState, Report report)
      throws AuthorizationDeniedException, RequestNotValidException, NotFoundException, GenericException,
      PluginException {

    Map<String, String> changedRepresentationsOnAIPs = new HashMap<>();

    for (OptionalWithCause<File> fileShallows : model.listExternalFilesUnder(file)) {
      for (File file1 : Arrays.asList(fileShallows.get())) {
        StorageService tmpStorageService = null;

        try {
          LOGGER.debug("Processing file {}", file1.getId());
          newRepresentationID = IdUtils.createUUID();
          pluginResultState = PluginState.SUCCESS;

          reportItem = PluginHelper.initPluginReportItem(this, IdUtils.getFileId(file1), File.class, AIPState.ACTIVE);
          if (createDIP) {
            reportItem.setOutcomeObjectClass(DIP.class.getName());
          }

          if (!file1.isDirectory()) {
            IndexedFile ifile = index.retrieve(IndexedFile.class, IdUtils.getFileId(file1),
                RodaConstants.FILE_FORMAT_FIELDS_TO_RETURN);
            String fileMimetype = ifile.getFileFormat().getMimeType();
            String filePronom = ifile.getFileFormat().getPronom();
            String fileFormat = ifile.getId().substring(ifile.getId().lastIndexOf('.') + 1);
            List<String> applicableTo = getApplicableTo();
            List<String> convertableTo = getConvertableTo();
            Map<String, List<String>> pronomToExtension = getPronomToExtension();
            Map<String, List<String>> mimetypeToExtension = getMimetypeToExtension();

            if (doPluginExecute(fileFormat, filePronom, fileMimetype, applicableTo, convertableTo, pronomToExtension,
                mimetypeToExtension)) {

              fileFormat = getNewFileFormat(fileFormat, filePronom, fileMimetype, applicableTo, pronomToExtension,
                  mimetypeToExtension);

              StoragePath fileStoragePath = ModelUtils.getFileStoragePath(file1);
              DirectResourceAccess directAccess = storage.getDirectAccess(fileStoragePath);
              if (file1.isReference()) {
                tmpStorageService = model.resolveTemporaryResourceShallow(cachedJob.getId(), file1);
                directAccess = tmpStorageService.getDirectAccess(fileStoragePath);
              }

              LOGGER.debug("Running a ConvertPlugin ({} to {}) on {}", fileFormat, outputFormat, file1.getId());
              try {
                Path pluginResult = Files.createTempFile(getWorkingDirectory(), "converted", "." + getOutputFormat());
                String result = executePlugin(directAccess.getPath(), pluginResult, fileFormat);

                ContentPayload payload = new FSPathContentPayload(pluginResult);

                // create a new representation if it does not exist
                LOGGER.debug("Creating a new representation {} on AIP {}", newRepresentationID, file1.getAipId());
                boolean original = false;
                if (createDIP) {
                  FileLink fileLink = new FileLink(file1.getAipId(), file1.getRepresentationId(), file1.getPath(),
                      file1.getId());
                  List<FileLink> links = new ArrayList<>();
                  links.add(fileLink);

                  AIP aipFile = model.retrieveAIP(file1.getAipId());

                  DIP dip = new DIP();
                  dip.setId(IdUtils.createUUID());
                  dip.setFileIds(links);
                  dip.setPermissions(aipFile.getPermissions());
                  dip.setTitle(dipTitle);
                  dip.setDescription(dipDescription);
                  dip.setType(RodaConstants.DIP_TYPE_CONVERSION);
                  dip = model.createDIP(dip, true);
                  newRepresentationID = dip.getId();
                } else {
                  // INFO will be a parameter
                  String newRepresentationType = RodaConstants.REPRESENTATION_TYPE_MIXED;

                  if (StringUtils.isNotBlank(representationType)) {
                    newRepresentationType = representationType;
                  }

                  List<String> state = Collections.emptyList();
                  if (markAsPreservation) {
                    state = List.of("PRESERVATION");
                  }

                  model.createRepresentation(file1.getAipId(), newRepresentationID, original,
                      newRepresentationType, true, cachedJob.getUsername(), state);
                }

                // update file on new representation
                if (outputFormat.equalsIgnoreCase(FilenameUtils.getExtension(file1.getId()))) {
                  // Same format, keep original filename
                  newFileId = file1.getId();
                } else {
                  // Different format, include original extension to avoid conflicts
                  String originalBaseName = FilenameUtils.getBaseName(file1.getId());
                  String originalExtension = FilenameUtils.getExtension(file1.getId());
                  newFileId = originalBaseName + "_" + originalExtension + "." + outputFormat;
                }

                if (createDIP) {
                  DIPFile f = model.createDIPFile(newRepresentationID, file1.getPath(), newFileId,
                      directAccess.getPath().toFile().length(), payload, notify);
                  newDIPFiles.add(f);
                } else {
                  model.deleteFile(file1.getAipId(), newRepresentationID, file1.getPath(), file1.getId(),
                      cachedJob.getUsername(), notify);
                  File f = model.createFile(file1.getAipId(), newRepresentationID, file1.getPath(), newFileId, payload,
                      cachedJob.getUsername(), notify);
                  newFiles.add(f);
                  reportItem.setSourceAndOutcomeObjectId(reportItem.getSourceObjectId(), IdUtils.getFileId(f));
                  changedRepresentationsOnAIPs.put(file1.getRepresentationId(), file1.getAipId());
                }

                Report fileReportItem = PluginHelper.initPluginReportItem(this, file1.getId(), File.class,
                    AIPState.ACTIVE);
                fileReportItem.setPluginState(PluginState.SUCCESS).setPluginDetails(result);
                reportItem.addReport(fileReportItem);

              } catch (CommandException e) {
                pluginResultState = PluginState.PARTIAL_SUCCESS;
                Report fileReportItem = PluginHelper.initPluginReportItem(this, file1.getId(), File.class,
                    AIPState.ACTIVE);
                fileReportItem.setPluginState(PluginState.PARTIAL_SUCCESS)
                    .setPluginDetails(e.getMessage() + "\n" + e.getOutput());
                reportItem.addReport(fileReportItem);

                LOGGER.debug("Conversion ({} to {}) failed on file {} of representation {} from AIP {}", fileFormat,
                    outputFormat, file1.getId(), file1.getRepresentationId(), file1.getAipId());
              }
            } else {
              if (ignoreFiles) {
                reportItem.setPluginDetails("This file was ignored.");
              } else {
                pluginResultState = PluginState.FAILURE;
                reportItem.setPluginDetails(
                    "This file was not ignored and it is not listed on the supported input file formats.");
              }
            }
          }

          if (!pluginResultState.equals(PluginState.SUCCESS)) {
            reportState = PluginState.FAILURE;
          }

        } catch (RuntimeException | NotFoundException | GenericException | RequestNotValidException
            | AuthorizationDeniedException | IOException | AlreadyExistsException e) {
          LOGGER.error("Error processing File {}: {}", file1.getId(), e.getMessage(), e);
          reportState = PluginState.FAILURE;
          reportItem.setPluginDetails(e.getMessage());
        } finally {
          if (file1.isReference() && tmpStorageService != null) {
            try {
              if (!cachedJob.getPluginType().equals(PluginType.INGEST)) {
                ModelUtils.removeTemporaryResourceShallow(cachedJob.getId(), ModelUtils.getAIPStoragePath(file1.getAipId()));
              }
            } catch (RequestNotValidException | IOException e) {
              LOGGER.error("Error on removing temporary AIP " + file1.getAipId(), e);
            }
          }
          reportItem.setPluginState(pluginResultState);
          report.addReport(reportItem);
          PluginHelper.updatePartialJobReport(this, model, reportItem, true, cachedJob);
        }

        boolean notifyEvent = true;
        createEvent(model, index, file1.getAipId(), file1.getRepresentationId(), file1.getPath(), file1.getId(),
            outputFormat, reportState, Arrays.asList(file1), newFiles, notifyEvent, cachedJob);

        if (!createDIP) {
          try {
            Representation rep = model.retrieveRepresentation(file1.getAipId(), newRepresentationID);
            createPremisSkeletonOnRepresentation(model, file1.getAipId(), rep, cachedJob.getUsername());
          } catch (RequestNotValidException | GenericException | NotFoundException | AuthorizationDeniedException
              | ValidationException | IOException e) {
            LOGGER.error("Error running premis skeleton on new representation: {}", e.getMessage());
          }
        }
      }
    }

  }

  private boolean doPluginExecute(String fileFormat, String filePronom, String fileMimetype, List<String> applicableTo,
      List<String> convertableTo, Map<String, List<String>> pronomToExtension,
      Map<String, List<String>> mimetypeToExtension) {
    String lowerCaseFileFormat = fileFormat == null ? null : fileFormat.toLowerCase();
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Testing if input and output formats are correct: [{}, {}, {}, {}, {}, {}, {}]", lowerCaseFileFormat,
          filePronom, fileMimetype, applicableTo, convertableTo, pronomToExtension, mimetypeToExtension);
    }

    boolean format = getInputFormat().isEmpty() || getInputFormat().equalsIgnoreCase(lowerCaseFileFormat);
    boolean applicable = applicableTo.isEmpty() || (filePronom != null && pronomToExtension.containsKey(filePronom))
        || (fileMimetype != null && mimetypeToExtension.containsKey(fileMimetype))
        || (applicableTo.contains(lowerCaseFileFormat));
    boolean convertable = convertableTo.isEmpty() || convertableTo.contains(outputFormat.toLowerCase());

    LOGGER.debug("Input and ouput test results: format={} applicable={} convertable={}", format, applicable,
        convertable);
    return format && applicable && convertable;
  }

  private String getNewFileFormat(String fileFormat, String filePronom, String fileMimetype, List<String> applicableTo,
      Map<String, List<String>> pronomToExtension, Map<String, List<String>> mimetypeToExtension) {
    String newFileFormat = fileFormat;

    if (!applicableTo.isEmpty()) {
      if (StringUtils.isNotBlank(filePronom) && pronomToExtension.get(filePronom) != null
          && !pronomToExtension.get(filePronom).contains(fileFormat)) {
        newFileFormat = pronomToExtension.get(filePronom).get(0);
      } else if (StringUtils.isNotBlank(fileMimetype) && mimetypeToExtension.get(fileMimetype) != null
          && !mimetypeToExtension.get(fileMimetype).contains(fileFormat)) {
        newFileFormat = mimetypeToExtension.get(fileMimetype).get(0);
      }
    }

    return newFileFormat;
  }

  private void createNewFilesOnRepresentation(StorageService storage, ModelService model, List<File> unchangedFiles,
      String newRepresentationID, String username, boolean notify)
      throws RequestNotValidException, GenericException, NotFoundException, AuthorizationDeniedException,
      UnsupportedOperationException, IOException, AlreadyExistsException {
    for (File f : unchangedFiles) {
      StoragePath fileStoragePath = ModelUtils.getFileStoragePath(f);
      Binary binary = storage.getBinary(fileStoragePath);
      Path uriPath = Paths.get(binary.getContent().getURI());
      ContentPayload payload = new FSPathContentPayload(uriPath);
      model.createFile(f.getAipId(), newRepresentationID, f.getPath(), f.getId(), payload, username, notify);
    }
  }

  private void createNewFilesOnDIP(StorageService storage, ModelService model, List<File> unchangedFiles,
      String newRepresentationID, boolean notify) throws RequestNotValidException, GenericException, NotFoundException,
      AuthorizationDeniedException, UnsupportedOperationException, IOException, AlreadyExistsException {
    for (File f : unchangedFiles) {
      StoragePath fileStoragePath = ModelUtils.getFileStoragePath(f);
      Binary binary = storage.getBinary(fileStoragePath);
      Path uriPath = Paths.get(binary.getContent().getURI());
      ContentPayload payload = new FSPathContentPayload(uriPath);
      model.createDIPFile(newRepresentationID, f.getPath(), f.getId(), uriPath.toFile().length(), payload, notify);
    }
  }

  private String determineRepresentationId(IndexService index, ModelService model, String aipId,
      String baseRepresentationId,
      String desiredType, String username, boolean markAsPreservation) throws RequestNotValidException,
      GenericException, AuthorizationDeniedException, NotFoundException, AlreadyExistsException {

    String desiredRepresentationType = StringUtils.isNotBlank(desiredType) ? desiredType
        : RodaConstants.REPRESENTATION_TYPE_MIXED;

    // If marking as preservation, check for existing preservation representations
    // with matching type
    try {
      Filter filter = new Filter();
      filter.add(new SimpleFilterParameter(RodaConstants.REPRESENTATION_AIP_ID, aipId));
      filter.add(new SimpleFilterParameter(RodaConstants.REPRESENTATION_TYPE, desiredRepresentationType));
      
      if (markAsPreservation) {
        filter.add(new SimpleFilterParameter(RodaConstants.REPRESENTATION_STATES, "PRESERVATION"));
      }
      
      Sorter sorter = new Sorter();
      sorter.add(new SortParameter(RodaConstants.REPRESENTATION_CREATED_ON, false)); // ascending (oldest first)

      Sublist sublist = new Sublist(0, 1); // Get only the first result

      IndexResult<IndexedRepresentation> result = index.find(IndexedRepresentation.class, filter, sorter, sublist,
          null, null, false, Arrays.asList(RodaConstants.REPRESENTATION_ID));

      if (result.getTotalCount() > 0 && !result.getResults().isEmpty()) {
        IndexedRepresentation existingPreservationRep = result.getResults().get(0);
        String existingRepId = existingPreservationRep.getId();
        LOGGER.debug("Found existing preservation representation '{}' with type '{}' on AIP {}, reusing it",
            existingRepId, desiredRepresentationType, aipId);
        return existingRepId;
      }
    } catch (Exception e) {
      LOGGER.warn("Error querying for existing preservation representations on AIP {}: {}", aipId, e.getMessage());
      // Continue with normal logic if query fails
    }

    try {
      Representation existingRep = model.retrieveRepresentation(aipId, baseRepresentationId);

      if (desiredRepresentationType.equals(existingRep.getType())) {
        // If marking as preservation, ensure the representation has PRESERVATION state
        if (markAsPreservation) {
          List<String> currentStates = existingRep.getRepresentationStates();
          if (currentStates == null || !currentStates.contains("PRESERVATION")) {
            // Add PRESERVATION state to existing representation
            List<String> newStates = new ArrayList<>();
            if (currentStates != null) {
              newStates.addAll(currentStates);
            }
            if (!newStates.contains("PRESERVATION")) {
              newStates.add("PRESERVATION");
            }
            model.changeRepresentationStates(aipId, baseRepresentationId, newStates, username);
            LOGGER.debug("Added PRESERVATION state to existing representation '{}' with type '{}' on AIP {}",
                baseRepresentationId, desiredRepresentationType, aipId);
          }
        }
        LOGGER.debug("Reusing existing representation '{}' with matching type '{}' on AIP {}", baseRepresentationId,
            desiredRepresentationType, aipId);
        return baseRepresentationId;
      } else {
        // Type mismatch - create a new representation with unique ID
        String newRepresentationId = IdUtils.createUUID();
        LOGGER.debug(
            "Creating new representation '{}' (type: '{}') because existing '{}' has different type '{}' on AIP {}",
            newRepresentationId, desiredRepresentationType, baseRepresentationId, existingRep.getType(), aipId);

        createNewRepresentation(model, aipId, newRepresentationId, desiredRepresentationType, username,
            markAsPreservation);
        return newRepresentationId;
      }
    } catch (NotFoundException e) {
      // Base representation doesn't exist, create it
      LOGGER.debug("Creating base representation '{}' with type '{}' on AIP {}", baseRepresentationId,
          desiredRepresentationType, aipId);
      createNewRepresentation(model, aipId, baseRepresentationId, desiredRepresentationType, username,
          markAsPreservation);
      return baseRepresentationId;
    }
  }

  private void createNewRepresentation(ModelService model, String aipId, String representationId, String type,
      String username, boolean markAsPreservation) throws RequestNotValidException, GenericException,
      AuthorizationDeniedException, NotFoundException, AlreadyExistsException {
    boolean original = false;
    List<String> state = Collections.emptyList();
    if (markAsPreservation) {
      state = List.of("PRESERVATION");
    }

    model.createRepresentation(aipId, representationId, original, type, true, username, state);
  }

  private String getOutputMessage(CommandException exception) {
    if (StringUtils.isNotEmpty(exception.getOutput())) {
      return String.format("%s%n%s", exception.getMessage(), exception.getOutput());
    }

    return exception.getMessage();
  }

  @Override
  public PreservationEventType getPreservationEventType() {
    return PreservationEventType.MIGRATION;
  }

  @Override
  public String getPreservationEventDescription() {
    return "Converted, if possible, files to a new format (" + outputFormat + ").";
  }

  @Override
  public String getPreservationEventSuccessMessage() {
    return "Files were successfully converted to a new format.";
  }

  @Override
  public String getPreservationEventFailureMessage() {
    return "File conversion failed.";
  }

  @Override
  public List<String> getCategories() {
    return Arrays.asList(RodaConstants.PLUGIN_CATEGORY_CONVERSION, RodaConstants.PLUGIN_CATEGORY_DISSEMINATION);
  }

  public Path getWorkingDirectory() {
    return PluginHelper.getJobWorkingDirectory(this);
  }
}

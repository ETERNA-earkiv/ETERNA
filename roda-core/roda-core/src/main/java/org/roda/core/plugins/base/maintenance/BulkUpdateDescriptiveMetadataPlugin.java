/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.plugins.base.maintenance;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.io.IOUtils;
import org.roda.core.RodaCoreFactory;
import org.roda.core.common.access.AccessRulesModelObserver;
import org.roda.core.common.metadata.DescriptiveMetadataFieldUpdater;
import org.roda.core.common.metadata.DescriptiveMetadataFieldUpdater.Result;
import org.roda.core.common.metadata.DescriptiveMetadataFieldUpdater.UpdateMode;
import org.roda.core.common.validation.ValidationUtils;
import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.common.RodaConstants.PreservationEventType;
import org.roda.core.data.common.RodaConstants.VersionAction;
import org.roda.core.data.exceptions.GenericException;
import org.roda.core.data.exceptions.InvalidParameterException;
import org.roda.core.data.v2.LiteOptionalWithCause;
import org.roda.core.data.v2.ip.AIP;
import org.roda.core.data.v2.ip.AIPState;
import org.roda.core.data.v2.ip.metadata.DescriptiveMetadata;
import org.roda.core.data.v2.jobs.Job;
import org.roda.core.data.v2.jobs.PluginParameter;
import org.roda.core.data.v2.jobs.PluginParameter.PluginParameterType;
import org.roda.core.data.v2.jobs.PluginState;
import org.roda.core.data.v2.jobs.PluginType;
import org.roda.core.data.v2.jobs.Report;
import org.roda.core.data.v2.validation.ValidationReport;
import org.roda.core.index.IndexService;
import org.roda.core.model.ModelService;
import org.roda.core.plugins.AbstractPlugin;
import org.roda.core.plugins.Plugin;
import org.roda.core.plugins.PluginException;
import org.roda.core.plugins.PluginHelper;
import org.roda.core.plugins.RODAObjectsProcessingLogic;
import org.roda.core.plugins.orchestrate.JobPluginInfo;
import org.roda.core.storage.Binary;
import org.roda.core.storage.StringContentPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Massredigering av ett specifikt fält i beskrivande
 * metadata för ett obegränsat antal AIP:ar, baserat på urvalskriterier.
 *
 * Skrivstrategi: XPath-baserad in-place-uppdatering via
 * {@link DescriptiveMetadataFieldUpdater} — aldrig mallrendering (ej
 * förlustfri). Xpath resolvas alltid server-side ur formulärmallen (hård
 * vitlista). Spårbarhet per objekt: binärversion med VersionAction
 * BULK_UPDATED + job-id, PREMIS-event METADATA_MODIFICATION med gammalt och
 * nytt värde, XSD-validering före skrivning. Objektlås per AIP hanteras av
 * PluginHelper.processObjects (autoLocking).
 */
public class BulkUpdateDescriptiveMetadataPlugin extends AbstractPlugin<AIP> {
  private static final Logger LOGGER = LoggerFactory.getLogger(BulkUpdateDescriptiveMetadataPlugin.class);
  private static final String EVENT_DESCRIPTION = "Massredigering: uppdatering av ett specifikt fält i beskrivande "
    + "metadata direkt i dokumentet (utan omrendering) för ett urval av strukturenheter";

  private static final Map<String, PluginParameter> pluginParameters = new HashMap<>();
  static {
    pluginParameters.put(RodaConstants.PLUGIN_PARAMS_BULK_FIELD_NAME,
      PluginParameter.getBuilder(RodaConstants.PLUGIN_PARAMS_BULK_FIELD_NAME, "Fältnamn", PluginParameterType.STRING)
        .isMandatory(true)
        .withDescription("Fältets namn så som det deklareras i formulärmallen, t.ex. \"sekretess\" eller \"title\". "
          + "Fältets xpath resolvas alltid server-side ur mallen — fält utanför mallen kan inte uppdateras. "
          + "Vid fel fältnamn listar jobbrapporten de tillåtna fälten.")
        .build());
    pluginParameters.put(RodaConstants.PLUGIN_PARAMS_BULK_NEW_VALUE,
      PluginParameter.getBuilder(RodaConstants.PLUGIN_PARAMS_BULK_NEW_VALUE, "Nytt värde", PluginParameterType.STRING)
        .isMandatory(true)
        .withDescription("Nytt värde för fältet. För fält med kontrollerad vokabulär anges det interna värdet, "
          + "t.ex. \"sekretess_forekommer\" — resultatet XSD-valideras före skrivning, så ogiltiga värden avvisas.")
        .build());
    pluginParameters.put(RodaConstants.PLUGIN_PARAMS_BULK_EXPECTED_VALUE,
      PluginParameter
        .getBuilder(RodaConstants.PLUGIN_PARAMS_BULK_EXPECTED_VALUE, "Villkor: förväntat nuvarande värde",
          PluginParameterType.STRING)
        .isMandatory(false)
        .withDescription("Valfritt villkor: uppdatera endast objekt där fältets nuvarande värde är exakt detta. "
          + "Objekt som inte uppfyller villkoret hoppas över med orsak i jobbrapporten.")
        .build());
  }

  private String metadataTypeWithVersion = null;
  private String fieldName = null;
  private String newValue = null;
  private UpdateMode mode = UpdateMode.OVERWRITE;
  private String expectedValue = null;

  /** Skrivbar xpath, resolvad ur mallen — cache:as vid första användning. */
  private String resolvedXpath = null;

  @Override
  public void init() throws PluginException {
    // do nothing
  }

  @Override
  public void shutdown() {
    // do nothing
  }

  @Override
  public String getName() {
    return "Massredigering av metadatafält";
  }

  @Override
  public String getDescription() {
    return "Uppdaterar ett specifikt fält i beskrivande metadata för alla valda strukturenheter (AIP:ar). "
      + "Fältets xpath resolvas ur formulärmallen, uppdateringen sker direkt i dokumentet utan omrendering "
      + "(förlustfritt — övrigt innehåll bevaras oförändrat), resultatet valideras mot XML-schemat före "
      + "skrivning, och varje ändring är spårbar via en binärversion (åtgärd \"Massuppdaterad\" med jobb-id) "
      + "och en bevarandehändelse med gammalt och nytt värde. Objekt som inte uppdateras hoppas över med "
      + "orsak i jobbrapporten.";
  }

  @Override
  public String getVersionImpl() {
    return "1.0";
  }

  @Override
  public List<PluginParameter> getParameters() {
    ArrayList<PluginParameter> parameters = new ArrayList<>();
    // Metadatatyp och läge byggs vid anrop: typlistan som rullgardin över de
    // konfigurerade typerna (inkl. drift-överlagrade) i stället för fritext
    parameters.add(PluginParameter
      .getBuilder(RodaConstants.PLUGIN_PARAMS_BULK_METADATA_TYPE, "Metadatatyp", PluginParameterType.DROPDOWN)
      .withPossibleValues(RodaCoreFactory.getRodaConfigurationAsList("ui.browser.metadata.descriptive.types"))
      .isMandatory(true)
      .withDescription("Beskrivande metadatatyp (inklusive version) vars fält ska uppdateras, "
        + "t.ex. \"cits_erms_3\" eller \"eterna_admin\"")
      .build());
    parameters.add(pluginParameters.get(RodaConstants.PLUGIN_PARAMS_BULK_FIELD_NAME));
    parameters.add(pluginParameters.get(RodaConstants.PLUGIN_PARAMS_BULK_NEW_VALUE));
    parameters.add(PluginParameter
      .getBuilder(RodaConstants.PLUGIN_PARAMS_BULK_MODE, "Läge", PluginParameterType.DROPDOWN)
      .withPossibleValues(List.of("overwrite", "complete")).isMandatory(false).withDefaultValue("overwrite")
      .withDescription("\"overwrite\" skriver alltid; \"complete\" (komplettering) skriver aldrig över "
        + "ett icke-tomt värde")
      .build());
    parameters.add(pluginParameters.get(RodaConstants.PLUGIN_PARAMS_BULK_EXPECTED_VALUE));
    return parameters;
  }

  @Override
  public void setParameterValues(Map<String, String> parameters) throws InvalidParameterException {
    super.setParameterValues(parameters);
    metadataTypeWithVersion = parameters.get(RodaConstants.PLUGIN_PARAMS_BULK_METADATA_TYPE);
    fieldName = parameters.get(RodaConstants.PLUGIN_PARAMS_BULK_FIELD_NAME);
    newValue = parameters.get(RodaConstants.PLUGIN_PARAMS_BULK_NEW_VALUE);
    String modeParameter = parameters.get(RodaConstants.PLUGIN_PARAMS_BULK_MODE);
    if (modeParameter != null && !modeParameter.isBlank()) {
      try {
        mode = UpdateMode.valueOf(modeParameter.trim().toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException e) {
        throw new InvalidParameterException("Invalid mode '" + modeParameter + "', expected overwrite or complete");
      }
    }
    String expected = parameters.get(RodaConstants.PLUGIN_PARAMS_BULK_EXPECTED_VALUE);
    expectedValue = expected == null || expected.isBlank() ? null : expected;
  }

  @Override
  public boolean areParameterValuesValid() {
    return metadataTypeWithVersion != null && !metadataTypeWithVersion.isBlank() && fieldName != null
      && !fieldName.isBlank() && newValue != null && !newValue.isBlank();
  }

  /**
   * Gallrade AIP:er, AIP:er under bedömning eller bearbetning och AIP:er som
   * ingår i en gallringsbekräftelse får inte massredigeras.
   *
   * @return orsaken till att AIP:n inte får redigeras, eller null om den får det
   */
  static String notEditableReason(AIP aip) {
    if (aip.getState() != AIPState.ACTIVE) {
      return "AIP:n har tillståndet " + aip.getState() + " och kan inte massredigeras";
    }
    if (aip.getDisposalConfirmationId() != null && !aip.getDisposalConfirmationId().isBlank()) {
      return "AIP:n ingår i gallringsbekräftelsen " + aip.getDisposalConfirmationId() + " och kan inte massredigeras";
    }
    return null;
  }

  @Override
  public Report execute(IndexService index, ModelService model, List<LiteOptionalWithCause> liteList)
    throws PluginException {
    return PluginHelper.processObjects(this,
      (RODAObjectsProcessingLogic<AIP>) (index1, model1, report, cachedJob, jobPluginInfo, plugin,
        objects) -> processAIPs(model1, report, jobPluginInfo, cachedJob, objects),
      index, model, liteList);
  }

  private void processAIPs(ModelService model, Report report, JobPluginInfo jobPluginInfo, Job job, List<AIP> aips) {
    String xpath;
    try {
      xpath = getResolvedXpath();
    } catch (GenericException e) {
      // Ogiltig typ/fält gäller hela jobbet — fela ut samtliga objekt med orsak
      for (AIP aip : aips) {
        Report reportItem = PluginHelper.initPluginReportItem(this, aip.getId(), AIP.class);
        reportItem.setPluginState(PluginState.FAILURE).setPluginDetails(e.getMessage());
        jobPluginInfo.incrementObjectsProcessedWithFailure();
        report.addReport(reportItem);
        PluginHelper.updatePartialJobReport(this, model, reportItem, true, job);
      }
      return;
    }

    String type = metadataTypeWithVersion;
    String version = null;
    if (metadataTypeWithVersion.contains(RodaConstants.METADATA_VERSION_SEPARATOR)) {
      version = metadataTypeWithVersion
        .substring(metadataTypeWithVersion.lastIndexOf(RodaConstants.METADATA_VERSION_SEPARATOR) + 1);
      type = metadataTypeWithVersion.substring(0,
        metadataTypeWithVersion.lastIndexOf(RodaConstants.METADATA_VERSION_SEPARATOR));
    }

    // Varje metadatauppdatering notifierar observern för åtkomstreglerna, som
    // annars skulle starta ett efterkommande-jobb per AIP; samla dem till ett
    // jobb per batch
    final String resolvedType = type;
    final String resolvedVersion = version;
    AccessRulesModelObserver.deferDescendantsJobs(() -> {
      for (AIP aip : aips) {
        processAIP(model, report, jobPluginInfo, job, aip, resolvedType, resolvedVersion, xpath);
      }
    });
  }

  private void processAIP(ModelService model, Report report, JobPluginInfo jobPluginInfo, Job job, AIP aip,
    String type, String version, String xpath) {
    LOGGER.debug("Processing AIP {}", aip.getId());
    Report reportItem = PluginHelper.initPluginReportItem(this, aip.getId(), AIP.class);
    PluginHelper.updatePartialJobReport(this, model, reportItem, false, job);
    PluginState state;
    String details = "";
    String oldValueForEvent = null;

    try {
      String notEditableReason = notEditableReason(aip);
      DescriptiveMetadata metadata = notEditableReason == null ? findDescriptiveMetadata(aip, type, version) : null;
      if (notEditableReason != null) {
        state = PluginState.SKIPPED;
        details = notEditableReason;
      } else if (metadata == null) {
        state = PluginState.SKIPPED;
        details = "Objektet saknar beskrivande metadata av typen '" + metadataTypeWithVersion + "'";
      } else {
        Binary binary = model.retrieveDescriptiveMetadataBinary(aip.getId(), metadata.getId());
        String xml;
        try (InputStream stream = binary.getContent().createInputStream()) {
          xml = IOUtils.toString(stream, StandardCharsets.UTF_8);
        }

        Result result = DescriptiveMetadataFieldUpdater.update(xml, xpath, newValue, mode, expectedValue);
        oldValueForEvent = result.getOldValue();

        switch (result.getStatus()) {
          case UPDATED:
            StringContentPayload payload = new StringContentPayload(result.getNewXml());
            ValidationReport validationReport = ValidationUtils.validateDescriptiveBinary(payload, type, version,
              false);
            if (!validationReport.isValid()) {
              state = PluginState.FAILURE;
              details = "Det uppdaterade dokumentet klarade inte XSD-valideringen och skrevs INTE: "
                + validationReport.getIssues().toString();
            } else {
              Map<String, String> properties = new HashMap<>();
              properties.put(RodaConstants.VERSION_ACTION, VersionAction.BULK_UPDATED.toString());
              properties.put(RodaConstants.VERSION_USER, job.getUsername());
              properties.put(RodaConstants.VERSION_MESSAGE,
                "Massredigering av fältet '" + fieldName + "' (jobb " + job.getId() + ")");
              model.updateDescriptiveMetadata(aip.getId(), metadata.getId(), payload, type, version, properties,
                job.getUsername());
              state = PluginState.SUCCESS;
              details = "Fältet '" + fieldName + "' uppdaterat från '" + valueOrEmpty(result.getOldValue())
                + "' till '" + newValue + "'";
            }
            break;
          case SKIPPED_NODE_MISSING:
            state = PluginState.SKIPPED;
            details = "Fältet '" + fieldName + "' finns inte i dokumentet (noden saknas)";
            break;
          case SKIPPED_ALREADY_FILLED:
            state = PluginState.SKIPPED;
            details = "Fältet '" + fieldName + "' har redan ett värde och läget är komplettering (complete)";
            break;
          case SKIPPED_CONDITION_NOT_MET:
            state = PluginState.SKIPPED;
            details = "Nuvarande värde '" + valueOrEmpty(result.getOldValue())
              + "' uppfyller inte villkoret (förväntat: '" + expectedValue + "')";
            break;
          case SKIPPED_VALUE_UNCHANGED:
          default:
            state = PluginState.SKIPPED;
            details = "Fältet '" + fieldName + "' har redan det begärda värdet";
            break;
        }
      }
    } catch (Exception e) {
      state = PluginState.FAILURE;
      details = "Fel vid uppdatering av AIP " + aip.getId() + ": " + e.getMessage();
      LOGGER.error("Error processing AIP {}: {}", aip.getId(), e.getMessage(), e);
    }

    jobPluginInfo.incrementObjectsProcessed(state);
    reportItem.setPluginState(state).setPluginDetails(details);

    StringBuilder outcomeText = new StringBuilder().append("Massredigering av metadatafältet '").append(fieldName)
      .append("' (typ '").append(metadataTypeWithVersion).append("') på AIP '").append(aip.getId()).append("': ")
      .append(state);
    if (PluginState.SUCCESS.equals(state)) {
      outcomeText.append(". Gammalt värde: '").append(valueOrEmpty(oldValueForEvent)).append("', nytt värde: '")
        .append(newValue).append("'");
    } else {
      outcomeText.append(". ").append(details);
    }

    if (!PluginState.SKIPPED.equals(state)) {
      model.createUpdateAIPEvent(aip.getId(), null, null, null, PreservationEventType.METADATA_MODIFICATION,
        EVENT_DESCRIPTION, state, outcomeText.toString(), details, job.getUsername(), true, null);
    }
    report.addReport(reportItem);
    PluginHelper.updatePartialJobReport(this, model, reportItem, true, job);
  }

  private DescriptiveMetadata findDescriptiveMetadata(AIP aip, String type, String version) {
    for (DescriptiveMetadata metadata : aip.getDescriptiveMetadata()) {
      if (Objects.equals(type, metadata.getType()) && Objects.equals(version, metadata.getVersion())) {
        return metadata;
      }
    }
    return null;
  }

  /**
   * Resolvar fältets skrivbara xpath ur formulärmallen (hård vitlista — fält
   * utanför mallen kan inte uppdateras). Cache:as per plugininstans.
   */
  private synchronized String getResolvedXpath() throws GenericException {
    if (resolvedXpath != null) {
      return resolvedXpath;
    }
    String templatePath = RodaConstants.METADATA_TEMPLATE_FOLDER + "/" + metadataTypeWithVersion
      + RodaConstants.METADATA_TEMPLATE_EXTENSION;
    try (InputStream templateStream = RodaCoreFactory.getConfigurationFileAsStream(templatePath)) {
      if (templateStream == null) {
        throw new GenericException("Ingen formulärmall hittades för typen '" + metadataTypeWithVersion + "' ("
          + templatePath + ") — fältets xpath kan inte resolvas");
      }
      String template = IOUtils.toString(templateStream, StandardCharsets.UTF_8);
      Map<String, String> fieldXpaths = DescriptiveMetadataFieldUpdater.parseTemplateFieldXpaths(template);
      String templateXpath = fieldXpaths.get(fieldName);
      if (templateXpath == null) {
        throw new GenericException("Fältet '" + fieldName + "' är inte deklarerat med xpath i mallen '" + templatePath
          + "' — tillåtna fält: " + fieldXpaths.keySet());
      }
      String writable = DescriptiveMetadataFieldUpdater.toWritableXpath(templateXpath);
      DescriptiveMetadataFieldUpdater.validateXpath(writable);
      resolvedXpath = writable;
      return resolvedXpath;
    } catch (IOException e) {
      throw new GenericException("Kunde inte läsa formulärmallen '" + templatePath + "'", e);
    }
  }

  private static String valueOrEmpty(String value) {
    return value == null ? "" : value;
  }

  @Override
  public Report beforeAllExecute(IndexService index, ModelService model) throws PluginException {
    return new Report();
  }

  @Override
  public Report afterAllExecute(IndexService index, ModelService model) throws PluginException {
    return new Report();
  }

  @Override
  public Plugin<AIP> cloneMe() {
    return new BulkUpdateDescriptiveMetadataPlugin();
  }

  /**
   * Massredigering är en användarinitierad arkivvårdsåtgärd och ska därför
   * listas under Bevarande → Åtgärder (som t.ex. ExportAIPPlugin och
   * FixAncestorsPlugin) och kunna väljas i "Nytt jobb". Med INTERNAL hamnar
   * jobben enbart under Administration → Interna åtgärder, eftersom
   * ActionProcess filtrerar bort INTERNAL/INGEST och CreateDefaultJob
   * utesluter INTERNAL.
   */
  @Override
  public PluginType getType() {
    return PluginType.AIP_TO_AIP;
  }

  @Override
  public PreservationEventType getPreservationEventType() {
    return PreservationEventType.METADATA_MODIFICATION;
  }

  @Override
  public String getPreservationEventDescription() {
    return "Uppdaterar ett specifikt fält i beskrivande metadata direkt i dokumentet";
  }

  @Override
  public String getPreservationEventSuccessMessage() {
    return "Metadatafältet uppdaterades";
  }

  @Override
  public String getPreservationEventFailureMessage() {
    return "Metadatafältet kunde inte uppdateras";
  }

  @Override
  public List<String> getCategories() {
    return Collections.singletonList(RodaConstants.PLUGIN_CATEGORY_MAINTENANCE);
  }

  @Override
  public List<Class<AIP>> getObjectClasses() {
    return Collections.singletonList(AIP.class);
  }
}

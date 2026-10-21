/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.plugins.base.ingest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.roda.core.common.deliveryspec.DeliverySpec;
import org.roda.core.common.deliveryspec.DeliverySpecRegistry;
import org.roda.core.common.validation.ValidationUtils;
import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.common.RodaConstants.PreservationEventType;
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
import org.roda.core.data.v2.validation.ValidationIssue;
import org.roda.core.data.v2.validation.ValidationReport;
import org.roda.core.index.IndexService;
import org.roda.core.model.ModelService;
import org.roda.core.plugins.AbstractPlugin;
import org.roda.core.plugins.Plugin;
import org.roda.core.plugins.PluginException;
import org.roda.core.plugins.PluginHelper;
import org.roda.core.plugins.RODAObjectProcessingLogic;
import org.roda.core.plugins.orchestrate.JobPluginInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validering av inleverans mot en leveransspecifikation.
 *
 * En leveransspecifikation är ett konfigurationspaket (XSD + crosswalks +
 * formulärmall + registerpost i core.deliveryspec.*). Pluginet körs som
 * valbart ingeststeg och gör per AIP:
 *
 * 1. requireMetadata-kontroll — kräver specen metadata och AIP:en saknar
 *    beskrivande metadata av specens typ ⇒ FAILURE.
 * 2. XSD-validering med failIfNoSchema=true — åtgärdar hålet där en felaktig
 *    metadatatyp annars bara ger wellformedness-kontroll: saknas XSD för
 *    specens typ är det ett konfigurationsfel som ska synas, inte döljas.
 * 3. Valideringsutfall med radnummer i jobbrapporten och PREMIS-event
 *    (VALIDATION) med spec-id i utfallet.
 *
 * Explicit val av spec görs med jobparametern delivery_spec.id (dropdown
 * över registret). Auto-detektering via namespace och Schematron-regler är
 * ännu inte implementerade.
 */
public class DeliverySpecValidationPlugin extends AbstractPlugin<AIP> {
  private static final Logger LOGGER = LoggerFactory.getLogger(DeliverySpecValidationPlugin.class);
  private static final String EVENT_DESCRIPTION = "Validering av en strukturenhet mot en konfigurerad "
    + "leveransspecifikation";

  private String specId = null;

  public static String getStaticName() {
    return "Validering mot leveransspecifikation";
  }

  public static String getStaticDescription() {
    return "Kontrollerar att strukturenheten uppfyller en konfigurerad leveransspecifikation: beskrivande "
      + "metadata av specifikationens typ finns och är giltig mot dess XML-schema. Resultatet, inklusive "
      + "radnummer för schemafel, registreras i jobbrapporten och som en bevarandehändelse med "
      + "leveransspecifikationens identifierare.";
  }

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
    return getStaticName();
  }

  @Override
  public String getDescription() {
    return getStaticDescription();
  }

  @Override
  public String getVersionImpl() {
    return "1.0";
  }

  @Override
  public List<PluginParameter> getParameters() {
    ArrayList<PluginParameter> parameters = new ArrayList<>();
    parameters.add(buildSpecIdParameter());
    return parameters;
  }

  /** Dropdown över registrets spec-id:n — byggs vid anrop så att drift-
   * överlagrade registerposter syns utan omstart av pluginregistret. */
  public static PluginParameter buildSpecIdParameter() {
    return PluginParameter
      .getBuilder(RodaConstants.PLUGIN_PARAMS_DELIVERY_SPEC_ID, "Leveransspecifikation", PluginParameterType.DROPDOWN)
      .withPossibleValues(DeliverySpecRegistry.getSpecIds()).isMandatory(true)
      .withDescription("Identifierare för den leveransspecifikation som leveransen ska valideras mot "
        + "(konfigureras i core.deliveryspec.*, se registret över leveransspecifikationer)")
      .build();
  }

  @Override
  public void setParameterValues(Map<String, String> parameters) throws InvalidParameterException {
    super.setParameterValues(parameters);
    specId = parameters.get(RodaConstants.PLUGIN_PARAMS_DELIVERY_SPEC_ID);
  }

  @Override
  public boolean areParameterValuesValid() {
    return specId != null && !specId.isBlank();
  }

  @Override
  public Report execute(IndexService index, ModelService model, List<LiteOptionalWithCause> liteList)
    throws PluginException {
    return PluginHelper.processObjects(this, (RODAObjectProcessingLogic<AIP>) (index1, model1, report, cachedJob,
      jobPluginInfo, plugin, object) -> processAIP(index1, model1, report, cachedJob, jobPluginInfo, object), index,
      model, liteList);
  }

  private void processAIP(IndexService index, ModelService model, Report report, Job cachedJob,
    JobPluginInfo jobPluginInfo, AIP aip) {
    LOGGER.debug("Validating AIP {} against delivery specification '{}'", aip.getId(), specId);
    Report reportItem = PluginHelper.initPluginReportItem(this, aip.getId(), AIP.class, AIPState.INGEST_PROCESSING);
    PluginHelper.updatePartialJobReport(this, model, reportItem, false, cachedJob);

    PluginState state;
    String details;

    Optional<DeliverySpec> maybeSpec = DeliverySpecRegistry.getSpec(specId);
    if (maybeSpec.isEmpty()) {
      state = PluginState.FAILURE;
      details = "Leveransspecifikationen '" + specId + "' är inte konfigurerad (core.deliveryspec.*). "
        + "Tillgängliga specifikationer: " + DeliverySpecRegistry.getSpecIds();
    } else {
      DeliverySpec spec = maybeSpec.get();
      DescriptiveMetadata metadata = findDescriptiveMetadata(aip, spec);
      if (metadata == null) {
        if (spec.isRequireMetadata()) {
          state = PluginState.FAILURE;
          details = "Leveransspecifikationen '" + spec.getId() + "' kräver beskrivande metadata av typen '"
            + spec.getMetadataTypeWithVersion() + "', men paketet saknar sådan";
        } else {
          state = PluginState.SUCCESS;
          details = "Paketet saknar beskrivande metadata av typen '" + spec.getMetadataTypeWithVersion()
            + "' och leveransspecifikationen kräver inte sådan. Inget att validera";
        }
      } else {
        ValidationReport validationReport = validate(model, aip, metadata, spec);
        if (validationReport.isValid()) {
          state = PluginState.SUCCESS;
          details = "Beskrivande metadata '" + metadata.getId() + "' är giltig mot leveransspecifikationen '"
            + spec.getId() + "'";
        } else {
          state = PluginState.FAILURE;
          details = "Beskrivande metadata '" + metadata.getId() + "' bryter mot leveransspecifikationen '"
            + spec.getId() + "': " + formatIssues(validationReport);
        }
      }
    }

    if (PluginState.SUCCESS.equals(state)) {
      jobPluginInfo.incrementObjectsProcessedWithSuccess();
    } else {
      jobPluginInfo.incrementObjectsProcessedWithFailure();
    }
    reportItem.setPluginState(state).setPluginDetails(details);

    try {
      PluginHelper.createPluginEvent(this, aip.getId(), model, index, state,
        "Leveransspecifikation: '" + specId + "'. " + details, true, cachedJob);
    } catch (Exception e) {
      LOGGER.error("Could not create preservation event for AIP {}: {}", aip.getId(), e.getMessage(), e);
    }

    report.addReport(reportItem);
    PluginHelper.updatePartialJobReport(this, model, reportItem, true, cachedJob);
  }

  private DescriptiveMetadata findDescriptiveMetadata(AIP aip, DeliverySpec spec) {
    for (DescriptiveMetadata metadata : aip.getDescriptiveMetadata()) {
      if (Objects.equals(spec.getMetadataType(), metadata.getType())
        && Objects.equals(spec.getMetadataVersion(), metadata.getVersion())) {
        return metadata;
      }
    }
    return null;
  }

  private ValidationReport validate(ModelService model, AIP aip, DescriptiveMetadata metadata, DeliverySpec spec) {
    try {
      org.roda.core.storage.Binary binary = model.retrieveDescriptiveMetadataBinary(aip.getId(), metadata.getId());
      // failIfNoSchema=true: saknad XSD för specens typ är ett
      // konfigurationsfel som ska ge FAILURE, inte tyst wellformedness
      return ValidationUtils.validateDescriptiveBinary(binary.getContent(), spec.getMetadataType(),
        spec.getMetadataVersion(), true);
    } catch (Exception e) {
      ValidationReport errorReport = new ValidationReport();
      errorReport.setValid(false);
      errorReport.setMessage("Could not read descriptive metadata '" + metadata.getId() + "': " + e.getMessage());
      return errorReport;
    }
  }

  private static String formatIssues(ValidationReport validationReport) {
    if (validationReport.getIssues() == null || validationReport.getIssues().isEmpty()) {
      return validationReport.getMessage() == null ? "inga detaljer tillgängliga" : validationReport.getMessage();
    }
    StringBuilder builder = new StringBuilder();
    for (ValidationIssue issue : validationReport.getIssues()) {
      if (builder.length() > 0) {
        builder.append("; ");
      }
      builder.append("rad ").append(issue.getLineNumber()).append(": ").append(issue.getMessage());
    }
    return builder.toString();
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
    return new DeliverySpecValidationPlugin();
  }

  @Override
  public PluginType getType() {
    return PluginType.AIP_TO_AIP;
  }

  @Override
  public PreservationEventType getPreservationEventType() {
    return PreservationEventType.VALIDATION;
  }

  @Override
  public String getPreservationEventDescription() {
    return EVENT_DESCRIPTION;
  }

  @Override
  public String getPreservationEventSuccessMessage() {
    return "The intellectual entity fulfils the delivery specification";
  }

  @Override
  public String getPreservationEventFailureMessage() {
    return "The intellectual entity does not fulfil the delivery specification";
  }

  @Override
  public List<String> getCategories() {
    return Collections.singletonList(RodaConstants.PLUGIN_CATEGORY_VALIDATION);
  }

  @Override
  public List<Class<AIP>> getObjectClasses() {
    return Collections.singletonList(AIP.class);
  }
}

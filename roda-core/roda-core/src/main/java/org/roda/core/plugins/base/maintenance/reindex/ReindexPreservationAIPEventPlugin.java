/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.plugins.base.maintenance.reindex;

import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.common.RodaConstants.PreservationEventType;
import org.roda.core.data.exceptions.AuthorizationDeniedException;
import org.roda.core.data.exceptions.GenericException;
import org.roda.core.data.exceptions.InvalidParameterException;
import org.roda.core.data.v2.LiteOptionalWithCause;
import org.roda.core.data.v2.ip.AIP;
import org.roda.core.data.v2.jobs.Job;
import org.roda.core.data.v2.jobs.PluginParameter;
import org.roda.core.data.v2.jobs.PluginParameter.PluginParameterType;
import org.roda.core.data.v2.jobs.PluginState;
import org.roda.core.data.v2.jobs.PluginType;
import org.roda.core.data.v2.jobs.Report;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReindexPreservationAIPEventPlugin extends AbstractPlugin<AIP> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReindexPreservationAIPEventPlugin.class);
  private boolean clearIndexes = false;
  private boolean optimizeIndexes = true;

  private static Map<String, PluginParameter> pluginParameters = new HashMap<>();
  static {
    pluginParameters.put(RodaConstants.PLUGIN_PARAMS_CLEAR_INDEXES,
      PluginParameter.getBuilder(RodaConstants.PLUGIN_PARAMS_CLEAR_INDEXES, "Rensa index", PluginParameterType.BOOLEAN)
        .withDefaultValue("false").isMandatory(false).withDescription("Rensar alla index innan återindexering.")
        .build());

    pluginParameters.put(RodaConstants.PLUGIN_PARAMS_OPTIMIZE_INDEXES,
      PluginParameter
        .getBuilder(RodaConstants.PLUGIN_PARAMS_OPTIMIZE_INDEXES, "Optimera index", PluginParameterType.BOOLEAN)
        .withDefaultValue("true").isMandatory(false).withDescription("Optimerar index efter återindexering.").build());
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
    return "Återindexera bevarandehändelseindex för AIP";
  }

  @Override
  public String getDescription() {
    return "Rensar indexet och återskapar det från faktisk fysisk data som finns i lagringen. Denna uppgift syftar till att åtgärda inkonsekvenser "
      + "mellan vad som visas i arkivets grafiska gränssnitt och vad som faktiskt finns i lagringsskiktet. Sådana "
      + "inkonsekvenser kan uppstå av olika anledningar, t.ex. indexkorruption, ovarsam avstängning av arkivet, etc.";
  }

  @Override
  public String getVersionImpl() {
    return "1.0";
  }

  @Override
  public List<PluginParameter> getParameters() {
    ArrayList<PluginParameter> parameters = new ArrayList<>();
    parameters.add(pluginParameters.get(RodaConstants.PLUGIN_PARAMS_CLEAR_INDEXES));
    parameters.add(pluginParameters.get(RodaConstants.PLUGIN_PARAMS_OPTIMIZE_INDEXES));
    return parameters;
  }

  @Override
  public void setParameterValues(Map<String, String> parameters) throws InvalidParameterException {
    super.setParameterValues(parameters);

    if (parameters != null && parameters.containsKey(RodaConstants.PLUGIN_PARAMS_CLEAR_INDEXES)) {
      clearIndexes = Boolean.parseBoolean(parameters.get(RodaConstants.PLUGIN_PARAMS_CLEAR_INDEXES));
    }

    if (parameters != null && parameters.containsKey(RodaConstants.PLUGIN_PARAMS_OPTIMIZE_INDEXES)) {
      optimizeIndexes = Boolean.parseBoolean(parameters.get(RodaConstants.PLUGIN_PARAMS_OPTIMIZE_INDEXES));
    }
  }

  @Override
  public Report execute(IndexService index, ModelService model, List<LiteOptionalWithCause> liteList)
    throws PluginException {
    return PluginHelper.processObjects(this, new RODAObjectProcessingLogic<AIP>() {
      @Override
      public void process(IndexService index, ModelService model, Report report, Job cachedJob,
        JobPluginInfo jobPluginInfo, Plugin<AIP> plugin, AIP object) {
        processAIP(index, report, jobPluginInfo, object);
      }
    }, index, model, liteList);
  }

  public Report processAIP(IndexService index, Report report, JobPluginInfo jobPluginInfo, AIP aip) {
    try {
      index.reindexAIPPreservationEvents(aip);
      report.setPluginState(PluginState.SUCCESS);
      jobPluginInfo.incrementObjectsProcessedWithSuccess();
    } catch (Exception e) {
      report.setPluginState(PluginState.FAILURE);
      jobPluginInfo.incrementObjectsProcessedWithFailure();
    }

    return report;
  }

  @Override
  public Report beforeAllExecute(IndexService index, ModelService model) throws PluginException {
    if (clearIndexes) {
      LOGGER.debug("Clearing indexes");
      try {
        index.clearAIPEventIndex();
      } catch (GenericException | AuthorizationDeniedException e) {
        throw new PluginException("Error clearing index", e);
      }
    } else {
      LOGGER.debug("Skipping clear indexes");
    }

    return new Report();
  }

  @Override
  public Report afterAllExecute(IndexService index, ModelService model) throws PluginException {
    if (optimizeIndexes) {
      LOGGER.debug("Optimizing indexes");
      try {
        index.optimizeIndex(RodaConstants.INDEX_PRESERVATION_EVENTS);
      } catch (GenericException | AuthorizationDeniedException e) {
        throw new PluginException("Error optimizing index", e);
      }
    }

    return new Report();
  }

  @Override
  public Plugin<AIP> cloneMe() {
    return new ReindexPreservationAIPEventPlugin();
  }

  @Override
  public PluginType getType() {
    return PluginType.MISC;
  }

  @Override
  public boolean areParameterValuesValid() {
    return true;
  }

  @Override
  public PreservationEventType getPreservationEventType() {
    return PreservationEventType.NONE;
  }

  @Override
  public String getPreservationEventDescription() {
    return "Reindex ETERNA entity";
  }

  @Override
  public String getPreservationEventSuccessMessage() {
    return "All entities were reindexed with success";
  }

  @Override
  public String getPreservationEventFailureMessage() {
    return "An error occured while reindexing all entities";
  }

  @Override
  public List<String> getCategories() {
    return Arrays.asList(RodaConstants.PLUGIN_CATEGORY_REINDEX);
  }

  @Override
  public List<Class<AIP>> getObjectClasses() {
    return Arrays.asList(AIP.class);
  }

}

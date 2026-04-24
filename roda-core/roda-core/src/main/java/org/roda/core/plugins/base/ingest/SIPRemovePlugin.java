/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/keeps/roda
 */
package org.roda.core.plugins.base.ingest;

import java.util.Arrays;
import java.util.List;

import org.roda.core.RodaCoreFactory;
import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.common.RodaConstants.PreservationEventType;
import org.roda.core.data.exceptions.AuthorizationDeniedException;
import org.roda.core.data.exceptions.GenericException;
import org.roda.core.data.v2.LiteOptionalWithCause;
import org.roda.core.data.v2.ip.TransferredResource;
import org.roda.core.data.v2.jobs.Job;
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

public class SIPRemovePlugin extends AbstractPlugin<TransferredResource> {
  private static final Logger LOGGER = LoggerFactory.getLogger(SIPRemovePlugin.class);

  private boolean createEvent = true;

  @Override
  public void init() throws PluginException {
    // do nothing
  }

  @Override
  public void shutdown() {
    // do nothing
  }

  public static String getStaticName() {
    return "Radera SIP från överföringsyta";
  }

  @Override
  public String getName() {
    return getStaticName();
  }

  public static String getStaticDescription() {
    return "Raderar SIP:et från överföringsytan om inleveransprocessen lyckades.";
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
  public Report execute(IndexService index, ModelService model,
    List<LiteOptionalWithCause> liteList) throws PluginException {
    return PluginHelper.processObjects(this, new RODAObjectProcessingLogic<TransferredResource>() {
      @Override
      public void process(IndexService index, ModelService model, Report report, Job cachedJob,
        JobPluginInfo jobPluginInfo, Plugin<TransferredResource> plugin, TransferredResource object) {
        processTransferredResource(model, report, jobPluginInfo, cachedJob, object);
      }
    }, index, model, liteList);
  }

  private void processTransferredResource(ModelService model, Report report, JobPluginInfo pluginInfo, Job job,
    TransferredResource transferredResource) {
    Report reportItem = PluginHelper.initPluginReportItem(this, transferredResource);
    PluginHelper.updatePartialJobReport(this, model, reportItem, false, job);

    try {
      LOGGER.debug("Removing SIP {}", transferredResource.getFullPath());
      model.deleteTransferredResource(transferredResource);
      LOGGER.debug("Done with removing SIP {}", transferredResource.getFullPath());

      if (createEvent) {
        model.createRepositoryEvent(PreservationEventType.DELETION,
          "Processen att radera ett objekt från arkivet", PluginState.SUCCESS,
          "Den överförda resursen " + transferredResource.getId() + " har raderats.", "", job.getUsername(),
          true, null);
      }

      pluginInfo.incrementObjectsProcessedWithSuccess();
    } catch (RuntimeException | GenericException | AuthorizationDeniedException e) {
      if (createEvent) {
        model.createRepositoryEvent(PreservationEventType.DELETION,
          "Processen att radera ett objekt från arkivet", PluginState.SUCCESS,
          "Den överförda resursen " + transferredResource.getId() + " har inte raderats.", "", job.getUsername(),
          true, null);
      }

      pluginInfo.incrementObjectsProcessedWithFailure();
      reportItem.setPluginState(PluginState.FAILURE).setPluginDetails(e.getMessage());
      LOGGER.error("Error removing transferred resource " + transferredResource.getFullPath(), e);
    }

    report.addReport(reportItem);
    PluginHelper.updatePartialJobReport(this, model, reportItem, true, job);
  }

  @Override
  public Plugin<TransferredResource> cloneMe() {
    return new SIPRemovePlugin();
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
    return PreservationEventType.DELETION;
  }

  @Override
  public String getPreservationEventDescription() {
    return "Raderade SIP från överföringsytan.";
  }

  @Override
  public String getPreservationEventSuccessMessage() {
    return "Det ursprungliga SIP:et har raderats från överföringsytan.";
  }

  @Override
  public String getPreservationEventFailureMessage() {
    return "Misslyckades med att radera det ursprungliga SIP:et från överföringsytan.";
  }

  @Override
  public Report beforeAllExecute(IndexService index, ModelService model)
    throws PluginException {
    createEvent = Boolean.parseBoolean(RodaCoreFactory.getRodaConfigurationAsString("event", "create", "all"));
    return new Report();
  }

  @Override
  public Report afterAllExecute(IndexService index, ModelService model) throws PluginException {
    return new Report();
  }

  @Override
  public List<String> getCategories() {
    return Arrays.asList(RodaConstants.PLUGIN_CATEGORY_NOT_LISTABLE);
  }

  @Override
  public List<Class<TransferredResource>> getObjectClasses() {
    return Arrays.asList(TransferredResource.class);
  }

}
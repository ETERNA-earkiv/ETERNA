/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.transaction;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.roda.core.RodaCoreFactory;
import org.roda.core.config.ConfigurationManager;
import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.exceptions.AuthorizationDeniedException;
import org.roda.core.data.exceptions.GenericException;
import org.roda.core.data.exceptions.JobAlreadyStartedException;
import org.roda.core.data.exceptions.NotFoundException;
import org.roda.core.data.exceptions.RequestNotValidException;
import org.roda.core.data.v2.index.IndexResult;
import org.roda.core.data.common.RodaConstants.DateGranularity;
import org.roda.core.data.v2.index.filter.DateRangeFilterParameter;
import org.roda.core.data.v2.index.filter.Filter;
import org.roda.core.data.v2.index.filter.SimpleFilterParameter;
import org.roda.core.data.v2.index.sort.Sorter;
import org.roda.core.data.v2.index.sublist.Sublist;
import org.roda.core.data.v2.jobs.IndexedJob;
import org.roda.core.data.v2.jobs.Job;
import org.roda.core.util.IdUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import io.micrometer.core.annotation.Timed;

/**
 * Scheduled task that periodically checks for jobs in the SCHEDULED state
 * whose next run time has arrived and executes them.
 *
 * <p>
 * When a job fires, a new execution instance is cloned from the template job
 * and submitted to the plugin orchestrator. The template job's
 * {@code nextScheduledRun} field is then advanced to the next cron occurrence.
 * </p>
 *
 * @author Jerry Malmström
 */
@Component
@Timed
public class JobSchedulerTask {

  private static final Logger LOGGER = LoggerFactory.getLogger(JobSchedulerTask.class);

  /**
   * Runs every minute (configurable via {@code jobs.scheduler.interval.millis}).
   * Finds all SCHEDULED jobs whose {@code nextScheduledRun} is in the past and
   * fires an execution copy for each.
   */
  @Scheduled(fixedDelayString = "${jobs.scheduler.interval.millis:60000}")
  public void executeScheduledJobs() {
    if (!ConfigurationManager.getInstance().isInstantiated()) {
      return;
    }

    Filter filter = new Filter(
      new SimpleFilterParameter(RodaConstants.JOB_STATE, Job.JOB_STATE.SCHEDULED.name()),
      new DateRangeFilterParameter(RodaConstants.JOB_NEXT_SCHEDULED_RUN, null, new Date(), DateGranularity.MILLISECOND));

    try {
      IndexResult<IndexedJob> dueJobs = RodaCoreFactory.getIndexService().find(IndexedJob.class, filter, Sorter.NONE,
        new Sublist(0, RodaConstants.DEFAULT_PAGINATION_VALUE), Collections.emptyList());

      for (IndexedJob indexedJob : dueJobs.getResults()) {
        fireScheduledJob(indexedJob.getId());
      }
    } catch (GenericException | RequestNotValidException e) {
      LOGGER.error("Error querying scheduled jobs", e);
    }
  }

  private void fireScheduledJob(String templateJobId) {
    try {
      Job template = RodaCoreFactory.getModelService().retrieveJob(templateJobId);

      String cronExpression = template.getScheduleExpression();
      if (cronExpression == null) {
        LOGGER.warn("Scheduled job {} has no cron expression; skipping", templateJobId);
        return;
      }

      // Build an execution clone: new id, reset state and timing, no schedule info
      Job execution = template.clone();
      execution.setId(IdUtils.createUUID());
      execution.setState(Job.JOB_STATE.CREATED);
      execution.setStartDate(new Date());
      execution.setEndDate(null);
      execution.setScheduleExpression(null);
      execution.setNextScheduledRun(null);

      RodaCoreFactory.getPluginOrchestrator().createAndExecuteJobs(execution, true);
      LOGGER.info("Fired scheduled execution {} from template job {}", execution.getId(), templateJobId);

      if (cronExpression.startsWith("@once:")) {
        // One-shot: clear the schedule after firing
        template.setState(Job.JOB_STATE.STOPPED);
        template.setScheduleExpression(null);
        template.setNextScheduledRun(null);
      } else {
        // Recurring: advance to next cron occurrence
        CronExpression cron = CronExpression.parse(cronExpression);
        ZonedDateTime nextRun = cron.next(ZonedDateTime.now());
        template.setNextScheduledRun(nextRun != null ? Date.from(nextRun.toInstant()) : null);
      }
      RodaCoreFactory.getModelService().createOrUpdateJob(template);
      RodaCoreFactory.getIndexService().commit(IndexedJob.class);

    } catch (NotFoundException e) {
      LOGGER.warn("Scheduled job {} no longer exists; skipping", templateJobId);
    } catch (GenericException | RequestNotValidException | AuthorizationDeniedException
      | JobAlreadyStartedException e) {
      LOGGER.error("Error firing scheduled job {}", templateJobId, e);
    }
  }

  /**
   * Returns the due scheduled jobs as a list. Exposed for testing.
   */
  List<IndexedJob> findDueJobs() throws GenericException, RequestNotValidException {
    Filter filter = new Filter(
      new SimpleFilterParameter(RodaConstants.JOB_STATE, Job.JOB_STATE.SCHEDULED.name()),
      new DateRangeFilterParameter(RodaConstants.JOB_NEXT_SCHEDULED_RUN, null, new Date(), DateGranularity.MILLISECOND));

    return RodaCoreFactory.getIndexService()
      .find(IndexedJob.class, filter, Sorter.NONE, new Sublist(0, RodaConstants.DEFAULT_PAGINATION_VALUE),
        Collections.emptyList())
      .getResults();
  }
}

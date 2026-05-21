/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.transaction;

import java.time.ZonedDateTime;
import java.util.ArrayList;
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
      int offset = 0;
      int pageSize = RodaConstants.DEFAULT_PAGINATION_VALUE;
      List<String> jobIds = new ArrayList<>();

      // Collect all due job IDs before firing to avoid pagination drift caused by
      // state changes mid-loop.
      IndexResult<IndexedJob> page;
      do {
        page = RodaCoreFactory.getIndexService().find(IndexedJob.class, filter, Sorter.NONE,
          new Sublist(offset, pageSize), Collections.emptyList());
        for (IndexedJob indexedJob : page.getResults()) {
          jobIds.add(indexedJob.getId());
        }
        offset += pageSize;
      } while (page.getResults().size() == pageSize);

      for (String jobId : jobIds) {
        fireScheduledJob(jobId);
      }
    } catch (GenericException | RequestNotValidException e) {
      LOGGER.error("Error querying scheduled jobs", e);
    }
  }

  private void fireScheduledJob(String templateJobId) {
    try {
      Job template = RodaCoreFactory.getModelService().retrieveJob(templateJobId);

      // Non-atomic guard against double-firing when pollers race or the
      // template was rescheduled/unscheduled between query and retrieve.
      if (template.getState() != Job.JOB_STATE.SCHEDULED) {
        LOGGER.info("Scheduled job {} no longer in SCHEDULED state ({}); skipping",
          templateJobId, template.getState());
        return;
      }
      Date currentNextRun = template.getNextScheduledRun();
      if (currentNextRun == null || currentNextRun.after(new Date())) {
        LOGGER.info("Scheduled job {} nextScheduledRun already advanced; another poller likely claimed it",
          templateJobId);
        return;
      }

      String cronExpression = template.getScheduleExpression();
      if (cronExpression == null) {
        LOGGER.warn("Scheduled job {} has no cron expression; skipping", templateJobId);
        return;
      }

      // Snapshot original schedule so we can roll back if execution fails.
      Job.JOB_STATE originalState = template.getState();
      Date originalNextRun = template.getNextScheduledRun();

      // Advance the template BEFORE creating the execution to prevent duplicate
      // firings if two pollers race or if the execution write succeeds but the
      // template update later fails.
      if (cronExpression.startsWith("@once:")) {
        template.setState(Job.JOB_STATE.STOPPED);
        template.setScheduleExpression(null);
        template.setNextScheduledRun(null);
      } else {
        try {
          CronExpression cron = CronExpression.parse(cronExpression);
          ZonedDateTime nextRun = cron.next(ZonedDateTime.now());
          template.setNextScheduledRun(nextRun != null ? Date.from(nextRun.toInstant()) : null);
        } catch (IllegalArgumentException e) {
          LOGGER.error("Invalid cron expression on scheduled job {}; disabling it: {}", templateJobId, cronExpression, e);
          template.setState(Job.JOB_STATE.STOPPED);
          template.setScheduleExpression(null);
          template.setNextScheduledRun(null);
          RodaCoreFactory.getModelService().createOrUpdateJob(template);
          return;
        }
      }
      RodaCoreFactory.getModelService().createOrUpdateJob(template);
      RodaCoreFactory.getIndexService().commit(IndexedJob.class);

      // Build an execution clone: new id, reset state and timing, no schedule info
      Job execution = template.clone();
      execution.setId(IdUtils.createUUID());
      execution.setState(Job.JOB_STATE.CREATED);
      execution.setStartDate(new Date());
      execution.setEndDate(null);
      execution.setScheduleExpression(null);
      execution.setNextScheduledRun(null);

      try {
        RodaCoreFactory.getPluginOrchestrator().createAndExecuteJobs(execution, true);
        LOGGER.info("Fired scheduled execution {} from template job {}", execution.getId(), templateJobId);
      } catch (JobAlreadyStartedException | AuthorizationDeniedException | GenericException
        | RequestNotValidException e) {
        LOGGER.error("Failed to start execution for scheduled job {}", templateJobId, e);
        // Roll back the @once template so the next poll can retry it. For recurring
        // jobs the template already has nextScheduledRun advanced to the next
        // occurrence, so we skip without rolling back to avoid an immediate retry loop.
        if (cronExpression.startsWith("@once:")) {
          template.setState(originalState);
          template.setScheduleExpression(cronExpression);
          template.setNextScheduledRun(originalNextRun);
          try {
            RodaCoreFactory.getModelService().createOrUpdateJob(template);
          } catch (Exception rollbackEx) {
            LOGGER.error("Rollback failed for @once template job {}", templateJobId, rollbackEx);
          }
        }
      }

    } catch (NotFoundException e) {
      LOGGER.warn("Scheduled job {} no longer exists; skipping", templateJobId);
    } catch (GenericException | RequestNotValidException | AuthorizationDeniedException e) {
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

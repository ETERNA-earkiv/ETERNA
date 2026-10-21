/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.model;

import java.util.List;

import org.roda.core.data.exceptions.ReturnWithExceptions;
import org.roda.core.data.v2.disposal.confirmation.DisposalConfirmation;
import org.roda.core.data.v2.ip.AIP;
import org.roda.core.data.v2.ip.DIP;
import org.roda.core.data.v2.ip.DIPFile;
import org.roda.core.data.v2.ip.File;
import org.roda.core.data.v2.ip.Representation;
import org.roda.core.data.v2.ip.metadata.DescriptiveMetadata;
import org.roda.core.data.v2.ip.metadata.OtherMetadata;
import org.roda.core.data.v2.ip.metadata.PreservationMetadata;
import org.roda.core.data.v2.jobs.IndexedJob;
import org.roda.core.data.v2.jobs.Job;
import org.roda.core.data.v2.jobs.Report;
import org.roda.core.data.v2.log.LogEntry;
import org.roda.core.data.v2.notifications.Notification;
import org.roda.core.data.v2.ri.RepresentationInformation;
import org.roda.core.data.v2.risks.Risk;
import org.roda.core.data.v2.risks.RiskIncidence;
import org.roda.core.data.v2.user.Group;
import org.roda.core.data.v2.user.User;

/**
 * ETERNA: tom bas för modellobservatörer – varje metod i {@link ModelObserver}
 * gör ingenting och returnerar ett tomt {@link ReturnWithExceptions} med
 * denna observatör som källa. Underklasser åsidosätter bara de händelser de
 * bryr sig om (t.ex. {@code org.roda.core.common.access.AccessRulesModelObserver}).
 *
 * <p>
 * Metodlistan är genererad ur {@link ModelObserver}. Klassen är avsiktligt
 * konkret (inte abstrakt): får gränssnittet en ny metod säger kompilatorn
 * ifrån i just den här filen, inte först i en underklass.
 * </p>
 */
public class NoOpModelObserver implements ModelObserver {

  /** Tomt resultat med denna observatör som källa. */
  protected ReturnWithExceptions<Void, ModelObserver> none() {
    return new ReturnWithExceptions<>(this);
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> aipCreated(AIP aip) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> aipUpdated(AIP aip) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> aipUpdatedOn(AIP aip) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> aipDestroyed(AIP aip) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> aipStateUpdated(AIP aip) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> aipInstanceIdUpdated(AIP aip) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> aipMoved(AIP aip, String oldParentId, String newParentId) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> aipDeleted(String aipId, boolean deleteIncidences) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> descriptiveMetadataCreated(
    DescriptiveMetadata descriptiveMetadataBinary) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> descriptiveMetadataUpdated(
    DescriptiveMetadata descriptiveMetadataBinary) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> descriptiveMetadataDeleted(String aipId, String representationId,
    String descriptiveMetadataBinaryId) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> representationCreated(Representation representation) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> representationUpdated(Representation representation) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> representationDeleted(String aipId, String representationId,
    boolean deleteIncidences) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> representationUpdatedOn(Representation representation) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> fileCreated(File file) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> fileUpdated(File file) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> fileDeleted(String aipId, String representationId,
    List<String> fileDirectoryPath, String fileId, boolean deleteIncidences) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> logEntryCreated(LogEntry entry) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> userCreated(User user) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> userUpdated(User user) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> userDeleted(String userID) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> groupCreated(Group group) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> groupUpdated(Group group) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> groupDeleted(String groupID) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> preservationMetadataCreated(
    PreservationMetadata preservationMetadataBinary) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> preservationMetadataUpdated(
    PreservationMetadata preservationMetadataBinary) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> preservationMetadataDeleted(
    PreservationMetadata preservationMetadataBinary) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> otherMetadataCreated(OtherMetadata otherMetadataBinary) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> jobCreatedOrUpdated(Job job, boolean reindexJobReports) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> jobDeleted(String jobId) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> jobReportCreatedOrUpdated(Report jobReport, Job cachedJob) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> jobReportCreatedOrUpdated(Report jobReport, IndexedJob indexedJob) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> jobReportDeleted(String jobReportId) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> aipPermissionsUpdated(AIP aip) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> dipPermissionsUpdated(DIP dip) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> dipInstanceIdUpdated(DIP dip) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> transferredResourceDeleted(String transferredResourceID) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> riskCreatedOrUpdated(Risk risk, int incidences, boolean commit) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> riskDeleted(String riskId, boolean commit) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> riskIncidenceCreatedOrUpdated(RiskIncidence riskIncidence,
    boolean commit) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> riskIncidenceDeleted(String riskIncidenceId, boolean commit) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> representationInformationCreatedOrUpdated(
    RepresentationInformation ri, boolean commit) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> representationInformationDeleted(String representationInformationId,
    boolean commit) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> notificationCreatedOrUpdated(Notification notification) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> notificationDeleted(String notificationId) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> dipCreated(DIP dip, boolean commit) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> dipUpdated(DIP dip, boolean commit) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> dipDeleted(String dipId, boolean commit) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> dipFileCreated(DIPFile file) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> dipFileUpdated(DIPFile file) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> dipFileDeleted(String dipId, List<String> path, String fileId) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> disposalConfirmationCreateOrUpdate(
    DisposalConfirmation confirmation) {
    return none();
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> disposalConfirmationDeleted(String confirmationId, boolean commit) {
    return none();
  }
}

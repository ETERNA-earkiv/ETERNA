/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.plugins.base.ingest;

import org.roda.core.data.common.RodaConstants.PreservationEventType;
import org.roda.core.data.exceptions.AlreadyExistsException;
import org.roda.core.data.exceptions.AuthorizationDeniedException;
import org.roda.core.data.exceptions.GenericException;
import org.roda.core.data.exceptions.NotFoundException;
import org.roda.core.data.exceptions.RequestNotValidException;
import org.roda.core.data.v2.ip.AIP;
import org.roda.core.data.v2.ip.TransferredResource;
import org.roda.core.data.v2.jobs.Job;
import org.roda.core.data.v2.jobs.PluginState;
import org.roda.core.data.v2.jobs.PluginType;
import org.roda.core.data.v2.validation.ValidationException;
import org.roda.core.index.IndexService;
import org.roda.core.model.ModelService;
import org.roda.core.plugins.AbstractPlugin;
import org.roda.core.plugins.PluginHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SIPToAIPPlugin extends AbstractPlugin<TransferredResource> {
  private static final Logger LOGGER = LoggerFactory.getLogger(SIPToAIPPlugin.class);

  public static final String UNPACK_SUCCESS_MESSAGE = "SIP:et har packats upp.";
  public static final String UNPACK_FAILURE_MESSAGE = "Inleveransprocessen misslyckades med att packa upp SIP:et.";
  public static final String UNPACK_PARTIAL_MESSAGE = null;
  public static final PreservationEventType UNPACK_EVENT_TYPE = PreservationEventType.UNPACKING;

  public static final String WELLFORMED_DESCRIPTION = "Kontrollerade att det mottagna SIP:et är välformat, komplett och att inga oväntade filer inkluderades.";
  public static final String WELLFORMED_SUCCESS_MESSAGE = "SIP:et var välformat och komplett.";
  public static final String WELLFORMED_FAILURE_MESSAGE = "SIP:et var inte välformat eller några filer saknades.";
  public static final String WELLFORMED_PARTIAL_MESSAGE = null;
  public static final PreservationEventType WELLFORMED_EVENT_TYPE = PreservationEventType.WELLFORMEDNESS_CHECK;

  private String successMessage;
  private String failureMessage;
  private PreservationEventType eventType;
  private String eventDescription;

  @Override
  public PluginType getType() {
    return PluginType.SIP_TO_AIP;
  }

  @Override
  public boolean areParameterValuesValid() {
    return true;
  }

  @Override
  public PreservationEventType getPreservationEventType() {
    return eventType;
  }

  @Override
  public String getPreservationEventDescription() {
    return eventDescription;
  }

  @Override
  public String getPreservationEventSuccessMessage() {
    return successMessage;
  }

  @Override
  public String getPreservationEventFailureMessage() {
    return failureMessage;
  }

  public void setPreservationEventType(PreservationEventType t) {
    this.eventType = t;
  }

  public void setPreservationSuccessMessage(String message) {
    this.successMessage = message;
  }

  public void setPreservationFailureMessage(String message) {
    this.failureMessage = message;
  }

  public void setPreservationEventDescription(String description) {
    this.eventDescription = description;
  }

  protected void createUnpackingEventSuccess(ModelService model, IndexService index,
    TransferredResource transferredResource, AIP aip, String unpackDescription, Job cachedJob) {
    setPreservationEventType(UNPACK_EVENT_TYPE);
    setPreservationSuccessMessage(UNPACK_SUCCESS_MESSAGE);
    setPreservationFailureMessage(UNPACK_FAILURE_MESSAGE);
    setPreservationEventDescription(unpackDescription);
    try {
      boolean notify = true;
      PluginHelper.createPluginEvent(this, aip.getId(), model, index, transferredResource, PluginState.SUCCESS, "",
        notify, cachedJob);
    } catch (NotFoundException | RequestNotValidException | GenericException | AuthorizationDeniedException
      | ValidationException | AlreadyExistsException e) {
      LOGGER.warn("Error creating unpacking event: " + e.getMessage(), e);
    }
  }

  protected void createWellformedEventSuccess(ModelService model, IndexService index,
    TransferredResource transferredResource, AIP aip, Job cachedJob) {
    setPreservationEventType(WELLFORMED_EVENT_TYPE);
    setPreservationSuccessMessage(WELLFORMED_SUCCESS_MESSAGE);
    setPreservationFailureMessage(WELLFORMED_FAILURE_MESSAGE);
    setPreservationEventDescription(WELLFORMED_DESCRIPTION);
    try {
      boolean notify = true;
      PluginHelper.createPluginEvent(this, aip.getId(), model, index, transferredResource, PluginState.SUCCESS, "",
        notify, cachedJob);
    } catch (NotFoundException | RequestNotValidException | GenericException | AuthorizationDeniedException
      | ValidationException | AlreadyExistsException e) {
      LOGGER.warn("Error creating unpacking event: " + e.getMessage(), e);
    }
  }
}

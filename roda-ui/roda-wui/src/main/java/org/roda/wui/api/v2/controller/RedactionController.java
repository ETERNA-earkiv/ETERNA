/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/keeps/roda
 */
package org.roda.wui.api.v2.controller;

import java.io.IOException;

import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.exceptions.RODAException;
import org.roda.core.data.v2.ip.redaction.StartRedactionRequest;
import org.roda.wui.api.v2.services.RedactionService;
import org.roda.wui.client.services.RedactionRestService;
import org.roda.wui.common.RequestControllerAssistant;
import org.roda.wui.common.model.RequestContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for redaction operations.
 *
 * @author Tomas Fridekrans <tomas.fridekrans@whitered.se>
 */
@RestController
@RequestMapping(path = "/api/v2/redaction")
public class RedactionController implements RedactionRestService {

  @Autowired
  RedactionService redactionService;

  @Autowired
  RequestHandler requestHandler;

  @Override
  public Void logRedactionStart(@RequestBody StartRedactionRequest request) {
    return requestHandler.processRequest(new RequestHandler.RequestProcessor<Void>() {
      @Override
      public Void process(RequestContext requestContext, RequestControllerAssistant controllerAssistant)
        throws RODAException, IOException {
        controllerAssistant.setParameters(
          RodaConstants.CONTROLLER_AIP_ID_PARAM, request.getAipId(),
          RodaConstants.CONTROLLER_REPRESENTATION_ID_PARAM, request.getRepresentationId(),
          RodaConstants.CONTROLLER_FILE_ID_PARAM, request.getFileId(),
          RodaConstants.CONTROLLER_DETAILS_PARAM, request.getDetails()
        );
        redactionService.logRedactionStart(requestContext.getUser(), request);
        return null;
      }
    });
  }
}

/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/keeps/roda
 */
package org.roda.wui.api.v2.services;

import org.roda.core.data.v2.ip.redaction.StartRedactionRequest;
import org.roda.core.data.v2.user.User;
import org.springframework.stereotype.Service;

/**
 * Service for handling redaction operations.
 *
 * @author Tomas Fridekrans <tomas.fridekrans@whitered.se>
 */
@Service
public class RedactionService {

  /**
   * Logs the start of a redaction operation.
   *
   * Logging is handled automatically by RequestControllerAssistant.registerAction()
   * in the controller's RequestHandler.processRequest() finally-block.
   *
   * @param user
   *          the user performing the redaction
   * @param request
   *          the redaction request containing AIP ID, representation ID, file ID and details
   */
  public void logRedactionStart(User user, StartRedactionRequest request) {
    // Logging handled automatically by RequestControllerAssistant.registerAction()
    // in the controller's RequestHandler.processRequest() finally-block.
  }
}

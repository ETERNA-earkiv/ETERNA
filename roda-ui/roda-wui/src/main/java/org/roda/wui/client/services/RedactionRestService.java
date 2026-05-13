/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.client.services;

import org.fusesource.restygwt.client.DirectRestService;
import org.roda.core.data.v2.ip.redaction.SaveRedactionRequest;
import org.roda.wui.api.v2.exceptions.model.ErrorResponseMessage;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @author Tomas Fridekrans <tomas.fridekrans@whitered.se>
 */

@Tag(name = "Redaction")
@RequestMapping(path = "../api/v2/redaction")
public interface RedactionRestService extends DirectRestService {

  @RequestMapping(method = RequestMethod.POST, path = "/log", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Log redaction save", requestBody = @RequestBody(required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SaveRedactionRequest.class))), responses = {
    @ApiResponse(responseCode = "200", description = "Redaction save logged", content = @Content()),
    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponseMessage.class))),
    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorResponseMessage.class)))})
  Void logRedactionSave(@org.springframework.web.bind.annotation.RequestBody SaveRedactionRequest request);
}

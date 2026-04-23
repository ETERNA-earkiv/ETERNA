/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/keeps/roda
 */
package org.roda.wui.client.services;

import org.fusesource.restygwt.client.DirectRestService;
import org.roda.core.data.v2.ip.redaction.StartRedactionRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @author Redaction team
 */

@Tag(name = "Redaction")
@RequestMapping(path = "../api/v2/redaction")
public interface RedactionRestService extends DirectRestService {

  @RequestMapping(method = RequestMethod.POST, path = "/log", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Log redaction session start", responses = {
    @ApiResponse(responseCode = "200", description = "Redaction start logged"),
    @ApiResponse(responseCode = "401", description = "Unauthorized"),
    @ApiResponse(responseCode = "403", description = "Forbidden")})
  Void logRedactionStart(@RequestBody StartRedactionRequest request);
}

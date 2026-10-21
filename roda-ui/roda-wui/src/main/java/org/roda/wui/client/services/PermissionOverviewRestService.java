/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.client.services;

import org.fusesource.restygwt.client.DirectRestService;
import org.roda.core.data.v2.user.PermissionOverview;
import org.roda.wui.api.v2.exceptions.model.ErrorResponseMessage;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Klientgränssnitt för behörighetsöversikten. Bara
 * JSON-ändpunkten behövs i GWT; HTML- och CSV-rapporterna
 * (/report/html, /report/csv) öppnas som vanliga länkar.
 */
@Tag(name = "Permission overview")
@RequestMapping(path = "../api/v2/permission-overview")
public interface PermissionOverviewRestService extends DirectRestService {

  @RequestMapping(method = RequestMethod.GET, path = "", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Retrieve permission overview", description = "Builds a snapshot of the role catalogue, groups, users, structural-unit permissions and access rules visible to the caller", responses = {
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = PermissionOverview.class))),
    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponseMessage.class))),
    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorResponseMessage.class)))})
  PermissionOverview retrieveOverview(
    @Parameter(description = "The language to be used for internationalization") @RequestParam(name = "lang", required = false) String localeString);
}

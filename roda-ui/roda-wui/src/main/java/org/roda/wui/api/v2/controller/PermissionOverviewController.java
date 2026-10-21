/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.api.v2.controller;

import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.exceptions.RODAException;
import org.roda.core.data.v2.StreamResponse;
import org.roda.core.data.v2.user.PermissionOverview;
import org.roda.wui.api.v2.exceptions.RESTException;
import org.roda.wui.api.v2.exceptions.model.ErrorResponseMessage;
import org.roda.wui.api.v2.services.PermissionOverviewService;
import org.roda.wui.api.v2.utils.ApiUtils;
import org.roda.wui.client.services.PermissionOverviewRestService;
import org.roda.wui.common.RequestControllerAssistant;
import org.roda.wui.common.model.RequestContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST-ändpunkter för behörighetsöversikten. Rollerna
 * sätts i roda-roles.properties på metodnamnen retrieveOverview,
 * downloadOverviewHtml och downloadOverviewCsv (member.read); varje anrop
 * loggas via RequestHandler.
 */
@RestController
@RequestMapping(path = "/api/v2/permission-overview")
@Tag(name = PermissionOverviewController.SWAGGER_ENDPOINT)
public class PermissionOverviewController implements PermissionOverviewRestService {
  public static final String SWAGGER_ENDPOINT = "Permission overview";
  public static final String MEDIA_TYPE_CSV = "text/csv";

  @Autowired
  private RequestHandler requestHandler;

  @Autowired
  private PermissionOverviewService permissionOverviewService;

  @Override
  public PermissionOverview retrieveOverview(String localeString) {
    return requestHandler.processRequest(new RequestHandler.RequestProcessor<PermissionOverview>() {
      @Override
      public PermissionOverview process(RequestContext requestContext, RequestControllerAssistant controllerAssistant)
        throws RODAException, RESTException {
        controllerAssistant.setParameters(RodaConstants.CONTROLLER_LANG_PARAM, localeString);
        // delegate
        return permissionOverviewService.buildOverview(requestContext, localeString);
      }
    });
  }

  @GetMapping(path = "/report/html", produces = MediaType.TEXT_HTML_VALUE)
  @Operation(summary = "Permission overview as printable HTML", description = "Renders the permission overview (roles per group, users, structural units and access rules) as a self-contained, printable HTML report", responses = {
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = MediaType.TEXT_HTML_VALUE)),
    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponseMessage.class))),
    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorResponseMessage.class)))})
  public ResponseEntity<StreamingResponseBody> downloadOverviewHtml(
    @Parameter(description = "The language to be used for internationalization (default sv_SE)") @RequestParam(name = RodaConstants.API_QUERY_KEY_LANG, required = false) String localeString) {
    return requestHandler.processRequest(new RequestHandler.RequestProcessor<ResponseEntity<StreamingResponseBody>>() {
      @Override
      public ResponseEntity<StreamingResponseBody> process(RequestContext requestContext,
        RequestControllerAssistant controllerAssistant) throws RODAException, RESTException {
        controllerAssistant.setParameters(RodaConstants.CONTROLLER_LANG_PARAM, localeString);
        // delegate
        PermissionOverview overview = permissionOverviewService.buildOverview(requestContext, localeString);
        return inlineResponse(permissionOverviewService.renderHtml(overview, localeString));
      }
    });
  }

  @GetMapping(path = "/report/csv", produces = MEDIA_TYPE_CSV)
  @Operation(summary = "Permission overview as CSV", description = "Exports the permission overview as a CSV file (one section per report part) with the configured csv.delimiter (default ';')", responses = {
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = MEDIA_TYPE_CSV)),
    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponseMessage.class))),
    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorResponseMessage.class)))})
  public ResponseEntity<StreamingResponseBody> downloadOverviewCsv(
    @Parameter(description = "The language to be used for internationalization (default sv_SE)") @RequestParam(name = RodaConstants.API_QUERY_KEY_LANG, required = false) String localeString) {
    return requestHandler.processRequest(new RequestHandler.RequestProcessor<ResponseEntity<StreamingResponseBody>>() {
      @Override
      public ResponseEntity<StreamingResponseBody> process(RequestContext requestContext,
        RequestControllerAssistant controllerAssistant) throws RODAException, RESTException {
        controllerAssistant.setParameters(RodaConstants.CONTROLLER_LANG_PARAM, localeString);
        // delegate
        PermissionOverview overview = permissionOverviewService.buildOverview(requestContext, localeString);
        return ApiUtils.okResponse(permissionOverviewService.renderCsv(overview, localeString));
      }
    });
  }

  /** Som ApiUtils.okResponse men "inline" så att rapporten öppnas i webbläsaren, och utan cachning. */
  private static ResponseEntity<StreamingResponseBody> inlineResponse(StreamResponse streamResponse) {
    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.CONTENT_TYPE, streamResponse.getStream().getMediaType());
    headers.add(HttpHeaders.CONTENT_DISPOSITION,
      "inline; filename=\"" + streamResponse.getStream().getFileName() + "\"");
    headers.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(streamResponse.getStream().getSize()));
    StreamingResponseBody body = outputStream -> streamResponse.getStream().consumeOutputStream(outputStream);
    return ResponseEntity.ok().headers(headers).cacheControl(CacheControl.noStore()).body(body);
  }
}

/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.client.services;

import org.fusesource.restygwt.client.DirectRestService;
import org.roda.core.data.v2.accessrules.AccessRule;
import org.roda.core.data.v2.accessrules.AccessRuleSet;
import org.roda.core.data.v2.accessrules.AccessRulesStatus;
import org.roda.core.data.v2.accessrules.AccessRulesVocabulary;
import org.roda.core.data.v2.accessrules.LagrumNormalizeRequest;
import org.roda.core.data.v2.accessrules.LagrumNormalizeResult;
import org.roda.core.data.v2.jobs.Job;
import org.roda.wui.api.v2.exceptions.model.ErrorResponseMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST-kontrakt för åtkomstreglerna
 * (sekretess/personuppgifter → läsbehörighet per grupp). Delas av
 * Spring-kontrollern och RestyGWT-klienten.
 *
 * <p>
 * Literala sökvägar ({@code /status}, {@code /vocabulary}, {@code /apply},
 * {@code /normalize}) har företräde framför {@code /{group}}; grupper med
 * de namnen kan därför inte hämtas via GET /{group}.
 * </p>
 */
@Tag(name = "Access rules")
@RequestMapping(path = "../api/v2/access-rules")
public interface AccessRulesRestService extends DirectRestService {

  @RequestMapping(method = RequestMethod.GET, path = "", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "List access rules", description = "Gets the whole access rule set (policy defaults, enabled flag and one rule per managed group)", responses = {
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = AccessRuleSet.class))),
    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorResponseMessage.class)))})
  AccessRuleSet listAccessRules();

  @RequestMapping(method = RequestMethod.PUT, path = "", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Update access rule set", description = "Replaces the whole access rule set. The submitted version must equal the stored one (optimistic locking, 409 otherwise); the stored version is then incremented. With apply=true the apply job is started after a successful save (its id is written to the audit log).", requestBody = @RequestBody(required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AccessRuleSet.class))), responses = {
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = AccessRuleSet.class))),
    @ApiResponse(responseCode = "400", description = "Rule set not valid", content = @Content(schema = @Schema(implementation = ErrorResponseMessage.class))),
    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorResponseMessage.class))),
    @ApiResponse(responseCode = "409", description = "Version conflict", content = @Content(schema = @Schema(implementation = ErrorResponseMessage.class)))})
  AccessRuleSet updateAccessRuleSet(AccessRuleSet accessRuleSet,
    @Parameter(description = "If true, applies the rules to the repository after saving.", content = @Content(schema = @Schema(implementation = boolean.class, defaultValue = "false"))) @RequestParam(name = "apply", required = false, defaultValue = "false") boolean apply);

  @RequestMapping(method = RequestMethod.POST, path = "/apply", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Apply access rules to repository", description = "Starts an internal job that applies the access rules to all active AIPs. With dryRun=true nothing is written; the job report and CSV attachment show what would change.", responses = {
    @ApiResponse(responseCode = "200", description = "Job created", content = @Content(schema = @Schema(implementation = Job.class))),
    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorResponseMessage.class)))})
  Job applyAccessRules(
    @Parameter(description = "If true, computes and reports the changes without writing anything.", content = @Content(schema = @Schema(implementation = boolean.class, defaultValue = "false"))) @RequestParam(name = "dryRun", required = false, defaultValue = "false") boolean dryRun);

  @RequestMapping(method = RequestMethod.GET, path = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Get access rules status", description = "Whether the rules are effectively enabled, the current version, the managed groups and the last apply job", responses = {
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = AccessRulesStatus.class))),
    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorResponseMessage.class)))})
  AccessRulesStatus retrieveStatus();

  @RequestMapping(method = RequestMethod.GET, path = "/vocabulary", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Get access rules vocabulary", description = "Allowed values for sekretess, personuppgifter and personuppgiftsniva, and labels in the requested language", responses = {
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = AccessRulesVocabulary.class))),
    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorResponseMessage.class)))})
  AccessRulesVocabulary retrieveVocabulary(
    @Parameter(description = "The language to be used for the labels (e.g. sv_SE or en)") @RequestParam(name = "lang", required = false) String localeString);

  @RequestMapping(method = RequestMethod.POST, path = "/normalize", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Normalize legal references", description = "Parses free-text legal references (lagrum) into the pattern form used in rules, e.g. 'OSL 21 kap. 7 §' -> 'OSL 21:7'. Unparsable parts and texts yielding more than one reference give warnings. At most 50 texts of at most 200 characters each.", requestBody = @RequestBody(required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = LagrumNormalizeRequest.class))), responses = {
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = LagrumNormalizeResult.class))),
    @ApiResponse(responseCode = "400", description = "Too many or too long texts", content = @Content(schema = @Schema(implementation = ErrorResponseMessage.class))),
    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorResponseMessage.class)))})
  LagrumNormalizeResult normalizeLagrum(LagrumNormalizeRequest request);

  @RequestMapping(method = RequestMethod.GET, path = "/{group}", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Get access rule", description = "Gets the access rule for a group", responses = {
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = AccessRule.class))),
    @ApiResponse(responseCode = "404", description = "Not found", content = @Content(schema = @Schema(implementation = ErrorResponseMessage.class)))})
  AccessRule retrieveAccessRule(
    @Parameter(description = "The group name", required = true) @PathVariable(name = "group") String group);

  @RequestMapping(method = RequestMethod.PUT, path = "/{group}", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Create or update access rule", description = "Creates or replaces the access rule for a group (the group in the path wins; a different group in the body is rejected). With apply=true the apply job is started after a successful save (its id is written to the audit log).", requestBody = @RequestBody(required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AccessRule.class))), responses = {
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = AccessRule.class))),
    @ApiResponse(responseCode = "400", description = "Rule not valid", content = @Content(schema = @Schema(implementation = ErrorResponseMessage.class))),
    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorResponseMessage.class)))})
  AccessRule updateAccessRule(
    @Parameter(description = "The group name", required = true) @PathVariable(name = "group") String group,
    AccessRule accessRule,
    @Parameter(description = "If true, applies the rules to the repository after saving.", content = @Content(schema = @Schema(implementation = boolean.class, defaultValue = "false"))) @RequestParam(name = "apply", required = false, defaultValue = "false") boolean apply);

  @RequestMapping(method = RequestMethod.DELETE, path = "/{group}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Delete access rule", description = "Removes the access rule for a group; the group is no longer managed by the rules", responses = {
    @ApiResponse(responseCode = "204", description = "Resource deleted"),
    @ApiResponse(responseCode = "404", description = "Not found", content = @Content(schema = @Schema(implementation = ErrorResponseMessage.class)))})
  Void deleteAccessRule(
    @Parameter(description = "The group name", required = true) @PathVariable(name = "group") String group);
}

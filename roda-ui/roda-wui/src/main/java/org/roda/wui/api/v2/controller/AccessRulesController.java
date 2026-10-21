/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.api.v2.controller;

import java.io.IOException;

import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.exceptions.RODAException;
import org.roda.core.data.utils.JsonUtils;
import org.roda.core.data.v2.accessrules.AccessRule;
import org.roda.core.data.v2.accessrules.AccessRuleSet;
import org.roda.core.data.v2.accessrules.AccessRulesStatus;
import org.roda.core.data.v2.accessrules.AccessRulesVocabulary;
import org.roda.core.data.v2.accessrules.LagrumNormalizeRequest;
import org.roda.core.data.v2.accessrules.LagrumNormalizeResult;
import org.roda.core.data.v2.jobs.Job;
import org.roda.wui.api.v2.exceptions.RESTException;
import org.roda.wui.api.v2.services.AccessRulesService;
import org.roda.wui.client.services.AccessRulesRestService;
import org.roda.wui.common.RequestControllerAssistant;
import org.roda.wui.common.model.RequestContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.json.JsonSanitizer;

/**
 * REST-API för åtkomstreglerna (access-rules.json)
 * under {@code /api/v2/access-rules}. Rollkraven ligger i
 * roda-roles.properties (access_rules.read för läsning, access_rules.manage
 * för ändring och tillämpning); varje anrop loggas med gruppnamn, regel och
 * ev. startat jobb-id som parametrar.
 *
 * <p>
 * Sparande med {@code apply=true} startar tillämpningsjobbet efter en
 * lyckad sparning men returnerar ändå det sparade objektet (regeln resp.
 * regeluppsättningen); jobb-id:t loggas som parameter {@code jobId}. En
 * klient som vill följa jobbet anropar i stället {@code POST /apply}
 * separat, som returnerar jobbet.
 * </p>
 */
@RestController
@RequestMapping(path = "/api/v2/access-rules")
public class AccessRulesController implements AccessRulesRestService {

  @Autowired
  AccessRulesService accessRulesService;

  @Autowired
  RequestHandler requestHandler;

  @Override
  public AccessRuleSet listAccessRules() {
    return requestHandler.processRequest(new RequestHandler.RequestProcessor<AccessRuleSet>() {
      @Override
      public AccessRuleSet process(RequestContext requestContext, RequestControllerAssistant controllerAssistant)
        throws RODAException, RESTException, IOException {
        return accessRulesService.getRules();
      }
    });
  }

  @Override
  public AccessRule retrieveAccessRule(String group) {
    return requestHandler.processRequest(new RequestHandler.RequestProcessor<AccessRule>() {
      @Override
      public AccessRule process(RequestContext requestContext, RequestControllerAssistant controllerAssistant)
        throws RODAException, RESTException, IOException {
        controllerAssistant.setRelatedObjectId(group);
        controllerAssistant.setParameters(RodaConstants.CONTROLLER_GROUP_PARAM, group);

        return accessRulesService.getRule(group);
      }
    });
  }

  @Override
  public AccessRule updateAccessRule(String group, @RequestBody AccessRule accessRule, boolean apply) {
    return requestHandler.processRequest(new RequestHandler.RequestProcessor<AccessRule>() {
      @Override
      public AccessRule process(RequestContext requestContext, RequestControllerAssistant controllerAssistant)
        throws RODAException, RESTException, IOException {
        controllerAssistant.setRelatedObjectId(group);
        controllerAssistant.setParameters(RodaConstants.CONTROLLER_GROUP_PARAM, group,
          RodaConstants.CONTROLLER_ACCESS_RULE_PARAM, accessRule, RodaConstants.CONTROLLER_APPLY_PARAM, apply);

        // sanitize the input
        String sanitize = JsonSanitizer.sanitize(JsonUtils.getJsonFromObject(accessRule));
        AccessRule sanitizedAccessRule = JsonUtils.getObjectFromJson(sanitize, AccessRule.class);

        // delegate action to service
        AccessRule saved = accessRulesService.saveRule(group, sanitizedAccessRule, requestContext.getUser(),
          requestContext.getModelService());
        if (apply) {
          Job job = accessRulesService.apply(requestContext.getUser(), false);
          controllerAssistant.addParameters(RodaConstants.CONTROLLER_JOB_ID_PARAM, job.getId());
        }
        return saved;
      }
    });
  }

  @Override
  public Void deleteAccessRule(String group) {
    return requestHandler.processRequest(new RequestHandler.RequestProcessor<Void>() {
      @Override
      public Void process(RequestContext requestContext, RequestControllerAssistant controllerAssistant)
        throws RODAException, RESTException, IOException {
        controllerAssistant.setRelatedObjectId(group);
        controllerAssistant.setParameters(RodaConstants.CONTROLLER_GROUP_PARAM, group);

        // delegate
        accessRulesService.deleteRule(group, requestContext.getUser(), requestContext.getModelService());
        return null;
      }
    });
  }

  @Override
  public AccessRuleSet updateAccessRuleSet(@RequestBody AccessRuleSet accessRuleSet, boolean apply) {
    return requestHandler.processRequest(new RequestHandler.RequestProcessor<AccessRuleSet>() {
      @Override
      public AccessRuleSet process(RequestContext requestContext, RequestControllerAssistant controllerAssistant)
        throws RODAException, RESTException, IOException {
        controllerAssistant.setParameters(RodaConstants.CONTROLLER_ACCESS_RULE_SET_PARAM, accessRuleSet,
          RodaConstants.CONTROLLER_APPLY_PARAM, apply);

        // sanitize the input
        String sanitize = JsonSanitizer.sanitize(JsonUtils.getJsonFromObject(accessRuleSet));
        AccessRuleSet sanitizedAccessRuleSet = JsonUtils.getObjectFromJson(sanitize, AccessRuleSet.class);

        // delegate action to service
        AccessRuleSet saved = accessRulesService.saveRuleSet(sanitizedAccessRuleSet, requestContext.getUser(),
          requestContext.getModelService());
        if (apply) {
          Job job = accessRulesService.apply(requestContext.getUser(), false);
          controllerAssistant.addParameters(RodaConstants.CONTROLLER_JOB_ID_PARAM, job.getId());
        }
        return saved;
      }
    });
  }

  @Override
  public Job applyAccessRules(boolean dryRun) {
    return requestHandler.processRequest(new RequestHandler.RequestProcessor<Job>() {
      @Override
      public Job process(RequestContext requestContext, RequestControllerAssistant controllerAssistant)
        throws RODAException, RESTException, IOException {
        controllerAssistant.setParameters(RodaConstants.CONTROLLER_DRY_RUN_PARAM, Boolean.toString(dryRun));

        // delegate action to service
        Job job = accessRulesService.apply(requestContext.getUser(), dryRun);
        controllerAssistant.addParameters(RodaConstants.CONTROLLER_JOB_ID_PARAM, job.getId());
        return job;
      }
    });
  }

  @Override
  public AccessRulesStatus retrieveStatus() {
    return requestHandler.processRequest(new RequestHandler.RequestProcessor<AccessRulesStatus>() {
      @Override
      public AccessRulesStatus process(RequestContext requestContext, RequestControllerAssistant controllerAssistant)
        throws RODAException, RESTException, IOException {
        return accessRulesService.status(requestContext.getIndexService());
      }
    });
  }

  @Override
  public AccessRulesVocabulary retrieveVocabulary(String localeString) {
    return requestHandler.processRequest(new RequestHandler.RequestProcessor<AccessRulesVocabulary>() {
      @Override
      public AccessRulesVocabulary process(RequestContext requestContext,
        RequestControllerAssistant controllerAssistant) throws RODAException, RESTException, IOException {
        controllerAssistant.setParameters(RodaConstants.CONTROLLER_LANG_PARAM, localeString);

        return accessRulesService.vocabulary(localeString);
      }
    });
  }

  @Override
  public LagrumNormalizeResult normalizeLagrum(@RequestBody LagrumNormalizeRequest request) {
    return requestHandler.processRequest(new RequestHandler.RequestProcessor<LagrumNormalizeResult>() {
      @Override
      public LagrumNormalizeResult process(RequestContext requestContext,
        RequestControllerAssistant controllerAssistant) throws RODAException, RESTException, IOException {
        return accessRulesService.normalize(request);
      }
    });
  }
}

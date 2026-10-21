/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.common.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.roda.core.RodaCoreFactory;
import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.exceptions.NotFoundException;
import org.roda.core.data.exceptions.RODAException;
import org.roda.core.data.v2.index.filter.Filter;
import org.roda.core.data.v2.index.filter.OneOfManyFilterParameter;
import org.roda.core.data.v2.index.filter.SimpleFilterParameter;
import org.roda.core.data.v2.index.select.SelectedItemsFilter;
import org.roda.core.data.v2.ip.IndexedAIP;
import org.roda.core.data.v2.jobs.Job;
import org.roda.core.data.v2.jobs.Job.JOB_STATE;
import org.roda.core.data.v2.jobs.JobParallelism;
import org.roda.core.data.v2.jobs.JobPriority;
import org.roda.core.data.v2.jobs.JobUserDetails;
import org.roda.core.data.v2.jobs.PluginType;
import org.roda.core.index.IndexService;
import org.roda.core.util.IdUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Startar avgränsade batchjobb som tillämpar
 * behörighetsreglerna på ett urval AIP:er via pluginet
 * {@value #PLUGIN_CLASS}.
 *
 * <p>
 * Jobbet byggs på samma sätt som gränssnittets interna jobb
 * (CommonServicesUtils.createAndExecuteJob i roda-wui, som inte får
 * importeras härifrån): UUID, urval som {@link SelectedItemsFilter} över
 * {@link IndexedAIP}, plugintyp INTERNAL samt prioritet/parallellism från
 * core.orchestrator.internal_jobs_priority/-parallelism. Fel vid start
 * loggas; inget kastas.
 * </p>
 *
 * <p>
 * Efterkommande-jobb körs som systemanvändaren
 * {@link AccessRulesModelObserver#USER} (så att pluginets
 * läsbehörighetsfilter inte gäller dem – de är regelmotorns egna), men bär
 * den utlösande användaren i jobbnamnet, i parametern
 * {@value #PARAM_TRIGGERED_BY} (pluginet stämplar updatedBy med den) och som
 * {@link JobUserDetails} (bevarandehändelserna får användaren som agent).
 * </p>
 *
 * <p>
 * Dubbletter undviks: ett efterkommande-jobb startas inte för en AIP om ett
 * ännu inte påbörjat (CREATED) efterkommande-jobb för samma AIP eller för
 * någon av dess förfäder redan väntar – det jobbet läser de nya faktan när
 * det kör. Ett redan påbörjat jobb räknas inte (det kan ha cachat gamla
 * fakta), så då startas ett nytt. Registret över väntande jobb hålls i
 * minnet (AIP-id → jobb-id) och verifieras mot modellen vid varje kontroll.
 * </p>
 */
public final class AccessRulesJobs {

  private static final Logger LOGGER = LoggerFactory.getLogger(AccessRulesJobs.class);

  /**
   * Pluginet som tillämpar reglerna (AbstractPlugin&lt;AIP&gt;), som jobbets
   * plugin-id. Strängkonstant – regelmotorpaketet ska inte bero på
   * pluginpaketet.
   */
  public static final String PLUGIN_CLASS = "org.roda.core.plugins.base.ingest.AccessRulesPlugin";
  /** Pluginparameter: "true" ⇒ beräkna men skriv inget. */
  public static final String PARAM_DRY_RUN = RodaConstants.PLUGIN_PARAMS_ACCESS_RULES_DRY_RUN;
  /** Pluginparameter: "true" ⇒ PREMIS-händelse på AIP:er vars behörigheter ändras. */
  public static final String PARAM_CREATE_EVENTS = RodaConstants.PLUGIN_PARAMS_ACCESS_RULES_CREATE_EVENTS;
  /** Pluginparameter: användaren vars ändring utlöste jobbet. */
  public static final String PARAM_TRIGGERED_BY = RodaConstants.PLUGIN_PARAMS_ACCESS_RULES_TRIGGERED_BY;
  /** Jobbnamnets prefix för efterkommande-jobb. */
  public static final String DESCENDANTS_JOB_NAME_PREFIX = "Åtkomstregler: efterkommande till ";
  /** Max antal förfäder-id:n i ett sammanslaget efterkommande-jobb (Solr tillåter högst 1024 OR-led). */
  static final int MAX_IDS_PER_JOB = 1000;

  /** Startade efterkommande-jobb som kan vara oavslutade: förfaderns AIP-id → jobb-id. */
  private static final Map<String, String> PENDING_DESCENDANTS_JOBS = new ConcurrentHashMap<>();

  private AccessRulesJobs() {
    // statisk hjälpklass
  }

  /**
   * Startar ett jobb som tillämpar reglerna på alla efterkommande (alla
   * nivåer) till en AIP – används när AIP:ns egna fakta ändrats eller den
   * flyttats, eftersom efterkommande utan egen eterna_admin ärver faktan.
   *
   * @param aipId
   *          förfaderns id
   * @param triggeredBy
   *          användaren vars ändring utlöste jobbet (null ⇒ okänd)
   * @return jobbet, eller null om det inte behövdes eller inte kunde startas
   *         (felet loggas)
   */
  public static Job launchForDescendants(String aipId, String triggeredBy) {
    Objects.requireNonNull(aipId, "aipId");
    return launchForDescendants(Collections.singletonList(aipId), triggeredBy, null);
  }

  /**
   * Som {@link #launchForDescendants(String, String)} men för flera förfäder
   * i ett jobb (urvalet är unionen av deras efterkommande). Förfäder som
   * redan täcks av ett väntande jobb hoppas över; med ett index kontrolleras
   * även deras förfäder.
   *
   * @param aipIds
   *          förfädernas id
   * @param triggeredBy
   *          användaren vars ändring utlöste jobbet (null ⇒ okänd)
   * @param index
   *          indextjänst för förfäderuppslag, eller null
   * @return det (sista) startade jobbet, eller null om inget behövdes eller
   *         inget kunde startas (fel loggas)
   */
  public static Job launchForDescendants(Collection<String> aipIds, String triggeredBy, IndexService index) {
    Objects.requireNonNull(aipIds, "aipIds");
    Set<String> remaining = dropCoveredByQueuedJob(aipIds, id -> ancestorsOf(index, id), AccessRulesJobs::jobState,
      PENDING_DESCENDANTS_JOBS);
    if (remaining.isEmpty()) {
      LOGGER.debug("Inget efterkommande-jobb behövs för {} (täcks av väntande jobb)", aipIds);
      return null;
    }
    Job last = null;
    List<String> ids = new ArrayList<>(remaining);
    for (int from = 0; from < ids.size(); from += MAX_IDS_PER_JOB) {
      List<String> chunk = ids.subList(from, Math.min(ids.size(), from + MAX_IDS_PER_JOB));
      Job job = buildDescendantsJob(chunk, triggeredBy, configuredPriority(), configuredParallelism());
      if (execute(job)) {
        for (String id : chunk) {
          PENDING_DESCENDANTS_JOBS.put(id, job.getId());
        }
        last = job;
      }
    }
    return last;
  }

  /**
   * Startar ett jobb som tillämpar reglerna på de AIP:er ett indexfilter
   * matchar.
   *
   * @param filter
   *          urvalet (mot {@link IndexedAIP})
   * @param name
   *          jobbnamn
   * @param username
   *          användare jobbet körs som
   * @param dryRun
   *          true ⇒ beräkna och rapportera men skriv inget (inga händelser
   *          skapas)
   * @return jobbet, eller null om det inte kunde startas (felet loggas)
   */
  public static Job launchForFilter(Filter filter, String name, String username, boolean dryRun) {
    Job job = buildJob(filter, name, username, dryRun, configuredPriority(), configuredParallelism());
    return execute(job) ? job : null;
  }

  private static boolean execute(Job job) {
    try {
      RodaCoreFactory.getPluginOrchestrator().createAndExecuteJobs(job, true);
      LOGGER.info("Startade åtkomstregeljobb '{}' ({}) som {}", job.getName(), job.getId(), job.getUsername());
      return true;
    } catch (RODAException | RuntimeException e) {
      LOGGER.error("Kunde inte starta åtkomstregeljobbet '{}': {}", job.getName(), e.getMessage(), e);
      return false;
    }
  }

  /**
   * Bygger efterkommande-jobbet utan att starta det. Paketprivat för tester.
   *
   * @param aipIds
   *          förfädernas id (minst ett)
   * @param triggeredBy
   *          utlösande användare eller null
   */
  static Job buildDescendantsJob(Collection<String> aipIds, String triggeredBy, JobPriority priority,
    JobParallelism parallelism) {
    List<String> ids = new ArrayList<>(new LinkedHashSet<>(aipIds));
    if (ids.isEmpty()) {
      throw new IllegalArgumentException("aipIds är tom");
    }
    Filter filter;
    String name;
    if (ids.size() == 1) {
      filter = new Filter(new SimpleFilterParameter(RodaConstants.AIP_ANCESTORS, ids.get(0)));
      name = DESCENDANTS_JOB_NAME_PREFIX + ids.get(0);
    } else {
      filter = new Filter(new OneOfManyFilterParameter(RodaConstants.AIP_ANCESTORS, ids));
      name = DESCENDANTS_JOB_NAME_PREFIX + ids.size() + " strukturenheter";
    }
    String user = triggeredBy == null || triggeredBy.isBlank() ? null : triggeredBy.trim();
    if (user != null) {
      name += " (utlöst av " + user + ")";
    }

    Job job = buildJob(filter, name, AccessRulesModelObserver.USER, false, priority, parallelism);
    if (user != null) {
      job.getPluginParameters().put(PARAM_TRIGGERED_BY, user);
      JobUserDetails details = new JobUserDetails();
      details.setUsername(user);
      details.setRole(RodaConstants.PreservationAgentRole.IMPLEMENTER.toString());
      job.getJobUsersDetails().add(details);
    }
    return job;
  }

  /**
   * Bygger jobbet utan att starta det. Paketprivat för tester.
   *
   * @param dryRun
   *          true ⇒ {@value #PARAM_DRY_RUN}=true och
   *          {@value #PARAM_CREATE_EVENTS}=false; annars tvärtom
   */
  static Job buildJob(Filter filter, String name, String username, boolean dryRun, JobPriority priority,
    JobParallelism parallelism) {
    Objects.requireNonNull(filter, "filter");
    Map<String, String> parameters = new HashMap<>();
    parameters.put(PARAM_DRY_RUN, Boolean.toString(dryRun));
    parameters.put(PARAM_CREATE_EVENTS, Boolean.toString(!dryRun));

    Job job = new Job();
    job.setId(IdUtils.createUUID());
    job.setName(name == null || name.isBlank() ? "Åtkomstregler" : name);
    job.setSourceObjects(new SelectedItemsFilter<>(filter, IndexedAIP.class.getName(), false));
    job.setPlugin(PLUGIN_CLASS);
    job.setPluginType(PluginType.INTERNAL);
    job.setUsername(username == null || username.isBlank() ? AccessRulesModelObserver.USER : username);
    job.setPluginParameters(parameters);
    job.setPriority(priority == null ? JobPriority.MEDIUM : priority);
    job.setParallelism(parallelism == null ? JobParallelism.NORMAL : parallelism);
    return job;
  }

  // ---------------------------------------------------------------- dedup

  /**
   * Tar bort de AIP-id:n som redan täcks av ett väntande (CREATED)
   * efterkommande-jobb för id:t självt eller någon förfader. Poster i
   * registret vars jobb är avslutat eller saknas rensas. Paketprivat och
   * injicerbar för tester.
   *
   * @param aipIds
   *          kandidater
   * @param ancestorsOf
   *          förfäder (alla nivåer) för ett id; får kasta (⇒ inga förfäder)
   * @param jobState
   *          jobbtillstånd för ett jobb-id, null om jobbet saknas
   * @param pending
   *          registret AIP-id → jobb-id
   * @return de id:n som behöver ett nytt jobb, i ursprunglig ordning
   */
  static Set<String> dropCoveredByQueuedJob(Collection<String> aipIds, Function<String, List<String>> ancestorsOf,
    Function<String, JOB_STATE> jobState, Map<String, String> pending) {
    Set<String> remaining = new LinkedHashSet<>();
    for (String aipId : aipIds) {
      if (aipId == null || aipId.isBlank()) {
        continue;
      }
      List<String> candidates = new ArrayList<>();
      candidates.add(aipId);
      try {
        List<String> ancestors = ancestorsOf.apply(aipId);
        if (ancestors != null) {
          candidates.addAll(ancestors);
        }
      } catch (RuntimeException e) {
        LOGGER.debug("AIP {}: förfäder kunde inte hämtas för dubblettkontrollen: {}", aipId, e.getMessage());
      }
      if (!isCoveredByQueuedJob(aipId, candidates, jobState, pending)) {
        remaining.add(aipId);
      }
    }
    return remaining;
  }

  private static boolean isCoveredByQueuedJob(String aipId, List<String> candidates,
    Function<String, JOB_STATE> jobState, Map<String, String> pending) {
    for (String candidate : candidates) {
      String jobId = pending.get(candidate);
      if (jobId == null) {
        continue;
      }
      JOB_STATE state = jobState.apply(jobId);
      if (state == JOB_STATE.CREATED) {
        LOGGER.info("AIP {}: ett väntande efterkommande-jobb ({}) för {} täcker redan urvalet – inget nytt startas",
          aipId, jobId, candidate);
        return true;
      }
      if (state == null || Job.isFinalState(state)) {
        pending.remove(candidate, jobId);
      }
    }
    return false;
  }

  /** Jobbets tillstånd enligt modellen (lagringen, inte indexet); null om det saknas eller inte kan läsas. */
  private static JOB_STATE jobState(String jobId) {
    try {
      return RodaCoreFactory.getModelService().retrieveJob(jobId).getState();
    } catch (NotFoundException e) {
      return null;
    } catch (RODAException | RuntimeException e) {
      LOGGER.debug("Jobbet {} kunde inte läsas för dubblettkontrollen: {}", jobId, e.getMessage());
      return null;
    }
  }

  /** AIP:ns förfäder enligt indexet; tom lista utan index eller vid fel. */
  private static List<String> ancestorsOf(IndexService index, String aipId) {
    if (index == null) {
      return Collections.emptyList();
    }
    try {
      IndexedAIP indexed = index.retrieve(IndexedAIP.class, aipId,
        Collections.singletonList(RodaConstants.AIP_ANCESTORS));
      return indexed.getAncestors() == null ? Collections.emptyList() : indexed.getAncestors();
    } catch (RODAException | RuntimeException e) {
      LOGGER.debug("AIP {}: förfäder kunde inte hämtas ur indexet: {}", aipId, e.getMessage());
      return Collections.emptyList();
    }
  }

  /** Glömmer registret över väntande jobb (tester). */
  static void resetPendingForTests() {
    PENDING_DESCENDANTS_JOBS.clear();
  }

  // ---------------------------------------------------------------- konfiguration

  /** core.orchestrator.internal_jobs_priority, standard MEDIUM. */
  static JobPriority configuredPriority() {
    String priority = RodaCoreFactory.getRodaConfigurationAsString(RodaConstants.CORE_ORCHESTRATOR_PREFIX,
      RodaConstants.CORE_ORCHESTRATOR_PROP_INTERNAL_JOBS_PRIORITY);
    if (priority == null || priority.isBlank()) {
      return JobPriority.MEDIUM;
    }
    try {
      return JobPriority.valueOf(priority.trim());
    } catch (IllegalArgumentException e) {
      return JobPriority.MEDIUM;
    }
  }

  /** core.orchestrator.internal_jobs_parallelism, standard NORMAL. */
  static JobParallelism configuredParallelism() {
    String parallelism = RodaCoreFactory.getRodaConfigurationAsString(RodaConstants.CORE_ORCHESTRATOR_PREFIX,
      RodaConstants.CORE_ORCHESTRATOR_PROP_INTERNAL_JOBS_PARALLELISM);
    if (parallelism == null || parallelism.isBlank()) {
      return JobParallelism.NORMAL;
    }
    try {
      return JobParallelism.valueOf(parallelism.trim());
    } catch (IllegalArgumentException e) {
      return JobParallelism.NORMAL;
    }
  }
}

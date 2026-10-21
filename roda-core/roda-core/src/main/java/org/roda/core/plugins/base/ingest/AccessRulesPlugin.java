/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.plugins.base.ingest;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.roda.core.RodaCoreFactory;
import org.roda.core.common.access.AccessRuleRegistry;
import org.roda.core.common.access.AccessRulesApplier;
import org.roda.core.common.access.AccessRulesConfig;
import org.roda.core.common.access.AccessRulesModelObserver;
import org.roda.core.common.access.ApplyResult;
import org.roda.core.common.access.ApplyResultTexts;
import org.roda.core.common.access.EternaAdminFacts;
import org.roda.core.common.access.MergeResult;
import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.common.RodaConstants.PreservationEventType;
import org.roda.core.data.exceptions.InvalidParameterException;
import org.roda.core.data.exceptions.RODAException;
import org.roda.core.data.v2.LiteOptionalWithCause;
import org.roda.core.data.v2.ip.AIP;
import org.roda.core.data.v2.ip.AIPState;
import org.roda.core.data.v2.ip.IndexedAIP;
import org.roda.core.data.v2.ip.Permissions;
import org.roda.core.data.v2.ip.Permissions.PermissionType;
import org.roda.core.data.v2.jobs.Job;
import org.roda.core.data.v2.jobs.PluginParameter;
import org.roda.core.data.v2.jobs.PluginParameter.PluginParameterType;
import org.roda.core.data.v2.jobs.PluginState;
import org.roda.core.data.v2.jobs.PluginType;
import org.roda.core.data.v2.jobs.Report;
import org.roda.core.data.v2.user.User;
import org.roda.core.index.IndexService;
import org.roda.core.model.ModelService;
import org.roda.core.plugins.AbstractPlugin;
import org.roda.core.plugins.Plugin;
import org.roda.core.plugins.PluginException;
import org.roda.core.plugins.PluginHelper;
import org.roda.core.plugins.RODAObjectsProcessingLogic;
import org.roda.core.plugins.orchestrate.JobPluginInfo;
import org.roda.core.plugins.orchestrate.JobsHelper;
import org.roda.core.storage.fs.FSUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tillämpar åtkomstreglerna (access-rules.json) på
 * AIP:ers behörigheter via {@link AccessRulesApplier}.
 *
 * <p>
 * Pluginet används på två sätt:
 * </p>
 * <ul>
 * <li>som <em>inleveranssteg</em> i {@code ConfigurableIngestPlugin}
 * (obligatoriskt steg; stegbrytaren
 * {@link RodaConstants#PLUGIN_PARAMS_DO_APPLY_ACCESS_RULES} definieras av
 * inleveranspluginet, inte här), där bara {@link #execute} anropas, ingen
 * CSV-rapport skapas och torrkörning aldrig gäller (steget tvingar
 * dryRun=false så att parametern inte kan smugglas in via
 * inleveransjobbets parametrar);</li>
 * <li>som <em>fristående åtgärd</em> under Bevarande → Åtgärder (kategori
 * maintenance) på valfritt urval AIP:er, med torrkörning som val. Då samlas
 * en CSV-rad per AIP i en jobbspecifik fil under
 * {@code <arbetskatalog>/access-rules/<jobId>.csv} som i
 * {@link #afterAllExecute} bifogas jobbet som
 * {@code atkomstregler-<jobId>.csv}.</li>
 * </ul>
 *
 * <p>
 * Behörighetsfilter: ett jobb startat av en användare (allt utom
 * inleverans och regelmotorns egna jobb, som körs som
 * {@link AccessRulesModelObserver#USER}) behandlar bara AIP:er som
 * jobbanvändaren har READ på; övriga rapporteras SKIPPED med neutral text
 * och utan titel/fakta, så att rapport och CSV-bilaga inte avslöjar
 * sekretessläget för strukturenheter användaren inte får se. I övrigt
 * följer åtgärden RODA:s jobbmodell (rollen job.manage avgör vem som får
 * starta den).
 * </p>
 *
 * <p>
 * Huvudbrytaren core.access_rules.enabled = false ger SKIPPED för alla
 * AIP:er. En PREMIS-händelse (UPDATE) skapas bara när behörigheter faktiskt
 * ändrades och skrevs, när parametern access_rules.create_events är satt och
 * – under inleverans – core.access_rules.ingest.create_event är satt. Rena
 * tillståndsskrivningar (rulesVersion/factsSource) görs bara när parametern
 * access_rules.write_state är satt (standard av: skrivvägen omindexerar hela
 * AIP:n) och aldrig under inleverans; de ger ingen händelse. Pluginets egna
 * skrivningar görs inom {@link AccessRulesModelObserver#runGuarded} så att
 * observern inte utvärderar varje AIP en gång till.
 * </p>
 *
 * <p>
 * Orkestreraren kör batcher parallellt på egna klonade plugininstanser;
 * applikatorn (med sin faktacache) skapas därför lat per instans, och
 * tillägg i jobbets CSV-fil serialiseras med ett klassgemensamt lås.
 * </p>
 */
public class AccessRulesPlugin extends AbstractPlugin<AIP> {
  private static final Logger LOGGER = LoggerFactory.getLogger(AccessRulesPlugin.class);

  /** Katalog under arbetskatalogen där jobbens CSV-rapporter samlas. */
  public static final String CSV_WORK_FOLDER = "access-rules";
  /** Bilagans filnamn blir prefix + jobId + ".csv". */
  public static final String CSV_ATTACHMENT_PREFIX = "atkomstregler-";
  /** Kolumner i CSV-rapporten, i ordning (samma uppgifter som rapporttexten). */
  public static final List<String> CSV_HEADER = Collections.unmodifiableList(Arrays.asList("aipId", "title", "level",
    "faktakalla", "sekretess", "lagrum", "personuppgifter", "niva", "grupperTillagda", "grupperBorttagna",
    "skrivbehorigheterBorttagna", "skrivbehorigheterAterstallda", "varningar", "utfall"));
  /** Avskiljare mellan flera värden i en CSV-cell. */
  public static final String MULTI_VALUE_SEPARATOR = ApplyResultTexts.MULTI_VALUE_SEPARATOR;

  static final String EVENT_DESCRIPTION = ApplyResultTexts.EVENT_DESCRIPTION;
  static final String CONFIG_CSV_DELIMITER_KEY = "csv.delimiter";
  static final String SKIPPED_NO_READ = "Jobbanvändaren saknar läsbehörighet till strukturenheten – hoppas över";

  /** Batcher körs parallellt i samma JVM; tillägg i jobbets CSV-fil serialiseras. */
  private static final Object CSV_LOCK = new Object();

  private static final Map<String, PluginParameter> pluginParameters = new HashMap<>();

  static {
    pluginParameters.put(RodaConstants.PLUGIN_PARAMS_ACCESS_RULES_DRY_RUN,
      PluginParameter
        .getBuilder(RodaConstants.PLUGIN_PARAMS_ACCESS_RULES_DRY_RUN, "Torrkörning", PluginParameterType.BOOLEAN)
        .withDefaultValue("false")
        .withDescription("Beräkna och rapportera vilka behörighetsändringar reglerna skulle ge, utan att skriva "
          + "något till strukturenheterna.")
        .build());
    pluginParameters.put(RodaConstants.PLUGIN_PARAMS_ACCESS_RULES_CREATE_EVENTS,
      PluginParameter
        .getBuilder(RodaConstants.PLUGIN_PARAMS_ACCESS_RULES_CREATE_EVENTS, "Skapa bevarandehändelser",
          PluginParameterType.BOOLEAN)
        .withDefaultValue("true")
        .withDescription("Registrera en bevarandehändelse (PREMIS) på varje strukturenhet vars behörigheter "
          + "ändras. Under inleverans krävs dessutom core.access_rules.ingest.create_event.")
        .build());
    pluginParameters.put(RodaConstants.PLUGIN_PARAMS_ACCESS_RULES_WRITE_STATE,
      PluginParameter
        .getBuilder(RodaConstants.PLUGIN_PARAMS_ACCESS_RULES_WRITE_STATE, "Stämpla regeltillstånd",
          PluginParameterType.BOOLEAN)
        .withDefaultValue("false")
        .withDescription("Skriv regelversion och faktakälla (accessRuleState) även på strukturenheter vars "
          + "behörigheter inte ändras, så att drift mot en ny regelversion syns. Varje sådan skrivning "
          + "omindexerar hela strukturenheten; använd sparsamt på stora urval.")
        .build());
  }

  private boolean dryRun = false;
  private boolean createEvents = true;
  private boolean writeState = false;
  /** Skapas lat per plugininstans (en instans per batch); bär faktacachen. */
  private AccessRulesApplier applier;

  public static String getStaticName() {
    return "Tillämpa åtkomstregler";
  }

  public static String getStaticDescription() {
    return "Tillämpar åtkomstreglerna i access-rules.json på strukturenheternas behörigheter: regler per grupp "
      + "spärrar eller återställer läsbehörighet (READ) utifrån eterna_admin-metadata – sekretess, lagrum, "
      + "personuppgifter och personuppgiftsnivå. Reglerna ger aldrig behörighet som gruppen inte redan hade och "
      + "rör bara grupper som nämns i regelfilen. Som fristående åtgärd kan den köras på valfritt urval (även som "
      + "torrkörning) och bifogar en CSV-rapport till jobbet.";
  }

  @Override
  public void init() throws PluginException {
    // do nothing
  }

  @Override
  public void shutdown() {
    // do nothing
  }

  @Override
  public String getName() {
    return getStaticName();
  }

  @Override
  public String getDescription() {
    return getStaticDescription();
  }

  @Override
  public String getVersionImpl() {
    return "1.0";
  }

  @Override
  public List<PluginParameter> getParameters() {
    ArrayList<PluginParameter> parameters = new ArrayList<>();
    parameters.add(pluginParameters.get(RodaConstants.PLUGIN_PARAMS_ACCESS_RULES_DRY_RUN));
    parameters.add(pluginParameters.get(RodaConstants.PLUGIN_PARAMS_ACCESS_RULES_CREATE_EVENTS));
    parameters.add(pluginParameters.get(RodaConstants.PLUGIN_PARAMS_ACCESS_RULES_WRITE_STATE));
    return parameters;
  }

  @Override
  public void setParameterValues(Map<String, String> parameters) throws InvalidParameterException {
    super.setParameterValues(parameters);
    dryRun = booleanParameter(parameters, RodaConstants.PLUGIN_PARAMS_ACCESS_RULES_DRY_RUN, false);
    createEvents = booleanParameter(parameters, RodaConstants.PLUGIN_PARAMS_ACCESS_RULES_CREATE_EVENTS, true);
    writeState = booleanParameter(parameters, RodaConstants.PLUGIN_PARAMS_ACCESS_RULES_WRITE_STATE, false);
  }

  private static boolean booleanParameter(Map<String, String> parameters, String key, boolean defaultValue) {
    if (parameters == null) {
      return defaultValue;
    }
    String value = parameters.get(key);
    return value == null || value.isBlank() ? defaultValue : Boolean.parseBoolean(value.trim());
  }

  boolean isDryRun() {
    return dryRun;
  }

  boolean isCreateEvents() {
    return createEvents;
  }

  boolean isWriteState() {
    return writeState;
  }

  @Override
  public boolean areParameterValuesValid() {
    return true;
  }

  @Override
  public Report beforeAllExecute(IndexService index, ModelService model) throws PluginException {
    String jobId = PluginHelper.getJobId(this);
    if (jobId != null) {
      // rester från en tidigare körning med samma jobb-id ska inte blandas in
      FSUtils.deletePathQuietly(csvWorkFile(jobId));
    }
    return new Report();
  }

  @Override
  public Report execute(IndexService index, ModelService model, List<LiteOptionalWithCause> liteList)
    throws PluginException {
    return PluginHelper.processObjects(this, (RODAObjectsProcessingLogic<AIP>) (index1, model1, report, cachedJob,
      jobPluginInfo, plugin, aips) -> processAIPs(index1, model1, report, cachedJob, jobPluginInfo, aips), index,
      model, liteList);
  }

  /** Batchgemensamma avgöranden. */
  private static final class Context {
    boolean rulesEnabled;
    boolean ingest;
    boolean dryRun;
    boolean eventsAllowed;
    /** Jobbanvändaren när läsbehörighet ska kontrolleras; null ⇒ ingen kontroll (inleverans/systemjobb). */
    User readFilterUser;
    /** true när kontrollen krävs men användaren inte kunde hämtas (allt hoppas över). */
    boolean readFilterUnavailable;
    /** Stämplas som updatedBy: utlösande användare för regelmotorns egna jobb, annars jobbanvändaren. */
    String updatedBy;
    /** Utlösande användare för automatiska jobb (null annars). */
    String triggeredBy;
    /** Jobbet som bevarandehändelserna skapas för, se {@link AccessRulesPlugin#eventJob(Job, String)}. */
    Job eventJob;
    /** CSV-rader, eller null när pluginet körs som steg i ett annat plugin. */
    List<List<String>> csvRows;
  }

  private void processAIPs(IndexService index, ModelService model, Report report, Job cachedJob,
    JobPluginInfo jobPluginInfo, List<AIP> aips) {
    Context ctx = new Context();
    ctx.rulesEnabled = AccessRulesConfig.isEnabled();
    ctx.ingest = isIngest(cachedJob);
    // inleverans är aldrig torrkörning, oavsett vilka parametrar jobbet bär
    ctx.dryRun = dryRun && !ctx.ingest;
    ctx.eventsAllowed = createEvents && (!ctx.ingest || AccessRulesConfig.isIngestEventEnabled());
    ctx.triggeredBy = triggeredBy(cachedJob);
    ctx.updatedBy = ctx.triggeredBy != null ? ctx.triggeredBy : jobUsername(cachedJob);
    ctx.eventJob = eventJob(cachedJob, ctx.triggeredBy);
    if (cachedJob != null && !ctx.ingest && !isSystemJob(cachedJob)) {
      ctx.readFilterUser = retrieveJobUser(model, cachedJob);
      ctx.readFilterUnavailable = ctx.readFilterUser == null;
    }
    // CSV bara när detta plugin är jobbets eget (afterAllExecute körs då);
    // som inleveranssteg anropas enbart execute
    ctx.csvRows = isJobPlugin(cachedJob) ? new ArrayList<>() : null;

    for (AIP aip : aips) {
      processAIP(index, model, report, cachedJob, jobPluginInfo, aip, ctx);
    }

    if (ctx.csvRows != null && !ctx.csvRows.isEmpty()) {
      appendCsvRows(PluginHelper.getJobId(this), ctx.csvRows);
    }
  }

  private void processAIP(IndexService index, ModelService model, Report report, Job cachedJob,
    JobPluginInfo jobPluginInfo, AIP aip, Context ctx) {
    LOGGER.debug("Tillämpar åtkomstregler på AIP {} (torrkörning={})", aip.getId(), ctx.dryRun);
    Report reportItem = initReportItem(aip);
    PluginHelper.updatePartialJobReport(this, model, reportItem, false, cachedJob);

    PluginState state;
    String details;
    ApplyResult result = null;
    boolean readable = true;

    if (ctx.readFilterUnavailable) {
      state = PluginState.SKIPPED;
      details = "Jobbanvändaren kunde inte hämtas – läsbehörigheten kan inte kontrolleras, hoppas över";
      readable = false;
    } else if (ctx.readFilterUser != null && !userCanRead(ctx.readFilterUser, aip.getPermissions())) {
      state = PluginState.SKIPPED;
      details = SKIPPED_NO_READ;
      readable = false;
    } else if (!ctx.rulesEnabled) {
      state = PluginState.SKIPPED;
      details = "Åtkomstreglerna är avstängda i konfigurationen (core.access_rules.enabled = false)";
    } else {
      try {
        AccessRulesApplier applier = applier(model, ctx.ingest);
        result = AccessRulesModelObserver.runGuarded(() -> applier.applyToAip(aip, ctx.updatedBy, ctx.dryRun));
        if (result.isSkipped()) {
          state = PluginState.SKIPPED;
          details = "Hoppades över: " + result.getSkipReason();
        } else {
          state = PluginState.SUCCESS;
          details = ApplyResultTexts.describe(result, ctx.dryRun);
          if (ctx.triggeredBy != null) {
            details = "Automatiskt efterkommande-jobb utlöst av " + ctx.triggeredBy + "\n" + details;
          }
        }
      } catch (RODAException | RuntimeException e) {
        LOGGER.error("Kunde inte tillämpa åtkomstreglerna på AIP {}: {}", aip.getId(), e.getMessage(), e);
        state = PluginState.FAILURE;
        details = "Kunde inte tillämpa åtkomstreglerna: " + e.getMessage();
        result = null;
      }
    }

    switch (state) {
      case SUCCESS:
        jobPluginInfo.incrementObjectsProcessedWithSuccess();
        break;
      case SKIPPED:
        jobPluginInfo.incrementObjectsProcessedWithSkipped();
        break;
      default:
        jobPluginInfo.incrementObjectsProcessedWithFailure();
        break;
    }
    reportItem.setPluginState(state).setPluginDetails(details);
    report.addReport(reportItem);
    PluginHelper.updatePartialJobReport(this, model, reportItem, true, cachedJob);

    if (ctx.eventsAllowed && result != null && result.isWritten() && result.isPermissionsChanged()) {
      try {
        PluginHelper.createPluginEvent(this, aip.getId(), model, index, PluginState.SUCCESS, details, true,
          ctx.eventJob);
      } catch (RODAException | RuntimeException e) {
        LOGGER.error("Kunde inte skapa bevarandehändelse för AIP {}: {}", aip.getId(), e.getMessage(), e);
      }
    }

    if (ctx.csvRows != null) {
      ctx.csvRows.add(readable ? csvRow(index, aip, result, state, details, ctx.dryRun)
        : csvRowWithoutDetails(aip, state, details));
    }
  }

  /** Under inleverans är AIP:n i INGEST_PROCESSING och rapportposten ska bära det tillståndet. */
  private Report initReportItem(AIP aip) {
    if (AIPState.INGEST_PROCESSING.equals(aip.getState())) {
      return PluginHelper.initPluginReportItem(this, aip.getId(), AIP.class, AIPState.INGEST_PROCESSING);
    }
    return PluginHelper.initPluginReportItem(this, aip.getId(), AIP.class);
  }

  /**
   * En applikator per plugininstans: faktacachen gör förfäderuppslag billiga
   * inom batchen. Rena tillståndsskrivningar bara på begäran och aldrig
   * under inleverans (tillståndet stämplas ändå när behörigheter ändras).
   */
  private AccessRulesApplier applier(ModelService model, boolean ingest) {
    if (applier == null) {
      applier = new AccessRulesApplier(model, AccessRuleRegistry.getInstance().get(), AccessRulesConfig.defaultAct());
    }
    applier.setStateOnlyWrites(writeState && !ingest);
    return applier;
  }

  private static String jobUsername(Job cachedJob) {
    return cachedJob != null ? cachedJob.getUsername() : null;
  }

  private static boolean isIngest(Job cachedJob) {
    return cachedJob != null && PluginType.INGEST.equals(cachedJob.getPluginType());
  }

  /** true för regelmotorns egna jobb (startade av observern som systemanvändaren). */
  static boolean isSystemJob(Job cachedJob) {
    return cachedJob != null && AccessRulesModelObserver.USER.equals(cachedJob.getUsername());
  }

  /** Utlösande användare enligt jobbparametern, bara för systemjobb (annars kan parametern inte litas på). */
  static String triggeredBy(Job cachedJob) {
    if (!isSystemJob(cachedJob) || cachedJob.getPluginParameters() == null) {
      return null;
    }
    String user = cachedJob.getPluginParameters().get(RodaConstants.PLUGIN_PARAMS_ACCESS_RULES_TRIGGERED_BY);
    return user == null || user.isBlank() ? null : user.trim();
  }

  /**
   * Jobbet som bevarandehändelserna skapas för. PluginHelper.createPluginEvent
   * ger modellen jobbets användarnamn som händelsens skapare, och
   * createPreservationMetadata stämplar då AIP:ns updatedBy med det – för ett
   * systemjobb skulle det skriva över den utlösande användaren som appliern
   * nyss stämplat. Därför en kopia av jobbet med den utlösande användaren som
   * användarnamn (agenterna, JobUserDetails, är desamma); annars jobbet
   * självt.
   */
  static Job eventJob(Job cachedJob, String triggeredBy) {
    if (cachedJob == null || triggeredBy == null) {
      return cachedJob;
    }
    return new Job(cachedJob).setUsername(triggeredBy);
  }

  /** true när jobbet kör detta plugin direkt (inte som steg i ett annat plugin). */
  private boolean isJobPlugin(Job cachedJob) {
    return cachedJob != null && getClass().getName().equals(cachedJob.getPlugin());
  }

  /** Jobbanvändaren ur modellen (som SearchExportPlugin); null om den inte kan hämtas (loggas). */
  private static User retrieveJobUser(ModelService model, Job cachedJob) {
    try {
      return model.retrieveUser(cachedJob.getUsername());
    } catch (RODAException | RuntimeException e) {
      LOGGER.warn("Jobbanvändaren '{}' kunde inte hämtas – åtkomstregeljobbet hoppar över alla strukturenheter: {}",
        cachedJob.getUsername(), e.getMessage());
      return null;
    }
  }

  /**
   * true om användaren har READ på behörigheterna: administratören alltid,
   * annars via egen post eller någon av sina grupper (samma regel som
   * UserUtility.checkAIPPermissions).
   */
  static boolean userCanRead(User user, Permissions permissions) {
    if (user == null || user.getName() == null) {
      return false;
    }
    if (RodaConstants.ADMIN.equals(user.getName())) {
      return true;
    }
    if (permissions == null) {
      return false;
    }
    Set<PermissionType> own = permissions.getUserPermissions(user.getName());
    if (own != null && own.contains(PermissionType.READ)) {
      return true;
    }
    if (user.getGroups() != null) {
      for (String group : user.getGroups()) {
        Set<PermissionType> groupPermissions = permissions.getGroupPermissions(group);
        if (groupPermissions != null && groupPermissions.contains(PermissionType.READ)) {
          return true;
        }
      }
    }
    return false;
  }

  // -------------------------------------------------------------- CSV-rapport

  /** En rad enligt {@link #CSV_HEADER}; titel och nivå slås upp i indexet (bästa försök). */
  private static List<String> csvRow(IndexService index, AIP aip, ApplyResult result, PluginState state,
    String details, boolean dryRun) {
    String[] titleAndLevel = titleAndLevel(index, aip.getId());
    EternaAdminFacts facts = result == null ? null : result.getFacts();
    MergeResult merge = result == null ? null : result.getMerge();

    List<String> row = new ArrayList<>(CSV_HEADER.size());
    row.add(aip.getId());
    row.add(titleAndLevel[0]);
    row.add(titleAndLevel[1]);
    row.add(result == null || result.isSkipped() ? "" : ApplyResultTexts.factsSourceText(facts));
    row.add(facts == null ? "" : nullToEmpty(facts.getSekretess()));
    row.add(ApplyResultTexts.join(ApplyResultTexts.lagrumTexts(facts)));
    row.add(facts == null ? "" : nullToEmpty(facts.getPersonuppgifter()));
    row.add(facts == null ? "" : nullToEmpty(facts.getPersonuppgiftsniva()));
    row.add(merge == null ? "" : ApplyResultTexts.join(merge.getAdded()));
    row.add(merge == null ? "" : ApplyResultTexts.join(merge.getRemoved()));
    row.add(merge == null ? "" : ApplyResultTexts.join(merge.getWriteStripped()));
    row.add(merge == null ? "" : ApplyResultTexts.join(merge.getWriteRestored()));
    row.add(result == null ? "" : ApplyResultTexts.join(ApplyResultTexts.warnings(result)));
    if (PluginState.SUCCESS.equals(state) && result != null) {
      row.add(state.name() + ": " + ApplyResultTexts.outcomeText(result, dryRun));
    } else {
      row.add(state.name() + ": " + firstLine(details));
    }
    return sanitizeRow(row);
  }

  /** Rad för en AIP jobbanvändaren inte får läsa: bara id och utfall. */
  private static List<String> csvRowWithoutDetails(AIP aip, PluginState state, String details) {
    List<String> row = new ArrayList<>(CSV_HEADER.size());
    row.add(aip.getId());
    while (row.size() < CSV_HEADER.size() - 1) {
      row.add("");
    }
    row.add(state.name() + ": " + firstLine(details));
    return sanitizeRow(row);
  }

  private static List<String> sanitizeRow(List<String> row) {
    List<String> sanitized = new ArrayList<>(row.size());
    for (String cell : row) {
      sanitized.add(sanitizeCell(cell));
    }
    return sanitized;
  }

  /**
   * Skydd mot formelinjektion i kalkylprogram: en cell som börjar med =, +,
   * - eller @ får en inledande apostrof, och tabb/radbrytning ersätts med
   * blanksteg (titlar kommer från producentens metadata).
   */
  static String sanitizeCell(String value) {
    if (value == null || value.isEmpty()) {
      return "";
    }
    String cell = value.replace('\t', ' ').replace('\r', ' ').replace('\n', ' ');
    char first = cell.charAt(0);
    if (first == '=' || first == '+' || first == '-' || first == '@') {
      return "'" + cell;
    }
    return cell;
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  private static String[] titleAndLevel(IndexService index, String aipId) {
    try {
      IndexedAIP indexed = index.retrieve(IndexedAIP.class, aipId,
        Arrays.asList(RodaConstants.AIP_TITLE, RodaConstants.AIP_LEVEL));
      return new String[] {nullToEmpty(indexed.getTitle()), nullToEmpty(indexed.getLevel())};
    } catch (RODAException | RuntimeException e) {
      LOGGER.debug("Titel/nivå för AIP {} kunde inte hämtas ur indexet: {}", aipId, e.getMessage());
      return new String[] {"", ""};
    }
  }

  private static String firstLine(String text) {
    if (text == null) {
      return "";
    }
    int newline = text.indexOf('\n');
    return newline < 0 ? text : text.substring(0, newline);
  }

  /** Arbetsfilen access-rules/<jobId>.csv under arbetskatalogen. */
  static Path csvWorkFile(String jobId) {
    return RodaCoreFactory.getWorkingDirectory().resolve(CSV_WORK_FOLDER).resolve(jobId + ".csv");
  }

  /** Samma csv.delimiter-konfiguration som sökexporten; standard är komma. */
  static CSVFormat csvFormat() {
    String delimiter = RodaCoreFactory.getRodaConfiguration().getString(CONFIG_CSV_DELIMITER_KEY);
    if (delimiter == null || delimiter.isBlank()) {
      delimiter = String.valueOf(CSVFormat.DEFAULT.getDelimiter());
    }
    return CSVFormat.DEFAULT.withDelimiter(delimiter.charAt(0)).withRecordSeparator("\n");
  }

  /** Lägger till raderna i jobbets CSV-fil (UTF-8); rubrikraden skrivs när filen skapas. */
  private static void appendCsvRows(String jobId, List<List<String>> rows) {
    if (jobId == null) {
      LOGGER.warn("Jobb-id saknas – CSV-rapporten för åtkomstreglerna skrivs inte");
      return;
    }
    Path file = csvWorkFile(jobId);
    synchronized (CSV_LOCK) {
      try {
        Files.createDirectories(file.getParent());
        boolean isNew = Files.notExists(file);
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
          StandardOpenOption.APPEND); CSVPrinter printer = new CSVPrinter(writer, csvFormat())) {
          if (isNew) {
            printer.printRecord(CSV_HEADER);
          }
          for (List<String> row : rows) {
            printer.printRecord(row);
          }
        }
      } catch (IOException e) {
        LOGGER.error("Kunde inte skriva CSV-rapporten {} för jobb {}: {}", file, jobId, e.getMessage(), e);
      }
    }
  }

  @Override
  public Report afterAllExecute(IndexService index, ModelService model) throws PluginException {
    String jobId = PluginHelper.getJobId(this);
    if (jobId == null) {
      return new Report();
    }
    Path file = csvWorkFile(jobId);
    if (!Files.isRegularFile(file)) {
      LOGGER.debug("Ingen CSV-rapport att bifoga för jobb {} (inga AIP:er behandlade)", jobId);
      return new Report();
    }
    Path named = file.resolveSibling(CSV_ATTACHMENT_PREFIX + jobId + ".csv");
    try {
      Files.move(file, named, StandardCopyOption.REPLACE_EXISTING);
      JobsHelper.createJobAttachment(jobId, named);
    } catch (IOException | RODAException e) {
      LOGGER.error("Kunde inte bifoga CSV-rapporten för jobb {}: {}", jobId, e.getMessage(), e);
    } finally {
      FSUtils.deletePathQuietly(named);
      FSUtils.deletePathQuietly(file);
    }
    return new Report();
  }

  @Override
  public Plugin<AIP> cloneMe() {
    return new AccessRulesPlugin();
  }

  @Override
  public PluginType getType() {
    return PluginType.AIP_TO_AIP;
  }

  @Override
  public PreservationEventType getPreservationEventType() {
    return PreservationEventType.UPDATE;
  }

  @Override
  public String getPreservationEventDescription() {
    return EVENT_DESCRIPTION;
  }

  @Override
  public String getPreservationEventSuccessMessage() {
    return "Åtkomstreglerna tillämpades och behörigheterna uppdaterades";
  }

  @Override
  public String getPreservationEventFailureMessage() {
    return "Åtkomstreglerna kunde inte tillämpas på behörigheterna";
  }

  @Override
  public String getPreservationEventSkippedMessage() {
    return "Tillämpningen av åtkomstreglerna hoppades över";
  }

  @Override
  public List<String> getCategories() {
    return Collections.singletonList(RodaConstants.PLUGIN_CATEGORY_MAINTENANCE);
  }

  @Override
  public List<Class<AIP>> getObjectClasses() {
    return Collections.singletonList(AIP.class);
  }
}

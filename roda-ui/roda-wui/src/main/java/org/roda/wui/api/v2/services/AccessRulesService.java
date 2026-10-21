/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.api.v2.services;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.roda.core.RodaCoreFactory;
import org.roda.core.common.Messages;
import org.roda.core.common.access.AccessRuleEngine;
import org.roda.core.common.access.AccessRuleRegistry;
import org.roda.core.common.access.AccessRulesConfig;
import org.roda.core.common.access.Lagrum;
import org.roda.core.common.access.LagrumParser;
import org.roda.core.common.access.LagrumPattern;
import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.exceptions.AuthorizationDeniedException;
import org.roda.core.data.exceptions.GenericException;
import org.roda.core.data.exceptions.NotFoundException;
import org.roda.core.data.exceptions.RequestNotValidException;
import org.roda.core.data.exceptions.VersionConflictException;
import org.roda.core.data.v2.accessrules.AccessRule;
import org.roda.core.data.v2.accessrules.AccessRuleSet;
import org.roda.core.data.v2.accessrules.AccessRulesStatus;
import org.roda.core.data.v2.accessrules.AccessRulesVocabulary;
import org.roda.core.data.v2.accessrules.LagrumNormalizeRequest;
import org.roda.core.data.v2.accessrules.LagrumNormalizeResult;
import org.roda.core.data.v2.index.IndexResult;
import org.roda.core.data.v2.index.filter.Filter;
import org.roda.core.data.v2.index.filter.SimpleFilterParameter;
import org.roda.core.data.v2.index.select.SelectedItemsFilter;
import org.roda.core.data.v2.index.sort.SortParameter;
import org.roda.core.data.v2.index.sort.Sorter;
import org.roda.core.data.v2.index.sublist.Sublist;
import org.roda.core.data.v2.ip.AIPState;
import org.roda.core.data.v2.ip.IndexedAIP;
import org.roda.core.data.v2.jobs.IndexedJob;
import org.roda.core.data.v2.jobs.Job;
import org.roda.core.data.v2.user.Group;
import org.roda.core.data.v2.user.User;
import org.roda.core.index.IndexService;
import org.roda.core.model.ModelService;
import org.roda.core.plugins.base.ingest.AccessRulesPlugin;
import org.roda.wui.api.v2.utils.CommonServicesUtils;
import org.roda.wui.common.server.ServerTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Tjänstelager för åtkomstreglernas REST-API.
 * Reglerna lagras i regelfilen via {@link AccessRuleRegistry} (inte i
 * modellen), så ändringar görs som läs–ändra–skriv på hela
 * {@link AccessRuleSet} och valideras mot nivåvokabulären
 * (core.access_rules.personuppgiftsniva.values[]) och gruppkatalogen
 * ({@link ModelService#listGroups()}) innan de sparas.
 *
 * <ul>
 * <li>Valideringsfel ger {@link RequestNotValidException} (400) med
 * registrets problemlista i meddelandet.</li>
 * <li>{@link #saveRuleSet} gör en optimistisk versionskontroll: en inskickad
 * version som inte är exakt den lagrade (lägre eller högre) ger
 * {@link VersionConflictException} (409).</li>
 * <li>Lagrumstexter som ska tolkas begränsas i antal och längd
 * ({@link #MAX_NORMALIZE_INPUTS}, {@link AccessRuleRegistry#MAX_PATTERN_LENGTH})
 * så att tolkningen inte kan användas för att binda requesttrådar.</li>
 * <li>Vokabulären innehåller inte gruppkatalogen: att lista grupper kräver
 * member.read, och regelredigeraren väljer grupp via MemberSelectDialog.
 * Katalogen används bara serversidigt vid validering.</li>
 * <li>De muterande metoderna serialiseras med ett lås så att två samtidiga
 * ändringar av olika grupper inte skriver över varandra (registrets
 * {@code get()} och {@code save()} är var för sig synkroniserade, men inte
 * paret).</li>
 * <li>{@link #apply} startar pluginet {@link AccessRulesPlugin} som internt
 * jobb på alla aktiva AIP:er — även sådana utan egen eterna_admin, eftersom
 * de ärver fakta från förfäder och annars behandlas som ej bedömda.</li>
 * </ul>
 */
@Service
public class AccessRulesService {

  private static final Logger LOGGER = LoggerFactory.getLogger(AccessRulesService.class);

  /** Visningsordning för sekretessvärden (eterna_admin.xsd); värden som motorn känner men som saknas här läggs sist. */
  static final List<String> SEKRETESS_ORDER = List.of(AccessRuleEngine.EJ_BEDOMD, AccessRuleEngine.INGEN_SEKRETESS,
    "sekretess_forekommer", "helt_sekretessbelagd");
  /** Visningsordning för personuppgiftsvärden (eterna_admin.xsd). */
  static final List<String> PERSONUPPGIFTER_ORDER = List.of(AccessRuleEngine.EJ_BEDOMD, "inga", "forekommer",
    "kansliga_forekommer");

  /** ServerMessages-prefix för etiketter (nyckel = prefix + värde). */
  static final String LABEL_PREFIX_SEKRETESS = "eterna_admin.sekretess.";
  static final String LABEL_PREFIX_PERSONUPPGIFTER = "eterna_admin.personuppgifter.";
  static final String LABEL_PREFIX_PERSONUPPGIFTSNIVA = "eterna_admin.personuppgiftsniva.";

  /** Hur många av de senaste åtkomstregeljobben som gås igenom för att hitta det senaste som inte var en torrkörning. */
  static final int STATUS_JOB_CANDIDATES = 20;

  /** Högsta antal texter per normaliseringsanrop (regelredigeraren skickar en per lagrumsrad). */
  static final int MAX_NORMALIZE_INPUTS = 50;

  /** Serialiserar läs–ändra–skriv mot registret. */
  private static final Object LOCK = new Object();

  /** Hela regeluppsättningen (en kopia; se {@link AccessRuleRegistry#get()}). */
  public AccessRuleSet getRules() {
    return AccessRuleRegistry.getInstance().get();
  }

  /**
   * Regeln för en grupp.
   *
   * @throws NotFoundException
   *           om gruppen saknar regel
   */
  public AccessRule getRule(String group) throws NotFoundException {
    AccessRule rule = getRules().findRule(group);
    if (rule == null) {
      throw new NotFoundException("Gruppen '" + group + "' har ingen åtkomstregel");
    }
    return rule;
  }

  /**
   * Skapar eller ersätter regeln för en grupp (läs–ändra–skriv på hela
   * regeluppsättningen). Gruppnamnet i sökvägen gäller; anger kroppen ett
   * annat gruppnamn avvisas begäran.
   *
   * @return den sparade (normaliserade) regeln
   * @throws RequestNotValidException
   *           vid ogiltig regel (400)
   * @throws GenericException
   *           vid skrivfel eller om gruppkatalogen inte kan läsas
   */
  public AccessRule saveRule(String group, AccessRule rule, User user, ModelService model)
    throws RequestNotValidException, GenericException {
    if (rule == null) {
      throw new RequestNotValidException("Åtkomstregel saknas");
    }
    String name = requireGroup(group);
    if (rule.getGroup() != null && !rule.getGroup().isBlank() && !name.equals(rule.getGroup().trim())) {
      throw new RequestNotValidException(
        "Gruppnamnet i sökvägen ('" + name + "') skiljer sig från regelns ('" + rule.getGroup().trim() + "')");
    }
    rule.setGroup(name);

    synchronized (LOCK) {
      AccessRuleRegistry registry = AccessRuleRegistry.getInstance();
      AccessRuleSet set = registry.get();
      AccessRule existing = set.findRule(name);
      if (existing == null) {
        set.getRules().add(rule);
      } else {
        set.getRules().set(set.getRules().indexOf(existing), rule);
      }
      save(registry, set, user, model);
      return registry.get().findRule(name);
    }
  }

  /**
   * Tar bort regeln för en grupp; gruppen blir då ohanterad (tidigare
   * borttagen READ återställs inte förrän reglerna tillämpas på nytt – se
   * motorn).
   *
   * @throws NotFoundException
   *           om gruppen saknar regel
   */
  public void deleteRule(String group, User user, ModelService model)
    throws NotFoundException, RequestNotValidException, GenericException {
    String name = requireGroup(group);
    synchronized (LOCK) {
      AccessRuleRegistry registry = AccessRuleRegistry.getInstance();
      AccessRuleSet set = registry.get();
      AccessRule existing = set.findRule(name);
      if (existing == null) {
        throw new NotFoundException("Gruppen '" + name + "' har ingen åtkomstregel");
      }
      set.getRules().remove(existing);
      save(registry, set, user, model);
    }
  }

  /**
   * Ersätter hela regeluppsättningen (policyval, enabled och alla regler).
   * Den inskickade {@code version} måste vara exakt den lagrade (optimistisk
   * låsning – en klient som inte läst reglerna först kan inte skriva över
   * dem); registret räknar sedan upp versionen och stämplar
   * updatedBy/updatedOn.
   *
   * @return den sparade (normaliserade) regeluppsättningen med ny version
   * @throws VersionConflictException
   *           om inskickad version skiljer sig från den lagrade (409)
   */
  public AccessRuleSet saveRuleSet(AccessRuleSet set, User user, ModelService model)
    throws VersionConflictException, RequestNotValidException, GenericException {
    if (set == null) {
      throw new RequestNotValidException("Regeluppsättning saknas");
    }
    synchronized (LOCK) {
      AccessRuleRegistry registry = AccessRuleRegistry.getInstance();
      AccessRuleSet current = registry.get();
      if (set.getVersion() != current.getVersion()) {
        throw new VersionConflictException("Regeluppsättningen har ändrats sedan den lästes (lagrad version "
          + current.getVersion() + (current.getUpdatedBy() != null ? " av " + current.getUpdatedBy() : "")
          + ", inskickad version " + set.getVersion() + ") – läs om reglerna och gör om ändringen");
      }
      save(registry, set, user, model);
      return registry.get();
    }
  }

  /**
   * Statusöversikt: effektivt aktiv (huvudbrytaren core.access_rules.enabled
   * och regelfilens enabled), version, hanterade grupper samt senaste
   * tillämpningsjobb (senaste jobb med {@link AccessRulesPlugin} som inte
   * var en torrkörning; efterkommande-jobb från observern räknas också).
   * {@code driftCount} beräknas inte här.
   */
  public AccessRulesStatus status(IndexService index) throws GenericException, RequestNotValidException {
    AccessRuleSet set = getRules();
    AccessRulesStatus status = new AccessRulesStatus();
    status.setEnabled(AccessRulesConfig.isEnabled() && set.isEnabled());
    status.setVersion(set.getVersion());
    status.setManagedGroups(new ArrayList<>(set.getManagedGroups()));

    IndexedJob last = findLastAppliedJob(index);
    if (last != null) {
      status.setLastJobId(last.getId());
      status.setLastAppliedOn(toIso(last.getStartDate()));
    }
    return status;
  }

  /**
   * Vokabulären för regelredigeraren med etiketter på begärt språk
   * (null/tomt ⇒ sv_SE enligt {@link ServerTools#parseLocale}). Innehåller
   * inte gruppkatalogen (kräver member.read).
   */
  public AccessRulesVocabulary vocabulary(String localeString) {
    AccessRuleRegistry registry = AccessRuleRegistry.getInstance();
    AccessRulesVocabulary vocabulary = new AccessRulesVocabulary();
    vocabulary.setSekretess(ordered(AccessRuleEngine.SEKRETESS_VALUES, SEKRETESS_ORDER));
    vocabulary.setPersonuppgifter(ordered(AccessRuleEngine.PERSONUPPGIFTER_VALUES, PERSONUPPGIFTER_ORDER));
    List<String> niva = registry.getPersonuppgiftsnivaValues();
    vocabulary.setPersonuppgiftsniva(niva == null ? new ArrayList<>() : new ArrayList<>(niva));

    Messages messages = RodaCoreFactory.getI18NMessages(ServerTools.parseLocale(localeString));
    Map<String, String> labels = new LinkedHashMap<>();
    addLabels(labels, messages, LABEL_PREFIX_SEKRETESS, vocabulary.getSekretess());
    addLabels(labels, messages, LABEL_PREFIX_PERSONUPPGIFTER, vocabulary.getPersonuppgifter());
    addLabels(labels, messages, LABEL_PREFIX_PERSONUPPGIFTSNIVA, vocabulary.getPersonuppgiftsniva());
    vocabulary.setLabels(labels);
    return vocabulary;
  }

  /**
   * Normaliserar fritext till regelfilens lagrumsmönster med
   * {@link LagrumParser} (tolerant: en text får innehålla flera lagrum,
   * varje tolkat lagrum blir ett mönster) och regeluppsättningens
   * {@code defaults.defaultAct} för delar utan lagbeteckning — samma
   * tolkning som valideringen använder. "*" släpps igenom oförändrat;
   * tomma texter hoppas över. Otolkbara delar ger varningar, inga mönster.
   * En text som ger fler än ett mönster ger också en varning, eftersom en
   * regel bara godtar ett lagrum per post och sparningen annars avvisar
   * raden som förhandsvisningen godkände.
   *
   * @throws RequestNotValidException
   *           vid fler än {@link #MAX_NORMALIZE_INPUTS} texter eller en text
   *           längre än {@link AccessRuleRegistry#MAX_PATTERN_LENGTH} tecken
   */
  public LagrumNormalizeResult normalize(LagrumNormalizeRequest request) throws RequestNotValidException {
    LagrumNormalizeResult result = new LagrumNormalizeResult();
    if (request == null) {
      return result;
    }
    if (request.getInput().size() > MAX_NORMALIZE_INPUTS) {
      throw new RequestNotValidException(
        "Högst " + MAX_NORMALIZE_INPUTS + " lagrumstexter per anrop, fick " + request.getInput().size());
    }
    for (String text : request.getInput()) {
      if (text != null && text.length() > AccessRuleRegistry.MAX_PATTERN_LENGTH) {
        throw new RequestNotValidException("Lagrumstexten är längre än " + AccessRuleRegistry.MAX_PATTERN_LENGTH
          + " tecken: \"" + text.substring(0, 40) + "…\"");
      }
    }
    result.setInput(new ArrayList<>(request.getInput()));
    String defaultAct = getRules().getDefaults().getDefaultAct();

    Set<String> normalized = new LinkedHashSet<>();
    List<String> warnings = new ArrayList<>();
    for (String text : request.getInput()) {
      if (text == null || text.isBlank()) {
        continue;
      }
      String trimmed = text.trim();
      if (LagrumPattern.WILDCARD.equals(trimmed)) {
        normalized.add(LagrumPattern.WILDCARD);
        continue;
      }
      LagrumParser.ParseResult parsed = LagrumParser.parse(trimmed, defaultAct);
      List<String> patterns = new ArrayList<>();
      for (Lagrum lagrum : parsed.getLagrum()) {
        patterns.add(LagrumPattern.of(lagrum).toString());
      }
      normalized.addAll(patterns);
      warnings.addAll(parsed.getWarnings());
      if (patterns.size() > 1) {
        warnings.add("\"" + trimmed + "\" ger flera lagrum (" + String.join(", ", patterns)
          + ") – en regel godtar ett lagrum per rad, dela upp texten eller använd den normaliserade formen");
      }
    }
    result.setNormalized(new ArrayList<>(normalized));
    result.setWarnings(warnings);
    return result;
  }

  /**
   * Startar tillämpningsjobbet (pluginet {@link AccessRulesPlugin}) som
   * internt jobb på alla aktiva AIP:er i användarens namn. Pluginet hoppar
   * över AIP:er användaren saknar READ på. Vid torrkörning skrivs inget och
   * inga bevarandehändelser skapas; i båda fallen bifogas en CSV-rapport
   * till jobbet.
   */
  public Job apply(User user, boolean dryRun)
    throws GenericException, RequestNotValidException, NotFoundException, AuthorizationDeniedException {
    Map<String, String> pluginParameters = new HashMap<>();
    pluginParameters.put(RodaConstants.PLUGIN_PARAMS_ACCESS_RULES_DRY_RUN, Boolean.toString(dryRun));
    pluginParameters.put(RodaConstants.PLUGIN_PARAMS_ACCESS_RULES_CREATE_EVENTS, Boolean.toString(!dryRun));

    String name = AccessRulesPlugin.getStaticName() + (dryRun ? " (torrkörning)" : "");
    Job job = CommonServicesUtils.createAndExecuteInternalJob(name,
      new SelectedItemsFilter<>(new Filter(new SimpleFilterParameter(RodaConstants.AIP_STATE, AIPState.ACTIVE.name())),
        IndexedAIP.class.getName(), false),
      AccessRulesPlugin.class, user, pluginParameters, "Could not apply access rules");
    LOGGER.info("Startade åtkomstregeljobb '{}' ({}) som {}", job.getName(), job.getId(), user.getName());
    return job;
  }

  // ---------------------------------------------------------------- hjälpare

  private static void save(AccessRuleRegistry registry, AccessRuleSet set, User user, ModelService model)
    throws RequestNotValidException, GenericException {
    List<String> problems = AccessRuleRegistry.validate(set, registry.getPersonuppgiftsnivaValues(),
      knownGroups(model));
    if (!problems.isEmpty()) {
      throw new RequestNotValidException("Ogiltig regeluppsättning: " + String.join("; ", problems));
    }
    try {
      registry.save(set, user.getName());
    } catch (IllegalArgumentException e) {
      throw new RequestNotValidException(e.getMessage(), e);
    } catch (IOException e) {
      throw new GenericException("Kunde inte spara åtkomstreglerna: " + e.getMessage(), e);
    }
  }

  private static String requireGroup(String group) throws RequestNotValidException {
    if (group == null || group.isBlank()) {
      throw new RequestNotValidException("Gruppnamn saknas");
    }
    return group.trim();
  }

  /** Gruppnamnen i katalogen (LDAP). */
  private static List<String> knownGroups(ModelService model) throws GenericException {
    List<String> names = new ArrayList<>();
    for (Group group : model.listGroups()) {
      if (group.getName() != null && !group.getName().isBlank()) {
        names.add(group.getName());
      }
    }
    return names;
  }

  /** Senaste åtkomstregeljobb (startdatum fallande) som inte var en torrkörning, eller null. */
  private static IndexedJob findLastAppliedJob(IndexService index) throws GenericException, RequestNotValidException {
    Filter filter = new Filter(new SimpleFilterParameter(RodaConstants.JOB_PLUGIN, AccessRulesPlugin.class.getName()));
    Sorter sorter = new Sorter(new SortParameter(RodaConstants.JOB_START_DATE, true));
    IndexResult<IndexedJob> jobs = index.find(IndexedJob.class, filter, sorter, new Sublist(0, STATUS_JOB_CANDIDATES),
      Arrays.asList(RodaConstants.INDEX_UUID, RodaConstants.JOB_ID, RodaConstants.JOB_START_DATE,
        RodaConstants.JOB_PLUGIN_PARAMETERS));
    for (IndexedJob job : jobs.getResults()) {
      Map<String, String> parameters = job.getPluginParameters();
      boolean dryRun = parameters != null
        && Boolean.parseBoolean(parameters.get(RodaConstants.PLUGIN_PARAMS_ACCESS_RULES_DRY_RUN));
      if (!dryRun) {
        return job;
      }
    }
    return null;
  }

  /** Värdena i föredragen ordning; övriga (okända för ordningslistan) läggs sist, sorterade. */
  private static List<String> ordered(Collection<String> values, List<String> preferredOrder) {
    List<String> result = new ArrayList<>();
    for (String value : preferredOrder) {
      if (values.contains(value)) {
        result.add(value);
      }
    }
    List<String> rest = new ArrayList<>();
    for (String value : values) {
      if (!result.contains(value)) {
        rest.add(value);
      }
    }
    Collections.sort(rest);
    result.addAll(rest);
    return result;
  }

  private static void addLabels(Map<String, String> labels, Messages messages, String prefix, List<String> values) {
    for (String value : values) {
      labels.put(value, messages == null ? value : messages.getTranslation(prefix + value, value));
    }
  }

  /** ISO-8601 UTC med sekundprecision, eller null. */
  private static String toIso(Date date) {
    return date == null ? null : DateTimeFormatter.ISO_INSTANT.format(date.toInstant().truncatedTo(ChronoUnit.SECONDS));
  }
}

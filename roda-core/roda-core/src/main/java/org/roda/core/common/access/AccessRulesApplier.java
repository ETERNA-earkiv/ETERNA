/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.common.access;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.exceptions.GenericException;
import org.roda.core.data.exceptions.NotFoundException;
import org.roda.core.data.exceptions.RODAException;
import org.roda.core.data.v2.accessrules.AccessRuleSet;
import org.roda.core.data.v2.accessrules.AccessRuleState;
import org.roda.core.data.v2.ip.AIP;
import org.roda.core.data.v2.ip.Permissions;
import org.roda.core.data.v2.ip.metadata.DescriptiveMetadata;
import org.roda.core.data.v2.user.Group;
import org.roda.core.model.ModelService;
import org.roda.core.storage.Binary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tillämpar behörighetsreglerna på en AIP — hämtar
 * fakta ur AIP:ns eterna_admin-metadata (eller närmaste förfaders, om
 * defaults.missingMetadata = inherit eller inherit-closed; saknar även alla
 * förfäder eterna_admin är objektet obedömd struktur, som lämnas öppen med
 * inherit och behandlas som ej bedömt med inherit-closed), låter
 * {@link AccessRuleEngine} besluta, slår ihop beslutet med befintliga
 * behörigheter via {@link PermissionsMerger} och skriver resultatet till
 * modellen.
 *
 * <p>
 * Metadataposten hittas på normaliserat typ-id: RODA lagrar det
 * konfigurerade typ-id:t "eterna_admin" uppdelat på sista understrecket
 * (type="eterna", version="admin"), så både den formen och
 * (type="eterna_admin", version=null) matchar.
 * </p>
 *
 * <p>
 * Skrivning sker alltid mot en <em>färsk</em> kopia av AIP:n
 * ({@code ModelService.retrieveAIP}) som hämtas omedelbart före skrivningen
 * och som beslutet slås ihop med på nytt; så rullas inte behörighetsändringar
 * som gjorts under en lång batch tillbaka. Resultatet speglas på anroparens
 * AIP-objekt. När behörigheter ändras skrivs AIP:n med
 * {@code ModelService.updateAIPPermissions(AIP, String)} (behörighetsindex
 * uppdateras). När inget ändras men AIP:n saknar regeltillstånd, har
 * utvärderats mot en annan regelversion eller fått en annan faktakälla
 * skrivs bara tillståndet med {@code ModelService.updateAIP(AIP, String)} —
 * den metoden skriver om aip.json och meddelar observatörerna (omindexering)
 * men skapar ingen PREMIS-händelse, så den används för att hålla
 * rulesVersion aktuell (gör "drift" synlig) utan att förorena
 * händelsehistoriken. Sådana rena tillståndsskrivningar kan stängas av med
 * {@link #setStateOnlyWrites(boolean)} (lämpligt för observern).
 * </p>
 *
 * <p>
 * Avstängd regelfil ({@code enabled=false}) betyder "inga begränsningar":
 * motorn tillåter alla hanterade grupper och mergern återställer READ och
 * borttagna skrivbehörigheter; tillståndet tas bort när inget återstår.
 * Ingen tillståndsstämpling görs då. Saknar regelfilen helt regler (inga
 * hanterade grupper) hoppas AIP:n över. Den hårda "rör ingenting"-brytaren
 * är core.access_rules.enabled i roda-core.properties (hanteras av
 * anroparna: jobb, observer, ingest).
 * </p>
 *
 * <p>
 * Nekade grupper förlorar som standard även UPDATE/DELETE/GRANT (konfig
 * core.access_rules.strip_write_on_deny, se {@link PermissionsMerger}), så
 * att gruppen inte själv kan ändra den metadata beslutet bygger på.
 * </p>
 *
 * <p>
 * Instansen håller en cache per körning med de effektiva faktan per AIP-id,
 * så att tusentals syskon under samma förfader bara läser förfaderns
 * metadata en gång. Skapa en ny instans per jobb/händelse; klassen är inte
 * trådsäker. Cachade fakta ska behandlas som oföränderliga.
 * </p>
 *
 * <p>
 * Felhantering: undantag från modellen propageras som {@link RODAException}.
 * Ett eterna_admin-dokument som inte kan tolkas behandlas däremot som
 * <em>obedömt</em> (fakta present utan värden, med varning) så att trasig
 * indata aldrig ger ett öppnare resultat än korrekt indata; felet loggas och
 * syns i resultatets fakta. En förfader som inte kan hämtas (NotFound)
 * avbryter arvet på samma sätt (objektet behandlas som obedömt).
 * </p>
 */
public class AccessRulesApplier {

  private static final Logger LOGGER = LoggerFactory.getLogger(AccessRulesApplier.class);

  /** Metadatatyp när core.access_rules.metadata_type saknas. */
  public static final String DEFAULT_METADATA_TYPE = AccessRulesConfig.DEFAULT_METADATA_TYPE;
  /** Max antal förfäder som följs vid arv av fakta. */
  public static final int MAX_INHERIT_DEPTH = 64;
  /** factsSource när AIP:n har egen eterna_admin. */
  public static final String FACTS_SOURCE_SELF = "self";
  /** factsSource när varken egen eller ärvd eterna_admin finns. */
  public static final String FACTS_SOURCE_NONE = "none";
  /** Standard för core.access_rules.strip_write_on_deny. */
  public static final boolean DEFAULT_STRIP_WRITE_ON_DENY = AccessRulesConfig.DEFAULT_STRIP_WRITE_ON_DENY;
  /** Så länge gäller en gjord gruppkontroll mot katalogen innan den görs om (grupper kan tillkomma). */
  static final long GROUP_CHECK_TTL_MS = 60L * 60 * 1000;

  static final String POLICY_INHERIT = "inherit";

  /**
   * Hanterade gruppuppsättningar (sorterad lista som nyckel) som redan
   * kontrollerats mot katalogen, med tidpunkt – kontrollen (ett LDAP-anrop)
   * görs en gång per regelversion i JVM:n, inte per applikatorinstans
   * (observern skapar en instans per händelse).
   */
  private static final Map<String, Long> CHECKED_GROUP_SETS = new ConcurrentHashMap<>();

  private final ModelService model;
  private final AccessRuleSet rules;
  private final String defaultAct;
  private final String metadataType;
  private boolean stripWriteOnDeny = DEFAULT_STRIP_WRITE_ON_DENY;
  private boolean stateOnlyWrites = true;
  /** Effektiva fakta (egna eller ärvda) per AIP-id, för denna körning. */
  private final Map<String, EternaAdminFacts> factsCache = new HashMap<>();
  /** Varningstexter som redan loggats på WARN i denna körning. */
  private final Set<String> loggedWarnings = new HashSet<>();
  private boolean groupsChecked;

  /**
   * @param model
   *          modelltjänsten
   * @param rules
   *          regeluppsättningen (normalt {@code AccessRuleRegistry.getInstance().get()})
   * @param defaultAct
   *          lag som antas när lagrumstexten saknar lagnamn (konfig
   *          core.access_rules.default_lag); defaults.defaultAct i regelfilen
   *          har företräde
   */
  public AccessRulesApplier(ModelService model, AccessRuleSet rules, String defaultAct) {
    this(model, rules, defaultAct, AccessRulesConfig.metadataType());
    this.stripWriteOnDeny = AccessRulesConfig.isStripWriteOnDeny();
  }

  /**
   * Som ovan men med explicit metadatatyp och utan att läsa konfigurationen
   * (tester och verktyg). stripWriteOnDeny = {@link #DEFAULT_STRIP_WRITE_ON_DENY}.
   */
  public AccessRulesApplier(ModelService model, AccessRuleSet rules, String defaultAct, String metadataType) {
    this.model = model;
    this.rules = rules;
    this.defaultAct = effectiveDefaultAct(rules, defaultAct);
    this.metadataType = metadataType == null || metadataType.isBlank() ? DEFAULT_METADATA_TYPE
      : metadataType.trim();
  }

  /** Glömmer gjorda gruppkontroller så att nästa applikator kontrollerar igen (tester). */
  static void resetGroupCheckForTests() {
    CHECKED_GROUP_SETS.clear();
  }

  public AccessRuleSet getRules() {
    return rules;
  }

  public String getDefaultAct() {
    return defaultAct;
  }

  public String getMetadataType() {
    return metadataType;
  }

  public boolean isStripWriteOnDeny() {
    return stripWriteOnDeny;
  }

  /** true ⇒ nekade grupper förlorar även UPDATE/DELETE/GRANT (se {@link PermissionsMerger}). */
  public void setStripWriteOnDeny(boolean stripWriteOnDeny) {
    this.stripWriteOnDeny = stripWriteOnDeny;
  }

  public boolean isStateOnlyWrites() {
    return stateOnlyWrites;
  }

  /**
   * false ⇒ AIP:n skrivs bara när behörigheter ändras, aldrig enbart för att
   * stämpla rulesVersion/factsSource (lämpligt för observern; batchjobbet
   * bör låta det vara true så att "drift" blir synlig).
   */
  public void setStateOnlyWrites(boolean stateOnlyWrites) {
    this.stateOnlyWrites = stateOnlyWrites;
  }

  /**
   * Tillämpar reglerna på en AIP.
   *
   * @param aip
   *          AIP:n (med beskrivande metadata och behörigheter inlästa);
   *          uppdateras med skrivet resultat när något skrivs
   * @param user
   *          användare som stämplas som updatedBy
   * @param dryRun
   *          true ⇒ beräkna men skriv inget
   * @throws RODAException
   *           vid modellfel
   */
  public ApplyResult applyToAip(AIP aip, String user, boolean dryRun) throws RODAException {
    Objects.requireNonNull(aip, "aip");
    if (aip.getId() == null) {
      throw new IllegalArgumentException("AIP saknar id");
    }

    ApplyResult result = new ApplyResult();
    if (rules == null) {
      result.setSkipped(true);
      result.setSkipReason("Regeluppsättning saknas");
      return result;
    }
    if (rules.getManagedGroups().isEmpty()) {
      result.setSkipped(true);
      result.setSkipReason("Regelfilen innehåller inga regler (inga hanterade grupper)");
      return result;
    }
    warnUnknownGroupsOnce();

    // avstängda regler: faktan är irrelevant (alla hanterade grupper tillåts)
    EternaAdminFacts facts = rules.isEnabled() ? resolveFacts(aip) : EternaAdminFacts.absent();
    String now = AccessRuleRegistry.nowIso();
    result = evaluate(aip, facts, now);
    logMergeWarnings(aip.getId(), result.getMerge());

    if (result.isWriteNeeded() && !dryRun) {
      AIP fresh = model.retrieveAIP(aip.getId());
      ApplyResult freshResult = result;
      if (fresh != aip) {
        if (!Objects.equals(fresh.getPermissions(), aip.getPermissions())) {
          LOGGER.info("AIP {}: behörigheterna ändrades under utvärderingen – beslutet tillämpas på den färska kopian",
            aip.getId());
        }
        freshResult = evaluate(fresh, facts, now);
        logMergeWarnings(aip.getId(), freshResult.getMerge());
      }
      if (freshResult.isWriteNeeded()) {
        MergeResult merge = freshResult.getMerge();
        fresh.setAccessRuleState(merge.getState());
        if (merge.isChanged()) {
          fresh.setPermissions(merge.getPermissions());
          model.updateAIPPermissions(fresh, user);
        } else {
          model.updateAIP(fresh, user);
        }
        freshResult.setWritten(true);
        if (fresh != aip) {
          aip.setPermissions(fresh.getPermissions());
          aip.setAccessRuleState(fresh.getAccessRuleState());
          aip.setUpdatedOn(fresh.getUpdatedOn());
          aip.setUpdatedBy(fresh.getUpdatedBy());
        }
      }
      result = freshResult;
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("AIP {} (torrkörning={}): {}; beslut: {}", aip.getId(), dryRun, result.summary(),
        result.getDecision().getExplanation());
    }
    return result;
  }

  /**
   * Ren utvärdering utan modellanrop: beslut, sammanslagning och
   * regeltillstånd för givna fakta. Muterar inte AIP:n. Paketprivat för
   * tester.
   */
  ApplyResult evaluate(AIP aip, EternaAdminFacts facts, String appliedOn) {
    ApplyResult result = new ApplyResult();
    result.setFacts(facts);

    AccessDecision decision = AccessRuleEngine.evaluate(rules, facts);
    result.setDecision(decision);

    Permissions current = aip.getPermissions() != null ? aip.getPermissions() : new Permissions();
    String rulesVersion = String.valueOf(rules.getVersion());
    MergeResult merge = PermissionsMerger.apply(current, aip.getAccessRuleState(), decision, rulesVersion, appliedOn,
      stripWriteOnDeny);
    AccessRuleState state = Objects.requireNonNull(merge.getState(), "PermissionsMerger gav inget tillstånd");
    state.setFactsSource(factsSource(facts));
    result.setMerge(merge);

    boolean stateRefresh;
    if (!rules.isEnabled()) {
      // avstängda regler: lyft begränsningar men stämpla inte om hela
      // beståndet; töm tillståndet när inget återstår att återställa
      if (state.getRestrictedGroups().isEmpty() && state.getRemovedPermissions().isEmpty()) {
        merge.setState(null);
      }
      stateRefresh = false;
    } else {
      stateRefresh = stateOnlyWrites && stateRefreshNeeded(aip.getAccessRuleState(), state);
    }
    result.setWriteNeeded(merge.isChanged() || stateRefresh);
    result.setStateOnly(!merge.isChanged() && stateRefresh);
    return result;
  }

  /**
   * Effektiva fakta för AIP:n: egna om de finns, annars (vid
   * missingMetadata = inherit eller inherit-closed) närmaste förfaders,
   * annars {@link EternaAdminFacts#absent()} (obedömd struktur – motorn
   * avgör utifrån policyn om den lämnas öppen eller behandlas som ej
   * bedömd). Cachas per AIP-id.
   */
  EternaAdminFacts resolveFacts(AIP aip) throws RODAException {
    EternaAdminFacts cachedFacts = factsCache.get(aip.getId());
    if (cachedFacts != null) {
      return cachedFacts;
    }
    EternaAdminFacts own = readOwnFacts(aip);
    EternaAdminFacts effective = own.isPresent() || !isInheritPolicy(rules) ? own : inheritFromAncestors(aip);
    factsCache.put(aip.getId(), effective);
    return effective;
  }

  /**
   * Går uppåt i föräldrakedjan tills en förfader med eterna_admin hittas
   * (eller en vars effektiva fakta redan är cachade). Mellanliggande
   * förfäder utan metadata cachas med samma ärvda fakta. En förfader som
   * inte finns avbryter arvet (⇒ absent).
   */
  private EternaAdminFacts inheritFromAncestors(AIP aip) throws RODAException {
    Set<String> visited = new HashSet<>();
    visited.add(aip.getId());
    List<String> intermediate = new ArrayList<>();
    EternaAdminFacts found = EternaAdminFacts.absent();

    String parentId = aip.getParentId();
    int depth = 0;
    while (parentId != null && !parentId.isBlank()) {
      if (!visited.add(parentId)) {
        LOGGER.warn("AIP {}: cirkulär föräldrakedja vid {} – arv av eterna_admin avbryts", aip.getId(), parentId);
        break;
      }
      if (++depth > MAX_INHERIT_DEPTH) {
        LOGGER.warn("AIP {}: föräldrakedjan är djupare än {} – arv av eterna_admin avbryts", aip.getId(),
          MAX_INHERIT_DEPTH);
        break;
      }
      EternaAdminFacts cachedFacts = factsCache.get(parentId);
      if (cachedFacts != null) {
        found = cachedFacts;
        break;
      }
      AIP ancestor;
      try {
        ancestor = model.retrieveAIP(parentId);
      } catch (NotFoundException e) {
        LOGGER.warn("AIP {}: förfadern {} finns inte – arv av eterna_admin avbryts (objektet behandlas som obedömt)",
          aip.getId(), parentId);
        break;
      }
      EternaAdminFacts ancestorOwn = readOwnFacts(ancestor);
      if (ancestorOwn.isPresent()) {
        factsCache.put(parentId, ancestorOwn);
        found = ancestorOwn;
        break;
      }
      intermediate.add(parentId);
      parentId = ancestor.getParentId();
    }

    EternaAdminFacts inherited = found.isPresent() ? inheritedCopy(found) : EternaAdminFacts.absent();
    for (String id : intermediate) {
      factsCache.put(id, inherited);
    }
    return inherited;
  }

  /**
   * Läser AIP:ns egna eterna_admin (ingen cache, inget arv).
   *
   * @return fakta med sourceAipId = AIP:ns id; absent om metadatan saknas;
   *         present utan värden (obedömt, med varning) om dokumentet inte
   *         kan tolkas
   */
  EternaAdminFacts readOwnFacts(AIP aip) throws RODAException {
    DescriptiveMetadata dm = findAdminMetadata(aip, metadataType);
    if (dm == null) {
      return EternaAdminFacts.absent();
    }
    Binary binary = model.retrieveDescriptiveMetadataBinary(aip.getId(), dm.getId());
    EternaAdminFacts facts;
    try (InputStream in = binary.getContent().createInputStream()) {
      facts = EternaAdminParser.parse(in, defaultAct);
    } catch (IOException e) {
      LOGGER.error("AIP {}: {}-metadata '{}' kan inte tolkas ({}) – objektet behandlas som obedömt (stängt)",
        aip.getId(), metadataType, dm.getId(), e.getMessage());
      facts = new EternaAdminFacts();
      facts.setPresent(true);
      facts.getWarnings().add(metadataType + " '" + dm.getId() + "' kunde inte tolkas: " + e.getMessage());
    }
    facts.setSourceAipId(aip.getId());
    facts.setInherited(false);
    return facts;
  }

  /**
   * AIP:ns egna eterna_admin-post: beskrivande metadata på AIP-nivå (inte
   * representation) vars normaliserade typ-id (type, eller
   * type_version när version finns) är lika med den konfigurerade typen,
   * skiftlägesokänsligt. Träffar alltså både (type="eterna", version="admin")
   * som RODA skriver och (type="eterna_admin", version=null).
   */
  static DescriptiveMetadata findAdminMetadata(AIP aip, String metadataType) {
    if (aip == null || aip.getDescriptiveMetadata() == null || metadataType == null) {
      return null;
    }
    for (DescriptiveMetadata dm : aip.getDescriptiveMetadata()) {
      if (dm != null && dm.isFromAIP() && metadataType.equalsIgnoreCase(metadataTypeId(dm))) {
        return dm;
      }
    }
    return null;
  }

  /** "type" eller "type_version" – samma form som UI:t och bulk-pluginet använder. */
  static String metadataTypeId(DescriptiveMetadata dm) {
    if (dm.getType() == null) {
      return null;
    }
    if (dm.getVersion() == null || dm.getVersion().isEmpty()) {
      return dm.getType();
    }
    return dm.getType() + RodaConstants.METADATA_VERSION_SEPARATOR + dm.getVersion();
  }

  /** Kopia av förfaderns fakta märkt som ärvd; sourceAipId pekar på ägaren. */
  static EternaAdminFacts inheritedCopy(EternaAdminFacts source) {
    EternaAdminFacts copy = new EternaAdminFacts();
    copy.setPresent(source.isPresent());
    copy.setSekretess(source.getSekretess());
    copy.setSekretessgrund(source.getSekretessgrund());
    copy.setLagrum(new ArrayList<>(source.getLagrum()));
    copy.setWarnings(new ArrayList<>(source.getWarnings()));
    copy.setPersonuppgifter(source.getPersonuppgifter());
    copy.setPersonuppgiftsniva(source.getPersonuppgiftsniva());
    copy.setSourceAipId(source.getSourceAipId());
    copy.setInherited(true);
    return copy;
  }

  /** "self", förfaderns AIP-id eller "none". */
  static String factsSource(EternaAdminFacts facts) {
    if (facts == null || !facts.isPresent()) {
      return FACTS_SOURCE_NONE;
    }
    if (facts.isInherited()) {
      return facts.getSourceAipId() != null ? facts.getSourceAipId() : FACTS_SOURCE_NONE;
    }
    return FACTS_SOURCE_SELF;
  }

  /**
   * true när tillståndet saknas, gäller en annan regelversion eller har en
   * annan faktakälla (spårbarheten ska vara aktuell).
   */
  static boolean stateRefreshNeeded(AccessRuleState current, AccessRuleState next) {
    if (next == null) {
      return false;
    }
    return current == null || !Objects.equals(current.getRulesVersion(), next.getRulesVersion())
      || !Objects.equals(current.getFactsSource(), next.getFactsSource());
  }

  static boolean isInheritPolicy(AccessRuleSet rules) {
    if (rules == null || rules.getDefaults() == null || rules.getDefaults().getMissingMetadata() == null) {
      return false;
    }
    String policy = rules.getDefaults().getMissingMetadata().trim();
    return POLICY_INHERIT.equalsIgnoreCase(policy) || "inherit-closed".equalsIgnoreCase(policy);
  }

  /** defaults.defaultAct i regelfilen, annars konfigvärdet, annars OSL. */
  static String effectiveDefaultAct(AccessRuleSet rules, String configured) {
    if (rules != null && rules.getDefaults() != null && rules.getDefaults().getDefaultAct() != null
      && !rules.getDefaults().getDefaultAct().isBlank()) {
      return rules.getDefaults().getDefaultAct().trim();
    }
    if (configured != null && !configured.isBlank()) {
      return configured.trim();
    }
    return LagrumParser.DEFAULT_ACT;
  }

  /**
   * Loggar hanterade grupper som inte finns i katalogen – en sådan regel får
   * ingen effekt (gruppnamn jämförs exakt). Kontrollen görs en gång per
   * uppsättning hanterade grupper (och på nytt efter
   * {@link #GROUP_CHECK_TTL_MS}), inte per instans, eftersom uppslaget är ett
   * katalog-/LDAP-anrop. Fel vid uppslag ignoreras (kontrollen är bara en
   * varning).
   */
  private void warnUnknownGroupsOnce() {
    if (groupsChecked) {
      return;
    }
    groupsChecked = true;
    String key = new TreeSet<>(rules.getManagedGroups()).toString();
    long now = System.currentTimeMillis();
    Long checkedAt = CHECKED_GROUP_SETS.get(key);
    if (checkedAt != null && now - checkedAt < GROUP_CHECK_TTL_MS) {
      return;
    }
    CHECKED_GROUP_SETS.put(key, now);
    try {
      List<Group> groups = model.listGroups();
      if (groups == null) {
        return;
      }
      Set<String> names = new HashSet<>();
      for (Group group : groups) {
        if (group != null && group.getName() != null) {
          names.add(group.getName());
        }
      }
      for (String managed : rules.getManagedGroups()) {
        if (!names.contains(managed)) {
          LOGGER.warn("Behörighetsregeln för gruppen '{}' matchar ingen grupp i katalogen – regeln får ingen effekt "
            + "(kontrollera stavning och skiftläge)", managed);
        }
      }
    } catch (RODAException | RuntimeException e) {
      LOGGER.debug("Kunde inte kontrollera hanterade grupper mot katalogen: {}", e.getMessage());
    }
  }

  /** Första förekomsten av en varningstext loggas på WARN, upprepningar på DEBUG. */
  private void logMergeWarnings(String aipId, MergeResult merge) {
    if (merge == null) {
      return;
    }
    for (String warning : merge.getWarnings()) {
      if (loggedWarnings.add(warning)) {
        LOGGER.warn("AIP {}: {} (upprepningar i denna körning loggas på debug-nivå)", aipId, warning);
      } else if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("AIP {}: {}", aipId, warning);
      }
    }
  }
}

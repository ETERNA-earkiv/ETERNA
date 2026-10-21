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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.roda.core.RodaCoreFactory;
import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.common.RodaConstants.PreservationEventType;
import org.roda.core.data.exceptions.GenericException;
import org.roda.core.data.exceptions.NotFoundException;
import org.roda.core.data.exceptions.RODAException;
import org.roda.core.data.exceptions.ReturnWithExceptions;
import org.roda.core.data.v2.accessrules.AccessRuleSet;
import org.roda.core.data.v2.index.filter.EmptyKeyFilterParameter;
import org.roda.core.data.v2.index.filter.Filter;
import org.roda.core.data.v2.index.filter.SimpleFilterParameter;
import org.roda.core.data.v2.ip.AIP;
import org.roda.core.data.v2.ip.IndexedAIP;
import org.roda.core.data.v2.ip.metadata.DescriptiveMetadata;
import org.roda.core.data.v2.jobs.PluginState;
import org.roda.core.index.IndexService;
import org.roda.core.model.ModelObserver;
import org.roda.core.model.ModelService;
import org.roda.core.model.NoOpModelObserver;
import org.roda.core.model.TransactionalModelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Modellobservatör som utvärderar behörighetsreglerna
 * direkt när en AIP:s eterna_admin-metadata eller behörigheter ändras, så att
 * en redigering i gränssnittet får effekt utan att batchjobbet behöver köras.
 *
 * <p>
 * Reagerar på:
 * </p>
 * <ul>
 * <li>{@link #descriptiveMetadataCreated}/{@link #descriptiveMetadataUpdated}
 * för AIP-nivåmetadata av den konfigurerade typen
 * (core.access_rules.metadata_type, normalt "eterna_admin"; RODA lagrar det
 * som type="eterna", version="admin"),</li>
 * <li>{@link #descriptiveMetadataDeleted} när posten på AIP-nivå har ett id
 * som börjar med typnamnet (efter borttagning finns typen inte kvar att
 * fråga om),</li>
 * <li>{@link #aipPermissionsUpdated} – så att en manuell behörighetsändring
 * som ger en nekad grupp READ tillbaka omedelbart rättas; återinträdesvakten
 * {@link #ACTIVE} hindrar loopen (appliern skriver via
 * {@code ModelService.updateAIPPermissions} som notifierar samma händelse
 * igen),</li>
 * <li>{@link #aipMoved} – egna/ärvda fakta kan ändras av en flytt.</li>
 * </ul>
 *
 * <p>
 * Efter en metadatahändelse eller flytt startas dessutom ett batchjobb
 * ({@link AccessRulesJobs#launchForDescendants}) för AIP:ns efterkommande,
 * om indexet visar att det finns efterkommande utan egen sekretessmarkering
 * (de ärver faktan och måste utvärderas om). Kontrollen görs mot indexfältet
 * {@value #SEKRETESS_INDEX_FIELD} (satt av eterna_admin-crosswalken); en
 * efterkommande med eterna_admin men utan sekretess-element räknas alltså
 * med – det ger på sin höjd ett jobb för mycket, aldrig ett för lite. En
 * flyttad AIP med egen eterna_admin ger inget jobb: dess efterkommande ärver
 * från den, inte från den nya föräldern. Jobbstarten kan samlas ihop med
 * {@link #deferDescendantsJobs(Runnable)} (ett jobb per batch i stället för
 * ett per AIP, t.ex. vid massredigering) och skjuts i transaktionellt läge
 * ({@link TransactionalModelService}) upp tills transaktionen committats, så
 * att jobbet ser de nya faktan.
 * </p>
 *
 * <p>
 * Under ingest skapas beskrivande metadata med {@code notify=false}
 * (AIP:n indexeras i sin helhet i slutet av ingesten), så
 * descriptiveMetadataCreated/Updated utlöses inte då; ingeststeget
 * {@code AccessRulesPlugin} tillämpar reglerna i stället. Pluginets egna
 * skrivningar görs inom {@link #runGuarded} så att observern inte gör om
 * utvärderingen.
 * </p>
 *
 * <p>
 * Spårbarhet: skrivningar stämplas med den användare vars ändring utlöste
 * dem ({@code AIP.updatedBy} efter ändringen; {@value #USER} om okänd), och
 * när behörigheter faktiskt ändras skapas en PREMIS-händelse (UPDATE, samma
 * beskrivning som pluginets) om core.access_rules.observer.create_event är
 * satt. Observern skriver aldrig enbart för att stämpla regeltillstånd
 * ({@link AccessRulesApplier#setStateOnlyWrites(boolean) stateOnlyWrites=false}).
 * </p>
 *
 * <p>
 * Felhantering är "fail closed": ett modell-/lagringsfel under
 * utvärderingen läggs i det returnerade {@link ReturnWithExceptions} som en
 * {@link GenericException} med förklarande text, och modellen kastar den
 * vidare ({@code failOnError}) så att den utlösande operationen misslyckas
 * (i transaktionellt läge rullas transaktionen tillbaka). Så kan en AIP
 * aldrig få nya fakta utan att reglerna tillämpats. En AIP som inte längre
 * finns ignoreras. Fel vid jobbstart och händelseskapande loggas bara.
 * </p>
 *
 * <p>
 * Avstängning: core.access_rules.observer.enabled=false (registreras inte
 * alls, se {@link IndexService}) eller core.access_rules.enabled=false
 * (huvudbrytaren; kontrolleras vid varje händelse).
 * </p>
 *
 * <p>
 * Konfiguration, regelregister, applikator och jobbstart är injicerbara via
 * den paketprivata konstruktorn så att klassen kan testas utan
 * {@link RodaCoreFactory}.
 * </p>
 */
public class AccessRulesModelObserver extends NoOpModelObserver {

  private static final Logger LOGGER = LoggerFactory.getLogger(AccessRulesModelObserver.class);

  /** Systemanvändare: jobbens användarnamn och updatedBy när den utlösande användaren är okänd. */
  public static final String USER = "access-rules";
  /** Indexfält som eterna_admin-crosswalken fyller när sekretess är satt. */
  public static final String SEKRETESS_INDEX_FIELD = "sekretess_s";

  /** Återinträdesvakt per tråd: true medan en omvärdering (eller pluginets egen skrivning) pågår. */
  private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> Boolean.FALSE);
  /** Per tråd: pågående uppsamling av efterkommande-jobb, se {@link #deferDescendantsJobs(Runnable)}. */
  private static final ThreadLocal<Deferred> DEFERRED = new ThreadLocal<>();

  private final ModelService model;
  private final BooleanSupplier enabled;
  private final Supplier<AccessRuleSet> rules;
  private final Function<AccessRuleSet, AccessRulesApplier> applierFactory;
  /** Konfigurerad metadatatyp, eller null ⇒ läses ur konfigurationen vid behov. */
  private final String metadataType;
  private final Predicate<String> hasDescendantsWithoutOwnFacts;
  /** Startar efterkommande-jobb för en mängd förfäder-id:n, som (för) en användare. */
  private final BiConsumer<Collection<String>, String> descendantsJobLauncher;
  private final BooleanSupplier createEvents;

  /** Åtgärd som får kasta {@link RODAException}, för {@link #runGuarded}. */
  @FunctionalInterface
  public interface GuardedAction<T> {
    T run() throws RODAException;
  }

  /**
   * Produktionskonstruktor: konfiguration via {@link AccessRulesConfig}, regler
   * via {@link AccessRuleRegistry#getInstance()}, efterkommande-kontroll mot
   * indexet och jobbstart via {@link AccessRulesJobs}.
   *
   * @param model
   *          modelltjänsten (den observern läser och skriver genom)
   * @param index
   *          indextjänsten (för efterkommande-kontrollen); null ⇒ inga
   *          efterkommande-jobb startas
   */
  public AccessRulesModelObserver(ModelService model, IndexService index) {
    this(model, AccessRulesModelObserver::configuredEnabled, () -> AccessRuleRegistry.getInstance().get(),
      rules -> new AccessRulesApplier(model, rules, AccessRulesConfig.defaultAct()), null,
      aipId -> indexHasDescendantsWithoutOwnFacts(index, aipId),
      (aipIds, user) -> AccessRulesJobs.launchForDescendants(aipIds, user, index),
      AccessRulesConfig::isObserverEventEnabled);
  }

  /**
   * Fullt injicerbar konstruktor (tester).
   *
   * @param model
   *          modelltjänsten
   * @param enabled
   *          true ⇒ observern är aktiv (motsvarar core.access_rules.enabled
   *          &amp;&amp; core.access_rules.observer.enabled)
   * @param rules
   *          leverantör av regeluppsättningen
   * @param applierFactory
   *          skapar en applikator för en regeluppsättning
   * @param metadataType
   *          metadatatyp-id (t.ex. "eterna_admin") eller null ⇒ konfiguration
   * @param hasDescendantsWithoutOwnFacts
   *          true för ett AIP-id med efterkommande som saknar egen
   *          sekretessmarkering
   * @param descendantsJobLauncher
   *          startar efterkommande-jobbet för förfäder-id:n och utlösande
   *          användare
   * @param createEvents
   *          true ⇒ PREMIS-händelse när behörigheter ändras
   */
  AccessRulesModelObserver(ModelService model, BooleanSupplier enabled, Supplier<AccessRuleSet> rules,
    Function<AccessRuleSet, AccessRulesApplier> applierFactory, String metadataType,
    Predicate<String> hasDescendantsWithoutOwnFacts, BiConsumer<Collection<String>, String> descendantsJobLauncher,
    BooleanSupplier createEvents) {
    this.model = Objects.requireNonNull(model, "model");
    this.enabled = Objects.requireNonNull(enabled, "enabled");
    this.rules = Objects.requireNonNull(rules, "rules");
    this.applierFactory = Objects.requireNonNull(applierFactory, "applierFactory");
    this.metadataType = metadataType == null || metadataType.isBlank() ? null : metadataType.trim();
    this.hasDescendantsWithoutOwnFacts = Objects.requireNonNull(hasDescendantsWithoutOwnFacts,
      "hasDescendantsWithoutOwnFacts");
    this.descendantsJobLauncher = Objects.requireNonNull(descendantsJobLauncher, "descendantsJobLauncher");
    this.createEvents = Objects.requireNonNull(createEvents, "createEvents");
  }

  // ---------------------------------------------------------------- vakt och uppsamling

  /**
   * Kör en åtgärd med återinträdesvakten satt: modellhändelser som åtgärden
   * utlöser i samma tråd (t.ex. aipPermissionsUpdated från
   * {@code AccessRulesApplier.applyToAip}) ger varken ny utvärdering eller
   * efterkommande-jobb. Används av pluginet runt sina egna skrivningar –
   * resultatet är per definition konsistent med reglerna.
   */
  public static <T> T runGuarded(GuardedAction<T> action) throws RODAException {
    boolean wasActive = Boolean.TRUE.equals(ACTIVE.get());
    ACTIVE.set(Boolean.TRUE);
    try {
      return action.run();
    } finally {
      if (wasActive) {
        ACTIVE.set(Boolean.TRUE);
      } else {
        ACTIVE.remove();
      }
    }
  }

  /**
   * Samlar de efterkommande-jobb som händelser i tråden under {@code body}
   * skulle starta och startar i stället ett enda jobb (per observer) när
   * {@code body} är klar – även om den kastar, eftersom de metadataändringar
   * som redan gjorts består. Nästlade anrop samlas i det yttersta. Avsett
   * för batchplugin som ändrar eterna_admin på många AIP:er
   * (massredigering).
   */
  public static void deferDescendantsJobs(Runnable body) {
    if (DEFERRED.get() != null) {
      body.run();
      return;
    }
    Deferred deferred = new Deferred();
    DEFERRED.set(deferred);
    try {
      body.run();
    } finally {
      DEFERRED.remove();
      deferred.flush();
    }
  }

  /** Uppsamlade förfäder-id:n per observer, med den senast sedda utlösande användaren. */
  private static final class Deferred {
    private final Map<AccessRulesModelObserver, Set<String>> idsByObserver = new LinkedHashMap<>();
    private String user;

    void add(AccessRulesModelObserver observer, String aipId, String user) {
      idsByObserver.computeIfAbsent(observer, key -> new LinkedHashSet<>()).add(aipId);
      if (user != null) {
        this.user = user;
      }
    }

    void flush() {
      for (Map.Entry<AccessRulesModelObserver, Set<String>> entry : idsByObserver.entrySet()) {
        try {
          entry.getKey().launch(entry.getValue(), user);
        } catch (RuntimeException e) {
          LOGGER.error("Kunde inte starta uppsamlat åtkomstregeljobb för {}: {}", entry.getValue(), e.getMessage(),
            e);
        }
      }
    }
  }

  // ---------------------------------------------------------------- konfiguration

  /** Båda brytarna: core.access_rules.observer.enabled och core.access_rules.enabled. */
  static boolean configuredEnabled() {
    return AccessRulesConfig.isObserverEnabled() && AccessRulesConfig.isEnabled();
  }

  // ---------------------------------------------------------------- hjälpare

  /** true om observern ska reagera alls. */
  boolean enabled() {
    return enabled.getAsBoolean();
  }

  /** Metadatatyp-id:t som jämförs (konfigurerat eller injicerat), i gemener. */
  String metadataType() {
    String type = metadataType != null ? metadataType : AccessRulesConfig.metadataType();
    return type.toLowerCase(Locale.ROOT);
  }

  /**
   * Normaliserat typ-id för en metadatapost: "type" eller "type_version", i
   * gemener – samma form som {@link AccessRulesApplier} använder. null om typ
   * saknas.
   */
  static String metadataTypeId(DescriptiveMetadata dm) {
    if (dm == null) {
      return null;
    }
    String id = AccessRulesApplier.metadataTypeId(dm);
    return id == null ? null : id.toLowerCase(Locale.ROOT);
  }

  /** true om posten är AIP:ns egen eterna_admin (AIP-nivå, konfigurerad typ). */
  boolean isAdmin(DescriptiveMetadata dm) {
    return dm != null && dm.isFromAIP() && metadataType().equals(metadataTypeId(dm));
  }

  /**
   * true om ett borttaget metadata-id (filnamn, t.ex. "eterna_admin.xml") på
   * AIP-nivå avser den konfigurerade typen.
   */
  boolean isAdminId(String representationId, String descriptiveMetadataId) {
    return representationId == null && descriptiveMetadataId != null
      && descriptiveMetadataId.toLowerCase(Locale.ROOT).startsWith(metadataType());
  }

  /** Användaren vars ändring utlöste händelsen: AIP:ns updatedBy (stämplad av modellen), annars {@value #USER}. */
  static String triggeringUser(AIP aip) {
    String user = aip == null ? null : aip.getUpdatedBy();
    return user == null || user.isBlank() ? USER : user.trim();
  }

  /** Utfallet av en omvärdering. */
  static final class Evaluation {
    /** Fel att rapportera till modellen (tomt om allt gick bra). */
    final ReturnWithExceptions<Void, ModelObserver> ret;
    /** Applikatorns resultat, eller null om ingen utvärdering gjordes (avstängd, vakt, saknad AIP, fel). */
    final ApplyResult result;
    /** Utlösande användare, eller null om AIP:n inte hämtades. */
    final String user;

    Evaluation(ReturnWithExceptions<Void, ModelObserver> ret, ApplyResult result, String user) {
      this.ret = ret;
      this.result = result;
      this.user = user;
    }

    /** true när AIP:n har egen eterna_admin (faktakälla self) – efterkommande ärver då från den. */
    boolean hasOwnFacts() {
      return result != null && result.getFacts() != null && result.getFacts().isPresent()
        && !result.getFacts().isInherited();
    }
  }

  /**
   * Utvärderar reglerna för en AIP och skriver resultatet om behörigheterna
   * ändras. No-op när observern är avstängd eller när vakten är satt i
   * tråden. Kastar aldrig; fel returneras (se klassdokumentationen).
   */
  Evaluation reevaluate(String aipId, String reason) {
    ReturnWithExceptions<Void, ModelObserver> ret = none();
    if (aipId == null || !enabled() || Boolean.TRUE.equals(ACTIVE.get())) {
      return new Evaluation(ret, null, null);
    }
    ACTIVE.set(Boolean.TRUE);
    ApplyResult result = null;
    String user = null;
    try {
      AIP aip = model.retrieveAIP(aipId);
      user = triggeringUser(aip);
      AccessRulesApplier applier = applierFactory.apply(rules.get());
      applier.setStateOnlyWrites(false);
      result = applier.applyToAip(aip, user, false);
      if (result.isWritten()) {
        MergeResult merge = result.getMerge();
        LOGGER.info("AIP {}: behörigheter ändrade av åtkomstreglerna ({}, utlöst av {}): READ tillagd {}, READ "
          + "borttagen {}", aipId, reason, user, merge.getAdded(), merge.getRemoved());
        if (result.isPermissionsChanged()) {
          recordEvent(aipId, result, reason, user);
        }
      } else if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("AIP {}: åtkomstregler utvärderade ({}): {}", aipId, reason, result.summary());
      }
    } catch (NotFoundException e) {
      LOGGER.debug("AIP {}: finns inte längre – åtkomstreglerna utvärderas inte ({})", aipId, reason);
    } catch (RODAException | RuntimeException e) {
      LOGGER.error("AIP {}: åtkomstreglerna kunde inte tillämpas ({}): {}", aipId, reason, e.getMessage(), e);
      ret.add(new GenericException("Åtkomstreglerna kunde inte tillämpas på strukturenheten " + aipId + " (" + reason
        + "): " + e.getMessage() + ". Ändringen avvisas tills reglerna kan tillämpas.", e));
    } finally {
      ACTIVE.remove();
    }
    return new Evaluation(ret, result, user);
  }

  /**
   * PREMIS-händelse (UPDATE) med samma beskrivning och detaljformat som
   * pluginet, med den utlösande användaren som agent. Fel loggas bara.
   */
  private void recordEvent(String aipId, ApplyResult result, String reason, String user) {
    if (!createEvents.getAsBoolean()) {
      return;
    }
    try {
      String outcome = "Automatisk tillämpning av åtkomstreglerna efter " + reason + " (utlöst av " + user + ")\n"
        + ApplyResultTexts.describe(result, false);
      model.createUpdateAIPEvent(aipId, null, null, null, PreservationEventType.UPDATE,
        ApplyResultTexts.EVENT_DESCRIPTION, PluginState.SUCCESS, outcome, "", user, true, null);
    } catch (RuntimeException e) {
      LOGGER.error("AIP {}: kunde inte skapa bevarandehändelse för åtkomstreglerna: {}", aipId, e.getMessage(), e);
    }
  }

  /**
   * Startar (eller samlar upp) efterkommande-jobbet om AIP:n har
   * efterkommande utan egen sekretessmarkering. No-op inom
   * {@link #runGuarded} (vakten gäller hela observern, inte bara
   * omvärderingen). Fel loggas bara.
   */
  void launchDescendantsJob(String aipId, String user) {
    if (aipId == null || !enabled() || Boolean.TRUE.equals(ACTIVE.get())) {
      return;
    }
    try {
      if (!hasDescendantsWithoutOwnFacts.test(aipId)) {
        return;
      }
      Deferred deferred = DEFERRED.get();
      if (deferred != null) {
        LOGGER.debug("AIP {}: efterkommande-jobbet samlas upp till batchens slut", aipId);
        deferred.add(this, aipId, user);
        return;
      }
      launch(Collections.singleton(aipId), user);
    } catch (RuntimeException e) {
      LOGGER.error("AIP {}: kunde inte starta åtkomstregeljobb för efterkommande: {}", aipId, e.getMessage(), e);
    }
  }

  /**
   * Startar jobbet nu, eller – när modellen är transaktionell – först när
   * transaktionen committats, eftersom jobbet körs mot huvudlagringen och
   * annars skulle läsa de gamla faktan.
   */
  private void launch(Collection<String> aipIds, String user) {
    List<String> ids = new ArrayList<>(aipIds);
    if (model instanceof TransactionalModelService transactional) {
      LOGGER.info("AIP {}: åtkomstregeljobbet för efterkommande startas när transaktionen committats", ids);
      transactional.addAfterCommitAction(() -> launchNow(ids, user));
    } else {
      launchNow(ids, user);
    }
  }

  private void launchNow(Collection<String> aipIds, String user) {
    LOGGER.info("AIP {}: startar åtkomstregeljobb för efterkommande som ärver sekretessfakta (utlöst av {})", aipIds,
      user == null ? USER : user);
    descendantsJobLauncher.accept(aipIds, user);
  }

  /**
   * Frågar indexet om AIP:n har efterkommande (alla nivåer) som saknar
   * {@value #SEKRETESS_INDEX_FIELD}. Utan index eller vid fel: false
   * (loggas).
   */
  static boolean indexHasDescendantsWithoutOwnFacts(IndexService index, String aipId) {
    if (index == null) {
      return false;
    }
    Filter filter = new Filter(new SimpleFilterParameter(RodaConstants.AIP_ANCESTORS, aipId),
      new EmptyKeyFilterParameter(SEKRETESS_INDEX_FIELD));
    try {
      Long count = index.count(IndexedAIP.class, filter);
      return count != null && count > 0;
    } catch (RODAException | RuntimeException e) {
      LOGGER.error("AIP {}: kunde inte kontrollera efterkommande i indexet: {}", aipId, e.getMessage(), e);
      return false;
    }
  }

  // ---------------------------------------------------------------- händelser

  /**
   * Utlöses bara vid notify=true; under ingest (notify=false) händer inget här
   * – ingeststeget AccessRulesPlugin tillämpar reglerna.
   */
  @Override
  public ReturnWithExceptions<Void, ModelObserver> descriptiveMetadataCreated(DescriptiveMetadata dm) {
    return adminMetadataChanged(dm, "eterna_admin skapad");
  }

  /** Se {@link #descriptiveMetadataCreated}. */
  @Override
  public ReturnWithExceptions<Void, ModelObserver> descriptiveMetadataUpdated(DescriptiveMetadata dm) {
    return adminMetadataChanged(dm, "eterna_admin uppdaterad");
  }

  @Override
  public ReturnWithExceptions<Void, ModelObserver> descriptiveMetadataDeleted(String aipId, String representationId,
    String descriptiveMetadataId) {
    if (!enabled() || !isAdminId(representationId, descriptiveMetadataId)) {
      return none();
    }
    Evaluation evaluation = reevaluate(aipId, "eterna_admin borttagen");
    launchDescendantsJob(aipId, evaluation.user);
    return evaluation.ret;
  }

  /**
   * Manuell behörighetsändring: nekade grupper tas bort igen. Appliern
   * notifierar samma händelse när den skriver; vakten gör det anropet till en
   * no-op.
   */
  @Override
  public ReturnWithExceptions<Void, ModelObserver> aipPermissionsUpdated(AIP aip) {
    if (aip == null || !enabled()) {
      return none();
    }
    return reevaluate(aip.getId(), "behörighetsändring").ret;
  }

  /**
   * Flytt: AIP:n utvärderas om (ärvda fakta kan ha ändrats). Efterkommande
   * utvärderas bara om AIP:n saknar egen eterna_admin – annars ärver de
   * oförändrat från den.
   */
  @Override
  public ReturnWithExceptions<Void, ModelObserver> aipMoved(AIP aip, String oldParentId, String newParentId) {
    if (aip == null || !enabled()) {
      return none();
    }
    Evaluation evaluation = reevaluate(aip.getId(), "flytt");
    if (evaluation.hasOwnFacts()) {
      LOGGER.debug("AIP {}: egen eterna_admin – flytten ändrar inte efterkommandes fakta, inget jobb", aip.getId());
    } else {
      launchDescendantsJob(aip.getId(), evaluation.user != null ? evaluation.user : triggeringUser(aip));
    }
    return evaluation.ret;
  }

  private ReturnWithExceptions<Void, ModelObserver> adminMetadataChanged(DescriptiveMetadata dm, String reason) {
    if (!enabled() || !isAdmin(dm)) {
      return none();
    }
    Evaluation evaluation = reevaluate(dm.getAipId(), reason);
    launchDescendantsJob(dm.getAipId(), evaluation.user);
    return evaluation.ret;
  }
}

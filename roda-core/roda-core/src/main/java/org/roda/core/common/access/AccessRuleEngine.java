/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.common.access;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.roda.core.data.v2.accessrules.AccessRule;
import org.roda.core.data.v2.accessrules.AccessRuleDefaults;
import org.roda.core.data.v2.accessrules.AccessRuleSet;

/**
 * Regelmotorn. Avgör, utan sidoeffekter, vilka av
 * regelfilens grupper som får läsa ett AIP givet objektets eterna_admin-fakta
 * (sekretess, lagrum, personuppgifter, personuppgiftsnivå).
 *
 * <p>
 * Semantik (se även {@link AccessRuleDefaults}):
 * </p>
 * <ul>
 * <li>{@code managedGroups} = alla grupper i regelfilen, även avstängda
 * regler.</li>
 * <li>Regelfilen avstängd ({@code enabled=false}) ⇒ alla hanterade grupper
 * tillåts (inga begränsningar; mergern återställer READ och borttagna
 * skrivbehörigheter för restricted grupper). Applikatorn kör motorn även
 * då, så att en avstängd regelfil lyfter tidigare begränsningar.</li>
 * <li>Fakta saknas ({@code !present}) – styrs av {@code missingMetadata}:
 * "open" ⇒ tillåten, "closed" ⇒ nekad, "inherit" (standard) ⇒ applikatorn
 * har redan försökt ärva från förfäder utan att hitta någon eterna_admin;
 * ett sådant objekt är obedömd struktur (t.ex. arkivbildare, klasser och
 * diarieposter utan bedömning) och lämnas <em>öppet</em> – bara objekt med
 * egen eller ärvd bedömning styrs av reglerna. "inherit-closed" ⇒ som
 * inherit, men ett objekt utan egen eller ärvd bedömning behandlas som
 * <em>ej bedömt</em> (sekretess och personuppgifter = "ej_bedomd") och
 * prövas mot regeln, dvs. stängt om inte regeln listar "ej_bedomd" (eller
 * har "*") eller {@code ejBedomd=open}. Okänt policyvärde ⇒ nekad. En avstängd regel
 * tillåter aldrig något, inte heller här.</li>
 * <li>Annars per regel — alla villkor måste vara uppfyllda:
 * <ol>
 * <li>sekretess ∈ regelns sekretess-lista ("*" = alla). Saknad/tom sekretess
 * = "ej_bedomd"; med {@code ejBedomd=closed} måste "ej_bedomd" listas
 * uttryckligen, med "open" tillåts ej bedömda alltid.</li>
 * <li>om sekretess är något annat än "ingen_sekretess" eller "ej_bedomd"
 * (dvs. "sekretess_forekommer", "helt_sekretessbelagd" och alla okända
 * värden): objektets lagrum måste täckas av regelns lagrumsmönster. Varje
 * otolkbar del av sekretessgrunden (varning i fakta) räknas som ett otäckt
 * lagrum. Inget lagrum alls (och inga varningar) ⇒ tillåten bara med "*"
 * eller {@code missingLagrum=open}. {@code multipleLagrum} "all" ⇒ varje
 * lagrum täcks, "any" ⇒ minst ett täcks. Ogiltiga mönster i regeln ignoreras
 * (täcker inget) och nämns i motiveringen.</li>
 * <li>personuppgifter ∈ regelns lista, samma ej_bedomd-regel som för
 * sekretess.</li>
 * <li>om personuppgiftsnivå finns: ∈ regelns lista ("*" = alla; tom lista =
 * inga nivåer tillåtna).</li>
 * </ol>
 * </li>
 * <li>Avstängd regel ⇒ gruppen hanterad men aldrig tillåten.</li>
 * <li>Flera regler för samma grupp: den första gäller (som
 * {@link AccessRuleSet#findRule}).</li>
 * </ul>
 *
 * <p>
 * Alla vokabulärvärden (i fakta och regel) jämförs trimmade och
 * skiftlägesokänsligt. Otolkbart/okänt stänger alltid hellre än öppnar.
 * </p>
 */
public final class AccessRuleEngine {

  /** Jokertecken i regelns listor: tillåter alla värden. */
  public static final String WILDCARD = "*";

  /** Vokabulärvärde (och effektivt värde för saknad bedömning). */
  public static final String EJ_BEDOMD = "ej_bedomd";

  /** Sekretessvärde för objekt utan sekretess. */
  public static final String INGEN_SEKRETESS = "ingen_sekretess";

  /** Sekretessvärden enligt config/schemas/eterna_admin.xsd. */
  public static final Set<String> SEKRETESS_VALUES = Set.of(EJ_BEDOMD, INGEN_SEKRETESS, "sekretess_forekommer",
    "helt_sekretessbelagd");

  /** Personuppgiftsvärden enligt config/schemas/eterna_admin.xsd. */
  public static final Set<String> PERSONUPPGIFTER_VALUES = Set.of(EJ_BEDOMD, "inga", "forekommer",
    "kansliga_forekommer");

  /**
   * Sekretessvärden som INTE aktiverar lagrumskontrollen. Alla andra värden,
   * även okända, kräver att lagrum täcks (okänt stänger).
   */
  public static final Set<String> SEKRETESS_WITHOUT_LAGRUM = Set.of(INGEN_SEKRETESS, EJ_BEDOMD);

  private static final String OPEN = "open";
  private static final String CLOSED = "closed";
  private static final String INHERIT = "inherit";
  private static final String INHERIT_CLOSED = "inherit-closed";
  private static final String ANY = "any";

  private AccessRuleEngine() {
    // statisk verktygsklass
  }

  /**
   * Utvärderar reglerna mot faktan.
   *
   * @param rules
   *          regeluppsättningen; null behandlas som tom och avstängd
   * @param facts
   *          objektets fakta; null behandlas som {@link EternaAdminFacts#absent()}
   * @return beslut med tillåtna/hanterade grupper och motivering
   */
  public static AccessDecision evaluate(AccessRuleSet rules, EternaAdminFacts facts) {
    AccessDecision decision = new AccessDecision();
    if (rules == null) {
      return decision;
    }
    EternaAdminFacts effectiveFacts = facts == null ? EternaAdminFacts.absent() : facts;
    AccessRuleDefaults defaults = rules.getDefaults() == null ? new AccessRuleDefaults() : rules.getDefaults();
    Set<String> managed = rules.getManagedGroups();
    decision.setManagedGroups(managed);

    for (String group : managed) {
      AccessRule rule = rules.findRule(group);
      if (!rules.isEnabled()) {
        appendAllowed(decision, group, "reglerna är avstängda");
        continue;
      }
      if (!rule.isEnabled()) {
        appendDenied(decision, group, "regeln är avstängd");
        continue;
      }

      List<String> notes = new ArrayList<>();
      EternaAdminFacts ruleFacts = effectiveFacts;
      if (!effectiveFacts.isPresent()) {
        String policy = missingMetadata(defaults);
        if (OPEN.equals(policy)) {
          appendAllowed(decision, group, "eterna_admin saknas (missingMetadata=open)");
          continue;
        }
        if (CLOSED.equals(policy)) {
          appendDenied(decision, group, "eterna_admin saknas (missingMetadata=closed)");
          continue;
        }
        if (INHERIT.equals(policy)) {
          // inherit: applikatorn har redan sökt förfäder utan träff ⇒ obedömd struktur lämnas öppen
          appendAllowed(decision, group,
            "eterna_admin saknas hos objektet och alla förfäder (missingMetadata=inherit) – obedömd struktur lämnas öppen");
          continue;
        }
        if (!INHERIT_CLOSED.equals(policy)) {
          appendDenied(decision, group, "eterna_admin saknas (okänt policyvärde missingMetadata=" + policy + ")");
          continue;
        }
        // inherit-closed: applikatorn har redan sökt förfäder utan träff ⇒ obedömt objekt
        ruleFacts = unassessed();
        notes.add("eterna_admin saknas hos objektet och alla förfäder (missingMetadata=inherit-closed) – behandlas som ej bedömd");
      }

      String reason = evaluateRule(rule, defaults, ruleFacts, notes);
      String suffix = notes.isEmpty() ? "" : " (" + String.join("; ", notes) + ")";
      if (reason == null) {
        decision.getAllowedGroups().add(group);
        decision.getExplanation().add(group + ": tillåten" + suffix);
      } else {
        decision.getExplanation().add(group + ": nekad – " + reason + suffix);
      }
    }
    return decision;
  }

  /** null = tillåten, annars skäl till nekande. Noteringar läggs i notes. */
  private static String evaluateRule(AccessRule rule, AccessRuleDefaults defaults, EternaAdminFacts facts,
    List<String> notes) {
    boolean ejBedomdOpen = OPEN.equals(lower(defaults.getEjBedomd()));

    // 1. sekretess
    String sekretess = normalizeValue(facts.getSekretess());
    String reason = checkVocabulary("sekretess", sekretess, rule.getSekretess(), ejBedomdOpen);
    if (reason != null) {
      return reason;
    }

    // 2. lagrum (för alla sekretessvärden utom ingen_sekretess/ej_bedomd)
    if (!SEKRETESS_WITHOUT_LAGRUM.contains(sekretess)) {
      reason = checkLagrum(rule, defaults, facts, notes);
      if (reason != null) {
        return reason;
      }
    }

    // 3. personuppgifter
    String personuppgifter = normalizeValue(facts.getPersonuppgifter());
    reason = checkVocabulary("personuppgifter", personuppgifter, rule.getPersonuppgifter(), ejBedomdOpen);
    if (reason != null) {
      return reason;
    }

    // 4. personuppgiftsnivå (bara om objektet har en nivå)
    String niva = lower(facts.getPersonuppgiftsniva());
    if (niva != null && !listContains(rule.getPersonuppgiftsniva(), WILDCARD)
      && !listContains(rule.getPersonuppgiftsniva(), niva)) {
      return "personuppgiftsnivå " + niva + " ingår inte i regeln";
    }
    return null;
  }

  /**
   * Kontroll av ett vokabulärvärde (sekretess/personuppgifter) mot regelns
   * lista. null = ok.
   */
  private static String checkVocabulary(String label, String value, List<String> allowed, boolean ejBedomdOpen) {
    if (listContains(allowed, WILDCARD) || listContains(allowed, value)) {
      return null;
    }
    if (EJ_BEDOMD.equals(value)) {
      if (ejBedomdOpen) {
        return null;
      }
      return label + " ej bedömd (" + EJ_BEDOMD + " ingår inte i regeln)";
    }
    return label + " " + value + " ingår inte i regeln";
  }

  /** Lagrumskontroll för sekretessbelagt objekt. null = ok. */
  private static String checkLagrum(AccessRule rule, AccessRuleDefaults defaults, EternaAdminFacts facts,
    List<String> notes) {
    List<LagrumPattern> patterns = new ArrayList<>();
    boolean wildcard = false;
    List<String> invalid = new ArrayList<>();
    for (String text : rule.getLagrum()) {
      try {
        LagrumPattern pattern = LagrumPattern.parse(text, defaults.getDefaultAct());
        if (pattern.isWildcard()) {
          wildcard = true;
        }
        patterns.add(pattern);
      } catch (IllegalArgumentException e) {
        invalid.add(String.valueOf(text));
      }
    }
    if (!invalid.isEmpty()) {
      notes.add("ogiltigt lagrumsmönster ignorerat: " + String.join(", ", invalid));
    }
    if (wildcard) {
      return null;
    }

    List<Lagrum> lagrum = facts.getLagrum();
    List<String> warnings = facts.getWarnings();
    if (lagrum.isEmpty() && warnings.isEmpty()) {
      if (OPEN.equals(lower(defaults.getMissingLagrum()))) {
        return null;
      }
      return "lagrum saknas (missingLagrum=closed)";
    }

    boolean any = ANY.equals(lower(defaults.getMultipleLagrum()));
    List<String> uncovered = new ArrayList<>();
    int covered = 0;
    for (Lagrum l : lagrum) {
      if (isCovered(l, patterns)) {
        covered++;
      } else {
        uncovered.add(l.toString());
      }
    }
    // otolkbara delar av sekretessgrunden räknas som otäckta lagrum
    for (String warning : warnings) {
      uncovered.add("otolkbart: " + warning);
    }

    if (any) {
      if (covered > 0) {
        return null;
      }
      return "inget lagrum täcks av regeln (" + String.join(", ", uncovered) + ")";
    }
    if (uncovered.isEmpty()) {
      return null;
    }
    return "lagrum täcks inte av regeln (" + String.join(", ", uncovered) + ")";
  }

  private static boolean isCovered(Lagrum lagrum, List<LagrumPattern> patterns) {
    for (LagrumPattern pattern : patterns) {
      if (pattern.covers(lagrum)) {
        return true;
      }
    }
    return false;
  }

  private static void appendAllowed(AccessDecision decision, String group, String why) {
    decision.getAllowedGroups().add(group);
    decision.getExplanation().add(group + ": tillåten – " + why);
  }

  private static void appendDenied(AccessDecision decision, String group, String why) {
    decision.getExplanation().add(group + ": nekad – " + why);
  }

  /** Fakta för ett obedömt objekt (present, alla värden saknas). */
  private static EternaAdminFacts unassessed() {
    EternaAdminFacts facts = new EternaAdminFacts();
    facts.setPresent(true);
    return facts;
  }

  private static String missingMetadata(AccessRuleDefaults defaults) {
    String value = lower(defaults.getMissingMetadata());
    return value == null ? INHERIT : value;
  }

  /** true om listan innehåller värdet (trimmat, skiftlägesokänsligt). */
  static boolean listContains(List<String> values, String wanted) {
    if (values == null || wanted == null) {
      return false;
    }
    for (String value : values) {
      if (wanted.equals(lower(value))) {
        return true;
      }
    }
    return false;
  }

  /** Saknat/tomt värde ⇒ "ej_bedomd"; annars trimmat värde i gemener. */
  private static String normalizeValue(String value) {
    String lowered = lower(value);
    return lowered == null ? EJ_BEDOMD : lowered;
  }

  private static String blankToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private static String lower(String value) {
    String trimmed = blankToNull(value);
    return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
  }
}

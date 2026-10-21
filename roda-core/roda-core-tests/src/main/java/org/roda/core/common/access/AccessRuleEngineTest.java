/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.common.access;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.v2.accessrules.AccessRule;
import org.roda.core.data.v2.accessrules.AccessRuleDefaults;
import org.roda.core.data.v2.accessrules.AccessRuleSet;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tester för regelmotorn — matris över ej_bedomd
 * (stängd/öppen), saknad metadata (open/closed/inherit/okänt), saknat lagrum,
 * flera lagrum (all/any), joker "*", okända sekretessvärden (kräver lagrum),
 * skiftlägesokänslighet, personuppgiftsnivåer, avstängd regel, avstängd
 * regelfil, varningar i fakta (otolkbar sekretessgrund) samt motiveringstext.
 * Vokabulären är den ur eterna_admin.xsd. Inga beroenden på Docker, Solr,
 * LDAP eller RodaCoreFactory.
 */
@Test(groups = {RodaConstants.TEST_GROUP_ALL, RodaConstants.TEST_GROUP_DEV, RodaConstants.TEST_GROUP_TRAVIS,
  "accessrules"})
public class AccessRuleEngineTest {

  private static final String SOC = "socialtjanst";
  private static final String HANDL = "handlaggare";
  private static final String EJ_SEKRETESS = "ingen_sekretess";
  private static final String SEKRETESS = "sekretess_forekommer";
  private static final String HELT = "helt_sekretessbelagd";
  private static final String EJ_PU = "inga";
  private static final String PU = "forekommer";
  private static final String KANSLIGA = "kansliga_forekommer";

  private static final Lagrum OSL_21_7 = new Lagrum("OSL", 21, 7);
  private static final Lagrum OSL_26_1 = new Lagrum("OSL", 26, 1);

  // ---------------------------------------------------------------- hjälpare

  private static AccessRule rule(String group, List<String> sekretess, List<String> lagrum,
    List<String> personuppgifter, List<String> niva) {
    AccessRule rule = new AccessRule(group);
    rule.setSekretess(sekretess);
    rule.setLagrum(lagrum);
    rule.setPersonuppgifter(personuppgifter);
    rule.setPersonuppgiftsniva(niva);
    return rule;
  }

  /** Regel som tillåter ingen_sekretess/sekretess_forekommer, OSL 21, inga/forekommer. */
  private static AccessRule socRule() {
    return rule(SOC, Arrays.asList(EJ_SEKRETESS, SEKRETESS), Arrays.asList("OSL 21"), Arrays.asList(EJ_PU, PU),
      Arrays.asList("niva_1", "niva_2"));
  }

  /** Regel som bara tillåter ingen_sekretess och inga personuppgifter. */
  private static AccessRule handlRule() {
    return rule(HANDL, Arrays.asList(EJ_SEKRETESS), Arrays.asList(), Arrays.asList(EJ_PU), Arrays.asList());
  }

  private static AccessRuleSet rules(AccessRule... rules) {
    AccessRuleSet set = new AccessRuleSet();
    set.setVersion(3);
    set.setEnabled(true);
    set.setDefaults(new AccessRuleDefaults());
    set.setRules(Arrays.asList(rules));
    return set;
  }

  private static EternaAdminFacts facts(String sekretess, List<Lagrum> lagrum, String personuppgifter, String niva) {
    EternaAdminFacts facts = new EternaAdminFacts();
    facts.setPresent(true);
    facts.setSekretess(sekretess);
    facts.setLagrum(lagrum);
    facts.setPersonuppgifter(personuppgifter);
    facts.setPersonuppgiftsniva(niva);
    return facts;
  }

  private static EternaAdminFacts openFacts() {
    return facts(EJ_SEKRETESS, List.of(), EJ_PU, null);
  }

  private static void assertAllowed(AccessDecision decision, String group) {
    Assert.assertTrue(decision.isAllowed(group), group + " borde vara tillåten: " + decision.getExplanation());
  }

  private static void assertDenied(AccessDecision decision, String group) {
    Assert.assertTrue(decision.isManaged(group), group + " borde vara hanterad");
    Assert.assertFalse(decision.isAllowed(group), group + " borde vara nekad: " + decision.getExplanation());
  }

  // ---------------------------------------------------------------- grundfall

  @Test
  public void testManagedGroupsIncludeDisabledRulesAndOnePerGroup() {
    AccessRule disabled = handlRule();
    disabled.setEnabled(false);
    AccessRuleSet set = rules(socRule(), disabled, socRule());

    AccessDecision decision = AccessRuleEngine.evaluate(set, openFacts());

    Assert.assertEquals(decision.getManagedGroups(), Set.of(SOC, HANDL));
    Assert.assertEquals(decision.getExplanation().size(), 2, "en rad per hanterad grupp");
    Assert.assertTrue(decision.getExplanation().get(0).startsWith(SOC + ":"));
    Assert.assertTrue(decision.getExplanation().get(1).startsWith(HANDL + ":"));
  }

  @Test
  public void testExplanationText() {
    AccessDecision decision = AccessRuleEngine.evaluate(rules(socRule(), handlRule()),
      facts(SEKRETESS, List.of(OSL_21_7), EJ_PU, null));

    Assert.assertEquals(decision.getExplanation(),
      List.of(SOC + ": tillåten", HANDL + ": nekad – sekretess sekretess_forekommer ingår inte i regeln"));
    Assert.assertEquals(decision.getAllowedGroups(), Set.of(SOC));
  }

  @Test
  public void testRulesDisabledAllowsAllManagedGroupsEvenDisabledRule() {
    AccessRule disabled = handlRule();
    disabled.setEnabled(false);
    AccessRuleSet set = rules(socRule(), disabled);
    set.setEnabled(false);

    AccessDecision decision = AccessRuleEngine.evaluate(set, facts(HELT, List.of(), PU, "niva_3"));

    Assert.assertEquals(decision.getManagedGroups(), Set.of(SOC, HANDL));
    Assert.assertEquals(decision.getAllowedGroups(), Set.of(SOC, HANDL));
    Assert.assertTrue(decision.getExplanation().get(0).contains("avstängda"), decision.getExplanation().toString());
  }

  @Test
  public void testDisabledRuleIsManagedButNeverAllowed() {
    AccessRule disabled = socRule();
    disabled.setEnabled(false);

    AccessDecision decision = AccessRuleEngine.evaluate(rules(disabled), openFacts());
    assertDenied(decision, SOC);
    Assert.assertTrue(decision.getExplanation().get(0).contains("regeln är avstängd"));

    // inte heller när metadata saknas och policyn är öppen
    AccessRuleSet set = rules(disabled);
    set.getDefaults().setMissingMetadata("open");
    assertDenied(AccessRuleEngine.evaluate(set, EternaAdminFacts.absent()), SOC);
  }

  @Test
  public void testNullRulesAndNullFacts() {
    AccessDecision empty = AccessRuleEngine.evaluate(null, openFacts());
    Assert.assertTrue(empty.getManagedGroups().isEmpty());
    Assert.assertTrue(empty.getAllowedGroups().isEmpty());
    Assert.assertTrue(empty.getExplanation().isEmpty());

    // null-fakta = saknad metadata; standard (inherit) ⇒ obedömd struktur lämnas öppen
    AccessDecision decision = AccessRuleEngine.evaluate(rules(socRule()), null);
    assertAllowed(decision, SOC);
    Assert.assertTrue(decision.getExplanation().get(0).contains("lämnas öppen"), decision.getExplanation().toString());
  }

  @Test
  public void testNoRulesGivesEmptyDecision() {
    AccessDecision decision = AccessRuleEngine.evaluate(rules(), facts(HELT, List.of(), PU, null));
    Assert.assertTrue(decision.getManagedGroups().isEmpty());
    Assert.assertTrue(decision.getAllowedGroups().isEmpty());
  }

  // ---------------------------------------------------------------- saknad metadata

  /** policy, tillåten? — med regler som inte listar ej_bedomd (socRule/handlRule). */
  @DataProvider(name = "missingMetadata")
  public Object[][] missingMetadata() {
    return new Object[][] {{"open", true}, {"closed", false}, {"inherit", true}, {"inherit-closed", false}, {null, true},
      {"OPEN", true},
      {" open ", true}, {"okant", false}, {"clsoed", false}};
  }

  @Test(dataProvider = "missingMetadata")
  public void testMissingMetadata(String policy, boolean expectedAllowed) {
    AccessRuleSet set = rules(socRule(), handlRule());
    set.getDefaults().setMissingMetadata(policy);

    AccessDecision decision = AccessRuleEngine.evaluate(set, EternaAdminFacts.absent());

    Assert.assertEquals(decision.getManagedGroups(), Set.of(SOC, HANDL));
    Assert.assertEquals(decision.isAllowed(SOC), expectedAllowed, decision.getExplanation().toString());
    Assert.assertEquals(decision.isAllowed(HANDL), expectedAllowed, decision.getExplanation().toString());
    Assert.assertTrue(decision.getExplanation().get(0).contains("eterna_admin saknas"),
      decision.getExplanation().toString());
  }

  @Test
  public void testUnknownMissingMetadataPolicyIsDeniedWithReason() {
    AccessRuleSet set = rules(rule(SOC, List.of("*"), List.of("*"), List.of("*"), List.of("*")));
    set.getDefaults().setMissingMetadata("clsoed");

    AccessDecision decision = AccessRuleEngine.evaluate(set, EternaAdminFacts.absent());

    assertDenied(decision, SOC);
    Assert.assertTrue(decision.getExplanation().get(0).contains("okänt policyvärde"),
      decision.getExplanation().toString());
  }

  @Test
  public void testInheritWithoutAncestorIsTreatedAsUnassessed() {
    // inherit (standard): frånvarande fakta efter arv ⇒ obedömd struktur lämnas öppen
    assertAllowed(AccessRuleEngine.evaluate(rules(socRule()), EternaAdminFacts.absent()), SOC);
    // inherit-closed: applikatorn har redan sökt förfäder; frånvarande fakta ⇒ objektet
    // är obedömt och prövas som ej_bedomd mot regeln
    AccessRuleSet strict = rules(socRule());
    strict.getDefaults().setMissingMetadata("inherit-closed");
    AccessDecision denied = AccessRuleEngine.evaluate(strict, EternaAdminFacts.absent());
    assertDenied(denied, SOC);
    Assert.assertTrue(denied.getExplanation().get(0).contains("behandlas som ej bedömd"),
      denied.getExplanation().toString());

    // regel som uttryckligen tillåter ej_bedomd för både sekretess och personuppgifter
    AccessRuleSet listsEjBedomd = rules(rule(SOC, List.of(EJ_SEKRETESS, "ej_bedomd"), List.of(),
      List.of(EJ_PU, "ej_bedomd"), List.of()));
    listsEjBedomd.getDefaults().setMissingMetadata("inherit-closed");
    assertAllowed(AccessRuleEngine.evaluate(listsEjBedomd, EternaAdminFacts.absent()), SOC);

    // bara sekretess listar ej_bedomd ⇒ personuppgifter stänger
    AccessRuleSet half = rules(rule(SOC, List.of(EJ_SEKRETESS, "ej_bedomd"), List.of(), List.of(EJ_PU), List.of()));
    half.getDefaults().setMissingMetadata("inherit-closed");
    assertDenied(AccessRuleEngine.evaluate(half, EternaAdminFacts.absent()), SOC);

    // joker tillåter
    AccessRuleSet wildcard = rules(rule(SOC, List.of("*"), List.of(), List.of("*"), List.of()));
    assertAllowed(AccessRuleEngine.evaluate(wildcard, EternaAdminFacts.absent()), SOC);

    // ejBedomd=open tillåter obedömda objekt
    AccessRuleSet open = rules(socRule());
    open.getDefaults().setEjBedomd("open");
    assertAllowed(AccessRuleEngine.evaluate(open, EternaAdminFacts.absent()), SOC);
  }

  // ---------------------------------------------------------------- ej_bedomd

  /** ejBedomd-policy, sekretess i fakta, regelns sekretesslista, tillåten? */
  @DataProvider(name = "ejBedomdSekretess")
  public Object[][] ejBedomdSekretess() {
    return new Object[][] {
      // stängd policy: ej_bedomd måste listas uttryckligen
      {"closed", null, List.of(EJ_SEKRETESS), false}, // ----
      {"closed", "", List.of(EJ_SEKRETESS), false}, // ----
      {"closed", "ej_bedomd", List.of(EJ_SEKRETESS), false}, // ----
      {"closed", null, List.of(EJ_SEKRETESS, "ej_bedomd"), true}, // ----
      {"closed", "ej_bedomd", List.of("ej_bedomd"), true}, // ----
      {"closed", null, List.of("*"), true}, // joker tillåter allt
      // öppen policy: ej bedömt tillåts alltid
      {"open", null, List.of(EJ_SEKRETESS), true}, // ----
      {"open", "ej_bedomd", List.of(EJ_SEKRETESS), true}, // ----
      {"open", null, List.of(), true}, // ----
      // öppen policy påverkar inte bedömda värden
      {"open", SEKRETESS, List.of(EJ_SEKRETESS), false}, // ----
      {"closed", EJ_SEKRETESS, List.of(EJ_SEKRETESS), true}, // ----
      {"closed", SEKRETESS, List.of(EJ_SEKRETESS), false}, // ----
      {"closed", "okant_varde", List.of(EJ_SEKRETESS, "ej_bedomd"), false}, // okänt värde stänger
      {"closed", "okant_varde", List.of("*"), true}, // (regeln har lagrum "*")
    };
  }

  @Test(dataProvider = "ejBedomdSekretess")
  public void testEjBedomdSekretess(String policy, String sekretess, List<String> ruleSekretess,
    boolean expectedAllowed) {
    AccessRuleSet set = rules(rule(SOC, ruleSekretess, List.of("*"), List.of("*"), List.of("*")));
    set.getDefaults().setEjBedomd(policy);

    AccessDecision decision = AccessRuleEngine.evaluate(set, facts(sekretess, List.of(), EJ_PU, null));

    Assert.assertEquals(decision.isAllowed(SOC), expectedAllowed, decision.getExplanation().toString());
    if (!expectedAllowed && sekretess == null) {
      Assert.assertTrue(decision.getExplanation().get(0).contains("sekretess ej bedömd"),
        decision.getExplanation().toString());
    }
  }

  /** ejBedomd-policy, personuppgifter i fakta, regelns lista, tillåten? */
  @DataProvider(name = "ejBedomdPersonuppgifter")
  public Object[][] ejBedomdPersonuppgifter() {
    return new Object[][] {
      {"closed", null, List.of(EJ_PU), false}, // ----
      {"closed", null, List.of(EJ_PU, "ej_bedomd"), true}, // ----
      {"closed", "ej_bedomd", List.of(EJ_PU), false}, // ----
      {"closed", null, List.of("*"), true}, // ----
      {"open", null, List.of(EJ_PU), true}, // ----
      {"open", PU, List.of(EJ_PU), false}, // ----
      {"closed", PU, List.of(EJ_PU, PU), true}, // ----
      {"closed", PU, List.of(EJ_PU), false}, // ----
      {"closed", KANSLIGA, List.of(EJ_PU, PU), false}, // ----
      {"closed", KANSLIGA, List.of(KANSLIGA), true}, // ----
    };
  }

  @Test(dataProvider = "ejBedomdPersonuppgifter")
  public void testEjBedomdPersonuppgifter(String policy, String personuppgifter, List<String> rulePu,
    boolean expectedAllowed) {
    AccessRuleSet set = rules(rule(SOC, List.of("*"), List.of("*"), rulePu, List.of("*")));
    set.getDefaults().setEjBedomd(policy);

    AccessDecision decision = AccessRuleEngine.evaluate(set, facts(EJ_SEKRETESS, List.of(), personuppgifter, null));

    Assert.assertEquals(decision.isAllowed(SOC), expectedAllowed, decision.getExplanation().toString());
    if (!expectedAllowed) {
      Assert.assertTrue(decision.getExplanation().get(0).contains("personuppgifter"),
        decision.getExplanation().toString());
    }
  }

  // ---------------------------------------------------------------- lagrum

  /** sekretess, fakta-lagrum, regelns lagrumsmönster, tillåten? (defaults) */
  @DataProvider(name = "lagrum")
  public Object[][] lagrum() {
    return new Object[][] {
      // kapitelmönster täcker paragraf, paragrafmönster bara exakt
      {SEKRETESS, List.of(OSL_21_7), List.of("OSL 21"), true}, // ----
      {SEKRETESS, List.of(OSL_21_7), List.of("OSL 21:7"), true}, // ----
      {SEKRETESS, List.of(OSL_21_7), List.of("OSL 21 kap. 7 §"), true}, // ----
      {SEKRETESS, List.of(OSL_21_7), List.of("OSL 21:8"), false}, // ----
      {SEKRETESS, List.of(OSL_21_7), List.of("OSL 26:1"), false}, // ----
      {SEKRETESS, List.of(OSL_21_7), List.of("OSL 26:1", "OSL 21:7"), true}, // ----
      {SEKRETESS, List.of(OSL_21_7), List.of("*"), true}, // ----
      {SEKRETESS, List.of(OSL_21_7), List.of(), false}, // inga mönster täcker inget
      {HELT, List.of(OSL_21_7), List.of("OSL 21"), true}, // helt sekretessbelagd prövas också
      {HELT, List.of(OSL_21_7), List.of("OSL 26"), false}, // ----
      // ingen sekretess: lagrumskravet prövas inte alls
      {EJ_SEKRETESS, List.of(OSL_21_7), List.of(), true}, // ----
      {EJ_SEKRETESS, List.of(), List.of(), true}, // ----
      // saknat lagrum med standardpolicy (closed): bara "*"
      {SEKRETESS, List.of(), List.of("OSL 21"), false}, // ----
      {SEKRETESS, List.of(), List.of("*"), true}, // ----
      {HELT, List.of(), List.of("OSL 21", "*"), true}, // ----
      // suffix måste matcha
      {SEKRETESS, List.of(new Lagrum("OSL", 21, 7, "a")), List.of("OSL 21:7"), false}, // ----
      {SEKRETESS, List.of(new Lagrum("OSL", 21, 7, "a")), List.of("OSL 21:7a"), true}, // ----
      {SEKRETESS, List.of(new Lagrum("OSL", 21, 7, "a")), List.of("OSL 21"), true}, // ----
      // annan lag
      {SEKRETESS, List.of(new Lagrum("TF", 2, 1)), List.of("OSL 2"), false}, // ----
      {SEKRETESS, List.of(new Lagrum("TF", 2, 1)), List.of("TF 2"), true}, // ----
      // okända/skiftlägesvarianter av sekretess kräver också lagrum
      {"HELT_SEKRETESSBELAGD", List.of(), List.of("OSL 21"), false}, // ----
      {"Sekretess_forekommer", List.of(OSL_21_7), List.of("OSL 21"), true}, // ----
      {"hemlig", List.of(), List.of("OSL 21"), false}, // ----
      {"hemlig", List.of(OSL_21_7), List.of("OSL 21"), true}, // ----
      {"hemlig", List.of(OSL_26_1), List.of("OSL 21"), false}, // ----
      {"INGEN_SEKRETESS", List.of(), List.of(), true}, // ingen sekretess oavsett skiftläge
    };
  }

  @Test(dataProvider = "lagrum")
  public void testLagrum(String sekretess, List<Lagrum> factsLagrum, List<String> patterns, boolean expectedAllowed) {
    AccessRuleSet set = rules(rule(SOC, List.of("*"), patterns, List.of("*"), List.of("*")));

    AccessDecision decision = AccessRuleEngine.evaluate(set, facts(sekretess, factsLagrum, EJ_PU, null));

    Assert.assertEquals(decision.isAllowed(SOC), expectedAllowed, decision.getExplanation().toString());
  }

  @Test
  public void testMissingLagrumPolicy() {
    AccessRuleSet set = rules(rule(SOC, List.of("*"), List.of("OSL 21"), List.of("*"), List.of("*")));
    EternaAdminFacts noLagrum = facts(SEKRETESS, List.of(), EJ_PU, null);

    set.getDefaults().setMissingLagrum("closed");
    AccessDecision closed = AccessRuleEngine.evaluate(set, noLagrum);
    assertDenied(closed, SOC);
    Assert.assertTrue(closed.getExplanation().get(0).contains("lagrum saknas"), closed.getExplanation().toString());

    set.getDefaults().setMissingLagrum("open");
    assertAllowed(AccessRuleEngine.evaluate(set, noLagrum), SOC);

    // "open" hoppar bara över när lagrum verkligen saknas – inte när det finns
    // men inte täcks
    assertDenied(AccessRuleEngine.evaluate(set, facts(SEKRETESS, List.of(OSL_26_1), EJ_PU, null)), SOC);
  }

  @Test
  public void testMultipleLagrumAllVersusAny() {
    AccessRuleSet set = rules(rule(SOC, List.of("*"), List.of("OSL 21"), List.of("*"), List.of("*")));
    EternaAdminFacts two = facts(SEKRETESS, List.of(OSL_21_7, OSL_26_1), EJ_PU, null);

    set.getDefaults().setMultipleLagrum("all");
    AccessDecision all = AccessRuleEngine.evaluate(set, two);
    assertDenied(all, SOC);
    Assert.assertTrue(all.getExplanation().get(0).contains("OSL 26:1"), all.getExplanation().toString());

    set.getDefaults().setMultipleLagrum("any");
    assertAllowed(AccessRuleEngine.evaluate(set, two), SOC);

    // any: inget täckt ⇒ nekad
    assertDenied(AccessRuleEngine.evaluate(set, facts(SEKRETESS, List.of(OSL_26_1), EJ_PU, null)), SOC);

    // all: båda täckta ⇒ tillåten
    set.getDefaults().setMultipleLagrum("all");
    set.getRules().get(0).setLagrum(List.of("OSL 21", "OSL 26:1"));
    assertAllowed(AccessRuleEngine.evaluate(set, two), SOC);
  }

  @Test
  public void testWarningsInFactsCountAsUncoveredUnlessWildcard() {
    AccessRuleSet set = rules(rule(SOC, List.of("*"), List.of("OSL 21"), List.of("*"), List.of("*")));
    EternaAdminFacts withWarning = facts(SEKRETESS, List.of(OSL_21_7), EJ_PU, null);
    withWarning.setWarnings(List.of("Kunde inte tolka \"skräp\""));

    // all: varningen är otäckt ⇒ nekad
    AccessDecision all = AccessRuleEngine.evaluate(set, withWarning);
    assertDenied(all, SOC);
    Assert.assertTrue(all.getExplanation().get(0).contains("otolkbart"), all.getExplanation().toString());

    // any: ett lagrum täcks ⇒ tillåten
    set.getDefaults().setMultipleLagrum("any");
    assertAllowed(AccessRuleEngine.evaluate(set, withWarning), SOC);

    // bara varning, inget lagrum: inte "saknat lagrum" utan otäckt – även med
    // missingLagrum=open
    set.getDefaults().setMultipleLagrum("all");
    set.getDefaults().setMissingLagrum("open");
    EternaAdminFacts onlyWarning = facts(SEKRETESS, List.of(), EJ_PU, null);
    onlyWarning.setWarnings(List.of("Kunde inte tolka \"skräp\""));
    assertDenied(AccessRuleEngine.evaluate(set, onlyWarning), SOC);

    // "*" täcker även varningar
    set.getRules().get(0).setLagrum(List.of("*"));
    assertAllowed(AccessRuleEngine.evaluate(set, onlyWarning), SOC);
    assertAllowed(AccessRuleEngine.evaluate(set, withWarning), SOC);
  }

  @Test
  public void testInvalidPatternInRuleIsIgnoredAndFailsClosed() {
    AccessRuleSet set = rules(rule(SOC, List.of("*"), List.of("skräp", "OSL 21"), List.of("*"), List.of("*")));

    AccessDecision covered = AccessRuleEngine.evaluate(set, facts(SEKRETESS, List.of(OSL_21_7), EJ_PU, null));
    assertAllowed(covered, SOC);
    Assert.assertTrue(covered.getExplanation().get(0).contains("ogiltigt lagrumsmönster"),
      covered.getExplanation().toString());

    AccessDecision uncovered = AccessRuleEngine.evaluate(set, facts(SEKRETESS, List.of(OSL_26_1), EJ_PU, null));
    assertDenied(uncovered, SOC);
  }

  @Test
  public void testDefaultActAppliesToPatternsWithoutAct() {
    AccessRuleSet set = rules(rule(SOC, List.of("*"), List.of("2 kap. 1 §"), List.of("*"), List.of("*")));
    set.getDefaults().setDefaultAct("TF");

    assertAllowed(AccessRuleEngine.evaluate(set, facts(SEKRETESS, List.of(new Lagrum("TF", 2, 1)), EJ_PU, null)),
      SOC);
    assertDenied(AccessRuleEngine.evaluate(set, facts(SEKRETESS, List.of(new Lagrum("OSL", 2, 1)), EJ_PU, null)),
      SOC);
  }

  // ---------------------------------------------------------------- skiftläge

  @Test
  public void testValuesAreComparedCaseInsensitivelyAndTrimmed() {
    AccessRule mixed = rule(SOC, List.of(" Ingen_Sekretess ", "SEKRETESS_FOREKOMMER"), List.of("OSL 21"),
      List.of("Inga", " FOREKOMMER"), List.of("NIVA_2"));
    AccessRuleSet set = rules(mixed);

    assertAllowed(AccessRuleEngine.evaluate(set, facts("ingen_sekretess", List.of(), "inga", null)), SOC);
    assertAllowed(AccessRuleEngine.evaluate(set, facts(" Sekretess_Forekommer ", List.of(OSL_21_7), "forekommer",
      "niva_2")), SOC);
    assertAllowed(AccessRuleEngine.evaluate(set, facts(EJ_SEKRETESS, List.of(), PU, " Niva_2 ")), SOC);
    assertDenied(AccessRuleEngine.evaluate(set, facts(EJ_SEKRETESS, List.of(), PU, "niva_3")), SOC);
    assertDenied(AccessRuleEngine.evaluate(set, facts("Helt_Sekretessbelagd", List.of(OSL_21_7), PU, null)), SOC);
  }

  // ---------------------------------------------------------------- personuppgiftsnivå

  /** nivå i fakta, regelns nivålista, tillåten? */
  @DataProvider(name = "niva")
  public Object[][] niva() {
    return new Object[][] {
      {"niva_2", List.of("niva_1", "niva_2"), true}, // ----
      {"niva_3", List.of("niva_1", "niva_2"), false}, // ----
      {"niva_3", List.of("*"), true}, // ----
      {"niva_1", List.of(), false}, // tom lista = inga nivåer tillåtna
      {null, List.of(), true}, // ingen nivå på objektet ⇒ prövas inte
      {"", List.of(), true}, // ----
      {null, List.of("niva_1"), true}, // ----
      {"okand", List.of("niva_1", "niva_2", "niva_3"), false}, // okänt värde stänger
    };
  }

  @Test(dataProvider = "niva")
  public void testPersonuppgiftsniva(String niva, List<String> ruleNiva, boolean expectedAllowed) {
    AccessRuleSet set = rules(rule(SOC, List.of("*"), List.of("*"), List.of("*"), ruleNiva));

    AccessDecision decision = AccessRuleEngine.evaluate(set, facts(EJ_SEKRETESS, List.of(), PU, niva));

    Assert.assertEquals(decision.isAllowed(SOC), expectedAllowed, decision.getExplanation().toString());
    if (!expectedAllowed) {
      Assert.assertTrue(decision.getExplanation().get(0).contains("personuppgiftsnivå"),
        decision.getExplanation().toString());
    }
  }

  // ---------------------------------------------------------------- kombinationer

  @Test
  public void testAllConditionsMustHold() {
    AccessRuleSet set = rules(socRule());

    // allt uppfyllt
    assertAllowed(AccessRuleEngine.evaluate(set, facts(SEKRETESS, List.of(OSL_21_7), PU, "niva_2")), SOC);
    // fel sekretess
    assertDenied(AccessRuleEngine.evaluate(set, facts(HELT, List.of(OSL_21_7), PU, "niva_2")), SOC);
    // fel lagrum
    assertDenied(AccessRuleEngine.evaluate(set, facts(SEKRETESS, List.of(OSL_26_1), PU, "niva_2")), SOC);
    // fel personuppgifter
    assertDenied(AccessRuleEngine.evaluate(set, facts(SEKRETESS, List.of(OSL_21_7), KANSLIGA, "niva_2")), SOC);
    // fel nivå
    assertDenied(AccessRuleEngine.evaluate(set, facts(SEKRETESS, List.of(OSL_21_7), PU, "niva_3")), SOC);
    // ingen sekretess: lagrum irrelevant, nivå prövas fortfarande
    assertAllowed(AccessRuleEngine.evaluate(set, facts(EJ_SEKRETESS, List.of(OSL_26_1), PU, "niva_1")), SOC);
    assertDenied(AccessRuleEngine.evaluate(set, facts(EJ_SEKRETESS, List.of(), PU, "niva_3")), SOC);
  }

  @Test
  public void testFirstRuleWinsForDuplicateGroup() {
    AccessRule first = rule(SOC, List.of(EJ_SEKRETESS), List.of(), List.of("*"), List.of("*"));
    AccessRule second = rule(SOC, List.of("*"), List.of("*"), List.of("*"), List.of("*"));
    AccessRuleSet set = rules(first, second);

    AccessDecision decision = AccessRuleEngine.evaluate(set, facts(SEKRETESS, List.of(OSL_21_7), EJ_PU, null));

    assertDenied(decision, SOC);
    Assert.assertEquals(decision.getExplanation().size(), 1);
  }

  @Test
  public void testEvaluateDoesNotMutateInput() {
    AccessRuleSet set = rules(socRule(), handlRule());
    AccessRuleSet snapshot = rules(socRule(), handlRule());
    EternaAdminFacts facts = facts(SEKRETESS, List.of(OSL_21_7), PU, "niva_2");
    String before = facts.toString();

    AccessRuleEngine.evaluate(set, facts);

    Assert.assertEquals(set, snapshot);
    Assert.assertEquals(facts.toString(), before);
  }

  @Test
  public void testVocabularyConstantsMatchXsd() {
    Assert.assertEquals(AccessRuleEngine.SEKRETESS_VALUES,
      Set.of("ej_bedomd", "ingen_sekretess", "sekretess_forekommer", "helt_sekretessbelagd"));
    Assert.assertEquals(AccessRuleEngine.PERSONUPPGIFTER_VALUES,
      Set.of("ej_bedomd", "inga", "forekommer", "kansliga_forekommer"));
  }
}

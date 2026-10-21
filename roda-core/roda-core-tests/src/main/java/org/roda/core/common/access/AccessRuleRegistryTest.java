/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.common.access;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.v2.accessrules.AccessRule;
import org.roda.core.data.v2.accessrules.AccessRuleSet;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tester för regelregistret mot en temporär katalog
 * — JSON-rundtur, okända fält, normalisering, sparning med historik och
 * versionsräkning, ogiltig JSON eller ogiltigt innehåll efter giltig,
 * omläsning vid ändrad mtime, klassvägsfallback, oberoende kopior från get()
 * samt validering (policyval, vokabulär, nivåer, gruppnamn). Kräver varken
 * Docker, Solr, LDAP eller RodaCoreFactory.
 */
@Test(groups = {RodaConstants.TEST_GROUP_ALL, RodaConstants.TEST_GROUP_DEV, RodaConstants.TEST_GROUP_TRAVIS,
  "accessrules"})
public class AccessRuleRegistryTest {

  private static final List<String> NIVA = List.of("niva_1", "niva_2", "niva_3");

  private Path dir;
  private Path file;

  @BeforeMethod(alwaysRun = true)
  public void setUp() throws IOException {
    dir = Files.createTempDirectory("accessrules-test");
    file = dir.resolve("access-rules.json");
  }

  @AfterMethod(alwaysRun = true)
  public void tearDown() throws IOException {
    if (dir != null && Files.exists(dir)) {
      try (Stream<Path> paths = Files.walk(dir)) {
        paths.sorted(Comparator.reverseOrder()).forEach(p -> {
          try {
            Files.deleteIfExists(p);
          } catch (IOException e) {
            // bäst möjliga städning
          }
        });
      }
    }
  }

  private static AccessRuleSet sampleSet() {
    AccessRuleSet set = new AccessRuleSet();
    set.setVersion(3);
    set.setEnabled(true);
    set.getDefaults().setEjBedomd("open");
    set.getDefaults().setMissingMetadata("closed");

    AccessRule social = new AccessRule("socialtjanst");
    social.setSekretess(List.of("ingen_sekretess", "sekretess_forekommer"));
    social.setLagrum(List.of("OSL 26:1", "OSL 21"));
    social.setPersonuppgifter(List.of("*"));
    social.setPersonuppgiftsniva(List.of("niva_1", "niva_2"));
    social.setNote("Socialtjänstens handläggare");
    set.getRules().add(social);

    AccessRule handlaggare = new AccessRule("handlaggare");
    handlaggare.setEnabled(false);
    handlaggare.setSekretess(List.of("ingen_sekretess"));
    set.getRules().add(handlaggare);
    return set;
  }

  private static String json(AccessRuleSet set) {
    return AccessRuleRegistry.toJson(set);
  }

  private static void writeFile(Path file, String content) throws IOException {
    Files.writeString(file, content, StandardCharsets.UTF_8);
  }

  private static void touchForward(Path file, long seconds) throws IOException {
    Files.setLastModifiedTime(file, FileTime.from(Instant.now().plusSeconds(seconds)));
  }

  private Set<String> dirEntries() throws IOException {
    try (Stream<Path> paths = Files.list(dir)) {
      return paths.map(p -> p.getFileName().toString()).collect(Collectors.toSet());
    }
  }

  @Test
  public void testParseToJsonRoundTrip() throws IOException {
    AccessRuleSet original = sampleSet();
    original.setUpdatedBy("anna");
    original.setUpdatedOn("2026-10-08T10:00:00Z");

    String json = json(original);
    AccessRuleSet parsed = AccessRuleRegistry.parse(json);

    Assert.assertEquals(parsed, original);
    Assert.assertEquals(parsed.getVersion(), 3);
    Assert.assertTrue(parsed.isEnabled());
    Assert.assertEquals(parsed.getDefaults().getEjBedomd(), "open");
    Assert.assertEquals(parsed.getDefaults().getMissingMetadata(), "closed");
    Assert.assertEquals(parsed.getDefaults().getMultipleLagrum(), "all");
    Assert.assertEquals(parsed.getRules().size(), 2);
    Assert.assertEquals(parsed.findRule("socialtjanst").getLagrum(), List.of("OSL 26:1", "OSL 21"));
    Assert.assertFalse(parsed.findRule("handlaggare").isEnabled());
    Assert.assertEquals(parsed.getManagedGroups().size(), 2);
    // utskriften är indragen (läsbar för administratören)
    Assert.assertTrue(json.contains("\n  \"rules\""), json);
    // samma JSON efter en andra rundtur
    Assert.assertEquals(json(parsed), json);
  }

  @Test
  public void testUnknownFieldsAreIgnored() throws IOException {
    String json = "{\n" // ----
      + "  \"version\": 5,\n" // ----
      + "  \"enabled\": true,\n" // ----
      + "  \"defaults\": {\"ejBedomd\": \"closed\", \"framtida\": 1},\n" // ----
      + "  \"rules\": [{\"group\": \"arkiv\", \"sekretess\": [\"*\"], \"okant\": true}, null],\n" // ----
      + "  \"_example\": {\"_about\": \"mall\", \"rule\": {\"group\": \"x\"}}\n" // ----
      + "}";
    AccessRuleSet set = AccessRuleRegistry.parse(json);

    Assert.assertEquals(set.getVersion(), 5);
    Assert.assertTrue(set.isEnabled());
    Assert.assertEquals(set.getDefaults().getEjBedomd(), "closed");
    // saknade defaults-fält får standardvärden
    Assert.assertEquals(set.getDefaults().getMissingMetadata(), "inherit");
    // null-poster tas bort, _example är inte en regel
    Assert.assertEquals(set.getRules().size(), 1);
    Assert.assertEquals(set.getRules().get(0).getGroup(), "arkiv");
    Assert.assertTrue(set.getRules().get(0).getLagrum().isEmpty());
    Assert.assertFalse(AccessRuleRegistry.toJson(set).contains("_example"));
  }

  @Test
  public void testParseNormalizesValues() throws IOException {
    String json = "{\n" // ----
      + "  \"version\": 1,\n" // ----
      + "  \"enabled\": true,\n" // ----
      + "  \"defaults\": {\"ejBedomd\": \" CLOSED \", \"missingMetadata\": \"Inherit\", \"defaultAct\": \" OSL \"},\n"
      + "  \"rules\": [{\"group\": \" Socialtjanst \", \"sekretess\": [\" Ingen_Sekretess \", \"*\"],\n" // ----
      + "    \"lagrum\": [\" OSL 21 \"], \"personuppgifter\": [\"INGA\"], \"personuppgiftsniva\": [\" Niva_1\"]}]\n"
      + "}";
    AccessRuleSet set = AccessRuleRegistry.parse(json);

    Assert.assertEquals(set.getDefaults().getEjBedomd(), "closed");
    Assert.assertEquals(set.getDefaults().getMissingMetadata(), "inherit");
    Assert.assertEquals(set.getDefaults().getDefaultAct(), "OSL");
    AccessRule rule = set.getRules().get(0);
    Assert.assertEquals(rule.getGroup(), "Socialtjanst", "gruppnamn trimmas men behåller skiftläge");
    Assert.assertEquals(rule.getSekretess(), List.of("ingen_sekretess", "*"));
    Assert.assertEquals(rule.getLagrum(), List.of("OSL 21"));
    Assert.assertEquals(rule.getPersonuppgifter(), List.of("inga"));
    Assert.assertEquals(rule.getPersonuppgiftsniva(), List.of("niva_1"));
    Assert.assertTrue(AccessRuleRegistry.validate(set).isEmpty(), AccessRuleRegistry.validate(set).toString());
  }

  @Test(expectedExceptions = IOException.class)
  public void testParseInvalidJsonThrows() throws IOException {
    AccessRuleRegistry.parse("{ \"version\": ");
  }

  @Test(expectedExceptions = IOException.class)
  public void testParseBlankThrows() throws IOException {
    AccessRuleRegistry.parse("   ");
  }

  @Test
  public void testMissingFileGivesEmptyDisabledSet() {
    AccessRuleRegistry registry = new AccessRuleRegistry(file);
    AccessRuleSet set = registry.get();

    Assert.assertNotNull(set);
    Assert.assertFalse(set.isEnabled());
    Assert.assertEquals(set.getVersion(), 0);
    Assert.assertTrue(set.getRules().isEmpty());
    Assert.assertFalse(Files.exists(file));
  }

  @Test
  public void testMissingFileUsesFallbackUntilFileAppears() throws IOException {
    AccessRuleSet fallbackSet = new AccessRuleSet();
    fallbackSet.setVersion(7);
    fallbackSet.setEnabled(false);
    byte[] fallbackJson = json(fallbackSet).getBytes(StandardCharsets.UTF_8);
    AccessRuleRegistry registry = new AccessRuleRegistry(file, () -> new ByteArrayInputStream(fallbackJson));

    Assert.assertEquals(registry.get().getVersion(), 7);
    Assert.assertEquals(registry.get().getVersion(), 7);

    // filen dyker upp (t.ex. kopierad in i RODA_HOME/config) ⇒ den gäller
    writeFile(file, json(sampleSet()));
    Assert.assertEquals(registry.get().getVersion(), 3);
    Assert.assertTrue(registry.get().isEnabled());
  }

  @Test
  public void testInvalidFallbackGivesEmptyDisabledSet() {
    byte[] broken = "{\"version\": 2, \"enabled\": true, \"defaults\": {\"missingMetadata\": \"kanske\"}}"
      .getBytes(StandardCharsets.UTF_8);
    AccessRuleRegistry registry = new AccessRuleRegistry(file, () -> new ByteArrayInputStream(broken));

    AccessRuleSet set = registry.get();
    Assert.assertFalse(set.isEnabled());
    Assert.assertTrue(set.getRules().isEmpty());
    Assert.assertEquals(set.getVersion(), 0);
  }

  @Test
  public void testSaveCreatesFileHistoryAndIncrementsVersion() throws IOException {
    AccessRuleRegistry registry = new AccessRuleRegistry(file);
    Assert.assertEquals(registry.get().getVersion(), 0);

    AccessRuleSet first = sampleSet();
    first.setVersion(0);
    registry.save(first, "anna");

    // ingen fil fanns ⇒ ingen historik, version 1
    Assert.assertTrue(Files.isRegularFile(file));
    Assert.assertEquals(first.getVersion(), 1);
    Assert.assertEquals(first.getUpdatedBy(), "anna");
    Assert.assertNotNull(first.getUpdatedOn());
    Assert.assertTrue(first.getUpdatedOn().matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z"),
      first.getUpdatedOn());
    Assert.assertTrue(registry.listHistory().isEmpty());
    Assert.assertEquals(registry.get().getVersion(), 1);
    Assert.assertEquals(AccessRuleRegistry.parse(Files.readString(file, StandardCharsets.UTF_8)).getVersion(), 1);
    // ingen tmp-fil ligger kvar
    Assert.assertEquals(dirEntries(), Set.of("access-rules.json"));

    // andra sparningen: föregående fil arkiveras, version 2
    AccessRuleSet second = registry.get();
    second.setEnabled(false);
    registry.save(second, "bengt.o/sson");

    Assert.assertEquals(second.getVersion(), 2);
    Assert.assertEquals(registry.get().getVersion(), 2);
    Assert.assertFalse(registry.get().isEnabled());
    Assert.assertEquals(dirEntries(), Set.of("access-rules.json", AccessRuleRegistry.HISTORY_DIR_NAME));
    List<Path> history = registry.listHistory();
    Assert.assertEquals(history.size(), 1);
    String historyName = history.get(0).getFileName().toString();
    Assert.assertTrue(historyName.matches("\\d{14}-bengt\\.o_sson\\.json"), historyName);
    AccessRuleSet archived = AccessRuleRegistry.parse(Files.readString(history.get(0), StandardCharsets.UTF_8));
    Assert.assertEquals(archived.getVersion(), 1);
    Assert.assertTrue(archived.isEnabled());

    // en inaktuell inskickad version (lägre än filens) räknas ändå upp från filens
    AccessRuleSet stale = sampleSet();
    stale.setVersion(0);
    registry.save(stale, null);
    Assert.assertEquals(stale.getVersion(), 3);
    Assert.assertEquals(registry.listHistory().size(), 2);
    Assert.assertTrue(registry.listHistory().get(1).getFileName().toString().endsWith("-unknown.json"));

    // cachen är en egen kopia: ändringar i anroparens objekt syns inte
    stale.setEnabled(false);
    Assert.assertTrue(registry.get().isEnabled());

    // en inskickad version högre än filens får inte räknaren att hoppa: alltid filens + 1
    AccessRuleSet ahead = sampleSet();
    ahead.setVersion(999999);
    registry.save(ahead, "anna");
    Assert.assertEquals(ahead.getVersion(), 4);
    Assert.assertEquals(registry.get().getVersion(), 4);
  }

  @Test
  public void testGetReturnsIndependentCopy() throws IOException {
    writeFile(file, json(sampleSet()));
    AccessRuleRegistry registry = new AccessRuleRegistry(file);

    AccessRuleSet first = registry.get();
    AccessRuleSet second = registry.get();
    Assert.assertNotSame(first, second);
    Assert.assertEquals(first, second);

    // mutation av det returnerade objektet når inte registret
    first.setEnabled(false);
    first.getDefaults().setMissingMetadata("open");
    first.getRules().clear();
    AccessRuleSet third = registry.get();
    Assert.assertTrue(third.isEnabled());
    Assert.assertEquals(third.getDefaults().getMissingMetadata(), "closed");
    Assert.assertEquals(third.getRules().size(), 2);

    // en misslyckad sparning lämnar cachen orörd
    AccessRuleSet invalid = registry.get();
    invalid.getDefaults().setMissingMetadata("kanske");
    try {
      registry.save(invalid, "anna");
      Assert.fail("ogiltig policy ska avvisas");
    } catch (IllegalArgumentException e) {
      Assert.assertTrue(e.getMessage().contains("missingMetadata"), e.getMessage());
    }
    Assert.assertEquals(registry.get().getDefaults().getMissingMetadata(), "closed");
    Assert.assertEquals(registry.get().getVersion(), 3);
  }

  @Test
  public void testInvalidJsonAfterValidKeepsLastValid() throws IOException {
    writeFile(file, json(sampleSet()));
    AccessRuleRegistry registry = new AccessRuleRegistry(file);
    Assert.assertEquals(registry.get().getVersion(), 3);

    writeFile(file, "{ \"version\": 4, \"rules\": [ trasigt");
    touchForward(file, 10);
    AccessRuleSet afterBreak = registry.get();
    Assert.assertEquals(afterBreak.getVersion(), 3);
    Assert.assertTrue(afterBreak.isEnabled());
    Assert.assertEquals(afterBreak.getRules().size(), 2);
    // upprepade anrop ger fortfarande senaste giltiga
    Assert.assertEquals(registry.get().getVersion(), 3);

    // när filen rättas läses den nya versionen
    AccessRuleSet fixed = sampleSet();
    fixed.setVersion(4);
    writeFile(file, json(fixed));
    touchForward(file, 20);
    Assert.assertEquals(registry.get().getVersion(), 4);
  }

  @Test
  public void testInvalidContentAfterValidKeepsLastValid() throws IOException {
    writeFile(file, json(sampleSet()));
    AccessRuleRegistry registry = new AccessRuleRegistry(file);
    Assert.assertEquals(registry.get().getVersion(), 3);

    // felstavat policyval (skulle annars kunna öppna) ⇒ filen avvisas
    AccessRuleSet typo = sampleSet();
    typo.setVersion(4);
    typo.getDefaults().setMissingMetadata("opne");
    writeFile(file, json(typo));
    touchForward(file, 10);
    Assert.assertEquals(registry.get().getVersion(), 3);
    Assert.assertEquals(registry.get().getDefaults().getMissingMetadata(), "closed");

    // värde utanför vokabulären ⇒ filen avvisas
    AccessRuleSet vocab = sampleSet();
    vocab.setVersion(5);
    vocab.findRule("socialtjanst").setSekretess(List.of("ej_sekretess"));
    writeFile(file, json(vocab));
    touchForward(file, 20);
    Assert.assertEquals(registry.get().getVersion(), 3);

    // rättad fil läses
    AccessRuleSet fixed = sampleSet();
    fixed.setVersion(6);
    writeFile(file, json(fixed));
    touchForward(file, 30);
    Assert.assertEquals(registry.get().getVersion(), 6);
  }

  @Test
  public void testInvalidJsonWithoutPreviousValidGivesDisabledSet() throws IOException {
    writeFile(file, "inte json");
    AccessRuleRegistry registry = new AccessRuleRegistry(file);
    AccessRuleSet set = registry.get();
    Assert.assertFalse(set.isEnabled());
    Assert.assertTrue(set.getRules().isEmpty());

    writeFile(file, json(sampleSet()));
    touchForward(file, 10);
    Assert.assertEquals(registry.get().getVersion(), 3);
  }

  @Test
  public void testInvalidContentWithoutPreviousValidGivesDisabledSetWithoutRules() throws IOException {
    AccessRuleSet typo = sampleSet();
    typo.getDefaults().setEjBedomd("öppen");
    writeFile(file, json(typo));
    AccessRuleRegistry registry = new AccessRuleRegistry(file);

    AccessRuleSet set = registry.get();
    Assert.assertFalse(set.isEnabled());
    Assert.assertTrue(set.getRules().isEmpty(), "en ogiltig fil får inte ge några hanterade grupper");
    Assert.assertTrue(set.getManagedGroups().isEmpty());
  }

  @Test
  public void testModifiedFileIsReloaded() throws IOException {
    AccessRuleSet v1 = sampleSet();
    v1.setVersion(1);
    v1.setEnabled(false);
    writeFile(file, json(v1));
    AccessRuleRegistry registry = new AccessRuleRegistry(file);
    Assert.assertEquals(registry.get().getVersion(), 1);
    Assert.assertFalse(registry.get().isEnabled());
    AccessRuleSet cachedBefore = registry.get();
    Assert.assertEquals(registry.get(), cachedBefore, "oförändrad fil ska ge samma innehåll");

    AccessRuleSet v2 = sampleSet();
    v2.setVersion(2);
    v2.setEnabled(true);
    v2.getRules().add(new AccessRule("nyGrupp"));
    writeFile(file, json(v2));
    touchForward(file, 10);

    AccessRuleSet reloaded = registry.get();
    Assert.assertNotEquals(reloaded, cachedBefore);
    Assert.assertEquals(reloaded.getVersion(), 2);
    Assert.assertTrue(reloaded.isEnabled());
    Assert.assertEquals(reloaded.getRules().size(), 3);
    Assert.assertTrue(reloaded.getManagedGroups().contains("nyGrupp"));
  }

  @Test
  public void testInvalidateForcesReload() throws IOException {
    writeFile(file, json(sampleSet()));
    AccessRuleRegistry registry = new AccessRuleRegistry(file);
    AccessRuleSet before = registry.get();
    registry.invalidate();
    AccessRuleSet after = registry.get();
    Assert.assertNotSame(after, before);
    Assert.assertEquals(after, before);
  }

  @Test
  public void testValidateFindsProblems() {
    AccessRuleSet set = sampleSet();
    Assert.assertTrue(AccessRuleRegistry.validate(set).isEmpty(), AccessRuleRegistry.validate(set).toString());

    set.getDefaults().setMissingMetadata("kanske");
    set.getRules().add(new AccessRule("socialtjanst")); // dubblett
    AccessRule blank = new AccessRule("  ");
    blank.setLagrum(List.of("OSL 21 kap. 7 §", "hej hopp", "*"));
    blank.setSekretess(List.of("ingen_sekretess", ""));
    set.getRules().add(blank);

    List<String> problems = AccessRuleRegistry.validate(set);
    Assert.assertEquals(problems.size(), 5, problems.toString());
    Assert.assertTrue(problems.stream().anyMatch(p -> p.contains("defaults.missingMetadata")), problems.toString());
    Assert.assertTrue(problems.stream().anyMatch(p -> p.contains("mer än en regel")), problems.toString());
    Assert.assertTrue(problems.stream().anyMatch(p -> p.contains("group saknas")), problems.toString());
    Assert.assertTrue(problems.stream().anyMatch(p -> p.contains("tomt värde")), problems.toString());
    Assert.assertTrue(problems.stream().anyMatch(p -> p.contains("hej hopp")), problems.toString());

    Assert.assertEquals(AccessRuleRegistry.validate(null).size(), 1);
  }

  @Test
  public void testValidateRejectsOverlongLagrumPattern() {
    AccessRuleSet set = sampleSet();
    // tolkbart i sig (svansen ignoreras) men längre än gränsen
    String overlong = "OSL 21:7 " + "1 st. ".repeat(40);
    Assert.assertTrue(overlong.length() > AccessRuleRegistry.MAX_PATTERN_LENGTH);
    set.findRule("socialtjanst").setLagrum(List.of("OSL 21:7", overlong));

    List<String> problems = AccessRuleRegistry.validate(set);
    Assert.assertEquals(problems.size(), 1, problems.toString());
    Assert.assertTrue(problems.get(0).contains(".lagrum") && problems.get(0).contains("längre än"),
      problems.toString());
  }

  @Test
  public void testValidatePolicyValuesAreCaseInsensitive() {
    AccessRuleSet set = sampleSet();
    set.getDefaults().setEjBedomd("OPEN");
    set.getDefaults().setMissingMetadata(" Inherit ");
    set.getDefaults().setMultipleLagrum("ANY");
    Assert.assertTrue(AccessRuleRegistry.validate(set).isEmpty(), AccessRuleRegistry.validate(set).toString());
  }

  @Test
  public void testValidateVocabulary() {
    AccessRuleSet set = sampleSet();
    set.findRule("socialtjanst").setSekretess(List.of("ej_sekretess", "Sekretess_Forekommer"));
    set.findRule("socialtjanst").setPersonuppgifter(List.of("ej_personuppgifter", "personuppgifter_forekommer"));
    set.findRule("handlaggare").setSekretess(List.of("*"));

    List<String> problems = AccessRuleRegistry.validate(set);
    Assert.assertEquals(problems.size(), 2, problems.toString());
    Assert.assertTrue(problems.stream().anyMatch(
      p -> p.contains(".sekretess") && p.contains("ej_sekretess") && !p.contains("Sekretess_Forekommer")),
      problems.toString());
    Assert.assertTrue(problems.stream().anyMatch(p -> p.contains(".personuppgifter")
      && p.contains("ej_personuppgifter") && p.contains("personuppgifter_forekommer")), problems.toString());
    Assert.assertTrue(problems.get(0).contains("tillåtna"), problems.toString());

    // "*" och ej_bedomd är alltid tillåtna
    set.findRule("socialtjanst").setSekretess(List.of("*", "ej_bedomd"));
    set.findRule("socialtjanst").setPersonuppgifter(List.of("ej_bedomd", "kansliga_forekommer"));
    Assert.assertTrue(AccessRuleRegistry.validate(set).isEmpty(), AccessRuleRegistry.validate(set).toString());
  }

  @Test
  public void testValidateNivaAndGroupCatalog() {
    AccessRuleSet set = sampleSet();
    List<String> known = List.of("socialtjanst", "handlaggare", "administrators");

    Assert.assertTrue(AccessRuleRegistry.validate(set, NIVA, known).isEmpty(),
      AccessRuleRegistry.validate(set, NIVA, known).toString());

    // nivå utanför vokabulären
    set.findRule("socialtjanst").setPersonuppgiftsniva(List.of("niva_1", "niva_9"));
    List<String> problems = AccessRuleRegistry.validate(set, NIVA, known);
    Assert.assertEquals(problems.size(), 1, problems.toString());
    Assert.assertTrue(problems.get(0).contains("personuppgiftsniva") && problems.get(0).contains("niva_9"),
      problems.toString());
    // utan nivåvokabulär kontrolleras nivåerna inte
    Assert.assertTrue(AccessRuleRegistry.validate(set, null, known).isEmpty());
    Assert.assertTrue(AccessRuleRegistry.validate(set, List.of(), known).isEmpty());
    set.findRule("socialtjanst").setPersonuppgiftsniva(List.of("*"));

    // fel skiftläge mot katalogen
    set.findRule("socialtjanst").setGroup("Socialtjanst");
    problems = AccessRuleRegistry.validate(set, NIVA, known);
    Assert.assertEquals(problems.size(), 1, problems.toString());
    Assert.assertTrue(problems.get(0).contains("finns inte i katalogen"), problems.toString());
    // utan katalog godtas namnet
    Assert.assertTrue(AccessRuleRegistry.validate(set, NIVA, null).isEmpty());

    // inre blanksteg avvisas alltid
    set.findRule("Socialtjanst").setGroup("social tjanst");
    problems = AccessRuleRegistry.validate(set, NIVA, null);
    Assert.assertEquals(problems.size(), 1, problems.toString());
    Assert.assertTrue(problems.get(0).contains("blanksteg"), problems.toString());
  }

  @Test
  public void testRegistryEnforcesNivaVocabularyOnSaveAndLoad() throws IOException {
    AccessRuleRegistry registry = new AccessRuleRegistry(file, null, NIVA);
    Assert.assertEquals(registry.getPersonuppgiftsnivaValues(), NIVA);

    AccessRuleSet set = sampleSet();
    set.findRule("socialtjanst").setPersonuppgiftsniva(List.of("niva_9"));
    try {
      registry.save(set, "anna");
      Assert.fail("okänd nivå ska avvisas");
    } catch (IllegalArgumentException e) {
      Assert.assertTrue(e.getMessage().contains("niva_9"), e.getMessage());
    }
    Assert.assertFalse(Files.exists(file));

    // en fil med okänd nivå laddas inte heller
    writeFile(file, json(set));
    Assert.assertTrue(registry.get().getRules().isEmpty());

    // giltig nivå sparas
    set.findRule("socialtjanst").setPersonuppgiftsniva(List.of("NIVA_3"));
    registry.save(set, "anna");
    Assert.assertEquals(registry.get().findRule("socialtjanst").getPersonuppgiftsniva(), List.of("niva_3"));
  }

  @Test
  public void testSaveRejectsInvalidSet() throws IOException {
    AccessRuleRegistry registry = new AccessRuleRegistry(file);
    AccessRuleSet set = sampleSet();
    set.findRule("socialtjanst").setLagrum(List.of("OSL 21 kap. 7 §", "26 kap. 1 §", "fel"));
    try {
      registry.save(set, "anna");
      Assert.fail("Ogiltigt lagrumsmönster ska avvisas");
    } catch (IllegalArgumentException e) {
      Assert.assertTrue(e.getMessage().contains("fel"), e.getMessage());
    }
    Assert.assertFalse(Files.exists(file));
    // det inskickade objektet stämplas inte vid misslyckad sparning
    Assert.assertEquals(set.getVersion(), 3);
    Assert.assertNull(set.getUpdatedBy());
  }

  @Test
  public void testSaveNormalizesWhatIsWritten() throws IOException {
    AccessRuleRegistry registry = new AccessRuleRegistry(file);
    AccessRuleSet set = sampleSet();
    set.getDefaults().setEjBedomd("CLOSED");
    AccessRule social = set.findRule("socialtjanst");
    social.setGroup(" socialtjanst ");
    social.setSekretess(List.of("Ingen_Sekretess"));
    registry.save(set, "anna");

    AccessRuleSet onDisk = AccessRuleRegistry.parse(Files.readString(file, StandardCharsets.UTF_8));
    Assert.assertEquals(onDisk.getDefaults().getEjBedomd(), "closed");
    Assert.assertNotNull(onDisk.findRule("socialtjanst"));
    Assert.assertEquals(onDisk.findRule("socialtjanst").getSekretess(), List.of("ingen_sekretess"));
    // ingen fil fanns (lagrad version 0) ⇒ 1; att sampleSet anger version 3 spelar ingen roll
    Assert.assertEquals(onDisk.getVersion(), 1);
    Assert.assertEquals(set.getVersion(), 1);
    Assert.assertEquals(onDisk.getUpdatedBy(), "anna");
  }

  @Test
  public void testNowIsoFormat() {
    Assert.assertTrue(AccessRuleRegistry.nowIso().matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z"));
  }
}

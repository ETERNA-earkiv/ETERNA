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
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.roda.core.RodaCoreFactory;
import org.roda.core.data.v2.accessrules.AccessRule;
import org.roda.core.data.v2.accessrules.AccessRuleDefaults;
import org.roda.core.data.v2.accessrules.AccessRuleSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Register över behörighetsreglerna — läser,
 * validerar, cachar och sparar regelfilen (normalt
 * RODA_HOME/config/access-rules.json, se core.access_rules.file).
 *
 * <ul>
 * <li>{@link #get()} returnerar en <em>kopia</em> av den cachade
 * regeluppsättningen (anroparen får ändra den fritt utan att påverka
 * registret) och läser om filen när dess mtime eller storlek ändrats
 * (drop-in-redigering i drift). Filen valideras vid inläsning
 * ({@link #validate}); ogiltig JSON eller ogiltigt innehåll loggas som fel
 * och den senast giltiga versionen behålls. Finns ingen giltig version
 * används en tom, avstängd regeluppsättning (inga hanterade grupper ⇒
 * applikatorn rör ingenting).</li>
 * <li>Saknas filen i konfigurationskatalogen används klassvägsstandarden
 * (config/access-rules.json i roda-core, tom regellista och enabled=false);
 * saknas även den används en tom, avstängd regeluppsättning.</li>
 * <li>{@link #save(AccessRuleSet, String)} normaliserar och validerar en
 * kopia, stämplar updatedBy/updatedOn, sätter version = lagrad version + 1
 * (den inskickade versionen ignoreras; konfliktkontrollen görs av
 * anroparen, se AccessRulesService), kopierar
 * föregående fil till {@code <dir>/access-rules.history/<yyyyMMddHHmmss>-<user>.json}
 * och skriver atomiskt (tmp + move). Misslyckad validering lämnar cache och
 * fil orörda.</li>
 * <li>Värden normaliseras vid inläsning ({@link #parse}): policyval och
 * vokabulärvärden trimmas och görs till gemener, gruppnamn och
 * lagrumsmönster trimmas.</li>
 * </ul>
 *
 * Instansen från {@link #getInstance()} är en lat singleton som använder
 * RodaCoreFactory; konstruktorerna med {@link Path} är till för tester och
 * verktyg och kräver ingen uppstartad kärna. Alla instansmetoder är
 * synkroniserade.
 */
public class AccessRuleRegistry {

  private static final Logger LOGGER = LoggerFactory.getLogger(AccessRuleRegistry.class);

  /** Regelfilens namn när core.access_rules.file saknas. */
  public static final String DEFAULT_FILE_NAME = "access-rules.json";
  /** Katalog (bredvid regelfilen) där föregående versioner sparas. */
  public static final String HISTORY_DIR_NAME = "access-rules.history";

  static final String[] CONFIG_FILE_KEY = {"core", "access_rules", "file"};
  /** Tillåtna personuppgiftsnivåer (flervärdig nyckel i roda-core.properties). */
  static final String CONFIG_NIVA_VALUES_KEY = "core.access_rules.personuppgiftsniva.values[]";
  /**
   * Längsta tillåtna lagrumsmönster (tecken). Ett giltigt mönster är några
   * tiotal tecken; gränsen hindrar att en lång text skickas genom
   * {@link LagrumPattern#parse} (regex) från REST-API:et.
   */
  public static final int MAX_PATTERN_LENGTH = 200;

  private static final DateTimeFormatter HISTORY_STAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
    .withZone(ZoneOffset.UTC);

  private static final ObjectMapper MAPPER = newMapper();

  private static volatile AccessRuleRegistry instance;

  private final Path file;
  private final Supplier<InputStream> fallback;
  /** Tillåtna personuppgiftsnivåer (gemener) eller null = ingen kontroll. */
  private final List<String> personuppgiftsnivaValues;

  /** Senast giltiga regeluppsättning (null = aldrig läst). */
  private AccessRuleSet cached;
  /** true om {@link #cached} kommer från filen, false om från fallback. */
  private boolean cachedFromFile;
  /** mtime/storlek för den filversion som senast lästes (giltig eller ej). */
  private FileTime seenMtime;
  private long seenSize = -1;

  /**
   * Register för en given regelfil utan klassvägsfallback (saknas filen ges
   * en tom, avstängd regeluppsättning) och utan nivåvokabulär. Avsedd för
   * tester och verktyg.
   */
  public AccessRuleRegistry(Path file) {
    this(file, null, null);
  }

  /** Som ovan men med fallback-innehåll när filen saknas. */
  public AccessRuleRegistry(Path file, Supplier<InputStream> fallback) {
    this(file, fallback, null);
  }

  /**
   * @param file
   *          regelfilen
   * @param fallback
   *          leverantör av standardinnehåll när filen saknas (får returnera
   *          null), eller null
   * @param personuppgiftsnivaValues
   *          tillåtna värden för personuppgiftsniva i regler (konfig
   *          core.access_rules.personuppgiftsniva.values[]); null eller tom
   *          ⇒ nivåer valideras inte mot någon vokabulär
   */
  public AccessRuleRegistry(Path file, Supplier<InputStream> fallback, Collection<String> personuppgiftsnivaValues) {
    this.file = Objects.requireNonNull(file, "file").toAbsolutePath().normalize();
    this.fallback = fallback;
    this.personuppgiftsnivaValues = personuppgiftsnivaValues == null || personuppgiftsnivaValues.isEmpty() ? null
      : Collections.unmodifiableList(lowerAll(personuppgiftsnivaValues));
  }

  /** Lat singleton kopplad till RODA-konfigurationen. */
  public static AccessRuleRegistry getInstance() {
    AccessRuleRegistry local = instance;
    if (local == null) {
      synchronized (AccessRuleRegistry.class) {
        local = instance;
        if (local == null) {
          local = createFromConfiguration();
          instance = local;
        }
      }
    }
    return local;
  }

  /**
   * Nollställer den lata singletonen så att nästa {@link #getInstance()}
   * bygger om registret ur den aktuella konfigurationen. Avsedd för tester
   * som startar en egen RODA_HOME (regelfilen ligger under dess
   * konfigurationskatalog); ska inte användas i drift.
   */
  public static synchronized void resetInstanceForTests() {
    instance = null;
  }

  /**
   * Bygger registret ur konfigurationen: filnamnet från
   * core.access_rules.file (relativt RODA_HOME/config; en sökväg som pekar
   * utanför konfigurationskatalogen avvisas), klassvägsstandarden som
   * fallback och nivåvokabulären från
   * core.access_rules.personuppgiftsniva.values[].
   */
  static AccessRuleRegistry createFromConfiguration() {
    String configured = RodaCoreFactory.getRodaConfigurationAsString(CONFIG_FILE_KEY);
    String name = configured == null || configured.isBlank() ? DEFAULT_FILE_NAME : configured.trim();
    Path configPath = RodaCoreFactory.getConfigPath().toAbsolutePath().normalize();
    Path resolved = configPath.resolve(name).normalize();
    if (!resolved.startsWith(configPath)) {
      LOGGER.error("core.access_rules.file = '{}' pekar utanför konfigurationskatalogen – använder {}", name,
        DEFAULT_FILE_NAME);
      name = DEFAULT_FILE_NAME;
      resolved = configPath.resolve(name);
    }
    final String fallbackName = name;
    List<String> niva = RodaCoreFactory.getRodaConfigurationAsList(CONFIG_NIVA_VALUES_KEY);
    return new AccessRuleRegistry(resolved,
      () -> RodaCoreFactory.getConfigurationFileAsStream(fallbackName, DEFAULT_FILE_NAME), niva);
  }

  /** Regelfilen (absolut sökväg). */
  public Path getFile() {
    return file;
  }

  /** Historikkatalogen bredvid regelfilen. */
  public Path getHistoryDir() {
    return file.resolveSibling(HISTORY_DIR_NAME);
  }

  /** Tillåtna personuppgiftsnivåer (gemener) eller null om ingen kontroll görs. */
  public List<String> getPersonuppgiftsnivaValues() {
    return personuppgiftsnivaValues;
  }

  /**
   * Aktuell regeluppsättning som en egen kopia — ändringar i det returnerade
   * objektet påverkar inte registret förrän {@link #save} anropas. Läser om
   * filen när den ändrats; vid ogiltig fil behålls senast giltiga version.
   * Returnerar aldrig null.
   */
  public synchronized AccessRuleSet get() {
    return copyOf(current());
  }

  /** Den cachade (muterbara) regeluppsättningen; bara för internt bruk. */
  private AccessRuleSet current() {
    if (Files.isRegularFile(file)) {
      FileTime mtime;
      long size;
      try {
        mtime = Files.getLastModifiedTime(file);
        size = Files.size(file);
      } catch (IOException e) {
        LOGGER.warn("Kunde inte läsa attribut för regelfilen {}: {}", file, e.getMessage());
        return cached != null ? cached : emptyDisabled();
      }

      boolean changed = !cachedFromFile || !mtime.equals(seenMtime) || size != seenSize;
      if (cached == null || changed) {
        seenMtime = mtime;
        seenSize = size;
        try {
          String json = Files.readString(file, StandardCharsets.UTF_8);
          // attributen läses igen efter innehållet: ändrades filen under
          // läsningen läses den om vid nästa anrop i stället för att ett
          // halvskrivet innehåll blir liggande i cachen
          if (!Files.getLastModifiedTime(file).equals(mtime) || Files.size(file) != size) {
            seenMtime = null;
            seenSize = -1;
          }
          AccessRuleSet loaded = parse(json);
          List<String> problems = validate(loaded, personuppgiftsnivaValues, null);
          if (!problems.isEmpty()) {
            throw new IOException("ogiltigt innehåll: " + String.join("; ", problems));
          }
          cached = loaded;
          cachedFromFile = true;
          LOGGER.info("Läste behörighetsregler version {} ({} regler, enabled={}) från {}", loaded.getVersion(),
            loaded.getRules().size(), loaded.isEnabled(), file);
        } catch (IOException e) {
          if (cached == null) {
            LOGGER.error("Regelfilen {} kan inte användas ({}) och ingen tidigare giltig version finns – "
              + "reglerna behandlas som avstängda (inga hanterade grupper) tills filen rättats", file,
              e.getMessage());
            cached = emptyDisabled();
            cachedFromFile = true;
          } else {
            LOGGER.error("Regelfilen {} kan inte användas ({}) – behåller senast giltiga version {}", file,
              e.getMessage(), cached.getVersion());
          }
        }
      }
      return cached;
    }

    // filen saknas: läs standarden en gång (eller på nytt om filen tagits bort)
    if (cached == null || cachedFromFile) {
      cached = loadFallback();
      cachedFromFile = false;
      seenMtime = null;
      seenSize = -1;
    }
    return cached;
  }

  /** Tvingar omläsning vid nästa {@link #get()}. */
  public synchronized void invalidate() {
    cached = null;
    cachedFromFile = false;
    seenMtime = null;
    seenSize = -1;
  }

  /**
   * Sparar en ny version av reglerna. En normaliserad kopia valideras och
   * skrivs; på det inskickade objektet sätts sedan version = sparad version
   * + 1 (den inskickade versionen ignoreras så att räknaren inte kan hoppa)
   * samt updatedBy/updatedOn (ISO-8601 UTC).
   * Föregående fil kopieras till historikkatalogen och den nya skrivs
   * atomiskt. Cachen uppdateras först när filen skrivits.
   *
   * @throws IllegalArgumentException
   *           om regeluppsättningen inte klarar {@link #validate(AccessRuleSet)}
   *           (registrets nivåvokabulär används); cache och fil rörs då inte
   * @throws IOException
   *           vid skrivfel
   */
  public synchronized void save(AccessRuleSet set, String user) throws IOException {
    if (set == null) {
      throw new IllegalArgumentException("Regeluppsättning saknas");
    }
    AccessRuleSet copy = copyOf(set);
    List<String> problems = validate(copy, personuppgiftsnivaValues, null);
    if (!problems.isEmpty()) {
      throw new IllegalArgumentException("Ogiltig regeluppsättning: " + String.join("; ", problems));
    }

    AccessRuleSet current = current();
    copy.setVersion(current.getVersion() + 1);
    copy.setUpdatedBy(user);
    copy.setUpdatedOn(nowIso());
    String json = toJson(copy);

    Files.createDirectories(file.getParent());
    if (Files.isRegularFile(file)) {
      archiveCurrent(user);
    }

    Path tmp = file.resolveSibling(file.getFileName().toString() + ".tmp" + System.nanoTime());
    try {
      Files.writeString(tmp, json, StandardCharsets.UTF_8);
      try {
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } catch (AtomicMoveNotSupportedException e) {
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
      }
    } finally {
      Files.deleteIfExists(tmp);
    }

    cached = parse(json);
    cachedFromFile = true;
    seenMtime = Files.getLastModifiedTime(file);
    seenSize = Files.size(file);

    // spegla stämplarna på anroparens objekt
    set.setVersion(copy.getVersion());
    set.setUpdatedBy(copy.getUpdatedBy());
    set.setUpdatedOn(copy.getUpdatedOn());
    LOGGER.info("Sparade behörighetsregler version {} ({} regler, enabled={}) av {} till {}", cached.getVersion(),
      cached.getRules().size(), cached.isEnabled(), user, file);
  }

  /** Historikfiler, äldst först (tom lista om katalogen saknas). */
  public List<Path> listHistory() throws IOException {
    Path dir = getHistoryDir();
    if (!Files.isDirectory(dir)) {
      return Collections.emptyList();
    }
    try (Stream<Path> files = Files.list(dir)) {
      return files.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().toList();
    }
  }

  /**
   * Kontrollerar en regeluppsättning utan nivåvokabulär och utan
   * gruppkatalog; se {@link #validate(AccessRuleSet, Collection, Collection)}.
   */
  public static List<String> validate(AccessRuleSet set) {
    return validate(set, null, null);
  }

  /**
   * Kontrollerar en regeluppsättning: policyval (skiftlägesokänsligt),
   * gruppnamn (ej tomma, inga blanksteg, inga dubbletter, finns i katalogen
   * om en sådan ges), tomma listvärden, vokabulär för sekretess och
   * personuppgifter enligt eterna_admin.xsd (plus "*"), personuppgiftsnivå
   * mot given vokabulär (plus "*") samt att alla lagrumsmönster är högst
   * {@link #MAX_PATTERN_LENGTH} tecken och går att tolka. Värden jämförs
   * trimmade och i gemener. Tom lista = giltig.
   *
   * @param set
   *          regeluppsättningen
   * @param allowedNiva
   *          tillåtna personuppgiftsnivåer eller null/tom = ingen kontroll
   * @param knownGroups
   *          gruppnamn i katalogen (t.ex. ModelService.listGroups) eller
   *          null = ingen kontroll
   */
  public static List<String> validate(AccessRuleSet set, Collection<String> allowedNiva,
    Collection<String> knownGroups) {
    List<String> problems = new ArrayList<>();
    if (set == null) {
      problems.add("regeluppsättning saknas");
      return problems;
    }
    AccessRuleDefaults defaults = set.getDefaults();
    checkChoice(problems, "defaults.ejBedomd", defaults.getEjBedomd(), "closed", "open");
    checkChoice(problems, "defaults.missingMetadata", defaults.getMissingMetadata(), "inherit", "inherit-closed", "open",
      "closed");
    checkChoice(problems, "defaults.missingLagrum", defaults.getMissingLagrum(), "closed", "open");
    checkChoice(problems, "defaults.multipleLagrum", defaults.getMultipleLagrum(), "all", "any");
    if (defaults.getDefaultAct() == null || defaults.getDefaultAct().isBlank()) {
      problems.add("defaults.defaultAct saknas");
    }

    Set<String> nivaVocabulary = allowedNiva == null || allowedNiva.isEmpty() ? null
      : new HashSet<>(lowerAll(allowedNiva));
    Set<String> groupCatalog = knownGroups == null ? null : new HashSet<>(knownGroups);

    Set<String> seen = new HashSet<>();
    int index = 0;
    for (AccessRule rule : set.getRules()) {
      String label = "rules[" + index++ + "]";
      if (rule == null) {
        problems.add(label + " är tom");
        continue;
      }
      String group = rule.getGroup() == null ? null : rule.getGroup().trim();
      if (group == null || group.isEmpty()) {
        problems.add(label + ": group saknas");
      } else {
        label += " (" + group + ")";
        if (!seen.add(group)) {
          problems.add(label + ": gruppen har mer än en regel");
        }
        if (group.chars().anyMatch(Character::isWhitespace)) {
          problems.add(label + ": gruppnamnet innehåller blanksteg");
        } else if (groupCatalog != null && !groupCatalog.contains(group)) {
          problems.add(label + ": gruppen finns inte i katalogen (kontrollera stavning och skiftläge)");
        }
      }
      checkValues(problems, label + ".sekretess", rule.getSekretess());
      checkValues(problems, label + ".lagrum", rule.getLagrum());
      checkValues(problems, label + ".personuppgifter", rule.getPersonuppgifter());
      checkValues(problems, label + ".personuppgiftsniva", rule.getPersonuppgiftsniva());
      checkVocabulary(problems, label + ".sekretess", rule.getSekretess(), AccessRuleEngine.SEKRETESS_VALUES);
      checkVocabulary(problems, label + ".personuppgifter", rule.getPersonuppgifter(),
        AccessRuleEngine.PERSONUPPGIFTER_VALUES);
      if (nivaVocabulary != null) {
        checkVocabulary(problems, label + ".personuppgiftsniva", rule.getPersonuppgiftsniva(), nivaVocabulary);
      }
      for (String pattern : rule.getLagrum()) {
        if (pattern == null || pattern.isBlank()) {
          continue; // redan rapporterat
        }
        if (pattern.length() > MAX_PATTERN_LENGTH) {
          problems.add(label + ".lagrum: mönstret är längre än " + MAX_PATTERN_LENGTH + " tecken");
          continue;
        }
        try {
          LagrumPattern.parse(pattern, defaults.getDefaultAct());
        } catch (IllegalArgumentException e) {
          problems.add(label + ".lagrum: " + e.getMessage());
        }
      }
    }
    return problems;
  }

  /**
   * Tolkar regelfilens JSON och normaliserar värdena (se klassdokumentationen).
   * Okända fält (t.ex. "_example" i standardfilen) ignoreras; null-poster i
   * rules tas bort.
   *
   * @throws IOException
   *           vid tom text eller ogiltig JSON
   */
  public static AccessRuleSet parse(String json) throws IOException {
    if (json == null || json.isBlank()) {
      throw new IOException("Regelfilen är tom");
    }
    AccessRuleSet set = MAPPER.readValue(json, AccessRuleSet.class);
    if (set == null) {
      throw new IOException("Regelfilen innehåller ingen regeluppsättning");
    }
    set.getRules().removeIf(Objects::isNull);
    normalize(set);
    return set;
  }

  /** Serialiserar med indrag; null-fält utelämnas. */
  public static String toJson(AccessRuleSet set) {
    try {
      return MAPPER.writeValueAsString(Objects.requireNonNull(set, "set"));
    } catch (IOException e) {
      throw new IllegalStateException("Kunde inte serialisera regeluppsättningen", e);
    }
  }

  /** Djup, normaliserad kopia via JSON-rundtur. */
  public static AccessRuleSet copyOf(AccessRuleSet set) {
    try {
      return parse(toJson(set));
    } catch (IOException e) {
      throw new IllegalStateException("Kunde inte kopiera regeluppsättningen", e);
    }
  }

  /**
   * Normaliserar in place: policyval och vokabulärvärden trimmas och görs
   * till gemener, gruppnamn, lagrumsmönster och defaultAct trimmas. Tomma
   * värden lämnas (så att validering rapporterar dem).
   */
  static void normalize(AccessRuleSet set) {
    AccessRuleDefaults defaults = set.getDefaults();
    defaults.setEjBedomd(lower(defaults.getEjBedomd()));
    defaults.setMissingMetadata(lower(defaults.getMissingMetadata()));
    defaults.setMissingLagrum(lower(defaults.getMissingLagrum()));
    defaults.setMultipleLagrum(lower(defaults.getMultipleLagrum()));
    defaults.setDefaultAct(trim(defaults.getDefaultAct()));
    for (AccessRule rule : set.getRules()) {
      rule.setGroup(trim(rule.getGroup()));
      rule.setSekretess(lowerAll(rule.getSekretess()));
      rule.setLagrum(trimAll(rule.getLagrum()));
      rule.setPersonuppgifter(lowerAll(rule.getPersonuppgifter()));
      rule.setPersonuppgiftsniva(lowerAll(rule.getPersonuppgiftsniva()));
    }
  }

  /** Tidsstämpel nu i ISO-8601 UTC med sekundprecision, t.ex. 2026-10-08T12:34:56Z. */
  public static String nowIso() {
    return DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.SECONDS));
  }

  private AccessRuleSet loadFallback() {
    if (fallback != null) {
      try (InputStream in = fallback.get()) {
        if (in != null) {
          AccessRuleSet set = parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
          List<String> problems = validate(set, personuppgiftsnivaValues, null);
          if (!problems.isEmpty()) {
            throw new IOException("ogiltigt innehåll: " + String.join("; ", problems));
          }
          LOGGER.info("Regelfilen {} saknas – använder standardfilen ({} regler, enabled={})", file,
            set.getRules().size(), set.isEnabled());
          return set;
        }
      } catch (IOException | RuntimeException e) {
        LOGGER.error("Standardfilen för behörighetsregler kan inte användas: {}", e.getMessage());
      }
    }
    LOGGER.info("Regelfilen {} saknas – tom regeluppsättning (avstängd)", file);
    return emptyDisabled();
  }

  private void archiveCurrent(String user) throws IOException {
    Path dir = getHistoryDir();
    Files.createDirectories(dir);
    String base = HISTORY_STAMP.format(Instant.now()) + "-" + safeFileToken(user);
    Path target = dir.resolve(base + ".json");
    int n = 1;
    while (Files.exists(target)) {
      target = dir.resolve(base + "-" + (++n) + ".json");
    }
    Files.copy(file, target);
  }

  private static AccessRuleSet emptyDisabled() {
    AccessRuleSet set = new AccessRuleSet();
    set.setEnabled(false);
    return set;
  }

  private static ObjectMapper newMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    return mapper;
  }

  private static void checkChoice(List<String> problems, String key, String value, String... allowed) {
    String normalized = lower(value);
    if (normalized == null || !Arrays.asList(allowed).contains(normalized)) {
      problems.add(key + " måste vara något av " + Arrays.toString(allowed) + ", var '" + value + "'");
    }
  }

  private static void checkValues(List<String> problems, String key, List<String> values) {
    if (values == null) {
      return;
    }
    for (String value : values) {
      if (value == null || value.isBlank()) {
        problems.add(key + " innehåller ett tomt värde");
        return;
      }
    }
  }

  /** Alla icke-tomma värden måste finnas i vokabulären (gemener) eller vara "*". */
  private static void checkVocabulary(List<String> problems, String key, List<String> values,
    Set<String> vocabulary) {
    if (values == null) {
      return;
    }
    List<String> unknown = new ArrayList<>();
    for (String value : values) {
      String normalized = lower(value);
      if (normalized == null || normalized.isEmpty()) {
        continue; // tomt värde rapporteras av checkValues
      }
      if (!AccessRuleEngine.WILDCARD.equals(normalized) && !vocabulary.contains(normalized)) {
        unknown.add(value.trim());
      }
    }
    if (!unknown.isEmpty()) {
      List<String> sorted = new ArrayList<>(vocabulary);
      Collections.sort(sorted);
      problems.add(key + " innehåller okända värden " + unknown + " (tillåtna: " + sorted + " samt \"*\")");
    }
  }

  private static String trim(String value) {
    return value == null ? null : value.trim();
  }

  private static String lower(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? trimmed : trimmed.toLowerCase(Locale.ROOT);
  }

  private static List<String> lowerAll(Collection<String> values) {
    List<String> result = new ArrayList<>();
    if (values != null) {
      for (String value : values) {
        result.add(lower(value));
      }
    }
    return result;
  }

  private static List<String> trimAll(Collection<String> values) {
    List<String> result = new ArrayList<>();
    if (values != null) {
      for (String value : values) {
        result.add(trim(value));
      }
    }
    return result;
  }

  private static String safeFileToken(String user) {
    if (user == null || user.isBlank()) {
      return "unknown";
    }
    String token = user.trim().replaceAll("[^A-Za-z0-9._-]", "_");
    return token.length() > 64 ? token.substring(0, 64) : token;
  }
}

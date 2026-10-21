/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.common.access;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tolkar fritexten i eterna_admin/sekretessgrund till
 * strukturerade {@link Lagrum}. Fältet skrivs av handläggare och förekommer i
 * många varianter — parsern är därför tolerant mot skrivsätt men strikt i
 * resultatet: en del som inte kan tolkas ger INGET lagrum utan en varning, så
 * att behörighetsmotorn kan falla tillbaka på "stängt" i stället för att
 * gissa.
 *
 * <p>
 * Tolkade former (skiftlägesokänsligt, lagbeteckning valfri):
 * </p>
 *
 * <pre>
 * OSL 21 kap. 7 §        OSL 21 kap 7 §        21 kap. 7 §
 * OSL 21:7               21:7                  OSL 21 kap. 7 a §
 * OSL 21:7a              OSL 21 kap.           OSL 21
 * OSL 26 kap. 1 § och 21 kap. 7 §   (separatorer: , ; / "och" "samt")
 * OSL 21 kap. 1, 3 och 7 §§          (paragrafuppräkning: lag+kapitel ärvs
 *                                     från föregående del; ett naket tal
 *                                     efter ett helt kapitel, "OSL 21, 26",
 *                                     är tvetydigt och ger varning)
 * OSL (2009:400) 21 kap. 7 §         (SFS-nummer ignoreras)
 * OSL 21 kap. 7 § 1 st.              (stycke/punkt/mening ignoreras —
 *                                     precisionen är paragraf)
 * </pre>
 *
 * <p>
 * Lagbeteckning: en del utan lag ärver lagen från närmast föregående del i
 * samma text ("TF 2 kap. 1 § och 3 kap. 2 §" ⇒ TF 2:1, TF 3:2); saknas även
 * sådan används {@code defaultAct} (konfig core.access_rules.default_lag,
 * normalt OSL). Dubbletter avdubbleras med bevarad ordning. Tom/null text ger
 * tom lista utan varningar. Intervall ("1–3 §§") stöds inte och ger varning.
 * </p>
 */
public final class LagrumParser {

  /** Lag som används när varken texten eller anropet anger någon. */
  public static final String DEFAULT_ACT = "OSL";

  /** Blankstegsföljder (även unicode) som kollapsas till ett mellanslag före tolkningen. */
  private static final Pattern WHITESPACE = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);

  /**
   * Delare mellan flera lagrum i samma text. Alla mönster nedan använder
   * possessiva kvantifierare ({@code \\s*+}) och texten får sina
   * blankstegsföljder kollapsade innan den tolkas, så att en text med
   * tusentals blanksteg inte kan få regexmotorn att backtracka polynomiellt
   * (CPU-DoS via REST-API:ets normalisering och sparning).
   */
  private static final Pattern SEPARATOR = Pattern.compile("\\s*+(?:[,;/]|\\boch\\b|\\bsamt\\b)\\s*+",
    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS);

  /**
   * En enskild del. Grupper: lag (valfri, ev. med SFS-nummer i parentes), kap,
   * kapord ("kap."/"kapitel") eller kolon, par + suffix (valfria), § och en
   * ignorerad svans (stycke/punkt/mening).
   */
  private static final Pattern SEGMENT = Pattern.compile("^" // ----
    + "(?:(?<lag>\\p{L}[\\p{L}\\-]*+)(?:\\s*+\\(\\d{4}:\\d++\\))?\\s++)?" // lag + ev. (2009:400)
    + "(?<kap>\\d++)" // kapitel
    + "(?:\\s*+(?<kapord>kap(?:itlet|itel)?\\.?)|(?<kolon>:))?" // "kap." eller ":"
    + "(?:\\s*+(?<par>\\d++)\\s*+(?<suffix>\\p{L})?\\s*+§{0,2})?" // paragraf, suffix, §
    + "(?<svans>(?:\\s*+(?:\\d++|första|andra|tredje|fjärde|femte|sjätte)\\s*+" // 1 st., 2 p.
    + "(?:st|stycket|stycke|p|punkten|punkt|men|meningen)\\b\\.?)*)" // ----
    + "\\s*+$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS);

  /**
   * Enbart paragraf ("8 §", "8 a §§", "3") — i uppräkningar som
   * "21 kap. 1, 3 och 7 §§"; lag och kapitel ärvs från föregående del. Se
   * {@link #parseParagraphOnly} för när ett tal utan § får tolkas så.
   */
  private static final Pattern PARAGRAPH_ONLY = Pattern.compile(
    "^(?<par>\\d++)\\s*+(?<suffix>\\p{L})?\\s*+"
      + "(?<tecken>§{1,2})?(?:\\s*+(?:\\d++|första|andra|tredje|fjärde|femte|sjätte)\\s*+"
      + "(?:st|stycket|stycke|p|punkten|punkt|men|meningen)\\b\\.?)*\\s*+$",
    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS);

  /** Lagbeteckning som i själva verket är ordet "kap." — avvisas. */
  private static final Pattern KAP_WORD = Pattern.compile("KAP(?:ITEL|ITLET)?\\.?");

  private LagrumParser() {
    // statisk verktygsklass
  }

  /** Resultat av en tolkning: lagrum i textordning samt varningar. */
  public static final class ParseResult {
    private final List<Lagrum> lagrum;
    private final List<String> warnings;

    ParseResult(List<Lagrum> lagrum, List<String> warnings) {
      this.lagrum = Collections.unmodifiableList(new ArrayList<>(lagrum));
      this.warnings = Collections.unmodifiableList(new ArrayList<>(warnings));
    }

    /** Tolkade lagrum, avdubblerade, i textordning (oföränderlig lista). */
    public List<Lagrum> getLagrum() {
      return lagrum;
    }

    /** En varning per otolkbar del (oföränderlig lista). */
    public List<String> getWarnings() {
      return warnings;
    }

    public boolean hasWarnings() {
      return !warnings.isEmpty();
    }

    @Override
    public String toString() {
      return "ParseResult{lagrum=" + lagrum + ", warnings=" + warnings + "}";
    }
  }

  /**
   * Tolkar fritext till lagrum.
   *
   * @param text
   *          fritext, t.ex. "OSL 26 kap. 1 § och 21 kap. 7 §"; null/tom ger tomt
   *          resultat
   * @param defaultAct
   *          lag för delar utan lagbeteckning (null/tom ⇒ {@link #DEFAULT_ACT})
   */
  public static ParseResult parse(String text, String defaultAct) {
    Set<Lagrum> found = new LinkedHashSet<>();
    List<String> warnings = new ArrayList<>();
    if (text == null || text.isBlank()) {
      return new ParseResult(new ArrayList<>(found), warnings);
    }

    String currentLag = normalizeLag(defaultAct);
    if (currentLag == null) {
      currentLag = DEFAULT_ACT;
    }
    Lagrum previous = null;

    // blankstegsföljder kollapsas: likvärdigt för tolkningen och tar bort
    // det enda sättet att få separator- och delmönstren att backtracka
    String collapsed = WHITESPACE.matcher(text).replaceAll(" ").strip();
    for (String rawSegment : SEPARATOR.split(collapsed)) {
      String segment = rawSegment.trim();
      if (segment.isEmpty()) {
        continue;
      }
      Lagrum lagrum = parseSegment(segment, currentLag, previous);
      if (lagrum == null) {
        warnings.add("Otolkbart lagrum: \"" + segment + "\"");
      } else {
        currentLag = lagrum.getLag();
        previous = lagrum;
        found.add(lagrum);
      }
    }
    return new ParseResult(new ArrayList<>(found), warnings);
  }

  /**
   * Tolkar en enskild del (utan separatorer). Returnerar null om delen inte
   * kan tolkas entydigt.
   *
   * @param fallbackLag
   *          lag när delen saknar lagbeteckning
   * @param previous
   *          närmast föregående tolkade lagrum i samma text (null om inget) —
   *          ger lag och kapitel åt en del som bara anger paragraf ("8 §")
   */
  static Lagrum parseSegment(String segment, String fallbackLag, Lagrum previous) {
    Matcher m = SEGMENT.matcher(segment);
    if (!m.matches()) {
      return parseParagraphOnly(segment, previous);
    }
    String lag = normalizeLag(m.group("lag"));
    if (lag != null && KAP_WORD.matcher(lag).matches()) {
      // "kap 21" — ordet kap. är inte en lag
      return null;
    }
    boolean hasKapord = m.group("kapord") != null;
    boolean hasKolon = m.group("kolon") != null;
    String par = m.group("par");
    String suffix = m.group("suffix");
    String svans = m.group("svans");

    if (par == null) {
      // kapitelform: kräver "kap." eller en uttrycklig lag ("OSL 21");
      // "OSL 21:" är inte entydigt
      if (hasKolon || suffix != null || (svans != null && !svans.isEmpty())) {
        return null;
      }
      if (!hasKapord && lag == null) {
        // naket tal ("3"): bara tolkbart som paragraf i en uppräkning
        return parseParagraphOnly(segment, previous);
      }
    } else if (!hasKapord && !hasKolon) {
      // "OSL 21 7 §" — saknar både "kap." och kolon
      return null;
    }

    try {
      int kapitel = Integer.parseInt(m.group("kap"));
      Integer paragraf = par == null ? null : Integer.valueOf(par);
      return new Lagrum(lag == null ? fallbackLag : lag, kapitel, paragraf, suffix);
    } catch (IllegalArgumentException e) {
      // för stora tal, kapitel 0 osv. — behandlas som otolkbart
      return null;
    }
  }

  /**
   * Paragraf som ärver lag och kapitel från föregående del i samma text.
   * Med §-tecken ("8 §") räcker det att en föregående del finns; utan tecken
   * ("3" i "21 kap. 1, 3 och 7 §§") krävs dessutom att föregående del själv
   * är en paragraf — efter ett helt kapitel ("OSL 21, 26") är ett naket tal
   * tvetydigt (kapitel 26 eller 21:26?) och avvisas.
   */
  private static Lagrum parseParagraphOnly(String segment, Lagrum previous) {
    if (previous == null) {
      return null;
    }
    Matcher m = PARAGRAPH_ONLY.matcher(segment);
    if (!m.matches()) {
      return null;
    }
    if (m.group("tecken") == null && previous.isWholeChapter()) {
      return null;
    }
    try {
      return new Lagrum(previous.getLag(), previous.getKapitel(), Integer.valueOf(m.group("par")), m.group("suffix"));
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private static String normalizeLag(String lag) {
    if (lag == null) {
      return null;
    }
    String trimmed = lag.trim();
    return trimmed.isEmpty() ? null : trimmed.toUpperCase(Locale.ROOT);
  }
}

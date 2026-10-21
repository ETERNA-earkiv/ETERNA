/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.common.access;

import java.io.Serializable;
import java.util.Objects;

/**
 * Ett lagrumsmönster i en behörighetsregel
 * (access-rules.json, {@code rules[].lagrum[]}). Tre former:
 *
 * <ul>
 * <li>{@code "*"} — täcker alla lagrum (och även objekt helt utan tolkbart
 * lagrum; se motorn).</li>
 * <li>{@code "OSL 21"} / {@code "OSL 21 kap."} — hela kapitlet: täcker varje
 * paragraf i kapitlet samt lagrum som själva avser hela kapitlet.</li>
 * <li>{@code "OSL 21:7"} / {@code "OSL 21 kap. 7 §"} — exakt paragraf,
 * inklusive suffix ("OSL 21:7a" täcker inte "OSL 21:7" och vice versa). Ett
 * lagrum som avser hela kapitlet täcks INTE av ett paragrafmönster (det är
 * bredare än mönstret).</li>
 * </ul>
 *
 * Lagbeteckning utelämnad ⇒ {@code defaultAct}. Otolkbart mönster ger
 * {@link IllegalArgumentException} så att en felskriven regel upptäcks när
 * den sparas/laddas i stället för att tyst släppa igenom eller stänga ute.
 */
public final class LagrumPattern implements Serializable {

  private static final long serialVersionUID = 1L;

  public static final String WILDCARD = "*";

  private static final LagrumPattern WILDCARD_PATTERN = new LagrumPattern(null);

  /** null = jokertecken. */
  private final Lagrum lagrum;

  private LagrumPattern(Lagrum lagrum) {
    this.lagrum = lagrum;
  }

  /** Mönstret "*". */
  public static LagrumPattern wildcard() {
    return WILDCARD_PATTERN;
  }

  /** Mönster som exakt motsvarar ett lagrum (kapitel eller paragraf). */
  public static LagrumPattern of(Lagrum lagrum) {
    if (lagrum == null) {
      throw new IllegalArgumentException("Lagrum saknas");
    }
    return new LagrumPattern(lagrum);
  }

  /**
   * Tolkar ett mönster från regelfilen.
   *
   * @throws IllegalArgumentException
   *           om mönstret är tomt, innehåller flera lagrum eller inte kan
   *           tolkas
   */
  public static LagrumPattern parse(String pattern, String defaultAct) {
    if (pattern == null || pattern.isBlank()) {
      throw new IllegalArgumentException("Tomt lagrumsmönster");
    }
    String trimmed = pattern.trim();
    if (WILDCARD.equals(trimmed)) {
      return WILDCARD_PATTERN;
    }
    LagrumParser.ParseResult result = LagrumParser.parse(trimmed, defaultAct);
    if (result.hasWarnings() || result.getLagrum().size() != 1) {
      throw new IllegalArgumentException("Otolkbart lagrumsmönster: \"" + trimmed
        + "\" (förväntat \"*\", \"OSL 21\", \"OSL 21:7\" eller \"OSL 21 kap. 7 §\")");
    }
    return new LagrumPattern(result.getLagrum().get(0));
  }

  public boolean isWildcard() {
    return lagrum == null;
  }

  /** Mönstrets lagrum; null för jokertecknet. */
  public Lagrum getLagrum() {
    return lagrum;
  }

  /**
   * true om mönstret täcker lagrummet. Jokertecknet täcker allt (även null);
   * övriga mönster kräver samma lag och kapitel, och ett paragrafmönster
   * dessutom exakt samma paragraf och suffix.
   */
  public boolean covers(Lagrum candidate) {
    if (lagrum == null) {
      return true;
    }
    if (candidate == null) {
      return false;
    }
    if (!lagrum.getLag().equals(candidate.getLag()) || lagrum.getKapitel() != candidate.getKapitel()) {
      return false;
    }
    if (lagrum.isWholeChapter()) {
      return true;
    }
    return Objects.equals(lagrum.getParagraf(), candidate.getParagraf())
      && Objects.equals(lagrum.getSuffix(), candidate.getSuffix());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof LagrumPattern)) {
      return false;
    }
    return Objects.equals(lagrum, ((LagrumPattern) o).lagrum);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(lagrum);
  }

  /** Normaliserad form: "*", "OSL 21", "OSL 21:7" eller "OSL 21:7a". */
  @Override
  public String toString() {
    return lagrum == null ? WILDCARD : lagrum.toString();
  }
}

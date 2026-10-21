/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.common.access;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;

/**
 * Ett lagrum — lag, kapitel och valfri paragraf med
 * bokstavssuffix, t.ex. "OSL 21:7a". Oföränderlig värdeklass.
 *
 * <ul>
 * <li>Lagens beteckning lagras alltid versalt så att "osl" och "OSL" är samma
 * lag.</li>
 * <li>{@code paragraf == null} betyder hela kapitlet ("OSL 21").</li>
 * <li>Suffix lagras gement och kräver paragraf ("OSL 21:7a").</li>
 * </ul>
 *
 * Normaliserad textform via {@link #toString()}: "OSL 21", "OSL 21:7",
 * "OSL 21:7a".
 */
public final class Lagrum implements Serializable, Comparable<Lagrum> {

  private static final long serialVersionUID = 1L;

  private static final Comparator<Lagrum> ORDER = Comparator.comparing(Lagrum::getLag)
    .thenComparingInt(Lagrum::getKapitel)
    .thenComparing(Lagrum::getParagraf, Comparator.nullsFirst(Comparator.naturalOrder()))
    .thenComparing(Lagrum::getSuffix, Comparator.nullsFirst(Comparator.naturalOrder()));

  private final String lag;
  private final int kapitel;
  private final Integer paragraf;
  private final String suffix;

  /**
   * @param lag
   *          lagens beteckning, t.ex. "OSL" (lagras versalt)
   * @param kapitel
   *          kapitelnummer (≥ 1)
   * @param paragraf
   *          paragrafnummer (≥ 1) eller null för hela kapitlet
   * @param suffix
   *          bokstavssuffix, t.ex. "a" för "7 a §", eller null
   * @throws IllegalArgumentException
   *           vid tom lag, kapitel/paragraf &lt; 1 eller suffix utan paragraf
   */
  public Lagrum(String lag, int kapitel, Integer paragraf, String suffix) {
    if (lag == null || lag.isBlank()) {
      throw new IllegalArgumentException("Lagrum: lag saknas");
    }
    if (kapitel < 1) {
      throw new IllegalArgumentException("Lagrum: kapitel måste vara minst 1, var " + kapitel);
    }
    if (paragraf != null && paragraf < 1) {
      throw new IllegalArgumentException("Lagrum: paragraf måste vara minst 1, var " + paragraf);
    }
    String normalizedSuffix = suffix == null ? null : suffix.trim().toLowerCase(Locale.ROOT);
    if (normalizedSuffix != null && normalizedSuffix.isEmpty()) {
      normalizedSuffix = null;
    }
    if (normalizedSuffix != null && paragraf == null) {
      throw new IllegalArgumentException("Lagrum: suffix '" + suffix + "' kräver paragraf");
    }
    this.lag = lag.trim().toUpperCase(Locale.ROOT);
    this.kapitel = kapitel;
    this.paragraf = paragraf;
    this.suffix = normalizedSuffix;
  }

  /** Hela kapitlet, t.ex. "OSL 21". */
  public Lagrum(String lag, int kapitel) {
    this(lag, kapitel, null, null);
  }

  /** Paragraf utan suffix, t.ex. "OSL 21:7". */
  public Lagrum(String lag, int kapitel, int paragraf) {
    this(lag, kapitel, paragraf, null);
  }

  public String getLag() {
    return lag;
  }

  public int getKapitel() {
    return kapitel;
  }

  /** Paragrafnummer; null betyder hela kapitlet. */
  public Integer getParagraf() {
    return paragraf;
  }

  /** Bokstavssuffix (gement), t.ex. "a"; null om inget. */
  public String getSuffix() {
    return suffix;
  }

  /** true om lagrummet avser hela kapitlet (ingen paragraf). */
  public boolean isWholeChapter() {
    return paragraf == null;
  }

  @Override
  public int compareTo(Lagrum other) {
    return ORDER.compare(this, other);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Lagrum)) {
      return false;
    }
    Lagrum other = (Lagrum) o;
    return kapitel == other.kapitel && lag.equals(other.lag) && Objects.equals(paragraf, other.paragraf)
      && Objects.equals(suffix, other.suffix);
  }

  @Override
  public int hashCode() {
    return Objects.hash(lag, kapitel, paragraf, suffix);
  }

  /** Normaliserad form: "OSL 21", "OSL 21:7" eller "OSL 21:7a". */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(lag).append(' ').append(kapitel);
    if (paragraf != null) {
      sb.append(':').append(paragraf);
      if (suffix != null) {
        sb.append(suffix);
      }
    }
    return sb.toString();
  }
}

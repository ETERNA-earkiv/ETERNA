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
import java.util.List;

/**
 * Läsbara texter för ett {@link ApplyResult} –
 * används i jobbrapporter, CSV-rapporten och PREMIS-händelser av både
 * batchpluginet och {@link AccessRulesModelObserver}, så att spårbarheten
 * ser likadan ut oavsett vem som tillämpade reglerna.
 */
public final class ApplyResultTexts {

  /** Beskrivning av PREMIS-händelsen (UPDATE) när reglerna ändrar behörigheter. */
  public static final String EVENT_DESCRIPTION = "Tillämpning av åtkomstregler (sekretess/personuppgifter) "
    + "på behörigheter";
  /** Avskiljare mellan flera värden i en cell/rad. */
  public static final String MULTI_VALUE_SEPARATOR = " | ";

  private ApplyResultTexts() {
    // statisk hjälpklass
  }

  /**
   * Detaljtext: faktakälla, fakta, motivering per grupp, ändrade behörigheter,
   * varningar och utfall.
   */
  public static String describe(ApplyResult result, boolean dryRun) {
    EternaAdminFacts facts = result.getFacts();
    StringBuilder sb = new StringBuilder();
    sb.append("Faktakälla: ").append(factsSourceText(facts));
    sb.append("\nSekretess: ").append(valueOrDash(facts == null ? null : facts.getSekretess()));
    sb.append("\nLagrum: ").append(valueOrDash(join(lagrumTexts(facts))));
    sb.append("\nPersonuppgifter: ").append(valueOrDash(facts == null ? null : facts.getPersonuppgifter()));
    sb.append("\nPersonuppgiftsnivå: ").append(valueOrDash(facts == null ? null : facts.getPersonuppgiftsniva()));

    AccessDecision decision = result.getDecision();
    if (decision != null && !decision.getExplanation().isEmpty()) {
      sb.append("\nBeslut:");
      for (String line : decision.getExplanation()) {
        sb.append("\n  - ").append(line);
      }
    }

    MergeResult merge = result.getMerge();
    if (merge != null) {
      appendIfNotEmpty(sb, "READ tillagd", merge.getAdded());
      appendIfNotEmpty(sb, "READ borttagen", merge.getRemoved());
      appendIfNotEmpty(sb, "Skrivbehörigheter borttagna", merge.getWriteStripped());
      appendIfNotEmpty(sb, "Skrivbehörigheter återställda", merge.getWriteRestored());
    }
    appendIfNotEmpty(sb, "Varningar", warnings(result));
    sb.append("\nUtfall: ").append(outcomeText(result, dryRun));
    return sb.toString();
  }

  /** "egen", "ärvd från &lt;aipId&gt;" eller "saknas". */
  public static String factsSourceText(EternaAdminFacts facts) {
    if (facts == null || !facts.isPresent()) {
      return "saknas";
    }
    if (facts.isInherited()) {
      return "ärvd från " + facts.getSourceAipId();
    }
    return "egen";
  }

  /** Utfallet i en rad: skrivet, skulle skrivas (torrkörning) eller oförändrad. */
  public static String outcomeText(ApplyResult result, boolean dryRun) {
    if (result.isWritten()) {
      return result.isStateOnly() ? "regeltillstånd skrivet (behörigheter oförändrade)" : "behörigheter uppdaterade";
    }
    if (result.isWriteNeeded()) {
      String what = result.isStateOnly() ? "regeltillstånd skulle skrivas" : "behörigheter skulle ändras";
      return dryRun ? what + " (torrkörning)" : what;
    }
    return "oförändrad";
  }

  /** Lagrummen som text, i ordning. */
  public static List<String> lagrumTexts(EternaAdminFacts facts) {
    List<String> texts = new ArrayList<>();
    if (facts != null) {
      for (Lagrum lagrum : facts.getLagrum()) {
        if (lagrum != null) {
          texts.add(lagrum.toString());
        }
      }
    }
    return texts;
  }

  /** Varningar från fakta och sammanslagning. */
  public static List<String> warnings(ApplyResult result) {
    List<String> warnings = new ArrayList<>();
    if (result.getFacts() != null) {
      warnings.addAll(result.getFacts().getWarnings());
    }
    if (result.getMerge() != null) {
      warnings.addAll(result.getMerge().getWarnings());
    }
    return warnings;
  }

  /** Värdena sammanfogade med {@value #MULTI_VALUE_SEPARATOR}; tom sträng för null. */
  public static String join(Collection<String> values) {
    return values == null ? "" : String.join(MULTI_VALUE_SEPARATOR, values);
  }

  private static void appendIfNotEmpty(StringBuilder sb, String label, Collection<String> values) {
    if (values != null && !values.isEmpty()) {
      sb.append('\n').append(label).append(": ").append(join(values));
    }
  }

  private static String valueOrDash(String value) {
    return value == null || value.isBlank() ? "-" : value;
  }
}

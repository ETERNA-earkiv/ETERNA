/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.client.management.accessrules;

import java.util.ArrayList;
import java.util.List;

import org.roda.core.data.v2.accessrules.AccessRule;
import org.roda.core.data.v2.accessrules.AccessRulesVocabulary;
import org.roda.wui.common.client.tools.Humanize;
import org.roda.wui.common.client.tools.StringUtils;

import com.google.gwt.core.client.GWT;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.InlineLabel;

import config.i18n.client.ClientMessages;

/**
 * Gemensam presentation av åtkomstregler i
 * klienten (etiketter ur vokabulären, datum och sammanfattning av en regel).
 */
public final class AccessRulesFormat {
  private static final ClientMessages messages = GWT.create(ClientMessages.class);

  /** Jokertecknet som tillåter alla värden i en regel. */
  public static final String ALL = "*";

  /** ISO-8601 med sekundprecision och Z, som registret och statusen skriver. */
  private static final RegExp ISO_SECONDS = RegExp.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$");

  private AccessRulesFormat() {
    // do nothing
  }

  /**
   * Etiketter för en värdelista: "*" visas som "Alla (*)", en tom lista som
   * "Inga" och övriga värden via vokabulärens etiketter.
   */
  public static String labels(List<String> values, AccessRulesVocabulary vocabulary) {
    if (values == null || values.isEmpty()) {
      return messages.accessRuleNone();
    }
    List<String> labels = new ArrayList<>();
    for (String value : values) {
      if (ALL.equals(value)) {
        labels.add(messages.accessRuleAll());
      } else {
        labels.add(vocabulary != null ? vocabulary.labelOf(value) : value);
      }
    }
    return StringUtils.join(labels, ", ");
  }

  /** Lagrum visas som de är (normaliserade mönster), "*" som "Alla (*)". */
  public static String lagrum(List<String> values) {
    if (values == null || values.isEmpty()) {
      return messages.accessRuleNone();
    }
    List<String> out = new ArrayList<>();
    for (String value : values) {
      out.add(ALL.equals(value) ? messages.accessRuleAll() : value);
    }
    return StringUtils.join(out, ", ");
  }

  public static String enabled(boolean enabled) {
    return enabled ? messages.accessRuleEnabledYes() : messages.accessRuleEnabledNo();
  }

  /**
   * Formaterar en ISO-8601-tidpunkt från servern med användarens datumformat.
   * Servern skriver sekundprecision med Z; klientens JSON-format kräver
   * millisekunder och numerisk zon, så strängen anpassas före tolkningen.
   * Kan strängen inte tolkas visas den oförändrad.
   */
  public static String dateTime(String iso) {
    if (iso == null || iso.isEmpty()) {
      return "";
    }
    String normalized = iso;
    if (ISO_SECONDS.test(normalized)) {
      normalized = normalized.substring(0, normalized.length() - 1) + ".000+0000";
    } else if (normalized.endsWith("Z")) {
      normalized = normalized.substring(0, normalized.length() - 1) + "+0000";
    }
    try {
      return Humanize.formatDateTime(normalized);
    } catch (IllegalArgumentException e) {
      return iso;
    }
  }

  /** Punktlista som sammanfattar en regel (används på gruppsidan). */
  public static FlowPanel summary(AccessRule rule, AccessRulesVocabulary vocabulary) {
    FlowPanel panel = new FlowPanel();
    panel.add(line(messages.accessRuleEnabled(), enabled(rule.isEnabled())));
    panel.add(line(messages.accessRuleSekretess(), labels(rule.getSekretess(), vocabulary)));
    panel.add(line(messages.accessRuleLagrum(), lagrum(rule.getLagrum())));
    panel.add(line(messages.accessRulePersonuppgifter(), labels(rule.getPersonuppgifter(), vocabulary)));
    panel.add(line(messages.accessRulePersonuppgiftsniva(), labels(rule.getPersonuppgiftsniva(), vocabulary)));
    if (rule.getNote() != null && !rule.getNote().isEmpty()) {
      panel.add(line(messages.accessRuleNote(), rule.getNote()));
    }
    return panel;
  }

  private static FlowPanel line(String label, String value) {
    FlowPanel panel = new FlowPanel();
    InlineHTML bullet = new InlineHTML("&#8226;");
    bullet.addStyleName("bullet");
    panel.add(bullet);
    panel.add(new InlineLabel(label + ": " + value));
    return panel;
  }
}

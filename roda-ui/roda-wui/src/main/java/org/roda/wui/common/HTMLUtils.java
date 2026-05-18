/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.roda.core.RodaCoreFactory;
import org.roda.core.common.Messages;
import org.roda.core.common.RodaUtils;
import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.exceptions.GenericException;
import org.roda.core.storage.Binary;

import com.google.common.io.CharStreams;

/**
 * HTML related utility class
 *
 * @author Hélder Silva <hsilva@keep.pt>
 * @author Luis Faria <lfaria@keep.pt>
 * @author Sébastien Leroux <sleroux@keep.pt>
 */
public final class HTMLUtils {

  // XSS defense-in-depth for XSLT-rendered HTML.
  //
  // The toHtml() methods below feed their return value into trusted HTML
  // sinks on the client side, where the browser bypasses normal escaping:
  //
  //   - descriptive/technical metadata + preservation event → wrapped by
  //     SafeHtmlUtils.fromTrustedString(...) and inserted into the DOM
  //     (AIPDescriptiveMetadataTabs, FileTechnicalMetadataTabs,
  //      ShowPreservationEvent)
  //   - representation file → returned over REST and assigned to an
  //     iframe srcdoc attribute (BitstreamPreview.loadXsltPreview)
  //
  // Both sinks render the string as HTML without further escaping. XSLT
  // input is derived from user-controllable artifacts (SIPs, metadata
  // files, custom uploaded stylesheets), so the rendered string must be
  // sanitized server-side to prevent stored-XSS via crafted XML/XSLT.
  //
  // The policy below is intentionally permissive enough to render existing
  // descriptive/technical/preservation metadata crosswalks unchanged
  // (tables, lists, inline formatting, anchors, images, inline styles).
  // If a future crosswalk needs an element/attribute not on this list,
  // extend the policy here — do NOT bypass sanitization at call sites.
  private static final PolicyFactory HTML_SANITIZER = new HtmlPolicyBuilder()
    .allowCommonBlockElements()
    .allowCommonInlineFormattingElements()
    .allowElements("table", "thead", "tbody", "tfoot", "tr", "th", "td", "caption", "col", "colgroup")
    .allowElements("dl", "dt", "dd", "pre", "code", "hr", "a", "img", "section", "header", "footer", "nav", "main")
    .allowAttributes("class", "id", "style", "title", "lang").globally()
    .allowAttributes("href").onElements("a")
    .allowAttributes("src", "alt", "width", "height").onElements("img")
    .allowAttributes("colspan", "rowspan", "scope", "align", "valign").onElements("th", "td")
    .allowUrlProtocols("http", "https", "data")
    .allowStyling()
    .toFactory();

  private static String sanitizeHtml(String html) {
    return HTML_SANITIZER.sanitize(html);
  }

  // Cache the preview stylesheet contents on first use. The config file is
  // looked up via RODA's configuration mechanism so deployments can override
  // it by placing a file at $RODA_HOME/config/theme/xslt-preview.css.
  private static final AtomicReference<String> CACHED_XSLT_PREVIEW_CSS = new AtomicReference<>();

  private static String loadXsltPreviewCss() {
    String cached = CACHED_XSLT_PREVIEW_CSS.get();
    if (cached != null) {
      return cached;
    }
    String content = "";
    try (InputStream is = RodaCoreFactory.getConfigurationFileAsStream("theme/xslt-preview.css")) {
      if (is != null) {
        try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
          content = CharStreams.toString(reader);
        }
      }
    } catch (IOException e) {
      // Fall back to no styling — the iframe will render unstyled HTML.
      // Logging would require a logger import; the empty fallback is safe.
      content = "";
    }
    CACHED_XSLT_PREVIEW_CSS.compareAndSet(null, content);
    return content;
  }

  /**
   * Wraps an XSLT-rendered HTML fragment in a complete HTML document with an inline
   * stylesheet, so the result renders with ETERNA-style typography when assigned to
   * an iframe srcdoc attribute. Default XSLT crosswalks emit a bare fragment
   * (e.g. {@code <div class="descriptiveMetadata">…</div>}) — without this wrapper
   * the iframe shows unstyled HTML because srcdoc is isolated from the parent page.
   */
  static String wrapXsltHtml(String renderedFragment) {
    String css = loadXsltPreviewCss();
    StringBuilder b = new StringBuilder(renderedFragment.length() + css.length() + 256);
    b.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">");
    if (!css.isEmpty()) {
      b.append("<style>").append(css).append("</style>");
    }
    b.append("</head><body>").append(renderedFragment).append("</body></html>");
    return b.toString();
  }

  /** Private empty constructor */
  private HTMLUtils() {
    // do nothing
  }

  public static String descriptiveMetadataToHtml(Binary binary, String metadataType, String metadataVersion,
    final Locale locale) throws GenericException {
    Map<String, String> translations = getTranslations(metadataType, metadataVersion, locale);
    Reader reader = RodaUtils.applyMetadataStylesheet(binary, RodaConstants.CROSSWALKS_DISSEMINATION_HTML_PATH,
      metadataType, metadataVersion, translations);
    try {
      return sanitizeHtml(CharStreams.toString(reader));
    } catch (IOException e) {
      throw new GenericException("Could not transform PREMIS to HTML", e);
    }
  }

  public static String technicalMetadataToHtml(Binary binary, String metadataType, String metadataVersion,
    final Locale locale) throws GenericException {
    Map<String, String> translations = getTranslations(metadataType, metadataVersion, locale);

    String lowerCaseMetadataTypeWithVersion;

    if (metadataVersion != null) {
      lowerCaseMetadataTypeWithVersion = metadataType.toLowerCase() + RodaConstants.METADATA_VERSION_SEPARATOR
        + metadataVersion;
    } else {
      lowerCaseMetadataTypeWithVersion = metadataType.toLowerCase();
    }

    Reader reader;
    if ((RodaCoreFactory.getConfigurationFileAsStream(
      RodaConstants.CROSSWALKS_DISSEMINATION_HTML_PATH + lowerCaseMetadataTypeWithVersion + ".xslt")) == null) {
      reader = RodaUtils.applyMetadataStylesheet(binary, RodaConstants.CROSSWALKS_DISSEMINATION_HTML_PATH, "plain",
        null, translations);
    }
    else {
      reader = RodaUtils.applyMetadataStylesheet(binary, RodaConstants.CROSSWALKS_DISSEMINATION_HTML_PATH, metadataType,
        metadataVersion, translations);
    }
    try {
      return sanitizeHtml(CharStreams.toString(reader));
    } catch (IOException e) {
      throw new GenericException("Could not transform PREMIS to HTML", e);
    }
  }

  public static String preservationMetadataEventToHtml(Binary binary, boolean onlyDetails, final Locale locale)
    throws GenericException {

    Map<String, String> translations = getEventTranslations(locale);

    Reader reader = RodaUtils.applyEventStylesheet(binary, onlyDetails, translations,
      RodaConstants.CROSSWALKS_DISSEMINATION_HTML_EVENT_PATH);

    try {
      return sanitizeHtml(CharStreams.toString(reader));
    } catch (IOException e) {
      throw new GenericException("Could not transform PREMIS to HTML", e);
    }
  }

  public static Map<String, String>   getTranslations(String descriptiveMetadataType, String descriptiveMetadataVersion,
    final Locale locale) {
    Map<String, String> translations = null;
    Messages messages = RodaCoreFactory.getI18NMessages(locale);
    if (descriptiveMetadataType != null) {
      String lowerCaseDescriptiveMetadataType = descriptiveMetadataType.toLowerCase();
      if (descriptiveMetadataVersion != null) {
        String lowerCaseDescriptiveMetadataTypeWithVersion = lowerCaseDescriptiveMetadataType
          + RodaConstants.METADATA_VERSION_SEPARATOR + descriptiveMetadataVersion;
        translations = messages.getTranslations(
          RodaConstants.I18N_CROSSWALKS_DISSEMINATION_HTML_PREFIX + lowerCaseDescriptiveMetadataTypeWithVersion,
          String.class, true);
      }

      if (translations == null || translations.isEmpty()) {
        translations = messages.getTranslations(
          RodaConstants.I18N_CROSSWALKS_DISSEMINATION_HTML_PREFIX + lowerCaseDescriptiveMetadataType, String.class,
          true);
      }

    } else {
      translations = new HashMap<>();
    }
    return translations;
  }

  public static Map<String, String> getEventTranslations(final Locale locale) {
    Messages messages = RodaCoreFactory.getI18NMessages(locale);
    return messages.getTranslations(RodaConstants.I18N_CROSSWALKS_DISSEMINATION_HTML_PREFIX + "event", String.class,
      true);
  }


  public static String representationFileToHtml(Binary binary, String xsltName, final Locale locale)
    throws GenericException {
    Map<String, String> translations = getTranslations(xsltName, null, locale);
    Reader reader = RodaUtils.applyMetadataStylesheet(binary,
      RodaConstants.CROSSWALKS_DISSEMINATION_HTML_REPRESENTATION_PATH, xsltName, null, translations);
    try {
      return wrapXsltHtml(sanitizeHtml(CharStreams.toString(reader)));
    } catch (IOException e) {
      throw new GenericException("Could not transform representation file to HTML", e);
    }
  }

  public static String representationFileToHtmlWithCustomXslt(Binary binary, InputStream xsltInputStream,
    final Locale locale) throws GenericException {
    Map<String, String> translations = new HashMap<>();
    Reader reader = RodaUtils.applyCustomStylesheet(binary, xsltInputStream, translations);
    try {
      return wrapXsltHtml(sanitizeHtml(CharStreams.toString(reader)));
    } catch (IOException e) {
      throw new GenericException("Could not transform representation file with custom XSLT", e);
    }
  }

}

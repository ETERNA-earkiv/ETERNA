/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.api.v2.services;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Locale;

import org.roda.core.RodaCoreFactory;
import org.roda.core.common.Messages;
import org.roda.core.common.permissions.PermissionOverviewBuilder;
import org.roda.core.common.permissions.PermissionOverviewCsvRenderer;
import org.roda.core.common.permissions.PermissionOverviewHtmlRenderer;
import org.roda.core.common.permissions.PermissionOverviewMessages;
import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.v2.ConsumesOutputStream;
import org.roda.core.data.v2.DefaultConsumesOutputStream;
import org.roda.core.data.v2.StreamResponse;
import org.roda.core.data.v2.user.PermissionOverview;
import org.roda.wui.common.model.RequestContext;
import org.roda.wui.common.server.ServerTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Tjänst för behörighetsöversikten – bygger
 * {@link PermissionOverview} ur anropets modell/index (anroparens
 * READ-filter) och renderar den som HTML eller CSV med serverns
 * översättningar. Språket styr även byggarens varningstexter, så att JSON,
 * HTML och CSV är konsekventa.
 */
@Service
public class PermissionOverviewService {

  private static final Logger LOGGER = LoggerFactory.getLogger(PermissionOverviewService.class);

  public static final String HTML_FILE_NAME = "behorighetsoversikt.html";
  public static final String CSV_FILE_NAME = "behorighetsoversikt.csv";
  public static final String MEDIA_TYPE_HTML_UTF8 = RodaConstants.MEDIA_TYPE_TEXT_HTML + "; charset=UTF-8";
  public static final String MEDIA_TYPE_CSV_UTF8 = "text/csv; charset=UTF-8";
  /** Samma avgränsarkonfiguration som SearchExportPlugin. */
  public static final String CSV_DELIMITER_KEY = "csv.delimiter";

  public PermissionOverview buildOverview(RequestContext requestContext, String localeString) {
    return PermissionOverviewBuilder.build(requestContext.getModelService(), requestContext.getIndexService(),
      requestContext.getUser(), messages(localeString));
  }

  public StreamResponse renderHtml(PermissionOverview overview, String localeString) {
    String html = PermissionOverviewHtmlRenderer.render(overview, messages(localeString));
    return streamResponse(HTML_FILE_NAME, MEDIA_TYPE_HTML_UTF8, html);
  }

  public StreamResponse renderCsv(PermissionOverview overview, String localeString) {
    char delimiter = PermissionOverviewCsvRenderer
      .delimiterOf(RodaCoreFactory.getRodaConfiguration().getString(CSV_DELIMITER_KEY));
    String csv = PermissionOverviewCsvRenderer.render(overview, messages(localeString), delimiter);
    return streamResponse(CSV_FILE_NAME, MEDIA_TYPE_CSV_UTF8, csv);
  }

  /** Serverns meddelanden för språket (tomt ⇒ sv_SE); saknas de används fallback-texterna. */
  PermissionOverviewMessages messages(String localeString) {
    Messages messages = null;
    try {
      Locale locale = ServerTools.parseLocale(localeString);
      messages = locale != null ? RodaCoreFactory.getI18NMessages(locale) : null;
    } catch (RuntimeException e) {
      LOGGER.warn("Kunde inte läsa meddelanden för språket '{}' – använder standardtexter", localeString, e);
    }
    return PermissionOverviewMessages.of(messages);
  }

  private static StreamResponse streamResponse(String fileName, String mediaType, String content) {
    byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
    ConsumesOutputStream stream = new DefaultConsumesOutputStream(fileName, mediaType, new Date(), bytes.length,
      out -> {
        try {
          out.write(bytes);
          out.flush();
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      });
    return new StreamResponse(stream);
  }
}

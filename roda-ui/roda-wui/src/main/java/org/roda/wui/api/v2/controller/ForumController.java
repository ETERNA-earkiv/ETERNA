/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/keeps/roda
 */
package org.roda.wui.api.v2.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Proxy endpoint that fetches the latest discussions from the ETERNA forum
 * (eterna.whitered.se) and returns a simplified JSON list to the browser.
 * A server-side proxy is needed because the forum does not send CORS headers.
 */
@RestController
@RequestMapping("/api/v2/forum")
public class ForumController {

  private static final Logger LOGGER = LoggerFactory.getLogger(ForumController.class);
  private static final String DISCUSSIONS_URL =
    "https://eterna.whitered.se/api/discussions?sort=-lastPostedAt&page[limit]=5";
  private static final String FORUM_DISCUSSION_BASE = "https://eterna.whitered.se/d/";
  private static final long CACHE_TTL_MS = 300_000L; // 5 minutes

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final HttpClient httpClient = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(5))
    .build();

  private volatile List<Map<String, Object>> cachedResult = null;
  private volatile long cacheExpiry = 0;

  @GetMapping("/latest")
  public List<Map<String, Object>> latestDiscussions() {
    long now = System.currentTimeMillis();
    if (cachedResult != null && now < cacheExpiry) {
      return cachedResult;
    }
    try {
      HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(DISCUSSIONS_URL))
        .header("Accept", "application/json")
        .timeout(Duration.ofSeconds(8))
        .GET()
        .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      JsonNode root = objectMapper.readTree(response.body());
      JsonNode data = root.get("data");
      if (data == null || !data.isArray()) {
        return Collections.emptyList();
      }

      List<Map<String, Object>> result = new ArrayList<>();
      for (JsonNode item : data) {
        JsonNode attrs = item.get("attributes");
        if (attrs == null) continue;
        Map<String, Object> post = new HashMap<>();
        post.put("title", attrs.path("title").asText());
        post.put("url", FORUM_DISCUSSION_BASE + attrs.path("slug").asText());
        post.put("lastPostedAt", attrs.path("lastPostedAt").asText());
        post.put("commentCount", attrs.path("commentCount").asInt());
        result.add(post);
      }

      cachedResult = result;
      cacheExpiry = now + CACHE_TTL_MS;
      return result;
    } catch (Exception e) {
      LOGGER.warn("Could not fetch forum discussions", e);
      return cachedResult != null ? cachedResult : Collections.emptyList();
    }
  }
}

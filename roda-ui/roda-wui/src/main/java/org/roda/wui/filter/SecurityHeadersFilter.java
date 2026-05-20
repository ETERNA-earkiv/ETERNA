/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.configuration2.Configuration;
import org.roda.core.RodaCoreFactory;
import org.roda.core.common.RodaUtils;

import java.io.IOException;
import java.util.*;

public class SecurityHeadersFilter implements Filter {

  private static final String REPLAY_SW_PATH = "/replay/sw.js";
  private static final String REPLAY_SW_ALLOWED_SCOPE = "/replay/";

  private Boolean contentSecurityPolicyEnabled = true;
  private String contentSecurityPolicy;
  private String replayContentSecurityPolicy;
  private String replayPathPrefix;
  private String replayViewerPath;

  @Override
  public void init(final FilterConfig filterConfig) throws ServletException {
    final String configPrefix = filterConfig.getInitParameter("config-prefix");

    Configuration configuration  = RodaCoreFactory.getRodaConfiguration();
    contentSecurityPolicyEnabled = configuration.getBoolean(configPrefix + ".csp.enabled", true);

    if (contentSecurityPolicyEnabled) {
      List<String> cspDirectives = RodaUtils.copyList(configuration.getList(configPrefix + ".csp.directives[]"));

      if (!cspDirectives.isEmpty()) {
        contentSecurityPolicy = String.join("; ", cspDirectives) + ";";
      }

      List<String> replayCspDirectives = RodaUtils.copyList(
        configuration.getList(configPrefix + ".csp.replay.directives[]"));

      if (!replayCspDirectives.isEmpty()) {
        replayContentSecurityPolicy = String.join("; ", replayCspDirectives) + ";";
      }
    }

    replayPathPrefix = configuration.getString(
      configPrefix + ".csp.replay.path-prefix", "/replay/");
    replayViewerPath = configuration.getString(
      configPrefix + ".csp.replay.viewer-path", "/replay-viewer.html");
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    HttpServletResponse httpServletResponse = (HttpServletResponse) response;
    HttpServletRequest httpServletRequest = (HttpServletRequest) request;
    String requestPath = getRequestPath(httpServletRequest);

    httpServletResponse.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

    if (contentSecurityPolicyEnabled) {
      String effectiveCsp = pickContentSecurityPolicy(requestPath);
      if (effectiveCsp != null) {
        httpServletResponse.setHeader("Content-Security-Policy", effectiveCsp);
      }
    }

    if (REPLAY_SW_PATH.equals(requestPath)) {
      // Bound the replay service worker registration to /replay/ even if a future
      // change moves the SW file or a wider scope is attempted from elsewhere.
      httpServletResponse.setHeader("Service-Worker-Allowed", REPLAY_SW_ALLOWED_SCOPE);
    }

    httpServletResponse.setHeader("X-XSS-Protection", "1; mode=block");
    httpServletResponse.setHeader("X-Permitted-Cross-Domain-Policies", "none");
    httpServletResponse.setHeader("Feature-Policy", "camera 'none'; fullscreen 'self'; geolocation *; " + "microphone 'self'");
    httpServletResponse.setHeader("X-Frame-Options", "SAMEORIGIN");
    httpServletResponse.setHeader("X-Content-Type-Options", "nosniff");
    httpServletResponse.setHeader("Referrer-Policy", "no-referrer");
    httpServletResponse.setHeader("Permissions-Policy", "geolocation=(self)");

    chain.doFilter(request, response);
  }

  private String getRequestPath(HttpServletRequest request) {
    String contextPath = request.getContextPath();
    String uri = request.getRequestURI();
    if (uri == null) {
      return "";
    }
    if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
      return uri.substring(contextPath.length());
    }
    return uri;
  }

  private String pickContentSecurityPolicy(String requestPath) {
    if (replayContentSecurityPolicy != null && requestPath != null
        && (requestPath.startsWith(replayPathPrefix) || requestPath.equals(replayViewerPath))) {
      return replayContentSecurityPolicy;
    }
    return contentSecurityPolicy;
  }

  @Override
  public void destroy() {
  }
}
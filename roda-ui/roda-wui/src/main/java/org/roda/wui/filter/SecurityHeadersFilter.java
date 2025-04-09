/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 * <p>
 * https://github.com/keeps/roda
 */
package org.roda.wui.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.configuration.Configuration;
import org.roda.core.RodaCoreFactory;
import org.roda.core.common.RodaUtils;

import java.io.IOException;
import java.util.*;

public class SecurityHeadersFilter implements Filter {

  private Boolean contentSecurityPolicyEnabled = true;
  private String contentSecurityPolicy;

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
    }
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    HttpServletResponse httpServletResponse = (HttpServletResponse) response;

    httpServletResponse.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

    if (contentSecurityPolicyEnabled && contentSecurityPolicy != null) {
      httpServletResponse.setHeader("Content-Security-Policy", contentSecurityPolicy);
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

  @Override
  public void destroy() {
  }
}
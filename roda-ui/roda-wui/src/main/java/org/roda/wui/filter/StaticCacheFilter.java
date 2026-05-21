/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Sets appropriate Cache-Control headers for static resources.
 *
 * HTML entry points and GWT bootstrap files (.nocache.js) get no-store so
 * browsers always fetch a fresh copy after a deploy. Content-hashed files
 * (.cache.js, webjars) get immutable so they are cached indefinitely.
 */
public class StaticCacheFilter implements Filter {

  private static final String NO_STORE = "no-store";
  private static final String IMMUTABLE = "public, max-age=31536000, immutable";

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;
    String path = resolvePath(httpRequest);

    if (isNeverCache(path)) {
      httpResponse.setHeader("Cache-Control", NO_STORE);
    } else if (isImmutable(path)) {
      httpResponse.setHeader("Cache-Control", IMMUTABLE);
    }

    chain.doFilter(request, response);
  }

  private String resolvePath(HttpServletRequest request) {
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

  // HTML shells and GWT bootstrap must never be served from cache after a deploy
  private boolean isNeverCache(String path) {
    return "/".equals(path) || path.endsWith(".html") || path.endsWith(".nocache.js");
  }

  // Content-hashed files are safe to cache indefinitely
  private boolean isImmutable(String path) {
    return path.endsWith(".cache.js") || path.startsWith("/webjars/");
  }

  @Override
  public void destroy() {
  }
}

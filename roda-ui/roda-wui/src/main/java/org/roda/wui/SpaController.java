/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Forwards all non-API, non-static-resource routes to the SPA's index.html so
 * that React Router can handle client-side navigation.
 *
 * <p>Exclusions handled by Spring's static resource handler and the API prefix filter:
 * <ul>
 *   <li>/api/v2/** — handled by Jersey/REST controllers
 *   <li>/assets/**, /webjars/**, and paths with a file extension — served as static files
 * </ul>
 */
@Controller
@RequestMapping
public class SpaController {

  /**
   * Catch-all for top-level paths with no file extension.
   * Covers routes like /login, /browse, /ingest, /jobs, /audit, /search, etc.
   */
  @GetMapping("/{path:[^\\.]*}")
  public String spaRoot() {
    return "forward:/index.html";
  }

  /**
   * Catch-all for one level of nesting, e.g. /browse/some-id, /management/users.
   */
  @GetMapping("/{segment1:[^\\.]*}/{path:[^\\.]*}")
  public String spaOneLevel() {
    return "forward:/index.html";
  }

  /**
   * Catch-all for two levels of nesting, e.g. /browse/:aipId/representation/:repId.
   */
  @GetMapping("/{segment1:[^\\.]*}/{segment2:[^\\.]*}/{path:[^\\.]*}")
  public String spaTwoLevels() {
    return "forward:/index.html";
  }

  /**
   * Catch-all for three levels of nesting, e.g. /browse/:aipId/representation/:repId/file/:id.
   */
  @GetMapping("/{segment1:[^\\.]*}/{segment2:[^\\.]*}/{segment3:[^\\.]*}/{path:[^\\.]*}")
  public String spaThreeLevels() {
    return "forward:/index.html";
  }
}

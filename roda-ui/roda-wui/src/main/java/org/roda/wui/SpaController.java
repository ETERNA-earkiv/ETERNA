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

/**
 * Forwards all non-API, non-static-resource routes to the SPA's index.html so
 * that React Router can handle client-side navigation.
 */
@Controller
public class SpaController {

  /**
   * Catch-all for paths without a file extension that are not under /api. Returns
   * the SPA entry point so the React router takes over.
   */
  @GetMapping(value = {"/", "/login", "/browse", "/browse/{id:[^.]+}", "/management/users",
    "/management/users/{path:[^.]+}"})
  public String index() {
    return "forward:/index.html";
  }
}

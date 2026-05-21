/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.api.v2.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import io.swagger.v3.oas.annotations.Operation;

/**
 * @author Miguel Guimarães <mguimaraes@keep.pt>
 */
public interface Exportable {

  @PostMapping(path = "/export/csv", produces = "text/csv", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  @Operation(summary = "Exports a set of finite elements to CSV", description = "", responses = {})
  ResponseEntity<StreamingResponseBody> exportToCSV(@RequestParam("findRequest") String findRequest);
}

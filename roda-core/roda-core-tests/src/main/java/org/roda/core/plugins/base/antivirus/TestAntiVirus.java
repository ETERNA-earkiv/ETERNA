/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree.
 */
package org.roda.core.plugins.base.antivirus;

import java.nio.file.Path;

public class TestAntiVirus implements AntiVirus {

  @Override
  public VirusCheckResult checkForVirus(Path path) {
    VirusCheckResult result = new VirusCheckResult();
    result.setClean(true);
    result.setReport("Test antivirus: clean");
    return result;
  }

  @Override
  public String getVersion() {
    return "test-antivirus";
  }
}

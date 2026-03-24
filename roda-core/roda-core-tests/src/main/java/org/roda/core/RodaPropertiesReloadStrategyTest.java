/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/keeps/roda
 */
package org.roda.core;

import static org.mockito.Mockito.mockStatic;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.mockito.MockedStatic;
import org.roda.core.data.common.RodaConstants;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link RodaPropertiesReloadStrategy}.
 *
 * <p>Verifies that the file-change watcher correctly reloads the shared
 * {@link PropertiesConfiguration} without ever exposing an empty or partial
 * config to concurrent readers.
 */
@Test(groups = {RodaConstants.TEST_GROUP_ALL, RodaConstants.TEST_GROUP_DEV, RodaConstants.TEST_GROUP_TRAVIS})
public class RodaPropertiesReloadStrategyTest {

  /** Polling interval used by all tests. */
  private static final long POLL_MS = 50;

  /**
   * How long to wait after modifying a file before asserting config state.
   * Must be at least 2× POLL_MS to guarantee at least one poll cycle fires.
   */
  private static final long WAIT_MS = 400;

  @Test
  public void reloadsConfigWhenFileChanges() throws Exception {
    File f = createTempPropertiesFile("key1=initial\n");
    PropertiesConfiguration config = new PropertiesConfiguration();
    config.setProperty("key1", "initial");

    try (MockedStatic<RodaCoreFactory> mock = mockStatic(RodaCoreFactory.class)) {
      new RodaPropertiesReloadStrategy().watch(config, f, POLL_MS);
      Thread.sleep(POLL_MS * 3); // let baseline poll establish lastModified

      rewriteFile(f, "key1=updated\n");
      Thread.sleep(WAIT_MS);

      assertEquals(config.getString("key1"), "updated");
    }
  }

  @Test
  public void removesStaleKeysAfterReload() throws Exception {
    File f = createTempPropertiesFile("key1=v1\nkey2=v2\n");
    PropertiesConfiguration config = new PropertiesConfiguration();
    config.setProperty("key1", "v1");
    config.setProperty("key2", "v2");

    try (MockedStatic<RodaCoreFactory> mock = mockStatic(RodaCoreFactory.class)) {
      new RodaPropertiesReloadStrategy().watch(config, f, POLL_MS);
      Thread.sleep(POLL_MS * 3);

      rewriteFile(f, "key1=v1updated\n"); // key2 intentionally absent
      Thread.sleep(WAIT_MS);

      assertEquals(config.getString("key1"), "v1updated");
      assertNull(config.getString("key2"), "key2 should be absent after reload");
    }
  }

  @Test
  public void configPreservedWhenFileBecomesUnreadable() throws Exception {
    File f = createTempPropertiesFile("key1=original\n");
    PropertiesConfiguration config = new PropertiesConfiguration();
    config.setProperty("key1", "original");

    // clearRodaCachableObjectsAfterConfigurationChange is never reached when
    // load fails, so no mock is needed for this test.
    new RodaPropertiesReloadStrategy().watch(config, f, POLL_MS);
    Thread.sleep(POLL_MS * 3);

    // Remove read permission so fh.load(file) throws ConfigurationException.
    // config.clear() must NOT have been called before the failed load.
    f.setReadable(false);
    // Touch mtime so the watcher detects a "change".
    f.setLastModified(System.currentTimeMillis() + 2000);
    Thread.sleep(WAIT_MS);

    assertEquals(config.getString("key1"), "original",
      "Config must be unchanged when file load fails");

    f.setReadable(true); // restore so deleteOnExit can clean up
  }

  @Test
  public void noReloadWhenFileNotModified() throws Exception {
    File f = createTempPropertiesFile("key1=stable\n");
    PropertiesConfiguration config = new PropertiesConfiguration();
    config.setProperty("key1", "stable");

    try (MockedStatic<RodaCoreFactory> mock = mockStatic(RodaCoreFactory.class)) {
      new RodaPropertiesReloadStrategy().watch(config, f, POLL_MS);
      Thread.sleep(WAIT_MS); // several poll cycles, no file change

      assertEquals(config.getString("key1"), "stable");
      mock.verifyNoInteractions(); // clearRodaCachableObjectsAfterConfigurationChange never called
    }
  }

  // ── helpers ──────────────────────────────────────────────────────────────────

  private File createTempPropertiesFile(String content) throws Exception {
    File f = Files.createTempFile("roda-reload-test-", ".properties").toFile();
    f.deleteOnExit();
    rewriteFile(f, content);
    return f;
  }

  private void rewriteFile(File f, String content) throws Exception {
    try (FileWriter w = new FileWriter(f)) {
      w.write(content);
    }
  }
}

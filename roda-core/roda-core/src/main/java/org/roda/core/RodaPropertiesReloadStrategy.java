/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/keeps/roda
 */
package org.roda.core;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.io.FileHandler;
import org.roda.core.data.common.RodaConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Watches a properties file for changes and reloads it in-place when modified,
 * then clears RODA's configuration caches. Replaces the commons-configuration
 * 1.x {@code FileChangedReloadingStrategy} which no longer exists in 2.x.
 */
public class RodaPropertiesReloadStrategy {

  private static final Logger LOGGER = LoggerFactory.getLogger(RodaPropertiesReloadStrategy.class);

  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
    Thread t = new Thread(r, "roda-config-reload-watcher");
    t.setDaemon(true);
    return t;
  });

  /**
   * Starts watching {@code file} for modifications. When a change is detected
   * the configuration is reloaded in-place and
   * {@link RodaCoreFactory#clearRodaCachableObjectsAfterConfigurationChange()}
   * is called.
   *
   * @param config
   *          the live {@link PropertiesConfiguration} instance to reload
   * @param file
   *          the backing file to watch
   * @param refreshDelayMs
   *          polling interval in milliseconds
   */
  public void watch(PropertiesConfiguration config, File file, long refreshDelayMs) {
    final long[] lastModified = {file.lastModified()};
    scheduler.scheduleWithFixedDelay(() -> {
      long current = file.lastModified();
      if (current != lastModified[0]) {
        lastModified[0] = current;
        try {
          config.clear();
          FileHandler fh = new FileHandler(config);
          fh.setEncoding(RodaConstants.DEFAULT_ENCODING);
          fh.load(file);
          RodaCoreFactory.clearRodaCachableObjectsAfterConfigurationChange();
        } catch (ConfigurationException e) {
          LOGGER.error("Error reloading configuration from {}", file, e);
        }
      }
    }, refreshDelayMs, refreshDelayMs, TimeUnit.MILLISECONDS);
  }

}

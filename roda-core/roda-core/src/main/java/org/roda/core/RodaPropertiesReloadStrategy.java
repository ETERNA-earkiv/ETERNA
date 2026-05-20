/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

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

  private final Object reloadLock = new Object();

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
    // Prefer the timestamp from the config's own file handler (captured at load
    // time) so a change between load and watch() is not silently dropped.
    FileHandler configFh = new FileHandler(config);
    long seed = configFh.getFile() != null ? configFh.getFile().lastModified() : file.lastModified();
    final AtomicLong lastModified = new AtomicLong(seed);
    scheduler.scheduleWithFixedDelay(() -> {
      long current = file.lastModified();
      if (current != lastModified.get()) {
        try {
          PropertiesConfiguration tmp = new PropertiesConfiguration();
          FileHandler fh = new FileHandler(tmp);
          fh.setEncoding(RodaConstants.DEFAULT_ENCODING);
          fh.load(file);
          synchronized (reloadLock) {
            config.clear();
            tmp.getKeys().forEachRemaining(key -> config.setProperty(key, tmp.getProperty(key)));
            RodaCoreFactory.clearRodaCachableObjectsAfterConfigurationChange();
          }
          // Only advance the baseline after a successful reload so transient
          // load/parse errors do not permanently suppress future retries.
          lastModified.set(current);
        } catch (ConfigurationException e) {
          LOGGER.error("Error reloading configuration from {}", file, e);
        }
      }
    }, refreshDelayMs, refreshDelayMs, TimeUnit.MILLISECONDS);
  }

}

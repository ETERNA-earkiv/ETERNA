/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.common.pekko.messages.plugins;

import org.roda.core.common.pekko.messages.AbstractMessage;
import org.roda.core.data.v2.IsRODAObject;
import org.roda.core.plugins.Plugin;

import java.io.Serial;

/**
 * @author Miguel Guimarães <mguimaraes@keep.pt>
 */
public class PluginMethodIsReady<T extends IsRODAObject> extends AbstractMessage {
  @Serial
  private static final long serialVersionUID = -5214600055070295410L;

  private Plugin<T> plugin;

  public PluginMethodIsReady(Plugin<T> plugin) {
    super();
    this.plugin = plugin;
  }

  public Plugin<T> getPlugin() {
    return plugin;
  }

  @Override
  public String toString() {
    return "PluginMethodIsReady [plugin=" + plugin + "]";
  }
}

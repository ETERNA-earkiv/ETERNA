/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.client.common.utils;

import java.util.List;

import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.v2.index.filter.Filter;
import org.roda.core.data.v2.index.filter.OneOfManyFilterParameter;
import org.roda.wui.common.client.tools.ConfigurationManager;

/**
 * Begränsar trädfrågor till de beskrivningsnivåer som listas i
 * {@code ui.tree.levels}. Tom eller saknad lista betyder att alla nivåer visas.
 */
public final class TreeLevelFilter {

  private TreeLevelFilter() {
    // utility class
  }

  /**
   * Lägger till nivåfiltret på {@code filter} (om nivåer är konfigurerade) och
   * returnerar samma filter.
   */
  public static Filter apply(Filter filter) {
    List<String> levels = ConfigurationManager.getStringList(RodaConstants.UI_TREE_LEVELS);
    if (!levels.isEmpty()) {
      filter.add(new OneOfManyFilterParameter(RodaConstants.AIP_LEVEL, levels));
    }
    return filter;
  }
}

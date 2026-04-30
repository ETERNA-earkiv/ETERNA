/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/keeps/roda
 */
package org.roda.core.plugins;

import java.util.Locale;
import java.util.ResourceBundle.Control;

/**
 * @author Miguel Guimarães <mguimaraes@keep.pt>
 */
public class PluginMessagesControl extends Control {

  public PluginMessagesControl() {
    // empty constructor
  }

  @Override
  public Locale getFallbackLocale(String baseName, Locale locale) {
    if (baseName == null) {
      throw new NullPointerException();
    }
    Locale defaultLocale = Locale.of("sv", "SE");
    return locale.equals(defaultLocale) ? null : defaultLocale;
  }
}

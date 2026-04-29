/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.common.pekko.messages.jobs;

import org.roda.core.common.pekko.messages.AbstractMessage;

import java.io.Serial;

/**
 * @author Miguel Guimarães <mguimaraes@keep.pt>
 */
public class JobsManagerTick extends AbstractMessage {
  @Serial
  private static final long serialVersionUID = -2514581679498648676L;

  public JobsManagerTick() {
    super();
  }

  @Override
  public String toString() {
    return "JobManagerTick []";
  }
}

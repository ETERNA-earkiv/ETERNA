/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.data.exceptions;

import java.io.Serial;

/**
 * @author Hélder Silva <hsilva@keep.pt>
 *
 */
public class JobAlreadyStartedException extends JobException {

  @Serial
  private static final long serialVersionUID = -6744205569453461540L;

  public JobAlreadyStartedException() {
    super();
  }

  public JobAlreadyStartedException(String message) {
    super(message);
  }

  public JobAlreadyStartedException(String message, Throwable cause) {
    super(message, cause);
  }

  public JobAlreadyStartedException(Throwable cause) {
    super(cause);
  }

}

/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.data.v2.generics;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author Miguel Guimarães <mguimaraes@keep.pt>
 */
public class LongResponse implements Serializable {

  @Serial
  private static final long serialVersionUID = 5965085690339865049L;

  private Long result;

  public LongResponse() {
    // empty constructor
  }

  public LongResponse(Long result) {
    this.result = result;
  }

  public Long getResult() {
    return result;
  }

  public void setResult(Long result) {
    this.result = result;
  }
}

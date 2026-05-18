/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.data.v2.ri;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

import org.roda.core.data.v2.generics.MetadataValue;

/**
 * @author Miguel Guimarães <mguimaraes@keep.pt>
 */
public class RepresentationInformationCustomForm implements Serializable {
  @Serial
  private static final long serialVersionUID = 1768436615696712964L;

  private Set<MetadataValue> values;

  public RepresentationInformationCustomForm() {
    super();
  }

  public RepresentationInformationCustomForm(Set<MetadataValue> values) {
    super();
    this.values = values;
  }

  public Set<MetadataValue> getValues() {
    return values;
  }

  public void setValues(Set<MetadataValue> values) {
    this.values = values;
  }

  @Override
  public String toString() {
    return "RepresentationInformationCustomForm{" +
        "values=" + values +
        '}';
  }
}

/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.common.deliveryspec;

import java.io.Serializable;
import java.util.Objects;

/**
 * En leveransspecifikation är ett konfigurationspaket —
 * ingen ny domänentitet. Denna klass är den lästa registerposten från
 * core.deliveryspec.*-nycklarna: bindningen till metadatatyp/-version (och
 * därmed XSD, formulärmall och crosswalks via befintliga uppslagsvägar),
 * rot-namespace för auto-detektering samt valfri Schematron (båda ännu
 * inte implementerade i valideringen).
 */
public final class DeliverySpec implements Serializable {

  private static final long serialVersionUID = 1L;

  private final String id;
  private final String metadataType;
  private final String metadataVersion;
  private final String namespace;
  private final boolean requireMetadata;
  private final String schematron;

  public DeliverySpec(String id, String metadataType, String metadataVersion, String namespace,
    boolean requireMetadata, String schematron) {
    this.id = id;
    this.metadataType = metadataType;
    this.metadataVersion = metadataVersion;
    this.namespace = namespace;
    this.requireMetadata = requireMetadata;
    this.schematron = schematron;
  }

  public String getId() {
    return id;
  }

  public String getMetadataType() {
    return metadataType;
  }

  /** Metadataversion — kan vara null för oversionerade typer. */
  public String getMetadataVersion() {
    return metadataVersion;
  }

  /** Rot-namespace för auto-detektering; kan vara null. */
  public String getNamespace() {
    return namespace;
  }

  /** Kräv att metadata av specens typ finns — annars valideringsfel. */
  public boolean isRequireMetadata() {
    return requireMetadata;
  }

  /** Sökväg till förkompilerad Schematron-XSLT; kan vara null. */
  public String getSchematron() {
    return schematron;
  }

  /** Metadatatypens fulla id, t.ex. "cits_erms_3" eller "fgs_arende_2.0". */
  public String getMetadataTypeWithVersion() {
    return metadataVersion == null || metadataVersion.isBlank() ? metadataType
      : metadataType + "_" + metadataVersion;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DeliverySpec)) {
      return false;
    }
    return Objects.equals(id, ((DeliverySpec) o).id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }

  @Override
  public String toString() {
    return "DeliverySpec [id=" + id + ", metadataType=" + metadataType + ", metadataVersion=" + metadataVersion
      + ", namespace=" + namespace + ", requireMetadata=" + requireMetadata + ", schematron=" + schematron + "]";
  }
}

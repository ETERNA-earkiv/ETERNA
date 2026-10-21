/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.common.deliveryspec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

import org.roda.core.RodaCoreFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Register över leveransspecifikationer, läst från
 * konfigurationen (drop-in-leveransbart i drift via RODA_HOME/config):
 *
 * <pre>
 * core.deliveryspec = fgs_arende_2.0
 * core.deliveryspec.fgs_arende_2.0.metadataType = fgs_arende
 * core.deliveryspec.fgs_arende_2.0.metadataVersion = 2.0
 * core.deliveryspec.fgs_arende_2.0.namespace = &lt;target-namespace&gt;
 * core.deliveryspec.fgs_arende_2.0.requireMetadata = true
 * core.deliveryspec.fgs_arende_2.0.schematron = deliveryspecs/fgs_arende_2.0/rules.xslt
 * </pre>
 *
 * Namnkonvention: metadatatypens fulla id splittas på SISTA understrecket
 * (typ_version) — versionen får därför aldrig innehålla "_"; använd punkter
 * (2.0, inte 2_0). Ogiltiga poster hoppas över med varning i loggen så att
 * en felkonfigurerad spec aldrig hindrar uppstart eller övriga specar.
 */
public final class DeliverySpecRegistry {

  private static final Logger LOGGER = LoggerFactory.getLogger(DeliverySpecRegistry.class);

  private static final String CONFIG_PREFIX = "core.deliveryspec";

  private DeliverySpecRegistry() {
    // statisk verktygsklass
  }

  /** Läser registret ur RODA-konfigurationen. */
  public static List<DeliverySpec> load() {
    List<String> ids = RodaCoreFactory.getRodaConfigurationAsList(CONFIG_PREFIX);
    return parse(ids, key -> RodaCoreFactory.getRodaConfigurationAsString(key));
  }

  public static Optional<DeliverySpec> getSpec(String id) {
    return load().stream().filter(spec -> spec.getId().equals(id)).findFirst();
  }

  public static List<String> getSpecIds() {
    return load().stream().map(DeliverySpec::getId).toList();
  }

  /**
   * Parsar registerposter. Separerad från konfigurationskällan för
   * enhetstestbarhet: {@code propertyResolver} slår upp en full
   * konfigurationsnyckel till dess strängvärde (null om saknas).
   */
  public static List<DeliverySpec> parse(List<String> ids, UnaryOperator<String> propertyResolver) {
    List<DeliverySpec> specs = new ArrayList<>();
    if (ids == null) {
      return specs;
    }
    // LinkedHashMap avdublonnerar med bevarad ordning
    Map<String, Boolean> seen = new LinkedHashMap<>();
    for (String rawId : ids) {
      String id = rawId == null ? "" : rawId.trim();
      if (id.isEmpty() || seen.containsKey(id)) {
        continue;
      }
      seen.put(id, Boolean.TRUE);

      String type = trimToNull(propertyResolver.apply(CONFIG_PREFIX + "." + id + ".metadataType"));
      String version = trimToNull(propertyResolver.apply(CONFIG_PREFIX + "." + id + ".metadataVersion"));
      String namespace = trimToNull(propertyResolver.apply(CONFIG_PREFIX + "." + id + ".namespace"));
      String requireMetadataRaw = trimToNull(propertyResolver.apply(CONFIG_PREFIX + "." + id + ".requireMetadata"));
      String schematron = trimToNull(propertyResolver.apply(CONFIG_PREFIX + "." + id + ".schematron"));

      if (type == null) {
        LOGGER.warn("Delivery spec '{}' is missing mandatory key {}.{}.metadataType — skipping entry", id,
          CONFIG_PREFIX, id);
        continue;
      }
      if (version != null && version.contains("_")) {
        // typ/version splittas på sista "_" i resten av systemet — en version
        // med understreck ger fel typuppslag och avvisas därför här
        LOGGER.warn("Delivery spec '{}' has metadataVersion '{}' containing '_' — versions must use dots "
          + "(e.g. 2.0), skipping entry", id, version);
        continue;
      }

      boolean requireMetadata = Boolean.parseBoolean(requireMetadataRaw);
      specs.add(new DeliverySpec(id, type, version, namespace, requireMetadata, schematron));
    }
    return specs;
  }

  private static String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}

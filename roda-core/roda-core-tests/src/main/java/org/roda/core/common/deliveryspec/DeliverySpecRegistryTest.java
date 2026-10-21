/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.common.deliveryspec;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.roda.core.data.common.RodaConstants;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Enhetstester för leveransspec-registrets parsning —
 * giltiga poster, namnkonventionen (version utan understreck), saknade
 * obligatoriska nycklar, dubbletter och standardvärden.
 */
@Test(groups = {RodaConstants.TEST_GROUP_ALL, RodaConstants.TEST_GROUP_DEV, RodaConstants.TEST_GROUP_TRAVIS})
public class DeliverySpecRegistryTest {

  private static Map<String, String> baseProperties() {
    Map<String, String> props = new HashMap<>();
    props.put("core.deliveryspec.fgs_arende_2.0.metadataType", "fgs_arende");
    props.put("core.deliveryspec.fgs_arende_2.0.metadataVersion", "2.0");
    props.put("core.deliveryspec.fgs_arende_2.0.namespace", "http://www.riksarkivet.se/schemas/fgs");
    props.put("core.deliveryspec.fgs_arende_2.0.requireMetadata", "true");
    props.put("core.deliveryspec.fgs_arende_2.0.schematron", "deliveryspecs/fgs_arende_2.0/rules.xslt");
    props.put("core.deliveryspec.cits_erms_3.metadataType", "cits_erms");
    props.put("core.deliveryspec.cits_erms_3.metadataVersion", "3");
    props.put("core.deliveryspec.cits_erms_3.namespace", "https://DILCIS.eu/XML/ERMS");
    props.put("core.deliveryspec.cits_erms_3.requireMetadata", "true");
    return props;
  }

  @Test
  public void testParseValidSpecs() {
    Map<String, String> props = baseProperties();
    List<DeliverySpec> specs = DeliverySpecRegistry.parse(Arrays.asList("fgs_arende_2.0", "cits_erms_3"), props::get);

    Assert.assertEquals(specs.size(), 2);

    DeliverySpec fgs = specs.get(0);
    Assert.assertEquals(fgs.getId(), "fgs_arende_2.0");
    Assert.assertEquals(fgs.getMetadataType(), "fgs_arende");
    Assert.assertEquals(fgs.getMetadataVersion(), "2.0");
    Assert.assertEquals(fgs.getMetadataTypeWithVersion(), "fgs_arende_2.0");
    Assert.assertTrue(fgs.isRequireMetadata());
    Assert.assertEquals(fgs.getSchematron(), "deliveryspecs/fgs_arende_2.0/rules.xslt");

    DeliverySpec erms = specs.get(1);
    Assert.assertEquals(erms.getMetadataTypeWithVersion(), "cits_erms_3");
    Assert.assertNull(erms.getSchematron());
  }

  @Test
  public void testVersionWithUnderscoreIsRejected() {
    Map<String, String> props = baseProperties();
    props.put("core.deliveryspec.trasig_1_0.metadataType", "trasig");
    props.put("core.deliveryspec.trasig_1_0.metadataVersion", "1_0");
    List<DeliverySpec> specs = DeliverySpecRegistry.parse(Arrays.asList("trasig_1_0", "cits_erms_3"), props::get);

    // posten med understreck i versionen hoppas över, övriga läses
    Assert.assertEquals(specs.size(), 1);
    Assert.assertEquals(specs.get(0).getId(), "cits_erms_3");
  }

  @Test
  public void testMissingMetadataTypeIsRejected() {
    Map<String, String> props = baseProperties();
    List<DeliverySpec> specs = DeliverySpecRegistry.parse(Arrays.asList("okand_spec", "cits_erms_3"), props::get);
    Assert.assertEquals(specs.size(), 1);
    Assert.assertEquals(specs.get(0).getId(), "cits_erms_3");
  }

  @Test
  public void testDuplicatesAndBlanksAreIgnored() {
    Map<String, String> props = baseProperties();
    List<DeliverySpec> specs = DeliverySpecRegistry
      .parse(Arrays.asList("cits_erms_3", "cits_erms_3", "  ", null, "cits_erms_3"), props::get);
    Assert.assertEquals(specs.size(), 1);
  }

  @Test
  public void testRequireMetadataDefaultsToFalse() {
    Map<String, String> props = new HashMap<>();
    props.put("core.deliveryspec.enkel.metadataType", "enkel");
    List<DeliverySpec> specs = DeliverySpecRegistry.parse(List.of("enkel"), props::get);
    Assert.assertEquals(specs.size(), 1);
    Assert.assertFalse(specs.get(0).isRequireMetadata());
    Assert.assertNull(specs.get(0).getMetadataVersion());
    // oversionerad typ: fulla id:t är typen själv
    Assert.assertEquals(specs.get(0).getMetadataTypeWithVersion(), "enkel");
  }

  @Test
  public void testEmptyRegistry() {
    Assert.assertTrue(DeliverySpecRegistry.parse(null, key -> null).isEmpty());
    Assert.assertTrue(DeliverySpecRegistry.parse(List.of(), key -> null).isEmpty());
  }
}

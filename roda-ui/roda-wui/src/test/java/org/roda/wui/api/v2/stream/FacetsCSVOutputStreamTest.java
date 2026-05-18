/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.api.v2.stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;
import org.roda.core.data.v2.index.facet.FacetFieldResult;

public class FacetsCSVOutputStreamTest {

  @Test
  public void testOutputStartsWithUtf8Bom() throws Exception {
    FacetsCSVOutputStream stream = new FacetsCSVOutputStream(Collections.emptyList(), "test.csv", ",");
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    stream.consumeOutputStream(out);

    byte[] bytes = out.toByteArray();
    assertTrue("Utdata ska innehålla minst 3 byte", bytes.length >= 3);
    assertEquals("Första BOM-byte saknas", (byte) 0xEF, bytes[0]);
    assertEquals("Andra BOM-byte saknas", (byte) 0xBB, bytes[1]);
    assertEquals("Tredje BOM-byte saknas", (byte) 0xBF, bytes[2]);
  }

  @Test
  public void testSwedishFacetValuesEncodedAsUtf8() throws Exception {
    FacetFieldResult facet = new FacetFieldResult("level", 1L);
    facet.addFacetValue("Arkivbestånd", "fonds", 42L);

    FacetsCSVOutputStream stream = new FacetsCSVOutputStream(Collections.singletonList(facet), "test.csv", ",");
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    stream.consumeOutputStream(out);

    String csv = new String(out.toByteArray(), 3, out.size() - 3, StandardCharsets.UTF_8);
    assertTrue("CSV borde innehålla ÅÄÖ i facet-etiketten", csv.contains("Arkivbestånd"));
  }

  @Test
  public void testFacetCsvHasCorrectHeaders() throws Exception {
    FacetsCSVOutputStream stream = new FacetsCSVOutputStream(Collections.emptyList(), "test.csv", ",");
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    stream.consumeOutputStream(out);

    String csv = new String(out.toByteArray(), 3, out.size() - 3, StandardCharsets.UTF_8);
    assertTrue("CSV borde ha 'field'-kolumn", csv.contains("field"));
    assertTrue("CSV borde ha 'label'-kolumn", csv.contains("label"));
    assertTrue("CSV borde ha 'value'-kolumn", csv.contains("value"));
    assertTrue("CSV borde ha 'count'-kolumn", csv.contains("count"));
  }

  @Test
  public void testMultipleFacetFieldsAndValues() throws Exception {
    FacetFieldResult facet1 = new FacetFieldResult("level", 2L);
    facet1.addFacetValue("Arkivbestånd", "fonds", 10L);
    facet1.addFacetValue("Serie", "series", 5L);

    FacetFieldResult facet2 = new FacetFieldResult("state", 1L);
    facet2.addFacetValue("Aktiv", "active", 3L);

    FacetsCSVOutputStream stream = new FacetsCSVOutputStream(
      Arrays.asList(facet1, facet2), "test.csv", ",");
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    stream.consumeOutputStream(out);

    String csv = new String(out.toByteArray(), 3, out.size() - 3, StandardCharsets.UTF_8);
    assertTrue("CSV borde innehålla level-facet", csv.contains("level"));
    assertTrue("CSV borde innehålla state-facet", csv.contains("state"));
    assertTrue("CSV borde innehålla Arkivbestånd", csv.contains("Arkivbestånd"));
    assertTrue("CSV borde innehålla Serie", csv.contains("Serie"));
  }
}

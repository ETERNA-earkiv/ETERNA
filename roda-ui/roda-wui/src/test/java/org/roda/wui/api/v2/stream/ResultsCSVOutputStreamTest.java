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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;
import org.roda.core.data.v2.index.IndexResult;
import org.roda.core.data.v2.index.IsIndexed;

public class ResultsCSVOutputStreamTest {

  @Test
  public void testOutputStartsWithUtf8Bom() throws Exception {
    IndexResult<IsIndexed> result = new IndexResult<>();
    result.setResults(Collections.emptyList());
    result.setTotalCount(0);

    ResultsCSVOutputStream<IsIndexed> stream = new ResultsCSVOutputStream<>(result, "test.csv", ",");
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    stream.consumeOutputStream(out);

    byte[] bytes = out.toByteArray();
    assertTrue("Utdata ska innehålla minst 3 byte", bytes.length >= 3);
    assertEquals("Första BOM-byte saknas", (byte) 0xEF, bytes[0]);
    assertEquals("Andra BOM-byte saknas", (byte) 0xBB, bytes[1]);
    assertEquals("Tredje BOM-byte saknas", (byte) 0xBF, bytes[2]);
  }

  @Test
  public void testSwedishCharactersEncodedAsUtf8() throws Exception {
    IsIndexed mockResult = mock(IsIndexed.class);
    when(mockResult.toCsvHeaders()).thenReturn(Arrays.asList("Titel", "Nivå"));
    when(mockResult.toCsvValues()).thenReturn(Arrays.asList("Arkivbestånd Åström", "Dossié"));

    IndexResult<IsIndexed> result = new IndexResult<>();
    result.setResults(Collections.singletonList(mockResult));
    result.setTotalCount(1);

    ResultsCSVOutputStream<IsIndexed> stream = new ResultsCSVOutputStream<>(result, "test.csv", ",");
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    stream.consumeOutputStream(out);

    // Hoppa över BOM (3 byte) och tolka resten som UTF-8
    String csv = new String(out.toByteArray(), 3, out.size() - 3, StandardCharsets.UTF_8);
    assertTrue("CSV borde innehålla svenska tecken i rubriken", csv.contains("Nivå"));
    assertTrue("CSV borde innehålla ÅÄÖ i data", csv.contains("Arkivbestånd Åström"));
    assertTrue("CSV borde innehålla ä-tecken i data", csv.contains("Dossié"));
  }

  @Test
  public void testEmptyResultsWritesBomOnly() throws Exception {
    IndexResult<IsIndexed> result = new IndexResult<>();
    result.setResults(Collections.emptyList());
    result.setTotalCount(0);

    ResultsCSVOutputStream<IsIndexed> stream = new ResultsCSVOutputStream<>(result, "test.csv", ",");
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    stream.consumeOutputStream(out);

    // Med tom resultatlista skrivs bara BOM (ingen header-rad utan data)
    assertEquals("Tre BOM-bytes förväntas för tom lista", 3, out.toByteArray().length);
  }
}

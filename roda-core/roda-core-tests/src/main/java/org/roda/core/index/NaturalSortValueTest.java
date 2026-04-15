/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/keeps/roda
 */
package org.roda.core.index;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.roda.core.data.common.RodaConstants;
import org.roda.core.index.utils.SolrUtils;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link SolrUtils#toNaturalSortValue(String)}.
 *
 * These tests are deliberately infrastructure-free (no Solr, no Docker, no
 * RodaCoreFactory) so they can always be run in isolation without a running
 * environment.
 */
@Test(groups = {RodaConstants.TEST_GROUP_ALL, RodaConstants.TEST_GROUP_DEV, RodaConstants.TEST_GROUP_TRAVIS})
public class NaturalSortValueTest {

  @Test
  public void testNullInputReturnsNull() {
    assertNull(SolrUtils.toNaturalSortValue(null));
  }

  @Test
  public void testEmptyStringReturnsEmpty() {
    assertEquals("", SolrUtils.toNaturalSortValue(""));
  }

  @Test
  public void testStringWithoutDigitsIsOnlyLowercased() {
    assertEquals("protokoll", SolrUtils.toNaturalSortValue("Protokoll"));
    assertEquals("nämnd", SolrUtils.toNaturalSortValue("Nämnd"));
  }

  @Test
  public void testSingleDigitIsPaddedToThirteenChars() {
    assertEquals("0000000000001", SolrUtils.toNaturalSortValue("1"));
    assertEquals("0000000000002", SolrUtils.toNaturalSortValue("2"));
    assertEquals("0000000000009", SolrUtils.toNaturalSortValue("9"));
  }

  @Test
  public void testDigitInTextIsPadded() {
    assertEquals("kapitel 0000000000002", SolrUtils.toNaturalSortValue("Kapitel 2"));
    assertEquals("kapitel 0000000000010", SolrUtils.toNaturalSortValue("Kapitel 10"));
  }

  @Test
  public void testMultipleDigitGroupsArePadded() {
    assertEquals("kapitel 0000000000002 del 0000000000010",
      SolrUtils.toNaturalSortValue("Kapitel 2 del 10"));
  }

  @Test
  public void testLeadingZerosAreNormalised() {
    // "007" → parsed as 7 → re-padded to 13 digits
    assertEquals("0000000000007", SolrUtils.toNaturalSortValue("007"));
    // Padding "1" and "01" should produce identical sort keys
    assertEquals(SolrUtils.toNaturalSortValue("1"), SolrUtils.toNaturalSortValue("01"));
  }

  @Test
  public void testExactlyThirteenDigitNumberIsUnchanged() {
    assertEquals("9999999999999", SolrUtils.toNaturalSortValue("9999999999999"));
    assertEquals("0000000000000", SolrUtils.toNaturalSortValue("0000000000000"));
  }

  @Test
  public void testDotSeparatedNumbersInTitle() {
    // "1. Protokoll" should produce a sort key that lexicographically
    // precedes "10. Protokoll"
    String key1 = SolrUtils.toNaturalSortValue("1. Protokoll");
    String key10 = SolrUtils.toNaturalSortValue("10. Protokoll");
    assertEquals("0000000000001. protokoll", key1);
    assertEquals("0000000000010. protokoll", key10);
    assert key1.compareTo(key10) < 0 : "1. Protokoll sort key should be less than 10. Protokoll";
  }

  // -----------------------------------------------------------------------
  // Ordering guarantees — the whole point of this feature
  // -----------------------------------------------------------------------

  @Test
  public void testNaturalNumericOrderForPureTitles() {
    List<String> titles = Arrays.asList("10", "3", "1", "20", "2");
    List<String> sorted = titles.stream()
      .sorted(Comparator.comparing(SolrUtils::toNaturalSortValue))
      .toList();
    assertEquals(Arrays.asList("1", "2", "3", "10", "20"), sorted);
  }

  @Test
  public void testNaturalNumericOrderForTitlesWithPrefix() {
    List<String> titles = Arrays.asList("Kapitel 10", "Kapitel 2", "Kapitel 1", "Kapitel 11");
    List<String> sorted = titles.stream()
      .sorted(Comparator.comparing(SolrUtils::toNaturalSortValue))
      .toList();
    assertEquals(Arrays.asList("Kapitel 1", "Kapitel 2", "Kapitel 10", "Kapitel 11"), sorted);
  }

  @Test
  public void testNaturalNumericOrderForDotPrefixedTitles() {
    List<String> titles = Arrays.asList("10. Protokoll", "2. Protokoll", "1. Protokoll");
    List<String> sorted = titles.stream()
      .sorted(Comparator.comparing(SolrUtils::toNaturalSortValue))
      .toList();
    assertEquals(Arrays.asList("1. Protokoll", "2. Protokoll", "10. Protokoll"), sorted);
  }

  @Test
  public void testLexicographicAndNaturalOrderDiffer() {
    // Sanity check: without padding, "10" sorts before "2" lexicographically
    List<String> titles = Arrays.asList("10. Protokoll", "2. Protokoll", "1. Protokoll");
    List<String> lexSorted = titles.stream().sorted().toList();
    List<String> natSorted = titles.stream()
      .sorted(Comparator.comparing(SolrUtils::toNaturalSortValue))
      .toList();
    // Lex order: 1, 10, 2 — Natural order: 1, 2, 10
    assertEquals("10. Protokoll", lexSorted.get(1));
    assertEquals("2. Protokoll", natSorted.get(1));
  }
}

/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/keeps/roda
 */
package org.roda.core.index;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import org.roda.core.data.common.RodaConstants;
import org.roda.core.index.utils.SolrUtils;
import org.testng.annotations.Test;

/**
 * Security tests for {@link SolrUtils#toNaturalSortValue(String)}.
 *
 * These tests verify that adversarial or unusual input cannot crash the
 * indexing pipeline or silently corrupt sort ordering. No external
 * infrastructure (Solr, Docker, RodaCoreFactory) is required.
 *
 * Threat model: a user who can set the title of an AIP, Representation or
 * Representation Information item controls the input to this method. A
 * malicious title must never cause an exception that aborts indexing, and must
 * never produce a sort key wider than 13 digits (which would break ordering
 * for all other documents).
 */
@Test(groups = {RodaConstants.TEST_GROUP_ALL, RodaConstants.TEST_GROUP_DEV, RodaConstants.TEST_GROUP_TRAVIS})
public class NaturalSortValueSecurityTest {

  // -----------------------------------------------------------------------
  // A — Long number overflow (previously threw NumberFormatException,
  //     crashing the entire document indexing operation)
  // -----------------------------------------------------------------------

  @Test
  public void testVeryLargeNumberDoesNotThrow() {
    // 19-digit number exceeds Long.MAX_VALUE — must not throw
    String result = SolrUtils.toNaturalSortValue("Dokument 9999999999999999999");
    assertNotNull("Should return a value, not throw", result);
  }

  @Test
  public void testVeryLargeNumberIsCappedAtThirteenNines() {
    // Numbers beyond the 13-digit cap must be clamped to 9999999999999
    assertEquals("dokument 9999999999999", SolrUtils.toNaturalSortValue("Dokument 9999999999999999999"));
    assertEquals("9999999999999", SolrUtils.toNaturalSortValue("99999999999999"));
    assertEquals("9999999999999", SolrUtils.toNaturalSortValue("100000000000000000000"));
  }

  @Test
  public void testLargeNumberInLongerTitle() {
    // Sort key must always be exactly 13 digits, regardless of input length
    String result = SolrUtils.toNaturalSortValue("Ärende 12345678901234567890 bilaga");
    assertEquals("ärende 9999999999999 bilaga", result);
  }

  // -----------------------------------------------------------------------
  // B — Padding overflow (previously %013d produced 14+ chars for large
  //     numbers, silently breaking sort order for all documents)
  // -----------------------------------------------------------------------

  @Test
  public void testFourteenDigitNumberProducesExactlyThirteenDigitSortKey() {
    // Without the cap, %013d("10000000000000") produces 14 chars
    String result = SolrUtils.toNaturalSortValue("10000000000000");
    assertEquals("Sort key must be exactly 13 digits", 13, result.length());
    assertEquals("9999999999999", result);
  }

  @Test
  public void testSortKeyAlwaysExactlyThirteenDigits() {
    // Any number, regardless of size, must produce a 13-char padded sort key
    String[] inputs = {"1", "99", "999", "9999", "99999", "999999", "9999999",
      "99999999", "999999999", "9999999999", "99999999999", "999999999999",
      "9999999999999", "99999999999999", "123456789012345678"};

    for (String input : inputs) {
      String result = SolrUtils.toNaturalSortValue(input);
      assertEquals("Sort key for '" + input + "' must be 13 digits", 13, result.length());
    }
  }

  @Test
  public void testSortOrderPreservedWhenLargeNumbersAreCapped() {
    // When two large numbers both exceed the cap they become equal —
    // that is acceptable and preferable to a crash or wrong-width sort key.
    String key1 = SolrUtils.toNaturalSortValue("999999999999999");
    String key2 = SolrUtils.toNaturalSortValue("100000000000000");
    assertNotNull(key1);
    assertNotNull(key2);
    // Both are capped at 9999999999999 — equal sort keys, no exception
    assertEquals(key1, key2);
  }

  // -----------------------------------------------------------------------
  // C — Extremely long all-digit string (performance / DoS probe)
  // -----------------------------------------------------------------------

  @Test(timeOut = 5000)
  public void testOneThousandDigitStringCompletesWithinFiveSeconds() {
    // A title containing a 1000-digit number must not hang or throw
    String bigNumber = "1".repeat(1000);
    String result = SolrUtils.toNaturalSortValue("Titel " + bigNumber + " slut");
    assertNotNull(result);
    assertEquals("titel 9999999999999 slut", result);
  }

  @Test(timeOut = 5000)
  public void testTenThousandDigitStringCompletesWithinFiveSeconds() {
    // Stress test: 10 000-digit number must not cause hang or OOM
    String bigNumber = "9".repeat(10_000);
    String result = SolrUtils.toNaturalSortValue(bigNumber);
    assertNotNull(result);
    assertEquals("9999999999999", result);
  }

  // -----------------------------------------------------------------------
  // D — Solr special characters stored in sort field
  //     These are safe as field values (not query syntax) but we verify
  //     the method handles them without throwing.
  // -----------------------------------------------------------------------

  @Test
  public void testSolrSpecialCharactersDoNotThrow() {
    // Characters that are special in Solr query syntax must not cause issues
    String[] titles = {
      "Dokument [1]",
      "Ärendet {2}",
      "Bilaga \"tre\"",
      "Post \\4\\",
      "Kapitel (5)",
      "Serie ^6^",
      "Volym ~7~",
      "Enhet +8-",
      "Fil !9!",
      "Akt &&10||"
    };
    for (String title : titles) {
      String result = SolrUtils.toNaturalSortValue(title);
      assertNotNull("Should not throw for title: " + title, result);
    }
  }

  // -----------------------------------------------------------------------
  // E — Unicode digits (Arabic-Indic, etc.)
  //     Previously \d matched these but Long.parseLong could not parse them,
  //     causing NumberFormatException that crashed indexing.
  // -----------------------------------------------------------------------

  @Test
  public void testArabicIndicNumeralsDoNotThrow() {
    // Arabic-Indic digits ١٢٣ are NOT matched by [0-9] — they must pass through
    // unchanged without throwing an exception.
    String result = SolrUtils.toNaturalSortValue("Dokument ١٢٣");
    assertNotNull("Should not throw for Arabic-Indic numerals", result);
    // The Unicode digits are left untouched (not matched by [0-9]+)
    assertEquals("dokument ١٢٣", result);
  }

  @Test
  public void testExtendedUnicodeDigitsPassThroughUntouched() {
    // Devanagari digits (१२३), Persian digits (۱۲۳), Thai digits (๑๒๓)
    String[] unicodeTitles = {
      "Dokument १२३",   // Devanagari
      "Dokument ۱۲۳",   // Extended Arabic-Indic (Persian)
      "Dokument ๑๒๓",   // Thai
    };
    for (String title : unicodeTitles) {
      String result = SolrUtils.toNaturalSortValue(title);
      assertNotNull("Should not throw for: " + title, result);
      // No ASCII digits present → result is just the lowercased input
      assertTrue("Unicode digits should be left untouched in: " + result,
        result.startsWith("dokument "));
    }
  }

  @Test
  public void testMixedAsciiAndUnicodeDigits() {
    // ASCII digits are padded; Unicode digits are left as-is
    String result = SolrUtils.toNaturalSortValue("Kapitel 3 och ٣");
    assertNotNull(result);
    // ASCII "3" is padded; Arabic-Indic "٣" is left untouched
    assertEquals("kapitel 0000000000003 och ٣", result);
  }

  // -----------------------------------------------------------------------
  // Regression: edge cases that must continue to work after the fix
  // -----------------------------------------------------------------------

  @Test
  public void testMaxSafeValueProducesExactlyThirteenNines() {
    assertEquals("9999999999999", SolrUtils.toNaturalSortValue("9999999999999"));
  }

  @Test
  public void testJustBelowCapIsNotCapped() {
    // 9999999999998 is 13 digits and below the cap — must not be changed
    assertEquals("9999999999998", SolrUtils.toNaturalSortValue("9999999999998"));
  }

  @Test
  public void testJustAboveCapIsClamped() {
    // 10000000000000 is 14 digits — must be capped at 9999999999999
    assertEquals("9999999999999", SolrUtils.toNaturalSortValue("10000000000000"));
  }
}

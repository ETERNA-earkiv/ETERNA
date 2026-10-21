/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.common.access;

import org.roda.core.data.common.RodaConstants;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tester för lagrumsmönster i regelfilen — "*",
 * kapitelmönster täcker paragrafer, paragrafmönster täcker bara exakt, suffix
 * måste matcha, annan lag täcker inte, normalisering och avvisade mönster.
 */
@Test(groups = {RodaConstants.TEST_GROUP_ALL, RodaConstants.TEST_GROUP_DEV, RodaConstants.TEST_GROUP_TRAVIS,
  "accessrules"})
public class LagrumPatternTest {

  private static final String OSL = "OSL";

  private static final Lagrum OSL_21 = new Lagrum("OSL", 21);
  private static final Lagrum OSL_21_7 = new Lagrum("OSL", 21, 7);
  private static final Lagrum OSL_21_7A = new Lagrum("OSL", 21, 7, "a");
  private static final Lagrum OSL_21_8 = new Lagrum("OSL", 21, 8);
  private static final Lagrum OSL_22_1 = new Lagrum("OSL", 22, 1);
  private static final Lagrum TF_21_7 = new Lagrum("TF", 21, 7);

  /** mönster, kandidat, täcks? */
  @DataProvider(name = "covers")
  public Object[][] covers() {
    return new Object[][] {
      // --- joker täcker allt
      {"*", OSL_21, true}, // ----
      {"*", OSL_21_7, true}, // ----
      {"*", OSL_21_7A, true}, // ----
      {"*", TF_21_7, true}, // ----
      {"*", null, true}, // ----
      // --- kapitelmönster täcker kapitlet och alla dess paragrafer
      {"OSL 21", OSL_21, true}, // ----
      {"OSL 21", OSL_21_7, true}, // ----
      {"OSL 21", OSL_21_7A, true}, // ----
      {"OSL 21", OSL_21_8, true}, // ----
      {"OSL 21 kap.", OSL_21_7, true}, // ----
      {"OSL 21", OSL_22_1, false}, // annat kapitel
      {"OSL 21", TF_21_7, false}, // annan lag
      {"OSL 21", null, false}, // ----
      // --- paragrafmönster täcker bara exakt paragraf (inkl. suffix)
      {"OSL 21:7", OSL_21_7, true}, // ----
      {"OSL 21 kap. 7 §", OSL_21_7, true}, // ----
      {"OSL 21:7", OSL_21_7A, false}, // suffix måste matcha
      {"OSL 21:7a", OSL_21_7A, true}, // ----
      {"OSL 21 kap. 7 a §", OSL_21_7A, true}, // ----
      {"OSL 21:7a", OSL_21_7, false}, // ----
      {"OSL 21:7", OSL_21_8, false}, // ----
      {"OSL 21:7", OSL_21, false}, // hela kapitlet är bredare än mönstret
      {"OSL 21:7", TF_21_7, false}, // annan lag
      {"OSL 21:7", null, false}, // ----
      // --- skiftläge och utelämnad lag
      {"osl 21:7", OSL_21_7, true}, // ----
      {"21:7", OSL_21_7, true}, // defaultAct = OSL
      {"21 kap. 7 §", OSL_21_7, true}, // ----
    };
  }

  @Test(dataProvider = "covers")
  public void testCovers(String pattern, Lagrum candidate, boolean expected) {
    LagrumPattern p = LagrumPattern.parse(pattern, OSL);
    Assert.assertEquals(p.covers(candidate), expected, "\"" + pattern + "\" täcker " + candidate);
  }

  /** mönster, defaultAct, normaliserad form, joker? */
  @DataProvider(name = "normalized")
  public Object[][] normalized() {
    return new Object[][] {
      {"*", OSL, "*", true}, // ----
      {" * ", OSL, "*", true}, // ----
      {"OSL 21", OSL, "OSL 21", false}, // ----
      {"OSL 21 kap.", OSL, "OSL 21", false}, // ----
      {"osl 21 kap. 7 §", OSL, "OSL 21:7", false}, // ----
      {"OSL 21:7", OSL, "OSL 21:7", false}, // ----
      {"OSL 21 kap. 7 a §", OSL, "OSL 21:7a", false}, // ----
      {"21:7", "TF", "TF 21:7", false}, // ----
      {"21 kap.", "tf", "TF 21", false}, // ----
    };
  }

  @Test(dataProvider = "normalized")
  public void testParseAndToString(String pattern, String defaultAct, String expected, boolean wildcard) {
    LagrumPattern p = LagrumPattern.parse(pattern, defaultAct);
    Assert.assertEquals(p.toString(), expected);
    Assert.assertEquals(p.isWildcard(), wildcard);
    if (wildcard) {
      Assert.assertNull(p.getLagrum());
      Assert.assertSame(p, LagrumPattern.wildcard());
    } else {
      Assert.assertEquals(p.getLagrum().toString(), expected);
    }
  }

  @DataProvider(name = "invalid")
  public Object[][] invalid() {
    return new Object[][] {{null}, {""}, {"   "}, {"skräp"}, {"OSL"}, {"21"}, {"OSL 21:"}, {"OSL 21:7 och 21:8"},
      {"OSL 21:7, OSL 26:1"}, {"**"}, {"OSL *"}, {"OSL 21 kap. 1–3 §§"}};
  }

  @Test(dataProvider = "invalid")
  public void testInvalidPatternIsRejected(String pattern) {
    Assert.assertThrows(IllegalArgumentException.class, () -> LagrumPattern.parse(pattern, OSL));
  }

  @Test
  public void testEqualsAndHashCode() {
    Assert.assertEquals(LagrumPattern.parse("OSL 21 kap. 7 §", OSL), LagrumPattern.parse("osl 21:7", OSL));
    Assert.assertEquals(LagrumPattern.parse("OSL 21 kap. 7 §", OSL).hashCode(),
      LagrumPattern.parse("osl 21:7", OSL).hashCode());
    Assert.assertEquals(LagrumPattern.parse("*", OSL), LagrumPattern.wildcard());
    Assert.assertEquals(LagrumPattern.of(OSL_21_7), LagrumPattern.parse("OSL 21:7", OSL));
    Assert.assertNotEquals(LagrumPattern.parse("OSL 21", OSL), LagrumPattern.parse("OSL 21:7", OSL));
    Assert.assertNotEquals(LagrumPattern.parse("OSL 21:7", OSL), LagrumPattern.parse("OSL 21:7a", OSL));
    Assert.assertNotEquals(LagrumPattern.parse("OSL 21:7", OSL), LagrumPattern.wildcard());
    Assert.assertThrows(IllegalArgumentException.class, () -> LagrumPattern.of(null));
  }
}

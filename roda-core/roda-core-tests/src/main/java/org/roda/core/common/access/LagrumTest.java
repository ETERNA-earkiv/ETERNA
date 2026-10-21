/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.common.access;

import java.util.Arrays;
import java.util.List;

import org.roda.core.data.common.RodaConstants;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Värdeklassen Lagrum — normalisering (versal lag,
 * gement suffix), textform, likhet, ordning och ogiltiga värden.
 */
@Test(groups = {RodaConstants.TEST_GROUP_ALL, RodaConstants.TEST_GROUP_DEV, RodaConstants.TEST_GROUP_TRAVIS,
  "accessrules"})
public class LagrumTest {

  @Test
  public void testToString() {
    Assert.assertEquals(new Lagrum("OSL", 21).toString(), "OSL 21");
    Assert.assertEquals(new Lagrum("OSL", 21, 7).toString(), "OSL 21:7");
    Assert.assertEquals(new Lagrum("OSL", 21, 7, "a").toString(), "OSL 21:7a");
    Assert.assertEquals(new Lagrum("OSL", 21, null, null).toString(), "OSL 21");
  }

  @Test
  public void testNormalization() {
    Lagrum lagrum = new Lagrum(" osl ", 21, 7, " A ");
    Assert.assertEquals(lagrum.getLag(), "OSL");
    Assert.assertEquals(lagrum.getSuffix(), "a");
    // tomt suffix är inget suffix
    Assert.assertNull(new Lagrum("OSL", 21, 7, "").getSuffix());
    Assert.assertNull(new Lagrum("OSL", 21, 7, "  ").getSuffix());
    Assert.assertNull(new Lagrum("OSL", 21, null, "").getSuffix());
  }

  @Test
  public void testEqualsAndHashCode() {
    Assert.assertEquals(new Lagrum("osl", 21, 7, "A"), new Lagrum("OSL", 21, 7, "a"));
    Assert.assertEquals(new Lagrum("osl", 21, 7, "A").hashCode(), new Lagrum("OSL", 21, 7, "a").hashCode());
    Assert.assertEquals(new Lagrum("OSL", 21), new Lagrum("OSL", 21, null, null));
    Assert.assertNotEquals(new Lagrum("OSL", 21), new Lagrum("OSL", 21, 7));
    Assert.assertNotEquals(new Lagrum("OSL", 21, 7), new Lagrum("OSL", 21, 7, "a"));
    Assert.assertNotEquals(new Lagrum("OSL", 21, 7), new Lagrum("TF", 21, 7));
    Assert.assertNotEquals(new Lagrum("OSL", 21, 7), new Lagrum("OSL", 22, 7));
  }

  @Test
  public void testOrdering() {
    List<Lagrum> list = Arrays.asList(new Lagrum("TF", 1, 1), new Lagrum("OSL", 21, 7, "a"), new Lagrum("OSL", 21, 7),
      new Lagrum("OSL", 21), new Lagrum("OSL", 3, 9));
    List<String> sorted = list.stream().sorted().map(Lagrum::toString).toList();
    Assert.assertEquals(sorted, List.of("OSL 3:9", "OSL 21", "OSL 21:7", "OSL 21:7a", "TF 1:1"));
  }

  @Test
  public void testInvalidValuesAreRejected() {
    Assert.assertThrows(IllegalArgumentException.class, () -> new Lagrum(null, 21, 7, null));
    Assert.assertThrows(IllegalArgumentException.class, () -> new Lagrum("  ", 21, 7, null));
    Assert.assertThrows(IllegalArgumentException.class, () -> new Lagrum("OSL", 0, 7, null));
    Assert.assertThrows(IllegalArgumentException.class, () -> new Lagrum("OSL", 21, 0, null));
    // suffix utan paragraf är meningslöst
    Assert.assertThrows(IllegalArgumentException.class, () -> new Lagrum("OSL", 21, null, "a"));
  }
}

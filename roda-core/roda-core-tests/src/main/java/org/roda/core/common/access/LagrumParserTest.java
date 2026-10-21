/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.common.access;

import java.util.List;

import org.roda.core.common.access.LagrumParser.ParseResult;
import org.roda.core.data.common.RodaConstants;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tabellstyrda tester för lagrumstolkningen — alla
 * skrivsätt i kontraktet, suffix, flera lagrum med olika separatorer,
 * tomt/null, skräptext (varning i stället för gissning), skiftläge, hela
 * kapitlet samt lagarv mellan delar.
 */
@Test(groups = {RodaConstants.TEST_GROUP_ALL, RodaConstants.TEST_GROUP_DEV, RodaConstants.TEST_GROUP_TRAVIS,
  "accessrules"})
public class LagrumParserTest {

  private static final String OSL = "OSL";

  /** text, defaultAct, förväntade lagrum (normaliserad form), antal varningar. */
  @DataProvider(name = "cases")
  public Object[][] cases() {
    return new Object[][] {
      // --- paragrafformer
      {"OSL 21 kap. 7 §", OSL, List.of("OSL 21:7"), 0}, // ----
      {"OSL 21 kap 7 §", OSL, List.of("OSL 21:7"), 0}, // ----
      {"OSL 21 kap.7§", OSL, List.of("OSL 21:7"), 0}, // ----
      {"OSL 21 kapitel 7 §", OSL, List.of("OSL 21:7"), 0}, // ----
      {"21 kap. 7 §", OSL, List.of("OSL 21:7"), 0}, // ----
      {"OSL 21:7", OSL, List.of("OSL 21:7"), 0}, // ----
      {"21:7", OSL, List.of("OSL 21:7"), 0}, // ----
      {"  OSL 21:7  ", OSL, List.of("OSL 21:7"), 0}, // ----
      {"OSL 21 kap. 7", OSL, List.of("OSL 21:7"), 0}, // § är valfritt
      // --- suffix
      {"OSL 21 kap. 7 a §", OSL, List.of("OSL 21:7a"), 0}, // ----
      {"OSL 21 kap. 7a §", OSL, List.of("OSL 21:7a"), 0}, // ----
      {"OSL 21:7a", OSL, List.of("OSL 21:7a"), 0}, // ----
      {"OSL 21:7 a", OSL, List.of("OSL 21:7a"), 0}, // ----
      {"OSL 21 kap. 7 A §", OSL, List.of("OSL 21:7a"), 0}, // suffix normaliseras gement
      // --- hela kapitlet
      {"OSL 21 kap.", OSL, List.of("OSL 21"), 0}, // ----
      {"OSL 21 kap", OSL, List.of("OSL 21"), 0}, // ----
      {"21 kap.", OSL, List.of("OSL 21"), 0}, // ----
      {"OSL 21", OSL, List.of("OSL 21"), 0}, // ----
      // --- skiftläge
      {"osl 21 kap. 7 §", OSL, List.of("OSL 21:7"), 0}, // ----
      {"Osl 21:7", OSL, List.of("OSL 21:7"), 0}, // ----
      {"OSL 21 KAP. 7 §", OSL, List.of("OSL 21:7"), 0}, // ----
      // --- flera lagrum, olika separatorer
      {"OSL 26 kap. 1 § och 21 kap. 7 §", OSL, List.of("OSL 26:1", "OSL 21:7"), 0}, // ----
      {"OSL 26 kap. 1 §, 21 kap. 7 §", OSL, List.of("OSL 26:1", "OSL 21:7"), 0}, // ----
      {"OSL 26 kap. 1 §; 21 kap. 7 §", OSL, List.of("OSL 26:1", "OSL 21:7"), 0}, // ----
      {"OSL 26:1 / 21:7", OSL, List.of("OSL 26:1", "OSL 21:7"), 0}, // ----
      {"OSL 26:1 samt OSL 21:7", OSL, List.of("OSL 26:1", "OSL 21:7"), 0}, // ----
      {"OSL 21:7,OSL 26:1;TF 2:1/3:2 samt 4:1", OSL, List.of("OSL 21:7", "OSL 26:1", "TF 2:1", "TF 3:2", "TF 4:1"),
        0},
      {"OSL 26 kap. 1 § och 21 kap. 7 § och 25 kap. 1 §", OSL, List.of("OSL 26:1", "OSL 21:7", "OSL 25:1"), 0},
      // --- paragrafuppräkning: "8 §" ärver lag och kapitel från föregående del
      {"OSL 21 kap. 7 och 8 §§", OSL, List.of("OSL 21:7", "OSL 21:8"), 0}, // ----
      {"OSL 21 kap. 1, 3 och 7 §§", OSL, List.of("OSL 21:1", "OSL 21:3", "OSL 21:7"), 0}, // ----
      {"OSL 21 kap. 7 § och 8 a §", OSL, List.of("OSL 21:7", "OSL 21:8a"), 0}, // ----
      {"OSL 21:7, 8 §, 26 kap. 1 §, 2 §", OSL, List.of("OSL 21:7", "OSL 21:8", "OSL 26:1", "OSL 26:2"), 0},
      {"OSL 21 kap. 7 § och 8 § 1 st.", OSL, List.of("OSL 21:7", "OSL 21:8"), 0}, // ----
      {"OSL 21 kap. 7 § och 8", OSL, List.of("OSL 21:7", "OSL 21:8"), 0}, // i paragrafkontext
      {"OSL 21 kap. 7 och 8 a §§", OSL, List.of("OSL 21:7", "OSL 21:8a"), 0}, // ----
      {"8 §", OSL, List.of(), 1}, // ingen föregående del att ärva från
      {"3", OSL, List.of(), 1}, // ----
      {"OSL 21, 26", OSL, List.of("OSL 21"), 1}, // naket tal efter helt kapitel: tvetydigt
      {"OSL 21 kap., 26", OSL, List.of("OSL 21"), 1}, // ----
      {"OSL 21, 7 §", OSL, List.of("OSL 21", "OSL 21:7"), 0}, // med § är det entydigt
      // --- lagarv: del utan lag ärver närmast föregående, annars defaultAct
      {"TF 2 kap. 1 § och 3 kap. 2 §", OSL, List.of("TF 2:1", "TF 3:2"), 0}, // ----
      {"21:7 och TF 2:1 och 3:2", OSL, List.of("OSL 21:7", "TF 2:1", "TF 3:2"), 0}, // ----
      // --- defaultAct
      {"21:7", "TF", List.of("TF 21:7"), 0}, // ----
      {"21:7", "tf", List.of("TF 21:7"), 0}, // ----
      {"21:7", null, List.of("OSL 21:7"), 0}, // null ⇒ OSL
      {"21:7", "  ", List.of("OSL 21:7"), 0}, // tom ⇒ OSL
      // --- dubbletter avdubbleras, ordning bevaras
      {"OSL 21:7, OSL 21 kap. 7 §, 21:7", OSL, List.of("OSL 21:7"), 0}, // ----
      {"OSL 21:7, OSL 21", OSL, List.of("OSL 21:7", "OSL 21"), 0}, // kapitel ≠ paragraf
      // --- SFS-nummer och svans (stycke/punkt) ignoreras
      {"OSL (2009:400) 21 kap. 7 §", OSL, List.of("OSL 21:7"), 0}, // ----
      {"OSL 21 kap. 7 § 1 st.", OSL, List.of("OSL 21:7"), 0}, // ----
      {"OSL 21 kap. 7 § första stycket", OSL, List.of("OSL 21:7"), 0}, // ----
      {"OSL 21 kap. 7 § 1 st. 2 p.", OSL, List.of("OSL 21:7"), 0}, // ----
      // --- tomt
      {"", OSL, List.of(), 0}, // ----
      {"   ", OSL, List.of(), 0}, // ----
      {null, OSL, List.of(), 0}, // ----
      {",", OSL, List.of(), 0}, // bara separator
      {"OSL 21:7,", OSL, List.of("OSL 21:7"), 0}, // avslutande separator
      // --- otolkbart ⇒ varning, inget lagrum
      {"sekretess enligt lag", OSL, List.of(), 1}, // ----
      {"OSL", OSL, List.of(), 1}, // ----
      {"21", OSL, List.of(), 1}, // naket tal: varken lag eller kap.
      {"OSL 21:", OSL, List.of(), 1}, // ----
      {"OSL 21 7 §", OSL, List.of(), 1}, // varken kap. eller kolon
      {"OSL 0 kap. 1 §", OSL, List.of(), 1}, // kapitel 0
      {"OSL 21 kap. 0 §", OSL, List.of(), 1}, // paragraf 0
      {"kap 21", OSL, List.of(), 1}, // ordet kap är ingen lag
      {"OSL 21 kap. 1–3 §§", OSL, List.of(), 1}, // intervall stöds inte
      {"OSL 21 kap. 7 st", OSL, List.of(), 1}, // "st" utan styckenummer
      {"OSL 21 kap. 7 § och sekretess enligt lag", OSL, List.of("OSL 21:7"), 1}, // ----
      {"skräp, OSL 21:7, mer skräp", OSL, List.of("OSL 21:7"), 2}, // ----
    };
  }

  @Test(dataProvider = "cases")
  public void testParse(String text, String defaultAct, List<String> expected, int expectedWarnings) {
    ParseResult result = LagrumParser.parse(text, defaultAct);
    List<String> actual = result.getLagrum().stream().map(Lagrum::toString).toList();
    Assert.assertEquals(actual, expected, "lagrum för \"" + text + "\" " + result);
    Assert.assertEquals(result.getWarnings().size(), expectedWarnings, "varningar för \"" + text + "\" " + result);
    Assert.assertEquals(result.hasWarnings(), expectedWarnings > 0);
  }

  @Test
  public void testWarningNamesTheUnparsablePart() {
    ParseResult result = LagrumParser.parse("OSL 21:7 och sekretess enligt lag", OSL);
    Assert.assertEquals(result.getWarnings().size(), 1);
    Assert.assertTrue(result.getWarnings().get(0).contains("sekretess enligt lag"), result.getWarnings().get(0));
    Assert.assertFalse(result.getWarnings().get(0).contains("21:7"), result.getWarnings().get(0));
  }

  @Test
  public void testStructuredValues() {
    ParseResult result = LagrumParser.parse("OSL 21 kap. 7 a §", OSL);
    Lagrum lagrum = result.getLagrum().get(0);
    Assert.assertEquals(lagrum.getLag(), "OSL");
    Assert.assertEquals(lagrum.getKapitel(), 21);
    Assert.assertEquals(lagrum.getParagraf(), Integer.valueOf(7));
    Assert.assertEquals(lagrum.getSuffix(), "a");
    Assert.assertFalse(lagrum.isWholeChapter());

    Lagrum chapter = LagrumParser.parse("OSL 21 kap.", OSL).getLagrum().get(0);
    Assert.assertNull(chapter.getParagraf());
    Assert.assertNull(chapter.getSuffix());
    Assert.assertTrue(chapter.isWholeChapter());
    Assert.assertEquals(chapter, new Lagrum("osl", 21));
  }

  @Test
  public void testResultListsAreUnmodifiable() {
    ParseResult result = LagrumParser.parse("OSL 21:7", OSL);
    Assert.assertThrows(UnsupportedOperationException.class, () -> result.getLagrum().add(new Lagrum("OSL", 1)));
    Assert.assertThrows(UnsupportedOperationException.class, () -> result.getWarnings().add("x"));
  }
}

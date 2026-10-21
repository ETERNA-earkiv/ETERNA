/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.common.access;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.roda.core.data.common.RodaConstants;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tester för läsningen av eterna_admin-dokument —
 * prefixat dokument, standardnamnrymd, dokument utan namnrymd, saknade/tomma
 * element, lagrumstolkning med varningar samt XXE-skydd (DOCTYPE avvisas).
 */
@Test(groups = {RodaConstants.TEST_GROUP_ALL, RodaConstants.TEST_GROUP_DEV, RodaConstants.TEST_GROUP_TRAVIS,
  "accessrules"})
public class EternaAdminParserTest {

  private static final String OSL = "OSL";
  private static final String NS = EternaAdminParser.NAMESPACE;

  private static final String COMPLETE_WITH_PREFIX = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
    + "<ea:eternaAdmin xmlns:ea=\"" + NS + "\">\n" // ----
    + "  <ea:sekretess>sekretess_forekommer</ea:sekretess>\n" // ----
    + "  <ea:sekretessgrund>OSL 26 kap. 1 § och 21 kap. 7 a §</ea:sekretessgrund>\n" // ----
    + "  <ea:personuppgifter>kansliga_forekommer</ea:personuppgifter>\n" // ----
    + "  <ea:personuppgiftsniva>niva_3</ea:personuppgiftsniva>\n" // ----
    + "  <ea:pulGrund>Art. 6.1 e</ea:pulGrund>\n" // ----
    + "  <ea:granskningsdatum>2026-09-01</ea:granskningsdatum>\n" // ----
    + "  <ea:anteckning>Bedömd av arkivarie</ea:anteckning>\n" // ----
    + "</ea:eternaAdmin>\n";

  /** Som formulärmallen eterna_admin.xml.hbs skriver det. */
  private static final String DEFAULT_NAMESPACE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
    + "<eternaAdmin xmlns=\"" + NS + "\">\n" // ----
    + "    <sekretess>helt_sekretessbelagd</sekretess>\n" // ----
    + "    <sekretessgrund>OSL 21:7</sekretessgrund>\n" // ----
    + "    <personuppgifter>forekommer</personuppgifter>\n" // ----
    + "    <personuppgiftsniva>niva_2</personuppgiftsniva>\n" // ----
    + "</eternaAdmin>\n";

  private static final String NO_NAMESPACE = "<eternaAdmin>" // ----
    + "<sekretess>ingen_sekretess</sekretess>" // ----
    + "<personuppgifter>inga</personuppgifter>" // ----
    + "</eternaAdmin>";

  @Test
  public void testCompleteDocumentWithPrefix() throws IOException {
    EternaAdminFacts facts = EternaAdminParser.parse(stream(COMPLETE_WITH_PREFIX), OSL);

    Assert.assertTrue(facts.isPresent());
    Assert.assertEquals(facts.getSekretess(), "sekretess_forekommer");
    Assert.assertEquals(facts.getSekretessgrund(), "OSL 26 kap. 1 § och 21 kap. 7 a §");
    Assert.assertEquals(facts.getLagrum(), List.of(new Lagrum("OSL", 26, 1), new Lagrum("OSL", 21, 7, "a")));
    Assert.assertTrue(facts.getWarnings().isEmpty(), facts.getWarnings().toString());
    Assert.assertEquals(facts.getPersonuppgifter(), "kansliga_forekommer");
    Assert.assertEquals(facts.getPersonuppgiftsniva(), "niva_3");
    // arv sätts av applikatorn, inte parsern
    Assert.assertFalse(facts.isInherited());
    Assert.assertNull(facts.getSourceAipId());
  }

  @Test
  public void testDefaultNamespaceWithoutPrefix() throws IOException {
    EternaAdminFacts facts = EternaAdminParser.parse(stream(DEFAULT_NAMESPACE), OSL);

    Assert.assertTrue(facts.isPresent());
    Assert.assertEquals(facts.getSekretess(), "helt_sekretessbelagd");
    Assert.assertEquals(facts.getLagrum(), List.of(new Lagrum("OSL", 21, 7)));
    Assert.assertEquals(facts.getPersonuppgifter(), "forekommer");
    Assert.assertEquals(facts.getPersonuppgiftsniva(), "niva_2");
  }

  @Test
  public void testDocumentWithoutNamespace() throws IOException {
    EternaAdminFacts facts = EternaAdminParser.parse(stream(NO_NAMESPACE), OSL);

    Assert.assertTrue(facts.isPresent());
    Assert.assertEquals(facts.getSekretess(), "ingen_sekretess");
    Assert.assertEquals(facts.getPersonuppgifter(), "inga");
    Assert.assertNull(facts.getSekretessgrund());
    Assert.assertNull(facts.getPersonuppgiftsniva());
    Assert.assertTrue(facts.getLagrum().isEmpty());
    Assert.assertTrue(facts.getWarnings().isEmpty());
  }

  @Test
  public void testStringOverloadMatchesStreamOverload() throws IOException {
    EternaAdminFacts fromString = EternaAdminParser.parse(COMPLETE_WITH_PREFIX, OSL);
    EternaAdminFacts fromStream = EternaAdminParser.parse(stream(COMPLETE_WITH_PREFIX), OSL);
    Assert.assertEquals(fromString.toString(), fromStream.toString());
  }

  @Test
  public void testMissingPersonuppgiftsniva() throws IOException {
    // elementet är valfritt (tillkom i XSD 1.1) — dokument utan det måste läsas
    // som null, inte fel
    String xml = "<eternaAdmin xmlns=\"" + NS + "\">" // ----
      + "<sekretess>ingen_sekretess</sekretess>" // ----
      + "<personuppgifter>forekommer</personuppgifter>" // ----
      + "</eternaAdmin>";
    EternaAdminFacts facts = EternaAdminParser.parse(stream(xml), OSL);

    Assert.assertTrue(facts.isPresent());
    Assert.assertEquals(facts.getPersonuppgifter(), "forekommer");
    Assert.assertNull(facts.getPersonuppgiftsniva());
  }

  @Test
  public void testEmptyAndBlankElementsAreNull() throws IOException {
    String xml = "<eternaAdmin xmlns=\"" + NS + "\">" // ----
      + "<sekretess>sekretess_forekommer</sekretess>" // ----
      + "<sekretessgrund></sekretessgrund>" // ----
      + "<personuppgifter>   </personuppgifter>" // ----
      + "<personuppgiftsniva/>" // ----
      + "</eternaAdmin>";
    EternaAdminFacts facts = EternaAdminParser.parse(stream(xml), OSL);

    Assert.assertEquals(facts.getSekretess(), "sekretess_forekommer");
    Assert.assertNull(facts.getSekretessgrund());
    Assert.assertNull(facts.getPersonuppgifter());
    Assert.assertNull(facts.getPersonuppgiftsniva());
    Assert.assertTrue(facts.getLagrum().isEmpty());
    Assert.assertTrue(facts.getWarnings().isEmpty());
  }

  @Test
  public void testValuesAreTrimmed() throws IOException {
    String xml = "<eternaAdmin xmlns=\"" + NS + "\">" // ----
      + "<sekretess>\n  sekretess_forekommer\n</sekretess>" // ----
      + "<sekretessgrund>\n  21 kap. 7 §\n</sekretessgrund>" // ----
      + "</eternaAdmin>";
    EternaAdminFacts facts = EternaAdminParser.parse(stream(xml), OSL);
    Assert.assertEquals(facts.getSekretess(), "sekretess_forekommer");
    Assert.assertEquals(facts.getSekretessgrund(), "21 kap. 7 §");
    Assert.assertEquals(facts.getLagrum(), List.of(new Lagrum("OSL", 21, 7)));
  }

  @Test
  public void testUnparsableSekretessgrundGivesWarning() throws IOException {
    String xml = "<eternaAdmin xmlns=\"" + NS + "\">" // ----
      + "<sekretess>sekretess_forekommer</sekretess>" // ----
      + "<sekretessgrund>OSL 21:7 och sekretess enligt lag</sekretessgrund>" // ----
      + "</eternaAdmin>";
    EternaAdminFacts facts = EternaAdminParser.parse(stream(xml), OSL);

    Assert.assertEquals(facts.getLagrum(), List.of(new Lagrum("OSL", 21, 7)));
    Assert.assertEquals(facts.getWarnings().size(), 1);
    Assert.assertTrue(facts.getWarnings().get(0).contains("sekretess enligt lag"));
  }

  @Test
  public void testDefaultActIsUsedForLagrumWithoutAct() throws IOException {
    String xml = "<eternaAdmin xmlns=\"" + NS + "\"><sekretessgrund>2 kap. 1 §</sekretessgrund></eternaAdmin>";
    Assert.assertEquals(EternaAdminParser.parse(stream(xml), "TF").getLagrum(), List.of(new Lagrum("TF", 2, 1)));
    Assert.assertEquals(EternaAdminParser.parse(stream(xml), OSL).getLagrum(), List.of(new Lagrum("OSL", 2, 1)));
  }

  @Test
  public void testDoctypeIsRejected() {
    String xml = "<?xml version=\"1.0\"?>\n" // ----
      + "<!DOCTYPE eternaAdmin [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>\n" // ----
      + "<eternaAdmin xmlns=\"" + NS + "\"><sekretess>&xxe;</sekretess></eternaAdmin>";
    Assert.assertThrows(IOException.class, () -> EternaAdminParser.parse(stream(xml), OSL));
  }

  @Test
  public void testHarmlessDoctypeIsAlsoRejected() {
    // även en DOCTYPE utan entiteter avvisas — enklast och säkrast
    String xml = "<!DOCTYPE eternaAdmin>\n<eternaAdmin><sekretess>inga</sekretess></eternaAdmin>";
    Assert.assertThrows(IOException.class, () -> EternaAdminParser.parse(stream(xml), OSL));
  }

  @Test
  public void testMalformedXmlIsAnIOException() {
    Assert.assertThrows(IOException.class,
      () -> EternaAdminParser.parse(stream("<eternaAdmin><sekretess>x</eternaAdmin>"), OSL));
    Assert.assertThrows(IOException.class, () -> EternaAdminParser.parse(stream(""), OSL));
  }

  @Test
  public void testNullInputIsAbsent() throws IOException {
    EternaAdminFacts facts = EternaAdminParser.parse((InputStream) null, OSL);
    Assert.assertFalse(facts.isPresent());
    Assert.assertNull(facts.getSekretess());
    Assert.assertTrue(facts.getLagrum().isEmpty());
    Assert.assertFalse(EternaAdminParser.parse((String) null, OSL).isPresent());
  }

  @Test
  public void testAbsentFacts() {
    EternaAdminFacts facts = EternaAdminFacts.absent();
    Assert.assertFalse(facts.isPresent());
    Assert.assertNull(facts.getSekretess());
    Assert.assertNull(facts.getSekretessgrund());
    Assert.assertNull(facts.getPersonuppgifter());
    Assert.assertNull(facts.getPersonuppgiftsniva());
    Assert.assertNotNull(facts.getLagrum());
    Assert.assertNotNull(facts.getWarnings());
    Assert.assertFalse(facts.isInherited());
    // setters med null ger tomma listor, aldrig null
    facts.setLagrum(null);
    facts.setWarnings(null);
    Assert.assertTrue(facts.getLagrum().isEmpty());
    Assert.assertTrue(facts.getWarnings().isEmpty());
  }

  @Test
  public void testStreamIsClosedAfterParse() throws IOException {
    boolean[] closed = {false};
    InputStream in = new ByteArrayInputStream(NO_NAMESPACE.getBytes(StandardCharsets.UTF_8)) {
      @Override
      public void close() throws IOException {
        closed[0] = true;
        super.close();
      }
    };
    EternaAdminParser.parse(in, OSL);
    Assert.assertTrue(closed[0]);
  }

  private static InputStream stream(String xml) {
    return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
  }
}

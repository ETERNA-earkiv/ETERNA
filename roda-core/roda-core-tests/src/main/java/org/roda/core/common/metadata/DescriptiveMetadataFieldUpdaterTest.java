/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.common.metadata;

import java.util.Map;

import org.roda.core.common.metadata.DescriptiveMetadataFieldUpdater.Result;
import org.roda.core.common.metadata.DescriptiveMetadataFieldUpdater.Status;
import org.roda.core.common.metadata.DescriptiveMetadataFieldUpdater.UpdateMode;
import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.exceptions.GenericException;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Enhetstester för XPath-baserad in-place-uppdatering.
 * Rena tester utan RODA-miljö: EAD/DC/ERMS-liknande dokument, namespaces,
 * attribut, villkor, lägen och förbjudna xpath.
 */
@Test(groups = {RodaConstants.TEST_GROUP_ALL, RodaConstants.TEST_GROUP_DEV, RodaConstants.TEST_GROUP_TRAVIS})
public class DescriptiveMetadataFieldUpdaterTest {

  private static final String EAD_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
    + "<ead xmlns=\"urn:isbn:1-931666-22-9\"><archdesc level=\"fonds\"><did>"
    + "<unittitle encodinganalog=\"3.1.2\">Gammal titel</unittitle>"
    + "<unitid countrycode=\"SE\">SE/HALM/1</unitid>"
    + "</did><scopecontent><p>Utanför mallen</p></scopecontent></archdesc></ead>";

  private static final String DC_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
    + "<simpledc xmlns:dc=\"http://purl.org/dc/elements/1.1/\">"
    + "<title>DC-titel</title><description></description></simpledc>";

  private static final String ERMS_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
    + "<erms xmlns=\"https://DILCIS.eu/XML/ERMS\"><aggregations>"
    + "<aggregation systemIdentifier=\"SYS-1\" aggregationType=\"caseFile\">"
    + "<objectId>OBJ-1</objectId><title>Ärendetitel</title><status value=\"open\"/>"
    + "</aggregation></aggregations></erms>";

  // --- elementuppdatering med namespace-wildcard (mallarnas *:-syntax) ---

  @Test
  public void testUpdateElementWithNamespaceWildcard() throws GenericException {
    Result result = DescriptiveMetadataFieldUpdater.update(EAD_XML, "//*:archdesc/*:did/*:unittitle[1]", "Ny titel",
      UpdateMode.OVERWRITE, null);
    Assert.assertEquals(result.getStatus(), Status.UPDATED);
    Assert.assertEquals(result.getOldValue(), "Gammal titel");
    Assert.assertTrue(result.getNewXml().contains(">Ny titel</unittitle>"));
    // in-place: element utanför mallen finns kvar (förlustfritt)
    Assert.assertTrue(result.getNewXml().contains("Utanför mallen"));
    // attribut på det uppdaterade elementet bevaras
    Assert.assertTrue(result.getNewXml().contains("encodinganalog=\"3.1.2\""));
  }

  @Test
  public void testUpdateElementWithoutNamespace() throws GenericException {
    Result result = DescriptiveMetadataFieldUpdater.update(DC_XML, "//*:simpledc/*:title[1]", "Ny DC-titel",
      UpdateMode.OVERWRITE, null);
    Assert.assertEquals(result.getStatus(), Status.UPDATED);
    Assert.assertTrue(result.getNewXml().contains(">Ny DC-titel</title>"));
  }

  // --- attributuppdatering ---

  @Test
  public void testUpdateAttribute() throws GenericException {
    Result result = DescriptiveMetadataFieldUpdater.update(ERMS_XML, "//*:aggregation[1]/@systemIdentifier", "SYS-2",
      UpdateMode.OVERWRITE, null);
    Assert.assertEquals(result.getStatus(), Status.UPDATED);
    Assert.assertEquals(result.getOldValue(), "SYS-1");
    Assert.assertTrue(result.getNewXml().contains("systemIdentifier=\"SYS-2\""));
  }

  @Test
  public void testUpdateAttributeWithPredicate() throws GenericException {
    Result result = DescriptiveMetadataFieldUpdater.update(ERMS_XML, "//*:aggregation[1]/*:status/@value", "closed",
      UpdateMode.OVERWRITE, null);
    Assert.assertEquals(result.getStatus(), Status.UPDATED);
    Assert.assertEquals(result.getOldValue(), "open");
    Assert.assertTrue(result.getNewXml().contains("value=\"closed\""));
  }

  // --- lägen och villkor ---

  @Test
  public void testCompleteModeSkipsFilledField() throws GenericException {
    Result result = DescriptiveMetadataFieldUpdater.update(EAD_XML, "//*:archdesc/*:did/*:unittitle[1]", "Ny titel",
      UpdateMode.COMPLETE, null);
    Assert.assertEquals(result.getStatus(), Status.SKIPPED_ALREADY_FILLED);
    Assert.assertNull(result.getNewXml());
  }

  @Test
  public void testCompleteModeFillsEmptyField() throws GenericException {
    Result result = DescriptiveMetadataFieldUpdater.update(DC_XML, "//*:simpledc/*:description[1]", "Ny beskrivning",
      UpdateMode.COMPLETE, null);
    Assert.assertEquals(result.getStatus(), Status.UPDATED);
    Assert.assertTrue(result.getNewXml().contains(">Ny beskrivning</description>"));
  }

  @Test
  public void testConditionMet() throws GenericException {
    Result result = DescriptiveMetadataFieldUpdater.update(ERMS_XML, "//*:aggregation[1]/*:status/@value", "closed",
      UpdateMode.OVERWRITE, "open");
    Assert.assertEquals(result.getStatus(), Status.UPDATED);
  }

  @Test
  public void testConditionNotMet() throws GenericException {
    Result result = DescriptiveMetadataFieldUpdater.update(ERMS_XML, "//*:aggregation[1]/*:status/@value", "closed",
      UpdateMode.OVERWRITE, "ad_acta");
    Assert.assertEquals(result.getStatus(), Status.SKIPPED_CONDITION_NOT_MET);
    Assert.assertNull(result.getNewXml());
  }

  @Test
  public void testValueUnchangedIsSkipped() throws GenericException {
    Result result = DescriptiveMetadataFieldUpdater.update(EAD_XML, "//*:archdesc/*:did/*:unittitle[1]",
      "Gammal titel", UpdateMode.OVERWRITE, null);
    Assert.assertEquals(result.getStatus(), Status.SKIPPED_VALUE_UNCHANGED);
  }

  @Test
  public void testNodeMissingIsSkipped() throws GenericException {
    Result result = DescriptiveMetadataFieldUpdater.update(DC_XML, "//*:simpledc/*:subject[1]", "Nytt ämne",
      UpdateMode.OVERWRITE, null);
    Assert.assertEquals(result.getStatus(), Status.SKIPPED_NODE_MISSING);
    Assert.assertNull(result.getNewXml());
  }

  // --- element med elementbarn skrivs aldrig över (barnen skulle försvinna) ---

  private static final String EAD_MIXED_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
    + "<ead xmlns=\"urn:isbn:1-931666-22-9\"><archdesc level=\"fonds\"><did>"
    + "<unittitle>Protokoll <emph render=\"italic\">kommunfullmäktige</emph></unittitle></did></archdesc></ead>";

  @Test(expectedExceptions = GenericException.class)
  public void testElementWithElementChildrenIsRejected() throws GenericException {
    DescriptiveMetadataFieldUpdater.update(EAD_MIXED_XML, "//*:did/*:unittitle[1]", "Ny titel", UpdateMode.OVERWRITE,
      null);
  }

  @Test(expectedExceptions = GenericException.class)
  public void testElementWithElementChildrenIsRejectedBeforeConditions() throws GenericException {
    // avvisas även när villkoret/läget annars hade gett SKIPPED
    DescriptiveMetadataFieldUpdater.update(EAD_MIXED_XML, "//*:did/*:unittitle[1]", "Ny titel", UpdateMode.COMPLETE,
      "annat värde");
  }

  // --- förbjudna xpath ---

  @Test(expectedExceptions = GenericException.class)
  public void testForbiddenDocumentFunction() throws GenericException {
    DescriptiveMetadataFieldUpdater.update(DC_XML, "//*:title[document('file:///etc/passwd')]", "x",
      UpdateMode.OVERWRITE, null);
  }

  @Test(expectedExceptions = GenericException.class)
  public void testForbiddenUnparsedText() throws GenericException {
    DescriptiveMetadataFieldUpdater.update(DC_XML, "//*:title[unparsed-text('x')]", "x", UpdateMode.OVERWRITE, null);
  }

  @Test(expectedExceptions = GenericException.class)
  public void testRelativeXpathRejected() throws GenericException {
    DescriptiveMetadataFieldUpdater.update(DC_XML, "title", "x", UpdateMode.OVERWRITE, null);
  }

  @Test(expectedExceptions = GenericException.class)
  public void testEmptyXpathRejected() throws GenericException {
    DescriptiveMetadataFieldUpdater.update(DC_XML, "  ", "x", UpdateMode.OVERWRITE, null);
  }

  @Test(expectedExceptions = GenericException.class)
  public void testUnparsableXmlRejected() throws GenericException {
    DescriptiveMetadataFieldUpdater.update("<trasig", "//*:title", "x", UpdateMode.OVERWRITE, null);
  }

  // --- mallparsning (vitlistan) ---

  @Test
  public void testParseTemplateFieldXpaths() {
    String template = "{{~field name=\"title\" order=\"75\" type=\"text\" label='{\"sv\": \"Titel\"}' "
      + "xpath=\"//*:aggregation[1]/*:title/string()\" auto-generate=\"title\"~}}\n"
      + "{{~field name=\"sektion\" type=\"separator\" label='{\"sv\": \"Sektion\"}'~}}\n"
      + "{{~field name=\"status\" type=\"text\" xpath=\"//*:aggregation[1]/*:status/@value/string()\"~}}\n"
      + "<xml>{{title}}</xml>";
    Map<String, String> fields = DescriptiveMetadataFieldUpdater.parseTemplateFieldXpaths(template);
    Assert.assertEquals(fields.size(), 2);
    Assert.assertEquals(fields.get("title"), "//*:aggregation[1]/*:title/string()");
    Assert.assertEquals(fields.get("status"), "//*:aggregation[1]/*:status/@value/string()");
    // separatorer utan xpath ingår inte i vitlistan
    Assert.assertFalse(fields.containsKey("sektion"));
  }

  @Test
  public void testParseTemplateWithNestedJsonAndEscapedQuotes() {
    // ead-dialekten: nästlad JSON med "}}" inuti optionsLabels, avslut "~}}"
    String eadStyle = "{{~field name=\"level\" type=\"list\" options='[\"fonds\",\"file\"]' "
      + "optionsLabels='{\"fonds\": {\"en\":\"Fonds\", \"sv\":\"Arkivbestånd\"}}' "
      + "xpath=\"/*:ead/*:archdesc/@level/string()\" ~}}";
    // dc-dialekten: escapade citattecken i label, avslut \"}}\" utan tilde
    String dcStyle = "{{field name=\"title\" order='2' label=\"{\\\"en\\\": \\\"Title\\\"}\" "
      + "xpath=\"//*:title/string()\"}}";
    Map<String, String> eadFields = DescriptiveMetadataFieldUpdater.parseTemplateFieldXpaths(eadStyle);
    Assert.assertEquals(eadFields.get("level"), "/*:ead/*:archdesc/@level/string()");
    Map<String, String> dcFields = DescriptiveMetadataFieldUpdater.parseTemplateFieldXpaths(dcStyle);
    Assert.assertEquals(dcFields.get("title"), "//*:title/string()");
  }

  @Test
  public void testParseRealTemplateDialectCoverage() {
    // säkerställ att parsern hittar alla xpath-försedda fält i en mall som
    // blandar deklarationer och XML-kropp
    String template = "{{~field name=\"a\" xpath=\"//*:a/string()\"~}}\n"
      + "<?xml version=\"1.0\"?>\n<root>{{#if a}}<a>{{a}}</a>{{/if}}</root>";
    Map<String, String> fields = DescriptiveMetadataFieldUpdater.parseTemplateFieldXpaths(template);
    Assert.assertEquals(fields.size(), 1);
    Assert.assertEquals(fields.get("a"), "//*:a/string()");
  }

  @Test
  public void testToWritableXpath() {
    Assert.assertEquals(DescriptiveMetadataFieldUpdater.toWritableXpath("//*:title/string()"), "//*:title");
    Assert.assertEquals(DescriptiveMetadataFieldUpdater.toWritableXpath("//*:a/@b/string()"), "//*:a/@b");
    Assert.assertEquals(DescriptiveMetadataFieldUpdater.toWritableXpath("//*:title"), "//*:title");
  }

  // --- XML-deklaration och svenska tecken bevaras ---

  @Test
  public void testEncodingAndSwedishCharactersPreserved() throws GenericException {
    Result result = DescriptiveMetadataFieldUpdater.update(EAD_XML, "//*:archdesc/*:did/*:unittitle[1]",
      "Bygglovsärende — Åkersberga gård", UpdateMode.OVERWRITE, null);
    Assert.assertEquals(result.getStatus(), Status.UPDATED);
    Assert.assertTrue(result.getNewXml().contains("Bygglovsärende — Åkersberga gård"));
    Assert.assertTrue(result.getNewXml().startsWith("<?xml"));
  }
}

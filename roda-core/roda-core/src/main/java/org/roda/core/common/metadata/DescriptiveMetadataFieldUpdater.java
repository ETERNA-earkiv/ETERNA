/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.common.metadata;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.roda.core.data.exceptions.GenericException;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmValue;

/**
 * XPath-baserad in-place-uppdatering av ett enskilt fält
 * i en beskrivande metadatafil.
 *
 * Uppdateringen sker direkt i dokumentträdet (Saxon s9api över DOM) — aldrig
 * genom omrendering via formulärmallen, eftersom mallrendering inte är
 * förlustfri: element som saknas i mallen försvinner vid omrendering.
 *
 * Klassen är ren och sidoeffektsfri: indata är XML-innehåll + xpath + nytt
 * värde, utdata är ett {@link Result} med status, gammalt värde och nytt
 * XML-innehåll. Anroparen ansvarar för att xpath alltid resolvas server-side
 * ur formulärmallen (hård vitlista) — se {@link #parseTemplateFieldXpaths}.
 */
public final class DescriptiveMetadataFieldUpdater {

  /** Skrivläge för uppdateringen. */
  public enum UpdateMode {
    /** Skriv alltid (standard för massredigering). */
    OVERWRITE,
    /** Komplettering: skriv aldrig över ett icke-tomt värde. */
    COMPLETE
  }

  public enum Status {
    UPDATED, SKIPPED_NODE_MISSING, SKIPPED_ALREADY_FILLED, SKIPPED_CONDITION_NOT_MET, SKIPPED_VALUE_UNCHANGED
  }

  public static final class Result {
    private final Status status;
    private final String oldValue;
    private final String newXml;

    private Result(Status status, String oldValue, String newXml) {
      this.status = status;
      this.oldValue = oldValue;
      this.newXml = newXml;
    }

    public Status getStatus() {
      return status;
    }

    public String getOldValue() {
      return oldValue;
    }

    /** Nytt XML-innehåll — endast satt när status är UPDATED. */
    public String getNewXml() {
      return newXml;
    }

    public boolean isUpdated() {
      return status == Status.UPDATED;
    }
  }

  /**
   * Matchar starten av fältdeklarationer i .xml.hbs-mallar:
   * {{~field name="title" ... xpath="..." ...~}} eller {{field ...}}.
   * Slutet hittas med en teckenscanner ({@link #findHelperEnd}) eftersom
   * attributvärdena kan innehålla '}}' (nästlad JSON i optionsLabels) och
   * escapade citattecken.
   */
  private static final Pattern TEMPLATE_FIELD_START_PATTERN = Pattern.compile("\\{\\{~?field\\b");
  private static final Pattern ATTR_NAME_PATTERN = Pattern.compile("(?:^|\\s)name=\"([^\"]+)\"");
  private static final Pattern ATTR_XPATH_PATTERN = Pattern.compile("(?:^|\\s)xpath=\"([^\"]+)\"");

  /** XPath-konstruktioner som aldrig är legitima i en fältuppdatering. */
  private static final String[] FORBIDDEN_XPATH_CONSTRUCTS = {"document(", "doc(", "collection(", "unparsed-text",
    "system-property", "environment-variable", "transform("};

  private DescriptiveMetadataFieldUpdater() {
    // statisk verktygsklass
  }

  /**
   * Extraherar fältnamn → xpath ur en .xml.hbs-mall. Endast fält med
   * xpath-attribut tas med (separatorer m.fl. saknar xpath). Detta är den
   * hårda vitlistan: ett fält som inte finns i mallen kan inte uppdateras.
   */
  public static Map<String, String> parseTemplateFieldXpaths(String templateContent) {
    Map<String, String> result = new LinkedHashMap<>();
    if (templateContent == null) {
      return result;
    }
    Matcher fields = TEMPLATE_FIELD_START_PATTERN.matcher(templateContent);
    while (fields.find()) {
      int end = findHelperEnd(templateContent, fields.end());
      if (end < 0) {
        break;
      }
      String attributes = templateContent.substring(fields.end(), end);
      Matcher name = ATTR_NAME_PATTERN.matcher(attributes);
      Matcher xpath = ATTR_XPATH_PATTERN.matcher(attributes);
      if (name.find() && xpath.find()) {
        result.put(name.group(1), xpath.group(1));
      }
    }
    return result;
  }

  /**
   * Hittar index för avslutande "}}" utanför citattecken, med hänsyn till
   * escapade tecken. Returnerar -1 om inget avslut hittas.
   */
  private static int findHelperEnd(String content, int from) {
    char quote = 0;
    for (int i = from; i < content.length() - 1; i++) {
      char c = content.charAt(i);
      if (c == '\\') {
        i++;
      } else if (quote != 0) {
        if (c == quote) {
          quote = 0;
        }
      } else if (c == '\'' || c == '"') {
        quote = c;
      } else if (c == '}' && content.charAt(i + 1) == '}') {
        return i;
      }
    }
    return -1;
  }

  /**
   * Gör en mall-xpath skrivbar: mallarnas xpath slutar i regel med
   * "/string()" (värdeuttag) — för skrivning behövs noden själv.
   */
  public static String toWritableXpath(String xpath) {
    String trimmed = xpath.trim();
    if (trimmed.endsWith("/string()")) {
      return trimmed.substring(0, trimmed.length() - "/string()".length());
    }
    return trimmed;
  }

  /**
   * Defensiv kontroll utöver mallvitlistan: avvisa xpath med konstruktioner
   * som läser externa resurser eller systemtillstånd.
   */
  public static void validateXpath(String xpath) throws GenericException {
    if (xpath == null || xpath.trim().isEmpty()) {
      throw new GenericException("Empty xpath");
    }
    String normalized = xpath.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    for (String forbidden : FORBIDDEN_XPATH_CONSTRUCTS) {
      if (normalized.contains(forbidden)) {
        throw new GenericException("Forbidden construct in xpath: " + forbidden);
      }
    }
    if (!xpath.trim().startsWith("/")) {
      throw new GenericException("Xpath must be absolute (start with / or //): " + xpath);
    }
  }

  /**
   * Uppdaterar det första fält som matchar xpath (element eller attribut).
   *
   * @param xmlContent
   *          befintligt XML-innehåll
   * @param writableXpath
   *          skrivbar xpath (se {@link #toWritableXpath}), resolvad ur mallen
   * @param newValue
   *          nytt värde
   * @param mode
   *          skrivläge; i COMPLETE skrivs aldrig ett icke-tomt värde över
   * @param expectedOldValue
   *          valfritt villkor: uppdatera endast om nuvarande värde (trimmat)
   *          är exakt detta; null = inget villkor
   * @return resultat med status, gammalt värde och nytt XML-innehåll
   * @throws GenericException
   *           vid ogiltig xpath, oparsbar XML eller serialiseringsfel
   */
  public static Result update(String xmlContent, String writableXpath, String newValue, UpdateMode mode,
    String expectedOldValue) throws GenericException {
    validateXpath(writableXpath);

    Document document = parse(xmlContent);
    Node target = selectFirstNode(document, writableXpath);

    if (target == null) {
      return new Result(Status.SKIPPED_NODE_MISSING, null, null);
    }

    // setTextContent på ett element med elementbarn tar bort barnen, vilket
    // inte är förlustfritt; sådana fält kan inte massredigeras
    if (target instanceof Element) {
      NodeList children = target.getChildNodes();
      for (int i = 0; i < children.getLength(); i++) {
        if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
          throw new GenericException("The element selected by xpath '" + writableXpath
            + "' contains child elements and cannot be updated without losing content");
        }
      }
    }

    String oldValue = target instanceof Attr ? ((Attr) target).getValue() : target.getTextContent();
    String oldTrimmed = oldValue == null ? "" : oldValue.trim();
    String newTrimmed = newValue == null ? "" : newValue.trim();

    if (mode == UpdateMode.COMPLETE && !oldTrimmed.isEmpty()) {
      return new Result(Status.SKIPPED_ALREADY_FILLED, oldValue, null);
    }
    if (expectedOldValue != null && !oldTrimmed.equals(expectedOldValue.trim())) {
      return new Result(Status.SKIPPED_CONDITION_NOT_MET, oldValue, null);
    }
    if (oldTrimmed.equals(newTrimmed)) {
      return new Result(Status.SKIPPED_VALUE_UNCHANGED, oldValue, null);
    }

    if (target instanceof Attr) {
      ((Attr) target).setValue(newTrimmed);
    } else if (target instanceof Element) {
      target.setTextContent(newTrimmed);
    } else {
      target.setNodeValue(newTrimmed);
    }

    return new Result(Status.UPDATED, oldValue, serialize(document));
  }

  private static Document parse(String xmlContent) throws GenericException {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      factory.setExpandEntityReferences(false);
      return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xmlContent)));
    } catch (Exception e) {
      throw new GenericException("Could not parse descriptive metadata XML", e);
    }
  }

  /**
   * Väljer första matchande nod med Saxon (XPath 2.0 — mallarna använder
   * *:-wildcards som JAXP:s inbyggda XPath 1.0 inte stöder). Saxon wrappar
   * DOM-trädet, så den matchade nodens externalNode är den ursprungliga
   * DOM-noden och kan muteras in-place.
   */
  private static Node selectFirstNode(Document document, String xpath) throws GenericException {
    try {
      Processor processor = new Processor(false);
      XPathCompiler compiler = processor.newXPathCompiler();
      XPathSelector selector = compiler.compile(xpath).load();
      XdmNode wrapped = processor.newDocumentBuilder().wrap(document);
      selector.setContextItem(wrapped);
      XdmValue matches = selector.evaluate();
      for (XdmItem item : matches) {
        if (item instanceof XdmNode) {
          Object external = ((XdmNode) item).getExternalNode();
          if (external instanceof Node) {
            return (Node) external;
          }
        }
      }
      return null;
    } catch (net.sf.saxon.s9api.SaxonApiException e) {
      throw new GenericException("Could not evaluate xpath '" + xpath + "'", e);
    }
  }

  private static String serialize(Document document) throws GenericException {
    try {
      TransformerFactory factory = TransformerFactory.newInstance();
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      Transformer transformer = factory.newTransformer();
      transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
      StringWriter writer = new StringWriter();
      transformer.transform(new DOMSource(document), new StreamResult(writer));
      return writer.toString();
    } catch (Exception e) {
      throw new GenericException("Could not serialize descriptive metadata XML", e);
    }
  }
}

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

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Läser ett eterna_admin-dokument
 * (namnrymd {@value #NAMESPACE}, schema config/schemas/eterna_admin.xsd) till
 * {@link EternaAdminFacts}.
 *
 * <ul>
 * <li>DOM-parser med XXE-skydd: DOCTYPE avvisas (ger {@link IOException}),
 * externa entiteter och extern DTD är avstängda, XInclude av.</li>
 * <li>Element matchas på lokalt namn oavsett prefix och namnrymd — både
 * {@code <ea:sekretess>}, {@code <sekretess xmlns="...">} och ett dokument
 * helt utan namnrymd läses. Första förekomsten i dokumentordning gäller.</li>
 * <li>Tomma element (eller enbart blanktecken) ger null.</li>
 * <li>{@code sekretessgrund} tolkas till lagrum via {@link LagrumParser};
 * otolkbara delar hamnar i {@code warnings}.</li>
 * <li>{@code present} = true så snart dokumentet kunde parsas; vilka element
 * som saknas avgörs av motorn (saknad sekretess ⇒ ej_bedomd).</li>
 * </ul>
 */
public final class EternaAdminParser {

  public static final String NAMESPACE = "https://eterna.whitered.se/xml/eterna_admin/1";
  public static final String ROOT_ELEMENT = "eternaAdmin";
  public static final String EL_SEKRETESS = "sekretess";
  public static final String EL_SEKRETESSGRUND = "sekretessgrund";
  public static final String EL_PERSONUPPGIFTER = "personuppgifter";
  public static final String EL_PERSONUPPGIFTSNIVA = "personuppgiftsniva";

  private EternaAdminParser() {
    // statisk verktygsklass
  }

  /**
   * Tolkar ett eterna_admin-dokument.
   *
   * @param xml
   *          dokumentet; strömmen stängs av metoden (null ⇒
   *          {@link EternaAdminFacts#absent()})
   * @param defaultAct
   *          lag för lagrum utan lagbeteckning (konfig
   *          core.access_rules.default_lag)
   * @throws IOException
   *           vid läsfel, ogiltig XML eller avvisat dokument (DOCTYPE)
   */
  public static EternaAdminFacts parse(InputStream xml, String defaultAct) throws IOException {
    if (xml == null) {
      return EternaAdminFacts.absent();
    }
    Document document;
    try (InputStream in = xml) {
      document = newDocumentBuilder().parse(in);
    } catch (SAXException e) {
      throw new IOException("Ogiltigt eterna_admin-dokument: " + e.getMessage(), e);
    }

    Element root = document.getDocumentElement();
    if (root == null) {
      return EternaAdminFacts.absent();
    }

    EternaAdminFacts facts = new EternaAdminFacts();
    facts.setPresent(true);
    facts.setSekretess(textOf(root, EL_SEKRETESS));
    facts.setSekretessgrund(textOf(root, EL_SEKRETESSGRUND));
    facts.setPersonuppgifter(textOf(root, EL_PERSONUPPGIFTER));
    facts.setPersonuppgiftsniva(textOf(root, EL_PERSONUPPGIFTSNIVA));

    LagrumParser.ParseResult parsed = LagrumParser.parse(facts.getSekretessgrund(), defaultAct);
    facts.getLagrum().addAll(parsed.getLagrum());
    facts.getWarnings().addAll(parsed.getWarnings());
    return facts;
  }

  /** Bekvämlighet för tester och verktyg: dokumentet som sträng (UTF-8). */
  public static EternaAdminFacts parse(String xml, String defaultAct) throws IOException {
    if (xml == null) {
      return EternaAdminFacts.absent();
    }
    return parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), defaultAct);
  }

  /**
   * Säker DOM-byggare: inga DOCTYPE, inga externa entiteter/DTD:er, ingen
   * XInclude. Samma uppsättning som DescriptiveMetadataFieldUpdater.
   */
  private static DocumentBuilder newDocumentBuilder() throws IOException {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
      factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      factory.setXIncludeAware(false);
      factory.setExpandEntityReferences(false);
      try {
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
      } catch (IllegalArgumentException e) {
        // attributen stöds inte av denna parser — funktionerna ovan räcker
      }
      return factory.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      throw new IOException("Kunde inte skapa säker XML-parser för eterna_admin", e);
    }
  }

  /**
   * Textinnehållet i första elementet (dokumentordning, rot inkluderad) med
   * angivet lokalt namn, oavsett prefix/namnrymd. Trimmat; tomt ⇒ null.
   */
  private static String textOf(Element root, String localName) {
    if (localName.equals(localNameOf(root))) {
      return trimToNull(root.getTextContent());
    }
    NodeList all = root.getElementsByTagName("*");
    for (int i = 0; i < all.getLength(); i++) {
      Node node = all.item(i);
      if (localName.equals(localNameOf(node))) {
        return trimToNull(node.getTextContent());
      }
    }
    return null;
  }

  private static String localNameOf(Node node) {
    String local = node.getLocalName();
    if (local != null) {
      return local;
    }
    // ej namnrymdsmedvetet nodnamn — skala av ev. prefix
    String name = node.getNodeName();
    int colon = name.indexOf(':');
    return colon < 0 ? name : name.substring(colon + 1);
  }

  private static String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}

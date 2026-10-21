/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.common.permissions;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.v2.accessrules.AccessRule;
import org.roda.core.data.v2.user.GroupOverview;
import org.roda.core.data.v2.user.NodePermission;
import org.roda.core.data.v2.user.PermissionOverview;
import org.roda.core.data.v2.user.RoleArea;
import org.roda.core.data.v2.user.UserOverview;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tester för HTML- och CSV-renderarna med en
 * fejkad {@link PermissionOverviewMessages} (svenska rubriker) – ingen
 * RodaCoreFactory.
 */
@Test(groups = {RodaConstants.TEST_GROUP_ALL, RodaConstants.TEST_GROUP_DEV, RodaConstants.TEST_GROUP_TRAVIS,
  "accessrules"})
public class PermissionOverviewRenderersTest {

  private static final String SOC = "socialtjanst";
  private static final String HANDL = "handlaggare";
  private static final String GENERATED_AT = "2026-10-08T10:00:00Z";
  private static final String EVIL_TITLE = "<b>Arkiv & \"Co\"</b>";

  /** Fejkade meddelanden: några svenska nycklar, resten fallback. */
  private static PermissionOverviewMessages swedish() {
    Map<String, String> m = new HashMap<>();
    m.put("permission.overview.lang", "sv");
    m.put("permission.overview.title", "Behörighetsöversikt");
    m.put("permission.overview.section.roles", "Rättigheter per grupp");
    m.put("permission.overview.section.users", "Användare");
    m.put("permission.overview.section.nodes", "Strukturenheter");
    m.put("permission.overview.section.rules", "Åtkomstregler");
    m.put("permission.overview.column.email", "E-post");
    m.put("permission.overview.column.fullName", "Fullständigt namn");
    m.put("permission.overview.roleTitle.aip", "Strukturenheter (AIP)");
    m.put("permission.overview.role.aip.read", "Lista och sök efter strukturenheter (AIP)");
    m.put("permission.overview.permission.READ", "Läsa");
    m.put("permission.overview.level.class", "Klass");
    m.put("ui.facets.IndexedAIP.level.fonds", "Arkivbestånd");
    m.put("permission.overview.rules.none", "Inga åtkomstregler är aktiva.");
    m.put("permission.overview.yes", "Ja");
    m.put("permission.overview.no", "Nej");
    return (key, fallback) -> m.getOrDefault(key, fallback);
  }

  private static PermissionOverview overview(boolean rulesEnabled) {
    PermissionOverview o = new PermissionOverview();
    o.setGeneratedAt(GENERATED_AT);
    o.setGeneratedBy("admin");

    o.setRoleAreas(Arrays.asList(new RoleArea("aip", Arrays.asList("aip.read", "aip.update")),
      new RoleArea("member", Collections.singletonList("member.read"))));

    GroupOverview handl = new GroupOverview();
    handl.setName(HANDL);
    handl.setFullName("Handläggare");
    handl.setActive(true);
    handl.setAllRoles(Collections.singletonList("aip.read"));
    handl.setDirectRoles(Collections.singletonList("aip.read"));
    handl.setUsers(Collections.singletonList("cecilia"));
    GroupOverview soc = new GroupOverview();
    soc.setName(SOC);
    soc.setFullName("Socialtjänsten; Exempelstad");
    soc.setActive(false);
    soc.setAllRoles(Arrays.asList("aip.read", "aip.update", "member.read"));
    soc.setDirectRoles(Arrays.asList("aip.read", "aip.update", "member.read"));
    soc.setUsers(Arrays.asList("anna", "bertil"));
    o.setGroups(Arrays.asList(handl, soc));

    UserOverview anna = new UserOverview();
    anna.setName("anna");
    anna.setFullName("Svensson; Anna");
    anna.setEmail("anna@example.org");
    anna.setActive(true);
    anna.setGroups(Collections.singletonList(SOC));
    anna.setDirectRoles(Collections.singletonList("job.read"));
    anna.setAllRoles(Arrays.asList("aip.read", "aip.update", "job.read", "member.read"));
    UserOverview cecilia = new UserOverview();
    cecilia.setName("cecilia");
    cecilia.setFullName("Cecilia \"C\"");
    cecilia.setActive(false);
    cecilia.setGroups(Collections.singletonList(HANDL));
    cecilia.setAllRoles(Collections.singletonList("aip.read"));
    o.setUsers(Arrays.asList(anna, cecilia));

    NodePermission f1 = new NodePermission();
    f1.setAipId("f1");
    f1.setLevel("fonds");
    f1.setTitle(EVIL_TITLE);
    f1.setGroupsByType(map("READ", Arrays.asList(HANDL, SOC), "GRANT", Collections.singletonList(SOC)));
    f1.setUsersByType(map("READ", Collections.singletonList("anna")));
    f1.setRestrictedGroups(Collections.singletonList(HANDL));
    NodePermission c1 = new NodePermission();
    c1.setAipId("c1");
    c1.setLevel("class");
    c1.setTitle("Klass A");
    c1.setParentId("f1");
    c1.setGroupsByType(map("READ", Collections.singletonList(SOC)));
    c1.setUsersByType(map());
    o.setNodes(Arrays.asList(f1, c1));

    AccessRule rule = new AccessRule(HANDL);
    rule.setSekretess(Collections.singletonList("ingen_sekretess"));
    rule.setLagrum(Arrays.asList("OSL 26:1", "OSL 26:3"));
    rule.setPersonuppgifter(Collections.singletonList("*"));
    rule.setPersonuppgiftsniva(Arrays.asList("inga", "vanliga"));
    rule.setNote("Han sa \"hej\"; sedan gick han");
    o.setRules(Collections.singletonList(rule));
    o.setRulesEnabled(rulesEnabled);
    return o;
  }

  private static Map<String, List<String>> map(Object... keyValues) {
    Map<String, List<String>> map = new LinkedHashMap<>();
    for (int i = 0; i + 1 < keyValues.length; i += 2) {
      @SuppressWarnings("unchecked")
      List<String> value = (List<String>) keyValues[i + 1];
      map.put((String) keyValues[i], value);
    }
    return map;
  }

  private static int count(String haystack, String needle) {
    int count = 0;
    int idx = 0;
    while ((idx = haystack.indexOf(needle, idx)) >= 0) {
      count++;
      idx += needle.length();
    }
    return count;
  }

  // ---------------------------------------------------------------------- HTML

  @Test
  public void testHtmlContainsSectionsGroupsMarksAndSwedishHeadings() {
    String html = PermissionOverviewHtmlRenderer.render(overview(true), swedish());

    Assert.assertTrue(html.startsWith("<!DOCTYPE html>"), "fristående dokument");
    Assert.assertTrue(html.contains("<meta charset=\"utf-8\">"));
    Assert.assertTrue(html.contains("@media print"), "utskriftsregler");
    Assert.assertFalse(html.contains("<link"), "inga externa resurser");
    Assert.assertTrue(html.contains("<title>Behörighetsöversikt – " + GENERATED_AT + " – admin</title>"),
      html.substring(0, Math.min(400, html.length())));
    Assert.assertTrue(html.contains("<h1>Behörighetsöversikt – " + GENERATED_AT + " – admin</h1>"));

    // fyra avsnitt med svenska rubriker
    Assert.assertTrue(html.contains("<h2>1. Rättigheter per grupp</h2>"));
    Assert.assertTrue(html.contains("<h2>2. Användare</h2>"));
    Assert.assertTrue(html.contains("<h2>3. Strukturenheter</h2>"));
    Assert.assertTrue(html.contains("<h2>4. Åtkomstregler</h2>"));
    Assert.assertTrue(html.contains("<th>E-post</th>"));
    Assert.assertTrue(html.contains("<th>Fullständigt namn</th>"));

    // matrisen: gruppkolumner, områdesrad, beskrivning + nyckel, bockar
    Assert.assertTrue(html.contains("<th>" + HANDL + "</th>"));
    Assert.assertTrue(html.contains("<th class=\"inactive\">" + SOC + "</th>"), "inaktiv grupp markeras");
    Assert.assertTrue(html.contains("<th colspan=\"3\">Strukturenheter (AIP)</th>"));
    Assert.assertTrue(html.contains("Lista och sök efter strukturenheter (AIP) <code>aip.read</code>"));
    Assert.assertTrue(html.contains("<code>aip.update</code></th>"));
    Assert.assertTrue(html.contains(PermissionOverviewHtmlRenderer.CHECK));
    // handlaggare: aip.read; socialtjanst: aip.read, aip.update, member.read ⇒ 4 bockar
    Assert.assertEquals(count(html, "<td class=\"y\">" + PermissionOverviewHtmlRenderer.CHECK + "</td>"), 4);
    Assert.assertEquals(count(html, "<td class=\"n\"></td>"), 2);

    // användare
    Assert.assertTrue(html.contains("<td>anna@example.org</td>"));
    Assert.assertTrue(html.contains("<td>Svensson; Anna</td>"));
    Assert.assertTrue(html.contains("<td>Cecilia &quot;C&quot;</td>"));

    // strukturenheter: nivåöversättning, behörigheter per typ, spärrade grupper
    Assert.assertTrue(html.contains("<td>Arkivbestånd</td>"), "facettens nivånamn används som fallback");
    Assert.assertTrue(html.contains("<td>Klass</td>"));
    Assert.assertTrue(html.contains("<th>Läsa</th>"));
    Assert.assertTrue(html.contains(HANDL + ", " + SOC));
    Assert.assertTrue(html.contains("<td>" + HANDL + "</td>"), "spärrad grupp");

    // regler
    Assert.assertTrue(html.contains("OSL 26:1, OSL 26:3"));
    Assert.assertTrue(html.contains("inga, vanliga"));
    Assert.assertTrue(html.contains("Han sa &quot;hej&quot;; sedan gick han"));
    Assert.assertTrue(html.trim().endsWith("</html>"));
  }

  @Test
  public void testHtmlEscapesEverything() {
    String html = PermissionOverviewHtmlRenderer.render(overview(true), swedish());
    Assert.assertTrue(html.contains("&lt;b&gt;Arkiv &amp; &quot;Co&quot;&lt;/b&gt;"));
    Assert.assertFalse(html.contains(EVIL_TITLE), "råtiteln får inte läcka ut oescapad");
    Assert.assertFalse(html.contains("<b>Arkiv"));

    Assert.assertEquals(PermissionOverviewHtmlRenderer.esc("a<b>&\"'c"), "a&lt;b&gt;&amp;&quot;&#39;c");
    Assert.assertEquals(PermissionOverviewHtmlRenderer.esc(null), "");
    Assert.assertEquals(PermissionOverviewHtmlRenderer.esc("åäö"), "åäö");
  }

  @Test
  public void testHtmlWithRulesDisabledAndEmptyOverview() {
    String html = PermissionOverviewHtmlRenderer.render(overview(false), swedish());
    Assert.assertTrue(html.contains("<p>Inga åtkomstregler är aktiva.</p>"));
    Assert.assertFalse(html.contains("OSL 26:1"), "reglerna ska inte listas när de är avstängda");

    String empty = PermissionOverviewHtmlRenderer.render(new PermissionOverview(), null);
    Assert.assertTrue(empty.contains("<h2>1. Rättigheter per grupp</h2>"), "fallback-texter utan meddelanden");
    Assert.assertTrue(empty.contains("<p>Inga grupper.</p>"));
    Assert.assertTrue(empty.contains("<p>Inga användare.</p>"));
    Assert.assertTrue(empty.contains("<p>Inga strukturenheter på de valda nivåerna.</p>"));
    Assert.assertTrue(empty.contains("<p>Inga åtkomstregler är aktiva.</p>"));
    Assert.assertTrue(empty.contains("<title>Behörighetsöversikt –  – </title>"));

    PermissionOverview enabledButEmpty = new PermissionOverview();
    enabledButEmpty.setRulesEnabled(true);
    String html2 = PermissionOverviewHtmlRenderer.render(enabledButEmpty, swedish());
    Assert.assertTrue(html2.contains("Åtkomstreglerna är aktiva men inga regler är definierade."));
  }

  @Test
  public void testHtmlLangFollowsMessages() {
    Assert.assertTrue(PermissionOverviewHtmlRenderer.render(overview(true), swedish()).contains("<html lang=\"sv\">"));
    Assert.assertTrue(PermissionOverviewHtmlRenderer.render(new PermissionOverview(), null).contains("<html lang=\"sv\">"),
      "fallback utan meddelanden är svenska");
    PermissionOverviewMessages english = (key, fallback) -> "permission.overview.lang".equals(key) ? "en" : fallback;
    Assert.assertTrue(PermissionOverviewHtmlRenderer.render(overview(true), english).contains("<html lang=\"en\">"));
  }

  @Test
  public void testLevelNameFallsBackToCrosswalkTranslation() {
    PermissionOverview o = new PermissionOverview();
    NodePermission sub = new NodePermission();
    sub.setAipId("u1");
    sub.setLevel("subseries");
    sub.setTitle("Underserie X");
    NodePermission grp = new NodePermission();
    grp.setAipId("g1");
    grp.setLevel("recordgrp");
    grp.setTitle("Volym Y");
    NodePermission noLevel = new NodePermission();
    noLevel.setAipId("n1");
    noLevel.setTitle("Utan nivå");
    o.setNodes(Arrays.asList(sub, grp, noLevel));

    Map<String, String> m = new HashMap<>();
    m.put("crosswalks.dissemination.html.ead.level.subseries", "Underserie");
    m.put("crosswalks.dissemination.html.ead.level.recordgrp", "Volym");
    // facetten går före crosswalken, permission.overview.level.* före båda
    m.put("ui.facets.IndexedAIP.level.recordgrp", "Facettvolym");
    PermissionOverviewMessages messages = (key, fallback) -> m.getOrDefault(key, fallback);

    String html = PermissionOverviewHtmlRenderer.render(o, messages);
    Assert.assertTrue(html.contains("<tr><td>Underserie</td><td>Underserie X"), html);
    Assert.assertTrue(html.contains("<tr><td>Facettvolym</td><td>Volym Y"), html);
    Assert.assertTrue(html.contains("<tr><td>–</td><td>Utan nivå"), "nivå saknas ⇒ tomt-markering");
    String csv = PermissionOverviewCsvRenderer.render(o, messages);
    Assert.assertTrue(csv.contains("Underserie;Underserie X;u1;"), csv);
    Assert.assertTrue(csv.contains("Facettvolym;Volym Y;g1;"), csv);
    // helt okänd nivå ⇒ råa nyckeln
    Assert.assertTrue(PermissionOverviewHtmlRenderer.render(o, swedish()).contains("<tr><td>subseries</td>"));
  }

  @Test
  public void testWarningsRenderedAsHtmlBannerAndCsvSection() {
    PermissionOverview o = overview(true);
    o.setWarnings(
      Arrays.asList("Kunde inte lista användare: LDAP <nere>", "Kunde inte hämta strukturenheter: Solr nere"));

    String html = PermissionOverviewHtmlRenderer.render(o, swedish());
    Assert.assertTrue(html.contains("<div class=\"warning\" role=\"alert\">"), html);
    Assert.assertTrue(html.contains("<strong>Varningar – rapporten är ofullständig</strong>"), html);
    Assert.assertTrue(html.contains("<li>Kunde inte lista användare: LDAP &lt;nere&gt;</li>"), "escapad");
    Assert.assertTrue(html.contains("<li>Kunde inte hämta strukturenheter: Solr nere</li>"));
    Assert.assertTrue(html.indexOf("class=\"warning\"") < html.indexOf("<h2>1. "), "bannern före första avsnittet");
    Assert.assertFalse(PermissionOverviewHtmlRenderer.render(overview(true), swedish()).contains("class=\"warning\""),
      "ingen banner utan varningar");

    String csv = PermissionOverviewCsvRenderer.render(o, swedish());
    List<String> lines = Arrays.asList(csv.split("\n"));
    Assert.assertEquals(lines.get(0), "Behörighetsöversikt;" + GENERATED_AT + ";admin");
    Assert.assertEquals(lines.get(1), "");
    Assert.assertEquals(lines.get(2), "Varningar – rapporten är ofullständig", "egen sektion direkt efter titeln");
    Assert.assertEquals(lines.get(3), "Kunde inte lista användare: LDAP <nere>");
    Assert.assertEquals(lines.get(4), "Kunde inte hämta strukturenheter: Solr nere");
    Assert.assertEquals(lines.get(5), "");
    Assert.assertEquals(lines.get(6), "Rättigheter per grupp");
    Assert.assertFalse(PermissionOverviewCsvRenderer.render(overview(true), swedish()).contains("Varningar"),
      "ingen sektion utan varningar");
  }

  // ----------------------------------------------------------------------- CSV

  private static List<CSVRecord> parse(String csv, char delimiter) throws IOException {
    try (CSVParser parser = CSVFormat.DEFAULT.withDelimiter(delimiter).parse(new StringReader(csv))) {
      return new ArrayList<>(parser.getRecords());
    }
  }

  private static CSVRecord find(List<CSVRecord> records, int column, String value) {
    for (CSVRecord record : records) {
      if (record.size() > column && value.equals(record.get(column))) {
        return record;
      }
    }
    throw new AssertionError("ingen rad med '" + value + "' i kolumn " + column);
  }

  @Test
  public void testCsvSectionsRowsAndSemicolonDelimiter() throws IOException {
    String csv = PermissionOverviewCsvRenderer.render(overview(true), swedish());

    Assert.assertEquals(csv.charAt(0), 'B', "ingen BOM (som SearchExportPlugin)");
    List<String> lines = Arrays.asList(csv.split("\n"));
    Assert.assertEquals(lines.get(0), "Behörighetsöversikt;" + GENERATED_AT + ";admin");
    Assert.assertEquals(lines.get(1), "", "tom rad efter titeln");
    Assert.assertEquals(lines.get(2), "Rättigheter per grupp", "sektionsrubrik på egen rad");
    Assert.assertEquals(lines.get(3), "Område;Rättighet;Beskrivning;" + HANDL + ";" + SOC);
    Assert.assertTrue(lines.contains("Användare"), "sektionsrubrik användare");
    Assert.assertTrue(lines.contains("Strukturenheter"));
    Assert.assertTrue(lines.contains("Åtkomstregler"));
    Assert.assertFalse(csv.contains("\r"), "radslut \\n");

    List<CSVRecord> records = parse(csv, ';');
    // matrisrad: område, rättighet, beskrivning, x per grupp
    CSVRecord aipRead = find(records, 1, "aip.read");
    Assert.assertEquals(aipRead.get(0), "Strukturenheter (AIP)");
    Assert.assertEquals(aipRead.get(2), "Lista och sök efter strukturenheter (AIP)");
    Assert.assertEquals(aipRead.get(3), PermissionOverviewCsvRenderer.MARK);
    Assert.assertEquals(aipRead.get(4), PermissionOverviewCsvRenderer.MARK);
    CSVRecord memberRead = find(records, 1, "member.read");
    Assert.assertEquals(memberRead.get(0), "member", "område utan översättning ⇒ nyckeln");
    Assert.assertEquals(memberRead.get(2), "", "ingen beskrivning ⇒ tomt");
    Assert.assertEquals(memberRead.get(3), "");
    Assert.assertEquals(memberRead.get(4), PermissionOverviewCsvRenderer.MARK);
    // aktiv-raden i matrisen
    CSVRecord active = find(records, 1, "Aktiv");
    Assert.assertEquals(active.get(3), "Ja");
    Assert.assertEquals(active.get(4), "Nej");

    // användare: 7 kolumner, flervärden med ", "
    CSVRecord anna = find(records, 0, "anna");
    Assert.assertEquals(anna.size(), 7);
    Assert.assertEquals(anna.get(1), "Svensson; Anna");
    Assert.assertEquals(anna.get(2), "anna@example.org");
    Assert.assertEquals(anna.get(3), "Ja");
    Assert.assertEquals(anna.get(4), SOC);
    Assert.assertEquals(anna.get(6), "aip.read, aip.update, job.read, member.read");
    CSVRecord cecilia = find(records, 0, "cecilia");
    Assert.assertEquals(cecilia.get(1), "Cecilia \"C\"");
    Assert.assertEquals(cecilia.get(3), "Nej");

    // strukturenheter: nivå, titel, id, överordnad, 5 × (grupper, användare), spärrade
    CSVRecord f1 = find(records, 2, "f1");
    Assert.assertEquals(f1.size(), 4 + 2 * 5 + 1);
    Assert.assertEquals(f1.get(0), "Arkivbestånd");
    Assert.assertEquals(f1.get(1), EVIL_TITLE, "CSV escapar inte HTML");
    Assert.assertEquals(f1.get(3), "");
    CSVRecord nodeHeader = find(records, 2, "Identifierare");
    Assert.assertEquals(nodeHeader.get(6), "Läsa – grupper");
    Assert.assertEquals(nodeHeader.get(7), "Läsa – användare");
    Assert.assertEquals(f1.get(6), HANDL + ", " + SOC);
    Assert.assertEquals(f1.get(7), "anna");
    Assert.assertEquals(f1.get(12), SOC, "GRANT – grupper");
    Assert.assertEquals(f1.get(14), HANDL, "spärrade grupper");
    CSVRecord c1 = find(records, 2, "c1");
    Assert.assertEquals(c1.get(0), "Klass");
    Assert.assertEquals(c1.get(3), "f1");
    Assert.assertEquals(c1.get(14), "");

    // regler
    CSVRecord rule = find(records, 0, HANDL);
    Assert.assertEquals(rule.size(), 7);
    Assert.assertEquals(rule.get(1), "Ja");
    Assert.assertEquals(rule.get(2), "ingen_sekretess");
    Assert.assertEquals(rule.get(3), "OSL 26:1, OSL 26:3");
    Assert.assertEquals(rule.get(4), "*");
    Assert.assertEquals(rule.get(5), "inga, vanliga");
    Assert.assertEquals(rule.get(6), "Han sa \"hej\"; sedan gick han");
  }

  @Test
  public void testCsvEscapesDelimiterAndQuotes() {
    String csv = PermissionOverviewCsvRenderer.render(overview(true), swedish());
    // fält med avgränsare citeras, citattecken dubbleras (RFC 4180)
    Assert.assertTrue(csv.contains("\"Svensson; Anna\""), csv);
    Assert.assertTrue(csv.contains("\"Cecilia \"\"C\"\"\""), csv);
    Assert.assertTrue(csv.contains("\"Han sa \"\"hej\"\"; sedan gick han\""), csv);
    Assert.assertTrue(csv.contains("anna;\"Svensson; Anna\";anna@example.org;Ja;" + SOC + ";job.read;"), csv);
  }

  @Test
  public void testCsvNeutralisesFormulaPrefixes() throws IOException {
    PermissionOverview o = overview(true);
    o.getNodes().get(0).setTitle("=SUM(1)");
    o.getRules().get(0).setNote("@cmd");
    o.getUsers().get(0).setFullName("+46 70 123");
    o.getUsers().get(0).setEmail("  -anna@example.org");
    o.getUsers().get(1).setFullName("\t|calc");
    o.getGroups().get(0).setName("@handl");

    String csv = PermissionOverviewCsvRenderer.render(o, swedish());
    List<CSVRecord> records = parse(csv, ';');
    CSVRecord f1 = find(records, 2, "f1");
    Assert.assertEquals(f1.get(1), "'=SUM(1)", "AIP-titel");
    CSVRecord rule = find(records, 0, HANDL);
    Assert.assertEquals(rule.get(6), "'@cmd", "regelns anteckning");
    CSVRecord anna = find(records, 0, "anna");
    Assert.assertEquals(anna.get(1), "'+46 70 123", "fullständigt namn");
    Assert.assertEquals(anna.get(2), "'  -anna@example.org", "inledande blanksteg skyddar inte");
    CSVRecord cecilia = find(records, 0, "cecilia");
    Assert.assertEquals(cecilia.get(1), "'\t|calc", "tabb");
    CSVRecord matrixHeader = find(records, 1, "Rättighet");
    Assert.assertEquals(matrixHeader.get(3), "'@handl", "gruppnamn i rubrikraden är användarstyrda");
    Assert.assertEquals(matrixHeader.get(0), "Område", "etiketter rörs inte");
    // ofarliga värden rörs inte
    Assert.assertEquals(anna.get(0), "anna");
    Assert.assertEquals(f1.get(0), "Arkivbestånd");
    Assert.assertEquals(f1.get(6), HANDL + ", " + SOC);

    Assert.assertEquals(PermissionOverviewCsvRenderer.safe("=1+1"), "'=1+1");
    Assert.assertEquals(PermissionOverviewCsvRenderer.safe("+1"), "'+1");
    Assert.assertEquals(PermissionOverviewCsvRenderer.safe("-1"), "'-1");
    Assert.assertEquals(PermissionOverviewCsvRenderer.safe("@x"), "'@x");
    Assert.assertEquals(PermissionOverviewCsvRenderer.safe("\tx"), "'\tx");
    Assert.assertEquals(PermissionOverviewCsvRenderer.safe("\rx"), "'\rx");
    Assert.assertEquals(PermissionOverviewCsvRenderer.safe(" =x"), "' =x");
    Assert.assertEquals(PermissionOverviewCsvRenderer.safe("Arkiv"), "Arkiv");
    Assert.assertEquals(PermissionOverviewCsvRenderer.safe("a=b"), "a=b");
    Assert.assertEquals(PermissionOverviewCsvRenderer.safe("   "), "   ");
    Assert.assertEquals(PermissionOverviewCsvRenderer.safe(""), "");
    Assert.assertEquals(PermissionOverviewCsvRenderer.safe(null), "");
  }

  @Test
  public void testCsvWithCommaDelimiterAndDelimiterConfig() throws IOException {
    String csv = PermissionOverviewCsvRenderer.render(overview(true), swedish(), ',');
    List<String> lines = Arrays.asList(csv.split("\n"));
    Assert.assertEquals(lines.get(0), "Behörighetsöversikt," + GENERATED_AT + ",admin");
    Assert.assertEquals(lines.get(3), "Område,Rättighet,Beskrivning," + HANDL + "," + SOC);
    // nu är det ", " i flervärdesfälten som måste citeras, men semikolon inte
    Assert.assertTrue(csv.contains("\"aip.read, aip.update, job.read, member.read\""), csv);
    Assert.assertTrue(csv.contains(",Svensson; Anna,"), csv);
    List<CSVRecord> records = parse(csv, ',');
    Assert.assertEquals(find(records, 0, "anna").get(6), "aip.read, aip.update, job.read, member.read");

    Assert.assertEquals(PermissionOverviewCsvRenderer.delimiterOf(null), ';');
    Assert.assertEquals(PermissionOverviewCsvRenderer.delimiterOf(" "), ';');
    Assert.assertEquals(PermissionOverviewCsvRenderer.delimiterOf(","), ',');
    Assert.assertEquals(PermissionOverviewCsvRenderer.delimiterOf(" \t;"), ';');
  }

  @Test
  public void testCsvWithRulesDisabledAndEmptyOverview() {
    String csv = PermissionOverviewCsvRenderer.render(overview(false), swedish());
    Assert.assertTrue(csv.endsWith("Åtkomstregler\nInga åtkomstregler är aktiva.\n"), csv);
    Assert.assertFalse(csv.contains("OSL 26:1"));

    String empty = PermissionOverviewCsvRenderer.render(null, null);
    Assert.assertTrue(empty.startsWith("Behörighetsöversikt;;\n\nRättigheter per grupp\nInga grupper.\n"), empty);
    Assert.assertTrue(empty.contains("\nAnvändare\nInga användare.\n"));
    Assert.assertTrue(empty.contains("\nStrukturenheter\nInga strukturenheter på de valda nivåerna.\n"));
  }
}

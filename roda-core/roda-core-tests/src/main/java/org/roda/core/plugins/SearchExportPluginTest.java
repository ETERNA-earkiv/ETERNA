package org.roda.core.plugins;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.roda.core.data.common.RodaConstants;
import org.roda.core.plugins.base.preservation.SearchExportPlugin;
import org.testng.annotations.Test;

@Test(groups = {RodaConstants.TEST_GROUP_ALL, RodaConstants.TEST_GROUP_DEV})
public class SearchExportPluginTest {

  @Test
  public void testParameterParsingWithAllFields() throws Exception {
    SearchExportPlugin plugin = new SearchExportPlugin();
    Map<String, String> params = new HashMap<>();
    params.put(SearchExportPlugin.PARAM_FILTER, "{\"filterParameters\":[]}");
    params.put(SearchExportPlugin.PARAM_FIELDS, "uuid,title,level");
    params.put(SearchExportPlugin.PARAM_FILENAME, "myexport");
    plugin.setParameterValues(params);

    assertEquals("myexport", plugin.getExportFilename());
    assertEquals(java.util.Arrays.asList("uuid", "title", "level"), plugin.getExportFields());
  }

  @Test
  public void testParameterParsingDefaultFilename() throws Exception {
    SearchExportPlugin plugin = new SearchExportPlugin();
    Map<String, String> params = new HashMap<>();
    params.put(SearchExportPlugin.PARAM_FILTER, "{\"filterParameters\":[]}");
    params.put(SearchExportPlugin.PARAM_FIELDS, "uuid,title");
    plugin.setParameterValues(params);

    assertTrue(plugin.getExportFilename().startsWith("export_"));
  }

  @Test
  public void testBuildCsvRow() {
    org.roda.core.data.v2.ip.IndexedAIP aip = new org.roda.core.data.v2.ip.IndexedAIP();
    aip.setId("aip-123");
    aip.setTitle("Testarkiv");

    java.util.List<String> row = SearchExportPlugin.buildCsvRow(aip,
      java.util.Arrays.asList("uuid", "title", "level"));

    assertEquals("aip-123", row.get(0));
    assertEquals("Testarkiv", row.get(1));
    assertEquals("", row.get(2)); // level är null → tom sträng
  }

  @Test
  public void testPluginTypeIsInternal() {
    SearchExportPlugin plugin = new SearchExportPlugin();
    assertEquals(org.roda.core.data.v2.jobs.PluginType.INTERNAL, plugin.getType());
  }
}

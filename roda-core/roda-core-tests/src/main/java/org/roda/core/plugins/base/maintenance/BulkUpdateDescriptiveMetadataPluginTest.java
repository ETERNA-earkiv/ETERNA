/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/keeps/roda
 */
package org.roda.core.plugins.base.maintenance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.exceptions.InvalidParameterException;
import org.roda.core.data.v2.disposal.metadata.DisposalAIPMetadata;
import org.roda.core.data.v2.disposal.metadata.DisposalConfirmationAIPMetadata;
import org.roda.core.data.v2.ip.AIP;
import org.roda.core.data.v2.ip.AIPState;
import org.roda.core.data.v2.jobs.PluginType;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Massredigeringen är en användarinitierad
 * arkivvårdsåtgärd. Jobben ska därför listas under Bevarande → Åtgärder
 * (ActionProcess filtrerar bort INTERNAL och INGEST) och pluginet ska kunna
 * väljas i "Nytt jobb" (CreateDefaultJob utesluter INTERNAL). Med typen
 * INTERNAL hamnade jobben enbart under Administration → Interna åtgärder och
 * uppfattades som att inget jobb skapats.
 */
@Test(groups = {RodaConstants.TEST_GROUP_ALL, RodaConstants.TEST_GROUP_DEV, RodaConstants.TEST_GROUP_TRAVIS})
public class BulkUpdateDescriptiveMetadataPluginTest {

  @Test
  public void jobTypeIsAipToAipSoThatJobsAppearUnderPreservationActions() {
    BulkUpdateDescriptiveMetadataPlugin plugin = new BulkUpdateDescriptiveMetadataPlugin();
    Assert.assertEquals(plugin.getType(), PluginType.AIP_TO_AIP,
      "Massredigering ska vara en arkivvårdsåtgärd (AIP_TO_AIP), inte en intern åtgärd");
  }

  @Test
  public void isCategorisedAsMaintenanceAndOperatesOnAips() {
    BulkUpdateDescriptiveMetadataPlugin plugin = new BulkUpdateDescriptiveMetadataPlugin();
    Assert.assertEquals(plugin.getCategories(), List.of(RodaConstants.PLUGIN_CATEGORY_MAINTENANCE));
    Assert.assertEquals(plugin.getObjectClasses(), List.of(AIP.class));
  }

  @Test
  public void cloneKeepsType() {
    BulkUpdateDescriptiveMetadataPlugin plugin = new BulkUpdateDescriptiveMetadataPlugin();
    Assert.assertEquals(plugin.cloneMe().getType(), plugin.getType());
  }

  @Test
  public void blankNewValueIsInvalid() throws InvalidParameterException {
    Assert.assertFalse(pluginWithNewValue("").areParameterValuesValid());
    Assert.assertFalse(pluginWithNewValue("   ").areParameterValuesValid());
    Assert.assertFalse(pluginWithNewValue(null).areParameterValuesValid());
    Assert.assertTrue(pluginWithNewValue("Ny titel").areParameterValuesValid());
  }

  @Test
  public void onlyActiveAipsWithoutDisposalConfirmationAreEditable() {
    AIP active = new AIP();
    active.setState(AIPState.ACTIVE);
    Assert.assertNull(BulkUpdateDescriptiveMetadataPlugin.notEditableReason(active));

    for (AIPState state : AIPState.values()) {
      if (state != AIPState.ACTIVE) {
        AIP aip = new AIP();
        aip.setState(state);
        Assert.assertNotNull(BulkUpdateDescriptiveMetadataPlugin.notEditableReason(aip), state.name());
      }
    }

    DisposalConfirmationAIPMetadata confirmation = new DisposalConfirmationAIPMetadata();
    confirmation.setId("confirmation-1");
    DisposalAIPMetadata disposal = new DisposalAIPMetadata();
    disposal.setConfirmation(confirmation);
    AIP underConfirmation = new AIP();
    underConfirmation.setState(AIPState.ACTIVE);
    underConfirmation.setDisposal(disposal);
    Assert.assertNotNull(BulkUpdateDescriptiveMetadataPlugin.notEditableReason(underConfirmation));
  }

  private static BulkUpdateDescriptiveMetadataPlugin pluginWithNewValue(String newValue)
    throws InvalidParameterException {
    Map<String, String> parameters = new HashMap<>();
    parameters.put(RodaConstants.PLUGIN_PARAMS_BULK_METADATA_TYPE, "ead_3");
    parameters.put(RodaConstants.PLUGIN_PARAMS_BULK_FIELD_NAME, "title");
    parameters.put(RodaConstants.PLUGIN_PARAMS_BULK_NEW_VALUE, newValue);
    BulkUpdateDescriptiveMetadataPlugin plugin = new BulkUpdateDescriptiveMetadataPlugin();
    plugin.setParameterValues(parameters);
    return plugin;
  }
}

/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.data.v2.ip;

import java.util.ArrayList;
import java.util.List;

import org.roda.core.data.common.RodaConstants;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = {RodaConstants.TEST_GROUP_ALL, RodaConstants.TEST_GROUP_DEV, RodaConstants.TEST_GROUP_TRAVIS})
public class IndexedAIPTest {

  @Test
  public void copyConstructorKeepsAccessRuleRestrictedGroups() {
    IndexedAIP original = new IndexedAIP();
    original.setId("aip-1");
    original.setAccessRuleRestrictedGroups(new ArrayList<>(List.of("handlaggare")));

    IndexedAIP copy = new IndexedAIP(original);

    Assert.assertEquals(copy.getAccessRuleRestrictedGroups(), List.of("handlaggare"));
    Assert.assertEquals(copy, original);
    // kopian delar inte lista med originalet
    original.getAccessRuleRestrictedGroups().add("arkivarier");
    Assert.assertEquals(copy.getAccessRuleRestrictedGroups(), List.of("handlaggare"));
  }
}

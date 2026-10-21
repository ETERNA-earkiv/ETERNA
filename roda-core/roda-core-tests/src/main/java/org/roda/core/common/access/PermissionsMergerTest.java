/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.common.access;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.v2.accessrules.AccessRuleState;
import org.roda.core.data.v2.ip.Permissions;
import org.roda.core.data.v2.ip.Permissions.PermissionType;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tester för sammanslagningen av regelbeslut med
 * AIP-behörigheter — bara hanterade grupper rörs, användare och övriga
 * behörighetstyper bevaras, grupper utan READ och utan restricted rörs inte,
 * återställning ur restrictedGroups, idempotens, orörd indata och varning när
 * READ tas bort men UPDATE finns kvar.
 */
@Test(groups = {RodaConstants.TEST_GROUP_ALL, RodaConstants.TEST_GROUP_DEV, RodaConstants.TEST_GROUP_TRAVIS,
  "accessrules"})
public class PermissionsMergerTest {

  private static final String SOC = "socialtjanst";
  private static final String HANDL = "handlaggare";
  private static final String ADMIN = "administrators"; // ej hanterad
  private static final String USER = "anna";
  private static final String VERSION = "3";
  private static final String NOW = "2026-10-08T12:00:00Z";

  // ---------------------------------------------------------------- hjälpare

  private static Permissions basePermissions() {
    Permissions p = new Permissions();
    p.setGroupPermissions(SOC, perms(PermissionType.READ, PermissionType.UPDATE));
    p.setGroupPermissions(HANDL, perms(PermissionType.READ));
    p.setGroupPermissions(ADMIN,
      perms(PermissionType.CREATE, PermissionType.READ, PermissionType.UPDATE, PermissionType.DELETE,
        PermissionType.GRANT));
    p.setUserPermissions(USER, perms(PermissionType.READ, PermissionType.UPDATE, PermissionType.GRANT));
    return p;
  }

  private static Set<PermissionType> perms(PermissionType... types) {
    return new HashSet<>(Arrays.asList(types));
  }

  private static AccessDecision decision(Set<String> managed, Set<String> allowed) {
    AccessDecision d = new AccessDecision();
    d.setManagedGroups(managed);
    d.setAllowedGroups(allowed);
    return d;
  }

  private static Set<String> set(String... values) {
    return new LinkedHashSet<>(Arrays.asList(values));
  }

  private static AccessRuleState state(String... restricted) {
    AccessRuleState s = new AccessRuleState();
    s.setRestrictedGroups(set(restricted));
    s.setRulesVersion("1");
    s.setAppliedOn("2026-01-01T00:00:00Z");
    s.setFactsSource("self");
    return s;
  }

  /** Oberoende ögonblicksbild av mapparna för att upptäcka mutation. */
  private static Map<PermissionType, Set<String>> snapshot(Map<PermissionType, Set<String>> source) {
    Map<PermissionType, Set<String>> copy = new EnumMap<>(PermissionType.class);
    for (Map.Entry<PermissionType, Set<String>> e : source.entrySet()) {
      copy.put(e.getKey(), new HashSet<>(e.getValue()));
    }
    return copy;
  }

  private static Set<String> members(Permissions p, PermissionType type) {
    return p.getGroups().get(type);
  }

  // ---------------------------------------------------------------- tester

  @Test
  public void testOnlyManagedGroupsReadIsTouched() {
    Permissions current = basePermissions();
    MergeResult result = PermissionsMerger.apply(current, null, decision(set(SOC, HANDL), set(SOC)), VERSION, NOW);

    Assert.assertTrue(result.isChanged());
    Assert.assertEquals(result.getRemoved(), set(HANDL));
    Assert.assertTrue(result.getAdded().isEmpty());

    Permissions merged = result.getPermissions();
    Assert.assertFalse(members(merged, PermissionType.READ).contains(HANDL), "HANDL ska ha förlorat READ");
    Assert.assertTrue(members(merged, PermissionType.READ).contains(SOC), "SOC behåller READ");
    Assert.assertTrue(members(merged, PermissionType.READ).contains(ADMIN), "ej hanterad grupp behåller READ");
    Assert.assertEquals(merged.getGroupPermissions(ADMIN), current.getGroupPermissions(ADMIN));
    Assert.assertEquals(merged.getGroupPermissions(SOC), perms(PermissionType.READ, PermissionType.UPDATE));

    Assert.assertEquals(result.getState().getRestrictedGroups(), set(HANDL));
    Assert.assertEquals(result.getState().getRulesVersion(), VERSION);
    Assert.assertEquals(result.getState().getAppliedOn(), NOW);
  }

  @Test
  public void testUsersAreNeverTouched() {
    Permissions current = basePermissions();
    current.setUserPermissions(SOC, perms(PermissionType.READ)); // användare med samma namn som grupp

    MergeResult result = PermissionsMerger.apply(current, null, decision(set(SOC, HANDL), set()), VERSION, NOW);

    Assert.assertEquals(result.getPermissions().getUsers(), current.getUsers());
    Assert.assertEquals(result.getPermissions().getUserPermissions(USER),
      perms(PermissionType.READ, PermissionType.UPDATE, PermissionType.GRANT));
    Assert.assertEquals(result.getPermissions().getUserPermissions(SOC), perms(PermissionType.READ));
    Assert.assertEquals(result.getRemoved(), set(SOC, HANDL));
  }

  @Test
  public void testOtherPermissionTypesPreservedAndWarningWhenWriteRetained() {
    Permissions current = basePermissions();
    current.setGroupPermissions(HANDL, perms(PermissionType.READ, PermissionType.DELETE, PermissionType.GRANT));

    MergeResult result = PermissionsMerger.apply(current, null, decision(set(SOC, HANDL), set()), VERSION, NOW);

    Permissions merged = result.getPermissions();
    Assert.assertEquals(merged.getGroupPermissions(SOC), perms(PermissionType.UPDATE));
    Assert.assertEquals(merged.getGroupPermissions(HANDL), perms(PermissionType.DELETE, PermissionType.GRANT));
    Assert.assertEquals(result.getRemoved(), set(SOC, HANDL));

    Assert.assertEquals(result.getWarnings().size(), 2, result.getWarnings().toString());
    Assert.assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains(SOC) && w.contains("UPDATE")),
      result.getWarnings().toString());
    Assert.assertTrue(
      result.getWarnings().stream().anyMatch(w -> w.contains(HANDL) && w.contains("DELETE") && w.contains("GRANT")),
      result.getWarnings().toString());
  }

  @Test
  public void testNoWarningWhenGroupOnlyHadRead() {
    Permissions current = new Permissions();
    current.setGroupPermissions(HANDL, perms(PermissionType.READ));

    MergeResult result = PermissionsMerger.apply(current, null, decision(set(HANDL), set()), VERSION, NOW);

    Assert.assertEquals(result.getRemoved(), set(HANDL));
    Assert.assertTrue(result.getWarnings().isEmpty(), result.getWarnings().toString());
    Assert.assertTrue(result.getPermissions().getGroupPermissions(HANDL).isEmpty());
    Assert.assertFalse(result.getPermissions().getGroupnames().contains(HANDL));
  }

  @Test
  public void testManagedGroupWithoutReadAndNotRestrictedIsNotTouched() {
    Permissions current = new Permissions();
    current.setGroupPermissions(SOC, perms(PermissionType.UPDATE)); // ingen READ
    // HANDL finns inte alls

    MergeResult result = PermissionsMerger.apply(current, null, decision(set(SOC, HANDL), set(SOC, HANDL)), VERSION,
      NOW);

    Assert.assertFalse(result.isChanged());
    Assert.assertTrue(result.getAdded().isEmpty());
    Assert.assertTrue(result.getRemoved().isEmpty());
    Assert.assertFalse(members(result.getPermissions(), PermissionType.READ).contains(SOC),
      "reglerna ger aldrig READ");
    Assert.assertFalse(members(result.getPermissions(), PermissionType.READ).contains(HANDL));
    Assert.assertEquals(result.getPermissions().getGroupPermissions(SOC), perms(PermissionType.UPDATE));
    Assert.assertTrue(result.getState().getRestrictedGroups().isEmpty());
  }

  @Test
  public void testRestoreFromRestrictedGroups() {
    Permissions current = new Permissions();
    current.setGroupPermissions(SOC, perms(PermissionType.UPDATE)); // READ borttagen tidigare
    AccessRuleState previous = state(SOC, HANDL);

    MergeResult result = PermissionsMerger.apply(current, previous, decision(set(SOC, HANDL), set(SOC)), VERSION,
      NOW);

    Assert.assertTrue(result.isChanged());
    Assert.assertEquals(result.getAdded(), set(SOC));
    Assert.assertTrue(result.getRemoved().isEmpty());
    Assert.assertEquals(result.getPermissions().getGroupPermissions(SOC),
      perms(PermissionType.READ, PermissionType.UPDATE));
    // HANDL är fortfarande nekad och hade ingen READ: förblir restricted
    Assert.assertFalse(members(result.getPermissions(), PermissionType.READ).contains(HANDL));
    Assert.assertEquals(result.getState().getRestrictedGroups(), set(HANDL));
    Assert.assertEquals(result.getState().getFactsSource(), "self", "factsSource bevaras från föregående tillstånd");
    // indata-tillståndet är orört
    Assert.assertEquals(previous.getRestrictedGroups(), set(SOC, HANDL));
    Assert.assertEquals(previous.getRulesVersion(), "1");
  }

  @Test
  public void testIdempotentSecondRunIsUnchanged() {
    Permissions current = basePermissions();
    AccessDecision decision = decision(set(SOC, HANDL), set(SOC));

    MergeResult first = PermissionsMerger.apply(current, null, decision, VERSION, NOW);
    Assert.assertTrue(first.isChanged());

    MergeResult second = PermissionsMerger.apply(first.getPermissions(), first.getState(), decision, "4",
      "2026-10-09T00:00:00Z");

    Assert.assertFalse(second.isChanged());
    Assert.assertTrue(second.getAdded().isEmpty());
    Assert.assertTrue(second.getRemoved().isEmpty());
    Assert.assertTrue(second.getWarnings().isEmpty(), second.getWarnings().toString());
    Assert.assertEquals(second.getPermissions(), first.getPermissions());
    Assert.assertEquals(second.getState().getRestrictedGroups(), first.getState().getRestrictedGroups());
    // stämpeln uppdateras alltid, även utan ändring
    Assert.assertEquals(second.getState().getRulesVersion(), "4");
    Assert.assertEquals(second.getState().getAppliedOn(), "2026-10-09T00:00:00Z");
  }

  @Test
  public void testInputIsNotMutatedAndResultIsIndependentCopy() {
    Permissions current = basePermissions();
    Map<PermissionType, Set<String>> groupsBefore = snapshot(current.getGroups());
    Map<PermissionType, Set<String>> usersBefore = snapshot(current.getUsers());
    AccessRuleState previous = state(HANDL);
    Set<String> restrictedBefore = new LinkedHashSet<>(previous.getRestrictedGroups());

    MergeResult result = PermissionsMerger.apply(current, previous, decision(set(SOC, HANDL), set(HANDL)), VERSION,
      NOW);

    Assert.assertTrue(result.isChanged());
    Assert.assertEquals(current.getGroups(), groupsBefore, "indatans grupper muterades");
    Assert.assertEquals(current.getUsers(), usersBefore, "indatans användare muterades");
    Assert.assertEquals(previous.getRestrictedGroups(), restrictedBefore, "indatans tillstånd muterades");
    Assert.assertNotSame(result.getPermissions(), current);
    Assert.assertNotSame(result.getState(), previous);

    // ändringar i resultatet läcker inte tillbaka
    result.getPermissions().getGroups().get(PermissionType.READ).add("ny_grupp");
    result.getPermissions().getUsers().get(PermissionType.READ).add("ny_anvandare");
    result.getState().getRestrictedGroups().add("ny_grupp");
    Assert.assertEquals(current.getGroups(), groupsBefore);
    Assert.assertEquals(current.getUsers(), usersBefore);
    Assert.assertEquals(previous.getRestrictedGroups(), restrictedBefore);
  }

  @Test
  public void testDeepCopyPreservesEverythingAndNullGivesEmpty() {
    Permissions current = basePermissions();
    Permissions copy = PermissionsMerger.deepCopy(current);

    Assert.assertEquals(copy, current);
    Assert.assertNotSame(copy.getGroups(), current.getGroups());
    Assert.assertNotSame(copy.getGroups().get(PermissionType.READ), current.getGroups().get(PermissionType.READ));

    Permissions empty = PermissionsMerger.deepCopy(null);
    Assert.assertEquals(empty, new Permissions());
    for (PermissionType type : PermissionType.values()) {
      Assert.assertTrue(empty.getGroups().get(type).isEmpty());
      Assert.assertTrue(empty.getUsers().get(type).isEmpty());
    }
  }

  @Test
  public void testNullInputsGiveEmptyUnchangedResult() {
    MergeResult result = PermissionsMerger.apply(null, null, null, VERSION, NOW);

    Assert.assertFalse(result.isChanged());
    Assert.assertEquals(result.getPermissions(), new Permissions());
    Assert.assertTrue(result.getState().getRestrictedGroups().isEmpty());
    Assert.assertEquals(result.getState().getRulesVersion(), VERSION);
    Assert.assertEquals(result.getState().getAppliedOn(), NOW);
    Assert.assertNull(result.getState().getFactsSource());
  }

  @Test
  public void testRestrictedGroupWithoutRuleIsLeftAloneWithWarning() {
    Permissions current = new Permissions();
    current.setGroupPermissions(ADMIN, perms(PermissionType.READ));
    AccessRuleState previous = state(HANDL); // HANDL har inte längre någon regel

    MergeResult result = PermissionsMerger.apply(current, previous, decision(set(SOC), set(SOC)), VERSION, NOW);

    Assert.assertFalse(result.isChanged());
    Assert.assertFalse(members(result.getPermissions(), PermissionType.READ).contains(HANDL));
    Assert.assertEquals(result.getState().getRestrictedGroups(), set(HANDL), "restricted bevaras för framtida regel");
    Assert.assertEquals(result.getWarnings().size(), 1, result.getWarnings().toString());
    Assert.assertTrue(result.getWarnings().get(0).contains(HANDL), result.getWarnings().toString());
  }

  @Test
  public void testStateChangeWithoutPermissionChangeCountsAsChanged() {
    // grupp i restricted som nu tillåts men inte hade READ: READ läggs till
    // (added) – det är den normala vägen. Här: grupp som har READ, är tillåten
    // men felaktigt ligger kvar i restricted ⇒ bara tillståndet ändras.
    Permissions current = new Permissions();
    current.setGroupPermissions(SOC, perms(PermissionType.READ));
    AccessRuleState previous = state(SOC);

    MergeResult result = PermissionsMerger.apply(current, previous, decision(set(SOC), set(SOC)), VERSION, NOW);

    Assert.assertTrue(result.isChanged(), "restrictedGroups ändrades");
    Assert.assertTrue(result.getAdded().isEmpty());
    Assert.assertTrue(result.getRemoved().isEmpty());
    Assert.assertTrue(result.getState().getRestrictedGroups().isEmpty());
    Assert.assertEquals(result.getPermissions(), current);
  }

  @Test
  public void testPermissionsFromJsonWithMissingTypeKeys() {
    // Efter JSON-inläsning kan mappar sakna nycklar; mergern ska klara det
    Permissions current = new Permissions();
    Map<PermissionType, Set<String>> groups = new EnumMap<>(PermissionType.class);
    groups.put(PermissionType.READ, new HashSet<>(Arrays.asList(SOC, HANDL)));
    current.setGroups(groups);
    current.setUsers(new EnumMap<>(PermissionType.class));

    MergeResult result = PermissionsMerger.apply(current, null, decision(set(SOC, HANDL), set(SOC)), VERSION, NOW);

    Assert.assertEquals(result.getRemoved(), set(HANDL));
    Assert.assertEquals(result.getPermissions().getGroupPermissions(SOC), perms(PermissionType.READ));
    Assert.assertTrue(result.getPermissions().getGroupPermissions(HANDL).isEmpty());
    Assert.assertTrue(result.getWarnings().isEmpty(), result.getWarnings().toString());
    for (PermissionType type : PermissionType.values()) {
      Assert.assertNotNull(result.getPermissions().getGroups().get(type));
      Assert.assertNotNull(result.getPermissions().getUsers().get(type));
    }
  }

  // ---------------------------------------------------------------- stripWriteOnDeny

  @Test
  public void testStripWriteOnDenyRemovesWriteTypesAndRecordsThem() {
    Permissions current = basePermissions();
    current.setGroupPermissions(SOC,
      perms(PermissionType.CREATE, PermissionType.READ, PermissionType.UPDATE, PermissionType.GRANT));

    MergeResult result = PermissionsMerger.apply(current, null, decision(set(SOC, HANDL), set()), VERSION, NOW, true);

    Assert.assertTrue(result.isChanged());
    Assert.assertEquals(result.getRemoved(), set(SOC, HANDL));
    Assert.assertEquals(result.getWriteStripped(), set(SOC));
    Assert.assertTrue(result.getWriteRestored().isEmpty());
    Assert.assertTrue(result.getWarnings().isEmpty(), "inget att varna för när skrivtyperna tas bort");

    Permissions merged = result.getPermissions();
    Assert.assertEquals(merged.getGroupPermissions(SOC), perms(PermissionType.CREATE), "CREATE rörs aldrig");
    Assert.assertTrue(merged.getGroupPermissions(HANDL).isEmpty());
    Assert.assertEquals(merged.getGroupPermissions(ADMIN), current.getGroupPermissions(ADMIN));
    Assert.assertEquals(merged.getUsers(), current.getUsers());

    Assert.assertEquals(result.getState().getRestrictedGroups(), set(SOC, HANDL));
    Assert.assertEquals(result.getState().getRemovedPermissions().keySet(), set(SOC));
    Assert.assertEquals(result.getState().getRemovedPermissions().get(SOC), set("UPDATE", "GRANT"));
    // indata orörd
    Assert.assertEquals(current.getGroupPermissions(SOC),
      perms(PermissionType.CREATE, PermissionType.READ, PermissionType.UPDATE, PermissionType.GRANT));
  }

  @Test
  public void testStripRestoresWriteTypesWhenAllowedAgainAndIsIdempotent() {
    Permissions current = basePermissions(); // SOC: READ+UPDATE
    MergeResult denied = PermissionsMerger.apply(current, null, decision(set(SOC, HANDL), set()), VERSION, NOW, true);
    Assert.assertEquals(denied.getPermissions().getGroupPermissions(SOC), perms());

    MergeResult allowed = PermissionsMerger.apply(denied.getPermissions(), denied.getState(),
      decision(set(SOC, HANDL), set(SOC)), "4", NOW, true);

    Assert.assertTrue(allowed.isChanged());
    Assert.assertEquals(allowed.getAdded(), set(SOC));
    Assert.assertEquals(allowed.getWriteRestored(), set(SOC));
    Assert.assertEquals(allowed.getPermissions().getGroupPermissions(SOC),
      perms(PermissionType.READ, PermissionType.UPDATE));
    Assert.assertEquals(allowed.getState().getRestrictedGroups(), set(HANDL));
    Assert.assertTrue(allowed.getState().getRemovedPermissions().isEmpty());

    MergeResult again = PermissionsMerger.apply(allowed.getPermissions(), allowed.getState(),
      decision(set(SOC, HANDL), set(SOC)), "4", NOW, true);
    Assert.assertFalse(again.isChanged());
    Assert.assertEquals(again.getPermissions(), allowed.getPermissions());
  }

  @Test
  public void testStripDeniedGroupWithOnlyWritePermissionsNeverGainsRead() {
    Permissions current = new Permissions();
    current.setGroupPermissions(SOC, perms(PermissionType.UPDATE, PermissionType.GRANT)); // ingen READ

    MergeResult denied = PermissionsMerger.apply(current, null, decision(set(SOC), set()), VERSION, NOW, true);

    Assert.assertTrue(denied.isChanged());
    Assert.assertTrue(denied.getRemoved().isEmpty(), "ingen READ att ta bort");
    Assert.assertEquals(denied.getWriteStripped(), set(SOC));
    Assert.assertTrue(denied.getPermissions().getGroupPermissions(SOC).isEmpty());
    Assert.assertTrue(denied.getState().getRestrictedGroups().isEmpty(), "READ togs aldrig bort");
    Assert.assertEquals(denied.getState().getRemovedPermissions().get(SOC), set("UPDATE", "GRANT"));

    MergeResult allowed = PermissionsMerger.apply(denied.getPermissions(), denied.getState(),
      decision(set(SOC), set(SOC)), VERSION, NOW, true);

    Assert.assertTrue(allowed.isChanged());
    Assert.assertTrue(allowed.getAdded().isEmpty(), "reglerna ger aldrig READ");
    Assert.assertEquals(allowed.getWriteRestored(), set(SOC));
    Assert.assertEquals(allowed.getPermissions().getGroupPermissions(SOC),
      perms(PermissionType.UPDATE, PermissionType.GRANT));
    Assert.assertTrue(allowed.getState().getRemovedPermissions().isEmpty());
  }

  @Test
  public void testStripAllowedGroupWithoutReadIsNotTouched() {
    Permissions current = new Permissions();
    current.setGroupPermissions(SOC, perms(PermissionType.UPDATE));

    MergeResult result = PermissionsMerger.apply(current, null, decision(set(SOC), set(SOC)), VERSION, NOW, true);

    Assert.assertFalse(result.isChanged());
    Assert.assertEquals(result.getPermissions().getGroupPermissions(SOC), perms(PermissionType.UPDATE));
    Assert.assertTrue(result.getState().getRemovedPermissions().isEmpty());
  }

  @Test
  public void testStripRemovedPermissionsWithoutRuleAreLeftAloneWithWarning() {
    Permissions current = new Permissions();
    AccessRuleState previous = state();
    previous.getRemovedPermissions().put(HANDL, set("UPDATE")); // HANDL har inte längre regel

    MergeResult result = PermissionsMerger.apply(current, previous, decision(set(SOC), set(SOC)), VERSION, NOW, true);

    Assert.assertFalse(result.isChanged());
    Assert.assertTrue(result.getPermissions().getGroupPermissions(HANDL).isEmpty());
    Assert.assertEquals(result.getState().getRemovedPermissions().get(HANDL), set("UPDATE"));
    Assert.assertEquals(result.getWarnings().size(), 1, result.getWarnings().toString());
    Assert.assertTrue(result.getWarnings().get(0).contains(HANDL), result.getWarnings().toString());
  }

  @Test
  public void testFiveArgApplyNeverStripsWriteTypes() {
    Permissions current = basePermissions(); // SOC: READ+UPDATE

    MergeResult result = PermissionsMerger.apply(current, null, decision(set(SOC), set()), VERSION, NOW);

    Assert.assertEquals(result.getPermissions().getGroupPermissions(SOC), perms(PermissionType.UPDATE));
    Assert.assertTrue(result.getWriteStripped().isEmpty());
    Assert.assertTrue(result.getState().getRemovedPermissions().isEmpty());
    Assert.assertEquals(result.getWarnings().size(), 1, result.getWarnings().toString());
  }
}

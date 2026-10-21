/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.common.access;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.roda.core.data.v2.accessrules.AccessRuleState;
import org.roda.core.data.v2.ip.Permissions;
import org.roda.core.data.v2.ip.Permissions.PermissionType;

/**
 * För in ett {@link AccessDecision} i ett AIP:s
 * {@link Permissions} — utan sidoeffekter på indata.
 *
 * <p>
 * Principer:
 * </p>
 * <ul>
 * <li>Bara hanterade grupper rörs. Icke-hanterade grupper och alla användare
 * bevaras exakt. CREATE rörs aldrig.</li>
 * <li>Reglerna ger aldrig behörighet: kandidater är bara grupper som redan
 * har READ eller som reglerna tidigare tagit READ ifrån
 * ({@link AccessRuleState#getRestrictedGroups()}). En hanterad grupp som
 * varken har READ eller är restricted får inte READ.</li>
 * <li>Hanterad kandidat som är tillåten ⇒ READ finns/läggs till och gruppen
 * tas bort ur restrictedGroups. Nekad ⇒ READ tas bort och gruppen läggs i
 * restrictedGroups (så att READ kan återställas när metadata/regler ändras).
 * </li>
 * <li>Med {@code stripWriteOnDeny} (standard i applikatorn, konfig
 * core.access_rules.strip_write_on_deny) tas även UPDATE/DELETE/GRANT bort
 * för nekade hanterade grupper – annars kan en nekad grupp ändra den
 * eterna_admin-metadata som beslutet bygger på eller ge sig själv READ. De
 * borttagna typerna sparas i {@link AccessRuleState#getRemovedPermissions()}
 * och återställs när gruppen åter tillåts. Utan stripWriteOnDeny rörs bara
 * READ och en varning ges när gruppen behåller skrivbehörigheter.</li>
 * <li>Grupp i restrictedGroups/removedPermissions som inte längre är hanterad
 * (regeln borttagen) lämnas orörd — behörigheterna återställs först när
 * gruppen åter har en regel som tillåter den. En varning ges så att
 * administratören ser det.</li>
 * <li>{@code changed} speglar bara behörighets-/tillståndsändringar;
 * rulesVersion/appliedOn sätts alltid i returnerat tillstånd men gör inte
 * resultatet "ändrat".</li>
 * </ul>
 *
 * <p>
 * Implementationsnot: {@link Permissions#setGroupPermissions} med tom mängd
 * tar bort gruppen ur alla typer, och kopieringskonstruktorn
 * {@code Permissions(Permissions)} delar de underliggande mapparna. Mergern
 * gör därför en egen djup kopia (map för map, mängd för mängd) och ändrar
 * mängderna direkt i kopian, så att övriga typer garanterat bevaras.
 * </p>
 */
public final class PermissionsMerger {

  /** Skrivbehörigheter som tas bort för nekade grupper vid stripWriteOnDeny. */
  public static final PermissionType[] WRITE_TYPES = {PermissionType.UPDATE, PermissionType.DELETE,
    PermissionType.GRANT};

  private PermissionsMerger() {
    // statisk verktygsklass
  }

  /**
   * Slår ihop beslutet med nuvarande behörigheter; rör bara READ (som
   * kontraktet för 4.3.7 beskriver). Se
   * {@link #apply(Permissions, AccessRuleState, AccessDecision, String, String, boolean)}.
   */
  public static MergeResult apply(Permissions current, AccessRuleState currentState, AccessDecision decision,
    String rulesVersion, String appliedOn) {
    return apply(current, currentState, decision, rulesVersion, appliedOn, false);
  }

  /**
   * Slår ihop beslutet med nuvarande behörigheter.
   *
   * @param current
   *          AIP:ns nuvarande behörigheter (null = tomma); muteras inte
   * @param currentState
   *          AIP:ns nuvarande regeltillstånd eller null om reglerna aldrig
   *          tillämpats; muteras inte
   * @param decision
   *          motorns beslut (null = inga hanterade grupper)
   * @param rulesVersion
   *          regelversion som stämplas i tillståndet
   * @param appliedOn
   *          ISO-8601-tidsstämpel som stämplas i tillståndet
   * @param stripWriteOnDeny
   *          true ⇒ nekade hanterade grupper förlorar även
   *          UPDATE/DELETE/GRANT (sparas i tillståndet och återställs när
   *          gruppen åter tillåts)
   * @return nytt resultat med kopierade behörigheter och nytt tillstånd
   */
  public static MergeResult apply(Permissions current, AccessRuleState currentState, AccessDecision decision,
    String rulesVersion, String appliedOn, boolean stripWriteOnDeny) {
    MergeResult result = new MergeResult();
    Permissions permissions = deepCopy(current);
    Map<PermissionType, Set<String>> groups = permissions.getGroups();
    Set<String> read = groups.get(PermissionType.READ);

    Set<String> previouslyRestricted = currentState == null ? new LinkedHashSet<>()
      : new LinkedHashSet<>(currentState.getRestrictedGroups());
    Map<String, Set<String>> previouslyRemoved = currentState == null ? AccessRuleState.copyRemoved(null)
      : AccessRuleState.copyRemoved(currentState.getRemovedPermissions());
    Set<String> restricted = new LinkedHashSet<>(previouslyRestricted);
    Map<String, Set<String>> removedPermissions = AccessRuleState.copyRemoved(previouslyRemoved);
    Set<String> managed = decision == null ? new LinkedHashSet<>() : decision.getManagedGroups();
    Set<String> allowed = decision == null ? new LinkedHashSet<>() : decision.getAllowedGroups();

    // kandidater: grupper med READ (sorterade för stabil ordning) ∪ restricted
    // ∪ (vid strip) grupper med skrivbehörighet ∪ grupper med borttagna typer
    Set<String> candidates = new LinkedHashSet<>(new TreeSet<>(read));
    candidates.addAll(previouslyRestricted);
    if (stripWriteOnDeny) {
      for (PermissionType type : WRITE_TYPES) {
        candidates.addAll(new TreeSet<>(groups.get(type)));
      }
    }
    candidates.addAll(previouslyRemoved.keySet());

    for (String group : candidates) {
      boolean previouslyTouched = previouslyRestricted.contains(group) || previouslyRemoved.containsKey(group);
      if (!managed.contains(group)) {
        if (previouslyTouched) {
          result.getWarnings().add("Gruppen " + group
            + " har behörigheter borttagna av en tidigare regel men saknar nu regel; de återställs inte förrän gruppen åter har en regel som tillåter den");
        }
        continue;
      }
      boolean hasRead = read.contains(group);
      if (allowed.contains(group)) {
        // READ återställs bara om reglerna tog bort den (reglerna ger aldrig ny READ)
        if (!hasRead && previouslyRestricted.contains(group)) {
          read.add(group);
          result.getAdded().add(group);
        }
        restricted.remove(group);
        Set<String> toRestore = removedPermissions.remove(group);
        if (toRestore != null) {
          for (String typeName : toRestore) {
            PermissionType type = parseType(typeName);
            if (type != null && groups.get(type).add(group)) {
              result.getWriteRestored().add(group);
            }
          }
        }
      } else {
        if (hasRead) {
          read.remove(group);
          result.getRemoved().add(group);
        }
        if (hasRead || previouslyRestricted.contains(group)) {
          restricted.add(group);
        }
        if (stripWriteOnDeny) {
          Set<String> stripped = removedPermissions.computeIfAbsent(group, k -> new LinkedHashSet<>());
          for (PermissionType type : WRITE_TYPES) {
            if (groups.get(type).remove(group)) {
              stripped.add(type.name());
              result.getWriteStripped().add(group);
            }
          }
          if (stripped.isEmpty()) {
            removedPermissions.remove(group);
          }
        } else if (hasRead) {
          List<String> retained = retainedWriteTypes(permissions, group);
          if (!retained.isEmpty()) {
            result.getWarnings().add("Gruppen " + group + " har fått READ borttagen men behåller "
              + String.join("/", retained));
          }
        }
      }
    }

    AccessRuleState state = new AccessRuleState();
    state.setRestrictedGroups(restricted);
    state.setRemovedPermissions(removedPermissions);
    state.setRulesVersion(rulesVersion);
    state.setAppliedOn(appliedOn);
    state.setFactsSource(currentState == null ? null : currentState.getFactsSource());

    result.setPermissions(permissions);
    result.setState(state);
    result.setChanged(!result.getAdded().isEmpty() || !result.getRemoved().isEmpty()
      || !result.getWriteStripped().isEmpty() || !result.getWriteRestored().isEmpty()
      || !restricted.equals(previouslyRestricted) || !removedPermissions.equals(previouslyRemoved));
    return result;
  }

  /**
   * Djup kopia av behörigheter: nya mappar med alla typer närvarande och nya
   * mängder. null ger tomma behörigheter.
   */
  public static Permissions deepCopy(Permissions source) {
    Permissions copy = new Permissions();
    if (source != null) {
      copy.setUsers(copyMap(source.getUsers()));
      copy.setGroups(copyMap(source.getGroups()));
    }
    return copy;
  }

  private static Map<PermissionType, Set<String>> copyMap(Map<PermissionType, Set<String>> source) {
    Map<PermissionType, Set<String>> copy = new EnumMap<>(PermissionType.class);
    for (PermissionType type : PermissionType.values()) {
      copy.put(type, new HashSet<>());
    }
    if (source != null) {
      for (Map.Entry<PermissionType, Set<String>> entry : source.entrySet()) {
        if (entry.getKey() != null && entry.getValue() != null) {
          copy.get(entry.getKey()).addAll(entry.getValue());
        }
      }
    }
    return copy;
  }

  /** Skrivtyper (UPDATE/DELETE/GRANT) som gruppen fortfarande har. */
  private static List<String> retainedWriteTypes(Permissions permissions, String group) {
    List<String> retained = new ArrayList<>();
    for (PermissionType type : WRITE_TYPES) {
      Set<String> members = permissions.getGroups().get(type);
      if (members != null && members.contains(group)) {
        retained.add(type.name());
      }
    }
    return retained;
  }

  /** Typnamn ur tillståndet ⇒ PermissionType; okänt/READ/CREATE ⇒ null (ignoreras). */
  private static PermissionType parseType(String name) {
    if (name == null) {
      return null;
    }
    for (PermissionType type : WRITE_TYPES) {
      if (type.name().equalsIgnoreCase(name.trim())) {
        return type;
      }
    }
    return null;
  }
}

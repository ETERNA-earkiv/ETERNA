# Permissions in ETERNA

This document describes how permissions work and are administered in ETERNA: roles and groups, object permissions on structural units (AIPs), access rules that restrict reading based on confidentiality and personal-data assessments, the permission overview, and the configuration behind all of this.

## Concepts

| Term | Meaning in ETERNA |
|---|---|
| **Role = group** | What requirement specifications call a *role* is a **group** in ETERNA (e.g. `archivists`, `socialtjanst`). A group carries a set of roles (permissions) and every member gets them. The number of groups is unlimited. |
| **Role (permission)** | A functional permission with a technical name such as `aip.read`, `job.manage` or `access_rules.manage`. Assigned to groups (normal case) or directly to a user. Every REST method is bound to a role in `roda-roles.properties`; a missing role gives 403. |
| **Role area** | Roles are grouped by area in the UI (e.g. *Structural units (AIP)*, *Users and groups*, *Access rules (confidentiality/personal data)*). The area is the prefix before the first dot of the role name; the heading comes from `ui.roleTitle` in `roda-wui.properties`. |
| **Object permission** | The permission list of one structural unit: **READ**, **CREATE**, **UPDATE**, **DELETE** and **GRANT** per user and per group. Without READ the unit is invisible in the catalogue, in search results and in reports. |
| **Access rule** | One rule per group stating which confidentiality and personal-data assessments (and legal references, *lagrum*) the group may read. Rules are evaluated against the unit's administrative metadata (`eterna_admin`) and **restrict** read permission the group would otherwise have. Rules never grant. |
| **Managed group** | A group that has an access rule (enabled or disabled). Only managed groups are touched by the rule engine. |
| **Restricted group** | A managed group whose read permission on a given unit was removed by the rules. Stored on the unit (`accessRuleState.restrictedGroups`) so it can be restored when the assessment or the rule changes. Shown in the **Permissions** tab under *Restricted by access rule* and in the permission overview. |

## Roles and groups

Groups and users are administered under **Administration → Users and groups**.

- **Unlimited number of groups.** Create one group per role in the organisation. A group has a technical name (no whitespace) and a display name.
- **Roles are assigned to the group** with check boxes per role area. Changing the group's roles affects all members.
- **Several groups per user.** A user may belong to several groups and gets the union of their roles. Roles may also be assigned directly to the user (*direct roles*); the *effective roles* are the union of direct and group roles.
- **New login after membership changes.** The user object (groups and roles) is loaded at login and kept in the session. Adding a user to or removing a user from a group, or changing a group's roles, takes full effect for that user at the next login.
- **Protected groups and users.** `administrators`, `users`, `guests` and the user `guest` are protected (`core.ldap.protectedGroups` / `core.ldap.protectedUsers`) and cannot be deleted or renamed in the UI.

## Object permissions per structural unit

Every structural unit has a **Permissions** tab.

- The tab lists permissions per user and per group with check boxes for READ, CREATE, UPDATE, DELETE and GRANT.
- **Recursive application.** On save the change can be applied to the whole subtree. The change runs as a job (`PATCH /api/v2/aips/permissions/update`).
- **Inheritance on creation.** A new unit gets its permissions from its parent plus the defaults configured with `core.aip.default_permissions.*` (see the section *Default AIP permissions configuration* below).
- **Section "Restricted by access rule".** At the bottom of the tab (single unit only) the groups whose read permission was removed by the access rules are listed, or *None*. Giving such a group READ manually is immediately reverted by the rule engine.
- The user `admin` bypasses object permissions. The group `administrators` normally has full permissions through `core.aip.default_permissions` but is an ordinary group in the permission lists.

## Access rules for confidentiality and personal data

Access rules tie permissions to metadata about confidentiality (act, chapter, section) and about personal data and personal-data level. They are administered under **Administration → Access rules**.

### What drives the rules

Rules are evaluated against the unit's administrative metadata of type `eterna_admin` (configurable with `core.access_rules.metadata_type`):

| Field in `eterna_admin` | Vocabulary | Used for |
|---|---|---|
| `sekretess` – confidentiality assessment | `ej_bedomd` (not assessed), `ingen_sekretess` (none), `sekretess_forekommer` (occurs), `helt_sekretessbelagd` (fully classified) | The rule lists the assessments the group may read. |
| `sekretessgrund` – free text with the legal reference, e.g. "OSL 26 kap. 1 § och 21 kap. 7 §" | free text | Parsed into legal references (act, chapter, section) compared with the rule's patterns. Checked only when the assessment is neither `ingen_sekretess` nor `ej_bedomd`. |
| `personuppgifter` – personal-data assessment | `ej_bedomd`, `inga`, `forekommer`, `kansliga_forekommer` | The rule lists the assessments the group may read. |
| `personuppgiftsniva` – personal-data level (optional) | `core.access_rules.personuppgiftsniva.values[]`, default `niva_1`, `niva_2`, `niva_3` | Checked only when the unit has a level. |

A unit without its own `eterna_admin` document inherits the assessment of its nearest ancestor that has one (`missingMetadata = inherit`, the default). If no ancestor has one, the unit is **unassessed structure** (e.g. fonds, classes, series and records nobody has assessed) and is left **open** – the rules only govern units with an own or inherited assessment; for unassessed structure only the object permissions apply. Use `missingMetadata = inherit-closed` if such units should instead be treated as *not assessed* (and therefore closed). A unit whose own or inherited assessment is explicitly `ej_bedomd` is always closed with `ejBedomd = closed`.

### Legal-reference syntax

One pattern per line in the rule:

| Pattern | Meaning |
|---|---|
| `*` | All legal references (and units without any parsable reference). |
| `OSL 21` or `OSL 21 kap.` | The whole chapter: covers every section of chapter 21 of OSL. |
| `OSL 21:7` or `OSL 21 kap. 7 §` | Exactly one section. `OSL 21:7a` (section 7 a) is different from `OSL 21:7`. |
| `21:7` | Act omitted: interpreted as `defaultAct` in the rule file (default `OSL`). |

A section pattern does not cover a reference to a whole chapter; a chapter pattern covers all its sections. The free text in `sekretessgrund` is parsed tolerantly (`OSL 21 kap. 7 §`, `21 kap 7 §`, `OSL 21:7`, `OSL (2009:400) 21 kap. 7 §`, enumerations with `,`, `;`, `/`, `och`, `samt`; "OSL 21 kap. 1, 3 och 7 §§" gives three references). Unparsable parts become **warnings** and count as uncovered references, so unreadable text closes rather than opens. The **Normalize** button on the rule page shows how a text is interpreted.

### Policy defaults in the rule file

| Policy | Values | Default | Meaning |
|---|---|---|---|
| `ejBedomd` | `closed`, `open` | `closed` | `closed`: a unit assessed as `ej_bedomd` (or without value) may be read only by groups whose rule explicitly lists `ej_bedomd` (or `*`). `open`: not-assessed units are always allowed. |
| `missingMetadata` | `inherit`, `inherit-closed`, `open`, `closed` | `inherit` | Unit without own `eterna_admin`: `inherit` = use the nearest ancestor's assessment; with no assessed ancestor the unit is left open (unassessed structure is not governed by the rules). `inherit-closed` = like `inherit`, but a unit without own or inherited assessment is treated as not assessed (closed unless the rule lists `ej_bedomd` or `ejBedomd = open`). `open` = no inheritance, all managed groups allowed. `closed` = no inheritance, no managed group allowed. |
| `missingLagrum` | `closed`, `open` | `closed` | Classified unit without parsable legal reference: `closed` = allowed only by rules with `*`; `open` = the legal-reference check is skipped. |
| `multipleLagrum` | `all`, `any` | `all` | `all`: every reference on the unit must be covered; `any`: one is enough. |
| `defaultAct` | act name | `OSL` | Act assumed when the text has none; takes precedence over `core.access_rules.default_lag`. |

The defaults are the restrictive reading for everything that has actually been assessed (not assessed closed, missing reference closed, every reference must be covered); unassessed structure without an assessed ancestor is left open by `inherit` – choose `inherit-closed` to close it too. Unknown policy values and unknown vocabulary values always deny.

### Decision

A managed group G may read a unit with facts F when **all** conditions hold: F.sekretess is in G's allowed assessments (`*` = all); if F.sekretess is `sekretess_forekommer`, `helt_sekretessbelagd` or unknown, the unit's legal references are covered by G's patterns according to `missingLagrum`/`multipleLagrum`; F.personuppgifter is in G's allowed assessments; and, if F has a level, the level is in G's allowed levels. An empty list allows nothing. A **disabled rule** keeps the group managed but never allows anything (the group is restricted everywhere); delete the rule instead if the group should no longer be managed.

### Rules restrict, never grant

- Only **managed groups** are touched; other groups and all individual users are left untouched. CREATE is never touched.
- A managed group gets READ only if it already had READ on the unit, or if the rules previously removed it (the group is restricted). The rules never add a group that never had read access through the structure.
- **A denied group also loses write permissions.** With `core.access_rules.strip_write_on_deny = true` (default) UPDATE, DELETE and GRANT are removed too, otherwise the group could edit the assessment or grant itself READ. The removed types are stored on the unit (`accessRuleState.removedPermissions`) and restored when the group is allowed again.
- Deleting a group's rule makes the group unmanaged; permissions removed by an earlier rule are **not** restored until the group again has a rule that allows it (a warning is reported).
- Disabling the whole rule file (`enabled = false` in `access-rules.json`) allows all managed groups and restores removed permissions the next time units are evaluated. To stop all evaluation without restoring anything use `core.access_rules.enabled = false`.

### When the rules are applied

| Trigger | Mechanism |
|---|---|
| **Ingest** | Mandatory ingest step *Tillämpa åtkomstregler* (`parameter.do_apply_access_rules`, locked on) after the producer authorisation check and before disposal rules, so a package never becomes active with READ for denied groups. |
| **Assessment changed** | The model observer re-evaluates the unit when `eterna_admin` is created, updated or deleted – metadata form, bulk edit or restoring an earlier metadata version. Descendants without their own assessment are re-evaluated by an automatic job *Åtkomstregler: efterkommande till …* (one job per bulk edit). |
| **Manual permission change** | The observer re-evaluates after every permission change; READ given to a restricted group is removed again at once. |
| **Move** | A moved unit is re-evaluated (inherited facts may change); if it has no own assessment its descendants are re-evaluated too. |
| **Job "Tillämpa åtkomstregler"** | Started from the rules page (*Apply rules now* / *Dry run*), via REST (`POST /api/v2/access-rules/apply`) or as a maintenance job under **Administration → Preservation actions** (maintenance jobs) on any selection. Evaluates all active units the job user may read and attaches a CSV report. |
| **Dry run** | Same job with the job parameter *Torrkörning* (dry run): computes and reports without writing anything and without preservation events. |

Rule changes take effect immediately for new assessments; existing units are updated when the job runs (tick *Apply the rules to the repository right after saving*).

Every write that changes permissions creates a preservation event (PREMIS UPDATE, description *Tillämpning av åtkomstregler (sekretess/personuppgifter) på behörigheter*) with facts, reasoning per group, the changed permissions and the triggering user as agent. Events can be switched off with `core.access_rules.observer.create_event`, `core.access_rules.ingest.create_event` and the job parameter *Skapa bevarandehändelser* (create preservation events).

### Administrators

The user `admin` bypasses all object permissions and is not affected. The group `administrators` is normally not listed in the rule file and keeps its permissions. The role `access_rules.manage` is repository-wide: the apply job runs as the caller and restricts the managed groups on every unit the caller may read; GRANT is not required because the rules only remove permissions. Units the job user may not read are reported as skipped without confidentiality details.

### The portal's guest group

Anonymous portal requests run as user `guest` in group `guests`. Treat it like any group: give it READ on what should be public and add an access rule for `guests` that allows only `ingen_sekretess` and `inga` (or `ej_bedomd`) as policy dictates. Classified and personal-data units are then restricted from the portal automatically, even when they received READ for `guests` by inheritance. Unassessed structure (no own or inherited assessment) is not restricted by the rule with `missingMetadata = inherit`; use `inherit-closed` if the portal should show assessed material only.

## Administration → Access rules

Requires `access_rules.read` (view) or `access_rules.manage` (edit and apply).

The **status row** shows *Status* (Active/Disabled – both `core.access_rules.enabled` and the file's `enabled`), *Version*, *Last applied* (time and *Show job* link to the last non-dry-run job, else *Never*) and *Last changed* (by whom, when). The **rule list** has columns *Group* (link), *Active*, *Allowed confidentiality assessments*, *Allowed legal references*, *Allowed personal data assessments*, *Allowed personal data levels*, *Edit* and *Remove*.

1. **Add rule for group** (sidebar *Actions*): pick the group in the member dialog. The form *Edit access rule* opens with *All (\*)* preselected in every check-box group so an untouched rule does not restrict the group from the whole repository.
2. **Active** – whether the rule is evaluated (a disabled rule restricts the group completely).
3. **Allowed confidentiality assessments** – *All (\*)*, *Not assessed*, *No confidentiality*, *Contains confidential information*, *Entirely confidential*. Ticking *All* locks the others. Nothing ticked = nothing allowed.
4. **Allowed legal references** – one per line; *Add legal reference*, *Remove*, *Normalize* (shows *Normalized form* and *Warnings*; *Use normalized form* replaces the rows). A row yielding more than one reference is rejected on save.
5. **Allowed personal data assessments** – check boxes from the vocabulary.
6. **Allowed personal data levels** – check boxes from the vocabulary (locked to *All* when no vocabulary is configured).
7. **Note** – free text.
8. **Apply the rules to the repository right after saving** – tick to start the apply job; click **Save**. The server validates (vocabulary, patterns, group exists in the directory) and answers 400 with a problem list if invalid.
9. **Remove** asks for confirmation; the group is then no longer managed.
10. **Apply rules now** / **Dry run** start the job on all active units after confirmation and offer to follow the job.

The **group page** shows the group's rule in summary (or *No rule*) with an *Edit* link.

## Permission overview and report

**Administration → Permission overview** (role `member.read`) is a read-only view with four tabs: **Permissions per group** (matrix of roles × groups, effective roles ticked), **Users** (groups, direct and effective roles, text filter), **Structural units** (units on the levels in `ui.permissionOverview.levels`, default `fonds,class,series`, with the groups/users holding each permission type and a facet on READ groups; only units the caller may read) and **Access rules** (the active rules).

**Report (HTML)** opens a printable report (`GET /api/v2/permission-overview/report/html`, file `behorighetsoversikt.html`) in a new window; **CSV** downloads the same data (`/report/csv`, `behorighetsoversikt.csv`, delimiter from `csv.delimiter`, default `;`). The report is built live from the directory and the index; a section that cannot be fetched is replaced by a warning ("the report is incomplete") rather than shown as complete. The HTML/CSV reports also list *Restricted groups* per unit.

## System administrator accounts

Create a personal account per administrator under **Administration → Users and groups** and make it a member of `administrators` (or of a custom group with a subset of administrative roles such as `member.manage` or `access_rules.manage`). The built-in `admin` account is a **bootstrap account** created at first start that bypasses object permissions: use it for installation and recovery, change its password, and use personal accounts for daily work so the audit log shows who did what. All permission changes are written to **Administration → Audit logs**.

## Configuration

### `core.access_rules.*` keys (roda-core.properties)

| Key | Default | Meaning |
|---|---|---|
| `core.access_rules.enabled` | `false` | Master switch; set to `true` to use access rules. `false` ⇒ nothing is evaluated (observer, jobs, ingest step skip; nothing is restored). |
| `core.access_rules.file` | `access-rules.json` | Rule file name relative to `RODA_HOME/config` (paths outside are rejected). Missing ⇒ built-in default (no rules, `enabled=false`). |
| `core.access_rules.observer.enabled` | `true` | Evaluate immediately on assessment change, permission change and move; `false` ⇒ ingest step and jobs only. |
| `core.access_rules.observer.create_event` | `true` | Preservation event when the observer changes permissions. |
| `core.access_rules.metadata_type` | `eterna_admin` | Descriptive metadata type carrying the assessments (without version). |
| `core.access_rules.default_lag` | `OSL` | Act assumed when the text has none; `defaults.defaultAct` in the file takes precedence. |
| `core.access_rules.ingest.create_event` | `true` | Preservation event when the ingest step changes permissions. |
| `core.access_rules.strip_write_on_deny` | `true` | Denied managed group also loses UPDATE/DELETE/GRANT (restored when allowed again). `false` ⇒ only READ, with a warning. |
| `core.access_rules.personuppgiftsniva.values[]` | `niva_1`, `niva_2`, `niva_3` | Allowed personal-data levels (organisation's guideline); used by the editor and for validation. Other level values in metadata are allowed only by `*`. |

### Rule file `RODA_HOME/config/access-rules.json`

```json
{
  "version": 3,
  "enabled": true,
  "defaults": {
    "ejBedomd": "closed",
    "missingMetadata": "inherit",
    "missingLagrum": "closed",
    "multipleLagrum": "all",
    "defaultAct": "OSL"
  },
  "rules": [
    {
      "group": "socialtjanst",
      "enabled": true,
      "sekretess": ["ingen_sekretess", "sekretess_forekommer"],
      "lagrum": ["OSL 26:1", "OSL 21"],
      "personuppgifter": ["inga", "forekommer", "kansliga_forekommer"],
      "personuppgiftsniva": ["niva_1", "niva_2"],
      "note": "Decision KS 2026/0123"
    }
  ],
  "updatedBy": "anna.jansson",
  "updatedOn": "2026-10-09T12:34:56Z"
}
```

- `version` is incremented by the server on every save and stamped on units (`accessRuleState.rulesVersion`); `PUT /api/v2/access-rules` must send the stored version (409 otherwise).
- Values are normalised on load; the file is validated (policies, group names, vocabulary per `eterna_admin.xsd` plus `*`, levels, patterns ≤ 200 characters and parsable). An invalid file is logged and the last valid version kept; with none, the rules are treated as disabled with no managed groups (nothing is touched).
- The file is re-read when its timestamp or size changes (editing on disk works without restart); writes are atomic.
- **History:** before every save the previous file is copied to `RODA_HOME/config/access-rules.history/<yyyyMMddHHmmss>-<user>.json`.

### Roles

| Role | Grants |
|---|---|
| `access_rules.read` | View rules, status, vocabulary, normalisation (`AccessRulesController.listAccessRules`, `retrieveAccessRule`, `retrieveStatus`, `retrieveVocabulary`, `normalizeLagrum`); menu gate `ui.menu.administration.access_rules.role`. |
| `access_rules.manage` | Edit, delete and apply (`updateAccessRule`, `deleteAccessRule`, `updateAccessRuleSet`, `applyAccessRules`). |
| `member.read` | Permission overview and reports (`PermissionOverviewController.*`); menu gate `ui.menu.administration.permission_overview.role`. |
| `job.manage` | Start *Tillämpa åtkomstregler* as a maintenance job on a custom selection. |

The roles are created in LDAP at start-up and shown as check boxes under the role area *Access rules (confidentiality/personal data)* (`ui.roleTitle: access_rules`, `ui.role: access_rules.read`, `ui.role: access_rules.manage`).

### `ui.permissionOverview.levels` (roda-wui.properties)

```properties
ui.permissionOverview.levels = fonds,class,series
```

Comma-separated levels (`IndexedAIP.level`) included in the *Structural units* section, in sort order; whitelisted for the client. Columns and facets of the tab are configured under `ui.lists.PermissionOverview_structureNodes.*`.

### Default AIP permissions configuration

It is possible to change how AIP permissions are granted to users and groups when creating an AIP on the Web interface
or when creating it via ingest. To do so, the following configurations must be changed in the roda-core.properties file.

1. If we wish to add a group as a super group that has full permissions when creating aip's, we simply need add the
   following lines:
   ```properties
    # Admin users or groups so AIPs can be administered

    core.aip.default_permissions.admin.group[] = administrators

    core.aip.default_permissions.admin.group[].administrators.permission[] = READ
    core.aip.default_permissions.admin.group[].administrators.permission[] = UPDATE
    core.aip.default_permissions.admin.group[].administrators.permission[] = CREATE
    core.aip.default_permissions.admin.group[].administrators.permission[] = GRANT
    core.aip.default_permissions.admin.group[].administrators.permission[] = DELETE
    ```

   The lines above configure the administrators group. If we wanted to add a super user, for example, an admin user, we
   would do it like so:
    ```properties
    core.aip.default_permissions.admin.user[] = admin

    core.aip.default_permissions.admin.user[].admin.permission[] = READ
    core.aip.default_permissions.admin.user[].admin.permission[] = UPDATE
    core.aip.default_permissions.admin.user[].admin.permission[] = CREATE
    core.aip.default_permissions.admin.user[].admin.permission[] = GRANT
    core.aip.default_permissions.admin.user[].admin.permission[] = DELETE
    ```

   If we want to set permissions for the user that is creating the AIP, we do it like so:
    ```properties
    # Direct creator permissions
    core.aip.default_permissions.creator.user.permission[] = CREATE
    core.aip.default_permissions.creator.user.permission[] = UPDATE
    core.aip.default_permissions.creator.user.permission[] = READ
    ```

   If we want to set permissions for some specific user, simply add the following (example of setting permissions for
   user 'foo'):
    ```properties
    # Direct creator permissions
    core.aip.default_permissions.users[] = foo

    core.aip.default_permissions.users[].foo.permission[] = CREATE
    core.aip.default_permissions.users[].foo.permission[] = DELETE
    core.aip.default_permissions.users[].foo.permission[] = READ
    ```

   Finally, we can also add other groups, other than super groups. These are normal groups, and do not have full
   permissions.
    ```properties
    # Additional group permissions
    core.aip.default_permissions.group[] = archivists
    core.aip.default_permissions.group[] = producers
    core.aip.default_permissions.group[] = guests

    core.aip.default_permissions.group[].archivists.permission[] = READ
    core.aip.default_permissions.group[].archivists.permission[] = UPDATE
    core.aip.default_permissions.group[].archivists.permission[] = CREATE

    core.aip.default_permissions.group[].producers.permission[] = READ

    core.aip.default_permissions.group[].guests.permission[] = READ
    ```

2. A requirement might be that the user that created the AIP must belong to one of the groups defined on the
   configurations file and it has READ and UPDATE permissions at least. If the intersection is empty, or generally, if
   the set permission ends up not providing the user with at least READ and UPDATE permissions to the created AIP, then
   these permissions are loaded as the minimum permissions from the config file.
   ```properties
    # Intersect creator groups with the configuration groups
    core.aip.default_permissions.intersect_groups = true

    # System expects a minimum set of direct or indirect permissions for the creator (DO NOT CHANGE THIS!)
    core.aip.default_permissions.creator.minimum.permissions[] = UPDATE
    core.aip.default_permissions.creator.minimum.permissions[] = READ
    ```

   By default, and to conform to legacy behaviour, the creator user is granted all permissions when creating an AIP. If
   you do not wish to use legacy permissions, set this property as follows:
    ```properties
    # Use legacy behaviour
    core.aip.default_permissions.legacy_permissions = false
    ```

The defaults only set the starting point; the access rules are applied afterwards in the ingest step and may remove READ (and write permissions) for managed groups.

## Troubleshooting

| Symptom | Likely cause and fix |
|---|---|
| **403** on Administration → Access rules or `/api/v2/access-rules` | Missing role `access_rules.read` (view) or `access_rules.manage` (edit/apply). Assign on the group and log in again. The overview needs `member.read`. |
| Menu item missing | Same cause (`ui.menu.administration.access_rules.role`, `ui.menu.administration.permission_overview.role`); log in again after group changes. |
| Units assessed **"not assessed" are invisible** to a group | Intended with `ejBedomd = closed`: `ej_bedomd` (own or inherited) is allowed only if the rule lists *Not assessed* (or *All*). With `missingMetadata = inherit-closed` this also applies to units without own `eterna_admin` and without an assessed ancestor. Add `ej_bedomd` to the rule or assess the unit. |
| Units **without any assessment are visible** to a group despite its rule | Intended with `missingMetadata = inherit` (default): a unit without own `eterna_admin` and without an assessed ancestor is unassessed structure that the rules do not govern – only the object permissions apply. To close such units set `missingMetadata = inherit-closed` (or assess the structural level so the assessment is inherited) and run *Apply rules now*. |
| Group lost READ everywhere | The rule is disabled (restricts completely) or a list is empty (nothing ticked = nothing allowed). Tick *All (\*)* where the assessment should not restrict. |
| Classified unit denied despite the right reference in the rule | The `sekretessgrund` text could not be parsed (warning in the job report) or the unit has several references and `multipleLagrum = all`. Fix the text (see *Normalize*) or add patterns. A section pattern does not cover a whole chapter. |
| Changed rule not reflected on existing units | Rules apply immediately to new assessments only. Run *Apply rules now*; the status row shows *Last applied*. |
| Job skips units: "Jobbanvändaren saknar läsbehörighet …" | The apply job only handles units the job user may read. Run as `admin` or a user with READ on everything. |
| Job skips everything: rules disabled / no rules | `core.access_rules.enabled = false`, or the rule file is missing/invalid and the empty default is used (check the log for "Regelfilen … kan inte användas"). |
| Log warns that a rule's group matches no group in the directory | Group name in the rule differs (case) from LDAP; the rule has no effect until fixed. |
| Save gives 400 "Ogiltig regeluppsättning: …" | The problem list names the field: unknown vocabulary value, unparsable pattern, unknown group, duplicate. |
| Save of the rule set via API gives 409 | Version conflict: re-read `GET /api/v2/access-rules` and resend with the current version. |
| Metadata change rejected with "Åtkomstreglerna kunde inte tillämpas …" | The observer fails closed: a storage/model error during evaluation stops the change so a unit never gets new facts without the rules being applied. Fix the logged error and retry. |
| Where are the jobs? | **Administration → Preservation actions** (maintenance jobs) lists *Tillämpa åtkomstregler* and *Åtkomstregler: efterkommande till …*. The report shows per unit facts source, assessment, references, decision per group and changed permissions; the CSV attachment `atkomstregler-<jobId>.csv` is downloaded from the job page (`GET /api/v2/jobs/{id}/attachment/{attachment}`). |
| Log | The server log contains all rule-engine messages (in Swedish): rule file load/save, observer decisions, started jobs and warnings (first occurrence WARN, repeats DEBUG). |
| Downgrade to a build without access rules | Once applied, `aip.json` contains `accessRuleState`; an older build cannot read such units. Remove the field before downgrading. |

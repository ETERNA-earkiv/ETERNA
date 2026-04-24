# Dynamic Group Authorization via IDP

ETERNA supports dynamic assignment of internal groups based on group membership in an external Identity Provider (IDP). Any IDP that exposes group membership through OIDC or SAML claims is supported, including Azure AD / Microsoft Entra ID, Keycloak, Okta, and others. When a user logs in via CAS, the user's group memberships are retrieved from the IDP and mapped to internal ETERNA groups.

## Configuration

Group mapping is configured in `roda-core.properties`.

### Enable external group mapping

```properties
core.authorization.external.group_mapping = true
core.authorization.external.attribute = memberOf
```

- `group_mapping = true` enables the feature.
- `attribute = memberOf` specifies which OIDC/SAML attribute contains the user's groups. The attribute name varies between IDPs — `memberOf` is the claim used by Azure AD, but your IDP may use a different name (e.g. `groups` or a custom claim).

### Define mappings

Each mapping connects an IDP group identifier (matched as a regex) to one or more internal ETERNA groups:

```properties
core.authorization.external.mappings[] = <mapping-name>
core.authorization.external.mapping.<mapping-name>.external.group = <regex matching IDP group ID>
core.authorization.external.mapping.<mapping-name>.internal.groups[] = <internal group>
```

Multiple mappings can be defined by repeating the `mappings[]` entry with different names.

### Example: Azure AD / Microsoft Entra ID

In Azure AD, groups are identified by their object ID (GUID). The following example maps two Azure AD groups to internal ETERNA groups:

```properties
core.authorization.external.group_mapping = true
core.authorization.external.attribute = memberOf

# Administrators
core.authorization.external.mappings[] = map_admins
core.authorization.external.mapping.map_admins.external.group = bbc466f2-55f4-4ca2-8303-d4238b8884d5
core.authorization.external.mapping.map_admins.internal.groups[] = administrators

# Standard users
core.authorization.external.mappings[] = map_users
core.authorization.external.mapping.map_users.external.group = f390adab-5c81-4a89-8e72-c9cc107bf7d5
core.authorization.external.mapping.map_users.internal.groups[] = users
```

The `external.group` value is matched as a regular expression against the group identifier returned by the IDP. For other IDPs the group identifier may be a name, a DN (LDAP-style), or some other string — consult your IDP's documentation.

## Limitations

### No support for nested (recursive) group membership

ETERNA only evaluates the groups returned directly in the `memberOf` attribute from the IDP. If a user is a member of Group A, and Group A is a member of Group B, the internal roles mapped to Group B are **not** assigned to the user.

**Example of what does not work:**
```
User → Group A (direct member) → Group B (nested) → mapped to internal group
```
The user will not receive the internal group mapped to Group B because the membership is indirect.

**Workarounds:**

- Add users directly to the IDP group that is mapped in `roda-core.properties`.
- Create an additional mapping targeting the intermediate group.
- **Configure your IDP to include transitive group memberships in the token claim.** Most modern IDPs support this natively and it requires no changes to ETERNA. See the IDP-specific notes below.

### Azure AD behavior

By default, Azure AD only includes direct group memberships in the `memberOf` claim via OIDC. To include transitive (nested) memberships, change the `groupMembershipClaims` setting in the application registration manifest from `"SecurityGroup"` to `"All"`:

```json
"groupMembershipClaims": "All"
```

This causes Azure AD to include all transitive group memberships in the token, which ETERNA then processes as if they were direct memberships.

For other IDPs, consult their documentation on how to include transitive group memberships in the OIDC/SAML group claim (e.g. Keycloak's group mapper, Okta's group claim filter).

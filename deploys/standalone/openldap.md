# OpenLDAP Configuration for ETERNA (Dev)

## Overview

ETERNA uses OpenLDAP for user authentication and role-based access control. The dev environment uses [vegardit/openldap](https://github.com/vegardit/docker-openldap) (OpenLDAP 2.6.x on Debian), replacing the deprecated osixia/openldap image.

## Architecture

```
ETERNA (Spring Boot) --LDAP--> OpenLDAP container (port 1389)
```

LDAP directory structure:
```
dc=roda,dc=org                          (root)
  ou=users                              (user accounts)
    uid=admin                           (admin user, password: eterna)
    uid=guest                           (guest user, password: roda)
  ou=groups                             (user groups)
    cn=administrators                   (admin group, member: uid=admin)
    cn=users                            (regular users group, member: uid=admin)
    cn=guests                           (guest group, member: uid=guest)
  ou=roles                              (RBAC roles, ~63 roles)
    cn=aip.read                         (example role)
    cn=aip.create                       (example role)
    cn=representation.apply_xslt        (XSLT viewer role)
    ...                                 (all roles from roda-roles.properties)
```

## How It Works

### RODA Bootstrap Behavior (Critical)

RODA's `LdapUtility.bootstrap()` checks if the root DN (`dc=roda,dc=org`) exists:
- **If root DN does NOT exist**: RODA creates everything (root, OUs, users, groups, roles)
- **If root DN EXISTS**: RODA **skips ALL initialization** — no users, groups, or roles are created

Since vegardit creates the root DN during container startup, RODA's bootstrap is skipped entirely. This means **all users, groups, and roles must be pre-seeded via init LDIFs**.

### Password Hashing

OpenLDAP uses **SSHA** (Salted SHA-1) for password hashing. PBKDF2-SHA512 is NOT supported by default OpenLDAP. Generate SSHA hashes with:

```bash
docker exec openldap slappasswd -s <password>
```

### Role Assignment

Roles use `organizationalRole` objects with `roleOccupant` pointing to a group:
```ldif
dn: cn=aip.read,ou=roles,dc=roda,dc=org
objectClass: organizationalRole
objectClass: top
cn: aip.read
roleOccupant: cn=administrators,ou=groups,dc=roda,dc=org
```

This gives all members of the `administrators` group the `aip.read` role.

## Files

| File | Purpose |
|------|---------|
| `docker-compose-dev.yaml` | vegardit/openldap service definition |
| `ldap/ldifs/init_org_tree.ldif` | Creates root DN + OUs (uses vegardit template variables) |
| `ldap/ldifs/init_org_entries.ldif` | Seeds users, groups, and all 63 RODA roles |
| `ldap/ldifs/init_org_ppolicy.ldif` | Empty — prevents vegardit from creating default password policy |

## Environment Variables

| Variable | Value | Description |
|----------|-------|-------------|
| `LDAP_INIT_ORG_DN` | `DC=roda,DC=org` | Base DN for the LDAP directory |
| `LDAP_INIT_ORG_NAME` | `RODA` | Organization name |
| `LDAP_INIT_ROOT_USER_DN` | `cn=admin,dc=roda,dc=org` | LDAP admin bind DN |
| `LDAP_INIT_ROOT_USER_PW` | `eterna` | LDAP admin password |
| `LDAP_TLS_ENABLED` | `false` | TLS disabled for dev |

ETERNA env vars (set when starting the jar):
| Variable | Value | Description |
|----------|-------|-------------|
| `LDAP_SERVER_URL` | `ldap://localhost` | LDAP server URL |
| `LDAP_SERVER_PORT` | `1389` | Host port (mapped from container 389) |

## Common Operations

### Reset LDAP (fresh start)

```bash
cd deploys/standalone
docker compose -f docker-compose-dev.yaml down openldap
docker volume rm standalone_roda-openldap-data standalone_roda-openldap-config
docker compose -f docker-compose-dev.yaml up -d openldap
```

### Verify LDAP contents

```bash
# List all entries
docker exec openldap ldapsearch -x -H ldap://localhost:389 \
  -D 'cn=admin,dc=roda,dc=org' -w eterna \
  -b 'dc=roda,dc=org' '(objectClass=*)' dn

# Test user login
docker exec openldap ldapwhoami -x -H ldap://localhost:389 \
  -D 'uid=admin,ou=users,dc=roda,dc=org' -w eterna
```

### Change a user password

```bash
docker exec openldap ldappasswd -x -H ldap://localhost:389 \
  -D 'cn=admin,dc=roda,dc=org' -w eterna \
  -s <new-password> 'uid=admin,ou=users,dc=roda,dc=org'
```

Then update the SSHA hash in `init_org_entries.ldif` for persistence across restarts:
```bash
docker exec openldap slappasswd -s <new-password>
# Copy the output hash into the LDIF
```

### Add a new role

1. Add the role to `roda-roles.properties` (maps Java method to role name)
2. Add an LDIF entry to `init_org_entries.ldif`:
   ```ldif
   dn: cn=new.role.name,ou=roles,dc=roda,dc=org
   objectClass: organizationalRole
   objectClass: top
   cn: new.role.name
   roleOccupant: cn=administrators,ou=groups,dc=roda,dc=org
   ```
3. Reset LDAP (see above)

### Regenerate all roles from roda-roles.properties

If `roda-roles.properties` changes significantly, regenerate the entries LDIF:

```bash
python3 -c "
roles = set()
with open('roda-core/roda-core/src/main/resources/config/roda-roles.properties') as f:
    for line in f:
        line = line.strip()
        if line.startswith('core.roles.') and '=' in line:
            val = line.split('=', 1)[1].strip()
            if val and not val.startswith('core.') and not val.endswith('='):
                roles.add(val)
for r in sorted(roles):
    print(r)
"
```

## Migration from osixia/openldap

The previous setup used `osixia/openldap:1.5.0` which:
- Had 1023 CVEs (Debian Buster base, EOL)
- Used `LDAP_ORGANISATION`, `LDAP_DOMAIN`, `LDAP_ADMIN_PASSWORD` env vars
- Mapped port 389:389 directly

vegardit/openldap:
- ~90 CVEs, 0 critical (Debian Trixie base)
- Uses `LDAP_INIT_ORG_DN`, `LDAP_INIT_ROOT_USER_DN`, etc.
- Maps port 1389:389 in dev compose
- Requires explicit init LDIFs for full RODA compatibility

**Note:** Production (`docker-compose.yaml`) still uses osixia/openldap. This migration only applies to the dev compose.

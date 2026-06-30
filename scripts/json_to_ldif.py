#!/usr/bin/env python3
"""Convert ETERNA 0.6.1 REST API v1 JSON export (users + groups) to a
1.0.0 OpenLDAP migration LDIF + CSV report.

Usage:
    python3 json_to_ldif.py \
        --users out/users.json \
        --groups out/groups.json \
        --ldif-out out/eterna-users-migration.ldif \
        --csv-out out/migration-report.csv \
        [--base dc=roda,dc=org] \
        [--include-protected]
"""
import argparse
import csv
import json
import sys

USER_OCS = [
    "extensibleObject", "top", "person",
    "organizationalPerson", "inetOrgPerson", "shadowAccount",
]
GROUP_OCS = ["extensibleObject", "top", "groupOfUniqueNames"]

PROTECTED_USERS = {"admin", "guest"}
PROTECTED_GROUPS = {"administrators", "users", "guests"}


def load_json(path, *keys):
    with open(path) as f:
        data = json.load(f)
    if isinstance(data, list):
        return data
    for key in keys:
        if key in data:
            return data[key]
    # fallback: first list value found
    for v in data.values():
        if isinstance(v, list):
            return v
    return []


def split_name(full_name):
    parts = (full_name or "").strip().split()
    if len(parts) >= 2:
        return " ".join(parts[:-1]), parts[-1]
    return full_name or "", full_name or ""


def build_user_ldif(u, base):
    uid = u.get("id") or u.get("name") or ""
    full_name = u.get("fullName") or u.get("name") or uid
    given, sn = split_name(full_name)
    email = u.get("email") or ""
    active = u.get("active", True)
    shadow_inactive = "0" if active else "1"

    lines = [f"dn: uid={uid},ou=users,{base}"]
    lines += [f"objectClass: {oc}" for oc in USER_OCS]
    lines += [
        f"uid: {uid}",
        f"cn: {full_name}",
        f"sn: {sn}",
    ]
    if given:
        lines.append(f"givenName: {given}")
    if email:
        lines.append(f"email: {email}")
    lines += [
        f"shadowInactive: {shadow_inactive}",
        "info: ;;;",
    ]
    return "\n".join(lines)


def build_group_add_ldif(g, base):
    cn = g.get("id") or g.get("name") or ""
    full_name = g.get("fullName") or cn
    active = g.get("active", True)
    shadow_inactive = "0" if active else "1"

    lines = [f"dn: cn={cn},ou=groups,{base}"]
    lines += [f"objectClass: {oc}" for oc in GROUP_OCS]
    lines += [
        f"cn: {cn}",
        f"ou: {full_name}",
        f"shadowInactive: {shadow_inactive}",
        f"uniqueMember: cn=roda,ou=system,{base}",
    ]
    return "\n".join(lines)


def build_group_membership_ldif(g, base):
    """changetype: modify to replace uniqueMember — works on existing groups."""
    cn = g.get("id") or g.get("name") or ""
    members = g.get("users", [])

    lines = [
        f"dn: cn={cn},ou=groups,{base}",
        "changetype: modify",
        "replace: uniqueMember",
    ]
    for uid in members:
        lines.append(f"uniqueMember: uid={uid},ou=users,{base}")
    lines.append(f"uniqueMember: cn=roda,ou=system,{base}")
    return "\n".join(lines)


def build_roles_ldif(groups, users, base):
    """Generate ldapmodify to restore roleOccupant assignments on existing role entries.

    Roles in OpenLDAP are created empty by ETERNA's syncMissingRoles on startup.
    This assigns groups/users to each role based on the v0.6.1 export.
    Must be applied AFTER ETERNA has started (role entries must already exist).
    Uses replace: roleOccupant so the result is authoritative — run once only.
    """
    role_occupants = {}  # role_name -> set of occupant DNs

    for g in groups:
        cn = g.get("id") or g.get("name") or ""
        group_dn = f"cn={cn},ou=groups,{base}"
        for role in g.get("directRoles", []):
            role_occupants.setdefault(role, set()).add(group_dn)

    for u in users:
        uid = u.get("id") or u.get("name") or ""
        user_dn = f"uid={uid},ou=users,{base}"
        for role in u.get("directRoles", []):
            role_occupants.setdefault(role, set()).add(user_dn)

    if not role_occupants:
        return ""

    blocks = []
    for role, occupants in sorted(role_occupants.items()):
        lines = [
            f"dn: cn={role},ou=roles,{base}",
            "changetype: modify",
            "replace: roleOccupant",
        ]
        lines += [f"roleOccupant: {o}" for o in sorted(occupants)]
        blocks.append("\n".join(lines))

    return "\n\n".join(blocks) + "\n"


def transform(users_path, groups_path, base, ldif_path, membership_ldif_path, roles_ldif_path, csv_path, include_protected):
    users = load_json(users_path, "users", "results")
    groups = load_json(groups_path, "groups", "results")

    # Build group→members map from user data (more reliable than groups API response)
    membership = {}
    for u in users:
        uid = u.get("id") or u.get("name") or ""
        for g in u.get("groups", []):
            membership.setdefault(g, []).append(uid)

    # Merge into groups list (handles groups missing from API or lacking users field)
    group_index = {(g.get("id") or g.get("name") or ""): g for g in groups}
    for cn, members in membership.items():
        if cn not in group_index:
            group_index[cn] = {"id": cn, "name": cn, "fullName": cn}
        existing = group_index[cn].get("users") or []
        merged = sorted(set(existing) | set(members))
        group_index[cn] = {**group_index[cn], "users": merged}
    groups = list(group_index.values())

    add_blocks = []       # ldapadd: users + new (non-protected) groups
    modify_blocks = []    # ldapmodify: membership update for ALL groups
    report_rows = []
    stats = {"users": 0, "groups": 0, "skipped_users": 0}

    for u in users:
        uid = u.get("id") or u.get("name") or ""
        if not include_protected and uid in PROTECTED_USERS:
            stats["skipped_users"] += 1
            continue
        add_blocks.append(build_user_ldif(u, base))
        report_rows.append({
            "uid": uid,
            "fullName": u.get("fullName") or u.get("name") or "",
            "email": u.get("email") or "",
            "active": u.get("active", True),
            "groups": "|".join(sorted(u.get("groups", []))),
            "note": "password reset required",
        })
        stats["users"] += 1

    for g in groups:
        cn = g.get("id") or g.get("name") or ""
        if cn not in PROTECTED_GROUPS:
            # New group — create it first, then update membership
            add_blocks.append(build_group_add_ldif(g, base))
        # ALL groups get a membership modify (protected ones already exist in OpenLDAP)
        if g.get("users"):
            modify_blocks.append(build_group_membership_ldif(g, base))
        stats["groups"] += 1

    with open(ldif_path, "w") as f:
        f.write("\n\n".join(add_blocks))
        if add_blocks:
            f.write("\n")

    with open(membership_ldif_path, "w") as f:
        f.write("\n\n".join(modify_blocks))
        if modify_blocks:
            f.write("\n")

    # Roles: build from ALL groups (including protected — administrators has all roles)
    roles_ldif = build_roles_ldif(groups, users, base)
    with open(roles_ldif_path, "w") as f:
        f.write(roles_ldif)
    stats["roles"] = roles_ldif.count("changetype: modify")

    with open(csv_path, "w", newline="") as f:
        w = csv.DictWriter(f, fieldnames=["uid", "fullName", "email", "active", "groups", "note"])
        w.writeheader()
        w.writerows(report_rows)

    return stats


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--users", required=True, help="Path to users.json from API v1")
    p.add_argument("--groups", required=True, help="Path to groups.json from API v1")
    p.add_argument("--ldif-out", required=True)
    p.add_argument("--membership-ldif-out", default=None,
                   help="Output path for group membership modify LDIF (default: derived from --ldif-out)")
    p.add_argument("--roles-ldif-out", default=None,
                   help="Output path for role assignments modify LDIF (default: derived from --ldif-out)")
    p.add_argument("--csv-out", required=True)
    p.add_argument("--base", default="dc=roda,dc=org")
    p.add_argument("--include-protected", action="store_true",
                   help="Also include admin/guest/administrators/users/guests")
    args = p.parse_args()

    stem = args.ldif_out.replace(".ldif", "")
    membership_path = args.membership_ldif_out or stem + "-membership.ldif"
    roles_path = args.roles_ldif_out or stem + "-roles.ldif"
    stats = transform(args.users, args.groups, args.base,
                      args.ldif_out, membership_path, roles_path, args.csv_out, args.include_protected)
    print(f"Done: {stats['users']} users, {stats['groups']} groups, "
          f"{stats['roles']} role assignments "
          f"({stats['skipped_users']} protected users skipped)", file=sys.stderr)
    print(f"  Add LDIF:        {args.ldif_out}", file=sys.stderr)
    print(f"  Membership LDIF: {membership_path}", file=sys.stderr)
    print(f"  Roles LDIF:      {roles_path}", file=sys.stderr)


if __name__ == "__main__":
    main()

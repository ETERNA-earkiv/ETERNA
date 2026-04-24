# Dynamisk grupptilldelning via IDP

ETERNA stöder dynamisk tilldelning av interna grupper baserat på gruppmedlemskap i en extern identitetsleverantör (IDP). Alla IDP:er som exponerar gruppmedlemskap via OIDC- eller SAML-attribut stöds, inklusive Azure AD / Microsoft Entra ID, Keycloak, Okta och andra. När en användare loggar in via CAS hämtas användarens gruppmedlemskap från IDP:n och mappas till interna ETERNA-grupper.

## Konfiguration

Gruppmappning konfigureras i `roda-core.properties`.

### Aktivera extern gruppmappning

```properties
core.authorization.external.group_mapping = true
core.authorization.external.attribute = memberOf
```

- `group_mapping = true` aktiverar funktionen.
- `attribute = memberOf` anger vilket OIDC/SAML-attribut som innehåller användarens grupper. Attributnamnet varierar mellan IDP:er — `memberOf` är det attribut som används av Azure AD, men din IDP kan använda ett annat namn (t.ex. `groups` eller ett eget attribut).

### Definiera mappningar

Varje mappning kopplar ett IDP-grupp-ID (matchat som regex) till en eller flera interna ETERNA-grupper:

```properties
core.authorization.external.mappings[] = <mappningsnamn>
core.authorization.external.mapping.<mappningsnamn>.external.group = <regex mot IDP-grupp-ID>
core.authorization.external.mapping.<mappningsnamn>.internal.groups[] = <intern grupp>
```

Flera mappningar kan definieras genom att upprepa `mappings[]`-posten med olika namn.

### Exempel: Azure AD / Microsoft Entra ID

I Azure AD identifieras grupper med sitt objekt-ID (GUID). Följande exempel mappar två Azure AD-grupper till interna ETERNA-grupper:

```properties
core.authorization.external.group_mapping = true
core.authorization.external.attribute = memberOf

# Administratörer
core.authorization.external.mappings[] = map_admins
core.authorization.external.mapping.map_admins.external.group = bbc466f2-55f4-4ca2-8303-d4238b8884d5
core.authorization.external.mapping.map_admins.internal.groups[] = administrators

# Standardanvändare
core.authorization.external.mappings[] = map_users
core.authorization.external.mapping.map_users.external.group = f390adab-5c81-4a89-8e72-c9cc107bf7d5
core.authorization.external.mapping.map_users.internal.groups[] = users
```

Värdet för `external.group` matchas som ett reguljärt uttryck mot det grupp-ID som returneras av IDP:n. För andra IDP:er kan grupp-ID:t vara ett namn, ett DN (LDAP-format) eller en annan sträng — se din IDP:s dokumentation.

## Begränsningar

### Inget stöd för nästlat (rekursivt) gruppmedlemskap

ETERNA kontrollerar enbart de grupper som returneras direkt i `memberOf`-attributet från IDP:n. Om en användare är medlem i Grupp A, och Grupp A är medlem i Grupp B, tilldelas **inte** de interna roller som är mappade mot Grupp B.

**Exempel på vad som inte fungerar:**
```
Användare → Grupp A (direkt medlem) → Grupp B (nästlad) → mappad till intern grupp
```
Användaren får inte den interna gruppen som är mappad mot Grupp B eftersom tillhörigheten är indirekt.

**Workaround:** Lägg till användare direkt i den IDP-grupp som är mappad i `roda-core.properties`, eller skapa en extra mappning mot den mellanliggande gruppen.

### Azure AD-beteende

Azure AD returnerar som standard bara direkta gruppmedlemskap i `memberOf`-attributet via OIDC. Transitiva (nästlade) medlemskap kräver ytterligare Graph API-anrop eller specifika appregistreringsinställningar som för närvarande inte hanteras av ETERNAs CAS-integration.



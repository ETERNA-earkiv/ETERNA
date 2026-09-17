# Gäståtkomst i ETERNA för sökportalen

Recept för att låta **oinloggade besökare** söka, läsa metadata och ladda ner
filer via sökportalen. Görs **en gång per ETERNA-miljö** (test, kund …). Inga
ändringar behövs i portalen och ingen källkod i ETERNA röras.

Utfört och verifierat 2026-09-17 mot lokal ETERNA (Spring Boot, port 8080,
OpenLDAP i Docker `standalone-openldap-1`). Kommandona nedan är exakt de som
kördes, med miljöspecifika värden markerade `<…>`.

## Bakgrund – varför tre steg

Portalen skickar oinloggade anrop vidare till ETERNA utan inloggning. ETERNA
behandlar dem som användaren **`guest`** (medlem i gruppen **`guests`**).
För att `guest` ska få se något måste två oberoende spärrar öppnas, och en
tredje inställning ser till att det förblir så:

| Steg | Spärr | Var | Effekt |
|---|---|---|---|
| 1 | **Roller** – vad gästen får *göra*. Gruppen `guests` har som standard inga roller alls. | LDAP | `aip.read`, `descriptive_metadata.read`, `representation.read` |
| 2 | **AIP-rättigheter** – vad gästen får *se*. Varje AIP har en egen rättighetslista; som standard är bara skaparen med. | ETERNA:s arkivdata (via API) | `READ` för `guests` på alla befintliga AIP:er |
| 3 | **Nya inleveranser** ska automatiskt få samma READ. | `roda-core.properties` (extern) | Nya AIP:er får `guests` READ vid skapande |

Gruppen `guests` är **skyddad** i ETERNA (`core.ldap.protectedGroups`), så
steg 1 går inte att göra i ETERNA:s administrationsgränssnitt – där ger det
"Illegal operation". Skyddet sitter bara i UI/API-lagret; ETERNA läser
rollerna direkt från LDAP, därför fungerar `ldapmodify`.

## Förutsättningar

- Åtkomst till ETERNA:s OpenLDAP med admin-DN (standard `cn=admin,dc=roda,dc=org`)
  och dess lösenord (`core.ldap.adminPassword` i `roda-core.properties`).
- Ett ETERNA-konto med rollen `aip.update` (t.ex. `admin`).
- På maskinen som kör skriptet: `bash`, `curl`, `python3`.
- ETERNA måste vara **helt uppstartad** (`Started RODA in …` i loggen) innan
  något anropar `/api/*`. Kontroll – ska ge **401**:

  ```bash
  curl -s -o /dev/null -w '%{http_code}\n' -u 'finnsinte:x' \
    -X POST http://<eterna>:8080/api/v2/aips/find \
    -H 'Content-Type: application/json' \
    -d '{"filter":{"parameters":[{"type":"AllFilterParameter"}]},"sublist":{"firstElementIndex":0,"maximumElementCount":1}}'
  ```
  403 betyder att API-autentiseringen låstes av vid uppstart – starta om
  ETERNA utan att något träffar API:t förrän den är klar.

## Steg 1 – ge gruppen `guests` läsroller (LDAP)

Fil: [`guests-roles.ldif`](guests-roles.ldif). Lägger `cn=guests` som
`roleOccupant` på de tre rollerna.

**LDAP i Docker (som här):**

```bash
docker exec -i <ldap-container> ldapmodify -c -x -H ldap://localhost:1389 \
  -D 'cn=admin,dc=roda,dc=org' -w '<ldap-adminlösenord>' < guests-roles.ldif
```

**LDAP nåbar direkt:**

```bash
ldapmodify -c -x -H ldap://<ldap-host>:<port> -D 'cn=admin,dc=roda,dc=org' \
  -w '<ldap-adminlösenord>' -f guests-roles.ldif
```

Förväntat: tre rader `modifying entry "cn=…,ou=roles,dc=roda,dc=org"`.
Flaggan `-c` (fortsätt vid fel) gör kommandot säkert att köra om: redan
tillagda värden ger `Type or value exists (20)` och hoppas över, exitkoden
blir 20 men resten läggs till. Utan `-c` stannar `ldapmodify` vid första
sådana rad och de följande posterna körs inte.

**Verifiera** (ingen omstart behövs – tar effekt direkt):

```bash
curl -s http://<eterna>:8080/api/v2/members/users/authenticated
```
→ `"id":"guest"` och `"allRoles":["aip.read","descriptive_metadata.read","representation.read"]`
(ordningen kan variera).

## Steg 2 – ge `guests` READ på alla befintliga AIP:er

Fil: [`grant-guests-read.sh`](grant-guests-read.sh). Hämtar alla AIP:er,
lägger till `guests` i READ per AIP – **befintliga rättigheter behålls** –
och skickar uppdateringen via `PATCH /api/v2/aips/permissions/update`.
Varje uppdatering körs som ett ETERNA-jobb som skriptet väntar in.

```bash
ETERNA_URL=http://<eterna>:8080 ETERNA_USER=admin ETERNA_PASSWORD='<lösenord>' \
  ./grant-guests-read.sh
```

Förväntat: en `OK`-rad per AIP och sist `Klart. Uppdaterade: N  Redan klara: 0  Fel: 0`.
Skriptet kan köras om; då blir alla `Redan klara`.

Vill ni använda en annan grupp än `guests`: `GROUP=<gruppnamn> ./grant-guests-read.sh`.

**Verifiera** – anonym sökning ska nu ge alla AIP:er:

```bash
curl -s -X POST http://<eterna>:8080/api/v2/aips/find \
  -H 'Content-Type: application/json' \
  -d '{"filter":{"parameters":[{"type":"AllFilterParameter"}]},"sublist":{"firstElementIndex":0,"maximumElementCount":1}}'
```
→ `"totalCount"` = antalet AIP:er i arkivet (0 här = steg 2 saknas; 403 = steg 1 saknas).

## Steg 3 – default-rättigheter för nya inleveranser

Fil: [`roda-core.properties.guests`](roda-core.properties.guests). Innehållet
ska in i ETERNA:s **externa** `roda-core.properties`, dvs.
`$RODA_HOME/config/roda-core.properties` (lokalt `~/.roda/config/`, i Docker
den monterade config-katalogen):

```properties
core.aip.default_permissions.group[] = guests
core.aip.default_permissions.group[].guests.permission[] = READ
```

Finns ingen extern `roda-core.properties` sedan tidigare räcker en fil med
bara dessa rader – ETERNA slår ihop extern och intern konfiguration
(`MergeCombiner`), så allt annat behålls. Finns filen redan: lägg till raderna.

**Kräver omstart av ETERNA.** Följ startordningen ovan (portalen stoppad,
vänta på `Started RODA in`, kontrollera 401).

**Verifiera:** skapa en tom AIP som admin och läs dess rättigheter, ta sedan bort den:

```bash
curl -s -u admin:'<lösenord>' -X POST 'http://<eterna>:8080/api/v2/aips?type=MIXED' \
  -H 'Content-Type: application/json'
```
→ i svaret: `"permissions":{ … "groups":{"READ":["guests"], …}}`. Ta bort test-AIP:n
i ETERNA:s gränssnitt eller med:

```bash
curl -s -u admin:'<lösenord>' -X POST http://<eterna>:8080/api/v2/aips/delete \
  -H 'Content-Type: application/json' \
  -d '{"itemsToDelete":{"@type":"SelectedItemsListRequest","ids":["<ny-aip-id>"]},"details":"Tar bort test-AIP"}'
```

## Slutkontroll via portalen

Med portalen startad, i ett **inkognitofönster** (ingen cookie):

- `/sok` visar träffar utan inloggning.
- En träff går att fälla ut (metadata) och *Öppna och ladda ner fil* fungerar.
- Admin-menyn syns inte; knappen *Logga in som administratör* visas.

## Ta bort gäståtkomsten igen

- Steg 1 ångras med samma LDIF där `add:` byts till `delete:` på alla tre poster.
- Steg 2 ångras per AIP i ETERNA:s gränssnitt (Rättigheter → ta bort `guests`),
  eller genom att sätta `GROUP=guests` och anpassa skriptet till `remove`.
- Steg 3 ångras genom att ta bort raderna och starta om.

## Att tänka på

- Allt material i arkivet blir läsbart för alla utan inloggning – även i
  ETERNA:s eget webbgränssnitt. Material som ska undantas hanteras per AIP
  genom att ta bort `guests` READ på just den AIP:n (och dess barn).
- Steg 1 och 2 är **data** i respektive miljö och följer inte med en
  ETERNA-uppgradering automatiskt bort, men ingår inte heller i en ny
  installation – kör receptet igen på varje ny miljö.
- Vill ni längre fram kunna hantera `guests` roller i ETERNA:s gränssnitt
  krävs en ändring i ETERNA:s källkod (`core.ldap.protectedGroups` i den
  interna `roda-core.properties`) – extern konfiguration kan bara lägga till
  listvärden, inte ta bort.

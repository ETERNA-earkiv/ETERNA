# Gallringsregler: "Tillämpa regler" matchade annorlunda än förhandsgranskningen

## Problem
En `METADATA_FIELD`-gallringsregel kunde förhandsgranskas och visa träffar, men jobbet
**"Tillämpa gallringsregler i arkivet"** (`ApplyDisposalRulesPlugin`) kopplade inget schema —
så inget dök upp under förfallna gallringar. För flervärda fält kraschade jobbet helt.

## Grundorsak
Förhandsgranskningen (`DisposalRuleDataPanel`) byggde en Solr-`Filter`, medan apply-jobbet
(`ApplyDisposalRulesPluginUtils`) gjorde `(String) value` + `String.equals` mot indexvärdet.
De två matchningarna skrevs separat och divergerade:

- **Tokeniserade fält** (`TYPE_TEXT`): exakt likhet matchade aldrig ett värde där villkoret
  bara är ett token, t.ex. `Luftfartyg` i `Köpebrev (Luftfartyg)`.
- **Flervärda fält** (`_txt`): indexvärdet är en `ArrayList` → `ClassCastException`.

## Åtgärd
`conditionTypeMetadataValue` gör nu en Solr-count scopad till AIP:n (`INDEX_UUID` +
`conditionKey:conditionValue` + `AIP_STATE=ACTIVE`) — samma semantik som förhandsgranskningen.
Det du förhandsgranskar är det som tillämpas.

Härdning (från kodgranskning):
- Tillåtna villkorsfält beräknas i core (`allowedMetadataConditionFields`) och delas av både
  jobbet och API-valideringen (`DisposalRuleService`). Tillåtna = textsökfälten UI:t erbjuder
  (`ui.search.fields.IndexedAIP.*` typ `text`) minus `ui.disposal.rule.blacklist.condition`.
  Blacklisten jämförs mot konfignyckeln, inte Solr-fältet — som UI:t.
- Apply-jobbet whitelistar `conditionKey` och hoppar över tomma/ovitlistade villkor i stället
  för att skriva dem rått in i Solr-queryn (injection/DoS) eller krascha (NPE).
- Allowlisten beräknas en gång per jobbkörning och loggar en varning om den är tom medan
  metadataregler finns (t.ex. headless-kontext utan WUI-config).

## Test
`ApplyDisposalRulesPluginTest` täcker: tokeniserad matchning kopplar schemat, tomt villkor
kraschar inte jobbet, och blacklist gäller konfignyckeln (inte Solr-fältet).

# Gallringsregler: "Tillämpa regler" matchar annorlunda än förhandsgranskningen

## Sammanfattning

En gallringsregel (`DisposalRule`) av typen `METADATA_FIELD` kan förhandsgranskas i UI:t
och visa matchande strukturenheter, men när jobbet **"Tillämpa gallringsregler i arkivet"**
(`ApplyDisposalRulesPlugin`) körs kopplas inget gallringsschema till samma enheter. För
flervärda metadatafält kraschar dessutom jobbet helt.

Följden är att inga enheter dyker upp under **förfallna gallringar**, trots att
förhandsgranskningen lovar träffar.

## Reproduktion

1. Leverera in en strukturenhet vars indexerade metadata innehåller ett **tokeniserat**
   fält (`Field.TYPE_TEXT`) eller ett **flervärt** fält (Solr dynamiskt `_txt`), t.ex. en
   AIP med titel `My example` eller ett `recordType_txt` med värden som `Köpebrev (Luftfartyg)`.
2. Skapa ett gallringsschema med `actionCode = DESTROY`.
3. Skapa en gallringsregel: typ `METADATA_FIELD`, `conditionKey = <fält>`,
   `conditionValue = <ord som förekommer inuti fältvärdet>` (t.ex. `example` resp. `Luftfartyg`).
4. **Förhandsgranska** regeln → enheten visas som matchande. ✅
5. Kör jobbet **"Tillämpa gallringsregler i arkivet"**.

### Förväntat
Enheten matchar, gallringsschemat kopplas (`associationType = RULES`), `overdueDate`
beräknas och enheten listas under förfallna gallringar.

### Faktiskt (före fix)
- **Tokeniserat enkelvärt fält:** jobbet rapporterar `SKIPPED` — *"did not match any
  disposal rule"*. Inget schema kopplas.
- **Flervärt fält:** jobbet misslyckas (`FAILED_TO_COMPLETE`) med
  `java.lang.ClassCastException: class java.util.ArrayList cannot be cast to class
  java.lang.String`. Alla källobjekt misslyckas.

## Grundorsak

Förhandsgranskning och tillämpning delar **ingen gemensam matchningslogik** — de skrevs
separat och har glidit isär:

| | Var | Hur den matchar |
|---|---|---|
| **Förhandsgranskning** | `DisposalRuleDataPanel#refreshPreviewAIPList` | Bygger en Solr-`Filter` (`SimpleFilterParameter(conditionKey, conditionValue)`) → tokeniserad sökmotor-matchning |
| **Tillämpning (jobb)** | `ApplyDisposalRulesPluginUtils#conditionTypeMetadataValue` | Hämtade indexvärdet och gjorde `(String) value` + `String.equals(conditionValue)` |

Den ursprungliga matchningen (upstream RODA, commit `573789919` "Fix #1005", 2022) antog att
`conditionKey` alltid pekar på ett **enkelvärt, exakt strängfält**. Det antagandet håller
inte för:

- **Tokeniserade fält** (`TYPE_TEXT`): `String.equals("example")` mot det lagrade värdet
  `"My example"` ger ingen träff, medan Solr-frågan `title:"example"` matchar tokenet.
- **Flervärda fält** (`_txt`): indexvärdet är en `ArrayList`, så `(String) value` kastar
  `ClassCastException`.

Eftersom förhandsgranskningen går via Solr men jobbet gör en rå Java-jämförelse blir
förhandsgranskningen **missvisande** — den lovar något jobbet inte levererar. För en
funktion som *förstör data* är det allvarligt: man kan inte lita på det man ser innan man
kör jobbet.

## Åtgärd

Låt jobbet utvärdera regeln med **samma Solr-fråga som förhandsgranskningen**, scopad till
en strukturenhet i taget (`INDEX_UUID = aip.getId()` + `conditionKey:conditionValue`).
Därmed finns bara en matchningssemantik kvar — *det du förhandsgranskar är det som
tillämpas* — och `ClassCastException` på flervärda fält försvinner.

Ändring: `roda-core/.../disposal/rules/ApplyDisposalRulesPluginUtils.java`
(`conditionTypeMetadataValue`).

## Härdning (från kodgranskning)

Eftersom apply-vägen nu bygger en Solr-fråga av `conditionKey`/`conditionValue` tillkom tre
skydd:

- **Delad whitelist** (`ApplyDisposalRulesPluginUtils.allowedMetadataConditionFields`): de
  tillåtna villkorsfälten beräknas i core och återanvänds av **både** apply-jobbet och
  API-valideringen, så de aldrig kan glida isär. Tillåtna fält = textsökfälten UI:t erbjuder
  (`ui.search.fields.IndexedAIP.*` med typ `text`), minus
  `ui.disposal.rule.blacklist.condition`. Blacklisten jämförs mot konfigurations*nyckeln*
  (t.ex. `reference`), inte det upplösta Solr-fältet (`unitId_txt`) — exakt som UI:t.
- **Apply-jobbet whitelistar nu också** (inte bara API:t): en regel vars `conditionKey` inte
  är vitlistat hoppas över i stället för att skrivas rått in i Solr-queryn
  (`SolrUtils#appendExactMatch` escapear inte fältnamnet). Det stänger query-injection/DoS-
  vektorn även för äldre/manipulerade regler som skapats utanför API-valideringen.
- **Server-validering** (`DisposalRuleService.validateDisposalRule`): för `METADATA_FIELD`
  krävs icke-tomma `conditionKey`/`conditionValue`, och `conditionKey` måste finnas i den
  delade whitelisten.
- **Defensivt skydd**: en regel med tomt villkor hoppas över i stället för att krascha hela
  jobbet (tidigare `NullPointerException` i `SolrUtils#appendExactMatch` på ett `null`-värde).
- **Preview-paritet**: filtret i apply-jobbet inkluderar nu även `AIP_STATE=ACTIVE`, precis
  som förhandsgranskningen.

## Test

`ApplyDisposalRulesPluginTest#applyRuleMatchesTokenizedMetadataFieldLikePreview` reproducerar
fallet: en AIP med titeln `My example`, en `DESTROY`-regel med `conditionValue = "example"`
(ett token *inuti* titeln). Testet är grönt med fixen och skulle ge `SKIPPED`/ingen koppling
med den gamla `String.equals`-logiken.

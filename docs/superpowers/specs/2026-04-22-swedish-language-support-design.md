# Design: Svenskt språkstöd — saknade översättningar

**Datum:** 2026-04-22
**Branch:** feat/swedish-language-support
**Status:** Godkänd

## Bakgrund

ETERNA har en svensk språkfil men 126 nycklar saknar svenska översättningar, vilket gör att engelska texter visas i gränssnittet. Målet är att inga engelska texter ska synas i ETERNAs ordinarie gränssnitt (loggar undantagna).

## Scope

### Filer som ändras
1. `roda-ui/roda-wui/src/main/resources/config/i18n/client/ClientMessages_sv_SE.properties` — 102 nycklar
2. `roda-ui/roda-wui/src/main/resources/config/i18n/ServerMessages_sv_SE.properties` — 24 nycklar

### Avgränsning
- Endast saknade nycklar läggs till — befintliga översättningar rörs inte
- Inga Java- eller GWT-kodfiler ändras
- Ingen kvalitetsgranskning av befintliga översättningar i detta skede
- Framtida i18n-förbättringar hanteras i separat feature

## Översättningsstrategi

Direkt 1:1-översättning av engelska termer till svenska med arkivterminologi där det är relevant.

**Exempel:**
| Engelska | Svenska |
|----------|---------|
| Destroy intellectual entities | Gallra logiska enheter |
| Lift disposal hold | Häv gallringsstopp |
| User does not have permission | Användaren saknar behörighet |
| Creator | Skapare |
| Disposal confirmations | Gallringsbekräftelser |

## Commit-struktur

Varje commit är en självständig enhet som kan återrullas med `git revert` utan att påverka övriga ändringar.

| Commit | Innehåll | Antal nycklar |
|--------|----------|---------------|
| 1 | Gallringsrelaterade texter (ClientMessages) | ~20 |
| 2 | Felmeddelanden & anledningar (ClientMessages) | ~25 |
| 3 | Detaljvy-etiketter & UI-texter (ClientMessages) | ~30 |
| 4 | Dialog-texter (ClientMessages) | ~15 |
| 5 | ServerMessages — revisionslogs & e-post | 24 |

## Definition of Done

- Inga engelska texter syns i ETERNAs ordinarie gränssnitt när språket är inställt på svenska
- Loggar är undantagna från kravet
- Alla 126 nycklar är tillagda i rätt filer
- Varje commit är reversibel

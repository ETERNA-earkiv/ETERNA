# REVIEW_RULES_CORE.md

> **Ägare:** Hela teamet · **Versionshanterad i:** `main`
> Dokumentet uppdateras löpande. Förslag till ändringar görs via PR med minst ett godkännande.

---

## Syfte

Det här dokumentet definierar de granskningsregler som gäller för all kod i ETERNA-repot.
Reglerna är anpassade till ETERNAs lagerstyrda arkitektur och kompletterar den automatiserade
granskningen i CodeRabbit (`.coderabbit.yaml`).

En PR som bryter mot en regel märkt **Blockerande** får inte mergas förrän problemet är åtgärdat.
En PR med en **Varning** kan mergas efter att granskaren explicit kvitterat punkten i PR-kommentarerna.

---

## Arkitekturöversikt

```
roda-ui  (GWT)       Presentationslager – ingen affärslogik
roda-common          Delade modeller och interfaces
roda-core            Affärslogik och preservation workflows
Plugins              AbstractPlugin – självständiga enheter
```

**Kritisk regel:** `roda-ui` får aldrig importera implementationsklasser direkt från `roda-core`.
Plugins får aldrig ha beroenden till `roda-ui` eller GWT-ramverket.
Kommunikation mellan lager sker uteslutande via det definierade API-lagret.

---

## Regler

### R1 · Lagerintegriteten är okränkbar

| | |
|---|---|
| **Typ** | Blockerande |
| **Kontroll** | Automatisk (CodeRabbit) |

Ingen klass får importera implementationsklasser från ett lager den inte tillhör.
Tillåtna beroenderiktningar:

- `roda-ui` → API-lagret i `roda-core` ✅
- `roda-ui` → `roda-common` ✅
- `roda-ui` → implementationsklasser i `roda-core` ❌
- Plugin → `roda-core` / `roda-common` via API ✅
- Plugin → `roda-ui` eller `com.google.gwt.*` ❌

CodeRabbit flaggar automatiskt vid överträdelse. Arkitektansvarig beslutar om undantag
finns – men undantag ska alltid dokumenteras explicit i PR-beskrivningen.

---

### R2 · Plugins är självständiga enheter

| | |
|---|---|
| **Typ** | Blockerande – eskaleras till arkitektansvarig |
| **Kontroll** | Flaggas av CodeRabbit, eskaleras manuellt |

En plugin-PR som kräver samtidiga ändringar i `roda-core` eller `roda-common` ska alltid
eskaleras till arkitektansvarig. Det är inte automatiskt fel – men det kräver explicit
motivering i PR-beskrivningen och ett godkännande från arkitektansvarig.

Kontrollera vid granskning:
- Kräver PR:n ändringar i fler än ett lager?
- Är motiveringen dokumenterad i PR-beskrivningen?
- Har arkitektansvarig godkänt?

---

### R3 · Asynkrona callbacks hanterar alltid fel

| | |
|---|---|
| **Typ** | Blockerande |
| **Kontroll** | Automatisk (CodeRabbit) |

Varje implementering av `GWT AsyncCallback` måste ha en `onFailure`-metod med faktisk
felhantering. En tom kropp eller en metod som enbart loggar utan att informera användaren
eller återställa state godkänns inte.

```java
// ❌ Otillräckligt
public void onFailure(Throwable caught) {
    // TODO
}

// ✅ Godkänt
public void onFailure(Throwable caught) {
    AsyncCallbackUtils.defaultFailureTreatment(caught);
}
```

---

### R4 · Intern state exponeras aldrig direkt

| | |
|---|---|
| **Typ** | Varning |
| **Kontroll** | Manuell granskning vid 🟡-utfall |

Publika get-metoder som returnerar samlingar eller mutable objekt ska returnera defensiva
kopior, inte referenser till interna fält.

```java
// ❌ Exponerar intern state
public List<String> getItems() {
    return this.items;
}

// ✅ Defensiv kopia
public List<String> getItems() {
    return Collections.unmodifiableList(this.items);
}
```

---

### R5 · All UI-text hanteras via i18n

| | |
|---|---|
| **Typ** | Varning |
| **Kontroll** | Automatisk (CodeRabbit) |

Hårdkodade strängar i `.java`-filer under `roda-ui` som är avsedda att visas i gränssnittet
ska ligga i `ClientMessages`-gränssnitt. Undantag gäller tekniska konstanter, loggar och
testfiler.

```java
// ❌ Hårdkodad UI-sträng
label.setText("Behörighet saknas");

// ✅ Via i18n
label.setText(constants.permissionDenied());
```

---

### R6 · Javadoc på publika metoder

| | |
|---|---|
| **Typ** | Varning |
| **Kontroll** | Automatisk (CodeRabbit) |

Täckning för publika metoder under 80 % är en varningsflagga. CodeRabbit rapporterar
saknade metoder i PR-kommentarerna. Granskaren kvitterar varje punkt eller åtgärdar
innan merge.

Plugins och `roda-core`-klasser prioriteras. Undantag kan göras för enkla data-klasser
(POJO/record) om det motiveras i PR-beskrivningen.

---

## Beslutsmatris – vem godkänner?

| CodeRabbit-utfall | Vem godkänner? | Villkor |
|---|---|---|
| 🟢 Inga flaggor | Vem som helst i teamet | CodeRabbit-granskning bifogad i PR |
| 🟡 Nitpicks / minor | Junior utvecklare läser och kvitterar | Varje punkt ska kommenteras i PR:n |
| 🔴 Potential issue | Ingen merge | Åtgärda → ny granskning → börja om |
| ⚠️ Lagergräns korsas | Arkitektansvarig beslutar | Kräver explicit motivering |
| ⚠️ Plugin kräver core-ändring | Arkitektansvarig beslutar | Alltid eskalera – se R2 |

---

## Vad systemet inte fångar

Reglerna ovan fångar väldefinierade strukturella problem. De ersätter inte:

- Arkitektoniska misstag som byggs upp gradvis över tid
- Fel logik i välstrukturerad kod
- Domänspecifika säkerhetsbrister inom e-arkivering och OAIS-efterlevnad

**Kompletterande åtgärd:** En Java-arkitekt på 2–4 timmar per månad för att granska
mönster och trender – inte enskilda PRs – täcker det som automatiserad granskning missar.
Fokus bör ligga på `roda-core` och plugin-gränssnitten.

---

*Senast uppdaterad: april 2026 · Förslag till ändringar görs via PR mot `main`*

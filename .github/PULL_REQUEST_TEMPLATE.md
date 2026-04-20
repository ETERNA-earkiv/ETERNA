## Beskrivning

<!-- Beskriv vad PR:n gör och varför. -->

## Berörda lager

<!-- Markera alla lager som berörs av denna PR. -->

- [ ] `roda-ui`
- [ ] `roda-common`
- [ ] `roda-core`
- [ ] Plugin: <!-- namn -->

## Checklista

### Lagerintegriteten (R1)
- [ ] `roda-ui` importerar inga implementationsklasser direkt från `roda-core`
- [ ] Inga plugins har beroenden till `roda-ui` eller `com.google.gwt.*`

### Affärslogik i UI (R1)
- [ ] UI-ändringar innehåller ingen affärslogik, state-management eller låsningslogik
      <!-- Om ja – beskriv varför det ändå är rätt: -->

### Plugin-självständighet (R2)
- [ ] PR:n kräver **inte** samtidiga ändringar i `roda-core` eller `roda-common`
      <!-- Om den gör det – motivera och tagga @arkitekt som reviewer: -->

### Asynkron felhantering (R3)
- [ ] Alla `AsyncCallback`-implementationer har en fungerande `onFailure`-metod
- [ ] Inte tillämpligt (inga asynkrona anrop i denna PR)

### i18n (R5)
- [ ] Inga hårdkodade UI-strängar – all text via `ClientMessages`
- [ ] Inte tillämpligt (inga UI-strängar i denna PR)

### CodeRabbit-granskning
- [ ] CodeRabbit har granskat PR:n
- [ ] Utfall: 🟢 Inga flaggor / 🟡 Nitpicks kvitterade nedan / *(ta bort ej tillämpliga)*

## Dokumentation
- [ ] Relevant dokumentation eller konfiguration är uppdaterad
- [ ] Inte tillämpligt (PR implementerar inga nya funktioner eller förändringar som påverkar dokumentationen)

### Övrigt
- [ ] PR:n är kopplad till relevant issue eller task i Github. Under Development, klicka på hjulet och sök upp rätt issue
- [ ] Rätt taggar är satta. (t ex 1.X eller 0.X)
- [ ] PR:n är rimligt avgränsad och möjlig att granska
- [ ] Eventuella risker, beroenden, migrationer eller manuella steg är beskrivna
- [ ] Nya beroenden är säkerhetstestade
- [ ] PR:n har inga olösta konflikter
- [ ] Det finns inget uppenbart som blockerar merge



<!-- Om 🟡 – lista varje punkt och hur den hanterades:
- Punkt 1: ...
- Punkt 2: ...
-->

---

> 🔴 **Potential issue** från CodeRabbit = ingen merge förrän åtgärdat och omgranskat.
> ⚠️ **Lagergräns korsas eller plugin kräver core-ändring** = arkitektansvarig beslutar.

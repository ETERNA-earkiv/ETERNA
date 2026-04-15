# ETERNA — instruktioner för Claude

## Commit-strategi

**Committa ofta och tidigt.** Gör en commit efter varje logisk förändring — det behöver inte vara en "stor" förändring för att förtjäna en commit. Vänta inte med att committa tills en hel feature är klar.

- Committa efter varje fil eller grupp av relaterade filer är ändrad
- Committa efter att ett test passerar
- Committa efter att en bugg är fixad
- Committa efter dokumentationsändringar
- Committa innan du påbörjar nästa deluppgift

**Branch-konvention:** Skapa alltid feature-branches från `eterna-v1-alpha`, aldrig från `main`. Namnge branches `feat/<issue-nummer>-kort-beskrivning`.

**Commit-format:** `<type>: <beskrivning>` — tillåtna typer: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `perf`, `ci`. Länka issues med `Closes #<nummer>`.

**PR:** Skapa inte PR utan att användaren explicit bett om det.

## Miljö

- Git-kommandon måste köras via WSL: `wsl -d Ubuntu -- bash -c "cd ~/ETERNA && git <kommando>"`
- Se `C:\Users\AnnaJansson\Claude\ETERNA.md` för fullständig projektdokumentation

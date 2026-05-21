# Administration av Portal-API-användare

## Översikt

Denna dokumentation beskriver administrationen av API-användaren som möjliggör åtkomst till den externa portalen. Det denna användare har behörighet till kommer att kunna visas på den publika portalen. Det är väldigt viktigt att behörighetshanteringen av denna användare sköts korrekt för att inte tillgängligöra sekretessbelagd information.

## Portal-API-användaren

Portal-API-användaren är ett särskilt användarkonto som skapats specifikt för att ge det externa portalen åtkomst till nödvändiga resurser via API. Detta konto är redan konfigurerat och har en tilldelad API-nyckel.

### Viktiga egenskaper:
- **Dedikerat konto**: Används enbart för portal-integration
- **API-nyckel**: Unik nyckel för autentisering av API-anrop
- **Begränsade behörigheter**: Ska endast ha nödvändiga rättigheter för portalens funktion

## Behörigheter och säkerhetsrisker

### Rekommenderade behörigheter
- Portal-API-användaren bör ej tillhöra någon grupp
- Grundläggande läsbehörighet till publika dokument
- Ingen administrativ åtkomst

### Specifika rekommenderade behörigheter

#### Förvaringsenheter (AIP)

- Hämta förvaringsenheter (AIP)
- Lista och sök efter förvaringsenheter (AIP)

#### Representationer och filer
- Lista och sök i representationer och filer
- Hämta representationer och filer

#### Beskrivande metadata
- Lista och hämta beskrivande metadata


### ⚠️ **OBS! Kritiska säkerhetsrisker**

Att placera Portal-API-användaren i grupper med utökade behörigheter eller på fel del av arkivbeståndet utgör allvarliga säkerhetsrisker såsom: 

- **Dataexponering**: Om portalen komprometteras kan känsliga arkivdokument läckas.
- **Efterlevnadsbrott**: Överträdelser av dataskyddsregler (Dataskyddslagen, GDPR, etc.)
- **Brott mot offentlighets- och sekretesslagen**: Om uppgifter som innefattas av sekretess publiceras offentligt. 

## Administrationsriktlinjer
Det är viktigt att verksamheten har en administratör för behörighetshantering. Detta kan till exempel vara e-arkivarien.
### Regelbundna kontroller
Som administratör ansvarar du för att regelbundet granska:
- Användarens gruppmedlemskap
- Aktiva behörigheter
- API-nyckelns användningsmönster
- Åtkomstloggar

### Åtgärder vid misstanke om kompromettering
1. **Omedelbar isolering**: Ta bort användaren från alla grupper
2. **API-nyckelrotation**: Generera ny nyckel och uppdatera portalen
3. **Loggranskning**: Undersök alla aktiviteter under den senaste perioden
4. **Incidentrapportering**: Dokumentera händelsen för framtida referens

### Bästa praxis
- **Minsta behörighetsprincip**: Ge endast nödvändiga rättigheter
- **Regelbunden granskning**: Månadsvis kontroll av behörigheter

## Kontakt och support

Vid frågor eller behov av behörighetsändringar, kontakta systemadministratören eller säkerhetsansvarig innan några åtgärder vidtas.

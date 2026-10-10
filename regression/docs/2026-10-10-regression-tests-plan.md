# Automatiska regressionstester för ETERNA — implementationsplan

> **För agenter:** Obligatorisk sub-skill: använd superpowers:subagent-driven-development (rekommenderas) eller superpowers:executing-plans för att genomföra planen uppgift för uppgift. Stegen använder checkboxar (`- [ ]`) för att följa upp arbetet.

**Mål:** En black-box-regressionssvit i `regression/` som startar en kortlivad ETERNA-stack, testar ingest, sökning, nedladdning, behörigheter och felfall via V2 REST API, och sedan river stacken.

**Arkitektur:** Playwright Test kör tre projekt: `unit` (rena hjälpfunktioner, ingen stack), `smoke` (läser bara) och `core` (beror på smoke). Worker-fixtures ger en inloggad klient, ett körnings-ID, ett giltigt SIP som ingestas en gång per worker och en städare som tar bort testdata vid teardown. SIP:ar byggs i minnet med `fflate`. Stacken är en compose-fil som speglar `deploys/standalone/docker-compose.yaml`, och varje image kan överstyras.

**Teknikstack:** TypeScript, `@playwright/test` 1.63.0 (bara `request`-API, inga webbläsare), `fflate` 0.8.2, Bun 1.4 som pakethanterare och skriptkörare, Node ≥ 22 som runtime för Playwright, Docker Compose.

**Spec:** [2026-10-10-regression-tests-design.md](2026-10-10-regression-tests-design.md)

> **Ändrat efter kodgranskning av uppgift 1–5 (commit `8a72db4d5`):** koden i uppgift 3–5 nedan visar den ursprungliga versionen. Den committade koden har dessutom: `redactSecrets` och privat `http` ersatt av Nodes inbyggda `fetch` (så att inga headers hamnar i Playwrights rapport), publik `getRaw(path, headers?)` som ger `{ status, body }` i `client.ts`, `Cleanup.trackIngestJob` (ersätter `trackAip`; AIP:er hittas via `ingestJobId` vid städning), `waitForReports` som kastar vid timeout, validering av filnamn i `sip.ts` och scheman från `commons-ip/src/main/resources/schemas2/`. Uppgift 6 och framåt bygger på den committade koden.

---

## Bakgrund för den som genomför planen

- Arbeta på grenen `feat/regression-tests` i `/home/jerry/Git/eterna-stack/ETERNA`. Alla sökvägar nedan är relativa till repots rot om inget annat anges.
- **Kör aldrig `mvn formatter:format`.** Sviten rör ingen Java-kod, så Maven behövs inte alls.
- API-fakta är kartlagda i koden. De viktigaste:
  - Basic Auth krävs på `/api/*`. Ett anrop utan header blir användaren `guest`, inte 401. Fel lösenord ger 401.
  - `find` görs med `POST /api/v2/<entitet>/find` och body `{"filter":{"parameters":[...]},"onlyActive":false,"sublist":{...}}`. Filterparametrar har diskriminatorn `"type"`. En tom parameterlista ger tomt resultat, så använd `AllFilterParameter`.
  - Urval (`SelectedItemsListRequest`) har diskriminatorn `"@type"`.
  - Ingest: ladda upp via `POST /api/v2/transfers/create/resource` (multipart-del `resource`, fält `commit=true`) och starta sedan `POST /api/v2/jobs` med `ConfigurableIngestPlugin`.
  - Jobbets slutlägen är `COMPLETED`, `FAILED_TO_COMPLETE`, `STOPPED`, `FAILED_DURING_CREATION`, `REJECTED`, `PENDING_APPROVAL` och `SCHEDULED`. Ett ogiltigt SIP ger troligen `COMPLETED` med `sourceObjectsProcessedWithFailure=1`.
  - Rapporter hämtas med `GET /api/v2/jobs/{id}/reports?start=0&limit=100&job-just-failed=false` och ger `{"reports":[...]}`. `outcomeObjectId` är AIP-id:t, eller `NO_OUTCOME_ID` om ingen AIP skapades.
  - Preservation events filtreras på `aipID` (stort ID). `eventType` är text med gemener (`virus check`, `format identification`, `message digest calculation`) och `eventOutcome` är `SUCCESS`, `FAILURE` och så vidare.
  - Lösenord vid skapande av användare skickas som `{"chars":"..."}`, inte som en sträng.
  - Versionen finns i `/version.json` under nyckeln `git.build.version`.
- Några punkter är bara verifierade genom att läsa koden. De listas i `regression/docs/api-probes.md` (uppgift 6) och bekräftas eller rättas när testerna körs mot stacken för första gången.

## Filstruktur

| Fil | Ansvar |
|---|---|
| `regression/package.json` | Beroenden och skript |
| `regression/tsconfig.json` | Typkontroll (`bun run typecheck`) |
| `regression/playwright.config.ts` | Projekten unit, smoke och core; läser `.env` |
| `regression/.gitignore`, `regression/.env.example` | Ignorerade filer och mall för miljövariabler |
| `regression/src/config.ts` | Läser och validerar miljövariabler |
| `regression/src/run-id.ts` | Körnings-ID |
| `regression/src/types.ts` | Typer för API-svaren |
| `regression/src/filters.ts` | Byggare för filterparametrar |
| `regression/src/client.ts` | API-klient med Basic Auth och felbeskrivningar |
| `regression/src/jobs.ts` | Polling av jobb, rapporter och stegsökning |
| `regression/src/ingest.ts` | Ingest från början till slut och väntan på indexering |
| `regression/src/sip.ts` | Bygger CSIP-SIP och trasigt SIP |
| `regression/src/eicar.ts` | EICAR-fil och EICAR-SIP |
| `regression/src/cleanup.ts` | Spårar och tar bort testdata |
| `regression/src/fixtures.ts` | Playwright-fixtures |
| `regression/fixtures/schemas/*.xsd` | Scheman som läggs i SIP:ar |
| `regression/env/docker-compose.yaml`, `up.sh`, `down.sh` | Kortlivad stack |
| `regression/scripts/regression.sh` | up → test → loggar → down |
| `regression/tests/unit/*.spec.ts` | Enhetstester |
| `regression/tests/smoke/*.spec.ts` | Rökprov |
| `regression/tests/core/*.spec.ts` | Kärnflöde |
| `regression/docs/api-probes.md` | Verifierade API-fakta och avvikelser |
| `regression/README.md` | Användning |
| `.github/workflows/regression.yml` | CI |

---

### Uppgift 1: Grundstruktur

**Filer:**
- Skapa: `regression/package.json`, `regression/tsconfig.json`, `regression/playwright.config.ts`, `regression/.gitignore`, `regression/.env.example`, `regression/src/config.ts` (stub)

- [ ] **Steg 1: Skapa `regression/package.json`**

```json
{
  "name": "eterna-regression",
  "private": true,
  "scripts": {
    "test": "playwright test",
    "test:unit": "playwright test --project=unit",
    "test:smoke": "playwright test --project=smoke",
    "test:core": "playwright test --project=core",
    "regression": "./scripts/regression.sh",
    "report": "playwright show-report",
    "typecheck": "tsc"
  }
}
```

- [ ] **Steg 2: Installera beroenden med exakta versioner**

Kör i `regression/`:
```bash
bun add -d --exact @playwright/test@1.63.0 fflate@0.8.2 typescript@5.9.3 @types/node@24.19.2
```
Förväntat: `package.json` får `devDependencies` med exakta versioner och `bun.lock` skapas.

- [ ] **Steg 3: Skapa `regression/tsconfig.json`**

```json
{
  "compilerOptions": {
    "target": "ES2022",
    "module": "commonjs",
    "moduleResolution": "node",
    "strict": true,
    "esModuleInterop": true,
    "skipLibCheck": true,
    "noEmit": true,
    "types": ["node"]
  },
  "include": ["src", "tests", "playwright.config.ts"]
}
```

- [ ] **Steg 4: Skapa `regression/.gitignore`**

```
node_modules/
playwright-report/
test-results/
.env
```

- [ ] **Steg 5: Skapa `regression/.env.example`**

```bash
# Kopiera till .env för att köra mot en annan miljö än den kortlivade stacken.
# Variabler som redan är satta i miljön har företräde framför .env.
ETERNA_URL=http://localhost:8080
ETERNA_USER=admin
ETERNA_PASSWORD=eterna
# EXPECTED_VERSION=1.0.1
# JOB_TIMEOUT_MS=600000
# JOB_POLL_MS=3000
# KEEP_DATA=1

# Bara för den kortlivade stacken (env/docker-compose.yaml):
# ETERNA_PORT=8080
# ETERNA_IMAGE=ghcr.io/eterna-earkiv/eterna:v1.0.0
# SOLR_IMAGE=ghcr.io/eterna-earkiv/solr:9.10.1
# ZOOKEEPER_IMAGE=ghcr.io/eterna-earkiv/zookeeper:3.9.5
# CLAMAV_IMAGE=docker.io/clamav/clamav:stable
# SIEGFRIED_IMAGE=ghcr.io/eterna-earkiv/siegfried:v1.11.4
# OPENLDAP_IMAGE=docker.io/bitnamilegacy/openldap:2.6.10
# UNOSERVER_IMAGE=ghcr.io/eterna-earkiv/eterna-unoserver:v3.7.0
# MAILPIT_IMAGE=axllent/mailpit:v1.24
```

- [ ] **Steg 6: Skapa en stub för `regression/src/config.ts`** (den riktiga implementationen kommer i uppgift 2)

```ts
export interface Config {
  url: string;
  user: string;
  password: string;
  expectedVersion?: string;
  jobTimeoutMs: number;
  jobPollMs: number;
  keepData: boolean;
}

export function loadConfig(env: Record<string, string | undefined> = process.env): Config {
  return {
    url: env.ETERNA_URL ?? 'http://localhost:8080',
    user: 'admin',
    password: 'eterna',
    jobTimeoutMs: 600_000,
    jobPollMs: 3_000,
    keepData: false,
  };
}
```

- [ ] **Steg 7: Skapa `regression/playwright.config.ts`**

```ts
import { existsSync } from 'node:fs';
import { join } from 'node:path';
import { defineConfig } from '@playwright/test';
import { loadConfig } from './src/config';

// Variabler som redan finns i miljön har företräde framför .env.
const envFile = join(__dirname, '.env');
if (existsSync(envFile)) process.loadEnvFile(envFile);

const cfg = loadConfig();

export default defineConfig({
  testDir: './tests',
  workers: 1,
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  timeout: 120_000,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: cfg.url,
    extraHTTPHeaders: { Accept: 'application/json' },
  },
  projects: [
    { name: 'unit', testDir: './tests/unit' },
    { name: 'smoke', testDir: './tests/smoke', retries: 1 },
    { name: 'core', testDir: './tests/core', dependencies: ['smoke'], retries: 0 },
  ],
});
```

- [ ] **Steg 8: Kontrollera att konfigurationen laddas**

Kör i `regression/`: `mkdir -p tests/unit tests/smoke tests/core && bunx playwright test --list --pass-with-no-tests`
Förväntat: `Total: 0 tests in 0 files`, utan fel.

- [ ] **Steg 9: Commit**

```bash
git add regression/package.json regression/bun.lock regression/tsconfig.json regression/playwright.config.ts regression/.gitignore regression/.env.example regression/src/config.ts
git commit -m "test: scaffold Playwright regression suite"
```

---

### Uppgift 2: Konfiguration och körnings-ID

**Filer:**
- Ändra: `regression/src/config.ts`
- Skapa: `regression/src/run-id.ts`
- Test: `regression/tests/unit/config.spec.ts`, `regression/tests/unit/run-id.spec.ts`

- [ ] **Steg 1: Skriv testerna som ska misslyckas**

`regression/tests/unit/config.spec.ts`:
```ts
import { expect, test } from '@playwright/test';
import { loadConfig } from '../../src/config';

test('förval används när inget är satt', () => {
  expect(loadConfig({})).toEqual({
    url: 'http://localhost:8080',
    user: 'admin',
    password: 'eterna',
    expectedVersion: undefined,
    jobTimeoutMs: 600_000,
    jobPollMs: 3_000,
    keepData: false,
  });
});

test('avslutande snedstreck tas bort från ETERNA_URL', () => {
  expect(loadConfig({ ETERNA_URL: 'https://stage.example/' }).url).toBe('https://stage.example');
});

test('ogiltig ETERNA_URL ger tydligt fel', () => {
  expect(() => loadConfig({ ETERNA_URL: 'stage.example' })).toThrow(/ETERNA_URL/);
});

test('ogiltig JOB_TIMEOUT_MS ger tydligt fel', () => {
  expect(() => loadConfig({ JOB_TIMEOUT_MS: 'tio' })).toThrow(/JOB_TIMEOUT_MS/);
  expect(() => loadConfig({ JOB_TIMEOUT_MS: '0' })).toThrow(/JOB_TIMEOUT_MS/);
});

test('användare, lösenord, KEEP_DATA och EXPECTED_VERSION läses', () => {
  const cfg = loadConfig({ ETERNA_USER: 'u', ETERNA_PASSWORD: 'p', KEEP_DATA: '1', EXPECTED_VERSION: '1.0.1' });
  expect(cfg.user).toBe('u');
  expect(cfg.password).toBe('p');
  expect(cfg.keepData).toBe(true);
  expect(cfg.expectedVersion).toBe('1.0.1');
});
```

`regression/tests/unit/run-id.spec.ts`:
```ts
import { expect, test } from '@playwright/test';
import { createRunId } from '../../src/run-id';

test('körnings-ID byggs av UTC-tiden', () => {
  expect(createRunId(new Date('2026-10-10T12:34:56.789Z'))).toBe('regtest20261010t123456');
});

test('körnings-ID är en enda alfanumerisk token', () => {
  expect(createRunId()).toMatch(/^regtest\d{8}t\d{6}$/);
});
```

- [ ] **Steg 2: Kör testerna och kontrollera att de misslyckas**

Kör: `bun run test:unit`
Förväntat: FAIL. Det saknas modulen `../../src/run-id`, och `config.spec.ts` misslyckas på URL- och valideringsfallen.

- [ ] **Steg 3: Implementera `regression/src/config.ts`**

```ts
export interface Config {
  url: string;
  user: string;
  password: string;
  expectedVersion?: string;
  jobTimeoutMs: number;
  jobPollMs: number;
  keepData: boolean;
}

type Env = Record<string, string | undefined>;

export function loadConfig(env: Env = process.env): Config {
  const url = (env.ETERNA_URL || 'http://localhost:8080').replace(/\/+$/, '');
  if (!/^https?:\/\/\S+$/.test(url)) {
    throw new Error(`ETERNA_URL måste börja med http:// eller https://, fick "${url}"`);
  }
  return {
    url,
    user: env.ETERNA_USER || 'admin',
    password: env.ETERNA_PASSWORD || 'eterna',
    expectedVersion: env.EXPECTED_VERSION || undefined,
    jobTimeoutMs: positiveInt(env, 'JOB_TIMEOUT_MS', 600_000),
    jobPollMs: positiveInt(env, 'JOB_POLL_MS', 3_000),
    keepData: env.KEEP_DATA === '1',
  };
}

function positiveInt(env: Env, name: string, fallback: number): number {
  const raw = env[name];
  if (raw === undefined || raw === '') return fallback;
  const value = Number(raw);
  if (!Number.isInteger(value) || value <= 0) {
    throw new Error(`${name} måste vara ett positivt heltal, fick "${raw}"`);
  }
  return value;
}
```

- [ ] **Steg 4: Implementera `regression/src/run-id.ts`**

```ts
/**
 * Körnings-ID som märker all testdata. Det är en enda alfanumerisk token så att
 * Solr inte delar upp det vid fritextsökning, t.ex. "regtest20261010t123456".
 */
export function createRunId(now: Date = new Date()): string {
  const stamp = now
    .toISOString()
    .replace(/\.\d{3}Z$/, '')
    .replace(/[-:]/g, '')
    .toLowerCase();
  return `regtest${stamp}`;
}
```

- [ ] **Steg 5: Kör testerna och kontrollera att de går igenom**

Kör: `bun run test:unit`
Förväntat: 7 passed.

- [ ] **Steg 6: Commit**

```bash
git add regression/src/config.ts regression/src/run-id.ts regression/tests/unit/config.spec.ts regression/tests/unit/run-id.spec.ts
git commit -m "test: add config loading and run id for regression suite"
```

---

### Uppgift 3: Typer, filter och API-klient

**Filer:**
- Skapa: `regression/src/types.ts`, `regression/src/filters.ts`, `regression/src/client.ts`
- Test: `regression/tests/unit/filters.spec.ts`, `regression/tests/unit/client.spec.ts`

- [ ] **Steg 1: Skriv testerna som ska misslyckas**

`regression/tests/unit/filters.spec.ts`:
```ts
import { expect, test } from '@playwright/test';
import { equals, freeText, matchAll } from '../../src/filters';

test('filterparametrar har den form som ETERNA deserialiserar', () => {
  expect(matchAll()).toEqual({ type: 'AllFilterParameter' });
  expect(equals('aipId', 'x')).toEqual({ type: 'SimpleFilterParameter', name: 'aipId', value: 'x' });
  expect(freeText('foo')).toEqual({ type: 'BasicSearchFilterParameter', name: 'search', value: 'foo' });
});
```

`regression/tests/unit/client.spec.ts`:
```ts
import { expect, test } from '@playwright/test';
import { basicAuth, describeFailure } from '../../src/client';

test('basicAuth kodar användare och lösenord', () => {
  expect(basicAuth('admin', 'eterna')).toBe('Basic YWRtaW46ZXRlcm5h');
});

test('describeFailure tar med metod, URL, status och kropp', () => {
  expect(describeFailure('GET', 'http://x/api/v2/aips/1', 404, '{"message":"nope"}')).toBe(
    'GET http://x/api/v2/aips/1 → 404\n{"message":"nope"}',
  );
});

test('describeFailure kortar långa svar', () => {
  const text = describeFailure('POST', 'http://x', 500, 'a'.repeat(5000));
  expect(text.length).toBeLessThan(2100);
  expect(text).toContain('(5000 tecken)');
});
```

- [ ] **Steg 2: Kör testerna och kontrollera att de misslyckas**

Kör: `bun run test:unit`
Förväntat: FAIL, modulerna `filters` och `client` saknas.

- [ ] **Steg 3: Skapa `regression/src/types.ts`**

```ts
// Minimala typer för de delar av V2 API:ts svar som sviten läser.

export interface IndexResult<T> {
  offset: number;
  limit: number;
  totalCount: number;
  results: T[];
}

export interface TransferredResource {
  uuid: string;
  id: string;
  name: string;
}

export interface JobStats {
  sourceObjectsCount: number;
  sourceObjectsProcessedWithSuccess: number;
  sourceObjectsProcessedWithPartialSuccess: number;
  sourceObjectsProcessedWithFailure: number;
}

export interface Job {
  id: string;
  name: string;
  state: string;
  stateDetails?: string;
  jobStats: JobStats;
}

export interface Report {
  id: string;
  jobId: string;
  sourceObjectId: string;
  outcomeObjectId: string;
  outcomeObjectClass: string;
  plugin: string;
  pluginName: string;
  pluginState: string;
  pluginDetails: string;
  reports?: Report[];
}

export interface IndexedAIP {
  id: string;
  title: string;
  state: string;
  ingestJobId: string;
}

export interface IndexedRepresentation {
  uuid: string;
  id: string;
  aipId: string;
}

export interface IndexedFile {
  uuid: string;
  id: string;
  aipId: string;
  originalName: string;
  size: number;
  fileFormat?: { pronom?: string; mimeType?: string };
}

export interface PreservationEvent {
  id: string;
  aipID: string;
  eventType: string;
  eventOutcome: string;
}

export interface User {
  id: string;
  name: string;
  type: string;
}
```

- [ ] **Steg 4: Skapa `regression/src/filters.ts`**

```ts
export type FilterParameter = { type: string } & Record<string, unknown>;

/** En tom parameterlista ger tomt resultat i ETERNA, så "allt" måste anges explicit. */
export const matchAll = (): FilterParameter => ({ type: 'AllFilterParameter' });

export const equals = (name: string, value: string): FilterParameter => ({
  type: 'SimpleFilterParameter',
  name,
  value,
});

export const freeText = (value: string): FilterParameter => ({
  type: 'BasicSearchFilterParameter',
  name: 'search',
  value,
});
```

- [ ] **Steg 5: Skapa `regression/src/client.ts`**

```ts
import { request, type APIRequestContext, type APIResponse } from '@playwright/test';
import type { FilterParameter } from './filters';
import type { IndexResult, TransferredResource } from './types';

export type Entity =
  | 'aips'
  | 'representations'
  | 'files'
  | 'preservation/events'
  | 'jobs'
  | 'job-reports'
  | 'transfers'
  | 'members';

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

export function basicAuth(user: string, password: string): string {
  return `Basic ${Buffer.from(`${user}:${password}`, 'utf8').toString('base64')}`;
}

const MAX_BODY_CHARS = 2000;

/** Beskriver ett misslyckat anrop. Headers tas inte med, så lösenord läcker inte. */
export function describeFailure(method: string, url: string, status: number, body: string): string {
  const shown = body.length > MAX_BODY_CHARS ? `${body.slice(0, MAX_BODY_CHARS)}… (${body.length} tecken)` : body;
  return `${method} ${url} → ${status}\n${shown}`;
}

export class EternaClient {
  private constructor(readonly http: APIRequestContext) {}

  static async create(baseURL: string, user: string, password: string): Promise<EternaClient> {
    const http = await request.newContext({
      baseURL,
      extraHTTPHeaders: { Authorization: basicAuth(user, password), Accept: 'application/json' },
    });
    return new EternaClient(http);
  }

  dispose(): Promise<void> {
    return this.http.dispose();
  }

  async getJson<T>(path: string): Promise<T> {
    return this.json<T>('GET', await this.http.get(path));
  }

  async postJson<T>(path: string, data: unknown): Promise<T> {
    return this.json<T>('POST', await this.http.post(path, { data }));
  }

  async getBytes(path: string): Promise<Buffer> {
    const response = await this.http.get(path, { headers: { Accept: '*/*' } });
    await this.ensureOk('GET', response);
    return response.body();
  }

  async exists(path: string): Promise<boolean> {
    const response = await this.http.get(path);
    if (response.status() === 404) return false;
    await this.ensureOk('GET', response);
    return true;
  }

  async deleteIfExists(path: string): Promise<void> {
    const response = await this.http.delete(path);
    if (response.status() === 404) return;
    await this.ensureOk('DELETE', response);
  }

  async find<T>(entity: Entity, parameters: FilterParameter[], max = 100): Promise<IndexResult<T>> {
    return this.postJson<IndexResult<T>>(`/api/v2/${entity}/find`, {
      filter: { parameters },
      onlyActive: false,
      sublist: { firstElementIndex: 0, maximumElementCount: max },
    });
  }

  async upload(fileName: string, zip: Buffer): Promise<TransferredResource> {
    const response = await this.http.post('/api/v2/transfers/create/resource', {
      multipart: {
        resource: { name: fileName, mimeType: 'application/zip', buffer: zip },
        commit: 'true',
      },
    });
    return this.json<TransferredResource>('POST', response);
  }

  private async ensureOk(method: string, response: APIResponse): Promise<void> {
    if (response.ok()) return;
    const body = await response.text().catch(() => '<kunde inte läsa svaret>');
    throw new ApiError(describeFailure(method, response.url(), response.status(), body), response.status());
  }

  private async json<T>(method: string, response: APIResponse): Promise<T> {
    await this.ensureOk(method, response);
    const text = await response.text();
    return (text ? JSON.parse(text) : undefined) as T;
  }
}
```

- [ ] **Steg 6: Kör testerna och typkontrollen**

Kör: `bun run test:unit && bun run typecheck`
Förväntat: 11 passed, och `tsc` avslutar utan fel.

- [ ] **Steg 7: Commit**

```bash
git add regression/src/types.ts regression/src/filters.ts regression/src/client.ts regression/tests/unit/filters.spec.ts regression/tests/unit/client.spec.ts
git commit -m "test: add V2 API client and filter builders"
```

---

### Uppgift 4: SIP-byggare och EICAR

**Filer:**
- Skapa: `regression/fixtures/schemas/` (4 xsd), `regression/src/sip.ts`, `regression/src/eicar.ts`
- Test: `regression/tests/unit/sip.spec.ts`, `regression/tests/unit/eicar.spec.ts`

SIP-formatet är en port av `/home/jerry/Git/eterna-stack/test-data/create_sip.py`, vars paket redan har ingestats i ETERNA.

- [ ] **Steg 1: Kopiera schemana**

```bash
mkdir -p regression/fixtures/schemas
cp ../commons-ip/src/main/resources/schemas2/{mets1_12.xsd,xlink.xsd,DILCISExtensionMETS.xsd,DILCISExtensionSIPMETS.xsd} regression/fixtures/schemas/
ls regression/fixtures/schemas
```
Förväntat: de fyra filerna listas.

- [ ] **Steg 2: Skriv testerna som ska misslyckas**

`regression/tests/unit/sip.spec.ts`:
```ts
import { expect, test } from '@playwright/test';
import { strFromU8, strToU8, unzipSync } from 'fflate';
import { buildBrokenSip, buildSip, buildValidSip, sha256 } from '../../src/sip';

const RUN = 'regtest20261010t123456';

test('giltigt SIP har CSIP-strukturen under <sipId>/', () => {
  const sip = buildValidSip(RUN);
  const root = `${RUN}-valid`;
  expect(sip.fileName).toBe(`${root}.zip`);
  expect(Object.keys(unzipSync(sip.zip)).sort()).toEqual(
    [
      `${root}/METS.xml`,
      `${root}/metadata/descriptive/dc.xml`,
      `${root}/representations/representation_1/METS.xml`,
      `${root}/representations/representation_1/data/document.txt`,
      `${root}/representations/representation_1/data/record.xml`,
      `${root}/schemas/DILCISExtensionMETS.xsd`,
      `${root}/schemas/DILCISExtensionSIPMETS.xsd`,
      `${root}/schemas/mets1_12.xsd`,
      `${root}/schemas/xlink.xsd`,
    ].sort(),
  );
});

test('titeln innehåller körnings-ID:t och datafilernas checksummor finns i representationens METS', () => {
  const sip = buildValidSip(RUN);
  expect(sip.title).toContain(RUN);
  const entries = unzipSync(sip.zip);
  const repMets = strFromU8(entries[`${RUN}-valid/representations/representation_1/METS.xml`]);
  for (const file of sip.files) {
    expect(file.sha256).toBe(sha256(entries[`${RUN}-valid/representations/representation_1/data/${file.name}`]));
    expect(repMets).toContain(`CHECKSUM="${file.sha256}"`);
  }
});

test('titel XML-escapas i dc.xml och METS', () => {
  const sip = buildSip('x', 'A & B <C>', [{ name: 'a.txt', mimeType: 'text/plain', content: strToU8('a') }]);
  const entries = unzipSync(sip.zip);
  expect(strFromU8(entries['x/metadata/descriptive/dc.xml'])).toContain('<title>A &amp; B &lt;C&gt;</title>');
  expect(strFromU8(entries['x/METS.xml'])).toContain('LABEL="A &amp; B &lt;C&gt;"');
});

test('ogiltigt sipId avvisas', () => {
  expect(() => buildSip('a/b', 't', [])).toThrow(/sipId/);
});

test('trasigt SIP saknar METS.xml', () => {
  const sip = buildBrokenSip(`${RUN}-broken`);
  expect(sip.fileName).toBe(`${RUN}-broken.zip`);
  expect(Object.keys(unzipSync(sip.zip))).toEqual([`${RUN}-broken/README.txt`]);
});
```

`regression/tests/unit/eicar.spec.ts`:
```ts
import { expect, test } from '@playwright/test';
import { unzipSync } from 'fflate';
import { buildEicarSip, EICAR_SHA256, eicarBytes } from '../../src/eicar';
import { sha256 } from '../../src/sip';

test('EICAR-filen avkodas till den kända 68-bytesfilen', () => {
  const bytes = eicarBytes();
  expect(bytes.length).toBe(68);
  expect(sha256(bytes)).toBe(EICAR_SHA256);
});

test('EICAR-SIP innehåller testfilen som data', () => {
  const sip = buildEicarSip('regtest20261010t123456');
  const entry = unzipSync(sip.zip)['regtest20261010t123456-eicar/representations/representation_1/data/eicar.txt'];
  expect(sha256(entry)).toBe(EICAR_SHA256);
});
```

- [ ] **Steg 3: Kör testerna och kontrollera att de misslyckas**

Kör: `bun run test:unit`
Förväntat: FAIL, modulerna `sip` och `eicar` saknas.

- [ ] **Steg 4: Implementera `regression/src/sip.ts`**

```ts
import { createHash, randomUUID } from 'node:crypto';
import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { strToU8, zipSync } from 'fflate';

// Bygger E-ARK CSIP-SIP:ar i minnet. Formatet är portat från
// eterna-stack/test-data/create_sip.py, vars paket ingestas av EARKSIP2ToAIPPlugin.

export interface SipInputFile {
  name: string;
  mimeType: string;
  content: Uint8Array;
}

export interface SipFileInfo {
  name: string;
  size: number;
  sha256: string;
}

export interface SipPackage {
  sipId: string;
  fileName: string;
  zip: Buffer;
}

export interface BuiltSip extends SipPackage {
  title: string;
  files: SipFileInfo[];
}

/** Förväntade PRONOM-ID:n från Siegfried för filerna i buildValidSip. */
export const VALID_SIP_PRONOM: Record<string, string> = {
  'document.txt': 'x-fmt/111',
  'record.xml': 'fmt/101',
};

const SCHEMA_DIR = join(__dirname, '..', 'fixtures', 'schemas');
const SCHEMA_FILES = ['mets1_12.xsd', 'xlink.xsd', 'DILCISExtensionMETS.xsd', 'DILCISExtensionSIPMETS.xsd'];
const REP = 'representation_1';
const PROFILE = 'https://earkcsip.dilcis.eu/profile/E-ARK-CSIP.xml';
const METS_NS =
  'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://www.loc.gov/METS/" ' +
  'xmlns:csip="https://DILCIS.eu/XML/METS/CSIPExtensionMETS" xmlns:xlink="http://www.w3.org/1999/xlink" ' +
  'xmlns:sip="https://DILCIS.eu/XML/METS/SIPExtensionMETS"';

export const sha256 = (data: Uint8Array): string => createHash('sha256').update(data).digest('hex').toUpperCase();

export function escapeXml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;');
}

const newId = (): string => `uuid-${randomUUID().toUpperCase()}`;
const href = (rel: string): string => encodeURIComponent(rel);

function schemaLocation(prefix: string): string {
  return [
    `http://www.loc.gov/METS/ ${prefix}schemas/mets1_12.xsd`,
    `http://www.w3.org/1999/xlink ${prefix}schemas/xlink.xsd`,
    `https://DILCIS.eu/XML/METS/CSIPExtensionMETS ${prefix}schemas/DILCISExtensionMETS.xsd`,
    `https://DILCIS.eu/XML/METS/SIPExtensionMETS ${prefix}schemas/DILCISExtensionSIPMETS.xsd`,
  ].join(' ');
}

function dublinCore(title: string, identifier: string): string {
  return `<?xml version="1.0" encoding="UTF-8"?>
<simpledc xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <title>${escapeXml(title)}</title>
  <identifier>${escapeXml(identifier)}</identifier>
  <type>Text</type>
</simpledc>
`;
}

function representationMets(files: SipInputFile[], created: string): string {
  const dataGroup = newId();
  const entries = files.map((file) => {
    const id = newId();
    const xml =
      `            <file ID="${id}" MIMETYPE="${file.mimeType}" SIZE="${file.content.length}" CREATED="${created}" ` +
      `CHECKSUM="${sha256(file.content)}" CHECKSUMTYPE="SHA-256">\n` +
      `                <FLocat xlink:type="simple" xlink:href="${href(`data/${file.name}`)}" LOCTYPE="URL"/>\n` +
      `            </file>`;
    return { id, xml };
  });
  return `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<mets ${METS_NS} OBJID="${REP}" LABEL="" TYPE="Mixed" csip:CONTENTINFORMATIONTYPE="MIXED" PROFILE="${PROFILE}" xsi:schemaLocation="${schemaLocation('../../')}">
    <metsHdr CREATEDATE="${created}" LASTMODDATE="${created}" RECORDSTATUS="NEW" csip:OAISPACKAGETYPE="SIP"/>
    <amdSec ID="${newId()}"/>
    <fileSec ID="${newId()}">
        <fileGrp ID="${dataGroup}" USE="Data">
${entries.map((entry) => entry.xml).join('\n')}
        </fileGrp>
        <fileGrp ID="${newId()}" USE="Schemas"/>
        <fileGrp ID="${newId()}" USE="Documentation"/>
    </fileSec>
    <structMap ID="${newId()}" TYPE="PHYSICAL" LABEL="CSIP">
        <div ID="${newId()}" TYPE="ORIGINAL" LABEL="${REP}">
            <div ID="${newId()}" LABEL="Metadata"/>
            <div ID="${newId()}" LABEL="Metadata/Other"/>
            <div ID="${newId()}" LABEL="Data">
                <fptr FILEID="${dataGroup}"/>
${entries.map((entry) => `                <fptr FILEID="${entry.id}"/>`).join('\n')}
            </div>
            <div ID="${newId()}" LABEL="Schemas"/>
            <div ID="${newId()}" LABEL="Documentation"/>
        </div>
    </structMap>
</mets>
`;
}

function rootMets(sipId: string, title: string, dc: Uint8Array, repMets: Uint8Array, created: string): string {
  const dmd = newId();
  const repGroup = newId();
  const dcRel = 'metadata/descriptive/dc.xml';
  const repRel = `representations/${REP}/METS.xml`;
  return `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<mets ${METS_NS} OBJID="${escapeXml(sipId)}" LABEL="${escapeXml(title)}" TYPE="Mixed" PROFILE="${PROFILE}" xsi:schemaLocation="${schemaLocation('')}">
    <metsHdr CREATEDATE="${created}" LASTMODDATE="${created}" RECORDSTATUS="NEW" csip:OAISPACKAGETYPE="SIP">
        <agent ROLE="CREATOR" TYPE="OTHER" OTHERTYPE="SOFTWARE">
            <name>ETERNA regression suite</name>
            <note csip:NOTETYPE="SOFTWARE VERSION">1.0</note>
        </agent>
    </metsHdr>
    <dmdSec ID="${dmd}" CREATED="${created}" STATUS="CURRENT">
        <mdRef ID="${newId()}" LOCTYPE="URL" xlink:type="simple" xlink:href="${href(dcRel)}" MDTYPE="DC" MIMETYPE="application/xml" SIZE="${dc.length}" CREATED="${created}" CHECKSUM="${sha256(dc)}" CHECKSUMTYPE="SHA-256"/>
    </dmdSec>
    <amdSec ID="${newId()}"/>
    <fileSec ID="${newId()}">
        <fileGrp ID="${newId()}" USE="Data"/>
        <fileGrp ID="${repGroup}" USE="Representations/${REP}">
            <file ID="${newId()}" MIMETYPE="application/xml" SIZE="${repMets.length}" CREATED="${created}" CHECKSUM="${sha256(repMets)}" CHECKSUMTYPE="SHA-256">
                <FLocat xlink:type="simple" xlink:href="${href(repRel)}" LOCTYPE="URL"/>
            </file>
        </fileGrp>
    </fileSec>
    <structMap ID="${newId()}" TYPE="PHYSICAL" LABEL="CSIP">
        <div ID="${newId()}" LABEL="${escapeXml(sipId)}">
            <div ID="${newId()}" DMDID="${dmd}" LABEL="Metadata"/>
            <div ID="${newId()}" LABEL="Data"/>
            <div ID="${newId()}" LABEL="Representations/${REP}">
                <mptr LOCTYPE="URL" xlink:type="simple" xlink:href="${href(repRel)}" xlink:title="${repGroup}"/>
            </div>
        </div>
    </structMap>
</mets>
`;
}

function assertSipId(sipId: string): void {
  if (!/^[A-Za-z0-9_-]+$/.test(sipId)) throw new Error(`Ogiltigt sipId "${sipId}": bara A-Z, a-z, 0-9, _ och - är tillåtna`);
}

export function buildSip(sipId: string, title: string, files: SipInputFile[]): BuiltSip {
  assertSipId(sipId);
  const created = new Date().toISOString();
  const dc = strToU8(dublinCore(title, sipId));
  const repMets = strToU8(representationMets(files, created));
  const entries: Record<string, Uint8Array> = {
    [`${sipId}/METS.xml`]: strToU8(rootMets(sipId, title, dc, repMets, created)),
    [`${sipId}/metadata/descriptive/dc.xml`]: dc,
    [`${sipId}/representations/${REP}/METS.xml`]: repMets,
  };
  for (const file of files) entries[`${sipId}/representations/${REP}/data/${file.name}`] = file.content;
  for (const schema of SCHEMA_FILES) entries[`${sipId}/schemas/${schema}`] = readFileSync(join(SCHEMA_DIR, schema));
  return {
    sipId,
    title,
    fileName: `${sipId}.zip`,
    zip: Buffer.from(zipSync(entries)),
    files: files.map((file) => ({ name: file.name, size: file.content.length, sha256: sha256(file.content) })),
  };
}

/** Giltigt SIP med en txt- och en xml-fil. Titeln innehåller körnings-ID:t för sökning. */
export function buildValidSip(runId: string): BuiltSip {
  return buildSip(`${runId}-valid`, `Regressionstest ${runId}`, [
    { name: 'document.txt', mimeType: 'text/plain', content: strToU8(`Regressionstest för ETERNA.\nKörning: ${runId}\n`) },
    {
      name: 'record.xml',
      mimeType: 'application/xml',
      content: strToU8(`<?xml version="1.0" encoding="UTF-8"?>\n<record><run>${runId}</run></record>\n`),
    },
  ]);
}

/** Zip utan METS.xml, som ska avvisas av ingesten. */
export function buildBrokenSip(sipId: string): SipPackage {
  assertSipId(sipId);
  const zip = zipSync({ [`${sipId}/README.txt`]: strToU8('Detta SIP saknar METS.xml med avsikt.\n') });
  return { sipId, fileName: `${sipId}.zip`, zip: Buffer.from(zip) };
}
```

- [ ] **Steg 5: Implementera `regression/src/eicar.ts`**

```ts
import { buildSip, type BuiltSip } from './sip';

// EICAR-testfilen (ofarlig, standard för att testa antivirus). Den lagras
// base64-kodad så att källfilen själv inte flaggas av virusskydd eller skanning.
const EICAR_BASE64 =
  'WDVPIVAlQEFQWzRcUFpYNTQoUF4pN0NDKTd9JEVJQ0FSLVNUQU5EQVJELUFOVElWSVJVUy1URVNULUZJTEUhJEgrSCo=';

export const EICAR_SHA256 = '275A021BBFB6489E54D471899F7DB9D1663FC695EC2FE2A2C4538AABF651FD0F';

export function eicarBytes(): Uint8Array {
  return Buffer.from(EICAR_BASE64, 'base64');
}

export function buildEicarSip(runId: string): BuiltSip {
  return buildSip(`${runId}-eicar`, `Regressionstest EICAR ${runId}`, [
    { name: 'eicar.txt', mimeType: 'text/plain', content: eicarBytes() },
  ]);
}
```

- [ ] **Steg 6: Kör testerna och typkontrollen**

Kör: `bun run test:unit && bun run typecheck`
Förväntat: 18 passed, och `tsc` avslutar utan fel.

- [ ] **Steg 7: Commit**

```bash
git add regression/fixtures/schemas regression/src/sip.ts regression/src/eicar.ts regression/tests/unit/sip.spec.ts regression/tests/unit/eicar.spec.ts
git commit -m "test: generate CSIP, broken and EICAR SIPs in memory"
```

---

### Uppgift 5: Jobb- och ingest-hjälpare

**Filer:**
- Skapa: `regression/src/jobs.ts`, `regression/src/ingest.ts`, `regression/src/cleanup.ts`
- Test: `regression/tests/unit/jobs.spec.ts`

`cleanup.ts` skapas här eftersom `ingestSip` spårar data via den. Den testas mot stacken i uppgift 8.

- [ ] **Steg 1: Skriv testet som ska misslyckas**

`regression/tests/unit/jobs.spec.ts`:
```ts
import { expect, test } from '@playwright/test';
import { aipIdsFrom, ingestJobRequest, NO_OUTCOME_OBJECT_ID } from '../../src/ingest';
import { FINAL_JOB_STATES, findStep } from '../../src/jobs';
import type { Report } from '../../src/types';

const report = (overrides: Partial<Report>): Report => ({
  id: 'r',
  jobId: 'j',
  sourceObjectId: 's',
  outcomeObjectId: NO_OUTCOME_OBJECT_ID,
  outcomeObjectClass: '',
  plugin: '',
  pluginName: '',
  pluginState: 'SUCCESS',
  pluginDetails: '',
  ...overrides,
});

test('slutlägen innehåller COMPLETED men inte STARTED', () => {
  expect(FINAL_JOB_STATES.has('COMPLETED')).toBe(true);
  expect(FINAL_JOB_STATES.has('FAILED_TO_COMPLETE')).toBe(true);
  expect(FINAL_JOB_STATES.has('STARTED')).toBe(false);
});

test('findStep hittar ett nästlat delsteg på plugin-klass', () => {
  const step = report({ plugin: 'org.roda.core.plugins.base.antivirus.AntivirusPlugin', pluginState: 'FAILURE' });
  const tree = [report({ plugin: 'x.ConfigurableIngestPlugin', reports: [report({ plugin: 'x.Other' }), step] })];
  expect(findStep(tree, 'AntivirusPlugin')).toBe(step);
  expect(findStep(tree, 'Saknas')).toBeUndefined();
});

test('aipIdsFrom hoppar över NO_OUTCOME_ID och dubbletter', () => {
  const reports = [report({ outcomeObjectId: 'a' }), report({ outcomeObjectId: 'a' }), report({})];
  expect(aipIdsFrom(reports)).toEqual(['a']);
});

test('ingest-jobbet använder standardinleveransen med alla steg påslagna', () => {
  const body = ingestJobRequest('namn', 'uuid-1');
  expect(body.plugin).toBe('org.roda.core.plugins.base.ingest.v2.ConfigurableIngestPlugin');
  expect(body.sourceObjects).toEqual({ '@type': 'SelectedItemsListRequest', ids: ['uuid-1'] });
  expect(body.sourceObjectsClass).toBe('org.roda.core.data.v2.ip.TransferredResource');
  expect(body.priority).toBe('HIGH');
  expect(body.parallelism).toBe('NORMAL');
  expect(body.pluginParameters['parameter.do_virus_check']).toBe('true');
  expect(body.pluginParameters['parameter.do_file_format_identification']).toBe('true');
  expect(body.pluginParameters['parameter.create.premis.skeleton']).toBe('true');
});
```

- [ ] **Steg 2: Kör testet och kontrollera att det misslyckas**

Kör: `bun run test:unit`
Förväntat: FAIL, modulerna `ingest` och `jobs` saknas.

- [ ] **Steg 3: Implementera `regression/src/jobs.ts`**

```ts
import type { EternaClient } from './client';
import type { Job, Report } from './types';

/** Slutlägen enligt Job.isFinalState i roda-common-data. */
export const FINAL_JOB_STATES = new Set([
  'COMPLETED',
  'FAILED_TO_COMPLETE',
  'STOPPED',
  'FAILED_DURING_CREATION',
  'REJECTED',
  'PENDING_APPROVAL',
  'SCHEDULED',
]);

export interface PollOptions {
  timeoutMs: number;
  pollMs: number;
}

export const sleep = (ms: number): Promise<void> => new Promise((resolve) => setTimeout(resolve, ms));

export async function waitForJob(client: EternaClient, jobId: string, { timeoutMs, pollMs }: PollOptions): Promise<Job> {
  const deadline = Date.now() + timeoutMs;
  for (;;) {
    const job = await client.getJson<Job>(`/api/v2/jobs/${encodeURIComponent(jobId)}`);
    if (FINAL_JOB_STATES.has(job.state)) return job;
    if (Date.now() >= deadline) {
      throw new Error(`Jobbet ${jobId} nådde inget slutläge inom ${timeoutMs} ms (senaste state: ${job.state})`);
    }
    await sleep(pollMs);
  }
}

/** Startar ett jobb (t.ex. radering) och väntar tills det är klart. */
export async function runJob(client: EternaClient, path: string, body: unknown, poll: PollOptions): Promise<Job> {
  const job = await client.postJson<Job>(path, body);
  return waitForJob(client, job.id, poll);
}

export async function getReports(client: EternaClient, jobId: string): Promise<Report[]> {
  const body = await client.getJson<{ reports?: Report[] }>(
    `/api/v2/jobs/${encodeURIComponent(jobId)}/reports?start=0&limit=100&job-just-failed=false`,
  );
  return body.reports ?? [];
}

/** Rapporterna kan bli synliga en stund efter att jobbet nått sitt slutläge. */
export async function waitForReports(
  client: EternaClient,
  jobId: string,
  expected: number,
  { timeoutMs, pollMs }: PollOptions,
): Promise<Report[]> {
  const deadline = Date.now() + timeoutMs;
  for (;;) {
    const reports = await getReports(client, jobId);
    if (reports.length >= expected || Date.now() >= deadline) return reports;
    await sleep(pollMs);
  }
}

/** Söker rekursivt efter det första delsteget vars plugin-klass innehåller `pluginFragment`. */
export function findStep(reports: Report[], pluginFragment: string): Report | undefined {
  for (const report of reports) {
    if (report.plugin?.includes(pluginFragment)) return report;
    const nested = findStep(report.reports ?? [], pluginFragment);
    if (nested) return nested;
  }
  return undefined;
}
```

- [ ] **Steg 4: Implementera `regression/src/cleanup.ts`**

```ts
import type { EternaClient } from './client';
import { runJob, type PollOptions } from './jobs';
import type { Job } from './types';

const selected = (ids: string[]) => ({ '@type': 'SelectedItemsListRequest', ids });

function assertCompleted(what: string, job: Job): void {
  if (job.state !== 'COMPLETED' || job.jobStats.sourceObjectsProcessedWithFailure > 0) {
    throw new Error(
      `${what}: jobbet ${job.id} slutade i ${job.state} med ${job.jobStats.sourceObjectsProcessedWithFailure} fel`,
    );
  }
}

/** Spårar testdata och tar bort den via API:t. Fel samlas och rapporteras i stället för att döljas. */
export class Cleanup {
  private readonly aips = new Set<string>();
  private readonly transfers = new Set<string>();
  private readonly users = new Set<string>();

  trackAip(id: string): void {
    this.aips.add(id);
  }

  trackTransfer(uuid: string): void {
    this.transfers.add(uuid);
  }

  trackUser(name: string): void {
    this.users.add(name);
  }

  async run(client: EternaClient, poll: PollOptions): Promise<void> {
    const errors: string[] = [];
    const attempt = async (what: string, action: () => Promise<void>): Promise<void> => {
      try {
        await action();
      } catch (error) {
        errors.push(`${what}: ${(error as Error).message}`);
      }
    };

    for (const name of this.users) {
      await attempt(`användare ${name}`, () =>
        client.deleteIfExists(`/api/v2/members/users/${encodeURIComponent(name)}`),
      );
    }

    if (this.aips.size > 0) {
      await attempt('AIP:er', async () => {
        const body = { itemsToDelete: selected([...this.aips]), details: 'Städning efter regressionstest' };
        assertCompleted('Radering av AIP:er', await runJob(client, '/api/v2/aips/delete', body, poll));
      });
    }

    await attempt('transfers', async () => {
      // Ingesten kan redan ha flyttat bort transfern, så bara de som finns kvar raderas.
      const remaining: string[] = [];
      for (const uuid of this.transfers) {
        if (await client.exists(`/api/v2/transfers/${encodeURIComponent(uuid)}`)) remaining.push(uuid);
      }
      if (remaining.length > 0) {
        assertCompleted('Radering av transfers', await runJob(client, '/api/v2/transfers/delete', selected(remaining), poll));
      }
    });

    if (errors.length > 0) throw new Error(`Städningen misslyckades delvis:\n${errors.join('\n')}`);
  }
}
```

- [ ] **Steg 5: Implementera `regression/src/ingest.ts`**

```ts
import { ApiError, type EternaClient } from './client';
import type { Cleanup } from './cleanup';
import { sleep, waitForJob, waitForReports, type PollOptions } from './jobs';
import type { SipPackage } from './sip';
import type { IndexedAIP, Job, Report, TransferredResource } from './types';

export const INGEST_PLUGIN = 'org.roda.core.plugins.base.ingest.v2.ConfigurableIngestPlugin';
export const SIP_TO_AIP_PLUGIN = 'org.roda.core.plugins.base.ingest.EARKSIP2ToAIPPlugin';
/** Report.NO_OUTCOME_OBJECT_ID: värdet när ingen AIP skapades. */
export const NO_OUTCOME_OBJECT_ID = 'NO_OUTCOME_ID';

const INDEX_WAIT_MS = 30_000;

export function ingestJobRequest(name: string, transferUuid: string) {
  return {
    name,
    plugin: INGEST_PLUGIN,
    // Stegen styrs av Boolean.parseBoolean på varje parameter, så alla anges explicit.
    pluginParameters: {
      'parameter.sip_to_aip_class': SIP_TO_AIP_PLUGIN,
      'parameter.do_virus_check': 'true',
      'parameter.do_descriptive_metadata_validation': 'true',
      'parameter.create.premis.skeleton': 'true',
      'parameter.do_file_format_identification': 'true',
      'parameter.do_producer_authorization_check': 'true',
      'parameter.do_apply_disposal_rules': 'true',
      'parameter.do_auto_accept': 'true',
    } as Record<string, string>,
    sourceObjects: { '@type': 'SelectedItemsListRequest', ids: [transferUuid] },
    sourceObjectsClass: 'org.roda.core.data.v2.ip.TransferredResource',
    priority: 'HIGH',
    parallelism: 'NORMAL',
  };
}

export function aipIdsFrom(reports: Report[]): string[] {
  const ids = reports
    .map((report) => report.outcomeObjectId)
    .filter((id): id is string => !!id && id !== NO_OUTCOME_OBJECT_ID);
  return [...new Set(ids)];
}

export interface IngestResult {
  transfer: TransferredResource;
  job: Job;
  reports: Report[];
  aipIds: string[];
}

/** Laddar upp SIP:et, kör standardinleveransen och väntar tills jobbet och rapporterna är klara. */
export async function ingestSip(
  client: EternaClient,
  sip: SipPackage,
  poll: PollOptions,
  cleanup: Cleanup,
): Promise<IngestResult> {
  const transfer = await client.upload(sip.fileName, sip.zip);
  cleanup.trackTransfer(transfer.uuid);
  const created = await client.postJson<Job>('/api/v2/jobs', ingestJobRequest(`regression ${sip.sipId}`, transfer.uuid));
  const job = await waitForJob(client, created.id, poll);
  const reports = await waitForReports(client, job.id, 1, { timeoutMs: INDEX_WAIT_MS, pollMs: poll.pollMs });
  const aipIds = aipIdsFrom(reports);
  for (const id of aipIds) cleanup.trackAip(id);
  return { transfer, job, reports, aipIds };
}

/** Väntar tills AIP:n syns i indexet, eftersom indexeringen kan släpa efter jobbet. */
export async function waitForIndexedAip(client: EternaClient, aipId: string, pollMs: number): Promise<IndexedAIP> {
  const deadline = Date.now() + INDEX_WAIT_MS;
  for (;;) {
    try {
      return await client.getJson<IndexedAIP>(`/api/v2/aips/find/${encodeURIComponent(aipId)}`);
    } catch (error) {
      if (!(error instanceof ApiError) || error.status !== 404 || Date.now() >= deadline) throw error;
    }
    await sleep(pollMs);
  }
}
```

- [ ] **Steg 6: Kör testerna och typkontrollen**

Kör: `bun run test:unit && bun run typecheck`
Förväntat: 22 passed, och `tsc` avslutar utan fel.

- [ ] **Steg 7: Commit**

```bash
git add regression/src/jobs.ts regression/src/ingest.ts regression/src/cleanup.ts regression/tests/unit/jobs.spec.ts
git commit -m "test: add job polling, ingest and cleanup helpers"
```

---

### Uppgift 6: Kortlivad stack och API-protokoll

**Filer:**
- Skapa: `regression/env/docker-compose.yaml`, `regression/env/up.sh`, `regression/env/down.sh`, `regression/docs/api-probes.md`

- [ ] **Steg 1: Skapa `regression/env/docker-compose.yaml`**

```yaml
# Kortlivad stack för regressionstester. Speglar deploys/standalone/docker-compose.yaml.
# Håll images och miljövariabler i synk med den filen. Varje image kan överstyras
# med en miljövariabel för att testa en uppgradering, t.ex. SOLR_IMAGE=solr:9.11.0.
name: eterna-regression

services:
  zoo:
    image: ${ZOOKEEPER_IMAGE:-ghcr.io/eterna-earkiv/zookeeper:3.9.5}
    environment:
      - ZOO_4LW_COMMANDS_WHITELIST=mntr,conf,ruok
    volumes:
      - zookeeper_data:/data
      - zookeeper_datalog:/datalog
    healthcheck:
      test: ["CMD", "zkServer.sh", "status"]
      interval: 10s
      timeout: 5s
      retries: 3
      start_period: 30s

  solr:
    image: ${SOLR_IMAGE:-ghcr.io/eterna-earkiv/solr:9.10.1}
    user: "8983"
    environment:
      SOLR_HEAP: 2g
      ZK_HOST: zoo:2181
    command: -c
    depends_on:
      zoo:
        condition: service_healthy
    volumes:
      - solr_data:/var/solr
    healthcheck:
      test: ["CMD", "solr", "status"]
      interval: 10s
      timeout: 5s
      retries: 3
      start_period: 60s

  clamd:
    image: ${CLAMAV_IMAGE:-docker.io/clamav/clamav:stable}
    volumes:
      - clam_data:/var/lib/clamav
      - roda_data:/roda/data/
    healthcheck:
      test: ["CMD", "/usr/local/bin/clamdcheck.sh"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 120s

  siegfried:
    image: ${SIEGFRIED_IMAGE:-ghcr.io/eterna-earkiv/siegfried:v1.11.4}
    environment:
      SIEGFRIED_HOST: 0.0.0.0
      SIEGFRIED_PORT: 5138
    volumes:
      - siegfried_data:/home/siegfried/
      - roda_data:/roda/data/
    healthcheck:
      test: ["CMD", "sf", "-v"]
      interval: 10s
      timeout: 5s
      retries: 3
      start_period: 60s

  openldap:
    image: ${OPENLDAP_IMAGE:-docker.io/bitnamilegacy/openldap:2.6.10}
    user: 1001:root
    environment:
      - LDAP_ROOT=dc=roda,dc=org
      - LDAP_SKIP_DEFAULT_TREE=yes
      - LDAP_ADMIN_PASSWORD=eterna
      - LDAP_EXTRA_SCHEMAS=cosine,inetorgperson,nis,pbkdf2
    volumes:
      - ../../deploys/standalone/ldap/ldif/pbkdf2.ldif:/opt/bitnami/openldap/etc/schema/pbkdf2.ldif:ro
      - openldap_data:/bitnami/openldap
    healthcheck:
      test: ["CMD-SHELL", "ldapwhoami -H ldap://localhost:1389 -D cn=admin,dc=roda,dc=org -w $$LDAP_ADMIN_PASSWORD"]
      interval: 10s
      timeout: 5s
      retries: 3
      start_period: 30s

  unoserver:
    image: ${UNOSERVER_IMAGE:-ghcr.io/eterna-earkiv/eterna-unoserver:v3.7.0}
    healthcheck:
      test: ["CMD-SHELL", "nc -z localhost 2003 || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

  # Finns inte i deploy-compose, men ETERNA:s SMTP_HOST pekar på "mailserver".
  mailserver:
    image: ${MAILPIT_IMAGE:-axllent/mailpit:v1.24}

  eterna:
    image: ${ETERNA_IMAGE:-ghcr.io/eterna-earkiv/eterna:v1.0.0}
    ports:
      - "${ETERNA_PORT:-8080}:8080"
    depends_on:
      solr:
        condition: service_healthy
      clamd:
        condition: service_healthy
      siegfried:
        condition: service_healthy
      openldap:
        condition: service_healthy
      unoserver:
        condition: service_healthy
      mailserver:
        condition: service_started
    volumes:
      - roda_data:/roda/data/
      - ../../deploys/standalone/roda/config/roda-core.properties:/roda/config/roda-core.properties:ro
    environment:
      - RODA_CORE_SOLR_TYPE=CLOUD
      - RODA_CORE_SOLR_CLOUD_URLS=zoo:2181
      - SOLR_NUM_SHARDS=4
      - SOLR_REPLICATION_FACTOR=1
      - SIEGFRIED_SERVER_URL=http://siegfried:5138
      - CLAMD_TCPADDR=clamd
      - CLAMD_TCPSOCKET=3310
      - SMTP_HOST=mailserver
      - SMTP_PORT=1025
      - UNOSERVER_HOST=unoserver
      - UNOSERVER_PORT=2003

volumes:
  zookeeper_data:
  zookeeper_datalog:
  solr_data:
  clam_data:
  siegfried_data:
  roda_data:
  openldap_data:
```

- [ ] **Steg 2: Skapa `regression/env/up.sh`**

```bash
#!/usr/bin/env bash
# Startar den kortlivade regressionsstacken och väntar tills ETERNA svarar UP.
set -euo pipefail
cd "$(dirname "$0")"

timeout_s="${UP_TIMEOUT_S:-600}"
url="http://localhost:${ETERNA_PORT:-8080}/actuator/health"

show_status() {
  docker compose ps -a --format 'table {{.Service}}\t{{.State}}\t{{.Health}}' >&2
}

if ! docker compose up -d; then
  echo "docker compose up misslyckades. Status:" >&2
  show_status
  exit 1
fi

echo "Väntar på $url (max ${timeout_s}s) ..."
deadline=$((SECONDS + timeout_s))
until curl -fsS --max-time 5 "$url" 2>/dev/null | grep -q '"status":"UP"'; do
  if (( SECONDS >= deadline )); then
    echo "ETERNA blev inte redo inom ${timeout_s}s. Status:" >&2
    show_status
    exit 1
  fi
  sleep 5
done
echo "Stacken är redo på http://localhost:${ETERNA_PORT:-8080}"
```

- [ ] **Steg 3: Skapa `regression/env/down.sh`**

```bash
#!/usr/bin/env bash
# River den kortlivade regressionsstacken inklusive volymer.
set -euo pipefail
cd "$(dirname "$0")"
docker compose down -v --remove-orphans
```

- [ ] **Steg 4: Gör skripten körbara och validera compose-filen**

```bash
chmod +x regression/env/up.sh regression/env/down.sh
docker compose -f regression/env/docker-compose.yaml config --quiet && echo OK
```
Förväntat: `OK`.

- [ ] **Steg 5: Starta stacken**

Om dev-stacken använder port 8080, stoppa den först eller kör med `ETERNA_PORT=18080` och sätt `ETERNA_URL=http://localhost:18080` i stegen nedan.

Kör: `regression/env/up.sh`
Förväntat: `Stacken är redo på http://localhost:8080` inom 10 minuter. Första starten är långsam eftersom ClamAV laddar ned virusdefinitioner.

- [ ] **Steg 6: Gör några snabba kontroller mot stacken**

```bash
curl -s http://localhost:8080/actuator/health
curl -s http://localhost:8080/version.json | grep '"git.build.version"'
curl -s -o /dev/null -w '%{http_code}\n' -u admin:eterna http://localhost:8080/api/v2/members/users/authenticated
curl -s -o /dev/null -w '%{http_code}\n' -u admin:fel http://localhost:8080/api/v2/members/users/authenticated
curl -s -o /dev/null -w '%{http_code}\n' -u admin:eterna -H 'Content-Type: application/json' \
  -d '{"filter":{"parameters":[{"type":"AllFilterParameter"}]},"onlyActive":false}' http://localhost:8080/api/v2/jobs/count
```
Förväntat, i ordning: `{"status":"UP"...}`, en rad med versionen, `200`, `401` och sannolikt `403` (saknad rollnyckel för count på jobb). Skriv in de faktiska resultaten i protokollet i nästa steg.

- [ ] **Steg 7: Skapa `regression/docs/api-probes.md`** med resultaten från steg 6, och med de antaganden som bekräftas i senare uppgifter

```markdown
# Verifierade API-fakta

Antaganden från kodkartläggningen (2026-10-10) och hur de bekräftats mot en körande
stack. Uppdatera tabellen när ETERNA eller en komponent uppgraderas och ett test
börjar fallera på grund av ändrat API-beteende.

Stack: images enligt `env/docker-compose.yaml`, ETERNA-version enligt `/version.json`.

| # | Antagande | Status | Resultat |
|---|---|---|---|
| 1 | `/actuator/health` är öppen och svarar `{"status":"UP"}` | (fyll i från steg 6) | |
| 2 | `/version.json` har `git.build.version` | (fyll i från steg 6) | |
| 3 | Fel lösenord ger 401, rätt ger 200 | (fyll i från steg 6) | |
| 4 | `POST /api/v2/jobs/count` ger 403 för admin, så sviten använder `find` | (fyll i från steg 6) | |
| 5 | `GET /api/v2/members/users/{name}` har `type: "User"` | Bekräftas i uppgift 8 | |
| 6 | Uppladdning ger `TransferredResource` med `uuid` | Bekräftas i uppgift 9 | |
| 7 | Ingest av giltigt SIP: `COMPLETED`, 1 lyckat, rapportens `outcomeObjectId` = AIP-id | Bekräftas i uppgift 9 | |
| 8 | `files/find` med `aipId` + `isDirectory=false` ger bara de två datafilerna, med `fileFormat.pronom` `x-fmt/111` och `fmt/101` | Bekräftas i uppgift 9 | |
| 9 | Events med `aipID` innehåller `virus check`, `format identification`, `message digest calculation` med `SUCCESS` | Bekräftas i uppgift 9 | |
| 10 | `GET /api/v2/aips/{id}/download` ger zip (`PK`) | Bekräftas i uppgift 9 | |
| 11 | Användare skapad via API med `password: {"chars": ...}` kan logga in direkt | Bekräftas i uppgift 10 | |
| 12 | Användare utan objektbehörighet: `find` utelämnar AIP:n, `find/{id}` och filnedladdning ger 403 | Bekräftas i uppgift 10 | |
| 13 | SIP utan METS.xml: `COMPLETED`, 1 misslyckat, `outcomeObjectId` = `NO_OUTCOME_ID` | Bekräftas i uppgift 11 | |
| 14 | EICAR: antivirussteget (plugin-klass innehåller `AntivirusPlugin`) har `FAILURE` | Bekräftas i uppgift 11 | |
| 15 | EICAR: AIP skapas ändå och blir `ACTIVE` (virussteget `mandatory=false`) | Bekräftas i uppgift 11 | |
| 16 | Radering av AIP och transfer via `POST .../delete` ger ett jobb som når `COMPLETED` | Bekräftas i uppgift 8 | |

## Komponenttäckning

Fylls i under uppgift 14: vilket test som fallerar när respektive container stoppas.
```

Ersätt "(fyll i från steg 6)" med `Bekräftad` eller `Avviker`, och skriv det faktiska svaret i kolumnen Resultat. Om rad 1–4 avviker: anpassa `src/` innan du går vidare. Till exempel: om `/actuator/health` kräver inloggning, låt `up.sh` och `health.spec.ts` använda `-u`/`admin`-klienten.

- [ ] **Steg 8: Commit** (låt stacken vara igång för nästa uppgift)

```bash
chmod +x regression/env/*.sh
git add regression/env regression/docs/api-probes.md
git commit -m "test: add ephemeral docker compose stack for regression suite"
```

---

### Uppgift 7: Fixtures

**Filer:**
- Skapa: `regression/src/fixtures.ts`

- [ ] **Steg 1: Skapa `regression/src/fixtures.ts`**

```ts
import { test as base, type TestInfo } from '@playwright/test';
import { EternaClient } from './client';
import { Cleanup } from './cleanup';
import { loadConfig, type Config } from './config';
import { ingestSip, waitForIndexedAip, type IngestResult } from './ingest';
import type { PollOptions } from './jobs';
import { createRunId } from './run-id';
import { buildValidSip, type BuiltSip } from './sip';
import type { Report } from './types';

const CONFIG = loadConfig();

export const poll = (cfg: Config): PollOptions => ({ timeoutMs: cfg.jobTimeoutMs, pollMs: cfg.jobPollMs });

export interface ValidIngest extends IngestResult {
  sip: BuiltSip;
  /** Tom sträng om ingesten inte gav någon AIP. Använd requireAip. */
  aipId: string;
}

interface WorkerFixtures {
  cfg: Config;
  runId: string;
  admin: EternaClient;
  cleanup: Cleanup;
  validIngest: ValidIngest;
}

export const test = base.extend<object, WorkerFixtures>({
  cfg: [
    async ({}, use) => {
      await use(CONFIG);
    },
    { scope: 'worker' },
  ],
  runId: [
    async ({}, use) => {
      await use(createRunId());
    },
    { scope: 'worker' },
  ],
  admin: [
    async ({ cfg }, use) => {
      const client = await EternaClient.create(cfg.url, cfg.user, cfg.password);
      await use(client);
      await client.dispose();
    },
    { scope: 'worker' },
  ],
  cleanup: [
    async ({ admin, cfg }, use) => {
      const cleanup = new Cleanup();
      await use(cleanup);
      if (!cfg.keepData) await cleanup.run(admin, poll(cfg));
    },
    { scope: 'worker', timeout: CONFIG.jobTimeoutMs * 2 + 60_000 },
  ],
  // Det giltiga SIP:et ingestas en gång per worker och delas av alla kärnflödestester.
  validIngest: [
    async ({ admin, cfg, runId, cleanup }, use) => {
      const sip = buildValidSip(runId);
      const result = await ingestSip(admin, sip, poll(cfg), cleanup);
      const aipId = result.aipIds.length === 1 ? result.aipIds[0] : '';
      if (aipId) await waitForIndexedAip(admin, aipId, cfg.jobPollMs);
      await use({ ...result, sip, aipId });
    },
    { scope: 'worker', timeout: CONFIG.jobTimeoutMs + 120_000 },
  ],
});

export { expect } from '@playwright/test';

export function requireAip(ingest: ValidIngest): string {
  if (!ingest.aipId) {
    throw new Error(
      `Ingesten gav inte exakt en AIP (jobb ${ingest.job.id}, state ${ingest.job.state}, ` +
        `AIP:er ${JSON.stringify(ingest.aipIds)}). Se jobbrapporterna i ingest.spec.ts.`,
    );
  }
  return ingest.aipId;
}

export async function attachReports(testInfo: TestInfo, reports: Report[]): Promise<void> {
  await testInfo.attach('jobbrapporter.json', {
    body: JSON.stringify(reports, null, 2),
    contentType: 'application/json',
  });
}
```

- [ ] **Steg 2: Typkontroll**

Kör: `bun run typecheck`
Förväntat: inga fel.

- [ ] **Steg 3: Commit**

```bash
git add regression/src/fixtures.ts
git commit -m "test: add worker fixtures for client, ingest and cleanup"
```

---

### Uppgift 8: Rökprov

**Filer:**
- Skapa: `regression/tests/smoke/health.spec.ts`, `regression/tests/smoke/auth.spec.ts`, `regression/tests/smoke/index.spec.ts`
- Ändra: `regression/docs/api-probes.md` (rad 5)

Testerna är skrivna mot det kartlagda beteendet. Kör dem mot stacken. Om ett test fallerar på grund av ett fältnamn eller en statuskod som skiljer sig från kartläggningen (och inte på grund av en riktig bugg), rätta testet eller `src/` och notera avvikelsen i `api-probes.md`.

- [ ] **Steg 1: Skapa `regression/tests/smoke/health.spec.ts`**

```ts
import { expect, test } from '../../src/fixtures';

test('ETERNA svarar UP på /actuator/health', async ({ request }) => {
  const response = await request.get('/actuator/health');
  expect(response.status()).toBe(200);
  expect((await response.json()).status).toBe('UP');
});

test('driftsatt version matchar EXPECTED_VERSION', async ({ request, cfg }) => {
  test.skip(!cfg.expectedVersion, 'EXPECTED_VERSION är inte satt');
  const response = await request.get('/version.json');
  expect(response.status()).toBe(200);
  expect((await response.json())['git.build.version']).toBe(cfg.expectedVersion);
});
```

- [ ] **Steg 2: Skapa `regression/tests/smoke/auth.spec.ts`**

```ts
import { EternaClient } from '../../src/client';
import { expect, test } from '../../src/fixtures';
import type { User } from '../../src/types';

// Basic Auth kontrolleras mot LDAP, så de här testerna täcker även OpenLDAP.

test('rätt lösenord ger den inloggade användaren', async ({ admin, cfg }) => {
  const me = await admin.getJson<User>('/api/v2/members/users/authenticated');
  expect(me.name).toBe(cfg.user);
});

test('fel lösenord ger 401', async ({ cfg }) => {
  const client = await EternaClient.create(cfg.url, cfg.user, `fel-${Date.now()}`);
  try {
    const response = await client.getRaw('/api/v2/members/users/authenticated');
    expect(response.status).toBe(401);
  } finally {
    await client.dispose();
  }
});

test('användaren kan läsas från LDAP', async ({ admin, cfg }) => {
  const user = await admin.getJson<User>(`/api/v2/members/users/${encodeURIComponent(cfg.user)}`);
  expect(user.name).toBe(cfg.user);
  expect(user.type).toBe('User');
});
```

- [ ] **Steg 3: Skapa `regression/tests/smoke/index.spec.ts`**

```ts
import { expect, test } from '../../src/fixtures';
import { matchAll } from '../../src/filters';

// Sviten använder find i stället för count, eftersom count saknar rollnyckel för jobb (api-probes.md, rad 4).
for (const entity of ['aips', 'jobs', 'transfers'] as const) {
  test(`find på ${entity} ger ett giltigt IndexResult (Solr)`, async ({ admin }) => {
    const result = await admin.find(entity, [matchAll()], 1);
    expect(typeof result.totalCount).toBe('number');
    expect(Array.isArray(result.results)).toBe(true);
  });
}
```

- [ ] **Steg 4: Kör rökproven mot stacken**

Kör: `cd regression && bun run test:smoke`
Förväntat: 7 passed och 1 skipped (versionstestet). Kör därefter `EXPECTED_VERSION=<version från /version.json> bun run test:smoke`. Förväntat: 8 passed.

- [ ] **Steg 5: Kontrollera att städningen fungerar** (rad 16 i protokollet)

Rökproven skapar ingen data, så städningen körs först i uppgift 9. Notera i `api-probes.md` att rad 16 bekräftas där. Uppdatera rad 5 med resultatet från `användaren kan läsas från LDAP`.

- [ ] **Steg 6: Commit**

```bash
git add regression/tests/smoke regression/docs/api-probes.md
git commit -m "test: add smoke tests for health, auth and index"
```

---

### Uppgift 9: Kärnflöde – ingest, AIP, preservation, sök, nedladdning

**Filer:**
- Skapa: `regression/tests/core/ingest.spec.ts`, `aip.spec.ts`, `preservation.spec.ts`, `search.spec.ts`, `download.spec.ts`
- Ändra: `regression/docs/api-probes.md` (rad 6–10 och 16)

- [ ] **Steg 1: Skapa `regression/tests/core/ingest.spec.ts`**

```ts
import { attachReports, expect, test } from '../../src/fixtures';

test('giltigt SIP ingestas utan fel och ger exakt en AIP', async ({ validIngest }, testInfo) => {
  await attachReports(testInfo, validIngest.reports);
  expect(validIngest.job.state).toBe('COMPLETED');
  expect(validIngest.job.jobStats.sourceObjectsProcessedWithFailure).toBe(0);
  expect(validIngest.job.jobStats.sourceObjectsProcessedWithSuccess).toBe(1);
  expect(validIngest.aipIds).toHaveLength(1);
});
```

- [ ] **Steg 2: Skapa `regression/tests/core/aip.spec.ts`**

```ts
import { equals } from '../../src/filters';
import { expect, requireAip, test } from '../../src/fixtures';
import { VALID_SIP_PRONOM } from '../../src/sip';
import type { IndexedAIP, IndexedFile, IndexedRepresentation } from '../../src/types';

test('AIP:n har rätt titel och är ACTIVE', async ({ admin, validIngest }) => {
  const aip = await admin.getJson<IndexedAIP>(`/api/v2/aips/find/${encodeURIComponent(requireAip(validIngest))}`);
  expect(aip.title).toBe(validIngest.sip.title);
  expect(aip.state).toBe('ACTIVE');
});

test('AIP:n har en representation', async ({ admin, validIngest }) => {
  const result = await admin.find<IndexedRepresentation>('representations', [equals('aipId', requireAip(validIngest))]);
  expect(result.totalCount).toBe(1);
});

test('datafilerna finns och har förväntat PRONOM-ID (Siegfried)', async ({ admin, validIngest }) => {
  const files = await admin.find<IndexedFile>('files', [
    equals('aipId', requireAip(validIngest)),
    equals('isDirectory', 'false'),
  ]);
  const actual = Object.fromEntries(files.results.map((file) => [file.originalName, file.fileFormat?.pronom]));
  expect(actual).toEqual(VALID_SIP_PRONOM);
});
```

- [ ] **Steg 3: Skapa `regression/tests/core/preservation.spec.ts`**

```ts
import { equals } from '../../src/filters';
import { expect, requireAip, test } from '../../src/fixtures';
import type { PreservationEvent } from '../../src/types';

// "message digest calculation" är checksummorna vid ingest. "fixity check" skapas
// bara av det fristående fixity-pluginet.
const EXPECTED = [
  { eventType: 'virus check', component: 'ClamAV' },
  { eventType: 'format identification', component: 'Siegfried' },
  { eventType: 'message digest calculation', component: 'checksummor' },
];

for (const { eventType, component } of EXPECTED) {
  test(`event "${eventType}" (${component}) har utfallet SUCCESS`, async ({ admin, validIngest }) => {
    const events = await admin.find<PreservationEvent>(
      'preservation/events',
      [equals('aipID', requireAip(validIngest))],
      500,
    );
    const matching = events.results.filter((event) => event.eventType === eventType);
    expect(matching.length, `inga "${eventType}"-events för AIP:n`).toBeGreaterThan(0);
    expect(matching.map((event) => event.eventOutcome)).toEqual(matching.map(() => 'SUCCESS'));
  });
}
```

- [ ] **Steg 4: Skapa `regression/tests/core/search.spec.ts`**

```ts
import { equals, freeText } from '../../src/filters';
import { expect, requireAip, test } from '../../src/fixtures';
import type { IndexedAIP } from '../../src/types';

test('AIP:n hittas via fritext på körnings-ID:t', async ({ admin, validIngest, runId }) => {
  const result = await admin.find<IndexedAIP>('aips', [freeText(runId)]);
  expect(result.results.map((aip) => aip.id)).toContain(requireAip(validIngest));
});

test('AIP:n hittas via filter på ingestJobId', async ({ admin, validIngest }) => {
  const result = await admin.find<IndexedAIP>('aips', [equals('ingestJobId', validIngest.job.id)]);
  expect(result.results.map((aip) => aip.id)).toEqual([requireAip(validIngest)]);
});
```

- [ ] **Steg 5: Skapa `regression/tests/core/download.spec.ts`**

```ts
import { equals } from '../../src/filters';
import { expect, requireAip, test } from '../../src/fixtures';
import { sha256 } from '../../src/sip';
import type { IndexedFile } from '../../src/types';

test('nedladdade filer har samma SHA-256 som i SIP:et', async ({ admin, validIngest }) => {
  const files = await admin.find<IndexedFile>('files', [
    equals('aipId', requireAip(validIngest)),
    equals('isDirectory', 'false'),
  ]);
  const actual: Record<string, string> = {};
  for (const file of files.results) {
    actual[file.originalName] = sha256(await admin.getBytes(`/api/v2/files/${encodeURIComponent(file.uuid)}/download`));
  }
  expect(actual).toEqual(Object.fromEntries(validIngest.sip.files.map((file) => [file.name, file.sha256])));
});

test('AIP:n kan laddas ned som zip', async ({ admin, validIngest }) => {
  const body = await admin.getBytes(`/api/v2/aips/${encodeURIComponent(requireAip(validIngest))}/download`);
  expect(body.subarray(0, 2).toString('latin1')).toBe('PK');
});
```

- [ ] **Steg 6: Kör kärnflödet mot stacken**

Kör: `cd regression && bun run test:core`
Förväntat: rökproven (7 passed, 1 skipped) och sedan 11 passed i core. Städningen körs vid teardown utan fel. Öppna rapporten med `bun run report` och kontrollera att `jobbrapporter.json` är bifogad i ingest-testet.

Om ett test fallerar:
- Läs felet och den bifogade jobbrapporten.
- Om orsaken är ett fältnamn eller värde som skiljer sig från kartläggningen (till exempel att PRONOM-ID:t för xml blir `fmt/101` med en annan version, eller att `files/find` även ger METS-filen), rätta testet eller `VALID_SIP_PRONOM` och notera det i `api-probes.md`.
- Om orsaken är en riktig bugg i ETERNA: låt testet stå kvar och rapportera felet till användaren. Försvaga inte assertionen.

- [ ] **Steg 7: Kontrollera städningen**

Kör: `curl -s -u admin:eterna -H 'Content-Type: application/json' -d '{"filter":{"parameters":[{"type":"BasicSearchFilterParameter","name":"search","value":"regtest*"}]},"onlyActive":false}' http://localhost:8080/api/v2/aips/find | grep -o '"totalCount":[0-9]*'`
Förväntat: `"totalCount":0`. Kör sedan `KEEP_DATA=1 bun run test:core` och samma curl. Förväntat: `"totalCount":1`. Den kvarlämnade datan städas inte av senare körningar, så riv och starta om stacken (`env/down.sh && env/up.sh`) innan du går vidare.

- [ ] **Steg 8: Uppdatera `api-probes.md`** rad 6–10 och 16 med Bekräftad eller Avviker och de faktiska värdena.

- [ ] **Steg 9: Commit**

```bash
git add regression/tests/core regression/docs/api-probes.md regression/src
git commit -m "test: add core ingest, metadata, search and download tests"
```

---

### Uppgift 10: Behörigheter

**Filer:**
- Skapa: `regression/tests/core/permissions.spec.ts`
- Ändra: `regression/docs/api-probes.md` (rad 11–12)

- [ ] **Steg 1: Skapa `regression/tests/core/permissions.spec.ts`**

```ts
import { randomUUID } from 'node:crypto';
import { EternaClient } from '../../src/client';
import { equals } from '../../src/filters';
import { expect, requireAip, test } from '../../src/fixtures';
import type { IndexedAIP, IndexedFile, User } from '../../src/types';

test.describe('användare med läsroller men utan objektbehörighet', () => {
  let outsider: EternaClient;
  let name: string;

  test.beforeAll(async ({ admin, cfg, runId, cleanup }) => {
    name = `${runId}u`;
    const password = `Rt-${randomUUID()}`;
    await admin.postJson('/api/v2/members/users', {
      email: `${name}@example.org`,
      name,
      fullName: `Regressionstest ${runId}`,
      roles: ['aip.read', 'representation.read'],
      groups: [],
      guest: false,
      password: { chars: password },
      values: [],
    });
    cleanup.trackUser(name);
    outsider = await EternaClient.create(cfg.url, name, password);
  });

  test.afterAll(async () => {
    await outsider?.dispose();
  });

  test('kan logga in', async () => {
    const me = await outsider.getJson<User>('/api/v2/members/users/authenticated');
    expect(me.name).toBe(name);
  });

  test('ser inte AIP:n i find', async ({ validIngest }) => {
    const result = await outsider.find<IndexedAIP>('aips', [equals('id', requireAip(validIngest))]);
    expect(result.totalCount).toBe(0);
  });

  test('får 403 när AIP:n hämtas direkt', async ({ validIngest }) => {
    const response = await outsider.getRaw(`/api/v2/aips/find/${encodeURIComponent(requireAip(validIngest))}`);
    expect(response.status).toBe(403);
  });

  test('får 403 vid filnedladdning', async ({ admin, validIngest }) => {
    const files = await admin.find<IndexedFile>(
      'files',
      [equals('aipId', requireAip(validIngest)), equals('isDirectory', 'false')],
      1,
    );
    const response = await outsider.getRaw(`/api/v2/files/${encodeURIComponent(files.results[0].uuid)}/download`, {
      Accept: '*/*',
    });
    expect(response.status).toBe(403);
  });
});
```

- [ ] **Steg 2: Kör testerna mot stacken**

Kör: `cd regression && bunx playwright test --project=core tests/core/permissions.spec.ts`
Förväntat: 4 passed i permissions-filen.

Om `kan logga in` fallerar: kontrollera om användaren blir inaktiv i väntan på e-postbekräftelse. Mailpit fångar mejlet (port 8025 i containern `mailserver`). Lägg i så fall till aktivering via API:t i `beforeAll` och dokumentera hur i `api-probes.md`. Om `ser inte AIP:n` fallerar för att nya AIP:er som förval får READ för alla användare: notera det som ett fynd, rapportera det till användaren och ändra inte testet utan överenskommelse.

- [ ] **Steg 3: Uppdatera `api-probes.md`** rad 11–12.

- [ ] **Steg 4: Commit**

```bash
git add regression/tests/core/permissions.spec.ts regression/docs/api-probes.md
git commit -m "test: add object permission regression tests"
```

---

### Uppgift 11: Negativa fall

**Filer:**
- Skapa: `regression/tests/core/negative.spec.ts`
- Ändra: `regression/docs/api-probes.md` (rad 13–15)

- [ ] **Steg 1: Skapa `regression/tests/core/negative.spec.ts`**

```ts
import { buildEicarSip } from '../../src/eicar';
import { attachReports, expect, poll, test } from '../../src/fixtures';
import { ingestSip, waitForIndexedAip, type IngestResult } from '../../src/ingest';
import { findStep } from '../../src/jobs';
import { buildBrokenSip } from '../../src/sip';

test('SIP utan METS.xml ger ett misslyckat källobjekt och ingen AIP', async ({ admin, cfg, runId, cleanup }, testInfo) => {
  test.setTimeout(cfg.jobTimeoutMs + 120_000);
  const result = await ingestSip(admin, buildBrokenSip(`${runId}-broken`), poll(cfg), cleanup);
  await attachReports(testInfo, result.reports);
  expect(result.job.state).toBe('COMPLETED');
  expect(result.job.jobStats.sourceObjectsProcessedWithFailure).toBe(1);
  expect(result.aipIds).toEqual([]);
});

test.describe('SIP med EICAR-testvirus', () => {
  let result: IngestResult;

  test.beforeAll(async ({ admin, cfg, runId, cleanup }) => {
    test.setTimeout(cfg.jobTimeoutMs + 120_000);
    result = await ingestSip(admin, buildEicarSip(runId), poll(cfg), cleanup);
  });

  test('antivirussteget rapporterar FAILURE', async ({}, testInfo) => {
    await attachReports(testInfo, result.reports);
    const step = findStep(result.reports, 'AntivirusPlugin');
    expect(step, 'antivirussteget saknas i jobbrapporten').toBeDefined();
    expect(step?.pluginState).toBe('FAILURE');
  });

  test('infekterat SIP blir inte en ACTIVE AIP', async ({ admin, cfg }) => {
    // Känt beteende (docs/api-probes.md, rad 15): virussteget i ConfigurableIngestPlugin
    // är mandatory=false, så ett infekterat SIP blir ändå en ACTIVE AIP. test.fail()
    // dokumenterar avvikelsen. Testet larmar ("unexpected pass") när beteendet ändras.
    test.fail();
    for (const id of result.aipIds) {
      const aip = await waitForIndexedAip(admin, id, cfg.jobPollMs);
      expect(aip.state).not.toBe('ACTIVE');
    }
  });
});
```

- [ ] **Steg 2: Kör testerna mot stacken**

Kör: `cd regression && bunx playwright test --project=core tests/core/negative.spec.ts`
Förväntat: 2 passed och 1 "expected to fail" (räknas som godkänt).

Avgör sedan rad 15 utifrån körningen:
- Om AIP:n blev `ACTIVE`: behåll `test.fail()`, sätt rad 15 till Bekräftad och lyft fyndet för användaren (bugg eller avsett beteende?).
- Om ingen AIP skapades eller den inte blev `ACTIVE`: Playwright rapporterar "expected to fail, but passed". Ta då bort raden `test.fail()` och kommentaren ovanför, och sätt rad 15 till Avviker med det faktiska beteendet.
- Om `antivirussteget rapporterar FAILURE` fallerar för att steget inte hittas: läs `jobbrapporter.json` och rätta söksträngen till den faktiska plugin-klassen (fältet `plugin`). Notera det på rad 14.

- [ ] **Steg 3: Uppdatera `api-probes.md`** rad 13–15.

- [ ] **Steg 4: Commit**

```bash
git add regression/tests/core/negative.spec.ts regression/docs/api-probes.md
git commit -m "test: add broken and infected SIP regression tests"
```

---

### Uppgift 12: Körskript och README

**Filer:**
- Skapa: `regression/scripts/regression.sh`, `regression/README.md`

- [ ] **Steg 1: Skapa `regression/scripts/regression.sh`**

```bash
#!/usr/bin/env bash
# Startar den kortlivade stacken, kör sviten och river stacken, även om testerna fallerar.
# Argument skickas vidare till playwright test, t.ex. --project=smoke.
set -uo pipefail
cd "$(dirname "$0")/.."

export ETERNA_URL="http://localhost:${ETERNA_PORT:-8080}"

save_logs() {
  mkdir -p test-results
  docker compose -f env/docker-compose.yaml logs --no-color > test-results/compose.log 2>&1 || true
  echo "Containerloggar sparade i regression/test-results/compose.log" >&2
}

if ! ./env/up.sh; then
  save_logs
  ./env/down.sh
  exit 1
fi

bunx playwright test "$@"
status=$?

if [ "$status" -ne 0 ]; then
  save_logs
fi
./env/down.sh
exit "$status"
```

Kör: `chmod +x regression/scripts/regression.sh`

- [ ] **Steg 2: Skapa `regression/README.md`**

````markdown
# ETERNA regressionstester

Automatiska black-box-tester mot ETERNA:s V2 REST API. De körs inför varje release
och varje uppgradering av en komponent (Solr, ZooKeeper, ClamAV, Siegfried, OpenLDAP,
unoserver eller ETERNA-imagen).

Design: [docs/2026-10-10-regression-tests-design.md](docs/2026-10-10-regression-tests-design.md) ·
Verifierade API-fakta: [docs/api-probes.md](docs/api-probes.md)

## Krav

- Bun ≥ 1.2 och Node ≥ 22 (Playwright körs på Node)
- Docker med Compose v2
- Inga webbläsare behövs, eftersom sviten bara använder Playwrights `request`-API.

```bash
cd regression
bun install
```

## Köra allt mot en kortlivad stack

```bash
bun run regression
```

Kommandot startar stacken i `env/docker-compose.yaml` (projektet `eterna-regression`,
egna volymer), väntar på `/actuator/health`, kör alla tester och river stacken med
volymer, även om något fallerar. Vid fel sparas containerloggarna i
`test-results/compose.log`. Första starten tar några minuter eftersom ClamAV laddar
ned virusdefinitioner.

Port 8080 används som förval. Om dev-stacken redan kör: `ETERNA_PORT=18080 bun run regression`.

Stegvis, till exempel vid felsökning:

```bash
./env/up.sh
bun run test:unit     # enhetstester, ingen stack krävs
bun run test:smoke    # rökprov, läser bara
bun run test:core     # rökprov + kärnflöde
KEEP_DATA=1 bun run test:core   # låt testdatan ligga kvar
./env/down.sh
bun run report        # öppna HTML-rapporten
```

## Testa en komponentuppgradering

Överstyr imagen med en miljövariabel. Ingen kodändring behövs:

```bash
SOLR_IMAGE=ghcr.io/eterna-earkiv/solr:9.11.0 bun run regression
ETERNA_IMAGE=ghcr.io/eterna-earkiv/eterna:v1.0.1 EXPECTED_VERSION=1.0.1 bun run regression
```

Variabler: `ETERNA_IMAGE`, `SOLR_IMAGE`, `ZOOKEEPER_IMAGE`, `CLAMAV_IMAGE`,
`SIEGFRIED_IMAGE`, `OPENLDAP_IMAGE`, `UNOSERVER_IMAGE`, `MAILPIT_IMAGE`. Förvalen är
desamma som i `deploys/standalone/docker-compose.yaml`. Håll de två filerna i synk.

## Köra mot en befintlig miljö

```bash
ETERNA_URL=https://stage.example ETERNA_USER=admin ETERNA_PASSWORD=… bun run test:smoke
```

Rökproven skapar ingen data. `test:core` skapar AIP:er, transfers och en
testanvändare, märkta med ett körnings-ID (`regtest<tidsstämpel>`), och tar bort dem
efteråt. Kör `test:core` mot en delad miljö bara om det är okej att den får tillfälliga
testobjekt. Inställningarna kan också läggas i `.env` (se `.env.example`).

## Vad som täcks

| Komponent | Test |
|---|---|
| Tomcat/Spring | `smoke/health.spec.ts` |
| OpenLDAP | `smoke/auth.spec.ts` (Basic Auth och användaruppslag) |
| Solr + ZooKeeper | `smoke/index.spec.ts`, `core/search.spec.ts` |
| Ingest (E-ARK CSIP) | `core/ingest.spec.ts`, `core/aip.spec.ts` |
| ClamAV | `core/preservation.spec.ts` (`virus check`), `core/negative.spec.ts` (EICAR) |
| Siegfried | `core/aip.spec.ts` (PRONOM), `core/preservation.spec.ts` |
| Lagring och nedladdning | `core/download.spec.ts` (SHA-256 och AIP-zip) |
| Behörigheter | `core/permissions.spec.ts` |

Siegfried och ClamAV kan inte nås direkt via API:t, så de täcks bara av kärnflödet,
inte av rökproven.

**Täcks inte (än):** PostgreSQL, unoserver/konverterings-plugins, gallring, risker,
reindex, audit log, e-postnotifieringar, CAS/SSO, gränssnittet (GWT).

## Kända avvikelser

`core/negative.spec.ts` › "infekterat SIP blir inte en ACTIVE AIP" kan vara markerat
med `test.fail()`. Se `docs/api-probes.md`, rad 15.

## CI

`.github/workflows/regression.yml` körs manuellt (Actions → Regression → Run workflow,
med valfria image-taggar) och automatiskt efter ett lyckat `Release`-flöde. Det
blockerar inte releasen. `workflow_run` triggas bara om workflow-filen finns på
repots förvalda gren.
````

- [ ] **Steg 3: Kör hela flödet från noll**

Kör: `regression/env/down.sh && cd regression && bun run regression`
Förväntat: stacken startar, alla projekt går igenom (unit, smoke, core; EICAR-testet som "expected to fail" om rad 15 är bekräftad), stacken rivs och kommandot avslutas med kod 0. Kontrollera med `docker compose -f env/docker-compose.yaml ps -a` att inga containrar finns kvar.

- [ ] **Steg 4: Commit**

```bash
git add regression/scripts/regression.sh regression/README.md
git commit -m "docs: add regression runner script and README"
```

---

### Uppgift 13: GitHub Actions

**Filer:**
- Skapa: `.github/workflows/regression.yml`

- [ ] **Steg 1: Skapa `.github/workflows/regression.yml`**

```yaml
name: 'Regression'

on:
  workflow_dispatch:
    inputs:
      eterna_image:
        description: 'ETERNA-image (tom = förval i regression/env/docker-compose.yaml)'
        required: false
        default: ''
      expected_version:
        description: 'Förväntad git.build.version (tom = kontrolleras inte)'
        required: false
        default: ''
      solr_image:
        description: 'Solr-image (tom = förval)'
        required: false
        default: ''
      zookeeper_image:
        description: 'ZooKeeper-image (tom = förval)'
        required: false
        default: ''
      clamav_image:
        description: 'ClamAV-image (tom = förval)'
        required: false
        default: ''
      siegfried_image:
        description: 'Siegfried-image (tom = förval)'
        required: false
        default: ''
      openldap_image:
        description: 'OpenLDAP-image (tom = förval)'
        required: false
        default: ''
  workflow_run:
    workflows: ['Release']
    types: [completed]

permissions:
  contents: read
  packages: read

jobs:
  regression:
    if: github.event_name == 'workflow_dispatch' || github.event.workflow_run.conclusion == 'success'
    runs-on: ubuntu-latest
    timeout-minutes: 60
    defaults:
      run:
        working-directory: regression
    env:
      # Efter en release testas den image som release-flödet just publicerade (taggen = v<version>).
      ETERNA_IMAGE: ${{ github.event_name == 'workflow_run' && format('ghcr.io/eterna-earkiv/eterna:{0}', github.event.workflow_run.head_branch) || inputs.eterna_image }}
      EXPECTED_VERSION: ${{ inputs.expected_version }}
      SOLR_IMAGE: ${{ inputs.solr_image }}
      ZOOKEEPER_IMAGE: ${{ inputs.zookeeper_image }}
      CLAMAV_IMAGE: ${{ inputs.clamav_image }}
      SIEGFRIED_IMAGE: ${{ inputs.siegfried_image }}
      OPENLDAP_IMAGE: ${{ inputs.openldap_image }}
    steps:
      - uses: actions/checkout@v5
        with:
          ref: ${{ github.event.workflow_run.head_sha || github.sha }}

      - uses: actions/setup-node@v5
        with:
          node-version: 22

      - uses: oven-sh/setup-bun@v2
        with:
          bun-version: 1.4.2

      - name: Login to GitHub Container Registry
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Install dependencies
        run: bun install --frozen-lockfile

      - name: Unit tests
        run: bun run test:unit

      - name: Regression (ephemeral stack)
        run: ./scripts/regression.sh --project=smoke --project=core

      - name: Upload report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: regression-report
          path: |
            regression/playwright-report
            regression/test-results
          if-no-files-found: ignore
```

Tomma strängar ger förvalen i compose-filen, eftersom `${VAR:-förval}` behandlar en tom variabel som osatt.

- [ ] **Steg 2: Validera YAML lokalt**

Kör: `python3 -c "import yaml,sys; yaml.safe_load(open('.github/workflows/regression.yml')); print('OK')"`
Förväntat: `OK`. Om `actionlint` finns: `actionlint .github/workflows/regression.yml` ska ge inga fel.

- [ ] **Steg 3: Commit**

```bash
git add .github/workflows/regression.yml
git commit -m "ci: add regression workflow for manual and post-release runs"
```

- [ ] **Steg 4: Kör i CI** (kräver push, så fråga användaren först)

Fråga användaren om grenen får pushas. Om ja: `git push -u origin feat/regression-tests`, och sedan `gh workflow run regression.yml --ref feat/regression-tests` och `gh run watch`. Förväntat: grön körning och artefakten `regression-report`. `workflow_dispatch` kräver att workflow-filen finns på förvald gren. Om kommandot ger "workflow not found", notera att CI-körningen görs efter merge och rapportera det.

---

### Uppgift 14: Verifiera komponenttäckningen

**Filer:**
- Ändra: `regression/docs/api-probes.md` (avsnittet Komponenttäckning)

Det här visar att sviten faktiskt fallerar när en komponent inte fungerar (kriteriet i specen).

- [ ] **Steg 1: Starta stacken:** `regression/env/up.sh`

- [ ] **Steg 2: Stoppa ClamAV och kör kärnflödet**

```bash
docker compose -f regression/env/docker-compose.yaml stop clamd
cd regression && bun run test:core; cd ..
docker compose -f regression/env/docker-compose.yaml start clamd
```
Förväntat: minst `preservation.spec.ts` › "virus check" fallerar.

- [ ] **Steg 3: Upprepa med Siegfried**

```bash
docker compose -f regression/env/docker-compose.yaml stop siegfried
cd regression && bun run test:core; cd ..
docker compose -f regression/env/docker-compose.yaml start siegfried
```
Förväntat: minst `aip.spec.ts` › PRONOM eller `preservation.spec.ts` › "format identification" fallerar.

- [ ] **Steg 4: Upprepa med OpenLDAP**

```bash
docker compose -f regression/env/docker-compose.yaml stop openldap
cd regression && bun run test:smoke; cd ..
docker compose -f regression/env/docker-compose.yaml start openldap
```
Förväntat: `auth.spec.ts` fallerar.

- [ ] **Steg 5: Upprepa med Solr**

```bash
docker compose -f regression/env/docker-compose.yaml stop solr
cd regression && bun run test:smoke; cd ..
docker compose -f regression/env/docker-compose.yaml start solr
```
Förväntat: `index.spec.ts` fallerar.

- [ ] **Steg 6: Dokumentera** i `api-probes.md` under "Komponenttäckning" en tabell: komponent | test som fallerade | felmeddelande (en rad). Om en komponent stoppades utan att något test fallerade är det ett hål i täckningen. Rapportera det till användaren i stället för att markera uppgiften som klar.

- [ ] **Steg 7: Riv stacken och committa**

```bash
regression/env/down.sh
git add regression/docs/api-probes.md
git commit -m "docs: record component coverage verification for regression suite"
```

---

## Slutkontroll

- [ ] `cd regression && bun run typecheck && bun run test:unit` går igenom.
- [ ] `bun run regression` från noll går igenom med kod 0.
- [ ] `api-probes.md` har ingen rad kvar med "Bekräftas i uppgift" eller "fyll i".
- [ ] Inga containrar eller volymer från `eterna-regression` finns kvar (`docker volume ls | grep eterna-regression` ger inget).
- [ ] Låt **code-reviewer**-agenten granska `regression/` och `.github/workflows/regression.yml`.

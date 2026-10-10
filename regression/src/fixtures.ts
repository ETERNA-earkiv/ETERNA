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
      await use(EternaClient.create(cfg.url, cfg.user, cfg.password));
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

import type { EternaClient } from './client';
import type { Job, Report } from './types';

/**
 * Slutlägen enligt Job.isFinalState i roda-common-data.
 * PENDING_APPROVAL och SCHEDULED räknas med avsiktligt (speglar Job.isFinalState); testerna kontrollerar COMPLETED.
 */
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
    if (reports.length >= expected) return reports;
    if (Date.now() >= deadline) {
      throw new Error(`Jobbet ${jobId}: ${reports.length} av ${expected} rapporter inom ${timeoutMs} ms`);
    }
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

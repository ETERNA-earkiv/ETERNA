import { expect, test } from '@playwright/test';
import type { EternaClient } from '../../src/client';
import { NO_OUTCOME_OBJECT_ID } from '../../src/ingest';
import { FINAL_JOB_STATES, findStep, waitForJob, waitForReports } from '../../src/jobs';
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

test('waitForJob returnerar vid slutläge efter att ha pollat icke-slutlägen', async () => {
  const states = ['CREATED', 'STARTED', 'COMPLETED'];
  let calls = 0;
  const client = { getJson: async () => ({ id: 'j', state: states[calls++] }) } as unknown as EternaClient;
  const job = await waitForJob(client, 'j', { timeoutMs: 1000, pollMs: 1 });
  expect(job.state).toBe('COMPLETED');
  expect(calls).toBe(3);
});

test('waitForJob kastar med senaste state efter tidsgränsen', async () => {
  const client = { getJson: async () => ({ id: 'j', state: 'STARTED' }) } as unknown as EternaClient;
  await expect(waitForJob(client, 'j', { timeoutMs: 20, pollMs: 5 })).rejects.toThrow(/senaste state: STARTED/);
});

test('waitForReports kastar vid timeout i stället för att returnera delresultat', async () => {
  const client = { getJson: async () => ({ reports: [report({})] }) } as unknown as EternaClient;
  await expect(waitForReports(client, 'j1', 2, { timeoutMs: 20, pollMs: 5 })).rejects.toThrow(
    /Jobbet j1: 1 av 2 rapporter inom 20 ms/,
  );
});

test('waitForReports returnerar när tillräckligt många rapporter finns', async () => {
  const client = { getJson: async () => ({ reports: [report({})] }) } as unknown as EternaClient;
  expect(await waitForReports(client, 'j1', 1, { timeoutMs: 20, pollMs: 5 })).toHaveLength(1);
});

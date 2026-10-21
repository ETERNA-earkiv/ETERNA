import { expect, test } from '@playwright/test';
import { ApiError, type EternaClient } from '../../src/client';
import { aipIdsFrom, ingestJobRequest, NO_OUTCOME_OBJECT_ID, waitForIndexedAip } from '../../src/ingest';
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

test('waitForIndexedAip försöker igen vid 404 och returnerar AIP:n', async () => {
  let calls = 0;
  const client = {
    getJson: async () => {
      if (++calls === 1) throw new ApiError('saknas', 404);
      return { id: 'a1' };
    },
  } as unknown as EternaClient;
  expect(await waitForIndexedAip(client, 'a1', 1)).toEqual({ id: 'a1' });
  expect(calls).toBe(2);
});

test('waitForIndexedAip kastar vidare andra fel än 404', async () => {
  const client = {
    getJson: async () => {
      throw new ApiError('serverfel', 500);
    },
  } as unknown as EternaClient;
  await expect(waitForIndexedAip(client, 'a1', 1)).rejects.toThrow('serverfel');
});

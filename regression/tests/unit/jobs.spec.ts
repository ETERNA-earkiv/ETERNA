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

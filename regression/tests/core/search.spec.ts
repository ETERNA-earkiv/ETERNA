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

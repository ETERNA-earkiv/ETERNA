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

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

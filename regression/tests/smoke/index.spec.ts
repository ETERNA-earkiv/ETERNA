import { expect, test } from '../../src/fixtures';
import { matchAll } from '../../src/filters';

// Sviten använder find i stället för count, eftersom count på jobb ger {"result":-1} (api-probes.md, rad 4).
for (const entity of ['aips', 'jobs', 'transfers'] as const) {
  test(`find på ${entity} ger ett giltigt IndexResult (Solr)`, async ({ admin }) => {
    const result = await admin.find(entity, [matchAll()], 1);
    expect(typeof result.totalCount).toBe('number');
    expect(Array.isArray(result.results)).toBe(true);
  });
}

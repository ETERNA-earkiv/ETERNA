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
    cleanup.trackUser(name);
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
    outsider = EternaClient.create(cfg.url, name, password);
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
    expect(files.results).toHaveLength(1);
    const response = await outsider.getRaw(`/api/v2/files/${encodeURIComponent(files.results[0].uuid)}/download`, {
      Accept: '*/*',
    });
    expect(response.status).toBe(403);
  });
});

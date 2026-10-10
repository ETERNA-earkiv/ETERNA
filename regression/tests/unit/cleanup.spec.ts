import { expect, test } from '@playwright/test';
import type { EternaClient } from '../../src/client';
import { Cleanup } from '../../src/cleanup';

const poll = { timeoutMs: 100, pollMs: 1 };
const completedJob = { id: 'job', state: 'COMPLETED', jobStats: { sourceObjectsProcessedWithFailure: 0 } };

interface Calls {
  deleted: string[];
  posts: Array<{ path: string; data: unknown }>;
}

function stubClient(options: { existing?: string[]; aips?: string[]; failUserDelete?: boolean } = {}) {
  const calls: Calls = { deleted: [], posts: [] };
  const client = {
    deleteIfExists: async (path: string) => {
      if (options.failUserDelete) throw new Error('boom');
      calls.deleted.push(path);
    },
    find: async () => ({ results: (options.aips ?? []).map((id) => ({ id })) }),
    exists: async (path: string) => (options.existing ?? []).some((uuid) => path.endsWith(uuid)),
    postJson: async (path: string, data: unknown) => {
      calls.posts.push({ path, data });
      return completedJob;
    },
    getJson: async () => completedJob,
  } as unknown as EternaClient;
  return { client, calls };
}

test('städningen raderar användare och AIP:er som hör till spårade ingest-jobb', async () => {
  const { client, calls } = stubClient({ aips: ['aip-1', 'aip-2'] });
  const cleanup = new Cleanup();
  cleanup.trackUser('u1');
  cleanup.trackIngestJob('job-9');
  await cleanup.run(client, poll);
  expect(calls.deleted).toEqual(['/api/v2/members/users/u1']);
  const aipDelete = calls.posts.find((post) => post.path === '/api/v2/aips/delete');
  expect(aipDelete?.data).toMatchObject({ itemsToDelete: { ids: ['aip-1', 'aip-2'] } });
});

test('städningen hoppar över transfers som inte finns och raderar de som finns', async () => {
  const { client, calls } = stubClient({ existing: ['t2'] });
  const cleanup = new Cleanup();
  cleanup.trackTransfer('t1');
  cleanup.trackTransfer('t2');
  await cleanup.run(client, poll);
  expect(calls.posts).toHaveLength(1);
  expect(calls.posts[0].path).toBe('/api/v2/transfers/delete');
  expect(calls.posts[0].data).toMatchObject({ ids: ['t2'] });
});

test('städningen kastar ett samlat fel men kör övriga steg', async () => {
  const { client, calls } = stubClient({ failUserDelete: true, existing: ['t1'] });
  const cleanup = new Cleanup();
  cleanup.trackUser('u1');
  cleanup.trackTransfer('t1');
  await expect(cleanup.run(client, poll)).rejects.toThrow(/användare u1: boom/);
  expect(calls.posts.map((post) => post.path)).toEqual(['/api/v2/transfers/delete']);
});

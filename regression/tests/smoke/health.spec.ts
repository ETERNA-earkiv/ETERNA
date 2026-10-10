import { expect, test } from '../../src/fixtures';

test('ETERNA svarar UP på /actuator/health', async ({ request }) => {
  const response = await request.get('/actuator/health');
  expect(response.status()).toBe(200);
  expect((await response.json()).status).toBe('UP');
});

test('driftsatt version matchar EXPECTED_VERSION', async ({ request, cfg }) => {
  test.skip(!cfg.expectedVersion, 'EXPECTED_VERSION är inte satt');
  const response = await request.get('/version.json');
  expect(response.status()).toBe(200);
  expect((await response.json())['git.build.version']).toBe(cfg.expectedVersion);
});

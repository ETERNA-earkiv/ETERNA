import { EternaClient } from '../../src/client';
import { expect, test } from '../../src/fixtures';
import type { User } from '../../src/types';

// Basic Auth kontrolleras mot LDAP, så de här testerna täcker även OpenLDAP.

test('rätt lösenord ger den inloggade användaren', async ({ admin, cfg }) => {
  const me = await admin.getJson<User>('/api/v2/members/users/authenticated');
  expect(me.name).toBe(cfg.user);
});

test('fel lösenord ger 401', async ({ cfg }) => {
  const client = EternaClient.create(cfg.url, cfg.user, `fel-${Date.now()}`);
  const response = await client.getRaw('/api/v2/members/users/authenticated');
  expect(response.status).toBe(401);
});

test('användaren kan läsas från LDAP', async ({ admin, cfg }) => {
  const user = await admin.getJson<User>(`/api/v2/members/users/${encodeURIComponent(cfg.user)}`);
  expect(user.name).toBe(cfg.user);
  expect(user.type).toBe('User');
});

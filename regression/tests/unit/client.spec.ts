import { expect, test } from '@playwright/test';
import { basicAuth, describeFailure } from '../../src/client';

test('basicAuth kodar användare och lösenord', () => {
  expect(basicAuth('admin', 'eterna')).toBe('Basic YWRtaW46ZXRlcm5h');
});

test('describeFailure tar med metod, URL, status och kropp', () => {
  expect(describeFailure('GET', 'http://x/api/v2/aips/1', 404, '{"message":"nope"}')).toBe(
    'GET http://x/api/v2/aips/1 → 404\n{"message":"nope"}',
  );
});

test('describeFailure kortar långa svar', () => {
  const text = describeFailure('POST', 'http://x', 500, 'a'.repeat(5000));
  expect(text.length).toBeLessThan(2100);
  expect(text).toContain('(5000 tecken)');
});

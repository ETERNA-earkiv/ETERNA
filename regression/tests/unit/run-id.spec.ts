import { expect, test } from '@playwright/test';
import { createRunId } from '../../src/run-id';

test('körnings-ID byggs av UTC-tiden följd av fyra slumpade hextecken', () => {
  const id = createRunId(new Date('2026-10-10T12:34:56.789Z'));
  expect(id.slice(0, -4)).toBe('regtest20261010t123456');
  expect(id).toMatch(/[0-9a-f]{4}$/);
});

test('körnings-ID är en enda alfanumerisk token med gemener', () => {
  expect(createRunId()).toMatch(/^regtest\d{8}t\d{6}[0-9a-f]{4}$/);
});

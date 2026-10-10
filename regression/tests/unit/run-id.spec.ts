import { expect, test } from '@playwright/test';
import { createRunId } from '../../src/run-id';

test('körnings-ID byggs av UTC-tiden', () => {
  expect(createRunId(new Date('2026-10-10T12:34:56.789Z'))).toBe('regtest20261010t123456');
});

test('körnings-ID är en enda alfanumerisk token', () => {
  expect(createRunId()).toMatch(/^regtest\d{8}t\d{6}$/);
});

import { expect, test } from '@playwright/test';
import { loadConfig } from '../../src/config';

test('förval används när inget är satt', () => {
  expect(loadConfig({})).toEqual({
    url: 'http://localhost:8080',
    user: 'admin',
    password: 'eterna',
    expectedVersion: undefined,
    jobTimeoutMs: 600_000,
    jobPollMs: 3_000,
    keepData: false,
  });
});

test('avslutande snedstreck tas bort från ETERNA_URL', () => {
  expect(loadConfig({ ETERNA_URL: 'https://stage.example/' }).url).toBe('https://stage.example');
});

test('ogiltig ETERNA_URL ger tydligt fel', () => {
  expect(() => loadConfig({ ETERNA_URL: 'stage.example' })).toThrow(/ETERNA_URL/);
});

test('ogiltig JOB_TIMEOUT_MS ger tydligt fel', () => {
  expect(() => loadConfig({ JOB_TIMEOUT_MS: 'tio' })).toThrow(/JOB_TIMEOUT_MS/);
  expect(() => loadConfig({ JOB_TIMEOUT_MS: '0' })).toThrow(/JOB_TIMEOUT_MS/);
});

test('användare, lösenord, KEEP_DATA och EXPECTED_VERSION läses', () => {
  const cfg = loadConfig({ ETERNA_USER: 'u', ETERNA_PASSWORD: 'p', KEEP_DATA: '1', EXPECTED_VERSION: '1.0.1' });
  expect(cfg.user).toBe('u');
  expect(cfg.password).toBe('p');
  expect(cfg.keepData).toBe(true);
  expect(cfg.expectedVersion).toBe('1.0.1');
});

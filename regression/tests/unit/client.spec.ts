import { expect, test } from '@playwright/test';
import { basicAuth, describeFailure, parseJsonBody, redactSecrets } from '../../src/client';

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

test('redactSecrets döljer värdet i authorization-rader', () => {
  const text = 'Call log:\n  authorization: Basic abc==\n  Authorization: Bearer xyz\n  accept: application/json';
  expect(redactSecrets(text)).toBe(
    'Call log:\n  authorization: <dold>\n  Authorization: <dold>\n  accept: application/json',
  );
  expect(redactSecrets('inget hemligt')).toBe('inget hemligt');
});

test('parseJsonBody tolkar JSON, tom kropp ger undefined och icke-JSON ger tydligt fel', () => {
  expect(parseJsonBody('GET', 'http://x', 200, '{"a":1}')).toEqual({ a: 1 });
  expect(parseJsonBody('GET', 'http://x', 204, '')).toBeUndefined();
  expect(() => parseJsonBody('GET', 'http://x', 200, '<html>')).toThrow(
    'Svaret var inte JSON: GET http://x → 200\n<html>',
  );
});

import { expect, test } from '@playwright/test';
import { equals, freeText, matchAll } from '../../src/filters';

test('filterparametrar har den form som ETERNA deserialiserar', () => {
  expect(matchAll()).toEqual({ type: 'AllFilterParameter' });
  expect(equals('aipId', 'x')).toEqual({ type: 'SimpleFilterParameter', name: 'aipId', value: 'x' });
  expect(freeText('foo')).toEqual({ type: 'BasicSearchFilterParameter', name: 'search', value: 'foo' });
});

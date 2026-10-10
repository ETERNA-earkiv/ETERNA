import { expect, test } from '@playwright/test';
import { unzipSync } from 'fflate';
import { buildEicarSip, EICAR_SHA256, eicarBytes } from '../../src/eicar';
import { sha256 } from '../../src/sip';

test('EICAR-filen avkodas till den kända 68-bytesfilen', () => {
  const bytes = eicarBytes();
  expect(bytes.length).toBe(68);
  expect(sha256(bytes)).toBe(EICAR_SHA256);
});

test('EICAR-SIP innehåller testfilen som data', () => {
  const sip = buildEicarSip('regtest20261010t123456');
  const entry = unzipSync(sip.zip)['regtest20261010t123456-eicar/representations/representation_1/data/eicar.txt'];
  expect(sha256(entry)).toBe(EICAR_SHA256);
});

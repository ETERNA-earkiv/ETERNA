import { buildSip, type BuiltSip } from './sip';

// EICAR-testfilen (ofarlig, standard för att testa antivirus). Den lagras
// base64-kodad så att källfilen själv inte flaggas av virusskydd eller skanning.
const EICAR_BASE64 =
  'WDVPIVAlQEFQWzRcUFpYNTQoUF4pN0NDKTd9JEVJQ0FSLVNUQU5EQVJELUFOVElWSVJVUy1URVNULUZJTEUhJEgrSCo=';

export const EICAR_SHA256 = '275A021BBFB6489E54D471899F7DB9D1663FC695EC2FE2A2C4538AABF651FD0F';

export function eicarBytes(): Uint8Array {
  return Buffer.from(EICAR_BASE64, 'base64');
}

export function buildEicarSip(runId: string): BuiltSip {
  return buildSip(`${runId}-eicar`, `Regressionstest EICAR ${runId}`, [
    { name: 'eicar.txt', mimeType: 'text/plain', content: eicarBytes() },
  ]);
}

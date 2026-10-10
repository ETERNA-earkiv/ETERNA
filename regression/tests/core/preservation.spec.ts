import { equals } from '../../src/filters';
import { expect, requireAip, test } from '../../src/fixtures';
import type { PreservationEvent } from '../../src/types';

// "message digest calculation" är checksummorna vid ingest. "fixity check" skapas
// bara av det fristående fixity-pluginet.
const EXPECTED = [
  { eventType: 'virus check', component: 'ClamAV' },
  { eventType: 'format identification', component: 'Siegfried' },
  { eventType: 'message digest calculation', component: 'checksummor' },
];

for (const { eventType, component } of EXPECTED) {
  test(`event "${eventType}" (${component}) har utfallet SUCCESS`, async ({ admin, validIngest }) => {
    const events = await admin.find<PreservationEvent>(
      'preservation/events',
      [equals('aipID', requireAip(validIngest))],
      500,
    );
    const matching = events.results.filter((event) => event.eventType === eventType);
    expect(matching.length, `inga "${eventType}"-events för AIP:n`).toBeGreaterThan(0);
    expect(matching.map((event) => event.eventOutcome)).toEqual(matching.map(() => 'SUCCESS'));
  });
}

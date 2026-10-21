import { randomBytes } from 'node:crypto';

/**
 * Körnings-ID som märker all testdata. Det är en enda alfanumerisk token så att
 * Solr inte delar upp det vid fritextsökning, t.ex. "regtest20261010t123456a1b2".
 * De fyra sista hextecknen är slumpade så att två körningar samma sekund inte krockar.
 */
export function createRunId(now: Date = new Date()): string {
  const stamp = now
    .toISOString()
    .replace(/\.\d{3}Z$/, '')
    .replace(/[-:]/g, '')
    .toLowerCase();
  return `regtest${stamp}${randomBytes(2).toString('hex')}`;
}

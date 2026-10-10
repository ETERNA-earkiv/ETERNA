/**
 * Körnings-ID som märker all testdata. Det är en enda alfanumerisk token så att
 * Solr inte delar upp det vid fritextsökning, t.ex. "regtest20261010t123456".
 */
export function createRunId(now: Date = new Date()): string {
  const stamp = now
    .toISOString()
    .replace(/\.\d{3}Z$/, '')
    .replace(/[-:]/g, '')
    .toLowerCase();
  return `regtest${stamp}`;
}

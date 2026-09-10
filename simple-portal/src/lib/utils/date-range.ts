/**
 * Datumintervall-logik för klient-side datumfiltrering.
 *
 * Arkivdatum i EAD3 (standarddate) kan vara partiella: "1888", "1888-03" eller
 * "1888-03-15". De normaliseras till YYYY-MM-DD så att strängjämförelse blir
 * kronologisk. Ett startdatum fylls ut nedåt (år → 01-01), ett slutdatum uppåt
 * (år → 12-31, månad → sista dagen i månaden).
 */

const ISO_PARTIAL = /^(\d{4})(?:-(\d{2}))?(?:-(\d{2}))?/;

export function normalizeStartDate(value: string): string | null {
  const m = ISO_PARTIAL.exec(value.trim());
  if (!m) return null;
  const [, year, month = '01', day = '01'] = m;
  return `${year}-${month}-${day}`;
}

export function normalizeEndDate(value: string): string | null {
  const m = ISO_PARTIAL.exec(value.trim());
  if (!m) return null;
  const [, year, month, day] = m;
  if (day) return `${year}-${month}-${day}`;
  const effectiveMonth = month ?? '12';
  // Dag 0 i nästa månad = sista dagen i denna månad
  const lastDay = new Date(Date.UTC(Number(year), Number(effectiveMonth), 0)).getUTCDate();
  return `${year}-${effectiveMonth}-${String(lastDay).padStart(2, '0')}`;
}

/**
 * True om postens datum(intervall) överlappar filtrets intervall.
 * - Enkelt datum (bara start eller bara slut) behandlas som en punkt.
 * - Post utan något datum matchar aldrig ett aktivt datumfilter.
 * - Tom filtergräns = öppen (bara "från" eller bara "till").
 */
export function dateRangesOverlap(
  itemStart: string,
  itemEnd: string,
  filterFrom: string,
  filterTo: string,
): boolean {
  const start = normalizeStartDate(itemStart) ?? normalizeStartDate(itemEnd);
  const end = normalizeEndDate(itemEnd) ?? normalizeEndDate(itemStart);
  if (!start || !end) return false;

  const from = filterFrom ? normalizeStartDate(filterFrom) : null;
  const to = filterTo ? normalizeEndDate(filterTo) : null;

  if (from && end < from) return false;
  if (to && start > to) return false;
  return true;
}

/**
 * Search store — sökresultat, paginering och facetter.
 */

import { atom, computed } from 'nanostores';
import { searchAIPsWithFacets } from '../api/aip';
import { loadConfig } from '../api/config';
import { getAvailableMetadataIds, getMetadataXml } from '../api/metadata';
import { parseXmlToFields } from '../utils/metadata-parser';
import { dateRangesOverlap } from '../utils/date-range';
import type { IndexedAIP, FilterParameter, FacetResult } from '../types/api';
import { $resultsPerPage } from './config';

export const $query = atom('');
export const $filters = atom<FilterParameter[]>([]);
export const $results = atom<IndexedAIP[]>([]);
export const $totalCount = atom(0);
export const $offset = atom(0);
export const $loading = atom(false);
export const $error = atom<string | null>(null);
export const $facets = atom<FacetResult[]>([]);
export const $sortOptionId = atom<string>('title');
export const $sortField = atom<string>('title');
export const $sortDescending = atom<boolean>(false);
export const $clientSortActive = atom<boolean>(false);
/** Fält som inte finns i Solr-indexet (t.ex. origination/Arkivbildare) — filtreras
 * client-side genom att läsa varje AIP:s metadata-XML. Nyckel = fieldName. */
export const $clientFilters = atom<Record<string, string>>({});
const $clientSortedResults = atom<IndexedAIP[]>([]);

// Computed
export const $hasResults = computed($results, (r) => r.length > 0);
export const $isEmpty = computed([$results, $loading], (r, l) => !l && r.length === 0);
export const $currentPage = computed([$offset], (o) => Math.floor(o / ($resultsPerPage.get() || 10)) + 1);
export const $totalPages = computed([$totalCount], (tc) => Math.ceil(tc / ($resultsPerPage.get() || 10)));
export const $hasNextPage = computed([$currentPage, $totalPages], (cp, tp) => cp < tp);
export const $hasPrevPage = computed([$currentPage], (cp) => cp > 1);

// Cachad visibility config (läses en gång från config.json)
let cachedLevels: string[] | undefined;

async function loadAllowedLevels(): Promise<string[]> {
  if (cachedLevels !== undefined) return cachedLevels;
  try {
    const config = await loadConfig();
    const levels: string[] = config?.visibilityConfig?.allowedLevels || [];
    cachedLevels = levels;
    return levels;
  } catch {
    return [];
  }
}

function buildLevelFilters(levels: string[]): FilterParameter[] {
  if (levels.length === 0) return [];

  if (levels.length === 1) {
    return [{
      type: 'SimpleFilterParameter',
      name: 'level',
      value: levels[0],
    } as FilterParameter];
  }

  return [{
    type: 'OrFiltersParameters',
    values: levels.map((level) => ({
      type: 'SimpleFilterParameter',
      name: 'level',
      value: level,
    })),
  } as FilterParameter];
}

let searchDebounce: ReturnType<typeof setTimeout> | null = null;

/** True om något aktivt filter/sortering kräver att hela resultatlistan hämtas
 * och läses klient-side (fält som saknas i Solr-indexet). */
function needsClientSideSearch(): boolean {
  const optionId = $sortOptionId.get();
  if (optionId === 'originator' || optionId === 'createdOn') return true;
  return Object.values($clientFilters.get()).some((v) => v?.trim());
}

/** Hämtar alla matchande AIP:er, läser Arkivbildare, Startdatum och Slutdatum
 * ur metadata-XML för varje, filtrerar/sorterar client-side och paginerar
 * resultatet lokalt. Används när ett client-filter (Arkivbildare eller
 * datumintervall) är aktivt, eller när sortering på Arkivbildare eller Datum
 * (Startdatum) valts — allt detta kräver hela listan i minnet eftersom fälten
 * saknas i Solr-indexet (EAD3-datum med <datesingle> indexeras inte alls). */
async function runClientSideSearch(): Promise<void> {
  $loading.set(true);
  $error.set(null);

  try {
    const levels = await loadAllowedLevels();
    const levelFilters = buildLevelFilters(levels);
    const allFilters = [...$filters.get(), ...levelFilters];

    const result = await searchAIPsWithFacets(
      $query.get(),
      allFilters,
      0,
      10000,
      'title',
      false,
    );

    const allItems = (result?.results || []).map((item: unknown) => {
      const rec = item as Record<string, unknown>;
      return ('aip' in rec ? rec.aip : item) as IndexedAIP;
    });

    const withMetadata = await Promise.all(
      allItems.map(async (aip) => {
        let originator = '';
        let startDate = '';
        let endDate = '';
        try {
          const aipId = aip.uuid || aip.id;
          const ids = await getAvailableMetadataIds(aipId);
          const metaId = ids.find(id => id.toLowerCase().includes('ead_3')) || ids[0];
          if (metaId) {
            const xml = await getMetadataXml(aipId, metaId);
            const fields = parseXmlToFields(xml);
            originator = fields.find(f => f.label === 'Arkivbildare')?.value || '';
            startDate = fields.find(f => f.label === 'Startdatum')?.value || '';
            endDate = fields.find(f => f.label === 'Slutdatum')?.value || '';
          }
        } catch {}
        return { aip, originator, startDate, endDate };
      })
    );

    const cf = $clientFilters.get();
    const originatorFilter = cf.origination?.trim().toLowerCase() ?? '';
    const datesFrom = cf.datesFrom?.trim() ?? '';
    const datesTo = cf.datesTo?.trim() ?? '';
    const hasDateFilter = Boolean(datesFrom || datesTo);

    const filtered = withMetadata.filter((x) => {
      if (originatorFilter && !x.originator.toLowerCase().includes(originatorFilter)) return false;
      if (hasDateFilter && !dateRangesOverlap(x.startDate, x.endDate, datesFrom, datesTo)) return false;
      return true;
    });

    const descending = $sortDescending.get();
    const sortOptionId = $sortOptionId.get();
    const sortField = $sortField.get() as keyof IndexedAIP;
    filtered.sort((a, b) => {
      let cmp: number;
      if (sortOptionId === 'originator') {
        cmp = a.originator.localeCompare(b.originator, 'sv', { sensitivity: 'base' });
      } else if (sortOptionId === 'createdOn') {
        cmp = a.startDate.localeCompare(b.startDate, 'sv', { sensitivity: 'base' });
      } else {
        cmp = String(a.aip[sortField] ?? '').localeCompare(String(b.aip[sortField] ?? ''), 'sv', { sensitivity: 'base' });
      }
      return descending ? -cmp : cmp;
    });

    const sorted = filtered.map((x) => x.aip);
    $clientSortedResults.set(sorted);
    $clientSortActive.set(true);
    $totalCount.set(sorted.length);
    $facets.set(result?.facetResults || []);

    const limit = $resultsPerPage.get() || 10;
    const offset = $offset.get();
    $results.set(sorted.slice(offset, offset + limit));
  } catch (err) {
    $error.set(err instanceof Error ? err.message : 'Sökningen misslyckades');
    $results.set([]);
    $totalCount.set(0);
  } finally {
    $loading.set(false);
  }
}

export async function search(query?: string, filters?: FilterParameter[], clientFilters?: Record<string, string>): Promise<void> {
  // Återställ offset vid ny sökning (ändrad query eller filter)
  if (query !== undefined || filters !== undefined || clientFilters !== undefined) {
    $offset.set(0);
  }
  if (query !== undefined) $query.set(query);
  if (filters !== undefined) $filters.set(filters);
  if (clientFilters !== undefined) $clientFilters.set(clientFilters);

  if (searchDebounce) clearTimeout(searchDebounce);

  return new Promise((resolve) => {
    searchDebounce = setTimeout(async () => {
      if (needsClientSideSearch()) {
        await runClientSideSearch();
        resolve();
        return;
      }

      $clientSortActive.set(false);
      $clientSortedResults.set([]);
      $loading.set(true);
      $error.set(null);

      try {
        // Hämta level-filter från config.json
        const levels = await loadAllowedLevels();
        const levelFilters = buildLevelFilters(levels);
        const allFilters = [...$filters.get(), ...levelFilters];

        const result = await searchAIPsWithFacets(
          $query.get(),
          allFilters,
          $offset.get(),
          $resultsPerPage.get() || 10,
          $sortField.get(),
          $sortDescending.get(),
        );

        const items = (result?.results || []).map((item: unknown) => {
          const rec = item as Record<string, unknown>;
          return ('aip' in rec ? rec.aip : item) as IndexedAIP;
        });

        $results.set(items);
        $totalCount.set(result?.totalCount ?? 0);
        $facets.set(result?.facetResults || []);
      } catch (err) {
        $error.set(err instanceof Error ? err.message : 'Sökningen misslyckades');
        $results.set([]);
        $totalCount.set(0);
      } finally {
        $loading.set(false);
        resolve();
      }
    }, 300);
  });
}

export function goToPage(page: number): void {
  const limit = $resultsPerPage.get() || 10;
  if ($clientSortActive.get()) {
    const all = $clientSortedResults.get();
    const start = (page - 1) * limit;
    $results.set(all.slice(start, start + limit));
    $offset.set((page - 1) * limit);
    return;
  }
  $offset.set((page - 1) * limit);
  search();
}

export function nextPage(): void {
  if ($hasNextPage.get()) goToPage($currentPage.get() + 1);
}

export function prevPage(): void {
  if ($hasPrevPage.get()) goToPage($currentPage.get() - 1);
}

export async function setSort(optionId: string, field: string, descending: boolean): Promise<void> {
  $offset.set(0);
  $sortOptionId.set(optionId);
  $sortField.set(field);
  $sortDescending.set(descending);

  if (needsClientSideSearch()) {
    await runClientSideSearch();
    return;
  }

  $clientSortActive.set(false);
  $clientSortedResults.set([]);
  await search();
}

export async function sortByOriginator(descending: boolean): Promise<void> {
  $offset.set(0);
  $sortOptionId.set('originator');
  $sortDescending.set(descending);
  await runClientSideSearch();
}

export function resetSearch(): void {
  $query.set('');
  $filters.set([]);
  $clientFilters.set({});
  $results.set([]);
  $totalCount.set(0);
  $offset.set(0);
  $error.set(null);
  $facets.set([]);
  $sortOptionId.set('title');
  $sortField.set('title');
  $sortDescending.set(false);
  $clientSortActive.set(false);
  $clientSortedResults.set([]);
}

/** Invalidera cachad visibility (anropas efter admin-sparning) */
export function invalidateVisibilityCache(): void {
  cachedLevels = undefined;
}

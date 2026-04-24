/**
 * Search store — sökresultat, paginering och facetter.
 */

import { atom, computed } from 'nanostores';
import { searchAIPsWithFacets } from '../api/aip';
import { loadConfig } from '../api/config';
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

export async function search(query?: string, filters?: FilterParameter[]): Promise<void> {
  // Återställ offset vid ny sökning (ändrad query eller filter)
  if (query !== undefined || filters !== undefined) {
    $offset.set(0);
  }
  if (query !== undefined) $query.set(query);
  if (filters !== undefined) $filters.set(filters);

  if (searchDebounce) clearTimeout(searchDebounce);

  return new Promise((resolve) => {
    searchDebounce = setTimeout(async () => {
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
  $offset.set((page - 1) * limit);
  search();
}

export function nextPage(): void {
  if ($hasNextPage.get()) goToPage($currentPage.get() + 1);
}

export function prevPage(): void {
  if ($hasPrevPage.get()) goToPage($currentPage.get() - 1);
}

export function resetSearch(): void {
  $query.set('');
  $filters.set([]);
  $results.set([]);
  $totalCount.set(0);
  $offset.set(0);
  $error.set(null);
  $facets.set([]);
}

/** Invalidera cachad visibility (anropas efter admin-sparning) */
export function invalidateVisibilityCache(): void {
  cachedLevels = undefined;
}

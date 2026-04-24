/**
 * AIP (Archival Information Package) API
 */

import { apiPost, apiGet, apiGetHtml, apiPatch } from './client';
import { TtlCache } from '../utils/ttl-cache';
import type {
  FindRequest,
  IndexedAIP,
  AIPIndexResult,
  FilterParameter,
  BasicSearchFilterParameter,
  SimpleFilterParameter,
  EmptyKeyFilterParameter,
  Permissions,
  Job,
} from '../types/api';

const aipCache = new TtlCache<IndexedAIP | undefined>();
const ancestorCache = new TtlCache<IndexedAIP[]>();
const htmlCache = new TtlCache<string>();

export async function searchAIPs(
  query: string,
  filters: FilterParameter[] = [],
  offset = 0,
  limit = 10,
  sortField = 'title',
  sortDescending = false,
): Promise<AIPIndexResult> {
  const allFilters: FilterParameter[] = [];

  if (query?.trim()) {
    allFilters.push({
      type: 'BasicSearchFilterParameter',
      name: 'search',
      value: query,
    } as BasicSearchFilterParameter);
  }

  allFilters.push(...filters);

  const request: FindRequest = {
    filter: {
      parameters: allFilters.length > 0
        ? allFilters
        : [{ type: 'AllFilterParameter' }],
    },
    onlyActive: true,
    sublist: {
      firstElementIndex: offset,
      maximumElementCount: limit,
    },
    sorter: {
      parameters: [{ name: sortField, descending: sortDescending }],
    },
  };

  return apiPost<AIPIndexResult>('/api/v2/aips/find', request);
}

export async function searchAIPsWithFacets(
  query: string,
  filters: FilterParameter[] = [],
  offset = 0,
  limit = 10,
): Promise<AIPIndexResult> {
  const allFilters: FilterParameter[] = [];

  if (query?.trim()) {
    allFilters.push({
      type: 'BasicSearchFilterParameter',
      name: 'search',
      value: query,
    } as BasicSearchFilterParameter);
  }

  allFilters.push(...filters);

  const request: FindRequest = {
    filter: {
      parameters: allFilters.length > 0
        ? allFilters
        : [{ type: 'AllFilterParameter' }],
    },
    onlyActive: true,
    exportFacets: true,
    sublist: {
      firstElementIndex: offset,
      maximumElementCount: limit,
    },
  };

  try {
    return await apiPost<AIPIndexResult>('/api/v2/aips/find', request);
  } catch (err) {
    // Solr "undefined field" = fältet finns i config men inte i indexet ännu
    // Returnera tomt resultat istället för att krascha
    if (err instanceof Error && err.message.includes('undefined field')) {
      return { offset: 0, limit, totalCount: 0, results: [], facetResults: [], date: Date.now() };
    }
    throw err;
  }
}

export function findAIPById(id: string): Promise<IndexedAIP | undefined> {
  return aipCache.getOrFetch(`aip:${id}`, async () => {
    const request: FindRequest = {
      filter: {
        parameters: [{
          type: 'SimpleFilterParameter',
          name: 'uuid',
          value: id,
        } as SimpleFilterParameter],
      },
      onlyActive: true,
      sublist: { firstElementIndex: 0, maximumElementCount: 1 },
    };

    const result = await apiPost<AIPIndexResult>('/api/v2/aips/find', request);
    const items = result?.results || [];
    if (items.length === 0) return undefined;

    const item = items[0];
    return 'aip' in item ? (item as { aip: IndexedAIP }).aip : item as IndexedAIP;
  });
}

export function getAIPAncestors(id: string): Promise<IndexedAIP[]> {
  return ancestorCache.getOrFetch(`ancestors:${id}`, () =>
    apiGet<IndexedAIP[]>(`/api/v2/aips/${id}/ancestors`),
  );
}

export function getMetadataHtml(aipId: string, metadataId: string): Promise<string> {
  return htmlCache.getOrFetch(`html:${aipId}:${metadataId}`, () =>
    apiGetHtml(`/api/v2/aips/${aipId}/metadata/descriptive/${metadataId}/html`),
  );
}

export async function getTopLevelAIPs(
  offset = 0,
  limit = 50,
): Promise<AIPIndexResult> {
  const request: FindRequest = {
    filter: {
      parameters: [{
        type: 'EmptyKeyFilterParameter',
        name: 'parentID',
      } as EmptyKeyFilterParameter],
    },
    onlyActive: true,
    sublist: { firstElementIndex: offset, maximumElementCount: limit },
    sorter: { parameters: [{ name: 'title', descending: false }] },
  };

  return apiPost<AIPIndexResult>('/api/v2/aips/find', request);
}

export async function getChildrenOfAIP(
  parentId: string,
  offset = 0,
  limit = 50,
): Promise<AIPIndexResult> {
  const request: FindRequest = {
    filter: {
      parameters: [{
        type: 'SimpleFilterParameter',
        name: 'parentID',
        value: parentId,
      } as SimpleFilterParameter],
    },
    onlyActive: true,
    sublist: { firstElementIndex: offset, maximumElementCount: limit },
    sorter: { parameters: [{ name: 'title', descending: false }] },
  };

  return apiPost<AIPIndexResult>('/api/v2/aips/find', request);
}

export async function moveAIPs(
  selectedItems: { ids: string[] },
  targetAipId: string,
  details: string,
): Promise<Job> {
  aipCache.clear();
  ancestorCache.clear();
  return apiPost<Job>('/api/v2/aips/move', {
    selectedItems,
    targetAipId,
    details,
  });
}

export async function deleteAIPs(
  selectedItems: { ids: string[] },
  details: string,
): Promise<Job> {
  aipCache.clear();
  return apiPost<Job>('/api/v2/aips/delete', {
    selectedItems,
    details,
  });
}

export async function updatePermissions(
  aipId: string,
  permissions: Permissions,
): Promise<void> {
  aipCache.invalidate(`aip:${aipId}`);
  await apiPatch<void>('/api/v2/aips/permissions/update', {
    selectedItems: { ids: [aipId] },
    permissions,
  });
}

export function invalidateAipCache(aipId?: string): void {
  if (aipId) {
    aipCache.invalidate(`aip:${aipId}`);
    htmlCache.invalidateByPrefix(`html:${aipId}`);
    ancestorCache.invalidate(`ancestors:${aipId}`);
  } else {
    aipCache.clear();
    htmlCache.clear();
    ancestorCache.clear();
  }
}

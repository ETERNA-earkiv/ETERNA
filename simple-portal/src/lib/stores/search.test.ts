/**
 * Tests for search store — offset reset, pagination, and state management.
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

// Mock the API module
vi.mock('../api/aip', () => ({
  searchAIPsWithFacets: vi.fn().mockResolvedValue({
    results: [],
    totalCount: 0,
    facetResults: [],
  }),
}));

// Mock config store
vi.mock('./config', () => ({
  $resultsPerPage: { get: () => 10 },
}));

// Mock metadata-API + parser: varje AIP får ett "XML-token" som parsern mappar
// till fält. DOMParser finns inte i node-miljön, därför mockas parsern.
const METADATA_BY_AIP: Record<string, { label: string; value: string }[]> = {
  'aip-1890': [{ label: 'Titel', value: 'Gammalt protokoll' }, { label: 'Startdatum', value: '1890-05-01' }],
  'aip-1940': [{ label: 'Titel', value: 'Nyare protokoll' }, { label: 'Startdatum', value: '1940-03-10' }],
  'aip-range': [{ label: 'Titel', value: 'Serie' }, { label: 'Startdatum', value: '1899' }, { label: 'Slutdatum', value: '1905' }],
  'aip-nodate': [{ label: 'Titel', value: 'Odaterad' }],
};

vi.mock('../api/metadata', () => ({
  getAvailableMetadataIds: vi.fn(async () => ['ead_3.xml']),
  getMetadataXml: vi.fn(async (aipId: string) => `xml:${aipId}`),
}));

vi.mock('../utils/metadata-parser', () => ({
  parseXmlToFields: vi.fn((xml: string) => METADATA_BY_AIP[xml.replace('xml:', '')] ?? []),
}));

// Mock fetch for loadAllowedLevels
vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
  ok: true,
  json: () => Promise.resolve({ visibilityConfig: { allowedLevels: [] } }),
}));

import type { SimpleFilterParameter } from '../types/api';
import {
  $query,
  $offset,
  $results,
  $totalCount,
  $currentPage,
  search,
  goToPage,
  resetSearch,
} from './search';

describe('search store', () => {
  beforeEach(() => {
    resetSearch();
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('starts with offset 0', () => {
    expect($offset.get()).toBe(0);
  });

  it('resets offset to 0 when query changes', async () => {
    // Simulate being on page 3
    $offset.set(20);
    expect($offset.get()).toBe(20);

    // Search with new query should reset offset
    const promise = search('new query');
    expect($offset.get()).toBe(0);

    vi.advanceTimersByTime(300);
    await promise;
  });

  it('resets offset to 0 when filters change', async () => {
    $offset.set(30);

    const filter: SimpleFilterParameter = { type: 'SimpleFilterParameter', name: 'level', value: 'fonds' };
    const promise = search(undefined, [filter]);
    expect($offset.get()).toBe(0);

    vi.advanceTimersByTime(300);
    await promise;
  });

  it('does not reset offset when search() is called without args (pagination)', async () => {
    $offset.set(20);

    const promise = search();
    // Offset should remain unchanged when no query/filter args
    expect($offset.get()).toBe(20);

    vi.advanceTimersByTime(300);
    await promise;
  });

  it('goToPage sets correct offset', () => {
    goToPage(3);
    // Page 3 with 10 results per page = offset 20
    expect($offset.get()).toBe(20);
  });

  it('resetSearch clears all state', () => {
    $query.set('test');
    $offset.set(20);
    $totalCount.set(100);

    resetSearch();

    expect($query.get()).toBe('');
    expect($offset.get()).toBe(0);
    expect($totalCount.get()).toBe(0);
    expect($results.get()).toEqual([]);
  });

  it('currentPage computes correctly from offset', () => {
    $offset.set(0);
    expect($currentPage.get()).toBe(1);

    $offset.set(10);
    expect($currentPage.get()).toBe(2);

    $offset.set(20);
    expect($currentPage.get()).toBe(3);
  });
});

describe('client-side datumfilter (Startdatum/Slutdatum ur metadata)', () => {
  const ALL_AIPS = ['aip-1890', 'aip-1940', 'aip-range', 'aip-nodate'].map((id) => ({
    aip: { uuid: id, id, title: id, level: 'item', createdOn: '2026-01-01T00:00:00Z' },
  }));

  beforeEach(async () => {
    resetSearch();
    vi.useFakeTimers();
    const { searchAIPsWithFacets } = await import('../api/aip');
    (searchAIPsWithFacets as any).mockResolvedValue({
      results: ALL_AIPS,
      totalCount: ALL_AIPS.length,
      facetResults: [],
    });
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  async function runSearchWithClientFilters(clientFilters: Record<string, string>) {
    const promise = search('', [], clientFilters);
    await vi.advanceTimersByTimeAsync(300);
    await promise;
  }

  it('1888–1900 ger bara poster med datum i spannet — inte 1940 och inte odaterade', async () => {
    await runSearchWithClientFilters({ datesFrom: '1888-01-01', datesTo: '1900-01-01' });

    const ids = $results.get().map((a) => a.id).sort();
    expect(ids).toEqual(['aip-1890', 'aip-range']);
    expect($totalCount.get()).toBe(2);
  });

  it('bara "från" fungerar som öppen gräns', async () => {
    await runSearchWithClientFilters({ datesFrom: '1900-01-01' });

    const ids = $results.get().map((a) => a.id).sort();
    expect(ids).toEqual(['aip-1940', 'aip-range']);
  });

  it('bara "till" fungerar som öppen gräns', async () => {
    await runSearchWithClientFilters({ datesTo: '1895-12-31' });

    expect($results.get().map((a) => a.id)).toEqual(['aip-1890']);
  });

  it('utan datumfilter går sökningen server-side och lämnar alla poster orörda', async () => {
    await runSearchWithClientFilters({});

    expect($results.get()).toHaveLength(ALL_AIPS.length);
  });
});

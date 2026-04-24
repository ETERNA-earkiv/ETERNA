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

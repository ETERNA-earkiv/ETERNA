import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '../api/client'
import type { FindRequest, IndexResult, FilterParameter, SimpleFacetConfig } from '../types'

interface UseFindRequestOptions {
  endpoint: string
  queryKey: string
  defaultSort?: { name: string; descending: boolean }[]
  facetFields?: string[]
  pageSize?: number
  onlyActive?: boolean
  initialFilters?: FilterParameter[]
}

interface UseFindRequestResult<T> {
  data: T[]
  totalCount: number
  facets: IndexResult<T>['facetResults']
  isLoading: boolean
  error: Error | null
  page: number
  pageSize: number
  setPage: (page: number) => void
  filters: FilterParameter[]
  setFilters: (filters: FilterParameter[]) => void
  query: string
  setQuery: (q: string) => void
  refetch: () => void
}

export function useFindRequest<T>({
  endpoint,
  queryKey,
  defaultSort = [],
  facetFields = [],
  pageSize = 20,
  onlyActive,
  initialFilters = [],
}: UseFindRequestOptions): UseFindRequestResult<T> {
  const [page, setPage] = useState(0)
  const [filters, setFilters] = useState<FilterParameter[]>(initialFilters)
  const [query, setQueryState] = useState('')

  function setQuery(q: string) {
    setQueryState(q)
    setPage(0)
  }

  function setFiltersAndReset(f: FilterParameter[]) {
    setFilters(f)
    setPage(0)
  }

  const facetParams = facetFields.reduce<Record<string, SimpleFacetConfig>>(
    (acc, field) => ({
      ...acc,
      [field]: { type: 'SimpleFacetParameter', name: field, limit: 20 },
    }),
    {}
  )

  const body: FindRequest = {
    filter: {
      filterParameters: [
        ...(query
          ? [{ type: 'SimpleFilterParameter', name: 'fulltext', value: query } as FilterParameter]
          : []),
        ...filters,
      ],
    },
    sorter: defaultSort.length > 0 ? { parameters: defaultSort } : undefined,
    sublist: { firstElementIndex: page * pageSize, maximumElementCount: pageSize },
    facets: { parameters: facetParams },
    ...(onlyActive !== undefined && { onlyActive }),
  }

  const { data, isLoading, error, refetch } = useQuery({
    queryKey: [queryKey, page, filters, query],
    queryFn: () => api.post<IndexResult<T>>(endpoint, body),
  })

  return {
    data: data?.results ?? [],
    totalCount: data?.totalCount ?? 0,
    facets: data?.facetResults ?? null,
    isLoading,
    error: error as Error | null,
    page,
    pageSize,
    setPage,
    filters,
    setFilters: setFiltersAndReset,
    query,
    setQuery,
    refetch,
  }
}

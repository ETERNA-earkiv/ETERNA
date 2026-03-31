// Shared types used across multiple API modules and components

export interface FacetValue {
  value: string
  count: number
}

export interface FacetResult {
  field: string
  values: FacetValue[]
}

export interface IndexResult<T> {
  results: T[] | null
  totalCount: number
  offset: number
  limit: number
  facetResults?: FacetResult[] | null
}

export type FilterParameterType =
  | 'SimpleFilterParameter'
  | 'EmptyKeyFilterParameter'
  | 'BasicSearchFilterParameter'
  | 'DateRangeFilterParameter'
  | 'LongRangeFilterParameter'

export interface FilterParameter {
  type: FilterParameterType | string
  name: string
  value: string
}

export interface FindRequest {
  filter?: {
    filterParameters: FilterParameter[]
  }
  sorter?: {
    parameters: Array<{
      name: string
      descending: boolean
    }>
  }
  sublist?: {
    firstElementIndex: number
    maximumElementCount: number
  }
  facets?: {
    parameters: Record<string, unknown>
  }
  onlyActive?: boolean
}

export interface SimpleFacetConfig {
  type: 'SimpleFacetParameter'
  name: string
  limit: number
}

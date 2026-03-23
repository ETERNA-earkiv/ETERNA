import { api } from './client'

export interface IndexedAIP {
  id: string
  title: string
  dateCreated: string
  dateModified: string
  level: string
  state: string
  hasRepresentations: boolean
  numberOfSubmissionFiles: number
  numberOfDocumentationFiles: number
  numberOfSchemasFiles: number
  ingestJobId: string
  ghost: boolean
  onHold: boolean
}

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

export interface FindRequest {
  filter?: {
    filterParameters: Array<{
      type: string
      name: string
      value: string
    }>
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

export interface DescriptiveMetadataInfo {
  id: string
  type: string
  version: string
}

export function searchAIPs(
  query: string,
  offset = 0,
  limit = 20,
  activeFilters: Record<string, string> = {}
): Promise<IndexResult<IndexedAIP>> {
  const filterParameters: Array<{ type: string; name: string; value: string }> = []

  if (query) {
    filterParameters.push({ type: 'SimpleFilterParameter', name: 'fulltext', value: query })
  }

  for (const [field, value] of Object.entries(activeFilters)) {
    if (value) {
      filterParameters.push({ type: 'SimpleFilterParameter', name: field, value })
    }
  }

  const body: FindRequest = {
    filter: { filterParameters },
    sublist: { firstElementIndex: offset, maximumElementCount: limit },
    sorter: { parameters: [{ name: 'dateModified', descending: true }] },
    facets: {
      parameters: {
        state: { type: 'SimpleFacetParameter', name: 'state', limit: 10 },
        level: { type: 'SimpleFacetParameter', name: 'level', limit: 20 },
      },
    },
    onlyActive: true,
  }

  return api.post<IndexResult<IndexedAIP>>('/aips/find', body)
}

export function getMetadataList(
  aipId: string
): Promise<{ metadataInfos: DescriptiveMetadataInfo[] }> {
  return api.get<{ metadataInfos: DescriptiveMetadataInfo[] }>(
    `/aips/${aipId}/metadata/descriptive`
  )
}

export function getMetadataHtml(aipId: string, metadataId: string): Promise<string> {
  return api.getText(`/aips/${aipId}/metadata/descriptive/${metadataId}/html`)
}

export function getAIP(id: string): Promise<IndexedAIP> {
  return api.get<IndexedAIP>(`/aips/find/${id}`)
}

export interface Representation {
  id: string
  aipId: string
  type: string
  original: boolean
  numberOfDataFiles: number
}

export function getRepresentations(aipId: string): Promise<IndexResult<Representation>> {
  return api.post<IndexResult<Representation>>('/representations/find', {
    filter: {
      filterParameters: [{ type: 'SimpleFilterParameter', name: 'aipId', value: aipId }],
    },
    sublist: { firstElementIndex: 0, maximumElementCount: 100 },
    facets: { parameters: {} },
    onlyActive: true,
  })
}

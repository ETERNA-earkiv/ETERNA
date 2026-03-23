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

export interface IndexResult<T> {
  results: T[] | null
  totalCount: number
  offset: number
  limit: number
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

export function searchAIPs(
  query: string,
  offset = 0,
  limit = 20
): Promise<IndexResult<IndexedAIP>> {
  const body: FindRequest = {
    sublist: { firstElementIndex: offset, maximumElementCount: limit },
    sorter: { parameters: [{ name: 'dateModified', descending: true }] },
    facets: { parameters: {} },
    onlyActive: true,
  }
  if (query) {
    body.filter = {
      filterParameters: [{ type: 'SimpleFilterParameter', name: 'fulltext', value: query }],
    }
  } else {
    body.filter = { filterParameters: [] }
  }
  return api.post<IndexResult<IndexedAIP>>('/aips/find', body)
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

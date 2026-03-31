import { api } from './client'
import type { IndexResult } from '../types'

export interface IndexedRepresentation {
  uuid: string
  id: string
  aipId: string
  type: string
  original: boolean
  numberOfDataFiles: number
  numberOfDocumentationFiles: number
  numberOfSchemasFiles: number
  sizeInBytes: number
  states: string[]
}

export function findRepresentations(
  aipId: string,
  offset = 0,
  limit = 100
): Promise<IndexResult<IndexedRepresentation>> {
  return api.post<IndexResult<IndexedRepresentation>>('/representations/find', {
    filter: {
      filterParameters: [
        { type: 'SimpleFilterParameter', name: 'aipId', value: aipId },
      ],
    },
    sublist: { firstElementIndex: offset, maximumElementCount: limit },
    sorter: { parameters: [{ name: 'id', descending: false }] },
    facets: { parameters: {} },
    onlyActive: true,
  })
}

export function getRepresentation(uuid: string): Promise<IndexedRepresentation> {
  return api.get<IndexedRepresentation>(`/representations/find/${uuid}`)
}

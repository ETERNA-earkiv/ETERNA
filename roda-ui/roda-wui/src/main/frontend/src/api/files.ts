import { api } from './client'
import type { IndexResult } from '../types'

export interface IndexedFile {
  uuid: string
  id: string
  aipId: string
  representationId: string
  path: string[]
  fileId: string
  size: number
  fileFormat: string | null
}

export function searchFiles(
  representationUuid: string,
  offset = 0,
  limit = 100
): Promise<IndexResult<IndexedFile>> {
  return api.post<IndexResult<IndexedFile>>('/files', {
    filter: {
      filterParameters: [
        { type: 'SimpleFilterParameter', name: 'representationUUID', value: representationUuid },
      ],
    },
    sublist: { firstElementIndex: offset, maximumElementCount: limit },
    sorter: { parameters: [{ name: 'fileId', descending: false }] },
    facets: { parameters: {} },
    onlyActive: true,
  })
}

export function fileDownloadUrl(uuid: string): string {
  return `/api/v2/files/${uuid}/download`
}

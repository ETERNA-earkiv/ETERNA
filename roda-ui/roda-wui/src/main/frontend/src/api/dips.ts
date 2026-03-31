import { api } from './client'
import type { IndexResult } from '../types'

export interface IndexedDIP {
  uuid: string
  id: string
  title: string
  description: string
  type: string
  dateCreated: string
  dateModified: string
  openExternalURL: boolean
  openInternalURL: string
  aipIds: string[]
  representationIds: string[]
  fileIds: string[]
  permissions: Record<string, unknown>
}

export interface IndexedDIPFile {
  uuid: string
  id: string
  dipId: string
  path: string[]
  size: number
  fileFormat: string | null
}

export function findDIPs(
  aipId?: string,
  offset = 0,
  limit = 20
): Promise<IndexResult<IndexedDIP>> {
  const filterParameters = aipId
    ? [{ type: 'SimpleFilterParameter', name: 'aipIds', value: aipId }]
    : []

  return api.post<IndexResult<IndexedDIP>>('/dips/find', {
    filter: { filterParameters },
    sublist: { firstElementIndex: offset, maximumElementCount: limit },
    sorter: { parameters: [{ name: 'dateCreated', descending: true }] },
    facets: { parameters: {} },
    onlyActive: true,
  })
}

export function getDIP(uuid: string): Promise<IndexedDIP> {
  return api.get<IndexedDIP>(`/dips/find/${uuid}`)
}

export function findDIPFiles(dipId: string, offset = 0, limit = 100): Promise<IndexResult<IndexedDIPFile>> {
  return api.post<IndexResult<IndexedDIPFile>>('/dip-files/find', {
    filter: {
      filterParameters: [
        { type: 'SimpleFilterParameter', name: 'dipId', value: dipId },
      ],
    },
    sublist: { firstElementIndex: offset, maximumElementCount: limit },
    sorter: { parameters: [{ name: 'id', descending: false }] },
    facets: { parameters: {} },
  })
}

export function dipFileDownloadUrl(uuid: string): string {
  return `/api/v2/dip-files/${uuid}/download`
}

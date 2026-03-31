import { api } from './client'
import type { IndexResult } from '../types'

export interface TransferredResource {
  uuid: string
  name: string
  size: number
  date: string | null
  isDirectory: boolean
  ancestorsUUIDs: string[]
}

export function listTransfers(offset = 0, limit = 20): Promise<IndexResult<TransferredResource>> {
  return api.post<IndexResult<TransferredResource>>('/transfers/find', {
    filter: { filterParameters: [] },
    sublist: { firstElementIndex: offset, maximumElementCount: limit },
    sorter: { parameters: [{ name: 'date', descending: true }] },
    facets: { parameters: {} },
  })
}

export function deleteTransfer(uuid: string): Promise<void> {
  return api.delete<void>(`/transfers/${uuid}`)
}

export function uploadSIP(file: File): Promise<TransferredResource> {
  const formData = new FormData()
  formData.append('resource', file)
  formData.append('commit', 'true')
  return api.multipart<TransferredResource>('/transfers/create/resource', formData)
}

export function createIngestJob(transferredResourceUuid: string): Promise<{ id: string }> {
  return api.post<{ id: string }>('/jobs/create', {
    name: `Ingest ${transferredResourceUuid}`,
    plugin: 'org.roda.core.plugins.base.ingest.EARKSIP2ToAIPPlugin',
    sourceObjects: {
      objectList: [
        {
          objectClass: 'org.roda.core.data.v2.ip.TransferredResource',
          id: transferredResourceUuid,
        },
      ],
    },
    pluginParameters: {},
  })
}

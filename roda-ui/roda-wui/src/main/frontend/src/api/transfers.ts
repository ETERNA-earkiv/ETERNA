import { api } from './client'

export interface TransferredResource {
  uuid: string
  name: string
  size: number
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

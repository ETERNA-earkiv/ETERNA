import { api } from './client'
import type { IndexResult } from './aips'

export interface JobStats {
  sourceObjectsCount: number
  processedSourceObjectsCount: number
  sourceObjectsProcessedWithFailure: number
}

export interface IndexedJob {
  uuid: string
  name: string
  username: string
  startDate: string
  endDate: string | null
  state: string
  plugin: string
  jobStats: JobStats
}

export function searchJobs(offset = 0, limit = 20): Promise<IndexResult<IndexedJob>> {
  return api.post<IndexResult<IndexedJob>>('/jobs', {
    filter: { filterParameters: [] },
    sublist: { firstElementIndex: offset, maximumElementCount: limit },
    sorter: { parameters: [{ name: 'startDate', descending: true }] },
    facets: { parameters: {} },
    onlyActive: false,
  })
}

export function stopJob(id: string): Promise<void> {
  return api.put<void>(`/jobs/${id}/stop`, {})
}

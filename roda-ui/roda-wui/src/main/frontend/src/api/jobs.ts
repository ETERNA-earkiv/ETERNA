import { api } from './client'
import type { IndexResult } from '../types'

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

export function getJob(id: string): Promise<IndexedJob> {
  return api.get<IndexedJob>(`/jobs/find/${id}`)
}

export function stopJob(id: string): Promise<void> {
  return api.put<void>(`/jobs/${id}/stop`, {})
}

export interface JobReport {
  id: string
  jobId: string
  jobName: string
  sourceObjectId: string
  sourceObjectLabel: string
  sourceObjectClass: string
  outcomeObjectId: string
  outcomeObjectLabel: string
  outcomeObjectClass: string
  dateCreated: string
  dateUpdated: string
  completionPercentage: number
  stepsCompleted: number
  totalSteps: number
  successfulPlugins: number
  unsuccessfulPlugins: number
  pluginState: string
  pluginDetails: string
}

export interface JobReports {
  results: JobReport[]
  totalCount: number
  offset: number
  limit: number
}

export function listJobReports(
  jobId: string,
  justFailed = false,
  offset = 0,
  limit = 20
): Promise<JobReports> {
  return api.get<JobReports>(
    `/jobs/${jobId}/reports?justFailed=${justFailed}&start=${offset}&limit=${limit}`
  )
}

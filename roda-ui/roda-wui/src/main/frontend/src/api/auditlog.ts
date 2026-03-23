import { api } from './client'
import type { IndexResult } from './aips'

export interface LogEntry {
  uuid: string
  datetime: string
  username: string
  actionComponent: string
  actionMethod: string
  relatedObjectID: string
  state: string
  duration: number
}

export function searchLogs(query: string, offset = 0, limit = 20): Promise<IndexResult<LogEntry>> {
  const filterParameters: Array<{ type: string; name: string; value: string }> = []

  if (query) {
    filterParameters.push({ type: 'SimpleFilterParameter', name: 'fulltext', value: query })
  }

  return api.post<IndexResult<LogEntry>>('/audit-logs', {
    filter: { filterParameters },
    sublist: { firstElementIndex: offset, maximumElementCount: limit },
    sorter: { parameters: [{ name: 'datetime', descending: true }] },
    facets: { parameters: {} },
  })
}

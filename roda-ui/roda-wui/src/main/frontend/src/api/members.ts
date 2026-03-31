import { api } from './client'
import type { FindRequest, IndexResult } from '../types'

export interface RODAMember {
  id: string
  name: string
  fullName: string
  email: string
  active: boolean
  isUser: boolean
  isGroup: boolean
  allRoles: string[]
  groups: string[]
}

export function searchMembers(
  query: string,
  offset = 0,
  limit = 20
): Promise<IndexResult<RODAMember>> {
  const body: FindRequest = {
    sublist: { firstElementIndex: offset, maximumElementCount: limit },
    facets: { parameters: {} },
    onlyActive: false,
  }
  if (query) {
    body.filter = {
      filterParameters: [{ type: 'SimpleFilterParameter', name: 'fulltext', value: query }],
    }
  } else {
    body.filter = { filterParameters: [] }
  }
  return api.post<IndexResult<RODAMember>>('/members/find', body)
}

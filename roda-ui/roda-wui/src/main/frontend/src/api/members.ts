import { api } from './client'
import type { FindRequest, IndexResult } from './aips'

export interface RODAMember {
  id: string
  name: string
  fullName: string
  email: string
  active: boolean
  isUser: boolean
  isGroup: boolean
  roles: string[]
  groups: string[]
}

export function searchMembers(
  query: string,
  offset = 0,
  limit = 20
): Promise<IndexResult<RODAMember>> {
  const body: FindRequest = {
    sublist: { firstElementIndex: offset, maximumElementCount: limit },
  }
  if (query) {
    body.filter = {
      parameters: [{ name: 'fulltext', value: query }],
    }
  }
  return api.post<IndexResult<RODAMember>>('/members', body)
}

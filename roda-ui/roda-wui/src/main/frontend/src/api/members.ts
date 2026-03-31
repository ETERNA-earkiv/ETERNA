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

export interface User {
  id: string
  name: string
  fullName: string
  email: string
  active: boolean
  groups: string[]
  allRoles: string[]
  directRoles: string[]
  extra: Record<string, string>
  resetPasswordToken: string | null
  emailConfirmationToken: string | null
  ipAddress: string | null
  lastLogin: string | null
}

export interface Group {
  id: string
  name: string
  fullName: string
  users: string[]
  allRoles: string[]
  directRoles: string[]
}

export function getUser(name: string): Promise<User> {
  return api.get<User>(`/members/users/${name}`)
}

export function getGroup(name: string): Promise<Group> {
  return api.get<Group>(`/members/groups/${name}`)
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

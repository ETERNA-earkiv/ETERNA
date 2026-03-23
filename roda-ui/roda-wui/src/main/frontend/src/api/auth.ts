import { api } from './client'

export interface UserInfo {
  id: string
  name: string
  fullName: string
  email: string
  allRoles: string[]
  groups: string[]
  guest: boolean
  active: boolean
}

export async function login(username: string, password: string): Promise<UserInfo> {
  // RODA API expects password as {chars: char[]}
  return api.post<UserInfo>('/members/users/login', {
    username,
    password: { chars: password.split('') },
  })
}

export async function logout(): Promise<void> {
  // Spring Security logout via GET /logout (returns redirect)
  try {
    await fetch('/logout', { method: 'GET', credentials: 'include', redirect: 'manual' })
  } catch {
    // ignore redirect errors
  }
}

export async function getCurrentUser(): Promise<UserInfo> {
  return api.get<UserInfo>('/members/users/authenticated')
}

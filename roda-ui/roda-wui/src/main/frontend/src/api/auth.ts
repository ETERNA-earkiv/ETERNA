import { api } from './client'

export interface LoginRequest {
  username: string
  password: string
}

export interface UserInfo {
  id: string
  name: string
  fullName: string
  email: string
  roles: string[]
}

export async function login(credentials: LoginRequest): Promise<void> {
  await api.post('/members/login', credentials)
}

export async function logout(): Promise<void> {
  await api.post('/members/logout', {})
}

export async function getCurrentUser(): Promise<UserInfo> {
  return api.get<UserInfo>('/members/me')
}

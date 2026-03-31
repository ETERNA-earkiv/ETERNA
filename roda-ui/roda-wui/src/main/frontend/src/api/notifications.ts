import { api } from './client'
import type { IndexResult } from '../types'

export interface Notification {
  id: string
  subject: string
  body: string
  sentOn: string | null
  fromUser: string
  recipientUsers: string[]
  isAcknowledged: boolean
  acknowledgedUsers: Record<string, string>
  state: string
}

export function listNotifications(offset = 0, limit = 20): Promise<IndexResult<Notification>> {
  return api.post<IndexResult<Notification>>('/notifications/find', {
    filter: { filterParameters: [] },
    sublist: { firstElementIndex: offset, maximumElementCount: limit },
    sorter: { parameters: [{ name: 'sentOn', descending: true }] },
    facets: { parameters: {} },
  })
}

export function getNotification(id: string): Promise<Notification> {
  return api.get<Notification>(`/notifications/find/${id}`)
}

export function acknowledgeNotification(notificationUUID: string, token: string): Promise<{ value: string }> {
  return api.post<{ value: string }>('/notifications/acknowledge', {
    notificationUUID,
    token,
  })
}

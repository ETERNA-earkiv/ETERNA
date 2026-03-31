import { api } from './client'
import type { IndexResult } from '../types'

export interface IndexedPreservationEvent {
  id: string
  aipID: string
  representationID: string | null
  fileID: string | null
  objectClass: string
  eventType: string
  eventDateTime: string
  eventDetail: string
  eventOutcome: string
  eventOutcomeDetailNote: string
  linkingAgentIds: string[]
  linkingObjectIds: string[]
}

export interface PreservationEventLinkingAgent {
  value: string
  role: string
  type: string
}

export function findPreservationEvents(
  params: { aipId?: string; representationId?: string; fileId?: string },
  offset = 0,
  limit = 20
): Promise<IndexResult<IndexedPreservationEvent>> {
  const filterParameters: Array<{ type: string; name: string; value: string }> = []

  if (params.aipId) {
    filterParameters.push({ type: 'SimpleFilterParameter', name: 'aipID', value: params.aipId })
  }
  if (params.representationId) {
    filterParameters.push({
      type: 'SimpleFilterParameter',
      name: 'representationID',
      value: params.representationId,
    })
  }
  if (params.fileId) {
    filterParameters.push({ type: 'SimpleFilterParameter', name: 'fileID', value: params.fileId })
  }

  return api.post<IndexResult<IndexedPreservationEvent>>('/preservation/events/find', {
    filter: { filterParameters },
    sublist: { firstElementIndex: offset, maximumElementCount: limit },
    sorter: { parameters: [{ name: 'eventDateTime', descending: true }] },
    facets: { parameters: {} },
  })
}

export function getPreservationEvent(id: string): Promise<IndexedPreservationEvent> {
  return api.get<IndexedPreservationEvent>(`/preservation/events/find/${id}`)
}

export function getPreservationEventAgents(
  id: string
): Promise<PreservationEventLinkingAgent[]> {
  return api.get<PreservationEventLinkingAgent[]>(`/preservation/events/${id}/agents`)
}

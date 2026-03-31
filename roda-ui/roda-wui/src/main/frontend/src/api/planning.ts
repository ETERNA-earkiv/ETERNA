import { api } from './client'
import type { IndexResult } from '../types'

// ── Enums ──────────────────────────────────────────────────────────────────

export type IncidenceStatus = 'UNMITIGATED' | 'MITIGATED'
export type SeverityLevel = 'LOW' | 'MODERATE' | 'HIGH' | 'CRITICAL'

// ── Risk ───────────────────────────────────────────────────────────────────

export interface IndexedRisk {
  id: string
  name: string
  description: string | null
  identifiedOn: string | null
  identifiedBy: string | null
  categories: string[]
  notes: string | null

  preMitigationProbability: number | null
  preMitigationImpact: number | null
  preMitigationSeverity: number | null
  preMitigationSeverityLevel: SeverityLevel | null
  preMitigationNotes: string | null

  postMitigationProbability: number | null
  postMitigationImpact: number | null
  postMitigationSeverity: number | null
  postMitigationSeverityLevel: SeverityLevel | null
  postMitigationNotes: string | null

  mitigationStrategy: string | null
  mitigationOwner: string | null
  mitigationRelatedEventIdentifierType: string | null
  mitigationRelatedEventIdentifierValue: string | null

  incidencesCount: number

  createdOn: string | null
  createdBy: string | null
  updatedOn: string | null
  updatedBy: string | null
}

export function findRisks(
  offset = 0,
  limit = 20
): Promise<IndexResult<IndexedRisk>> {
  return api.post<IndexResult<IndexedRisk>>('/risks/find', {
    filter: { filterParameters: [] },
    sublist: { firstElementIndex: offset, maximumElementCount: limit },
    sorter: { parameters: [{ name: 'name', descending: false }] },
    facets: { parameters: {} },
  })
}

export function getRisk(id: string): Promise<IndexedRisk> {
  return api.get<IndexedRisk>(`/risks/find/${id}`)
}

// ── Risk Incidence ─────────────────────────────────────────────────────────

export interface IndexedRiskIncidence {
  id: string
  riskId: string
  aipId: string | null
  representationId: string | null
  fileId: string | null
  objectClass: string | null
  description: string | null
  byPlugin: boolean
  status: IncidenceStatus
  severity: SeverityLevel | null
  detectedOn: string | null
  detectedBy: string | null
  mitigatedOn: string | null
  mitigatedBy: string | null
  mitigatedDescription: string | null
  instanceId: string | null
  instanceName: string | null
  updatedOn: string | null
}

export function findRiskIncidences(
  riskId: string,
  offset = 0,
  limit = 20
): Promise<IndexResult<IndexedRiskIncidence>> {
  return api.post<IndexResult<IndexedRiskIncidence>>('/incidences/find', {
    filter: {
      filterParameters: [
        { type: 'SimpleFilterParameter', name: 'riskId', value: riskId },
      ],
    },
    sublist: { firstElementIndex: offset, maximumElementCount: limit },
    sorter: { parameters: [{ name: 'detectedOn', descending: true }] },
    facets: { parameters: {} },
  })
}

// ── Representation Information ─────────────────────────────────────────────

export interface RepresentationInformation {
  id: string
  name: string
  description: string | null
  family: string | null
  tags: string[]
  createdOn: string | null
  createdBy: string | null
  updatedOn: string | null
  updatedBy: string | null
}

export function findRepresentationInformation(
  offset = 0,
  limit = 20
): Promise<IndexResult<RepresentationInformation>> {
  return api.post<IndexResult<RepresentationInformation>>('/representation-information/find', {
    filter: { filterParameters: [] },
    sublist: { firstElementIndex: offset, maximumElementCount: limit },
    sorter: { parameters: [{ name: 'name', descending: false }] },
    facets: { parameters: {} },
  })
}

export function getRepresentationInformation(id: string): Promise<RepresentationInformation> {
  return api.get<RepresentationInformation>(`/representation-information/find/${id}`)
}

// ── Preservation Agent ─────────────────────────────────────────────────────

export interface IndexedPreservationAgent {
  id: string
  name: string
  type: string | null
  version: string | null
  note: string | null
  extension: string | null
  roles: string[]
  createdOn: string | null
  instanceId: string | null
  instanceName: string | null
}

export function findPreservationAgents(
  offset = 0,
  limit = 20
): Promise<IndexResult<IndexedPreservationAgent>> {
  return api.post<IndexResult<IndexedPreservationAgent>>('/preservation/agents/find', {
    filter: { filterParameters: [] },
    sublist: { firstElementIndex: offset, maximumElementCount: limit },
    sorter: { parameters: [{ name: 'name', descending: false }] },
    facets: { parameters: {} },
  })
}

export function getPreservationAgent(id: string): Promise<IndexedPreservationAgent> {
  return api.get<IndexedPreservationAgent>(`/preservation/agents/find/${id}`)
}

export function preservationAgentDownloadUrl(id: string): string {
  return `/api/v2/preservation/agents/${id}/download`
}

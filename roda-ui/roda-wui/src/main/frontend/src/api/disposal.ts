import { api } from './client'
import type { IndexResult } from '../types'

// ── Enums ──────────────────────────────────────────────────────────────────

export type DisposalActionCode = 'RETAIN_PERMANENTLY' | 'REVIEW' | 'DESTROY'
export type RetentionPeriodIntervalCode =
  | 'NO_RETENTION_PERIOD'
  | 'DAYS'
  | 'WEEKS'
  | 'MONTHS'
  | 'YEARS'
export type DisposalScheduleState = 'ACTIVE' | 'INACTIVE'
export type DisposalHoldState = 'ACTIVE' | 'LIFTED'
export type DisposalConfirmationState =
  | 'PENDING'
  | 'APPROVED'
  | 'RESTORED'
  | 'PERMANENTLY_DELETED'
  | 'EXECUTION_FAILED'
export type DisposalRuleConditionType = 'IS_CHILD_OF' | 'METADATA_FIELD'

// ── Disposal Schedule ──────────────────────────────────────────────────────

export interface DisposalSchedule {
  id: string
  title: string
  description: string | null
  mandate: string | null
  scopeNotes: string | null
  actionCode: DisposalActionCode
  retentionTriggerElementId: string | null
  retentionPeriodIntervalCode: RetentionPeriodIntervalCode
  retentionPeriodDuration: number | null
  state: DisposalScheduleState
  firstTimeUsed: string | null
  apiCounter: number
  createdOn: string | null
  createdBy: string | null
  updatedOn: string | null
  updatedBy: string | null
}

export function listDisposalSchedules(): Promise<DisposalSchedule[]> {
  return api.get<DisposalSchedule[]>('/disposal/schedules')
}

export function getDisposalSchedule(id: string): Promise<DisposalSchedule> {
  return api.get<DisposalSchedule>(`/disposal/schedules/${id}`)
}

export function createDisposalSchedule(data: Partial<DisposalSchedule>): Promise<DisposalSchedule> {
  return api.post<DisposalSchedule>('/disposal/schedules', data)
}

export function updateDisposalSchedule(data: Partial<DisposalSchedule>): Promise<DisposalSchedule> {
  return api.put<DisposalSchedule>('/disposal/schedules', data)
}

export function deleteDisposalSchedule(id: string): Promise<void> {
  return api.delete<void>(`/disposal/schedules/${id}`)
}

// ── Disposal Hold ──────────────────────────────────────────────────────────

export interface DisposalHold {
  id: string
  title: string
  description: string | null
  mandate: string | null
  scopeNotes: string | null
  state: DisposalHoldState
  originatedOn: string | null
  originatedBy: string | null
  liftedOn: string | null
  liftedBy: string | null
  firstTimeUsed: string | null
  aipCounter: number
  createdOn: string | null
  createdBy: string | null
  updatedOn: string | null
  updatedBy: string | null
}

export function listDisposalHolds(): Promise<DisposalHold[]> {
  return api.get<DisposalHold[]>('/disposal/holds')
}

export function getDisposalHold(id: string): Promise<DisposalHold> {
  return api.get<DisposalHold>(`/disposal/holds/${id}`)
}

export function createDisposalHold(data: Partial<DisposalHold>): Promise<DisposalHold> {
  return api.post<DisposalHold>('/disposal/holds', data)
}

export function updateDisposalHold(data: Partial<DisposalHold>): Promise<DisposalHold> {
  return api.put<DisposalHold>('/disposal/holds', data)
}

// ── Disposal Rule ──────────────────────────────────────────────────────────

export interface DisposalRule {
  id: string
  title: string
  description: string | null
  type: DisposalRuleConditionType
  conditionKey: string | null
  conditionValue: string | null
  disposalScheduleId: string | null
  disposalScheduleName: string | null
  order: number
  createdOn: string | null
  createdBy: string | null
  updatedOn: string | null
  updatedBy: string | null
}

export function listDisposalRules(): Promise<DisposalRule[]> {
  return api.get<DisposalRule[]>('/disposal/rules')
}

export function getDisposalRule(id: string): Promise<DisposalRule> {
  return api.get<DisposalRule>(`/disposal/rules/${id}`)
}

export function createDisposalRule(data: Partial<DisposalRule>): Promise<DisposalRule> {
  return api.post<DisposalRule>('/disposal/rules', data)
}

export function updateDisposalRule(data: Partial<DisposalRule>): Promise<DisposalRule> {
  return api.put<DisposalRule>('/disposal/rules', data)
}

export function deleteDisposalRule(id: string): Promise<void> {
  return api.delete<void>(`/disposal/rules/${id}`)
}

// ── Disposal Confirmation ──────────────────────────────────────────────────

export interface DisposalConfirmation {
  id: string
  title: string
  state: DisposalConfirmationState
  numberOfAIPs: number
  size: number
  disposalScheduleIds: string[]
  disposalHoldIds: string[]
  createdOn: string | null
  createdBy: string | null
  executedOn: string | null
  executedBy: string | null
  restoredOn: string | null
  restoredBy: string | null
  extraFields: Record<string, string>
}

export function listDisposalConfirmations(
  offset = 0,
  limit = 20
): Promise<IndexResult<DisposalConfirmation>> {
  return api.post<IndexResult<DisposalConfirmation>>('/disposal/confirmations/find', {
    filter: { filterParameters: [] },
    sublist: { firstElementIndex: offset, maximumElementCount: limit },
    sorter: { parameters: [{ name: 'createdOn', descending: true }] },
    facets: { parameters: {} },
  })
}

export function getDisposalConfirmation(id: string): Promise<DisposalConfirmation> {
  return api.get<DisposalConfirmation>(`/disposal/confirmations/${id}`)
}

export function disposalConfirmationReportUrl(id: string): string {
  return `/api/v2/disposal/confirmations/${id}/report/html`
}

import { api } from './client'

export interface Metrics {
  metricList: Record<string, string>
}

const METRIC_KEYS = [
  'aipTotal',
  'dipTotal',
  'representationTotal',
  'fileTotal',
  'jobTotal',
  'riskTotal',
  'riskIncidenceTotal',
  'disposalScheduleTotal',
  'disposalHoldTotal',
  'disposalConfirmationTotal',
  'representationInformationTotal',
  'preservationEventTotal',
]

export function getMetrics(): Promise<Metrics> {
  const params = METRIC_KEYS.map((k) => `metricsToObtain=${k}`).join('&')
  return api.get<Metrics>(`/metrics?${params}`)
}

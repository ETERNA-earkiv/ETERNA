import { useEffect, useState, useCallback } from 'react'
import { searchJobs, type IndexedJob } from '../api/jobs'
import { useInterval } from '../hooks/useInterval'
import {
  DigiTable,
  DigiLoaderSpinner,
  DigiNotificationAlert,
  DigiNavigationPagination,
} from '@designsystem-se/af-react'

const PAGE_SIZE = 20

const STATE_STYLE: Record<string, { background: string; color: string }> = {
  STARTED: { background: '#e8f0fe', color: '#1a73e8' },
  CREATED: { background: '#e8f0fe', color: '#1a73e8' },
  COMPLETED: { background: '#e6f4ea', color: '#2d7a3a' },
  FAILED_TO_COMPLETE: { background: '#fce8e6', color: '#c5221f' },
  STOPPED: { background: '#f1f3f4', color: '#5f6368' },
}

function StateBadge({ state }: { state: string }) {
  const style = STATE_STYLE[state] ?? { background: '#f1f3f4', color: '#5f6368' }
  return (
    <span
      style={{
        padding: '0.2rem 0.6rem',
        borderRadius: '2px',
        fontSize: '0.8rem',
        ...style,
      }}
    >
      {state}
    </span>
  )
}

function formatDate(iso: string | null): string {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('sv-SE')
}

function formatProgress(job: IndexedJob): string {
  const { sourceObjectsCount, processedSourceObjectsCount } = job.jobStats
  if (!sourceObjectsCount) return '—'
  return `${processedSourceObjectsCount}/${sourceObjectsCount}`
}

export function JobsPage() {
  const [jobs, setJobs] = useState<IndexedJob[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const fetchJobs = useCallback(() => {
    searchJobs(page * PAGE_SIZE, PAGE_SIZE)
      .then((result) => {
        setJobs(result.results ?? [])
        setTotal(result.totalCount)
        setError('')
      })
      .catch(() => setError('Kunde inte hämta jobb.'))
      .finally(() => setLoading(false))
  }, [page])

  useEffect(() => {
    setLoading(true)
    fetchJobs()
  }, [fetchJobs])

  const hasActive = jobs.some((j) => j.state === 'STARTED' || j.state === 'CREATED')
  useInterval(fetchJobs, hasActive ? 5000 : null)

  const totalPages = Math.ceil(total / PAGE_SIZE)

  return (
    <div>
      <h1 style={{ marginBottom: '1.5rem' }}>Jobb</h1>

      {loading && (
        <div style={{ textAlign: 'center', padding: '2rem' }}>
          <DigiLoaderSpinner />
        </div>
      )}

      {error && (
        <DigiNotificationAlert afVariation="danger" afHeading="Fel">
          {error}
        </DigiNotificationAlert>
      )}

      {!loading && !error && (
        <>
          <p style={{ marginBottom: '0.75rem', color: '#555', fontSize: '0.875rem' }}>
            {total} jobb totalt
            {hasActive && (
              <span style={{ marginLeft: '0.75rem', color: '#1a73e8' }}>
                ● Uppdateras automatiskt
              </span>
            )}
          </p>

          <DigiTable>
            <table>
              <thead>
                <tr>
                  <th>Namn</th>
                  <th>Status</th>
                  <th>Framsteg</th>
                  <th>Start</th>
                  <th>Slut</th>
                </tr>
              </thead>
              <tbody>
                {jobs.length === 0 ? (
                  <tr>
                    <td colSpan={5} style={{ textAlign: 'center', padding: '2rem' }}>
                      Inga jobb hittades
                    </td>
                  </tr>
                ) : (
                  jobs.map((job) => (
                    <tr key={job.uuid}>
                      <td>
                        <span style={{ fontSize: '0.875rem' }}>{job.name || job.plugin}</span>
                      </td>
                      <td>
                        <StateBadge state={job.state} />
                      </td>
                      <td>{formatProgress(job)}</td>
                      <td style={{ fontSize: '0.8rem' }}>{formatDate(job.startDate)}</td>
                      <td style={{ fontSize: '0.8rem' }}>{formatDate(job.endDate)}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </DigiTable>

          {totalPages > 1 && (
            <div style={{ display: 'flex', justifyContent: 'center', marginTop: '1.5rem' }}>
              <DigiNavigationPagination
                key={`jobs-pagination-${page}`}
                afInitActivePage={1}
                afTotalPages={totalPages}
                onAfOnPageChange={(e) => {
                  setPage((e.detail as number) - 1)
                  window.scrollTo(0, 0)
                }}
              />
            </div>
          )}
        </>
      )}
    </div>
  )
}

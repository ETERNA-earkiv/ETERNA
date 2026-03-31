import { useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQueryClient } from '@tanstack/react-query'
import { useFindRequest } from '../hooks/useFindRequest'
import { useInterval } from '../hooks/useInterval'
import { DataTable, type Column } from '../components/common/DataTable'
import { Pagination } from '../components/common/Pagination'
import { StatusBadge } from '../components/common/StatusBadge'
import { useTranslation } from '../i18n/TranslationContext'
import type { IndexedJob } from '../api/jobs'

function formatDate(iso: string | null): string {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('sv-SE')
}

function formatProgress(job: IndexedJob): string {
  const { sourceObjectsCount, processedSourceObjectsCount, sourceObjectsProcessedWithFailure } =
    job.jobStats
  if (!sourceObjectsCount) return '—'
  const failed = sourceObjectsProcessedWithFailure > 0
    ? ` (${sourceObjectsProcessedWithFailure} misslyckade)`
    : ''
  return `${processedSourceObjectsCount}/${sourceObjectsCount}${failed}`
}

export function JobsPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const { data: jobs, totalCount, isLoading, error, page, pageSize, setPage } =
    useFindRequest<IndexedJob>({
      endpoint: '/jobs/find',
      queryKey: 'jobs',
      defaultSort: [{ name: 'startDate', descending: true }],
    })

  const hasActive = useMemo(
    () => jobs.some((j) => j.state === 'STARTED' || j.state === 'CREATED'),
    [jobs]
  )

  // Auto-refresh when jobs are running
  useInterval(() => {
    queryClient.invalidateQueries({ queryKey: ['jobs'] })
  }, hasActive ? 5000 : null)

  const totalPages = Math.ceil(totalCount / pageSize)

  const columns: Column<IndexedJob>[] = [
    {
      key: 'name',
      header: t('label.name', 'Namn'),
      render: (job) => (
        <button
          onClick={() => navigate(`/jobs/${job.uuid}`)}
          style={{ background: 'none', border: 'none', padding: 0, cursor: 'pointer', color: 'var(--digi-color-primary, #006991)', fontSize: '0.875rem', textAlign: 'left' }}
        >
          {job.name || job.plugin}
        </button>
      ),
    },
    {
      key: 'state',
      header: t('label.state', 'Status'),
      render: (job) => <StatusBadge state={job.state} />,
      width: '140px',
    },
    {
      key: 'progress',
      header: t('job.progress', 'Framsteg'),
      render: (job) => formatProgress(job),
      width: '160px',
    },
    {
      key: 'startDate',
      header: t('job.start', 'Start'),
      render: (job) => <span style={{ fontSize: '0.8rem' }}>{formatDate(job.startDate)}</span>,
      width: '160px',
    },
    {
      key: 'endDate',
      header: t('job.end', 'Slut'),
      render: (job) => <span style={{ fontSize: '0.8rem' }}>{formatDate(job.endDate)}</span>,
      width: '160px',
    },
  ]

  return (
    <div>
      <h1 style={{ marginBottom: '1.5rem' }}>{t('job.title', 'Jobb')}</h1>

      <DataTable
        columns={columns}
        rows={jobs}
        rowKey={(job) => job.uuid}
        isLoading={isLoading}
        error={error?.message ?? null}
        emptyMessage={t('job.none', 'Inga jobb hittades')}
        summary={
          !isLoading && !error ? (
            <>
              {totalCount} jobb totalt
              {hasActive && (
                <span style={{ marginLeft: '0.75rem', color: '#1a56db' }}>
                  ● Uppdateras automatiskt
                </span>
              )}
            </>
          ) : undefined
        }
      />

      <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
    </div>
  )
}

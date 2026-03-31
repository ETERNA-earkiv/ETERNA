import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { getJob, listJobReports, type JobReport } from '../api/jobs'
import { Breadcrumb } from '../components/common/Breadcrumb'
import { StatusBadge } from '../components/common/StatusBadge'
import { DataTable, type Column } from '../components/common/DataTable'
import { TabBar } from '../components/common/TabBar'
import { Pagination } from '../components/common/Pagination'
import { DigiTable } from '@designsystem-se/af-react'

type TabId = 'overview' | 'reports' | 'failed'

const TABS = [
  { id: 'overview' as TabId, label: 'Översikt' },
  { id: 'reports' as TabId, label: 'Rapporter' },
  { id: 'failed' as TabId, label: 'Misslyckade' },
]

function pluginShortName(plugin: string): string {
  const parts = plugin.split('.')
  return parts[parts.length - 1] ?? plugin
}

function ProgressBar({ value, max }: { value: number; max: number }) {
  const pct = max > 0 ? Math.round((value / max) * 100) : 0
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
      <div style={{ flex: 1, height: '6px', background: '#e0e0e0', borderRadius: '3px', overflow: 'hidden' }}>
        <div
          style={{
            height: '100%',
            width: `${pct}%`,
            background: 'var(--digi-color-primary, #006991)',
            borderRadius: '3px',
            transition: 'width 0.3s',
          }}
        />
      </div>
      <span style={{ fontSize: '0.8rem', color: '#555', minWidth: '30px' }}>{pct}%</span>
    </div>
  )
}

function ReportsTab({ jobId, justFailed }: { jobId: string; justFailed: boolean }) {
  const [page, setPage] = useState(0)
  const PAGE_SIZE = 20

  const { data, isLoading, error } = useQuery({
    queryKey: ['job-reports', jobId, justFailed, page],
    queryFn: () => listJobReports(jobId, justFailed, page * PAGE_SIZE, PAGE_SIZE),
  })

  const reports = data?.results ?? []
  const totalPages = Math.ceil((data?.totalCount ?? 0) / PAGE_SIZE)

  const columns: Column<JobReport>[] = [
    {
      key: 'source',
      header: 'Källobjekt',
      render: (r) => (
        <span style={{ fontSize: '0.85rem' }}>
          {r.sourceObjectLabel || r.sourceObjectId || '—'}
        </span>
      ),
    },
    {
      key: 'outcome',
      header: 'Resultatobjekt',
      render: (r) => (
        <span style={{ fontSize: '0.85rem' }}>
          {r.outcomeObjectLabel || r.outcomeObjectId || '—'}
        </span>
      ),
    },
    {
      key: 'state',
      header: 'Status',
      render: (r) => <StatusBadge state={r.pluginState} />,
      width: '100px',
    },
    {
      key: 'progress',
      header: 'Framsteg',
      render: (r) => <ProgressBar value={r.stepsCompleted} max={r.totalSteps} />,
      width: '150px',
    },
    {
      key: 'updated',
      header: 'Uppdaterad',
      render: (r) => r.dateUpdated ? new Date(r.dateUpdated).toLocaleString('sv-SE') : '—',
      width: '150px',
    },
  ]

  return (
    <>
      <DataTable
        columns={columns}
        rows={reports}
        rowKey={(r) => r.id}
        isLoading={isLoading}
        error={error?.message ?? null}
        emptyMessage={justFailed ? 'Inga misslyckade objekt' : 'Inga rapporter hittades'}
        summary={!isLoading && reports.length > 0 ? `${data?.totalCount ?? 0} totalt` : undefined}
      />
      <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
    </>
  )
}

export function JobDetail() {
  const { jobId } = useParams<{ jobId: string }>()
  const navigate = useNavigate()
  const [activeTab, setActiveTab] = useState<TabId>('overview')

  const { data: job, isLoading, error } = useQuery({
    queryKey: ['job', jobId],
    queryFn: () => getJob(jobId!),
    enabled: !!jobId,
    refetchInterval: (query) => {
      const state = query.state.data?.state
      return state === 'RUNNING' || state === 'CREATED' ? 5000 : false
    },
  })

  if (isLoading) return <DataTable columns={[]} rows={[]} rowKey={() => ''} isLoading />
  if (error || !job) {
    return (
      <div style={{ color: '#c5221f', padding: '2rem' }}>
        Kunde inte hämta jobb.{' '}
        <button
          onClick={() => navigate(-1)}
          style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--digi-color-primary, #006991)' }}
        >
          Tillbaka
        </button>
      </div>
    )
  }

  const stats = job.jobStats
  const fields = [
    { label: 'UUID', value: job.uuid },
    { label: 'Namn', value: job.name },
    { label: 'Plugin', value: pluginShortName(job.plugin) },
    { label: 'Användare', value: job.username },
    { label: 'Status', value: <StatusBadge state={job.state} /> },
    { label: 'Startad', value: job.startDate ? new Date(job.startDate).toLocaleString('sv-SE') : '—' },
    { label: 'Avslutad', value: job.endDate ? new Date(job.endDate).toLocaleString('sv-SE') : '—' },
    { label: 'Källobjekt totalt', value: stats.sourceObjectsCount },
    { label: 'Bearbetade', value: stats.processedSourceObjectsCount },
    { label: 'Misslyckade', value: stats.sourceObjectsProcessedWithFailure },
  ]

  return (
    <div>
      <Breadcrumb
        items={[
          { label: 'Jobb', to: '/jobs' },
          { label: job.name },
        ]}
      />

      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: '1.5rem' }}>
        <h1 style={{ fontSize: '1.5rem', margin: 0 }}>{job.name}</h1>
        <StatusBadge state={job.state} />
      </div>

      {stats.sourceObjectsCount > 0 && (
        <div style={{ marginBottom: '1.5rem' }}>
          <ProgressBar value={stats.processedSourceObjectsCount} max={stats.sourceObjectsCount} />
        </div>
      )}

      <TabBar tabs={TABS} activeTab={activeTab} onTabChange={(id) => setActiveTab(id as TabId)} />

      {activeTab === 'overview' && (
        <DigiTable>
          <table>
            <tbody>
              {fields.map(({ label, value }) => (
                <tr key={label}>
                  <th style={{ width: '200px', textAlign: 'left', fontWeight: 600, padding: '0.5rem' }}>
                    {label}
                  </th>
                  <td style={{ padding: '0.5rem' }}>{value ?? '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </DigiTable>
      )}
      {activeTab === 'reports' && <ReportsTab jobId={job.uuid} justFailed={false} />}
      {activeTab === 'failed' && <ReportsTab jobId={job.uuid} justFailed={true} />}
    </div>
  )
}

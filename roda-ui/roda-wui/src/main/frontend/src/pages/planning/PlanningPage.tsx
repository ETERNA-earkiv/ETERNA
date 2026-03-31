import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import {
  findRisks,
  findRepresentationInformation,
  findPreservationAgents,
  type IndexedRisk,
  type RepresentationInformation,
  type IndexedPreservationAgent,
  type SeverityLevel,
} from '../../api/planning'
import { DataTable, type Column } from '../../components/common/DataTable'
import { TabBar } from '../../components/common/TabBar'
import { Pagination } from '../../components/common/Pagination'

type TabId = 'risks' | 'repinfo' | 'agents'

const TABS = [
  { id: 'risks' as TabId, label: 'Risker' },
  { id: 'repinfo' as TabId, label: 'Representationsinformation' },
  { id: 'agents' as TabId, label: 'Bevarandeagenter' },
]

function severityBadge(level: SeverityLevel | null) {
  if (!level) return <span style={{ color: '#999' }}>—</span>
  const colors: Record<SeverityLevel, string> = {
    LOW: '#2d7a3a',
    MODERATE: '#e65c00',
    HIGH: '#c5221f',
    CRITICAL: '#7b0000',
  }
  const labels: Record<SeverityLevel, string> = {
    LOW: 'Låg',
    MODERATE: 'Måttlig',
    HIGH: 'Hög',
    CRITICAL: 'Kritisk',
  }
  return (
    <span
      style={{
        display: 'inline-block',
        padding: '2px 8px',
        borderRadius: '12px',
        background: colors[level] + '22',
        color: colors[level],
        fontWeight: 600,
        fontSize: '0.75rem',
      }}
    >
      {labels[level]}
    </span>
  )
}

function RisksTab() {
  const [page, setPage] = useState(0)
  const PAGE_SIZE = 20

  const { data, isLoading, error } = useQuery({
    queryKey: ['risks', page],
    queryFn: () => findRisks(page * PAGE_SIZE, PAGE_SIZE),
  })

  const risks = data?.results ?? []
  const totalPages = Math.ceil((data?.totalCount ?? 0) / PAGE_SIZE)

  const columns: Column<IndexedRisk>[] = [
    {
      key: 'name',
      header: 'Namn',
      render: (r) => (
        <Link to={`/planning/risks/${r.id}`} style={{ color: 'var(--digi-color-primary, #006991)' }}>
          {r.name}
        </Link>
      ),
    },
    {
      key: 'severity',
      header: 'Allvarlighet (pre)',
      render: (r) => severityBadge(r.preMitigationSeverityLevel),
      width: '150px',
    },
    {
      key: 'postSeverity',
      header: 'Allvarlighet (post)',
      render: (r) => severityBadge(r.postMitigationSeverityLevel),
      width: '150px',
    },
    {
      key: 'incidences',
      header: 'Incidenter',
      render: (r) => r.incidencesCount,
      width: '90px',
    },
    {
      key: 'identifiedOn',
      header: 'Identifierad',
      render: (r) => r.identifiedOn ? new Date(r.identifiedOn).toLocaleDateString('sv-SE') : '—',
      width: '110px',
    },
  ]

  return (
    <>
      <DataTable
        columns={columns}
        rows={risks}
        rowKey={(r) => r.id}
        isLoading={isLoading}
        error={error?.message ?? null}
        emptyMessage="Inga risker hittades"
        summary={!isLoading && risks.length > 0 ? `${data?.totalCount ?? 0} risker totalt` : undefined}
      />
      <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
    </>
  )
}

function RepInfoTab() {
  const [page, setPage] = useState(0)
  const PAGE_SIZE = 20

  const { data, isLoading, error } = useQuery({
    queryKey: ['rep-info', page],
    queryFn: () => findRepresentationInformation(page * PAGE_SIZE, PAGE_SIZE),
  })

  const items = data?.results ?? []
  const totalPages = Math.ceil((data?.totalCount ?? 0) / PAGE_SIZE)

  const columns: Column<RepresentationInformation>[] = [
    {
      key: 'name',
      header: 'Namn',
      render: (r) => (
        <Link to={`/planning/repinfo/${r.id}`} style={{ color: 'var(--digi-color-primary, #006991)' }}>
          {r.name}
        </Link>
      ),
    },
    {
      key: 'family',
      header: 'Familj',
      render: (r) => r.family || '—',
      width: '160px',
    },
    {
      key: 'tags',
      header: 'Taggar',
      render: (r) => r.tags?.join(', ') || '—',
    },
    {
      key: 'updatedOn',
      header: 'Ändrad',
      render: (r) => r.updatedOn ? new Date(r.updatedOn).toLocaleDateString('sv-SE') : '—',
      width: '110px',
    },
  ]

  return (
    <>
      <DataTable
        columns={columns}
        rows={items}
        rowKey={(r) => r.id}
        isLoading={isLoading}
        error={error?.message ?? null}
        emptyMessage="Ingen representationsinformation hittades"
      />
      <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
    </>
  )
}

function AgentsTab() {
  const [page, setPage] = useState(0)
  const PAGE_SIZE = 20

  const { data, isLoading, error } = useQuery({
    queryKey: ['preservation-agents', page],
    queryFn: () => findPreservationAgents(page * PAGE_SIZE, PAGE_SIZE),
  })

  const agents = data?.results ?? []
  const totalPages = Math.ceil((data?.totalCount ?? 0) / PAGE_SIZE)

  const columns: Column<IndexedPreservationAgent>[] = [
    {
      key: 'name',
      header: 'Namn',
      render: (a) => (
        <Link to={`/planning/agents/${a.id}`} style={{ color: 'var(--digi-color-primary, #006991)' }}>
          {a.name}
        </Link>
      ),
    },
    {
      key: 'type',
      header: 'Typ',
      render: (a) => a.type || '—',
      width: '130px',
    },
    {
      key: 'version',
      header: 'Version',
      render: (a) => a.version || '—',
      width: '100px',
    },
    {
      key: 'roles',
      header: 'Roller',
      render: (a) => a.roles?.join(', ') || '—',
    },
    {
      key: 'instance',
      header: 'Instans',
      render: (a) => a.instanceName || a.instanceId || '—',
      width: '140px',
    },
  ]

  return (
    <>
      <DataTable
        columns={columns}
        rows={agents}
        rowKey={(a) => a.id}
        isLoading={isLoading}
        error={error?.message ?? null}
        emptyMessage="Inga bevarandeagenter hittades"
      />
      <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
    </>
  )
}

export function PlanningPage() {
  const [activeTab, setActiveTab] = useState<TabId>('risks')

  return (
    <div>
      <h1 style={{ fontSize: '1.5rem', marginBottom: '1.5rem' }}>Planering</h1>

      <TabBar tabs={TABS} activeTab={activeTab} onTabChange={(id) => setActiveTab(id as TabId)} />

      {activeTab === 'risks' && <RisksTab />}
      {activeTab === 'repinfo' && <RepInfoTab />}
      {activeTab === 'agents' && <AgentsTab />}
    </div>
  )
}

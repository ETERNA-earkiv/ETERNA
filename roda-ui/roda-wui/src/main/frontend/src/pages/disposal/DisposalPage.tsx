import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import {
  listDisposalSchedules,
  listDisposalHolds,
  listDisposalRules,
  listDisposalConfirmations,
  type DisposalSchedule,
  type DisposalHold,
  type DisposalRule,
  type DisposalConfirmation,
} from '../../api/disposal'
import { DataTable, type Column } from '../../components/common/DataTable'
import { TabBar } from '../../components/common/TabBar'
import { StatusBadge } from '../../components/common/StatusBadge'
import { Pagination } from '../../components/common/Pagination'

type TabId = 'schedules' | 'holds' | 'rules' | 'confirmations'

const TABS = [
  { id: 'schedules' as TabId, label: 'Gallringsplaner' },
  { id: 'holds' as TabId, label: 'Gallringsstopp' },
  { id: 'rules' as TabId, label: 'Gallringsregler' },
  { id: 'confirmations' as TabId, label: 'Gallringsbekräftelser' },
]

function actionCodeLabel(code: string): string {
  switch (code) {
    case 'RETAIN_PERMANENTLY': return 'Bevaras'
    case 'REVIEW': return 'Omprövas'
    case 'DESTROY': return 'Gallras'
    default: return code
  }
}

function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
  return `${(bytes / (1024 * 1024 * 1024)).toFixed(1)} GB`
}

function retentionLabel(
  intervalCode: string,
  duration: number | null
): string {
  if (intervalCode === 'NO_RETENTION_PERIOD') return '—'
  const unit =
    intervalCode === 'DAYS' ? 'dag' :
    intervalCode === 'WEEKS' ? 'vecka' :
    intervalCode === 'MONTHS' ? 'månad' :
    intervalCode === 'YEARS' ? 'år' : intervalCode
  return `${duration ?? '?'} ${unit}${(duration ?? 0) !== 1 ? 'ar' : ''}`
}

function SchedulesTab() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['disposal-schedules'],
    queryFn: listDisposalSchedules,
  })

  const columns: Column<DisposalSchedule>[] = [
    {
      key: 'title',
      header: 'Titel',
      render: (s) => (
        <Link to={`/disposal/schedules/${s.id}`} style={{ color: 'var(--digi-color-primary, #006991)' }}>
          {s.title}
        </Link>
      ),
    },
    {
      key: 'action',
      header: 'Åtgärd',
      render: (s) => actionCodeLabel(s.actionCode),
      width: '120px',
    },
    {
      key: 'retention',
      header: 'Bevarandetid',
      render: (s) => retentionLabel(s.retentionPeriodIntervalCode, s.retentionPeriodDuration),
      width: '130px',
    },
    {
      key: 'state',
      header: 'Status',
      render: (s) => <StatusBadge state={s.state} />,
      width: '90px',
    },
    {
      key: 'counter',
      header: 'Arkivobjekt',
      render: (s) => s.apiCounter,
      width: '100px',
    },
  ]

  return (
    <DataTable
      columns={columns}
      rows={data ?? []}
      rowKey={(s) => s.id}
      isLoading={isLoading}
      error={error?.message ?? null}
      emptyMessage="Inga gallringsplaner hittades"
    />
  )
}

function HoldsTab() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['disposal-holds'],
    queryFn: listDisposalHolds,
  })

  const columns: Column<DisposalHold>[] = [
    {
      key: 'title',
      header: 'Titel',
      render: (h) => (
        <Link to={`/disposal/holds/${h.id}`} style={{ color: 'var(--digi-color-primary, #006991)' }}>
          {h.title}
        </Link>
      ),
    },
    {
      key: 'state',
      header: 'Status',
      render: (h) => <StatusBadge state={h.state} />,
      width: '90px',
    },
    {
      key: 'counter',
      header: 'Arkivobjekt',
      render: (h) => h.aipCounter,
      width: '100px',
    },
    {
      key: 'originatedOn',
      header: 'Beslutsdatum',
      render: (h) => h.originatedOn ? new Date(h.originatedOn).toLocaleDateString('sv-SE') : '—',
      width: '130px',
    },
    {
      key: 'liftedOn',
      header: 'Hävt',
      render: (h) => h.liftedOn ? new Date(h.liftedOn).toLocaleDateString('sv-SE') : '—',
      width: '110px',
    },
  ]

  return (
    <DataTable
      columns={columns}
      rows={data ?? []}
      rowKey={(h) => h.id}
      isLoading={isLoading}
      error={error?.message ?? null}
      emptyMessage="Inga gallringsstopp hittades"
    />
  )
}

function RulesTab() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['disposal-rules'],
    queryFn: listDisposalRules,
  })

  const columns: Column<DisposalRule>[] = [
    {
      key: 'order',
      header: '#',
      render: (r) => r.order,
      width: '50px',
    },
    {
      key: 'title',
      header: 'Titel',
      render: (r) => (
        <Link to={`/disposal/rules/${r.id}`} style={{ color: 'var(--digi-color-primary, #006991)' }}>
          {r.title}
        </Link>
      ),
    },
    {
      key: 'type',
      header: 'Typ',
      render: (r) => r.type === 'IS_CHILD_OF' ? 'Är barn till' : 'Metadatafält',
      width: '130px',
    },
    {
      key: 'schedule',
      header: 'Gallringsplan',
      render: (r) => r.disposalScheduleName ? (
        <Link
          to={`/disposal/schedules/${r.disposalScheduleId}`}
          style={{ color: 'var(--digi-color-primary, #006991)', fontSize: '0.85rem' }}
        >
          {r.disposalScheduleName}
        </Link>
      ) : '—',
    },
  ]

  return (
    <DataTable
      columns={columns}
      rows={data ?? []}
      rowKey={(r) => r.id}
      isLoading={isLoading}
      error={error?.message ?? null}
      emptyMessage="Inga gallringsregler hittades"
    />
  )
}

function ConfirmationsTab() {
  const [page, setPage] = useState(0)
  const PAGE_SIZE = 20

  const { data, isLoading, error } = useQuery({
    queryKey: ['disposal-confirmations', page],
    queryFn: () => listDisposalConfirmations(page * PAGE_SIZE, PAGE_SIZE),
  })

  const confirmations = data?.results ?? []
  const totalPages = Math.ceil((data?.totalCount ?? 0) / PAGE_SIZE)

  const columns: Column<DisposalConfirmation>[] = [
    {
      key: 'title',
      header: 'Titel',
      render: (c) => (
        <Link to={`/disposal/confirmations/${c.id}`} style={{ color: 'var(--digi-color-primary, #006991)' }}>
          {c.title}
        </Link>
      ),
    },
    {
      key: 'state',
      header: 'Status',
      render: (c) => <StatusBadge state={c.state} />,
      width: '140px',
    },
    {
      key: 'aips',
      header: 'Arkivobjekt',
      render: (c) => c.numberOfAIPs,
      width: '100px',
    },
    {
      key: 'size',
      header: 'Storlek',
      render: (c) => formatSize(c.size),
      width: '100px',
    },
    {
      key: 'createdOn',
      header: 'Skapad',
      render: (c) => c.createdOn ? new Date(c.createdOn).toLocaleDateString('sv-SE') : '—',
      width: '110px',
    },
    {
      key: 'executedOn',
      header: 'Utförd',
      render: (c) => c.executedOn ? new Date(c.executedOn).toLocaleDateString('sv-SE') : '—',
      width: '110px',
    },
  ]

  return (
    <>
      <DataTable
        columns={columns}
        rows={confirmations}
        rowKey={(c) => c.id}
        isLoading={isLoading}
        error={error?.message ?? null}
        emptyMessage="Inga gallringsbekräftelser hittades"
      />
      <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
    </>
  )
}

export function DisposalPage() {
  const [activeTab, setActiveTab] = useState<TabId>('schedules')

  return (
    <div>
      <h1 style={{ fontSize: '1.5rem', marginBottom: '1.5rem' }}>Gallring</h1>

      <TabBar tabs={TABS} activeTab={activeTab} onTabChange={(id) => setActiveTab(id as TabId)} />

      {activeTab === 'schedules' && <SchedulesTab />}
      {activeTab === 'holds' && <HoldsTab />}
      {activeTab === 'rules' && <RulesTab />}
      {activeTab === 'confirmations' && <ConfirmationsTab />}
    </div>
  )
}

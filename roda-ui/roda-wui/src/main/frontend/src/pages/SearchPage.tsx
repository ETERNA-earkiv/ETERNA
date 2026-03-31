import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { api } from '../api/client'
import { DataTable, type Column } from '../components/common/DataTable'
import { StatusBadge } from '../components/common/StatusBadge'
import { TabBar } from '../components/common/TabBar'
import { Pagination } from '../components/common/Pagination'
import { DigiFormInputSearch } from '@designsystem-se/af-react'
import type { IndexResult } from '../types'
import type { IndexedAIP } from '../api/aips'
import type { IndexedFile } from '../api/files'
import type { IndexedPreservationEvent } from '../api/preservation-events'

type TabId = 'aips' | 'files' | 'events'

const TABS = [
  { id: 'aips' as TabId, label: 'Arkivobjekt' },
  { id: 'files' as TabId, label: 'Filer' },
  { id: 'events' as TabId, label: 'Bevarandehändelser' },
]

const PAGE_SIZE = 20

function fulltextFilter(q: string) {
  return q
    ? [{ type: 'SimpleFilterParameter', name: 'fulltext', value: q }]
    : []
}

function buildFindBody(q: string, offset: number, sort: { name: string; descending: boolean }[]) {
  return {
    filter: { filterParameters: fulltextFilter(q) },
    sublist: { firstElementIndex: offset, maximumElementCount: PAGE_SIZE },
    sorter: { parameters: sort },
    facets: { parameters: {} },
  }
}

function AIPResults({ q }: { q: string }) {
  const [page, setPage] = useState(0)
  const navigate = useNavigate()

  const { data, isLoading, error } = useQuery({
    queryKey: ['search-aips', q, page],
    queryFn: () =>
      api.post<IndexResult<IndexedAIP>>('/aips/find', buildFindBody(q, page * PAGE_SIZE, [{ name: 'title', descending: false }])),
    enabled: q.length > 0,
  })

  const rows = data?.results ?? []
  const totalPages = Math.ceil((data?.totalCount ?? 0) / PAGE_SIZE)

  const columns: Column<IndexedAIP>[] = [
    {
      key: 'title',
      header: 'Titel',
      render: (a) => (
        <button
          onClick={() => navigate(`/browse/${a.id}`)}
          style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--digi-color-primary, #006991)', textAlign: 'left', padding: 0 }}
        >
          {a.title || a.id}
        </button>
      ),
    },
    {
      key: 'level',
      header: 'Nivå',
      render: (a) => a.level || '—',
      width: '100px',
    },
    {
      key: 'state',
      header: 'Status',
      render: (a) => <StatusBadge state={a.state} />,
      width: '100px',
    },
    {
      key: 'dateCreated',
      header: 'Skapad',
      render: (a) => a.dateCreated ? new Date(a.dateCreated).toLocaleDateString('sv-SE') : '—',
      width: '110px',
    },
  ]

  return (
    <>
      <DataTable
        columns={columns}
        rows={rows}
        rowKey={(a) => a.id}
        isLoading={isLoading}
        error={error?.message ?? null}
        emptyMessage={q ? 'Inga arkivobjekt matchade sökningen' : 'Ange ett sökord ovan'}
        summary={data?.totalCount != null ? `${data.totalCount} träffar` : undefined}
      />
      <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
    </>
  )
}

function FileResults({ q }: { q: string }) {
  const [page, setPage] = useState(0)
  const navigate = useNavigate()

  const { data, isLoading, error } = useQuery({
    queryKey: ['search-files', q, page],
    queryFn: () =>
      api.post<IndexResult<IndexedFile>>('/files/find', buildFindBody(q, page * PAGE_SIZE, [{ name: 'fileId', descending: false }])),
    enabled: q.length > 0,
  })

  const rows = data?.results ?? []
  const totalPages = Math.ceil((data?.totalCount ?? 0) / PAGE_SIZE)

  const columns: Column<IndexedFile>[] = [
    {
      key: 'fileId',
      header: 'Filnamn',
      render: (f) => (
        <button
          onClick={() => navigate(`/browse/${f.aipId}/representation/${f.representationId}/file/${f.uuid}`)}
          style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--digi-color-primary, #006991)', textAlign: 'left', padding: 0 }}
        >
          {f.fileId}
        </button>
      ),
    },
    {
      key: 'format',
      header: 'Format',
      render: (f) => f.fileFormat || '—',
      width: '160px',
    },
    {
      key: 'size',
      header: 'Storlek',
      render: (f) => f.size ? formatSize(f.size) : '—',
      width: '90px',
    },
    {
      key: 'aipId',
      header: 'AIP',
      render: (f) => (
        <button
          onClick={() => navigate(`/browse/${f.aipId}`)}
          style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--digi-color-primary, #006991)', fontSize: '0.8rem', fontFamily: 'monospace', padding: 0 }}
        >
          {f.aipId}
        </button>
      ),
    },
  ]

  return (
    <>
      <DataTable
        columns={columns}
        rows={rows}
        rowKey={(f) => f.uuid}
        isLoading={isLoading}
        error={error?.message ?? null}
        emptyMessage={q ? 'Inga filer matchade sökningen' : 'Ange ett sökord ovan'}
        summary={data?.totalCount != null ? `${data.totalCount} träffar` : undefined}
      />
      <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
    </>
  )
}

function EventResults({ q }: { q: string }) {
  const [page, setPage] = useState(0)

  const { data, isLoading, error } = useQuery({
    queryKey: ['search-events', q, page],
    queryFn: () =>
      api.post<IndexResult<IndexedPreservationEvent>>('/preservation/events/find', buildFindBody(q, page * PAGE_SIZE, [{ name: 'eventDateTime', descending: true }])),
    enabled: q.length > 0,
  })

  const rows = data?.results ?? []
  const totalPages = Math.ceil((data?.totalCount ?? 0) / PAGE_SIZE)

  const columns: Column<IndexedPreservationEvent>[] = [
    {
      key: 'datetime',
      header: 'Tidpunkt',
      render: (e) => <span style={{ fontSize: '0.8rem' }}>{new Date(e.eventDateTime).toLocaleString('sv-SE')}</span>,
      width: '160px',
    },
    {
      key: 'type',
      header: 'Händelsetyp',
      render: (e) => e.eventType,
    },
    {
      key: 'outcome',
      header: 'Utfall',
      render: (e) => <StatusBadge state={e.eventOutcome} />,
      width: '100px',
    },
    {
      key: 'aip',
      header: 'AIP',
      render: (e) => <span style={{ fontSize: '0.8rem', fontFamily: 'monospace' }}>{e.aipID}</span>,
    },
  ]

  return (
    <>
      <DataTable
        columns={columns}
        rows={rows}
        rowKey={(e) => e.id}
        isLoading={isLoading}
        error={error?.message ?? null}
        emptyMessage={q ? 'Inga händelser matchade sökningen' : 'Ange ett sökord ovan'}
        summary={data?.totalCount != null ? `${data.totalCount} träffar` : undefined}
      />
      <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
    </>
  )
}

function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

type InputSearchEl = HTMLElement & { afValue: string }

export function SearchPage() {
  const [activeTab, setActiveTab] = useState<TabId>('aips')
  const [input, setInput] = useState('')
  const [committed, setCommitted] = useState('')

  function submit(value: string) {
    setCommitted(value.trim())
  }

  return (
    <div>
      <h1 style={{ fontSize: '1.5rem', marginBottom: '1.5rem' }}>Sök</h1>

      <div style={{ maxWidth: '600px', marginBottom: '1.5rem' }}>
        <DigiFormInputSearch
          afLabel="Sök i arkivet"
          afId="global-search"
          afValue={input}
          onAfOnInput={(e) => setInput((e.target as InputSearchEl).afValue)}
          onAfOnSubmitSearch={(e) => submit(e.detail as string || input)}
        />
      </div>

      <TabBar tabs={TABS} activeTab={activeTab} onTabChange={(id) => setActiveTab(id as TabId)} />

      {activeTab === 'aips' && <AIPResults q={committed} />}
      {activeTab === 'files' && <FileResults q={committed} />}
      {activeTab === 'events' && <EventResults q={committed} />}
    </div>
  )
}

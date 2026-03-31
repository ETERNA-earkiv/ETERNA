import { useState } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { getRepresentation, type IndexedRepresentation } from '../../api/representations'
import { searchFiles, fileDownloadUrl, type IndexedFile } from '../../api/files'
import { findPreservationEvents, type IndexedPreservationEvent } from '../../api/preservation-events'
import { DataTable, type Column } from '../../components/common/DataTable'
import { TabBar } from '../../components/common/TabBar'
import { Breadcrumb } from '../../components/common/Breadcrumb'
import { StatusBadge } from '../../components/common/StatusBadge'
import { Pagination } from '../../components/common/Pagination'
import { DigiTable } from '@designsystem-se/af-react'

type TabId = 'overview' | 'files' | 'events'

const TABS = [
  { id: 'overview' as TabId, label: 'Översikt' },
  { id: 'files' as TabId, label: 'Filer' },
  { id: 'events' as TabId, label: 'Händelser' },
]

function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

function OverviewTab({ rep }: { rep: IndexedRepresentation }) {
  const fields = [
    { label: 'UUID', value: rep.uuid },
    { label: 'ID', value: rep.id },
    { label: 'AIP-ID', value: rep.aipId },
    { label: 'Typ', value: rep.type },
    { label: 'Original', value: rep.original ? 'Ja' : 'Nej' },
    { label: 'Datafiler', value: rep.numberOfDataFiles },
    { label: 'Dokumentationsfiler', value: rep.numberOfDocumentationFiles },
    { label: 'Schemafiler', value: rep.numberOfSchemasFiles },
    { label: 'Storlek', value: rep.sizeInBytes ? formatSize(rep.sizeInBytes) : '—' },
  ]

  return (
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
  )
}

function FilesTab({ representationUuid, aipId, repId }: { representationUuid: string; aipId: string; repId: string }) {
  const [page, setPage] = useState(0)
  const PAGE_SIZE = 50

  const { data, isLoading, error } = useQuery({
    queryKey: ['files', representationUuid, page],
    queryFn: () => searchFiles(representationUuid, page * PAGE_SIZE, PAGE_SIZE),
  })

  const files = data?.results ?? []
  const totalPages = Math.ceil((data?.totalCount ?? 0) / PAGE_SIZE)

  const columns: Column<IndexedFile>[] = [
    {
      key: 'fileId',
      header: 'Filnamn',
      render: (f) => (
        <Link
          to={`/browse/${aipId}/representation/${repId}/file/${f.uuid}`}
          style={{ color: 'var(--digi-color-primary, #006991)' }}
        >
          {f.fileId}
        </Link>
      ),
    },
    {
      key: 'path',
      header: 'Sökväg',
      render: (f) => f.path?.join('/') || '—',
    },
    {
      key: 'size',
      header: 'Storlek',
      render: (f) => (f.size ? formatSize(f.size) : '—'),
      width: '100px',
    },
    {
      key: 'format',
      header: 'Format',
      render: (f) => f.fileFormat || '—',
      width: '160px',
    },
    {
      key: 'download',
      header: '',
      render: (f) => (
        <a
          href={fileDownloadUrl(f.uuid)}
          download
          style={{ color: 'var(--digi-color-primary, #006991)', fontSize: '0.8rem' }}
        >
          Ladda ner
        </a>
      ),
      width: '100px',
    },
  ]

  return (
    <>
      <DataTable
        columns={columns}
        rows={files}
        rowKey={(f) => f.uuid}
        isLoading={isLoading}
        error={error?.message ?? null}
        emptyMessage="Inga filer hittades"
      />
      <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
    </>
  )
}

function EventsTab({ aipId, representationId }: { aipId: string; representationId: string }) {
  const { data, isLoading, error } = useQuery({
    queryKey: ['preservation-events', aipId, representationId],
    queryFn: () => findPreservationEvents({ aipId, representationId }),
  })

  const events = data?.results ?? []

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
      key: 'detail',
      header: 'Detalj',
      render: (e) => (
        <span style={{ fontSize: '0.8rem', color: '#555' }}>
          {e.eventOutcomeDetailNote || e.eventDetail || '—'}
        </span>
      ),
    },
  ]

  return (
    <DataTable
      columns={columns}
      rows={events}
      rowKey={(e) => e.id}
      isLoading={isLoading}
      error={error?.message ?? null}
      emptyMessage="Inga händelser hittades"
    />
  )
}

export function RepresentationDetail() {
  const { aipId, repId } = useParams<{ aipId: string; repId: string }>()
  const navigate = useNavigate()
  const [activeTab, setActiveTab] = useState<TabId>('overview')

  const { data: rep, isLoading, error } = useQuery({
    queryKey: ['representation', repId],
    queryFn: () => getRepresentation(repId!),
    enabled: !!repId,
  })

  if (isLoading) return <DataTable columns={[]} rows={[]} rowKey={() => ''} isLoading />
  if (error || !rep) {
    return (
      <div style={{ color: '#c5221f', padding: '2rem' }}>
        Kunde inte hämta representation.{' '}
        <button onClick={() => navigate(-1)} style={{ color: 'var(--digi-color-primary, #006991)', background: 'none', border: 'none', cursor: 'pointer' }}>
          Tillbaka
        </button>
      </div>
    )
  }

  return (
    <div>
      <Breadcrumb
        items={[
          { label: 'Arkivobjekt', to: '/browse' },
          { label: aipId ?? '…', to: `/browse/${aipId}` },
          { label: rep.type || rep.id },
        ]}
      />

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1.5rem' }}>
        <h1 style={{ fontSize: '1.5rem', margin: 0 }}>
          {rep.original ? 'Original: ' : ''}{rep.type || rep.id}
        </h1>
      </div>

      <TabBar tabs={TABS} activeTab={activeTab} onTabChange={(id) => setActiveTab(id as TabId)} />

      {activeTab === 'overview' && <OverviewTab rep={rep} />}
      {activeTab === 'files' && (
        <FilesTab representationUuid={rep.uuid} aipId={aipId!} repId={repId!} />
      )}
      {activeTab === 'events' && (
        <EventsTab aipId={aipId!} representationId={repId!} />
      )}
    </div>
  )
}

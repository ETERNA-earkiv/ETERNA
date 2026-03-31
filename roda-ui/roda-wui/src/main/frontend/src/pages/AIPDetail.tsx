import { useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { getAIP, getMetadataList, getMetadataHtml, type IndexedAIP, type DescriptiveMetadataInfo } from '../api/aips'
import { api } from '../api/client'
import type { IndexResult } from '../types'
import { Pagination } from '../components/common/Pagination'
import { findRepresentations, type IndexedRepresentation } from '../api/representations'
import { findDIPs, type IndexedDIP } from '../api/dips'
import { findPreservationEvents, type IndexedPreservationEvent } from '../api/preservation-events'
import { searchFiles, fileDownloadUrl, type IndexedFile } from '../api/files'
import { TabBar } from '../components/common/TabBar'
import { Breadcrumb } from '../components/common/Breadcrumb'
import { StatusBadge } from '../components/common/StatusBadge'
import { DataTable, type Column } from '../components/common/DataTable'
import { DigiTable, DigiLoaderSpinner, DigiNotificationAlert } from '@designsystem-se/af-react'

type TabId = 'overview' | 'metadata' | 'files' | 'events' | 'dips' | 'children'

const TABS = [
  { id: 'overview' as TabId, label: 'Översikt' },
  { id: 'metadata' as TabId, label: 'Metadata' },
  { id: 'files' as TabId, label: 'Filer' },
  { id: 'events' as TabId, label: 'Händelser' },
  { id: 'dips' as TabId, label: 'DIPs' },
  { id: 'children' as TabId, label: 'Barn' },
]

function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

function OverviewTab({ aip, representations }: { aip: IndexedAIP; representations: IndexedRepresentation[] }) {
  const aipId = aip.id

  const fields = [
    { label: 'ID', value: aip.id },
    { label: 'Titel', value: aip.title },
    { label: 'Beskrivning', value: aip.description },
    { label: 'Nivå', value: aip.level },
    {
      label: 'Förälder',
      value: aip.parentID ? (
        <Link to={`/browse/${aip.parentID}`} style={{ color: 'var(--digi-color-primary, #006991)', fontFamily: 'monospace', fontSize: '0.85rem' }}>
          {aip.parentID}
        </Link>
      ) : null,
    },
    { label: 'Skapad', value: aip.dateCreated ? new Date(aip.dateCreated).toLocaleString('sv-SE') : null },
    { label: 'Ändrad', value: aip.dateModified ? new Date(aip.dateModified).toLocaleString('sv-SE') : null },
    { label: 'Ingest-jobb', value: aip.ingestJobId ? (
      <Link to={`/jobs/${aip.ingestJobId}`} style={{ color: 'var(--digi-color-primary, #006991)', fontFamily: 'monospace', fontSize: '0.85rem' }}>
        {aip.ingestJobId}
      </Link>
    ) : null },
    { label: 'Spärrat', value: aip.onHold ? 'Ja' : 'Nej' },
    { label: 'Submission-filer', value: aip.numberOfSubmissionFiles },
    { label: 'Dokumentationsfiler', value: aip.numberOfDocumentationFiles },
    {
      label: 'Gallringsplan',
      value: aip.disposalScheduleId ? (
        <Link to={`/disposal/schedules/${aip.disposalScheduleId}`} style={{ color: 'var(--digi-color-primary, #006991)' }}>
          {aip.disposalScheduleName || aip.disposalScheduleId}
        </Link>
      ) : null,
    },
    { label: 'Gallringsåtgärd', value: aip.disposalAction },
  ]

  const repColumns: Column<IndexedRepresentation>[] = [
    {
      key: 'id',
      header: 'ID',
      render: (r) => (
        <Link
          to={`/browse/${aipId}/representation/${r.uuid}`}
          style={{ color: 'var(--digi-color-primary, #006991)', fontFamily: 'monospace', fontSize: '0.8rem' }}
        >
          {r.id}
        </Link>
      ),
    },
    {
      key: 'type',
      header: 'Typ',
      render: (r) => r.type || '—',
      width: '140px',
    },
    {
      key: 'original',
      header: 'Original',
      render: (r) => (
        <span style={{ color: r.original ? '#2d7a3a' : '#555' }}>
          {r.original ? 'Ja' : 'Nej'}
        </span>
      ),
      width: '80px',
    },
    {
      key: 'files',
      header: 'Filer',
      render: (r) => r.numberOfDataFiles,
      width: '70px',
    },
    {
      key: 'size',
      header: 'Storlek',
      render: (r) => (r.sizeInBytes ? formatSize(r.sizeInBytes) : '—'),
      width: '100px',
    },
  ]

  return (
    <>
      <section style={{ marginBottom: '2rem' }}>
        <h2 style={{ fontSize: '1.125rem', marginBottom: '0.75rem' }}>Egenskaper</h2>
        <DigiTable>
          <table>
            <tbody>
              {fields.map(({ label, value }) =>
                value !== null && value !== undefined ? (
                  <tr key={label}>
                    <th style={{ width: '40%', textAlign: 'left', padding: '0.4rem' }}>{label}</th>
                    <td style={{ padding: '0.4rem' }}>{String(value)}</td>
                  </tr>
                ) : null
              )}
            </tbody>
          </table>
        </DigiTable>
      </section>

      {representations.length > 0 && (
        <section>
          <h2 style={{ fontSize: '1.125rem', marginBottom: '0.75rem' }}>
            Representationer ({representations.length})
          </h2>
          <DataTable
            columns={repColumns}
            rows={representations}
            rowKey={(r) => r.uuid}
            emptyMessage="Inga representationer"
          />
        </section>
      )}
    </>
  )
}

function MetadataTab({ aipId }: { aipId: string }) {
  const [selectedId, setSelectedId] = useState<string | null>(null)

  const { data: metadataListRes, isLoading, error } = useQuery({
    queryKey: ['metadata-list', aipId],
    queryFn: async () => {
      const res = await getMetadataList(aipId)
      return res.metadataInfos ?? []
    },
  })

  const metadataList: DescriptiveMetadataInfo[] = metadataListRes ?? []
  const activeId = selectedId ?? metadataList[0]?.id ?? null

  const { data: html, isLoading: htmlLoading } = useQuery({
    queryKey: ['metadata-html', aipId, activeId],
    queryFn: () => getMetadataHtml(aipId, activeId!),
    enabled: !!activeId,
  })

  if (isLoading) return <div style={{ textAlign: 'center', padding: '2rem' }}><DigiLoaderSpinner /></div>
  if (error) return <DigiNotificationAlert afVariation="danger" afHeading="Fel">Kunde inte hämta metadata.</DigiNotificationAlert>
  if (metadataList.length === 0) return <p style={{ color: '#666' }}>Ingen beskrivande metadata hittades.</p>

  return (
    <div>
      {metadataList.length > 1 && (
        <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1rem', flexWrap: 'wrap' }}>
          {metadataList.map((m) => (
            <button
              key={m.id}
              onClick={() => setSelectedId(m.id)}
              style={{
                padding: '0.375rem 0.875rem',
                border: '1px solid',
                borderColor: activeId === m.id ? 'var(--digi-color-primary, #006991)' : '#ccc',
                borderRadius: '4px',
                background: activeId === m.id ? 'var(--digi-color-primary, #006991)' : 'white',
                color: activeId === m.id ? 'white' : '#333',
                cursor: 'pointer',
                fontSize: '0.875rem',
              }}
            >
              {m.type || m.id}
            </button>
          ))}
        </div>
      )}

      {activeId && (
        <div style={{ marginBottom: '0.75rem', fontSize: '0.875rem' }}>
          <a
            href={`/api/v2/aips/${aipId}/metadata/descriptive/${activeId}/download`}
            style={{ color: 'var(--digi-color-primary, #006991)' }}
          >
            Ladda ned XML
          </a>
        </div>
      )}

      {htmlLoading && <div style={{ textAlign: 'center', padding: '2rem' }}><DigiLoaderSpinner /></div>}

      {html && !htmlLoading && (
        <iframe
          srcDoc={html}
          sandbox="allow-same-origin"
          style={{ width: '100%', border: '1px solid #e0e0e0', borderRadius: '4px', minHeight: '400px' }}
          title="Metadata"
        />
      )}
    </div>
  )
}

function FilesTab({ aipId, representations }: { aipId: string; representations: IndexedRepresentation[] }) {
  const [expandedRep, setExpandedRep] = useState<string | null>(null)

  const { data: filesData } = useQuery({
    queryKey: ['files', expandedRep],
    queryFn: () => searchFiles(expandedRep!),
    enabled: !!expandedRep,
  })

  function toggleRep(repId: string) {
    setExpandedRep((prev) => (prev === repId ? null : repId))
  }

  if (representations.length === 0) return <p style={{ color: '#666' }}>Inga representationer hittades.</p>

  return (
    <div>
      {representations.map((rep) => (
        <div key={rep.id} style={{ marginBottom: '1rem', border: '1px solid #e0e0e0', borderRadius: '4px' }}>
          <div
            style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0.75rem 1rem', background: '#f8f8f8', cursor: 'pointer' }}
            onClick={() => toggleRep(rep.uuid)}
          >
            <div>
              <Link
                to={`/browse/${aipId}/representation/${rep.uuid}`}
                style={{ fontWeight: 600, fontSize: '0.875rem', color: 'var(--digi-color-primary, #006991)' }}
                onClick={(e) => e.stopPropagation()}
              >
                {rep.type || rep.id}
              </Link>
              {rep.original && <span style={{ marginLeft: '0.5rem', fontSize: '0.8rem', color: '#2d7a3a' }}>(Original)</span>}
              <span style={{ marginLeft: '0.75rem', fontSize: '0.8rem', color: '#666' }}>{rep.numberOfDataFiles} filer</span>
            </div>
            <span style={{ color: '#666', fontSize: '0.875rem' }}>{expandedRep === rep.uuid ? '▲' : '▼'}</span>
          </div>

          {expandedRep === rep.uuid && (
            <div style={{ padding: '0.75rem 1rem' }}>
              {!filesData ? (
                <div style={{ textAlign: 'center', padding: '1rem' }}><DigiLoaderSpinner /></div>
              ) : (filesData?.results ?? []).length === 0 ? (
                <p style={{ color: '#666', fontSize: '0.875rem' }}>Inga filer hittades.</p>
              ) : (
                <DigiTable>
                  <table>
                    <thead>
                      <tr><th>Filnamn</th><th>Sökväg</th><th>Storlek</th><th>Format</th><th></th></tr>
                    </thead>
                    <tbody>
                      {(filesData?.results ?? []).map((file: IndexedFile) => (
                        <tr key={file.uuid}>
                          <td style={{ fontSize: '0.875rem' }}>
                            <Link
                              to={`/browse/${aipId}/representation/${rep.uuid}/file/${file.uuid}`}
                              style={{ color: 'var(--digi-color-primary, #006991)' }}
                            >
                              {file.fileId}
                            </Link>
                          </td>
                          <td style={{ fontSize: '0.8rem', color: '#666' }}>{file.path?.join('/') || '—'}</td>
                          <td style={{ fontSize: '0.8rem' }}>{formatSize(file.size)}</td>
                          <td style={{ fontSize: '0.8rem' }}>{file.fileFormat || '—'}</td>
                          <td><a href={fileDownloadUrl(file.uuid)} style={{ fontSize: '0.8rem', color: 'var(--digi-color-primary, #006991)' }}>Ladda ned</a></td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </DigiTable>
              )}
            </div>
          )}
        </div>
      ))}
    </div>
  )
}

function EventsTab({ aipId }: { aipId: string }) {
  const { data, isLoading, error } = useQuery({
    queryKey: ['preservation-events', aipId],
    queryFn: () => findPreservationEvents({ aipId }, 0, 50),
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
      width: '120px',
    },
    {
      key: 'detail',
      header: 'Detalj',
      render: (e) => <span style={{ fontSize: '0.8rem', color: '#555' }}>{e.eventOutcomeDetailNote || '—'}</span>,
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
      summary={!isLoading && events.length > 0 ? `${data?.totalCount ?? 0} händelser totalt` : undefined}
    />
  )
}

function DIPsTab({ aipId }: { aipId: string }) {
  const { data, isLoading, error } = useQuery({
    queryKey: ['dips', aipId],
    queryFn: () => findDIPs(aipId),
  })

  const dips = data?.results ?? []

  const columns: Column<IndexedDIP>[] = [
    {
      key: 'title',
      header: 'Titel',
      render: (d) => (
        <Link to={`/browse/dip/${d.uuid}`} style={{ color: 'var(--digi-color-primary, #006991)' }}>
          {d.title || d.uuid}
        </Link>
      ),
    },
    {
      key: 'type',
      header: 'Typ',
      render: (d) => d.type || '—',
      width: '120px',
    },
    {
      key: 'created',
      header: 'Skapad',
      render: (d) => (d.dateCreated ? new Date(d.dateCreated).toLocaleDateString('sv-SE') : '—'),
      width: '120px',
    },
  ]

  return (
    <DataTable
      columns={columns}
      rows={dips}
      rowKey={(d) => d.uuid}
      isLoading={isLoading}
      error={error?.message ?? null}
      emptyMessage="Inga DIPs hittades"
    />
  )
}

function ChildrenTab({ aipId }: { aipId: string }) {
  const PAGE_SIZE = 20
  const [page, setPage] = useState(0)

  const { data, isLoading, error } = useQuery({
    queryKey: ['aip-children', aipId, page],
    queryFn: () =>
      api.post<IndexResult<{ id: string; title: string; level: string; state: string }>>('/aips/find', {
        filter: {
          filterParameters: [
            { type: 'SimpleFilterParameter', name: 'parentID', value: aipId },
          ],
        },
        sublist: { firstElementIndex: page * PAGE_SIZE, maximumElementCount: PAGE_SIZE },
        sorter: { parameters: [{ name: 'title', descending: false }] },
        facets: { parameters: {} },
      }),
  })

  const children = data?.results ?? []
  const totalCount = data?.totalCount ?? 0
  const totalPages = Math.ceil(totalCount / PAGE_SIZE)

  const columns: Column<{ id: string; title: string; level: string; state: string }>[] = [
    {
      key: 'title',
      header: 'Titel',
      render: (child) => (
        <Link
          to={`/browse/${child.id}`}
          style={{ color: 'var(--digi-color-primary, #006991)' }}
        >
          {child.title || child.id}
        </Link>
      ),
    },
    { key: 'level', header: 'Nivå', width: '120px', render: (c) => c.level },
    { key: 'state', header: 'Status', width: '120px', render: (c) => <StatusBadge state={c.state} /> },
  ]

  return (
    <>
      <DataTable
        columns={columns}
        rows={children}
        rowKey={(c) => c.id}
        isLoading={isLoading}
        error={error?.message ?? null}
        emptyMessage="Inga underordnade arkivobjekt hittades"
      />
      <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
    </>
  )
}

export function AIPDetail() {
  const { id } = useParams<{ id: string }>()
  const [activeTab, setActiveTab] = useState<TabId>('overview')

  const { data: aip, isLoading: aipLoading, error: aipError } = useQuery({
    queryKey: ['aip', id],
    queryFn: () => getAIP(id!),
    enabled: !!id,
  })

  const { data: repsResult } = useQuery({
    queryKey: ['representations', id],
    queryFn: () => findRepresentations(id!),
    enabled: !!id,
  })

  const representations: IndexedRepresentation[] = repsResult?.results ?? []

  if (aipLoading) {
    return <div style={{ textAlign: 'center', padding: '4rem' }}><DigiLoaderSpinner /></div>
  }

  if (aipError || !aip) {
    return (
      <DigiNotificationAlert afVariation="danger" afHeading="Fel">
        {aipError?.message || 'Arkivobjektet hittades inte.'}
      </DigiNotificationAlert>
    )
  }

  return (
    <div>
      <Breadcrumb
        items={[
          { label: 'Arkivobjekt', to: '/browse' },
          { label: aip.title || aip.id },
        ]}
      />

      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: '1.5rem' }}>
        <h1 style={{ margin: 0 }}>{aip.title || aip.id}</h1>
        <StatusBadge state={aip.state} />
      </div>

      <TabBar tabs={TABS} activeTab={activeTab} onTabChange={(id) => setActiveTab(id as TabId)} />

      {activeTab === 'overview' && <OverviewTab aip={aip} representations={representations} />}
      {activeTab === 'metadata' && id && <MetadataTab aipId={id} />}
      {activeTab === 'files' && <FilesTab aipId={id!} representations={representations} />}
      {activeTab === 'events' && <EventsTab aipId={id!} />}
      {activeTab === 'dips' && <DIPsTab aipId={id!} />}
      {activeTab === 'children' && <ChildrenTab aipId={id!} />}
    </div>
  )
}

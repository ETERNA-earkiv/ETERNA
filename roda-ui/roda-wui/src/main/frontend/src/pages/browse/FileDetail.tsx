import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { api } from '../../api/client'
import { fileDownloadUrl, type IndexedFile } from '../../api/files'
import { findPreservationEvents, type IndexedPreservationEvent } from '../../api/preservation-events'
import { DataTable, type Column } from '../../components/common/DataTable'
import { TabBar } from '../../components/common/TabBar'
import { Breadcrumb } from '../../components/common/Breadcrumb'
import { StatusBadge } from '../../components/common/StatusBadge'
import { DigiTable } from '@designsystem-se/af-react'

type TabId = 'overview' | 'events'

const TABS = [
  { id: 'overview' as TabId, label: 'Översikt' },
  { id: 'events' as TabId, label: 'Händelser' },
]

function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

function FilePreview({ file }: { file: IndexedFile }) {
  const ext = file.fileId?.split('.').pop()?.toLowerCase()
  const url = fileDownloadUrl(file.uuid)

  if (['jpg', 'jpeg', 'png', 'gif', 'webp', 'svg'].includes(ext ?? '')) {
    return (
      <div style={{ marginTop: '1rem' }}>
        <img src={url} alt={file.fileId} style={{ maxWidth: '100%', maxHeight: '400px', border: '1px solid #e0e0e0' }} />
      </div>
    )
  }

  if (ext === 'pdf') {
    return (
      <div style={{ marginTop: '1rem' }}>
        <iframe src={url} style={{ width: '100%', height: '600px', border: '1px solid #e0e0e0' }} title={file.fileId} />
      </div>
    )
  }

  return null
}

function OverviewTab({ file }: { file: IndexedFile }) {
  const fields = [
    { label: 'UUID', value: file.uuid },
    { label: 'Fil-ID', value: file.fileId },
    { label: 'Sökväg', value: file.path?.join('/') || '—' },
    { label: 'Storlek', value: file.size ? formatSize(file.size) : '—' },
    { label: 'Format', value: file.fileFormat || '—' },
    { label: 'AIP-ID', value: file.aipId },
    { label: 'Representation-ID', value: file.representationId },
  ]

  return (
    <>
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
      <div style={{ marginTop: '1rem' }}>
        <a
          href={fileDownloadUrl(file.uuid)}
          download={file.fileId}
          style={{
            display: 'inline-block',
            padding: '0.5rem 1.25rem',
            background: 'var(--digi-color-primary, #006991)',
            color: 'white',
            borderRadius: '2px',
            textDecoration: 'none',
            fontSize: '0.9rem',
          }}
        >
          Ladda ner fil
        </a>
      </div>
      <FilePreview file={file} />
    </>
  )
}

function EventsTab({ aipId, fileId }: { aipId: string; fileId: string }) {
  const { data, isLoading, error } = useQuery({
    queryKey: ['preservation-events-file', aipId, fileId],
    queryFn: () => findPreservationEvents({ aipId, fileId }),
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

export function FileDetail() {
  const { aipId, repId, fileId } = useParams<{ aipId: string; repId: string; fileId: string }>()
  const navigate = useNavigate()
  const [activeTab, setActiveTab] = useState<TabId>('overview')

  const { data: file, isLoading, error } = useQuery({
    queryKey: ['file', fileId],
    queryFn: () => api.get<IndexedFile>(`/files/find/${fileId}`),
    enabled: !!fileId,
  })

  if (isLoading) return <DataTable columns={[]} rows={[]} rowKey={() => ''} isLoading />
  if (error || !file) {
    return (
      <div style={{ color: '#c5221f', padding: '2rem' }}>
        Kunde inte hämta filen.{' '}
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
          { label: 'Representation', to: `/browse/${aipId}/representation/${repId}` },
          { label: file.fileId },
        ]}
      />

      <h1 style={{ fontSize: '1.5rem', marginBottom: '1.5rem' }}>{file.fileId}</h1>

      <TabBar tabs={TABS} activeTab={activeTab} onTabChange={(id) => setActiveTab(id as TabId)} />

      {activeTab === 'overview' && <OverviewTab file={file} />}
      {activeTab === 'events' && <EventsTab aipId={aipId!} fileId={fileId!} />}
    </div>
  )
}

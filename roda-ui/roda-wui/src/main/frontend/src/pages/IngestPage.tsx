import { useState, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { uploadSIP, createIngestJob, listTransfers, deleteTransfer, type TransferredResource } from '../api/transfers'
import { TabBar } from '../components/common/TabBar'
import { DataTable, type Column } from '../components/common/DataTable'
import { Pagination } from '../components/common/Pagination'
import { DigiButton, DigiLoaderSpinner, DigiNotificationAlert } from '@designsystem-se/af-react'

type TabId = 'upload' | 'transfers'

const TABS = [
  { id: 'upload' as TabId, label: 'Ladda upp' },
  { id: 'transfers' as TabId, label: 'Överföringshistorik' },
]

function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

function formatDate(iso: string | null): string {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('sv-SE')
}

function UploadTab() {
  const [file, setFile] = useState<File | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const inputRef = useRef<HTMLInputElement>(null)
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  async function handleIngest() {
    if (!file) return
    setLoading(true)
    setError('')
    try {
      const resource = await uploadSIP(file)
      await createIngestJob(resource.uuid)
      queryClient.invalidateQueries({ queryKey: ['transfers'] })
      navigate('/jobs')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Kunde inte starta ingest.')
      setLoading(false)
    }
  }

  return (
    <div style={{ maxWidth: '600px' }}>
      {error && (
        <DigiNotificationAlert afVariation="danger" afHeading="Fel" style={{ marginBottom: '1rem' }}>
          {error}
        </DigiNotificationAlert>
      )}

      <div
        style={{
          border: '2px dashed #ccc',
          borderRadius: '4px',
          padding: '2rem',
          textAlign: 'center',
          marginBottom: '1.5rem',
          background: '#fafafa',
          cursor: 'pointer',
        }}
        onClick={() => inputRef.current?.click()}
      >
        <input
          ref={inputRef}
          type="file"
          accept=".zip,.tar,.bag,.tar.gz,.tgz"
          style={{ display: 'none' }}
          onChange={(e) => setFile(e.target.files?.[0] ?? null)}
        />
        {file ? (
          <div>
            <strong style={{ display: 'block', marginBottom: '0.25rem' }}>{file.name}</strong>
            <span style={{ color: '#666', fontSize: '0.875rem' }}>{formatSize(file.size)}</span>
          </div>
        ) : (
          <div>
            <span style={{ color: '#666' }}>Klicka för att välja SIP-fil</span>
            <div style={{ fontSize: '0.8rem', color: '#999', marginTop: '0.5rem' }}>
              Accepterade format: .zip, .tar, .bag, .tar.gz
            </div>
          </div>
        )}
      </div>

      {loading ? (
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <DigiLoaderSpinner />
          <span>Laddar upp och startar ingest...</span>
        </div>
      ) : file ? (
        <DigiButton afVariation="primary" onAfOnClick={handleIngest}>
          Starta ingest
        </DigiButton>
      ) : (
        <DigiButton afVariation="secondary">
          Välj en fil för att fortsätta
        </DigiButton>
      )}
    </div>
  )
}

function TransferHistoryTab() {
  const PAGE_SIZE = 20
  const [page, setPage] = useState(0)
  const [ingestingId, setIngestingId] = useState<string | null>(null)
  const [actionError, setActionError] = useState('')
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  const { data, isLoading, error } = useQuery({
    queryKey: ['transfers', page],
    queryFn: () => listTransfers(page * PAGE_SIZE, PAGE_SIZE),
  })

  const transfers = data?.results ?? []
  const totalCount = data?.totalCount ?? 0
  const totalPages = Math.ceil(totalCount / PAGE_SIZE)

  async function handleIngest(resource: TransferredResource) {
    setIngestingId(resource.uuid)
    setActionError('')
    try {
      await createIngestJob(resource.uuid)
      navigate('/jobs')
    } catch (err) {
      setActionError(err instanceof Error ? err.message : 'Kunde inte starta ingest.')
      setIngestingId(null)
    }
  }

  async function handleDelete(uuid: string) {
    setActionError('')
    try {
      await deleteTransfer(uuid)
      queryClient.invalidateQueries({ queryKey: ['transfers'] })
    } catch (err) {
      setActionError(err instanceof Error ? err.message : 'Kunde inte ta bort.')
    }
  }

  const columns: Column<TransferredResource>[] = [
    {
      key: 'name',
      header: 'Namn',
      render: (r) => <span style={{ fontFamily: 'monospace', fontSize: '0.85rem' }}>{r.name}</span>,
    },
    {
      key: 'size',
      header: 'Storlek',
      width: '100px',
      render: (r) => formatSize(r.size),
    },
    {
      key: 'date',
      header: 'Datum',
      width: '160px',
      render: (r) => <span style={{ fontSize: '0.8rem' }}>{formatDate(r.date)}</span>,
    },
    {
      key: 'actions',
      header: '',
      width: '180px',
      render: (r) => (
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          {ingestingId === r.uuid ? (
            <DigiLoaderSpinner />
          ) : (
            <DigiButton afVariation="primary" afSize="small" onAfOnClick={() => handleIngest(r)}>
              Ingest
            </DigiButton>
          )}
          <DigiButton afVariation="secondary" afSize="small" onAfOnClick={() => handleDelete(r.uuid)}>
            Ta bort
          </DigiButton>
        </div>
      ),
    },
  ]

  return (
    <>
      {actionError && (
        <DigiNotificationAlert afVariation="danger" afHeading="Fel" style={{ marginBottom: '1rem' }}>
          {actionError}
        </DigiNotificationAlert>
      )}
      <DataTable
        columns={columns}
        rows={transfers}
        rowKey={(r) => r.uuid}
        isLoading={isLoading}
        error={error?.message ?? null}
        emptyMessage="Inga överföringar hittades"
        summary={!isLoading && !error ? `${totalCount} överföringar totalt` : undefined}
      />
      <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
    </>
  )
}

export function IngestPage() {
  const [activeTab, setActiveTab] = useState<TabId>('upload')

  return (
    <div>
      <h1 style={{ marginBottom: '1.5rem' }}>Ingest</h1>
      <TabBar tabs={TABS} activeTab={activeTab} onTabChange={(id) => setActiveTab(id as TabId)} />
      {activeTab === 'upload' && <UploadTab />}
      {activeTab === 'transfers' && <TransferHistoryTab />}
    </div>
  )
}

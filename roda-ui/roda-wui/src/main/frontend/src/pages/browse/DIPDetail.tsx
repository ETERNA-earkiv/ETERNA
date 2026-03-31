import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { getDIP, findDIPFiles, dipFileDownloadUrl, type IndexedDIP, type IndexedDIPFile } from '../../api/dips'
import { DataTable, type Column } from '../../components/common/DataTable'
import { TabBar } from '../../components/common/TabBar'
import { Breadcrumb } from '../../components/common/Breadcrumb'
import { Pagination } from '../../components/common/Pagination'
import { DigiTable } from '@designsystem-se/af-react'

type TabId = 'overview' | 'files'

const TABS = [
  { id: 'overview' as TabId, label: 'Översikt' },
  { id: 'files' as TabId, label: 'Filer' },
]

function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

function OverviewTab({ dip }: { dip: IndexedDIP }) {
  const fields = [
    { label: 'UUID', value: dip.uuid },
    { label: 'Titel', value: dip.title },
    { label: 'Beskrivning', value: dip.description },
    { label: 'Typ', value: dip.type },
    { label: 'Skapad', value: dip.dateCreated ? new Date(dip.dateCreated).toLocaleString('sv-SE') : null },
    { label: 'Ändrad', value: dip.dateModified ? new Date(dip.dateModified).toLocaleString('sv-SE') : null },
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

function FilesTab({ dipId }: { dipId: string }) {
  const [page, setPage] = useState(0)
  const PAGE_SIZE = 50

  const { data, isLoading, error } = useQuery({
    queryKey: ['dip-files', dipId, page],
    queryFn: () => findDIPFiles(dipId, page * PAGE_SIZE, PAGE_SIZE),
  })

  const files = data?.results ?? []
  const totalPages = Math.ceil((data?.totalCount ?? 0) / PAGE_SIZE)

  const columns: Column<IndexedDIPFile>[] = [
    {
      key: 'id',
      header: 'Filnamn',
      render: (f) => f.id,
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
          href={dipFileDownloadUrl(f.uuid)}
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
        emptyMessage="Inga DIP-filer hittades"
      />
      <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
    </>
  )
}

export function DIPDetail() {
  const { dipId } = useParams<{ dipId: string }>()
  const navigate = useNavigate()
  const [activeTab, setActiveTab] = useState<TabId>('overview')

  const { data: dip, isLoading, error } = useQuery({
    queryKey: ['dip', dipId],
    queryFn: () => getDIP(dipId!),
    enabled: !!dipId,
  })

  if (isLoading) return <DataTable columns={[]} rows={[]} rowKey={() => ''} isLoading />
  if (error || !dip) {
    return (
      <div style={{ color: '#c5221f', padding: '2rem' }}>
        Kunde inte hämta DIP.{' '}
        <button onClick={() => navigate(-1)} style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--digi-color-primary, #006991)' }}>
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
          { label: 'DIP: ' + (dip.title || dip.uuid) },
        ]}
      />

      <h1 style={{ fontSize: '1.5rem', marginBottom: '1.5rem' }}>{dip.title || dip.uuid}</h1>

      <TabBar tabs={TABS} activeTab={activeTab} onTabChange={(id) => setActiveTab(id as TabId)} />

      {activeTab === 'overview' && <OverviewTab dip={dip} />}
      {activeTab === 'files' && <FilesTab dipId={dip.uuid} />}
    </div>
  )
}

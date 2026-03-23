import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  getAIP,
  getRepresentations,
  getMetadataList,
  getMetadataHtml,
  type IndexedAIP,
  type Representation,
  type DescriptiveMetadataInfo,
} from '../api/aips'
import { searchFiles, fileDownloadUrl, type IndexedFile } from '../api/files'
import { DigiTable, DigiLoaderSpinner, DigiNotificationAlert, DigiButton } from '@designsystem-se/af-react'

type Tab = 'overview' | 'metadata' | 'files'

const TAB_LABELS: Record<Tab, string> = {
  overview: 'Översikt',
  metadata: 'Metadata',
  files: 'Filer',
}

function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

function TabBar({ active, onChange }: { active: Tab; onChange: (t: Tab) => void }) {
  return (
    <div
      style={{
        display: 'flex',
        borderBottom: '2px solid #e0e0e0',
        marginBottom: '1.5rem',
        gap: '0',
      }}
    >
      {(Object.keys(TAB_LABELS) as Tab[]).map((tab) => (
        <button
          key={tab}
          onClick={() => onChange(tab)}
          style={{
            padding: '0.625rem 1.25rem',
            border: 'none',
            background: 'none',
            cursor: 'pointer',
            fontSize: '0.9375rem',
            borderBottom: active === tab ? '2px solid var(--digi-color-primary, #006991)' : '2px solid transparent',
            marginBottom: '-2px',
            fontWeight: active === tab ? 600 : 400,
            color: active === tab ? 'var(--digi-color-primary, #006991)' : '#555',
          }}
        >
          {TAB_LABELS[tab]}
        </button>
      ))}
    </div>
  )
}

function OverviewTab({ aip, representations }: { aip: IndexedAIP; representations: Representation[] }) {
  const fields: Array<{ label: string; value: string | number | boolean | undefined | null }> = [
    { label: 'ID', value: aip.id },
    { label: 'Titel', value: aip.title },
    { label: 'Nivå', value: aip.level },
    { label: 'Status', value: aip.state },
    { label: 'Skapad', value: aip.dateCreated ? new Date(aip.dateCreated).toLocaleString('sv-SE') : null },
    { label: 'Ändrad', value: aip.dateModified ? new Date(aip.dateModified).toLocaleString('sv-SE') : null },
    { label: 'Har representationer', value: aip.hasRepresentations ? 'Ja' : 'Nej' },
    { label: 'Antal filer (submission)', value: aip.numberOfSubmissionFiles },
    { label: 'Antal filer (dokumentation)', value: aip.numberOfDocumentationFiles },
    { label: 'Spärrat', value: aip.onHold ? 'Ja' : 'Nej' },
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
                    <th style={{ width: '40%', textAlign: 'left' }}>{label}</th>
                    <td>{String(value)}</td>
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
          <DigiTable>
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Typ</th>
                  <th>Original</th>
                  <th>Antal filer</th>
                </tr>
              </thead>
              <tbody>
                {representations.map((rep) => (
                  <tr key={rep.id}>
                    <td style={{ fontFamily: 'monospace', fontSize: '0.8rem' }}>{rep.id}</td>
                    <td>{rep.type}</td>
                    <td>{rep.original ? 'Ja' : 'Nej'}</td>
                    <td>{rep.numberOfDataFiles}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </DigiTable>
        </section>
      )}
    </>
  )
}

function MetadataTab({ aipId }: { aipId: string }) {
  const [metadataList, setMetadataList] = useState<DescriptiveMetadataInfo[]>([])
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [html, setHtml] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [htmlLoading, setHtmlLoading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    getMetadataList(aipId)
      .then((res) => {
        const list = res.metadataInfos ?? []
        setMetadataList(list)
        if (list.length > 0) setSelectedId(list[0].id)
      })
      .catch(() => setError('Kunde inte hämta metadata.'))
      .finally(() => setLoading(false))
  }, [aipId])

  useEffect(() => {
    if (!selectedId) return
    setHtmlLoading(true)
    setHtml(null)
    getMetadataHtml(aipId, selectedId)
      .then(setHtml)
      .catch(() => setHtml('<p>Kunde inte rendera metadata-HTML.</p>'))
      .finally(() => setHtmlLoading(false))
  }, [aipId, selectedId])

  if (loading) return <div style={{ textAlign: 'center', padding: '2rem' }}><DigiLoaderSpinner /></div>
  if (error) return <DigiNotificationAlert afVariation="danger" afHeading="Fel">{error}</DigiNotificationAlert>
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
                borderColor: selectedId === m.id ? 'var(--digi-color-primary, #006991)' : '#ccc',
                borderRadius: '4px',
                background: selectedId === m.id ? 'var(--digi-color-primary, #006991)' : 'white',
                color: selectedId === m.id ? 'white' : '#333',
                cursor: 'pointer',
                fontSize: '0.875rem',
              }}
            >
              {m.type || m.id}
            </button>
          ))}
        </div>
      )}

      {selectedId && (
        <div style={{ marginBottom: '0.75rem', fontSize: '0.875rem' }}>
          <a
            href={`/api/v2/aips/${aipId}/metadata/descriptive/${selectedId}/download`}
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

function FilesTab({ representations }: { representations: Representation[] }) {
  const [expandedRep, setExpandedRep] = useState<string | null>(null)
  const [filesMap, setFilesMap] = useState<Record<string, IndexedFile[]>>({})
  const [loadingMap, setLoadingMap] = useState<Record<string, boolean>>({})

  function toggleRep(repId: string) {
    if (expandedRep === repId) {
      setExpandedRep(null)
      return
    }
    setExpandedRep(repId)
    if (filesMap[repId]) return
    setLoadingMap((prev) => ({ ...prev, [repId]: true }))
    searchFiles(repId)
      .then((result) => setFilesMap((prev) => ({ ...prev, [repId]: result.results ?? [] })))
      .finally(() => setLoadingMap((prev) => ({ ...prev, [repId]: false })))
  }

  if (representations.length === 0) {
    return <p style={{ color: '#666' }}>Inga representationer hittades.</p>
  }

  return (
    <div>
      {representations.map((rep) => (
        <div key={rep.id} style={{ marginBottom: '1rem', border: '1px solid #e0e0e0', borderRadius: '4px' }}>
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              padding: '0.75rem 1rem',
              background: '#f8f8f8',
              cursor: 'pointer',
            }}
            onClick={() => toggleRep(rep.id)}
          >
            <div>
              <strong style={{ fontSize: '0.875rem' }}>{rep.type || rep.id}</strong>
              {rep.original && (
                <span style={{ marginLeft: '0.5rem', fontSize: '0.8rem', color: '#2d7a3a' }}>
                  (Original)
                </span>
              )}
              <span style={{ marginLeft: '0.75rem', fontSize: '0.8rem', color: '#666' }}>
                {rep.numberOfDataFiles} filer
              </span>
            </div>
            <span style={{ color: '#666', fontSize: '0.875rem' }}>
              {expandedRep === rep.id ? '▲' : '▼'}
            </span>
          </div>

          {expandedRep === rep.id && (
            <div style={{ padding: '0.75rem 1rem' }}>
              {loadingMap[rep.id] ? (
                <div style={{ textAlign: 'center', padding: '1rem' }}><DigiLoaderSpinner /></div>
              ) : (filesMap[rep.id] ?? []).length === 0 ? (
                <p style={{ color: '#666', fontSize: '0.875rem' }}>Inga filer hittades.</p>
              ) : (
                <DigiTable>
                  <table>
                    <thead>
                      <tr>
                        <th>Filnamn</th>
                        <th>Sökväg</th>
                        <th>Storlek</th>
                        <th>Format</th>
                        <th></th>
                      </tr>
                    </thead>
                    <tbody>
                      {(filesMap[rep.id] ?? []).map((file) => (
                        <tr key={file.uuid}>
                          <td style={{ fontSize: '0.875rem' }}>{file.fileId}</td>
                          <td style={{ fontSize: '0.8rem', color: '#666' }}>
                            {file.path?.join('/') || '—'}
                          </td>
                          <td style={{ fontSize: '0.8rem' }}>{formatSize(file.size)}</td>
                          <td style={{ fontSize: '0.8rem' }}>{file.fileFormat || '—'}</td>
                          <td>
                            <a
                              href={fileDownloadUrl(file.uuid)}
                              style={{ fontSize: '0.8rem', color: 'var(--digi-color-primary, #006991)' }}
                            >
                              Ladda ned
                            </a>
                          </td>
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

export function AIPDetail() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [aip, setAip] = useState<IndexedAIP | null>(null)
  const [representations, setRepresentations] = useState<Representation[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [tab, setTab] = useState<Tab>('overview')

  useEffect(() => {
    if (!id) return
    setLoading(true)
    Promise.all([getAIP(id), getRepresentations(id)])
      .then(([aipData, repsResult]) => {
        setAip(aipData)
        setRepresentations(repsResult.results ?? [])
      })
      .catch(() => setError('Kunde inte hämta arkivobjektet.'))
      .finally(() => setLoading(false))
  }, [id])

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '4rem' }}>
        <DigiLoaderSpinner />
      </div>
    )
  }

  if (error || !aip) {
    return (
      <DigiNotificationAlert afVariation="danger" afHeading="Fel">
        {error || 'Arkivobjektet hittades inte.'}
      </DigiNotificationAlert>
    )
  }

  return (
    <div>
      <nav style={{ marginBottom: '1rem', fontSize: '0.875rem' }}>
        <DigiButton
          afVariation="tertiary"
          onAfOnClick={() => navigate('/browse')}
          style={{ padding: 0 }}
        >
          Arkivobjekt
        </DigiButton>
        <span style={{ margin: '0 0.5rem', color: '#555' }}>›</span>
        <span>{aip.title || aip.id}</span>
      </nav>

      <div
        style={{
          display: 'flex',
          alignItems: 'flex-start',
          justifyContent: 'space-between',
          marginBottom: '1.5rem',
        }}
      >
        <h1>{aip.title || aip.id}</h1>
        <span
          style={{
            padding: '0.25rem 0.75rem',
            borderRadius: '2px',
            fontSize: '0.875rem',
            background: aip.state === 'ACTIVE' ? '#e6f4ea' : '#fce8e6',
            color: aip.state === 'ACTIVE' ? '#2d7a3a' : '#c5221f',
          }}
        >
          {aip.state}
        </span>
      </div>

      <TabBar active={tab} onChange={setTab} />

      {tab === 'overview' && (
        <OverviewTab aip={aip} representations={representations} />
      )}
      {tab === 'metadata' && id && (
        <MetadataTab aipId={id} />
      )}
      {tab === 'files' && (
        <FilesTab representations={representations} />
      )}
    </div>
  )
}

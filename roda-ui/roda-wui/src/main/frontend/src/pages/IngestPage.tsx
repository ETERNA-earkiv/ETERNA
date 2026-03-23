import { useState, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { uploadSIP, createIngestJob } from '../api/transfers'
import { DigiButton, DigiLoaderSpinner, DigiNotificationAlert } from '@designsystem-se/af-react'

export function IngestPage() {
  const [file, setFile] = useState<File | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const inputRef = useRef<HTMLInputElement>(null)
  const navigate = useNavigate()

  async function handleIngest() {
    if (!file) return
    setLoading(true)
    setError('')
    try {
      const resource = await uploadSIP(file)
      await createIngestJob(resource.uuid)
      navigate('/jobs')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Kunde inte starta ingest.')
      setLoading(false)
    }
  }

  function formatSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
  }

  return (
    <div style={{ maxWidth: '600px' }}>
      <h1 style={{ marginBottom: '1.5rem' }}>Ingest SIP-paket</h1>

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

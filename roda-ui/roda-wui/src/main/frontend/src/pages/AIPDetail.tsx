import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getAIP, getRepresentations, type IndexedAIP, type Representation } from '../api/aips'

export function AIPDetail() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [aip, setAip] = useState<IndexedAIP | null>(null)
  const [representations, setRepresentations] = useState<Representation[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!id) return
    setLoading(true)
    Promise.all([getAIP(id), getRepresentations(id)])
      .then(([aipData, repsResult]) => {
        setAip(aipData)
        setRepresentations(repsResult.results)
      })
      .catch(() => setError('Kunde inte hämta arkivobjektet.'))
      .finally(() => setLoading(false))
  }, [id])

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '4rem' }}>
        <digi-loader></digi-loader>
      </div>
    )
  }

  if (error || !aip) {
    return <digi-message af-type="error">{error || 'Arkivobjektet hittades inte.'}</digi-message>
  }

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
    <div>
      {/* Brödsmulor */}
      <nav style={{ marginBottom: '1rem', fontSize: '0.875rem' }}>
        <button
          onClick={() => navigate('/browse')}
          style={{
            background: 'none',
            border: 'none',
            color: 'var(--digi-color-primary, #006991)',
            cursor: 'pointer',
            padding: 0,
            textDecoration: 'underline',
          }}
        >
          Arkivobjekt
        </button>
        <span style={{ margin: '0 0.5rem', color: '#555' }}>›</span>
        <span>{aip.title || aip.id}</span>
      </nav>

      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: '1.5rem' }}>
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

      {/* Metadata */}
      <section style={{ marginBottom: '2rem' }}>
        <h2 style={{ fontSize: '1.125rem', marginBottom: '0.75rem' }}>Metadata</h2>
        <digi-table>
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
        </digi-table>
      </section>

      {/* Representationer */}
      {representations.length > 0 && (
        <section>
          <h2 style={{ fontSize: '1.125rem', marginBottom: '0.75rem' }}>
            Representationer ({representations.length})
          </h2>
          <digi-table>
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
          </digi-table>
        </section>
      )}
    </div>
  )
}

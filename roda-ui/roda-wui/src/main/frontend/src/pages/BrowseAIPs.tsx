import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { searchAIPs, type IndexedAIP } from '../api/aips'

const PAGE_SIZE = 20

export function BrowseAIPs() {
  const [aips, setAips] = useState<IndexedAIP[]>([])
  const [total, setTotal] = useState(0)
  const [query, setQuery] = useState('')
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const navigate = useNavigate()

  useEffect(() => {
    setLoading(true)
    setError('')
    searchAIPs(search, page * PAGE_SIZE, PAGE_SIZE)
      .then((result) => {
        setAips(result.results)
        setTotal(result.totalCount)
      })
      .catch(() => setError('Kunde inte hämta arkivobjekt.'))
      .finally(() => setLoading(false))
  }, [search, page])

  function handleSearch(e: React.FormEvent) {
    e.preventDefault()
    setPage(0)
    setSearch(query)
  }

  const totalPages = Math.ceil(total / PAGE_SIZE)

  return (
    <div>
      <h1 style={{ marginBottom: '1.5rem' }}>Arkivobjekt</h1>

      <form onSubmit={handleSearch} style={{ display: 'flex', gap: '0.75rem', marginBottom: '1.5rem' }}>
        <digi-input
          af-label="Sök arkivobjekt"
          af-placeholder="Sök på titel, ID…"
          af-value={query}
          onAfOnInputChange={(e: CustomEvent) =>
            setQuery((e.detail as { value: string }).value)
          }
          style={{ flex: 1 }}
        ></digi-input>
        <digi-button af-variation="primary" af-type="submit">
          Sök
        </digi-button>
      </form>

      {loading && (
        <div style={{ textAlign: 'center', padding: '2rem' }}>
          <digi-loader></digi-loader>
        </div>
      )}

      {error && (
        <digi-message af-type="error">{error}</digi-message>
      )}

      {!loading && !error && (
        <>
          <p style={{ marginBottom: '0.75rem', color: '#555', fontSize: '0.875rem' }}>
            Visar {aips.length} av {total} arkivobjekt
          </p>

          <digi-table>
            <table>
              <thead>
                <tr>
                  <th>Titel</th>
                  <th>Nivå</th>
                  <th>Status</th>
                  <th>Skapad</th>
                </tr>
              </thead>
              <tbody>
                {aips.length === 0 ? (
                  <tr>
                    <td colSpan={4} style={{ textAlign: 'center', padding: '2rem' }}>
                      Inga arkivobjekt hittades
                    </td>
                  </tr>
                ) : (
                  aips.map((aip) => (
                    <tr
                      key={aip.id}
                      style={{ cursor: 'pointer' }}
                      onClick={() => navigate(`/browse/${aip.id}`)}
                    >
                      <td>
                        <strong style={{ color: 'var(--digi-color-primary, #006991)' }}>
                          {aip.title || aip.id}
                        </strong>
                      </td>
                      <td>{aip.level || '—'}</td>
                      <td>
                        <span
                          style={{
                            padding: '0.2rem 0.6rem',
                            borderRadius: '2px',
                            fontSize: '0.8rem',
                            background: aip.state === 'ACTIVE' ? '#e6f4ea' : '#fce8e6',
                            color: aip.state === 'ACTIVE' ? '#2d7a3a' : '#c5221f',
                          }}
                        >
                          {aip.state}
                        </span>
                      </td>
                      <td>
                        {aip.dateCreated
                          ? new Date(aip.dateCreated).toLocaleDateString('sv-SE')
                          : '—'}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </digi-table>

          {totalPages > 1 && (
            <div style={{ display: 'flex', justifyContent: 'center', marginTop: '1.5rem' }}>
              <digi-pagination
                af-current-page={page + 1}
                af-total-pages={totalPages}
                onAfOnPageChange={(e: CustomEvent) => {
                  const newPage = (e.detail as { page: number }).page - 1
                  setPage(newPage)
                  window.scrollTo(0, 0)
                }}
              ></digi-pagination>
            </div>
          )}
        </>
      )}
    </div>
  )
}

import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { searchAIPs, type IndexedAIP } from '../api/aips'
import {
  DigiFormInputSearch,
  DigiTable,
  DigiNavigationPagination,
  DigiLoaderSpinner,
  DigiNotificationAlert,
} from '@designsystem-se/af-react'

const PAGE_SIZE = 20

type InputSearchEl = HTMLElement & { afValue: string }

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
        setAips(result.results ?? [])
        setTotal(result.totalCount)
      })
      .catch(() => setError('Kunde inte hämta arkivobjekt.'))
      .finally(() => setLoading(false))
  }, [search, page])

  function submitSearch(value: string) {
    setPage(0)
    setSearch(value)
    setQuery(value)
  }

  const totalPages = Math.ceil(total / PAGE_SIZE)

  return (
    <div>
      <h1 style={{ marginBottom: '1.5rem' }}>Arkivobjekt</h1>

      <div style={{ display: 'flex', gap: '0.75rem', marginBottom: '1.5rem', alignItems: 'flex-end' }}>
        <div style={{ flex: 1 }}>
          <DigiFormInputSearch
            afLabel="Sök arkivobjekt"
            afId="search-aips"
            afValue={query}
            onAfOnInput={(e) => setQuery((e.target as InputSearchEl).afValue)}
            onAfOnSubmitSearch={(e) => submitSearch(e.detail as string || query)}
          />
        </div>
      </div>

      {loading && (
        <div style={{ textAlign: 'center', padding: '2rem' }}>
          <DigiLoaderSpinner />
        </div>
      )}

      {error && (
        <DigiNotificationAlert afVariation="danger" afHeading="Fel">
          {error}
        </DigiNotificationAlert>
      )}

      {!loading && !error && (
        <>
          <p style={{ marginBottom: '0.75rem', color: '#555', fontSize: '0.875rem' }}>
            Visar {aips.length} av {total} arkivobjekt
          </p>

          <DigiTable>
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
          </DigiTable>

          {totalPages > 1 && (
            <div style={{ display: 'flex', justifyContent: 'center', marginTop: '1.5rem' }}>
              <DigiNavigationPagination
                key={`pagination-${search}`}
                afInitActivePage={1}
                afTotalPages={totalPages}
                onAfOnPageChange={(e) => {
                  setPage((e.detail as number) - 1)
                  window.scrollTo(0, 0)
                }}
              />
            </div>
          )}
        </>
      )}
    </div>
  )
}

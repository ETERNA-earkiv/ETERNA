import { useEffect, useState } from 'react'
import { searchLogs, type LogEntry } from '../api/auditlog'
import {
  DigiFormInputSearch,
  DigiTable,
  DigiLoaderSpinner,
  DigiNotificationAlert,
  DigiNavigationPagination,
} from '@designsystem-se/af-react'

const PAGE_SIZE = 20

type InputSearchEl = HTMLElement & { afValue: string }

const STATE_STYLE: Record<string, { background: string; color: string }> = {
  SUCCESS: { background: '#e6f4ea', color: '#2d7a3a' },
  FAILURE: { background: '#fce8e6', color: '#c5221f' },
  UNAUTHORIZED: { background: '#fff3e0', color: '#e65100' },
}

function StateBadge({ state }: { state: string }) {
  const style = STATE_STYLE[state] ?? { background: '#f1f3f4', color: '#5f6368' }
  return (
    <span style={{ padding: '0.2rem 0.6rem', borderRadius: '2px', fontSize: '0.8rem', ...style }}>
      {state}
    </span>
  )
}

export function AuditLogPage() {
  const [entries, setEntries] = useState<LogEntry[]>([])
  const [total, setTotal] = useState(0)
  const [query, setQuery] = useState('')
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    setLoading(true)
    setError('')
    searchLogs(search, page * PAGE_SIZE, PAGE_SIZE)
      .then((result) => {
        setEntries(result.results ?? [])
        setTotal(result.totalCount)
      })
      .catch(() => setError('Kunde inte hämta granskningslogg.'))
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
      <h1 style={{ marginBottom: '1.5rem' }}>Granskningslogg</h1>

      <div style={{ marginBottom: '1.5rem' }}>
        <DigiFormInputSearch
          afLabel="Sök i logg"
          afId="search-audit"
          afValue={query}
          onAfOnInput={(e) => setQuery((e.target as InputSearchEl).afValue)}
          onAfOnSubmitSearch={(e) => submitSearch(e.detail as string || query)}
        />
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
            Visar {entries.length} av {total} poster
          </p>

          <DigiTable>
            <table>
              <thead>
                <tr>
                  <th>Tidpunkt</th>
                  <th>Användare</th>
                  <th>Åtgärd</th>
                  <th>Objekt-ID</th>
                  <th>Status</th>
                  <th>ms</th>
                </tr>
              </thead>
              <tbody>
                {entries.length === 0 ? (
                  <tr>
                    <td colSpan={6} style={{ textAlign: 'center', padding: '2rem' }}>
                      Inga loggposter hittades
                    </td>
                  </tr>
                ) : (
                  entries.map((entry) => (
                    <tr key={entry.uuid}>
                      <td style={{ fontSize: '0.8rem', whiteSpace: 'nowrap' }}>
                        {new Date(entry.datetime).toLocaleString('sv-SE')}
                      </td>
                      <td>{entry.username}</td>
                      <td style={{ fontSize: '0.8rem' }}>
                        {entry.actionComponent}.{entry.actionMethod}
                      </td>
                      <td
                        style={{
                          fontFamily: 'monospace',
                          fontSize: '0.75rem',
                          maxWidth: '200px',
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                          whiteSpace: 'nowrap',
                        }}
                      >
                        {entry.relatedObjectID || '—'}
                      </td>
                      <td>
                        <StateBadge state={entry.state} />
                      </td>
                      <td style={{ fontSize: '0.8rem' }}>{entry.duration}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </DigiTable>

          {totalPages > 1 && (
            <div style={{ display: 'flex', justifyContent: 'center', marginTop: '1.5rem' }}>
              <DigiNavigationPagination
                key={`audit-pagination-${search}-${page}`}
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

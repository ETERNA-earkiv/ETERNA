import { useEffect, useState } from 'react'
import { searchMembers, type RODAMember } from '../api/members'

const PAGE_SIZE = 20

export function UserManagement() {
  const [members, setMembers] = useState<RODAMember[]>([])
  const [total, setTotal] = useState(0)
  const [query, setQuery] = useState('')
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    setLoading(true)
    setError('')
    searchMembers(search, page * PAGE_SIZE, PAGE_SIZE)
      .then((result) => {
        setMembers(result.results)
        setTotal(result.totalCount)
      })
      .catch(() => setError('Kunde inte hämta användare.'))
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
      <h1 style={{ marginBottom: '1.5rem' }}>Användarhantering</h1>

      <form onSubmit={handleSearch} style={{ display: 'flex', gap: '0.75rem', marginBottom: '1.5rem' }}>
        <digi-input
          af-label="Sök användare"
          af-placeholder="Sök på namn, e-post…"
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

      {error && <digi-message af-type="error">{error}</digi-message>}

      {!loading && !error && (
        <>
          <p style={{ marginBottom: '0.75rem', color: '#555', fontSize: '0.875rem' }}>
            Visar {members.length} av {total} poster
          </p>

          <digi-table>
            <table>
              <thead>
                <tr>
                  <th>Namn</th>
                  <th>Fullständigt namn</th>
                  <th>E-post</th>
                  <th>Typ</th>
                  <th>Aktiv</th>
                </tr>
              </thead>
              <tbody>
                {members.length === 0 ? (
                  <tr>
                    <td colSpan={5} style={{ textAlign: 'center', padding: '2rem' }}>
                      Inga användare hittades
                    </td>
                  </tr>
                ) : (
                  members.map((member) => (
                    <tr key={member.id}>
                      <td>
                        <strong>{member.name}</strong>
                      </td>
                      <td>{member.fullName || '—'}</td>
                      <td>{member.email || '—'}</td>
                      <td>
                        <span
                          style={{
                            padding: '0.2rem 0.5rem',
                            borderRadius: '2px',
                            fontSize: '0.8rem',
                            background: member.isUser ? '#e8f0fe' : '#f3e8fd',
                            color: member.isUser ? '#1a73e8' : '#7b1fa2',
                          }}
                        >
                          {member.isUser ? 'Användare' : 'Grupp'}
                        </span>
                      </td>
                      <td>
                        <span
                          style={{
                            padding: '0.2rem 0.5rem',
                            borderRadius: '2px',
                            fontSize: '0.8rem',
                            background: member.active ? '#e6f4ea' : '#f5f5f5',
                            color: member.active ? '#2d7a3a' : '#555',
                          }}
                        >
                          {member.active ? 'Ja' : 'Nej'}
                        </span>
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

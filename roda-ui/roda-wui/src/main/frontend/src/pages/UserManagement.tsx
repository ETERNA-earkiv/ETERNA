import { useEffect, useState } from 'react'
import { searchMembers, type RODAMember } from '../api/members'
import {
  DigiFormInputSearch,
  DigiTable,
  DigiNavigationPagination,
  DigiLoaderSpinner,
  DigiNotificationAlert,
} from '@designsystem-se/af-react'

const PAGE_SIZE = 20

type InputSearchEl = HTMLElement & { afValue: string }

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
        setMembers(result.results ?? [])
        setTotal(result.totalCount)
      })
      .catch(() => setError('Kunde inte hämta användare.'))
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
      <h1 style={{ marginBottom: '1.5rem' }}>Användarhantering</h1>

      <div style={{ marginBottom: '1.5rem' }}>
        <DigiFormInputSearch
          afLabel="Sök användare"
          afId="search-members"
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
            Visar {members.length} av {total} poster
          </p>

          <DigiTable>
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

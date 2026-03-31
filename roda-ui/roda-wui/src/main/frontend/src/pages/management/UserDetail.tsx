import { useParams, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { getUser } from '../../api/members'
import { Breadcrumb } from '../../components/common/Breadcrumb'
import { DataTable } from '../../components/common/DataTable'
import { DigiTable } from '@designsystem-se/af-react'

export function UserDetail() {
  const { username } = useParams<{ username: string }>()
  const navigate = useNavigate()

  const { data: user, isLoading, error } = useQuery({
    queryKey: ['user', username],
    queryFn: () => getUser(username!),
    enabled: !!username,
  })

  if (isLoading) return <DataTable columns={[]} rows={[]} rowKey={() => ''} isLoading />
  if (error || !user) {
    return (
      <div style={{ color: '#c5221f', padding: '2rem' }}>
        Kunde inte hämta användaren.{' '}
        <button
          onClick={() => navigate(-1)}
          style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--digi-color-primary, #006991)' }}
        >
          Tillbaka
        </button>
      </div>
    )
  }

  const fields = [
    { label: 'Användarnamn', value: user.name },
    { label: 'Fullständigt namn', value: user.fullName },
    { label: 'E-post', value: user.email },
    {
      label: 'Status',
      value: (
        <span style={{ color: user.active ? '#2d7a3a' : '#c5221f', fontWeight: 600 }}>
          {user.active ? 'Aktiv' : 'Inaktiv'}
        </span>
      ),
    },
    { label: 'Grupper', value: user.groups?.join(', ') || '—' },
    { label: 'Direkta roller', value: user.directRoles?.join(', ') || '—' },
    { label: 'Alla roller', value: user.allRoles?.join(', ') || '—' },
    { label: 'Senast inloggad', value: user.lastLogin ? new Date(user.lastLogin).toLocaleString('sv-SE') : '—' },
    { label: 'IP-adress', value: user.ipAddress },
  ]

  return (
    <div>
      <Breadcrumb
        items={[
          { label: 'Användarhantering', to: '/management/users' },
          { label: user.name },
        ]}
      />

      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.5rem' }}>
        <h1 style={{ fontSize: '1.5rem', margin: 0 }}>{user.fullName || user.name}</h1>
        <span
          style={{
            padding: '0.2rem 0.75rem',
            borderRadius: '2px',
            fontSize: '0.85rem',
            background: '#e8f0fe',
            color: '#1a56db',
          }}
        >
          Användare
        </span>
      </div>

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
    </div>
  )
}

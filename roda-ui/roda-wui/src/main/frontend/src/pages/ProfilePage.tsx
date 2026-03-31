import { useAuth } from '../components/AuthProvider'
import { DigiTable } from '@designsystem-se/af-react'

export function ProfilePage() {
  const { user } = useAuth()

  if (!user) {
    return <div style={{ padding: '2rem', color: '#555' }}>Inte inloggad.</div>
  }

  const fields = [
    { label: 'Användarnamn', value: user.name },
    { label: 'Fullständigt namn', value: user.fullName || '—' },
    { label: 'E-post', value: user.email || '—' },
    {
      label: 'Status',
      value: (
        <span style={{ color: user.active ? '#2d7a3a' : '#c5221f', fontWeight: 600 }}>
          {user.active ? 'Aktiv' : 'Inaktiv'}
        </span>
      ),
    },
    { label: 'Grupper', value: user.groups?.join(', ') || '—' },
    { label: 'Roller', value: user.allRoles?.join(', ') || '—' },
  ]

  return (
    <div style={{ maxWidth: '700px' }}>
      <h1 style={{ fontSize: '1.5rem', marginBottom: '1.5rem' }}>Min profil</h1>

      <DigiTable>
        <table>
          <tbody>
            {fields.map(({ label, value }) => (
              <tr key={label}>
                <th style={{ width: '200px', textAlign: 'left', fontWeight: 600, padding: '0.5rem' }}>
                  {label}
                </th>
                <td style={{ padding: '0.5rem' }}>{value}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </DigiTable>
    </div>
  )
}

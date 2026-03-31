import { useParams, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { getGroup } from '../../api/members'
import { Breadcrumb } from '../../components/common/Breadcrumb'
import { DataTable } from '../../components/common/DataTable'
import { DigiTable } from '@designsystem-se/af-react'

export function GroupDetail() {
  const { groupName } = useParams<{ groupName: string }>()
  const navigate = useNavigate()

  const { data: group, isLoading, error } = useQuery({
    queryKey: ['group', groupName],
    queryFn: () => getGroup(groupName!),
    enabled: !!groupName,
  })

  if (isLoading) return <DataTable columns={[]} rows={[]} rowKey={() => ''} isLoading />
  if (error || !group) {
    return (
      <div style={{ color: '#c5221f', padding: '2rem' }}>
        Kunde inte hämta gruppen.{' '}
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
    { label: 'Namn', value: group.name },
    { label: 'Fullständigt namn', value: group.fullName },
    { label: 'Medlemmar', value: group.users?.join(', ') || '—' },
    { label: 'Direkta roller', value: group.directRoles?.join(', ') || '—' },
    { label: 'Alla roller', value: group.allRoles?.join(', ') || '—' },
  ]

  return (
    <div>
      <Breadcrumb
        items={[
          { label: 'Användarhantering', to: '/management/users' },
          { label: group.name },
        ]}
      />

      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.5rem' }}>
        <h1 style={{ fontSize: '1.5rem', margin: 0 }}>{group.fullName || group.name}</h1>
        <span
          style={{
            padding: '0.2rem 0.75rem',
            borderRadius: '2px',
            fontSize: '0.85rem',
            background: '#f3e8fd',
            color: '#7b1fa2',
          }}
        >
          Grupp
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

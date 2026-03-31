import { useParams, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { getRepresentationInformation } from '../../api/planning'
import { Breadcrumb } from '../../components/common/Breadcrumb'
import { DataTable } from '../../components/common/DataTable'
import { DigiTable } from '@designsystem-se/af-react'

export function RepInfoDetail() {
  const { repInfoId } = useParams<{ repInfoId: string }>()
  const navigate = useNavigate()

  const { data: ri, isLoading, error } = useQuery({
    queryKey: ['rep-info', repInfoId],
    queryFn: () => getRepresentationInformation(repInfoId!),
    enabled: !!repInfoId,
  })

  if (isLoading) return <DataTable columns={[]} rows={[]} rowKey={() => ''} isLoading />
  if (error || !ri) {
    return (
      <div style={{ color: '#c5221f', padding: '2rem' }}>
        Kunde inte hämta representationsinformation.{' '}
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
    { label: 'ID', value: ri.id },
    { label: 'Namn', value: ri.name },
    { label: 'Beskrivning', value: ri.description },
    { label: 'Familj', value: ri.family },
    { label: 'Taggar', value: ri.tags?.join(', ') || null },
    { label: 'Skapad', value: ri.createdOn ? new Date(ri.createdOn).toLocaleString('sv-SE') : null },
    { label: 'Skapad av', value: ri.createdBy },
    { label: 'Ändrad', value: ri.updatedOn ? new Date(ri.updatedOn).toLocaleString('sv-SE') : null },
    { label: 'Ändrad av', value: ri.updatedBy },
  ]

  return (
    <div>
      <Breadcrumb
        items={[
          { label: 'Planering', to: '/planning' },
          { label: ri.name },
        ]}
      />

      <h1 style={{ fontSize: '1.5rem', marginBottom: '1.5rem' }}>{ri.name}</h1>

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

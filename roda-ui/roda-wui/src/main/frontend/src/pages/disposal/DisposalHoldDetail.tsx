import { useParams, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { getDisposalHold } from '../../api/disposal'
import { Breadcrumb } from '../../components/common/Breadcrumb'
import { StatusBadge } from '../../components/common/StatusBadge'
import { DataTable } from '../../components/common/DataTable'
import { DigiTable } from '@designsystem-se/af-react'

export function DisposalHoldDetail() {
  const { holdId } = useParams<{ holdId: string }>()
  const navigate = useNavigate()

  const { data: hold, isLoading, error } = useQuery({
    queryKey: ['disposal-hold', holdId],
    queryFn: () => getDisposalHold(holdId!),
    enabled: !!holdId,
  })

  if (isLoading) return <DataTable columns={[]} rows={[]} rowKey={() => ''} isLoading />
  if (error || !hold) {
    return (
      <div style={{ color: '#c5221f', padding: '2rem' }}>
        Kunde inte hämta gallringsstopp.{' '}
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
    { label: 'ID', value: hold.id },
    { label: 'Titel', value: hold.title },
    { label: 'Beskrivning', value: hold.description },
    { label: 'Mandat', value: hold.mandate },
    { label: 'Tillämpningsanvisning', value: hold.scopeNotes },
    { label: 'Status', value: <StatusBadge state={hold.state} /> },
    { label: 'Beslutsdatum', value: hold.originatedOn ? new Date(hold.originatedOn).toLocaleDateString('sv-SE') : null },
    { label: 'Beslutad av', value: hold.originatedBy },
    { label: 'Arkivobjekt', value: hold.aipCounter },
    { label: 'Hävt', value: hold.liftedOn ? new Date(hold.liftedOn).toLocaleDateString('sv-SE') : '—' },
    { label: 'Hävt av', value: hold.liftedBy },
    { label: 'Skapad', value: hold.createdOn ? new Date(hold.createdOn).toLocaleString('sv-SE') : null },
    { label: 'Skapad av', value: hold.createdBy },
    { label: 'Ändrad', value: hold.updatedOn ? new Date(hold.updatedOn).toLocaleString('sv-SE') : null },
    { label: 'Ändrad av', value: hold.updatedBy },
  ]

  return (
    <div>
      <Breadcrumb
        items={[
          { label: 'Gallring', to: '/disposal' },
          { label: hold.title },
        ]}
      />

      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: '1.5rem' }}>
        <h1 style={{ fontSize: '1.5rem', margin: 0 }}>{hold.title}</h1>
        <StatusBadge state={hold.state} />
      </div>

      <DigiTable>
        <table>
          <tbody>
            {fields.map(({ label, value }) => (
              <tr key={label}>
                <th style={{ width: '220px', textAlign: 'left', fontWeight: 600, padding: '0.5rem' }}>
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

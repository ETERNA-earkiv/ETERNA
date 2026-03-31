import { useParams, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { getDisposalConfirmation, disposalConfirmationReportUrl } from '../../api/disposal'
import { Breadcrumb } from '../../components/common/Breadcrumb'
import { StatusBadge } from '../../components/common/StatusBadge'
import { DataTable } from '../../components/common/DataTable'
import { DigiTable } from '@designsystem-se/af-react'

function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
  return `${(bytes / (1024 * 1024 * 1024)).toFixed(1)} GB`
}

export function DisposalConfirmationDetail() {
  const { confirmationId } = useParams<{ confirmationId: string }>()
  const navigate = useNavigate()

  const { data: confirmation, isLoading, error } = useQuery({
    queryKey: ['disposal-confirmation', confirmationId],
    queryFn: () => getDisposalConfirmation(confirmationId!),
    enabled: !!confirmationId,
  })

  if (isLoading) return <DataTable columns={[]} rows={[]} rowKey={() => ''} isLoading />
  if (error || !confirmation) {
    return (
      <div style={{ color: '#c5221f', padding: '2rem' }}>
        Kunde inte hämta gallringsbekräftelse.{' '}
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
    { label: 'ID', value: confirmation.id },
    { label: 'Titel', value: confirmation.title },
    { label: 'Status', value: <StatusBadge state={confirmation.state} /> },
    { label: 'Arkivobjekt', value: confirmation.numberOfAIPs },
    { label: 'Total storlek', value: formatSize(confirmation.size) },
    { label: 'Gallringsplaner', value: confirmation.disposalScheduleIds.join(', ') || '—' },
    { label: 'Gallringsstopp', value: confirmation.disposalHoldIds.join(', ') || '—' },
    { label: 'Skapad', value: confirmation.createdOn ? new Date(confirmation.createdOn).toLocaleString('sv-SE') : null },
    { label: 'Skapad av', value: confirmation.createdBy },
    { label: 'Utförd', value: confirmation.executedOn ? new Date(confirmation.executedOn).toLocaleString('sv-SE') : '—' },
    { label: 'Utförd av', value: confirmation.executedBy },
    { label: 'Återställd', value: confirmation.restoredOn ? new Date(confirmation.restoredOn).toLocaleString('sv-SE') : '—' },
    { label: 'Återställd av', value: confirmation.restoredBy },
  ]

  const extraEntries = Object.entries(confirmation.extraFields ?? {})

  return (
    <div>
      <Breadcrumb
        items={[
          { label: 'Gallring', to: '/disposal' },
          { label: confirmation.title },
        ]}
      />

      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: '1.5rem' }}>
        <h1 style={{ fontSize: '1.5rem', margin: 0 }}>{confirmation.title}</h1>
        <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center' }}>
          <StatusBadge state={confirmation.state} />
          <a
            href={disposalConfirmationReportUrl(confirmation.id)}
            target="_blank"
            rel="noreferrer"
            style={{
              padding: '0.4rem 1rem',
              background: 'var(--digi-color-primary, #006991)',
              color: 'white',
              borderRadius: '2px',
              textDecoration: 'none',
              fontSize: '0.875rem',
            }}
          >
            Rapport
          </a>
        </div>
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
            {extraEntries.map(([key, value]) => (
              <tr key={key}>
                <th style={{ width: '220px', textAlign: 'left', fontWeight: 600, padding: '0.5rem', color: '#555' }}>
                  {key}
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

import { useParams, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { getDisposalSchedule } from '../../api/disposal'
import { Breadcrumb } from '../../components/common/Breadcrumb'
import { StatusBadge } from '../../components/common/StatusBadge'
import { DataTable } from '../../components/common/DataTable'
import { DigiTable } from '@designsystem-se/af-react'

function actionCodeLabel(code: string): string {
  switch (code) {
    case 'RETAIN_PERMANENTLY': return 'Bevaras permanent'
    case 'REVIEW': return 'Omprövas'
    case 'DESTROY': return 'Gallras'
    default: return code
  }
}

function retentionLabel(intervalCode: string, duration: number | null): string {
  if (intervalCode === 'NO_RETENTION_PERIOD') return 'Ingen bevarandetid'
  const labels: Record<string, string> = {
    DAYS: 'dagar',
    WEEKS: 'veckor',
    MONTHS: 'månader',
    YEARS: 'år',
  }
  return `${duration ?? '?'} ${labels[intervalCode] ?? intervalCode}`
}

export function DisposalScheduleDetail() {
  const { scheduleId } = useParams<{ scheduleId: string }>()
  const navigate = useNavigate()

  const { data: schedule, isLoading, error } = useQuery({
    queryKey: ['disposal-schedule', scheduleId],
    queryFn: () => getDisposalSchedule(scheduleId!),
    enabled: !!scheduleId,
  })

  if (isLoading) return <DataTable columns={[]} rows={[]} rowKey={() => ''} isLoading />
  if (error || !schedule) {
    return (
      <div style={{ color: '#c5221f', padding: '2rem' }}>
        Kunde inte hämta gallringsplan.{' '}
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
    { label: 'ID', value: schedule.id },
    { label: 'Titel', value: schedule.title },
    { label: 'Beskrivning', value: schedule.description },
    { label: 'Mandat', value: schedule.mandate },
    { label: 'Tillämpningsanvisning', value: schedule.scopeNotes },
    { label: 'Åtgärd', value: actionCodeLabel(schedule.actionCode) },
    { label: 'Bevarandetid', value: retentionLabel(schedule.retentionPeriodIntervalCode, schedule.retentionPeriodDuration) },
    { label: 'Triggerelement', value: schedule.retentionTriggerElementId },
    { label: 'Status', value: <StatusBadge state={schedule.state} /> },
    { label: 'Arkivobjekt', value: schedule.apiCounter },
    { label: 'Skapad', value: schedule.createdOn ? new Date(schedule.createdOn).toLocaleString('sv-SE') : null },
    { label: 'Skapad av', value: schedule.createdBy },
    { label: 'Ändrad', value: schedule.updatedOn ? new Date(schedule.updatedOn).toLocaleString('sv-SE') : null },
    { label: 'Ändrad av', value: schedule.updatedBy },
    { label: 'Första användning', value: schedule.firstTimeUsed ? new Date(schedule.firstTimeUsed).toLocaleDateString('sv-SE') : '—' },
  ]

  return (
    <div>
      <Breadcrumb
        items={[
          { label: 'Gallring', to: '/disposal' },
          { label: schedule.title },
        ]}
      />

      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: '1.5rem' }}>
        <h1 style={{ fontSize: '1.5rem', margin: 0 }}>{schedule.title}</h1>
        <StatusBadge state={schedule.state} />
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

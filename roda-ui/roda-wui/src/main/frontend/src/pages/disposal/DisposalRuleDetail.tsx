import { useParams, useNavigate, Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { getDisposalRule } from '../../api/disposal'
import { Breadcrumb } from '../../components/common/Breadcrumb'
import { DataTable } from '../../components/common/DataTable'
import { DigiTable } from '@designsystem-se/af-react'

function conditionTypeLabel(type: string): string {
  switch (type) {
    case 'IS_CHILD_OF': return 'Är barn till'
    case 'METADATA_FIELD': return 'Metadatafält'
    default: return type
  }
}

export function DisposalRuleDetail() {
  const { ruleId } = useParams<{ ruleId: string }>()
  const navigate = useNavigate()

  const { data: rule, isLoading, error } = useQuery({
    queryKey: ['disposal-rule', ruleId],
    queryFn: () => getDisposalRule(ruleId!),
    enabled: !!ruleId,
  })

  if (isLoading) return <DataTable columns={[]} rows={[]} rowKey={() => ''} isLoading />
  if (error || !rule) {
    return (
      <div style={{ color: '#c5221f', padding: '2rem' }}>
        Kunde inte hämta gallringsregel.{' '}
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
    { label: 'ID', value: rule.id },
    { label: 'Titel', value: rule.title },
    { label: 'Beskrivning', value: rule.description },
    { label: 'Ordning', value: rule.order },
    { label: 'Typ', value: conditionTypeLabel(rule.type) },
    { label: 'Villkorsnyckel', value: rule.conditionKey },
    { label: 'Villkorsvärde', value: rule.conditionValue },
    {
      label: 'Gallringsplan',
      value: rule.disposalScheduleId ? (
        <Link
          to={`/disposal/schedules/${rule.disposalScheduleId}`}
          style={{ color: 'var(--digi-color-primary, #006991)' }}
        >
          {rule.disposalScheduleName || rule.disposalScheduleId}
        </Link>
      ) : null,
    },
    { label: 'Skapad', value: rule.createdOn ? new Date(rule.createdOn).toLocaleString('sv-SE') : null },
    { label: 'Skapad av', value: rule.createdBy },
    { label: 'Ändrad', value: rule.updatedOn ? new Date(rule.updatedOn).toLocaleString('sv-SE') : null },
    { label: 'Ändrad av', value: rule.updatedBy },
  ]

  return (
    <div>
      <Breadcrumb
        items={[
          { label: 'Gallring', to: '/disposal' },
          { label: rule.title },
        ]}
      />

      <h1 style={{ fontSize: '1.5rem', marginBottom: '1.5rem' }}>{rule.title}</h1>

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

import { useParams, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { getPreservationAgent, preservationAgentDownloadUrl } from '../../api/planning'
import { Breadcrumb } from '../../components/common/Breadcrumb'
import { DataTable } from '../../components/common/DataTable'
import { DigiTable } from '@designsystem-se/af-react'

export function PreservationAgentDetail() {
  const { agentId } = useParams<{ agentId: string }>()
  const navigate = useNavigate()

  const { data: agent, isLoading, error } = useQuery({
    queryKey: ['preservation-agent', agentId],
    queryFn: () => getPreservationAgent(agentId!),
    enabled: !!agentId,
  })

  if (isLoading) return <DataTable columns={[]} rows={[]} rowKey={() => ''} isLoading />
  if (error || !agent) {
    return (
      <div style={{ color: '#c5221f', padding: '2rem' }}>
        Kunde inte hämta bevarandeagent.{' '}
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
    { label: 'ID', value: agent.id },
    { label: 'Namn', value: agent.name },
    { label: 'Typ', value: agent.type },
    { label: 'Version', value: agent.version },
    { label: 'Roller', value: agent.roles?.join(', ') || null },
    { label: 'Anteckning', value: agent.note },
    { label: 'Instans', value: agent.instanceName || agent.instanceId },
    { label: 'Skapad', value: agent.createdOn ? new Date(agent.createdOn).toLocaleString('sv-SE') : null },
  ]

  return (
    <div>
      <Breadcrumb
        items={[
          { label: 'Planering', to: '/planning' },
          { label: agent.name },
        ]}
      />

      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: '1.5rem' }}>
        <h1 style={{ fontSize: '1.5rem', margin: 0 }}>{agent.name}</h1>
        <a
          href={preservationAgentDownloadUrl(agent.id)}
          download
          style={{
            padding: '0.4rem 1rem',
            background: 'var(--digi-color-primary, #006991)',
            color: 'white',
            borderRadius: '2px',
            textDecoration: 'none',
            fontSize: '0.875rem',
          }}
        >
          Ladda ner
        </a>
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

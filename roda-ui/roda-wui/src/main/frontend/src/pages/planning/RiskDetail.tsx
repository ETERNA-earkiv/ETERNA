import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { getRisk, findRiskIncidences, type IndexedRisk, type IndexedRiskIncidence, type SeverityLevel } from '../../api/planning'
import { Breadcrumb } from '../../components/common/Breadcrumb'
import { DataTable, type Column } from '../../components/common/DataTable'
import { TabBar } from '../../components/common/TabBar'
import { Pagination } from '../../components/common/Pagination'
import { DigiTable } from '@designsystem-se/af-react'

type TabId = 'overview' | 'incidences'

const TABS = [
  { id: 'overview' as TabId, label: 'Översikt' },
  { id: 'incidences' as TabId, label: 'Incidenter' },
]

function severityLabel(level: SeverityLevel | null): string {
  if (!level) return '—'
  const labels: Record<SeverityLevel, string> = {
    LOW: 'Låg',
    MODERATE: 'Måttlig',
    HIGH: 'Hög',
    CRITICAL: 'Kritisk',
  }
  return labels[level]
}

function OverviewTab({ risk }: { risk: IndexedRisk }) {
  const fields = [
    { label: 'ID', value: risk.id },
    { label: 'Namn', value: risk.name },
    { label: 'Beskrivning', value: risk.description },
    { label: 'Kategorier', value: risk.categories?.join(', ') || null },
    { label: 'Identifierad', value: risk.identifiedOn ? new Date(risk.identifiedOn).toLocaleDateString('sv-SE') : null },
    { label: 'Identifierad av', value: risk.identifiedBy },
    { label: 'Anteckningar', value: risk.notes },
    { label: '— Pre-mitigering —', value: null, heading: true },
    { label: 'Sannolikhet', value: risk.preMitigationProbability },
    { label: 'Påverkan', value: risk.preMitigationImpact },
    { label: 'Allvarlighet', value: risk.preMitigationSeverity },
    { label: 'Allvarlighetsnivå', value: severityLabel(risk.preMitigationSeverityLevel) },
    { label: 'Anteckningar', value: risk.preMitigationNotes },
    { label: '— Post-mitigering —', value: null, heading: true },
    { label: 'Sannolikhet', value: risk.postMitigationProbability },
    { label: 'Påverkan', value: risk.postMitigationImpact },
    { label: 'Allvarlighet', value: risk.postMitigationSeverity },
    { label: 'Allvarlighetsnivå', value: severityLabel(risk.postMitigationSeverityLevel) },
    { label: 'Anteckningar', value: risk.postMitigationNotes },
    { label: '— Mitigeringsstrategi —', value: null, heading: true },
    { label: 'Strategi', value: risk.mitigationStrategy },
    { label: 'Ansvarig', value: risk.mitigationOwner },
    { label: 'Skapad', value: risk.createdOn ? new Date(risk.createdOn).toLocaleString('sv-SE') : null },
    { label: 'Skapad av', value: risk.createdBy },
    { label: 'Ändrad', value: risk.updatedOn ? new Date(risk.updatedOn).toLocaleString('sv-SE') : null },
    { label: 'Ändrad av', value: risk.updatedBy },
  ]

  return (
    <DigiTable>
      <table>
        <tbody>
          {fields.map(({ label, value, heading }, i) => heading ? (
            <tr key={i}>
              <td
                colSpan={2}
                style={{ padding: '0.75rem 0.5rem 0.25rem', fontWeight: 700, color: '#555', fontSize: '0.8rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}
              >
                {label}
              </td>
            </tr>
          ) : (
            <tr key={label + i}>
              <th style={{ width: '220px', textAlign: 'left', fontWeight: 600, padding: '0.5rem' }}>
                {label}
              </th>
              <td style={{ padding: '0.5rem' }}>{value ?? '—'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </DigiTable>
  )
}

function IncidencesTab({ riskId }: { riskId: string }) {
  const [page, setPage] = useState(0)
  const PAGE_SIZE = 20

  const { data, isLoading, error } = useQuery({
    queryKey: ['risk-incidences', riskId, page],
    queryFn: () => findRiskIncidences(riskId, page * PAGE_SIZE, PAGE_SIZE),
  })

  const incidences = data?.results ?? []
  const totalPages = Math.ceil((data?.totalCount ?? 0) / PAGE_SIZE)

  const columns: Column<IndexedRiskIncidence>[] = [
    {
      key: 'objectClass',
      header: 'Objekttyp',
      render: (i) => i.objectClass || '—',
      width: '110px',
    },
    {
      key: 'object',
      header: 'Objekt',
      render: (i) => (
        <span style={{ fontSize: '0.8rem', fontFamily: 'monospace' }}>
          {i.fileId || i.representationId || i.aipId || '—'}
        </span>
      ),
    },
    {
      key: 'severity',
      header: 'Allvarlighet',
      render: (i) => i.severity || '—',
      width: '100px',
    },
    {
      key: 'status',
      header: 'Status',
      render: (i) => (
        <span style={{ color: i.status === 'MITIGATED' ? '#2d7a3a' : '#c5221f', fontWeight: 600, fontSize: '0.8rem' }}>
          {i.status === 'MITIGATED' ? 'Mitigerad' : 'Ej mitigerad'}
        </span>
      ),
      width: '110px',
    },
    {
      key: 'detectedOn',
      header: 'Detekterad',
      render: (i) => i.detectedOn ? new Date(i.detectedOn).toLocaleDateString('sv-SE') : '—',
      width: '110px',
    },
    {
      key: 'description',
      header: 'Beskrivning',
      render: (i) => <span style={{ fontSize: '0.8rem', color: '#555' }}>{i.description || '—'}</span>,
    },
  ]

  return (
    <>
      <DataTable
        columns={columns}
        rows={incidences}
        rowKey={(i) => i.id}
        isLoading={isLoading}
        error={error?.message ?? null}
        emptyMessage="Inga incidenter hittades"
      />
      <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
    </>
  )
}

export function RiskDetail() {
  const { riskId } = useParams<{ riskId: string }>()
  const navigate = useNavigate()
  const [activeTab, setActiveTab] = useState<TabId>('overview')

  const { data: risk, isLoading, error } = useQuery({
    queryKey: ['risk', riskId],
    queryFn: () => getRisk(riskId!),
    enabled: !!riskId,
  })

  if (isLoading) return <DataTable columns={[]} rows={[]} rowKey={() => ''} isLoading />
  if (error || !risk) {
    return (
      <div style={{ color: '#c5221f', padding: '2rem' }}>
        Kunde inte hämta risk.{' '}
        <button
          onClick={() => navigate(-1)}
          style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--digi-color-primary, #006991)' }}
        >
          Tillbaka
        </button>
      </div>
    )
  }

  return (
    <div>
      <Breadcrumb
        items={[
          { label: 'Planering', to: '/planning' },
          { label: risk.name },
        ]}
      />

      <h1 style={{ fontSize: '1.5rem', marginBottom: '1.5rem' }}>{risk.name}</h1>

      <TabBar tabs={TABS} activeTab={activeTab} onTabChange={(id) => setActiveTab(id as TabId)} />

      {activeTab === 'overview' && <OverviewTab risk={risk} />}
      {activeTab === 'incidences' && <IncidencesTab riskId={risk.id} />}
    </div>
  )
}

import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { getMetrics } from '../api/statistics'

interface StatCardProps {
  label: string
  value: string | number | undefined
  to?: string
  color?: string
}

function StatCard({ label, value, to, color = '#006991' }: StatCardProps) {
  const inner = (
    <div
      style={{
        background: 'white',
        border: '1px solid #e0e0e0',
        borderRadius: '4px',
        padding: '1.25rem 1.5rem',
        display: 'flex',
        flexDirection: 'column',
        gap: '0.25rem',
      }}
    >
      <div style={{ fontSize: '2rem', fontWeight: 700, color, lineHeight: 1.2 }}>
        {value ?? '—'}
      </div>
      <div style={{ fontSize: '0.875rem', color: '#555' }}>{label}</div>
    </div>
  )

  if (to) {
    return (
      <Link to={to} style={{ textDecoration: 'none' }}>
        {inner}
      </Link>
    )
  }
  return inner
}

export function StatisticsPage() {
  const { data, isLoading } = useQuery({
    queryKey: ['metrics'],
    queryFn: getMetrics,
    staleTime: 60_000,
  })

  const m = data?.metricList ?? {}

  const sections = [
    {
      title: 'Arkivbestånd',
      cards: [
        { label: 'Arkivobjekt (AIP)', key: 'aipTotal', to: '/browse' },
        { label: 'Representationer', key: 'representationTotal' },
        { label: 'Filer', key: 'fileTotal' },
        { label: 'DIP:ar', key: 'dipTotal', to: '/browse' },
        { label: 'Bevarandehändelser', key: 'preservationEventTotal' },
      ],
    },
    {
      title: 'Gallring',
      cards: [
        { label: 'Gallringsplaner', key: 'disposalScheduleTotal', to: '/disposal' },
        { label: 'Gallringsstopp', key: 'disposalHoldTotal', to: '/disposal' },
        { label: 'Bekräftelser', key: 'disposalConfirmationTotal', to: '/disposal' },
      ],
    },
    {
      title: 'Planering',
      cards: [
        { label: 'Risker', key: 'riskTotal', to: '/planning' },
        { label: 'Riskincidenter', key: 'riskIncidenceTotal', to: '/planning' },
        { label: 'Representationsinformation', key: 'representationInformationTotal', to: '/planning' },
      ],
    },
    {
      title: 'Processer',
      cards: [
        { label: 'Jobb', key: 'jobTotal', to: '/jobs' },
      ],
    },
  ]

  return (
    <div>
      <h1 style={{ fontSize: '1.5rem', marginBottom: '1.5rem' }}>Statistik</h1>

      {isLoading ? (
        <div style={{ color: '#555' }}>Laddar statistik…</div>
      ) : (
        sections.map((section) => (
          <section key={section.title} style={{ marginBottom: '2rem' }}>
            <h2 style={{ fontSize: '1rem', fontWeight: 600, color: '#555', marginBottom: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              {section.title}
            </h2>
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))',
                gap: '0.75rem',
              }}
            >
              {section.cards.map((card) => (
                <StatCard
                  key={card.key}
                  label={card.label}
                  value={m[card.key]}
                  to={card.to}
                />
              ))}
            </div>
          </section>
        ))
      )}
    </div>
  )
}

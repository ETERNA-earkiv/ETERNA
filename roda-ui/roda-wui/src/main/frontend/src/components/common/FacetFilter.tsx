import type { FacetResult } from '../../types'

interface FacetFilterProps {
  facets: FacetResult[]
  activeFilters: Record<string, string>
  labels?: Record<string, string>
  onToggle: (field: string, value: string) => void
  onClear: (field: string) => void
}

const BUTTON_ACTIVE = {
  borderColor: 'var(--digi-color-primary, #006991)',
  background: 'var(--digi-color-primary, #006991)',
  color: 'white',
}

const BUTTON_INACTIVE = {
  borderColor: '#ccc',
  background: 'white',
  color: '#555',
}

const BUTTON_BASE: React.CSSProperties = {
  padding: '0.2rem 0.65rem',
  borderRadius: '2px',
  border: '1px solid',
  fontSize: '0.8rem',
  cursor: 'pointer',
  marginRight: '0.3rem',
}

export function FacetFilter({ facets, activeFilters, labels = {}, onToggle, onClear }: FacetFilterProps) {
  const visibleFacets = facets.filter((f) => f.values.length > 0)

  if (visibleFacets.length === 0) return null

  return (
    <div style={{ marginBottom: '1.25rem', display: 'flex', flexWrap: 'wrap', gap: '1.25rem' }}>
      {visibleFacets.map((facet) => (
        <div key={facet.field}>
          <span style={{ fontSize: '0.8125rem', fontWeight: 600, color: '#444', marginRight: '0.5rem' }}>
            {labels[facet.field] ?? facet.field}:
          </span>
          <button
            onClick={() => onClear(facet.field)}
            style={{
              ...BUTTON_BASE,
              ...(!activeFilters[facet.field] ? BUTTON_ACTIVE : BUTTON_INACTIVE),
            }}
          >
            Alla
          </button>
          {facet.values.map((fv) => (
            <button
              key={fv.value}
              onClick={() => onToggle(facet.field, fv.value)}
              style={{
                ...BUTTON_BASE,
                ...(activeFilters[facet.field] === fv.value ? BUTTON_ACTIVE : BUTTON_INACTIVE),
              }}
            >
              {fv.value} ({fv.count})
            </button>
          ))}
        </div>
      ))}
    </div>
  )
}

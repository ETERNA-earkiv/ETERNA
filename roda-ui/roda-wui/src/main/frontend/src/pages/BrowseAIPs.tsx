import { useNavigate } from 'react-router-dom'
import { DigiFormInputSearch } from '@designsystem-se/af-react'
import { useFindRequest } from '../hooks/useFindRequest'
import { DataTable, type Column } from '../components/common/DataTable'
import { FacetFilter } from '../components/common/FacetFilter'
import { Pagination } from '../components/common/Pagination'
import { StatusBadge } from '../components/common/StatusBadge'
import { useTranslation } from '../i18n/TranslationContext'
import type { IndexedAIP } from '../api/aips'
import type { FacetResult, FilterParameter } from '../types'

type InputSearchEl = HTMLElement & { afValue: string }

const FACET_LABELS: Record<string, string> = {
  state: 'Tillstånd',
  level: 'Nivå',
}

export function BrowseAIPs() {
  const navigate = useNavigate()
  const { t } = useTranslation()

  const { data: aips, totalCount, facets, isLoading, error, page, pageSize, setPage, filters, setFilters, query, setQuery } =
    useFindRequest<IndexedAIP>({
      endpoint: '/aips/find',
      queryKey: 'aips',
      defaultSort: [{ name: 'dateModified', descending: true }],
      facetFields: ['state', 'level'],
      onlyActive: true,
    })

  const totalPages = Math.ceil(totalCount / pageSize)
  const facetResults: FacetResult[] = (facets as FacetResult[] | null) ?? []

  const activeFilters: Record<string, string> = filters.reduce<Record<string, string>>(
    (acc, f) => ({ ...acc, [f.name]: f.value }),
    {}
  )

  function toggleFilter(field: string, value: string) {
    const existing = filters.find((f) => f.name === field)
    if (existing?.value === value) {
      setFilters(filters.filter((f) => f.name !== field))
    } else {
      const next = filters.filter((f) => f.name !== field)
      next.push({ type: 'SimpleFilterParameter', name: field, value } as FilterParameter)
      setFilters(next)
    }
  }

  function clearFilter(field: string) {
    setFilters(filters.filter((f) => f.name !== field))
  }

  const columns: Column<IndexedAIP>[] = [
    {
      key: 'title',
      header: t('label.title', 'Titel'),
      render: (aip) => (
        <strong style={{ color: 'var(--digi-color-primary, #006991)' }}>
          {aip.title || aip.id}
        </strong>
      ),
    },
    {
      key: 'level',
      header: t('label.level', 'Nivå'),
      render: (aip) => aip.level || '—',
      width: '120px',
    },
    {
      key: 'state',
      header: t('label.state', 'Status'),
      render: (aip) => <StatusBadge state={aip.state} />,
      width: '100px',
    },
    {
      key: 'dateCreated',
      header: t('label.created', 'Skapad'),
      render: (aip) =>
        aip.dateCreated ? new Date(aip.dateCreated).toLocaleDateString('sv-SE') : '—',
      width: '140px',
    },
  ]

  return (
    <div>
      <h1 style={{ marginBottom: '1.5rem' }}>{t('aip.title', 'Arkivobjekt')}</h1>

      <div style={{ display: 'flex', gap: '0.75rem', marginBottom: '1.5rem', alignItems: 'flex-end' }}>
        <div style={{ flex: 1 }}>
          <DigiFormInputSearch
            afLabel={t('aip.search.label', 'Sök arkivobjekt')}
            afId="search-aips"
            afValue={query}
            onAfOnInput={(e) => setQuery((e.target as InputSearchEl).afValue)}
            onAfOnSubmitSearch={(e) => setQuery(e.detail as string || query)}
          />
        </div>
      </div>

      <FacetFilter
        facets={facetResults}
        activeFilters={activeFilters}
        labels={FACET_LABELS}
        onToggle={toggleFilter}
        onClear={clearFilter}
      />

      <DataTable
        columns={columns}
        rows={aips}
        rowKey={(aip) => aip.id}
        isLoading={isLoading}
        error={error?.message ?? null}
        emptyMessage={t('aip.none', 'Inga arkivobjekt hittades')}
        onRowClick={(aip) => navigate(`/browse/${aip.id}`)}
        summary={
          !isLoading && !error
            ? `Visar ${aips.length} av ${totalCount} arkivobjekt`
            : undefined
        }
      />

      <Pagination
        page={page}
        totalPages={totalPages}
        onPageChange={setPage}
        resetKey={`${query}-${JSON.stringify(activeFilters)}`}
      />
    </div>
  )
}

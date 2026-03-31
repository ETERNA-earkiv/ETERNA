import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { api } from '../../api/client'
import type { IndexResult } from '../../types'
import { DataTable, type Column } from '../../components/common/DataTable'
import { Pagination } from '../../components/common/Pagination'
import { Breadcrumb } from '../../components/common/Breadcrumb'

interface DestroyedAIP {
  id: string
  title: string
  level: string
  dateModified: string | null
  disposalScheduleName: string | null
  disposalConfirmationId: string | null
}

function formatDate(iso: string | null): string {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('sv-SE')
}

const PAGE_SIZE = 20

export function DestroyedRecordsPage() {
  const [page, setPage] = useState(0)

  const { data, isLoading, error } = useQuery({
    queryKey: ['destroyed-aips', page],
    queryFn: () =>
      api.post<IndexResult<DestroyedAIP>>('/aips/find', {
        filter: {
          filterParameters: [
            { type: 'SimpleFilterParameter', name: 'state', value: 'DESTROYED' },
          ],
        },
        sublist: { firstElementIndex: page * PAGE_SIZE, maximumElementCount: PAGE_SIZE },
        sorter: { parameters: [{ name: 'dateModified', descending: true }] },
        facets: { parameters: {} },
      }),
  })

  const records = data?.results ?? []
  const totalCount = data?.totalCount ?? 0
  const totalPages = Math.ceil(totalCount / PAGE_SIZE)

  const columns: Column<DestroyedAIP>[] = [
    {
      key: 'title',
      header: 'Titel',
      render: (r) => (
        <Link
          to={`/browse/${r.id}`}
          style={{ color: 'var(--digi-color-primary, #006991)' }}
        >
          {r.title || r.id}
        </Link>
      ),
    },
    {
      key: 'level',
      header: 'Nivå',
      width: '120px',
      render: (r) => r.level,
    },
    {
      key: 'disposalScheduleName',
      header: 'Gallringsschema',
      width: '200px',
      render: (r) =>
        r.disposalConfirmationId ? (
          <Link
            to={`/disposal/confirmations/${r.disposalConfirmationId}`}
            style={{ color: 'var(--digi-color-primary, #006991)', fontSize: '0.85rem' }}
          >
            {r.disposalScheduleName || r.disposalConfirmationId}
          </Link>
        ) : (
          r.disposalScheduleName || '—'
        ),
    },
    {
      key: 'dateModified',
      header: 'Gallringsdatum',
      width: '160px',
      render: (r) => <span style={{ fontSize: '0.8rem' }}>{formatDate(r.dateModified)}</span>,
    },
  ]

  return (
    <div>
      <Breadcrumb
        items={[
          { label: 'Gallring', to: '/disposal' },
          { label: 'Förstörda poster' },
        ]}
      />
      <h1 style={{ marginBottom: '1.5rem' }}>Förstörda poster</h1>

      <DataTable
        columns={columns}
        rows={records}
        rowKey={(r) => r.id}
        isLoading={isLoading}
        error={error?.message ?? null}
        emptyMessage="Inga förstörda poster hittades"
        summary={!isLoading && !error ? `${totalCount} poster totalt` : undefined}
      />

      <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
    </div>
  )
}

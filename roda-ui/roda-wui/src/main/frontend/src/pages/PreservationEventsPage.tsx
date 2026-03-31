import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { findPreservationEvents, type IndexedPreservationEvent } from '../api/preservation-events'
import { DataTable, type Column } from '../components/common/DataTable'
import { StatusBadge } from '../components/common/StatusBadge'
import { Pagination } from '../components/common/Pagination'
import { DigiFormInputSearch } from '@designsystem-se/af-react'
import { api } from '../api/client'
import type { IndexResult } from '../types'

const PAGE_SIZE = 20

type InputSearchEl = HTMLElement & { afValue: string }

function usePreservationEventsSearch(query: string, page: number) {
  return useQuery({
    queryKey: ['all-preservation-events', query, page],
    queryFn: () => {
      if (query) {
        return api.post<IndexResult<IndexedPreservationEvent>>('/preservation/events/find', {
          filter: {
            filterParameters: [{ type: 'SimpleFilterParameter', name: 'fulltext', value: query }],
          },
          sublist: { firstElementIndex: page * PAGE_SIZE, maximumElementCount: PAGE_SIZE },
          sorter: { parameters: [{ name: 'eventDateTime', descending: true }] },
          facets: { parameters: {} },
        })
      }
      return findPreservationEvents({}, page * PAGE_SIZE, PAGE_SIZE)
    },
  })
}

export function PreservationEventsPage() {
  const [page, setPage] = useState(0)
  const [input, setInput] = useState('')
  const [query, setQuery] = useState('')

  function submit(value: string) {
    setQuery(value.trim())
    setPage(0)
  }

  const { data, isLoading, error } = usePreservationEventsSearch(query, page)

  const events = data?.results ?? []
  const totalPages = Math.ceil((data?.totalCount ?? 0) / PAGE_SIZE)

  const columns: Column<IndexedPreservationEvent>[] = [
    {
      key: 'datetime',
      header: 'Tidpunkt',
      render: (e) => (
        <span style={{ fontSize: '0.85rem' }}>
          {new Date(e.eventDateTime).toLocaleString('sv-SE')}
        </span>
      ),
      width: '160px',
    },
    {
      key: 'type',
      header: 'Händelsetyp',
      render: (e) => e.eventType,
    },
    {
      key: 'outcome',
      header: 'Utfall',
      render: (e) => <StatusBadge state={e.eventOutcome} />,
      width: '100px',
    },
    {
      key: 'objectClass',
      header: 'Objekttyp',
      render: (e) => e.objectClass || '—',
      width: '100px',
    },
    {
      key: 'aip',
      header: 'AIP',
      render: (e) => (
        <span style={{ fontSize: '0.8rem', fontFamily: 'monospace' }}>{e.aipID}</span>
      ),
    },
    {
      key: 'detail',
      header: 'Detalj',
      render: (e) => (
        <span style={{ fontSize: '0.8rem', color: '#555' }}>
          {e.eventOutcomeDetailNote || e.eventDetail || '—'}
        </span>
      ),
    },
  ]

  return (
    <div>
      <h1 style={{ fontSize: '1.5rem', marginBottom: '1.5rem' }}>Bevarandehändelser</h1>

      <div style={{ maxWidth: '500px', marginBottom: '1.5rem' }}>
        <DigiFormInputSearch
          afLabel="Sök händelser"
          afId="search-events"
          afValue={input}
          onAfOnInput={(e) => setInput((e.target as InputSearchEl).afValue)}
          onAfOnSubmitSearch={(e) => submit(e.detail as string || input)}
        />
      </div>

      <DataTable
        columns={columns}
        rows={events}
        rowKey={(e) => e.id}
        isLoading={isLoading}
        error={error?.message ?? null}
        emptyMessage="Inga bevarandehändelser hittades"
        summary={!isLoading && data?.totalCount != null ? `${data.totalCount} händelser totalt` : undefined}
      />
      <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
    </div>
  )
}

import { DigiFormInputSearch } from '@designsystem-se/af-react'
import { useFindRequest } from '../hooks/useFindRequest'
import { DataTable, type Column } from '../components/common/DataTable'
import { Pagination } from '../components/common/Pagination'
import { StatusBadge } from '../components/common/StatusBadge'
import { useTranslation } from '../i18n/TranslationContext'
import type { LogEntry } from '../api/auditlog'

type InputSearchEl = HTMLElement & { afValue: string }

export function AuditLogPage() {
  const { t } = useTranslation()

  const { data: entries, totalCount, isLoading, error, page, pageSize, setPage, query, setQuery } =
    useFindRequest<LogEntry>({
      endpoint: '/audit-logs',
      queryKey: 'audit-logs',
      defaultSort: [{ name: 'datetime', descending: true }],
    })

  const totalPages = Math.ceil(totalCount / pageSize)

  const columns: Column<LogEntry>[] = [
    {
      key: 'datetime',
      header: 'Tidpunkt',
      render: (e) => (
        <span style={{ fontSize: '0.8rem', whiteSpace: 'nowrap' }}>
          {new Date(e.datetime).toLocaleString('sv-SE')}
        </span>
      ),
      width: '160px',
    },
    {
      key: 'username',
      header: t('label.username', 'Användare'),
      render: (e) => e.username,
      width: '140px',
    },
    {
      key: 'action',
      header: 'Åtgärd',
      render: (e) => (
        <span style={{ fontSize: '0.8rem' }}>
          {e.actionComponent}.{e.actionMethod}
        </span>
      ),
    },
    {
      key: 'relatedObject',
      header: 'Objekt-ID',
      render: (e) => (
        <span
          style={{
            fontFamily: 'monospace',
            fontSize: '0.75rem',
            display: 'block',
            maxWidth: '200px',
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            whiteSpace: 'nowrap',
          }}
        >
          {e.relatedObjectID || '—'}
        </span>
      ),
      width: '200px',
    },
    {
      key: 'state',
      header: t('label.state', 'Status'),
      render: (e) => <StatusBadge state={e.state} />,
      width: '100px',
    },
    {
      key: 'duration',
      header: 'ms',
      render: (e) => <span style={{ fontSize: '0.8rem' }}>{e.duration}</span>,
      width: '70px',
    },
  ]

  return (
    <div>
      <h1 style={{ marginBottom: '1.5rem' }}>Granskningslogg</h1>

      <div style={{ marginBottom: '1.5rem' }}>
        <DigiFormInputSearch
          afLabel={t('audit.search.label', 'Sök i logg')}
          afId="search-audit"
          afValue={query}
          onAfOnInput={(e) => setQuery((e.target as InputSearchEl).afValue)}
          onAfOnSubmitSearch={(e) => setQuery(e.detail as string || query)}
        />
      </div>

      <DataTable
        columns={columns}
        rows={entries}
        rowKey={(e) => e.uuid}
        isLoading={isLoading}
        error={error?.message ?? null}
        emptyMessage={t('audit.none', 'Inga loggposter hittades')}
        summary={!isLoading && !error ? `Visar ${entries.length} av ${totalCount} poster` : undefined}
      />

      <Pagination
        page={page}
        totalPages={totalPages}
        onPageChange={setPage}
        resetKey={`${query}-${page}`}
      />
    </div>
  )
}

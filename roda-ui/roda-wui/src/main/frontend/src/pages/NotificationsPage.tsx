import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { listNotifications, type Notification } from '../api/notifications'
import { DataTable, type Column } from '../components/common/DataTable'
import { Pagination } from '../components/common/Pagination'
import { DigiNotificationAlert } from '@designsystem-se/af-react'

function formatDate(iso: string | null): string {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('sv-SE')
}

function AcknowledgedBadge({ acked }: { acked: boolean }) {
  return (
    <span
      style={{
        display: 'inline-block',
        padding: '0.15rem 0.5rem',
        borderRadius: '3px',
        fontSize: '0.75rem',
        fontWeight: 600,
        background: acked ? '#d1fae5' : '#fef3c7',
        color: acked ? '#065f46' : '#92400e',
      }}
    >
      {acked ? 'Bekräftad' : 'Ej bekräftad'}
    </span>
  )
}

function NotificationRow({ notification, onSelect }: { notification: Notification; onSelect: (n: Notification) => void }) {
  return (
    <button
      onClick={() => onSelect(notification)}
      style={{
        background: 'none',
        border: 'none',
        padding: 0,
        cursor: 'pointer',
        color: 'var(--digi-color-primary, #006991)',
        fontSize: '0.875rem',
        textAlign: 'left',
      }}
    >
      {notification.subject || '(Inget ämne)'}
    </button>
  )
}

function NotificationDetail({ notification, onClose }: { notification: Notification; onClose: () => void }) {
  return (
    <div
      style={{
        border: '1px solid #e5e7eb',
        borderRadius: '4px',
        padding: '1.5rem',
        marginBottom: '1.5rem',
        background: '#fff',
        position: 'relative',
      }}
    >
      <button
        onClick={onClose}
        style={{
          position: 'absolute',
          top: '1rem',
          right: '1rem',
          background: 'none',
          border: 'none',
          cursor: 'pointer',
          fontSize: '1.2rem',
          color: '#666',
        }}
        aria-label="Stäng"
      >
        ×
      </button>
      <h2 style={{ margin: '0 0 1rem', fontSize: '1.1rem' }}>{notification.subject || '(Inget ämne)'}</h2>
      <table style={{ borderCollapse: 'collapse', width: '100%', marginBottom: '1rem' }}>
        <tbody>
          <tr>
            <td style={{ padding: '0.25rem 0.75rem 0.25rem 0', fontWeight: 600, width: '160px', verticalAlign: 'top' }}>Från</td>
            <td style={{ padding: '0.25rem 0' }}>{notification.fromUser || '—'}</td>
          </tr>
          <tr>
            <td style={{ padding: '0.25rem 0.75rem 0.25rem 0', fontWeight: 600, verticalAlign: 'top' }}>Mottagare</td>
            <td style={{ padding: '0.25rem 0' }}>{notification.recipientUsers?.join(', ') || '—'}</td>
          </tr>
          <tr>
            <td style={{ padding: '0.25rem 0.75rem 0.25rem 0', fontWeight: 600, verticalAlign: 'top' }}>Skickat</td>
            <td style={{ padding: '0.25rem 0' }}>{formatDate(notification.sentOn)}</td>
          </tr>
          <tr>
            <td style={{ padding: '0.25rem 0.75rem 0.25rem 0', fontWeight: 600, verticalAlign: 'top' }}>Status</td>
            <td style={{ padding: '0.25rem 0' }}><AcknowledgedBadge acked={notification.isAcknowledged} /></td>
          </tr>
        </tbody>
      </table>
      {notification.body && (
        <div
          style={{
            borderTop: '1px solid #e5e7eb',
            paddingTop: '1rem',
            fontSize: '0.9rem',
            whiteSpace: 'pre-wrap',
            lineHeight: 1.6,
          }}
        >
          {notification.body}
        </div>
      )}
    </div>
  )
}

export function NotificationsPage() {
  const PAGE_SIZE = 20
  const [page, setPage] = useState(0)
  const [selected, setSelected] = useState<Notification | null>(null)

  const { data, isLoading, error } = useQuery({
    queryKey: ['notifications', page],
    queryFn: () => listNotifications(page * PAGE_SIZE, PAGE_SIZE),
  })

  const notifications = data?.results ?? []
  const totalCount = data?.totalCount ?? 0
  const totalPages = Math.ceil(totalCount / PAGE_SIZE)

  const columns: Column<Notification>[] = [
    {
      key: 'subject',
      header: 'Ämne',
      render: (n) => <NotificationRow notification={n} onSelect={setSelected} />,
    },
    {
      key: 'fromUser',
      header: 'Från',
      width: '160px',
      render: (n) => n.fromUser || '—',
    },
    {
      key: 'sentOn',
      header: 'Skickat',
      width: '160px',
      render: (n) => <span style={{ fontSize: '0.8rem' }}>{formatDate(n.sentOn)}</span>,
    },
    {
      key: 'isAcknowledged',
      header: 'Status',
      width: '130px',
      render: (n) => <AcknowledgedBadge acked={n.isAcknowledged} />,
    },
  ]

  return (
    <div>
      <h1 style={{ marginBottom: '1.5rem' }}>Notifieringar</h1>

      {error && (
        <DigiNotificationAlert afVariation="danger" afHeading="Fel" style={{ marginBottom: '1rem' }}>
          {error.message}
        </DigiNotificationAlert>
      )}

      {selected && (
        <NotificationDetail notification={selected} onClose={() => setSelected(null)} />
      )}

      <DataTable
        columns={columns}
        rows={notifications}
        rowKey={(n) => n.id}
        isLoading={isLoading}
        error={null}
        emptyMessage="Inga notifieringar hittades"
        summary={!isLoading && !error ? `${totalCount} notifieringar totalt` : undefined}
      />

      <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
    </div>
  )
}

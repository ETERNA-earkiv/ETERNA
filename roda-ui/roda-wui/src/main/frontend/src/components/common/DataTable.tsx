import { DigiTable, DigiLoaderSpinner, DigiNotificationAlert } from '@designsystem-se/af-react'

export interface Column<T> {
  key: string
  header: string
  render: (row: T) => React.ReactNode
  width?: string
}

interface DataTableProps<T> {
  columns: Column<T>[]
  rows: T[]
  rowKey: (row: T) => string
  isLoading?: boolean
  error?: string | null
  emptyMessage?: string
  onRowClick?: (row: T) => void
  summary?: React.ReactNode
}

export function DataTable<T>({
  columns,
  rows,
  rowKey,
  isLoading,
  error,
  emptyMessage = 'Inga poster hittades',
  onRowClick,
  summary,
}: DataTableProps<T>) {
  if (isLoading) {
    return (
      <div style={{ textAlign: 'center', padding: '2rem' }}>
        <DigiLoaderSpinner />
      </div>
    )
  }

  if (error) {
    return (
      <DigiNotificationAlert afVariation="danger" afHeading="Fel">
        {error}
      </DigiNotificationAlert>
    )
  }

  return (
    <>
      {summary && (
        <p style={{ marginBottom: '0.75rem', color: '#555', fontSize: '0.875rem' }}>
          {summary}
        </p>
      )}
      <DigiTable>
        <table>
          <thead>
            <tr>
              {columns.map((col) => (
                <th key={col.key} style={col.width ? { width: col.width } : undefined}>
                  {col.header}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {rows.length === 0 ? (
              <tr>
                <td colSpan={columns.length} style={{ textAlign: 'center', padding: '2rem' }}>
                  {emptyMessage}
                </td>
              </tr>
            ) : (
              rows.map((row) => (
                <tr
                  key={rowKey(row)}
                  style={onRowClick ? { cursor: 'pointer' } : undefined}
                  onClick={onRowClick ? () => onRowClick(row) : undefined}
                >
                  {columns.map((col) => (
                    <td key={col.key}>{col.render(row)}</td>
                  ))}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </DigiTable>
    </>
  )
}

import { DigiNavigationPagination } from '@designsystem-se/af-react'

interface PaginationProps {
  page: number
  totalPages: number
  onPageChange: (page: number) => void
  resetKey?: string
}

export function Pagination({ page, totalPages, onPageChange, resetKey }: PaginationProps) {
  if (totalPages <= 1) return null

  return (
    <div style={{ display: 'flex', justifyContent: 'center', marginTop: '1.5rem' }}>
      <DigiNavigationPagination
        key={resetKey ?? String(totalPages)}
        afInitActivePage={page + 1}
        afTotalPages={totalPages}
        onAfOnPageChange={(e) => {
          onPageChange((e.detail as number) - 1)
          window.scrollTo(0, 0)
        }}
      />
    </div>
  )
}

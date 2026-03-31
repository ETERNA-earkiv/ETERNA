import { Link } from 'react-router-dom'

interface BreadcrumbItem {
  label: string
  to?: string
}

interface BreadcrumbProps {
  items: BreadcrumbItem[]
}

export function Breadcrumb({ items }: BreadcrumbProps) {
  return (
    <nav aria-label="Brödsmulor" style={{ marginBottom: '1.5rem', fontSize: '0.875rem', color: '#555' }}>
      {items.map((item, idx) => (
        <span key={idx}>
          {idx > 0 && <span style={{ margin: '0 0.4rem' }}>/</span>}
          {item.to ? (
            <Link to={item.to} style={{ color: 'var(--digi-color-primary, #006991)' }}>
              {item.label}
            </Link>
          ) : (
            <span style={{ color: '#333' }}>{item.label}</span>
          )}
        </span>
      ))}
    </nav>
  )
}

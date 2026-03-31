import { useState, useRef, useEffect } from 'react'
import { Outlet, NavLink, Link, useNavigate } from 'react-router-dom'
import { DigiButton } from '@designsystem-se/af-react'
import { useAuth } from './AuthProvider'
import { logout } from '../api/auth'

interface NavGroup {
  label: string
  items: { to: string; label: string }[]
}

const NAV_GROUPS: NavGroup[] = [
  {
    label: 'Arkiv',
    items: [
      { to: '/browse', label: 'Arkivobjekt' },
      { to: '/search', label: 'Sök' },
      { to: '/ingest', label: 'Ingest' },
      { to: '/preservation-events', label: 'Bevarandehändelser' },
    ],
  },
  {
    label: 'Gallring',
    items: [
      { to: '/disposal', label: 'Gallring' },
    ],
  },
  {
    label: 'Planering',
    items: [
      { to: '/planning', label: 'Planering' },
    ],
  },
  {
    label: 'Administration',
    items: [
      { to: '/jobs', label: 'Jobb' },
      { to: '/audit', label: 'Logg' },
      { to: '/notifications', label: 'Notifieringar' },
      { to: '/management/users', label: 'Användare' },
    ],
  },
]

interface DropdownMenuProps {
  group: NavGroup
}

function DropdownMenu({ group }: DropdownMenuProps) {
  const [open, setOpen] = useState(false)
  const ref = useRef<HTMLDivElement>(null)

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) {
        setOpen(false)
      }
    }
    if (open) document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [open])

  // Single-item groups render as a plain NavLink instead of a dropdown
  if (group.items.length === 1) {
    return (
      <NavLink
        to={group.items[0].to}
        style={({ isActive }) => ({
          color: 'white',
          textDecoration: isActive ? 'underline' : 'none',
          fontWeight: isActive ? 600 : 400,
          fontSize: '0.9rem',
        })}
      >
        {group.items[0].label}
      </NavLink>
    )
  }

  return (
    <div ref={ref} style={{ position: 'relative' }}>
      <button
        onClick={() => setOpen((v) => !v)}
        style={{
          background: 'none',
          border: 'none',
          color: 'white',
          cursor: 'pointer',
          fontSize: '0.9rem',
          padding: '0.25rem 0',
          display: 'flex',
          alignItems: 'center',
          gap: '0.25rem',
        }}
      >
        {group.label}
        <span style={{ fontSize: '0.6rem', opacity: 0.8 }}>{open ? '▲' : '▼'}</span>
      </button>

      {open && (
        <div
          style={{
            position: 'absolute',
            top: 'calc(100% + 0.5rem)',
            left: 0,
            background: 'white',
            border: '1px solid #e0e0e0',
            borderRadius: '4px',
            boxShadow: '0 4px 12px rgba(0,0,0,0.12)',
            minWidth: '180px',
            zIndex: 100,
          }}
        >
          {group.items.map(({ to, label }) => (
            <NavLink
              key={to}
              to={to}
              onClick={() => setOpen(false)}
              style={({ isActive }) => ({
                display: 'block',
                padding: '0.625rem 1rem',
                color: isActive ? 'var(--digi-color-primary, #006991)' : '#333',
                textDecoration: 'none',
                fontWeight: isActive ? 600 : 400,
                fontSize: '0.875rem',
              })}
            >
              {label}
            </NavLink>
          ))}
        </div>
      )}
    </div>
  )
}

export function AppShell() {
  const { user, setUser } = useAuth()
  const navigate = useNavigate()

  async function handleLogout() {
    await logout().catch(() => {})
    setUser(null)
    navigate('/login')
  }

  return (
    <div>
      <header
        style={{
          background: 'var(--digi-color-primary, #006991)',
          color: 'white',
          padding: '0 1.5rem',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          minHeight: '3.5rem',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '1.5rem' }}>
          <NavLink
            to="/statistics"
            style={{ color: 'white', textDecoration: 'none', fontWeight: 700, fontSize: '1.125rem' }}
          >
            ETERNA
          </NavLink>
          <nav style={{ display: 'flex', gap: '1.25rem', alignItems: 'center' }}>
            {NAV_GROUPS.map((group) => (
              <DropdownMenu key={group.label} group={group} />
            ))}
          </nav>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          {user && (
            <Link
              to="/profile"
              style={{ color: 'white', fontSize: '0.875rem', textDecoration: 'none' }}
              title="Min profil"
            >
              {user.fullName || user.name}
            </Link>
          )}
          <DigiButton
            afVariation="secondary"
            onAfOnClick={handleLogout}
            style={{ '--digi-button-color': 'white', '--digi-button-border-color': 'white' } as React.CSSProperties}
          >
            Logga ut
          </DigiButton>
        </div>
      </header>

      <main style={{ padding: '1.5rem 2rem', maxWidth: '1280px', margin: '0 auto' }}>
        <Outlet />
      </main>
    </div>
  )
}

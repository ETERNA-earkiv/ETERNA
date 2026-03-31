import { Outlet, NavLink, useNavigate } from 'react-router-dom'
import { DigiButton } from '@designsystem-se/af-react'
import { useAuth } from './AuthProvider'
import { logout } from '../api/auth'

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
        <div style={{ display: 'flex', alignItems: 'center', gap: '2rem' }}>
          <strong style={{ fontSize: '1.125rem' }}>ETERNA</strong>
          <nav style={{ display: 'flex', gap: '1.5rem' }}>
            {[
              { to: '/statistics', label: 'Statistik' },
              { to: '/browse', label: 'Arkivobjekt' },
              { to: '/ingest', label: 'Ingest' },
              { to: '/planning', label: 'Planering' },
              { to: '/disposal', label: 'Gallring' },
              { to: '/jobs', label: 'Jobb' },
              { to: '/audit', label: 'Logg' },
              { to: '/management/users', label: 'Användare' },
            ].map(({ to, label }) => (
              <NavLink
                key={to}
                to={to}
                style={({ isActive }) => ({
                  color: 'white',
                  textDecoration: isActive ? 'underline' : 'none',
                  fontWeight: isActive ? 600 : 400,
                })}
              >
                {label}
              </NavLink>
            ))}
          </nav>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          {user && (
            <span style={{ fontSize: '0.875rem' }}>
              {user.fullName || user.name}
            </span>
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

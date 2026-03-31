import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { DigiLoaderSpinner } from '@designsystem-se/af-react'
import { useAuth } from './AuthProvider'

interface PrivateRouteProps {
  children: ReactNode
  requiredRoles?: string[]
}

export function PrivateRoute({ children, requiredRoles }: PrivateRouteProps) {
  const { user, loading } = useAuth()

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', padding: '2rem' }}>
        <DigiLoaderSpinner />
      </div>
    )
  }

  if (!user) {
    return <Navigate to="/login" replace />
  }

  if (requiredRoles && requiredRoles.length > 0) {
    const hasRole = requiredRoles.some((r) => user.allRoles?.includes(r))
    if (!hasRole) {
      return (
        <div style={{ textAlign: 'center', padding: '3rem', color: '#c5221f' }}>
          <h2>Åtkomst nekad</h2>
          <p>Du har inte behörighet till den här sidan.</p>
        </div>
      )
    }
  }

  return <>{children}</>
}

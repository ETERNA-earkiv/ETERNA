import { useAuth } from '../AuthProvider'

// Well-known RODA role constants
export const ROLES = {
  ADMIN: 'administrator',
  INGEST_TRANSFER: 'ingest.transfer',
  INGEST_CREATE: 'ingest.create',
  DISPOSAL_MANAGE: 'disposal.manage',
  USER_MANAGE: 'user.management',
  REPORTING: 'reporting',
  RISKS_MANAGE: 'risks.manage',
} as const

interface RoleGateProps {
  roles: string[]
  requireAll?: boolean
  fallback?: React.ReactNode
  children: React.ReactNode
}

/**
 * Renders children only if the authenticated user has the required role(s).
 * By default requires at least one matching role (OR logic).
 * Set requireAll=true for AND logic.
 */
export function RoleGate({ roles, requireAll = false, fallback = null, children }: RoleGateProps) {
  const { user } = useAuth()

  if (!user) return <>{fallback}</>

  const userRoles = user.allRoles ?? []
  const hasAccess = requireAll
    ? roles.every((r) => userRoles.includes(r))
    : roles.some((r) => userRoles.includes(r))

  return hasAccess ? <>{children}</> : <>{fallback}</>
}

/**
 * Hook to check if the current user has a given role.
 */
export function useHasRole(role: string): boolean {
  const { user } = useAuth()
  return user?.allRoles?.includes(role) ?? false
}

/**
 * Hook to check if the current user has any of the given roles.
 */
export function useHasAnyRole(roles: string[]): boolean {
  const { user } = useAuth()
  if (!user) return false
  return roles.some((r) => user.allRoles?.includes(r))
}

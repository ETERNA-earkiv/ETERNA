/**
 * User store — inloggad användare, roller och behörigheter.
 */

import { atom, computed } from 'nanostores';
import { apiGet } from '../api/client';
import type { RodaUser } from '../types/user';
import { RODA_ROLES } from '../types/user';

export const $user = atom<RodaUser | null>(null);
export const $userLoading = atom(false);
export const $userError = atom<string | null>(null);

// Computed
export const $isLoggedIn = computed($user, (u) => u != null);
export const $username = computed($user, (u) => u?.name || '');
export const $displayName = computed($user, (u) => u?.fullName || u?.name || '');
export const $roles = computed($user, (u) => u?.allRoles || []);

// Role-based permissions
function hasRole(role: string): boolean {
  const user = $user.get();
  return user?.allRoles?.includes(role) ?? false;
}

export const $canBrowse = computed($user, () => hasRole(RODA_ROLES.AIP_READ));
export const $canEditAIPs = computed($user, () =>
  hasRole(RODA_ROLES.AIP_UPDATE) || hasRole(RODA_ROLES.MEMBER_MANAGE),
);
export const $canDeleteAIPs = computed($user, () =>
  hasRole(RODA_ROLES.AIP_DELETE) || hasRole(RODA_ROLES.MEMBER_MANAGE),
);
export const $canCreateAIPs = computed($user, () =>
  hasRole(RODA_ROLES.AIP_CREATE) || hasRole(RODA_ROLES.MEMBER_MANAGE),
);
export const $canManageMembers = computed($user, () =>
  hasRole(RODA_ROLES.MEMBER_MANAGE),
);
export const $canCreateRepresentations = computed($user, () =>
  hasRole(RODA_ROLES.REPRESENTATION_CREATE) || hasRole(RODA_ROLES.MEMBER_MANAGE),
);

export async function loadUserProfile(): Promise<void> {
  $userLoading.set(true);
  $userError.set(null);
  try {
    const user = await apiGet<RodaUser>('/api/v2/members/users/authenticated');
    $user.set(user);
  } catch (err) {
    $userError.set(err instanceof Error ? err.message : 'Kunde inte hämta användarprofil');
  } finally {
    $userLoading.set(false);
  }
}

export function clearUser(): void {
  $user.set(null);
  $userLoading.set(false);
  $userError.set(null);
}

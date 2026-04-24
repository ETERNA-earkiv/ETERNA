/**
 * Authentication API — login, session validation, logout.
 */

import { $user, clearUser, loadUserProfile } from '../stores/user';

let sessionChecked = false;
let sessionCheckPromise: Promise<boolean> | null = null;

/**
 * Login with Basic Auth credentials.
 * Sends Basic Auth ONLY on this request — RODA sets JSESSIONID cookie.
 */
export async function login(username: string, password: string): Promise<boolean> {
  const body = {
    filter: { parameters: [{ type: 'AllFilterParameter' }] },
    onlyActive: true,
    sublist: { firstElementIndex: 0, maximumElementCount: 1 },
  };

  const res = await fetch('/api/v2/aips/find', {
    method: 'POST',
    credentials: 'include',
    headers: {
      Authorization: 'Basic ' + btoa(Array.from(new TextEncoder().encode(`${username}:${password}`), (b) => String.fromCharCode(b)).join('')),
      'Content-Type': 'application/json',
      Accept: 'application/json',
    },
    body: JSON.stringify(body),
  });

  if (!res.ok) return false;

  // Session established — fetch user profile
  await loadUserProfile();
  sessionChecked = true;
  return true;
}

/**
 * Validate existing session cookie.
 * Called by auth guard on protected routes.
 */
export async function validateSession(): Promise<boolean> {
  if (sessionChecked && $user.get()) return true;

  if (sessionCheckPromise) return sessionCheckPromise;

  sessionCheckPromise = (async () => {
    try {
      const res = await fetch('/api/v2/members/users/authenticated', {
        credentials: 'include',
      });
      if (!res.ok) return false;

      const user = await res.json();
      if (user?.id === 'guest') return false;

      await loadUserProfile();
      sessionChecked = true;
      return true;
    } catch {
      return false;
    } finally {
      sessionCheckPromise = null;
    }
  })();

  return sessionCheckPromise;
}

/**
 * Logout — clear user state and expire cookie.
 */
export function logout(): void {
  document.cookie = 'JSESSIONID=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/; SameSite=Strict';
  clearUser();
  sessionChecked = false;
}

export function isAuthenticated(): boolean {
  return sessionChecked && $user.get() != null;
}

/**
 * POST /api/auth/logout — loggar ut den inloggade användaren.
 *
 * Sessionscookien (JSESSIONID) sätts av ETERNA som HttpOnly och kan därför
 * inte raderas från JavaScript i webbläsaren. Den här server-routen:
 *  1. ber ETERNA avsluta sessionen (servlet-filtrets /logout; best effort),
 *  2. raderar cookien i webbläsaren,
 *  3. skickar användaren tillbaka till söksidan.
 */

import type { APIRoute } from 'astro';
import { RODA_API_URL } from '@lib/server/env';

// Måste matcha attributen ETERNA satte på cookien, annars raderas den inte.
const SESSION_COOKIE_OPTIONS = {
  path: '/',
  httpOnly: true,
  secure: true,
  sameSite: 'strict' as const,
};

export const POST: APIRoute = async ({ cookies, redirect }) => {
  const sessionId = cookies.get('JSESSIONID')?.value;

  if (sessionId) {
    try {
      await fetch(`${RODA_API_URL}/logout`, {
        headers: { Cookie: `JSESSIONID=${sessionId}` },
        redirect: 'manual',
      });
    } catch {
      // ETERNA onåbart — cookien raderas ändå, sessionen dör vid timeout.
    }
  }

  cookies.delete('JSESSIONID', SESSION_COOKIE_OPTIONS);
  return redirect('/sok', 303);
};

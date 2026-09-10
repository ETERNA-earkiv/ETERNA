/**
 * Validerar en JSESSIONID-cookie mot RODA för att avgöra om besökaren är
 * en riktig inloggad (icke-guest) användare — inte bara om cookien finns.
 *
 * Används av Header.astro för att styra om Admin-länken visas. Motsvarar
 * samma kontroll som src/middleware.ts gör för det faktiska åtkomstskyddet
 * av /admin (som förblir den auktoritativa spärren — detta styr bara UI:t).
 */
import { RODA_API_URL } from './env';

export async function isAuthenticatedNonGuest(sessionCookieValue: string | undefined): Promise<boolean> {
  if (!sessionCookieValue) return false;

  try {
    const res = await fetch(`${RODA_API_URL}/api/v2/members/users/authenticated`, {
      headers: { Cookie: `JSESSIONID=${sessionCookieValue}` },
    });
    if (!res.ok) return false;

    const user = await res.json();
    return user?.id !== 'guest';
  } catch {
    return false;
  }
}

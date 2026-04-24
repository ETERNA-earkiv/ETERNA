/**
 * Service Account Session Manager
 *
 * Hanterar en server-side RODA-session för portalen.
 * Används för att låta anonyma besökare söka utan inloggning.
 *
 * - Lazy init: autentiserar vid första anropet, inte vid startup
 * - Cachar JSESSIONID i minnet
 * - Re-autentiserar automatiskt vid 401 (session expired)
 * - Deduplicerar samtida auth-försök (Promise-lock)
 */

import { RODA_API_URL, PORTAL_SERVICE_USER, PORTAL_SERVICE_PASSWORD } from './env';

let cachedSessionId: string | null = null;
let authPromise: Promise<string> | null = null;

/**
 * Autentisera mot RODA och extrahera JSESSIONID.
 */
async function authenticate(): Promise<string> {
  // Buffer.from hanterar UTF-8 korrekt (btoa klarar inte åäö)
  const credentials = Buffer.from(`${PORTAL_SERVICE_USER}:${PORTAL_SERVICE_PASSWORD}`, 'utf-8').toString('base64');

  const res = await fetch(`${RODA_API_URL}/api/v2/members/users/authenticated`, {
    method: 'GET',
    headers: {
      Authorization: `Basic ${credentials}`,
      Accept: 'application/json',
    },
  });

  if (!res.ok) {
    const status = res.status;
    if (status === 401 || status === 403) {
      throw new Error(
        `Service account autentisering misslyckades (${status}). Kontrollera PORTAL_SERVICE_USER/PASSWORD.`,
      );
    }
    throw new Error(`RODA svarade med ${status} vid service account auth.`);
  }

  // Extrahera JSESSIONID från Set-Cookie header
  const setCookie = res.headers.get('set-cookie') || '';
  const match = setCookie.match(/JSESSIONID=([^;]+)/);
  if (!match) {
    throw new Error(
      'RODA returnerade inget JSESSIONID cookie. Kontrollera att RODA körs korrekt.',
    );
  }

  return match[1];
}

/**
 * Hämta en giltig service account JSESSIONID.
 * Cachar sessionen och deduplicerar samtida anrop.
 */
export async function getServiceSessionCookie(): Promise<string> {
  if (cachedSessionId) {
    return `JSESSIONID=${cachedSessionId}`;
  }

  // Deduplicera: om auth redan pågår, vänta på samma Promise
  if (authPromise) {
    const id = await authPromise;
    return `JSESSIONID=${id}`;
  }

  authPromise = authenticate();
  try {
    cachedSessionId = await authPromise;
    return `JSESSIONID=${cachedSessionId}`;
  } catch (err) {
    cachedSessionId = null;
    throw err;
  } finally {
    authPromise = null;
  }
}

/**
 * Invalidera cached session. Anropas av proxyn vid 401 från RODA.
 */
export function invalidateServiceSession(): void {
  cachedSessionId = null;
}

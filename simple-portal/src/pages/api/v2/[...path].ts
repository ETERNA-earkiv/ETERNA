/**
 * Catch-all proxy: /api/v2/* → RODA
 *
 * Tre-vägs auth-logik:
 * 1. Request har Authorization-header → Login-försök, forward direkt
 * 2. Request har JSESSIONID cookie    → Inloggad user, forward session
 * 3. Inget av ovanstående             → Anonym, inject service account
 *
 * Service account JSESSIONID exponeras ALDRIG till browsern.
 */

import type { APIRoute } from 'astro';
import { RODA_API_URL } from '@lib/server/env';
import { getServiceSessionCookie, invalidateServiceSession } from '@lib/server/service-session';

/** Headers som INTE ska forwarda (hop-by-hop) */
const STRIP_REQUEST_HEADERS = new Set([
  'host', 'connection', 'keep-alive', 'transfer-encoding',
  'upgrade', 'proxy-connection', 'proxy-authenticate', 'proxy-authorization',
]);

type AuthMode = 'basic-auth' | 'user-session' | 'service-account';

export function detectAuthMode(request: Request): AuthMode {
  if (request.headers.get('authorization')) {
    return 'basic-auth';
  }
  const cookie = request.headers.get('cookie') || '';
  if (cookie.includes('JSESSIONID')) {
    return 'user-session';
  }
  return 'service-account';
}

async function proxyToRoda(
  request: Request,
  path: string,
  authMode: AuthMode,
): Promise<Response> {
  // Bygg target URL med query string
  const url = new URL(request.url);
  const targetUrl = `${RODA_API_URL}/api/v2/${path}${url.search}`;

  // Bygg headers
  const headers = new Headers();
  for (const [key, value] of request.headers.entries()) {
    if (!STRIP_REQUEST_HEADERS.has(key.toLowerCase())) {
      headers.set(key, value);
    }
  }

  // Injicera auth beroende på mode
  if (authMode === 'service-account') {
    // Ta bort eventuella cookie-headers (ska inte skicka browser-cookies)
    headers.delete('cookie');
    const serviceCookie = await getServiceSessionCookie();
    headers.set('cookie', serviceCookie);
    // Ta bort Authorization om det finns (ska inte finnas, men säkerhetsåtgärd)
    headers.delete('authorization');
  }
  // basic-auth och user-session: forward headers som de är

  // Forward body (streaming för binärdata/uploads)
  const hasBody = !['GET', 'HEAD'].includes(request.method);
  const body = hasBody ? request.body : undefined;

  const rodaRes = await fetch(targetUrl, {
    method: request.method,
    headers,
    body,
    // @ts-expect-error — Node fetch stödjer duplex för streaming
    duplex: hasBody ? 'half' : undefined,
    redirect: 'manual',
  });

  return rodaRes;
}

function buildResponse(
  rodaRes: Response,
  authMode: AuthMode,
): Response {
  // Kopiera response headers
  const responseHeaders = new Headers();
  for (const [key, value] of rodaRes.headers.entries()) {
    const lower = key.toLowerCase();
    // Forwarda Set-Cookie BARA för user sessions (login, explicit auth)
    // ALDRIG för service account (förhindrar läckage av server-session)
    if (lower === 'set-cookie') {
      if (authMode !== 'service-account') {
        responseHeaders.append(key, value);
      }
      continue;
    }
    // Skip hop-by-hop headers
    if (lower === 'transfer-encoding' || lower === 'connection') continue;
    responseHeaders.set(key, value);
  }

  return new Response(rodaRes.body, {
    status: rodaRes.status,
    statusText: rodaRes.statusText,
    headers: responseHeaders,
  });
}

/**
 * Tillåtna POST-endpoints för anonyma (service-account) requests.
 * RODA V2 kräver POST för alla /find-sökningar — dessa är read-only.
 */
const SERVICE_ACCOUNT_ALLOWED_POST_PATHS = [
  /^aips\/find$/,
  /^representations\/find$/,
  /^representations-information\/find$/,
  /^files\/find$/,
];

/** Avgör om en request är tillåten för service-account */
export function isAllowedForServiceAccount(method: string, path: string): boolean {
  if (method === 'GET' || method === 'HEAD') return true;
  if (method === 'POST') {
    return SERVICE_ACCOUNT_ALLOWED_POST_PATHS.some((re) => re.test(path));
  }
  return false;
}

const handler: APIRoute = async ({ params, request }) => {
  const path = params.path || '';
  const authMode = detectAuthMode(request);

  // Begränsa anonyma requests till läsning + vitlistade sök-endpoints
  if (authMode === 'service-account' && !isAllowedForServiceAccount(request.method, path)) {
    return new Response(
      JSON.stringify({ error: 'Autentisering krävs för denna operation.' }),
      { status: 401, headers: { 'Content-Type': 'application/json' } },
    );
  }

  try {
    const rodaRes = await proxyToRoda(request, path, authMode);

    // Om RODA returnerar 401 och vi använde service account — retry
    if (rodaRes.status === 401 && authMode === 'service-account') {
      invalidateServiceSession();
      try {
        const retryRes = await proxyToRoda(request, path, 'service-account');
        if (retryRes.status === 401) {
          return new Response(
            JSON.stringify({ error: 'Arkivet är tillfälligt otillgängligt.' }),
            { status: 503, headers: { 'Content-Type': 'application/json' } },
          );
        }
        return buildResponse(retryRes, 'service-account');
      } catch {
        return new Response(
          JSON.stringify({ error: 'Arkivet är tillfälligt otillgängligt.' }),
          { status: 503, headers: { 'Content-Type': 'application/json' } },
        );
      }
    }

    return buildResponse(rodaRes, authMode);
  } catch (err) {
    console.error('[proxy] Fel vid anslutning till RODA:', err);
    return new Response(
      JSON.stringify({ error: 'Kunde inte ansluta till arkivet.' }),
      { status: 502, headers: { 'Content-Type': 'application/json' } },
    );
  }
};

// Exportera för alla HTTP-metoder
export const GET = handler;
export const POST = handler;
export const PUT = handler;
export const PATCH = handler;
export const DELETE = handler;

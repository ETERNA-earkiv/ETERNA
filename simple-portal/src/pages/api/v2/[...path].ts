/**
 * Catch-all proxy: /api/v2/* → RODA
 *
 * Tre-vägs auth-logik:
 * 1. Request har Authorization-header → Login-försök, forward direkt
 * 2. Request har JSESSIONID cookie    → Inloggad user, forward session
 * 3. Inget av ovanstående             → Anonym, forward utan auth
 *
 * Anonyma requests skickas vidare till RODA helt utan Authorization/cookie.
 * RODA hanterar dem själv som gäst-användare enligt de roller som är
 * tilldelade "guests"-gruppen där (aip.read / descriptive_metadata.read /
 * representation.read). Inget lösenord eller service-konto krävs i portalen.
 */

import type { APIRoute } from 'astro';
import { RODA_API_URL } from '@lib/server/env';

/** Headers som INTE ska forwarda (hop-by-hop) */
const STRIP_REQUEST_HEADERS = new Set([
  'host', 'connection', 'keep-alive', 'transfer-encoding',
  'upgrade', 'proxy-connection', 'proxy-authenticate', 'proxy-authorization',
]);

type AuthMode = 'basic-auth' | 'user-session' | 'anonymous';

export function detectAuthMode(request: Request): AuthMode {
  if (request.headers.get('authorization')) {
    return 'basic-auth';
  }
  const cookie = request.headers.get('cookie') || '';
  if (cookie.includes('JSESSIONID')) {
    return 'user-session';
  }
  return 'anonymous';
}

async function proxyToRoda(
  request: Request,
  path: string,
  authMode: AuthMode,
  bufferedBody: ArrayBuffer | null,
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

  if (authMode === 'anonymous') {
    // Skicka aldrig med webbläsarens ev. cookie/Authorization för anonyma
    // requests — ETERNA ska se dem som riktigt anonyma (gäst).
    headers.delete('cookie');
    headers.delete('authorization');
  } else if (authMode === 'basic-auth') {
    // Ett inloggningsförsök ska bedömas på sina egna uppgifter. En kvarliggande
    // sessionscookie (t.ex. en gäst-session från ett tidigare misslyckat
    // försök) kan annars skugga Basic Auth i ETERNA.
    headers.delete('cookie');
  }
  // user-session: forward headers som de är

  const rodaRes = await fetch(targetUrl, {
    method: request.method,
    headers,
    body: bufferedBody ?? undefined,
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
    // Forwarda Set-Cookie BARA för riktiga inloggningar. ALDRIG för anonyma
    // requests, och inte heller för ett MISSLYCKAT inloggningsförsök: ETERNA
    // svarar då ändå med en JSESSIONID för en gäst-session, och en sådan
    // cookie i webbläsaren skulle skugga nästa (korrekta) inloggning.
    // En cookie ska alltså bara finnas i webbläsaren om en verklig inloggning
    // har skett (Header.astro/middleware.ts litar på detta).
    if (lower === 'set-cookie') {
      const isRealLogin = authMode === 'user-session' || (authMode === 'basic-auth' && rodaRes.ok);
      if (isRealLogin) {
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
 * Tillåtna POST-endpoints för anonyma requests.
 * RODA V2 kräver POST för alla /find-sökningar — dessa är read-only.
 */
const ANONYMOUS_ALLOWED_POST_PATHS = [
  /^aips\/find$/,
  /^representations\/find$/,
  /^representations-information\/find$/,
  /^files\/find$/,
];

/** Avgör om en request är tillåten för anonyma (oautentiserade) besökare */
export function isAllowedForServiceAccount(method: string, path: string): boolean {
  if (method === 'GET' || method === 'HEAD') return true;
  if (method === 'POST') {
    return ANONYMOUS_ALLOWED_POST_PATHS.some((re) => re.test(path));
  }
  return false;
}

const handler: APIRoute = async ({ params, request }) => {
  const path = params.path || '';
  const authMode = detectAuthMode(request);

  // Begränsa anonyma requests till läsning + vitlistade sök-endpoints.
  // RODA:s egna roller för "guests" är den auktoritativa spärren — detta
  // är ett extra säkerhetslager i proxyn.
  if (authMode === 'anonymous' && !isAllowedForServiceAccount(request.method, path)) {
    return new Response(
      JSON.stringify({ error: 'Autentisering krävs för denna operation.' }),
      { status: 401, headers: { 'Content-Type': 'application/json' } },
    );
  }

  // Buffra body en gång så att den kan återanvändas — ReadableStream är
  // engångskonsumerad.
  const hasBody = !['GET', 'HEAD'].includes(request.method);
  const bufferedBody: ArrayBuffer | null = hasBody ? await request.arrayBuffer() : null;

  try {
    const rodaRes = await proxyToRoda(request, path, authMode, bufferedBody);
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

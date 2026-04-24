/**
 * Astro middleware — Auth guard
 *
 * Skyddar adminytan genom att validera JSESSIONID-cookien
 * mot RODA V2 API innan sidan renderas.
 */

import { defineMiddleware } from 'astro:middleware';
import { RODA_API_URL } from '@lib/server/env';

const PROTECTED_PREFIXES = ['/admin'];
const LOGIN_PATH = '/logga-in';

export const onRequest = defineMiddleware(async (context, next) => {
  const { pathname } = context.url;

  // Only check protected routes
  const isProtected = PROTECTED_PREFIXES.some((p) => pathname.startsWith(p));
  if (!isProtected) return next();

  // Check for session cookie
  const sessionCookie = context.cookies.get('JSESSIONID');
  if (!sessionCookie?.value) {
    const returnUrl = encodeURIComponent(pathname + context.url.search);
    return context.redirect(`${LOGIN_PATH}?returnUrl=${returnUrl}`);
  }

  // Validate session against RODA API
  try {
    const res = await fetch(`${RODA_API_URL}/api/v2/members/users/authenticated`, {
      headers: {
        Cookie: `JSESSIONID=${sessionCookie.value}`,
      },
    });

    if (!res.ok) {
      const returnUrl = encodeURIComponent(pathname + context.url.search);
      return context.redirect(`${LOGIN_PATH}?returnUrl=${returnUrl}&expired=1`);
    }

    const user = await res.json();
    if (user?.id === 'guest') {
      const returnUrl = encodeURIComponent(pathname + context.url.search);
      return context.redirect(`${LOGIN_PATH}?returnUrl=${returnUrl}`);
    }
  } catch {
    // Backend unreachable — deny access (fail-closed)
    return new Response('Tjänsten är tillfälligt otillgänglig. Försök igen senare.', {
      status: 503,
      headers: { 'Content-Type': 'text/plain; charset=utf-8' },
    });
  }

  return next();
});

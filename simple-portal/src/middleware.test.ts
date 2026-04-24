/**
 * Tests for middleware auth guard — especially fail-closed behavior.
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

// Mock Astro virtual modules
vi.mock('astro:middleware', () => ({
  defineMiddleware: (fn: any) => fn,
}));

// Mock the env module
vi.mock('@lib/server/env', () => ({
  RODA_API_URL: 'http://mock-roda:8080',
}));

import { onRequest } from './middleware';

// Helper: create a mock Astro middleware context
function createContext(pathname: string, cookie?: string) {
  const url = new URL(`http://localhost${pathname}`);
  return {
    url,
    cookies: {
      get: (name: string) => {
        if (name === 'JSESSIONID' && cookie) {
          return { value: cookie };
        }
        return undefined;
      },
    },
    redirect: (target: string) => new Response(null, {
      status: 302,
      headers: { Location: target },
    }),
  } as any;
}

async function next(): Promise<Response> {
  return new Response('OK', { status: 200 });
}

describe('middleware auth guard', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn());
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('passes through non-protected routes without auth check', async () => {
    const res = await onRequest(createContext('/'), next) as Response;
    expect(res.status).toBe(200);
  });

  it('redirects to login when no session cookie on /admin', async () => {
    const res = await onRequest(createContext('/admin'), next) as Response;
    expect(res.status).toBe(302);
    expect(res.headers.get('Location')).toContain('/logga-in');
  });

  it('redirects to login when RODA says session is invalid', async () => {
    (globalThis.fetch as any).mockResolvedValue(new Response('Unauthorized', { status: 401 }));
    const res = await onRequest(createContext('/admin', 'valid-session'), next) as Response;
    expect(res.status).toBe(302);
    expect(res.headers.get('Location')).toContain('expired=1');
  });

  it('redirects to login when RODA returns guest user', async () => {
    (globalThis.fetch as any).mockResolvedValue(
      new Response(JSON.stringify({ id: 'guest' }), { status: 200 }),
    );
    const res = await onRequest(createContext('/admin', 'guest-session'), next) as Response;
    expect(res.status).toBe(302);
  });

  it('returns 503 when RODA is unreachable (fail-closed)', async () => {
    (globalThis.fetch as any).mockRejectedValue(new Error('ECONNREFUSED'));
    const res = await onRequest(createContext('/admin', 'valid-session'), next) as Response;
    expect(res.status).toBe(503);
    const body = await res.text();
    expect(body).toContain('otillgänglig');
  });

  it('allows through when RODA validates the session', async () => {
    (globalThis.fetch as any).mockResolvedValue(
      new Response(JSON.stringify({ id: 'admin-user', name: 'Admin' }), { status: 200 }),
    );
    const res = await onRequest(createContext('/admin', 'valid-session'), next) as Response;
    expect(res.status).toBe(200);
  });
});

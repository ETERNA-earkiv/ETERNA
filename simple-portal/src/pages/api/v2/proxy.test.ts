/**
 * Tests for the proxy auth mode detection and method restriction.
 */
import { describe, it, expect, vi, beforeEach } from 'vitest';

// Mock server dependencies before importing handler
vi.mock('@lib/server/env', () => ({
  RODA_API_URL: 'http://mock-roda:8080',
}));

import { detectAuthMode, isAllowedForServiceAccount, GET, POST, PUT, PATCH, DELETE } from './[...path]';

describe('detectAuthMode', () => {
  it('returns basic-auth when Authorization header is present', () => {
    const req = new Request('http://localhost/api/v2/test', {
      headers: { Authorization: 'Basic dXNlcjpwYXNz' },
    });
    expect(detectAuthMode(req)).toBe('basic-auth');
  });

  it('returns user-session when JSESSIONID cookie is present', () => {
    const req = new Request('http://localhost/api/v2/test', {
      headers: { Cookie: 'JSESSIONID=abc123' },
    });
    expect(detectAuthMode(req)).toBe('user-session');
  });

  it('prefers basic-auth over cookie when both are present', () => {
    const req = new Request('http://localhost/api/v2/test', {
      headers: {
        Authorization: 'Basic dXNlcjpwYXNz',
        Cookie: 'JSESSIONID=abc123',
      },
    });
    expect(detectAuthMode(req)).toBe('basic-auth');
  });

  it('returns anonymous when neither auth nor cookie is present', () => {
    const req = new Request('http://localhost/api/v2/test');
    expect(detectAuthMode(req)).toBe('anonymous');
  });
});

describe('isAllowedForServiceAccount', () => {
  // GET/HEAD always allowed
  it('allows GET for any path', () => {
    expect(isAllowedForServiceAccount('GET', 'aips')).toBe(true);
    expect(isAllowedForServiceAccount('GET', 'aips/123/download')).toBe(true);
  });

  it('allows HEAD for any path', () => {
    expect(isAllowedForServiceAccount('HEAD', 'aips')).toBe(true);
  });

  // Whitelisted POST /find endpoints
  it('allows POST to aips/find', () => {
    expect(isAllowedForServiceAccount('POST', 'aips/find')).toBe(true);
  });

  it('allows POST to representations/find', () => {
    expect(isAllowedForServiceAccount('POST', 'representations/find')).toBe(true);
  });

  it('allows POST to representations-information/find', () => {
    expect(isAllowedForServiceAccount('POST', 'representations-information/find')).toBe(true);
  });

  it('allows POST to files/find', () => {
    expect(isAllowedForServiceAccount('POST', 'files/find')).toBe(true);
  });

  // Non-whitelisted POST paths blocked
  it('blocks POST to aips (not /find)', () => {
    expect(isAllowedForServiceAccount('POST', 'aips')).toBe(false);
  });

  it('blocks POST to aips/move', () => {
    expect(isAllowedForServiceAccount('POST', 'aips/move')).toBe(false);
  });

  it('blocks POST to aips/delete', () => {
    expect(isAllowedForServiceAccount('POST', 'aips/delete')).toBe(false);
  });

  it('blocks POST to files/upload', () => {
    expect(isAllowedForServiceAccount('POST', 'files/upload')).toBe(false);
  });

  // PUT/PATCH/DELETE always blocked
  it('blocks PUT', () => {
    expect(isAllowedForServiceAccount('PUT', 'aips/123')).toBe(false);
  });

  it('blocks PATCH', () => {
    expect(isAllowedForServiceAccount('PATCH', 'aips/123')).toBe(false);
  });

  it('blocks DELETE', () => {
    expect(isAllowedForServiceAccount('DELETE', 'aips/123')).toBe(false);
  });
});

describe('proxy handler integration', () => {
  function mockContext(method: string, path: string, headers?: Record<string, string>) {
    return {
      params: { path },
      request: new Request(`http://localhost/api/v2/${path}`, {
        method,
        headers: headers || {},
      }),
    } as any;
  }

  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(
      new Response('{"ok": true}', {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    ));
  });

  it('allows GET for anonymous requests', async () => {
    const res = await GET(mockContext('GET', 'aips'));
    expect(res.status).toBe(200);
  });

  it('allows POST aips/find for anonymous requests (search)', async () => {
    const res = await POST(mockContext('POST', 'aips/find'));
    expect(res.status).toBe(200);
  });

  it('blocks POST aips/delete for anonymous requests', async () => {
    const res = await POST(mockContext('POST', 'aips/delete'));
    expect(res.status).toBe(401);
    const body = await res.json();
    expect(body.error).toContain('Autentisering krävs');
  });

  it('blocks PUT for anonymous requests', async () => {
    const res = await PUT(mockContext('PUT', 'aips/123'));
    expect(res.status).toBe(401);
  });

  it('blocks PATCH for anonymous requests', async () => {
    const res = await PATCH(mockContext('PATCH', 'aips/123'));
    expect(res.status).toBe(401);
  });

  it('blocks DELETE for anonymous requests', async () => {
    const res = await DELETE(mockContext('DELETE', 'aips/123'));
    expect(res.status).toBe(401);
  });

  it('allows POST for authenticated user-session requests', async () => {
    const ctx = mockContext('POST', 'aips', { Cookie: 'JSESSIONID=user-session-id' });
    const res = await POST(ctx);
    expect(res.status).toBe(200);
  });

  it('allows DELETE for basic-auth requests', async () => {
    const ctx = mockContext('DELETE', 'aips/123', { Authorization: 'Basic dXNlcjpwYXNz' });
    const res = await DELETE(ctx);
    expect(res.status).toBe(200);
  });

  it('strips a stale Cookie header from basic-auth (login) requests but keeps Authorization', async () => {
    const ctx = mockContext('POST', 'aips/find', {
      Authorization: 'Basic dXNlcjpwYXNz',
      Cookie: 'JSESSIONID=stale-guest-session',
    });
    await POST(ctx);
    const forwarded: Headers = (globalThis.fetch as any).mock.calls[0][1].headers;
    expect(forwarded.get('cookie')).toBeNull();
    expect(forwarded.get('authorization')).toBe('Basic dXNlcjpwYXNz');
  });

  it('forwards Set-Cookie on a successful basic-auth login', async () => {
    (globalThis.fetch as any).mockResolvedValue(new Response('{"ok": true}', {
      status: 200,
      headers: { 'Content-Type': 'application/json', 'Set-Cookie': 'JSESSIONID=admin-session; Path=/' },
    }));
    const res = await POST(mockContext('POST', 'aips/find', { Authorization: 'Basic dXNlcjpwYXNz' }));
    expect(res.headers.get('set-cookie')).toContain('JSESSIONID=admin-session');
  });

  it('does NOT forward Set-Cookie on a failed basic-auth login (guest session must not be planted)', async () => {
    (globalThis.fetch as any).mockResolvedValue(new Response('{"status":403}', {
      status: 403,
      headers: { 'Content-Type': 'application/json', 'Set-Cookie': 'JSESSIONID=guest-session; Path=/' },
    }));
    const res = await POST(mockContext('POST', 'aips/find', { Authorization: 'Basic dXNlcjpwYXNz' }));
    expect(res.status).toBe(403);
    expect(res.headers.get('set-cookie')).toBeNull();
  });

  it('never forwards Set-Cookie for anonymous requests', async () => {
    (globalThis.fetch as any).mockResolvedValue(new Response('{"ok": true}', {
      status: 200,
      headers: { 'Content-Type': 'application/json', 'Set-Cookie': 'JSESSIONID=guest-session; Path=/' },
    }));
    const res = await GET(mockContext('GET', 'aips'));
    expect(res.headers.get('set-cookie')).toBeNull();
  });
});

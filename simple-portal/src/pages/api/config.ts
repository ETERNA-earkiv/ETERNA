/**
 * Astro API endpoint: GET/PUT /api/config
 * Laser och skriver config.json - en enda kalla for all konfiguration.
 * Ingen extern server behovs.
 */

import type { APIRoute } from 'astro';
import { readFile, writeFile } from 'node:fs/promises';
import { join } from 'node:path';
import { RODA_API_URL } from '@lib/server/env';

const CONFIG_PATH = join(process.cwd(), 'public', 'assets', 'config', 'config.json');

function asRecord(value: unknown): Record<string, unknown> {
  return typeof value === 'object' && value !== null ? value as Record<string, unknown> : {};
}

function mergeConfig(
  existing: Record<string, unknown>,
  incoming: Record<string, unknown>,
): Record<string, unknown> {
  return {
    ...existing,
    ...incoming,
    siteConfig: {
      ...asRecord(existing.siteConfig),
      ...asRecord(incoming.siteConfig),
    },
    searchConfig: {
      ...asRecord(existing.searchConfig),
      ...asRecord(incoming.searchConfig),
    },
    visibilityConfig: {
      ...asRecord(existing.visibilityConfig),
      ...asRecord(incoming.visibilityConfig),
    },
  };
}

export const GET: APIRoute = async () => {
  try {
    const data = await readFile(CONFIG_PATH, 'utf-8');
    return new Response(data, {
      headers: { 'Content-Type': 'application/json' },
    });
  } catch {
    return new Response(JSON.stringify({ error: 'Config not found' }), { status: 404 });
  }
};

export const PUT: APIRoute = async ({ request }) => {
  // Kräv RODA-session för skrivning
  const cookie = request.headers.get('cookie') || '';
  if (!cookie.includes('JSESSIONID')) {
    return new Response(JSON.stringify({ error: 'Unauthorized' }), { status: 401 });
  }
  try {
    const authRes = await fetch(`${RODA_API_URL}/api/v2/members/users/authenticated`, {
      headers: { Cookie: cookie },
    });
    if (!authRes.ok || (await authRes.json())?.id === 'guest') {
      return new Response(JSON.stringify({ error: 'Unauthorized' }), { status: 401 });
    }
  } catch {
    return new Response(JSON.stringify({ error: 'Auth check failed' }), { status: 401 });
  }

  try {
    const body = await request.json() as Record<string, unknown>;

    let existing: Record<string, unknown> = {};
    try {
      existing = JSON.parse(await readFile(CONFIG_PATH, 'utf-8')) as Record<string, unknown>;
    } catch {
      existing = {};
    }

    const merged = mergeConfig(existing, body);

    await writeFile(CONFIG_PATH, JSON.stringify(merged, null, 2), 'utf-8');

    return new Response(JSON.stringify(merged), {
      headers: { 'Content-Type': 'application/json' },
    });
  } catch (err) {
    return new Response(
      JSON.stringify({ error: err instanceof Error ? err.message : 'Write failed' }),
      { status: 500 },
    );
  }
};

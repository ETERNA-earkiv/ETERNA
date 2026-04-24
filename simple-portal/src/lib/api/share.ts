/**
 * Share API — temporära delningslänkar.
 */

import { apiPost, apiGet } from './client';

export interface TempLink {
  token: string;
  aipId: string;
  title: string;
  expiresAt: string;
  url: string;
}

export async function createTempLink(
  aipId: string,
  title: string,
  expiresInHours = 72,
): Promise<TempLink> {
  const result = await apiPost<TempLink>('/api/portal/share', {
    aipId,
    title,
    expiresInHours,
  });

  return {
    ...result,
    url: buildShareUrl(result.token),
  };
}

export async function resolveTempLink(token: string): Promise<string | null> {
  try {
    const result = await apiGet<{ aipId: string }>(`/api/portal/share/${token}`);
    return result?.aipId || null;
  } catch {
    return null;
  }
}

export function buildShareUrl(token: string): string {
  return `${window.location.origin}/dela/${token}`;
}

export function sendByEmail(to: string, subject: string, body: string): void {
  const mailto = `mailto:${encodeURIComponent(to)}?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(body)}`;
  window.location.href = mailto;
}

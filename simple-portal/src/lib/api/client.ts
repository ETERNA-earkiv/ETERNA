/**
 * Fetch-baserad API-klient med credentials, retry och 401-redirect.
 */

interface FetchOptions extends Omit<RequestInit, 'body'> {
  params?: Record<string, string>;
  body?: unknown;
}

const MAX_RETRIES = 3;

async function handleRetry<T>(
  endpoint: string,
  options: FetchOptions,
  attempt: number,
  res: Response,
): Promise<T> {
  if (attempt >= MAX_RETRIES) {
    throw new Error(`429 Too Many Requests after ${MAX_RETRIES} retries: ${endpoint}`);
  }
  const retryAfter = parseInt(res.headers.get('Retry-After') || '', 10);
  const delay = retryAfter > 0 ? retryAfter * 1000 : Math.pow(2, attempt) * 1000;
  await new Promise((r) => setTimeout(r, delay));
  return request<T>(endpoint, options, attempt + 1);
}

/** Mappa HTTP-status till användarvänligt svenskt meddelande */
function friendlyHttpError(status: number, detail: string): string {
  switch (status) {
    case 400: return `Ogiltig förfrågan: ${detail}`;
    case 401: return 'Inte inloggad';
    case 403: return 'Du har inte behörighet att utföra denna åtgärd.';
    case 404: return 'Det efterfrågade objektet hittades inte.';
    case 409: return 'Åtgärden kunde inte utföras på grund av en konflikt. Försök igen.';
    case 500: return 'Ett internt serverfel uppstod. Försök igen senare.';
    case 502: return 'Arkivet kunde inte nås. Kontrollera att det är igång.';
    case 503: return 'Tjänsten är tillfälligt otillgänglig. Försök igen om en stund.';
    default: return `Oväntat fel (${status}): ${detail}`;
  }
}

const REQUEST_TIMEOUT_MS = 30_000;

async function request<T>(
  endpoint: string,
  options: FetchOptions = {},
  attempt = 0,
): Promise<T> {
  const { params, body, headers: extraHeaders, ...rest } = options;

  const url = new URL(endpoint, window.location.origin);
  if (params) {
    for (const [key, value] of Object.entries(params)) {
      url.searchParams.set(key, value);
    }
  }

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(extraHeaders as Record<string, string>),
  };

  // Timeout via AbortController
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);

  let res: Response;
  try {
    res = await fetch(url.toString(), {
      credentials: 'include',
      headers,
      body: body ? JSON.stringify(body) : undefined,
      signal: controller.signal,
      ...rest,
    });
  } catch (err) {
    clearTimeout(timeout);
    if (err instanceof DOMException && err.name === 'AbortError') {
      throw new Error('Förfrågan tog för lång tid. Kontrollera din anslutning och försök igen.');
    }
    throw new Error('Kunde inte ansluta till servern. Kontrollera att arkivet är igång.');
  } finally {
    clearTimeout(timeout);
  }

  if (res.status === 401 || res.status === 403) {
    const path = window.location.pathname;
    if (path.startsWith('/admin') || path.startsWith('/logga-in')) {
      const returnUrl = encodeURIComponent(path + window.location.search);
      window.location.href = `/logga-in?returnUrl=${returnUrl}&expired=1`;
    }
    throw new Error(friendlyHttpError(res.status, ''));
  }

  if (res.status === 429) {
    return handleRetry<T>(endpoint, options, attempt, res);
  }

  if (!res.ok) {
    const text = await res.text().catch(() => res.statusText);
    throw new Error(friendlyHttpError(res.status, text));
  }

  const contentType = res.headers.get('Content-Type') || '';
  if (contentType.includes('application/json')) {
    return res.json();
  }
  return res.text() as unknown as T;
}

export function api<T>(endpoint: string, options?: FetchOptions): Promise<T> {
  return request<T>(endpoint, options);
}

export function apiGet<T>(endpoint: string, params?: Record<string, string>): Promise<T> {
  return request<T>(endpoint, { method: 'GET', params });
}

export function apiPost<T>(endpoint: string, body?: unknown): Promise<T> {
  return request<T>(endpoint, { method: 'POST', body });
}

export function apiPut<T>(endpoint: string, body?: unknown): Promise<T> {
  return request<T>(endpoint, { method: 'PUT', body });
}

export function apiPatch<T>(endpoint: string, body?: unknown): Promise<T> {
  return request<T>(endpoint, { method: 'PATCH', body });
}

export function apiDelete<T>(endpoint: string, body?: unknown): Promise<T> {
  return request<T>(endpoint, { method: 'DELETE', body });
}

async function fetchWithTimeout(endpoint: string, init: RequestInit): Promise<Response> {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);
  try {
    const res = await fetch(endpoint, { ...init, signal: controller.signal });
    return res;
  } catch (err) {
    if (err instanceof DOMException && err.name === 'AbortError') {
      throw new Error('Förfrågan tog för lång tid. Kontrollera din anslutning och försök igen.');
    }
    throw new Error('Kunde inte ansluta till servern. Kontrollera att arkivet är igång.');
  } finally {
    clearTimeout(timeout);
  }
}

function checkResponse(res: Response): void {
  if (!res.ok) throw new Error(friendlyHttpError(res.status, res.statusText));
}

export async function apiGetText(endpoint: string): Promise<string> {
  const res = await fetchWithTimeout(endpoint, {
    credentials: 'include',
    headers: { Accept: 'text/plain' },
  });
  checkResponse(res);
  return res.text();
}

export async function apiGetHtml(endpoint: string): Promise<string> {
  const res = await fetchWithTimeout(endpoint, {
    credentials: 'include',
    headers: { Accept: 'text/html' },
  });
  checkResponse(res);
  return res.text();
}

export async function apiGetXml(endpoint: string): Promise<string> {
  const res = await fetchWithTimeout(endpoint, {
    credentials: 'include',
    headers: { Accept: 'application/xml' },
  });
  checkResponse(res);
  return res.text();
}

export async function apiDownload(endpoint: string): Promise<Blob> {
  const res = await fetchWithTimeout(endpoint, { credentials: 'include' });
  checkResponse(res);
  return res.blob();
}

export async function apiPostMultipart(
  endpoint: string,
  formData: FormData,
  onProgress?: (percent: number) => void,
): Promise<Response> {
  // XMLHttpRequest for progress tracking
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open('POST', endpoint);
    xhr.withCredentials = true;

    if (onProgress) {
      xhr.upload.addEventListener('progress', (e) => {
        if (e.lengthComputable) {
          onProgress(Math.round((e.loaded / e.total) * 100));
        }
      });
    }

    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        resolve(new Response(xhr.response, { status: xhr.status }));
      } else {
        reject(new Error(friendlyHttpError(xhr.status, xhr.statusText)));
      }
    };

    xhr.onerror = () => reject(new Error('Kunde inte ansluta till servern. Kontrollera att arkivet är igång.'));
    xhr.ontimeout = () => reject(new Error('Uppladdningen tog för lång tid.'));
    xhr.send(formData);
  });
}

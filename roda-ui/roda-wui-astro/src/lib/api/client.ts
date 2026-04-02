/**
 * Typed API client for the ETERNA REST API (/api/v2/).
 * In SSR context, pass the Cookie header string to forward session credentials.
 * In browser context (React islands), credentials are sent automatically via cookies.
 */

/**
 * In SSR (Node.js) context, fetch requires absolute URLs.
 * We call Spring Boot directly, bypassing the Vite proxy.
 * SPRING_BOOT_URL defaults to http://localhost:8080.
 */
const isSSR = typeof window === "undefined";
const SPRING_BOOT_URL = isSSR
  ? (import.meta.env.SPRING_BOOT_URL ?? "http://localhost:8080")
  : "";
const API_BASE = `${SPRING_BOOT_URL}/api/v2`;

export class ApiError extends Error {
  constructor(
    public status: number,
    public errorId: string,
    message: string,
    public details?: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

async function handleResponse<T>(res: Response): Promise<T> {
  if (!res.ok) {
    let errorId = "UNKNOWN_ERROR";
    let message = `HTTP ${res.status}`;
    let details: string | undefined;
    try {
      const body = await res.json();
      errorId = body.errorId ?? errorId;
      message = body.message ?? message;
      details = body.details;
    } catch {
      // non-JSON error body
    }
    throw new ApiError(res.status, errorId, message, details);
  }
  if (res.status === 204) return undefined as unknown as T;
  const contentType = res.headers.get("content-type") ?? "";
  if (contentType.includes("application/json")) {
    return res.json() as Promise<T>;
  }
  return res.text() as unknown as T;
}

export interface RequestOptions {
  /** Forward session cookie in SSR context */
  cookie?: string;
  signal?: AbortSignal;
}

function buildHeaders(cookie?: string): HeadersInit {
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    Accept: "application/json",
  };
  if (cookie) {
    headers["Cookie"] = cookie;
  }
  return headers;
}

export async function apiGet<T>(
  path: string,
  opts: RequestOptions = {},
): Promise<T> {
  const res = await fetch(`${API_BASE}${path}`, {
    method: "GET",
    headers: buildHeaders(opts.cookie),
    credentials: "include",
    signal: opts.signal,
  });
  return handleResponse<T>(res);
}

export async function apiPost<T>(
  path: string,
  body: unknown,
  opts: RequestOptions = {},
): Promise<T> {
  const res = await fetch(`${API_BASE}${path}`, {
    method: "POST",
    headers: buildHeaders(opts.cookie),
    credentials: "include",
    body: JSON.stringify(body),
    signal: opts.signal,
  });
  return handleResponse<T>(res);
}

export async function apiPut<T>(
  path: string,
  body: unknown,
  opts: RequestOptions = {},
): Promise<T> {
  const res = await fetch(`${API_BASE}${path}`, {
    method: "PUT",
    headers: buildHeaders(opts.cookie),
    credentials: "include",
    body: JSON.stringify(body),
    signal: opts.signal,
  });
  return handleResponse<T>(res);
}

export async function apiPatch<T>(
  path: string,
  body: unknown,
  opts: RequestOptions = {},
): Promise<T> {
  const res = await fetch(`${API_BASE}${path}`, {
    method: "PATCH",
    headers: buildHeaders(opts.cookie),
    credentials: "include",
    body: JSON.stringify(body),
    signal: opts.signal,
  });
  return handleResponse<T>(res);
}

export async function apiDelete<T>(
  path: string,
  body?: unknown,
  opts: RequestOptions = {},
): Promise<T> {
  const res = await fetch(`${API_BASE}${path}`, {
    method: "DELETE",
    headers: buildHeaders(opts.cookie),
    credentials: "include",
    body: body !== undefined ? JSON.stringify(body) : undefined,
    signal: opts.signal,
  });
  return handleResponse<T>(res);
}

// ---------------------------------------------------------------------------
// Common find/search helper (POST /api/v2/{resource}/find)
// ---------------------------------------------------------------------------

export interface FindRequest {
  /** filter.parameters is a list of FilterParameter (with Jackson @type discriminator) */
  filter: {
    parameters: FilterParameter[];
  };
  /** Required by the Java builder constructor */
  onlyActive: boolean;
  sublist?: {
    firstElementIndex: number;
    maximumElementCount: number;
  };
  sorter?: {
    parameters: SortParameter[];
  };
  /** facets.parameters is a Map<fieldName, FacetParameter> — NOT an array */
  facets?: {
    parameters: Record<string, FacetParameter>;
  };
}

export interface FilterParameter {
  /** Jackson @type discriminator, e.g. "AllFilterParameter", "BasicSearchFilterParameter", "SimpleFilterParameter" */
  type: string;
  name?: string;
  value?: string;
  values?: string[];
  id?: string;
  allFilters?: FilterParameter[];
  anyFilters?: FilterParameter[];
}

export interface SortParameter {
  name: string;
  descending: boolean;
}

export interface FacetParameter {
  /** Jackson @type discriminator: "SimpleFacetParameter" or "RangeFacetParameter" */
  type: string;
  name: string;
  limit?: number;
  minCount?: number;
}

export interface IndexResult<T> {
  results: T[];
  totalCount: number;
  offset: number;
  limit: number;
  facetResults?: FacetResult[];
  date?: string;
}

export interface FacetResult {
  field: string;
  values: FacetValue[];
}

export interface FacetValue {
  label: string;
  value: string;
  count: number;
}

export async function apiFindRequest<T>(
  resource: string,
  body: FindRequest,
  opts: RequestOptions = {},
): Promise<IndexResult<T>> {
  return apiPost<IndexResult<T>>(`/${resource}/find`, body, opts);
}

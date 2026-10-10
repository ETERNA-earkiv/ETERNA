import { request, type APIRequestContext, type APIResponse } from '@playwright/test';
import type { FilterParameter } from './filters';
import type { IndexResult, TransferredResource } from './types';

export type Entity =
  | 'aips'
  | 'representations'
  | 'files'
  | 'preservation/events'
  | 'jobs'
  | 'transfers';

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

export function basicAuth(user: string, password: string): string {
  return `Basic ${Buffer.from(`${user}:${password}`, 'utf8').toString('base64')}`;
}

const MAX_BODY_CHARS = 2000;

/** Beskriver ett misslyckat anrop. Headers tas inte med, så lösenord läcker inte. */
export function describeFailure(method: string, url: string, status: number, body: string): string {
  const shown = body.length > MAX_BODY_CHARS ? `${body.slice(0, MAX_BODY_CHARS)}… (${body.length} tecken)` : body;
  return `${method} ${url} → ${status}\n${shown}`;
}

/** Döljer värdet i authorization-rader, eftersom Playwrights felmeddelanden innehåller alla request-headers. */
export function redactSecrets(text: string): string {
  return text.replace(/(authorization:[ \t]*).*/gi, '$1<dold>');
}

/** Tolkar en JSON-kropp; en 2xx-kropp som inte är JSON blir ett tydligt fel. */
export function parseJsonBody<T>(method: string, url: string, status: number, text: string): T {
  if (!text) return undefined as T;
  try {
    return JSON.parse(text) as T;
  } catch {
    throw new Error(`Svaret var inte JSON: ${describeFailure(method, url, status, text)}`);
  }
}

export class EternaClient {
  private constructor(private readonly http: APIRequestContext) {}

  /** Alla råa anrop går hit så att inloggningsuppgifter aldrig följer med i kastade fel. */
  private async send<T>(fn: () => Promise<T>): Promise<T> {
    try {
      return await fn();
    } catch (error) {
      if (error instanceof Error) {
        error.message = redactSecrets(error.message);
        if (error.stack) error.stack = redactSecrets(error.stack);
      }
      throw error;
    }
  }

  static async create(baseURL: string, user: string, password: string): Promise<EternaClient> {
    const http = await request.newContext({
      baseURL,
      extraHTTPHeaders: { Authorization: basicAuth(user, password), Accept: 'application/json' },
    });
    return new EternaClient(http);
  }

  dispose(): Promise<void> {
    return this.http.dispose();
  }

  async getJson<T>(path: string): Promise<T> {
    return this.json<T>('GET', await this.send(() => this.http.get(path)));
  }

  async postJson<T>(path: string, data: unknown): Promise<T> {
    return this.json<T>('POST', await this.send(() => this.http.post(path, { data })));
  }

  async getBytes(path: string): Promise<Buffer> {
    const response = await this.send(() => this.http.get(path, { headers: { Accept: '*/*' } }));
    await this.ensureOk('GET', response);
    return response.body();
  }

  /** Råsvar utan statuskontroll, för tester som själva kontrollerar statuskoden. */
  getRaw(path: string, headers?: Record<string, string>): Promise<APIResponse> {
    return this.send(() => this.http.get(path, headers ? { headers } : undefined));
  }

  async exists(path: string): Promise<boolean> {
    const response = await this.send(() => this.http.get(path));
    if (response.status() === 404) return false;
    await this.ensureOk('GET', response);
    return true;
  }

  async deleteIfExists(path: string): Promise<void> {
    const response = await this.send(() => this.http.delete(path));
    if (response.status() === 404) return;
    await this.ensureOk('DELETE', response);
  }

  async find<T>(entity: Entity, parameters: FilterParameter[], max = 100): Promise<IndexResult<T>> {
    return this.postJson<IndexResult<T>>(`/api/v2/${entity}/find`, {
      filter: { parameters },
      onlyActive: false,
      sublist: { firstElementIndex: 0, maximumElementCount: max },
    });
  }

  async upload(fileName: string, zip: Buffer): Promise<TransferredResource> {
    const response = await this.send(() =>
      this.http.post('/api/v2/transfers/create/resource', {
        multipart: {
          resource: { name: fileName, mimeType: 'application/zip', buffer: zip },
          commit: 'true',
        },
      }),
    );
    return this.json<TransferredResource>('POST', response);
  }

  private async ensureOk(method: string, response: APIResponse): Promise<void> {
    if (response.ok()) return;
    const body = await response.text().catch(() => '<kunde inte läsa svaret>');
    throw new ApiError(describeFailure(method, response.url(), response.status(), body), response.status());
  }

  private async json<T>(method: string, response: APIResponse): Promise<T> {
    await this.ensureOk(method, response);
    const text = await response.text();
    return parseJsonBody<T>(method, response.url(), response.status(), text);
  }
}

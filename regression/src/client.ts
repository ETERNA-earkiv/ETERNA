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

export interface RawResponse {
  status: number;
  body: Buffer;
}

interface Reply extends RawResponse {
  url: string;
}

/** Bygger på Nodes inbyggda fetch, så Playwright inte loggar anropen (och lösenordet) i sin rapport. */
export class EternaClient {
  private constructor(
    private readonly baseURL: string,
    private readonly authorization: string,
  ) {}

  static create(baseURL: string, user: string, password: string): EternaClient {
    return new EternaClient(baseURL.replace(/\/+$/, ''), basicAuth(user, password));
  }

  /** Alla råa anrop går hit så att inloggningsuppgifter aldrig följer med i kastade fel. */
  private async send(method: string, path: string, init: { headers?: Record<string, string>; body?: BodyInit } = {}): Promise<Reply> {
    const url = `${this.baseURL}${path}`;
    try {
      const response = await fetch(url, {
        method,
        body: init.body,
        headers: { Authorization: this.authorization, Accept: 'application/json', ...init.headers },
      });
      return { url, status: response.status, body: Buffer.from(await response.arrayBuffer()) };
    } catch (error) {
      if (error instanceof Error) {
        error.message = redactSecrets(error.message);
        if (error.stack) error.stack = redactSecrets(error.stack);
      }
      throw error;
    }
  }

  async getJson<T>(path: string): Promise<T> {
    return this.json<T>('GET', await this.send('GET', path));
  }

  async postJson<T>(path: string, data: unknown): Promise<T> {
    const reply = await this.send('POST', path, {
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return this.json<T>('POST', reply);
  }

  async getBytes(path: string): Promise<Buffer> {
    const reply = await this.send('GET', path, { headers: { Accept: '*/*' } });
    this.ensureOk('GET', reply);
    return reply.body;
  }

  /** Råsvar utan statuskontroll, för tester som själva kontrollerar statuskoden. */
  async getRaw(path: string, headers?: Record<string, string>): Promise<RawResponse> {
    const { status, body } = await this.send('GET', path, { headers });
    return { status, body };
  }

  async exists(path: string): Promise<boolean> {
    const reply = await this.send('GET', path);
    if (reply.status === 404) return false;
    this.ensureOk('GET', reply);
    return true;
  }

  async deleteIfExists(path: string): Promise<void> {
    const reply = await this.send('DELETE', path);
    if (reply.status === 404) return;
    this.ensureOk('DELETE', reply);
  }

  async find<T>(entity: Entity, parameters: FilterParameter[], max = 100): Promise<IndexResult<T>> {
    return this.postJson<IndexResult<T>>(`/api/v2/${entity}/find`, {
      filter: { parameters },
      onlyActive: false,
      sublist: { firstElementIndex: 0, maximumElementCount: max },
    });
  }

  async upload(fileName: string, zip: Buffer): Promise<TransferredResource> {
    const form = new FormData();
    form.append('resource', new Blob([new Uint8Array(zip)], { type: 'application/zip' }), fileName);
    form.append('commit', 'true');
    return this.json<TransferredResource>('POST', await this.send('POST', '/api/v2/transfers/create/resource', { body: form }));
  }

  private ensureOk(method: string, reply: Reply): void {
    if (reply.status >= 200 && reply.status < 300) return;
    throw new ApiError(describeFailure(method, reply.url, reply.status, reply.body.toString('utf8')), reply.status);
  }

  private json<T>(method: string, reply: Reply): T {
    this.ensureOk(method, reply);
    return parseJsonBody<T>(method, reply.url, reply.status, reply.body.toString('utf8'));
  }
}

import { createServer, type IncomingMessage, type Server } from 'node:http';
import type { AddressInfo } from 'node:net';
import { expect, test } from '@playwright/test';
import { ApiError, basicAuth, EternaClient } from '../../src/client';

interface Seen {
  method?: string;
  url?: string;
  headers: IncomingMessage['headers'];
  body: string;
}

let server: Server;
let base: string;
let seen: Seen;

test.beforeAll(async () => {
  server = createServer((req, res) => {
    const chunks: Buffer[] = [];
    req.on('data', (chunk: Buffer) => chunks.push(chunk));
    req.on('end', () => {
      seen = { method: req.method, url: req.url, headers: req.headers, body: Buffer.concat(chunks).toString('utf8') };
      if (req.url === '/fel') {
        res.writeHead(500).end('trasigt');
      } else if (req.url === '/bytes') {
        res.writeHead(200).end('rå');
      } else {
        res.writeHead(200, { 'Content-Type': 'application/json' }).end('{"ok":true,"uuid":"u1"}');
      }
    });
  });
  await new Promise<void>((resolve) => server.listen(0, '127.0.0.1', resolve));
  base = `http://127.0.0.1:${(server.address() as AddressInfo).port}`;
});

test.afterAll(() => new Promise<void>((resolve) => server.close(() => resolve())));

test('getJson skickar Authorization och Accept', async () => {
  const client = EternaClient.create(base, 'admin', 'pw');
  expect(await client.getJson('/api/x')).toEqual({ ok: true, uuid: 'u1' });
  expect(seen.headers.authorization).toBe(basicAuth('admin', 'pw'));
  expect(seen.headers.accept).toBe('application/json');
});

test('postJson skickar JSON med Content-Type', async () => {
  const client = EternaClient.create(base, 'admin', 'pw');
  await client.postJson('/api/y', { a: 1 });
  expect(seen.method).toBe('POST');
  expect(seen.headers['content-type']).toBe('application/json');
  expect(JSON.parse(seen.body)).toEqual({ a: 1 });
});

test('upload skickar multipart med resource och commit', async () => {
  const client = EternaClient.create(base, 'admin', 'pw');
  await client.upload('sip.zip', Buffer.from('zipdata'));
  expect(seen.url).toBe('/api/v2/transfers/create/resource');
  expect(seen.headers['content-type']).toMatch(/^multipart\/form-data/);
  expect(seen.body).toMatch(/name="resource"; filename="sip.zip"/);
  expect(seen.body).toContain('application/zip');
  expect(seen.body).toMatch(/name="commit"\r\n\r\ntrue/);
});

test('getRaw ger status och kropp, och Accept kan ändras', async () => {
  const client = EternaClient.create(base, 'admin', 'pw');
  const raw = await client.getRaw('/bytes', { Accept: '*/*' });
  expect(raw.status).toBe(200);
  expect(raw.body.toString('utf8')).toBe('rå');
  expect(seen.headers.accept).toBe('*/*');
});

test('icke-2xx ger ApiError med status', async () => {
  const client = EternaClient.create(base, 'admin', 'pw');
  const error = await client.getJson('/fel').catch((e: unknown) => e);
  expect(error).toBeInstanceOf(ApiError);
  expect((error as ApiError).status).toBe(500);
});

test('fel vid anslutning innehåller inte lösenordet', async () => {
  const client = EternaClient.create('http://127.0.0.1:9', 'admin', 'TOPSECRET');
  const error = (await client.getJson('/x').catch((e: unknown) => e)) as Error;
  expect(error).toBeInstanceOf(Error);
  const text = `${error.message}\n${error.stack}`;
  expect(text).not.toContain('TOPSECRET');
  expect(text).not.toContain(Buffer.from('admin:TOPSECRET').toString('base64'));
});

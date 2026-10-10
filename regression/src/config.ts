export interface Config {
  url: string;
  user: string;
  password: string;
  expectedVersion?: string;
  jobTimeoutMs: number;
  jobPollMs: number;
  keepData: boolean;
}

type Env = Record<string, string | undefined>;

export function loadConfig(env: Env = process.env): Config {
  const url = (env.ETERNA_URL || 'http://localhost:8080').replace(/\/+$/, '');
  if (!/^https?:\/\/\S+$/.test(url)) {
    throw new Error('ETERNA_URL måste börja med http:// eller https://');
  }
  return {
    url,
    user: env.ETERNA_USER || 'admin',
    password: env.ETERNA_PASSWORD || 'eterna',
    expectedVersion: env.EXPECTED_VERSION || undefined,
    jobTimeoutMs: positiveInt(env, 'JOB_TIMEOUT_MS', 600_000),
    jobPollMs: positiveInt(env, 'JOB_POLL_MS', 3_000),
    keepData: env.KEEP_DATA === '1',
  };
}

function positiveInt(env: Env, name: string, fallback: number): number {
  const raw = env[name];
  if (raw === undefined || raw === '') return fallback;
  const value = Number(raw);
  if (!Number.isInteger(value) || value <= 0) {
    throw new Error(`${name} måste vara ett positivt heltal, fick "${raw}"`);
  }
  return value;
}

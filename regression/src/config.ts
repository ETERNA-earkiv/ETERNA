export interface Config {
  url: string;
  user: string;
  password: string;
  expectedVersion?: string;
  jobTimeoutMs: number;
  jobPollMs: number;
  keepData: boolean;
}

export function loadConfig(env: Record<string, string | undefined> = process.env): Config {
  return {
    url: env.ETERNA_URL ?? 'http://localhost:8080',
    user: 'admin',
    password: 'eterna',
    jobTimeoutMs: 600_000,
    jobPollMs: 3_000,
    keepData: false,
  };
}

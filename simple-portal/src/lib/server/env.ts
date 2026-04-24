/**
 * Server-side environment variables.
 * Importera BARA från server-kontext (middleware, API routes, SSR).
 * Kastar vid startup om obligatoriska variabler saknas.
 */

function getEnv(key: string, fallback?: string): string {
  // Astro exponerar env vars via import.meta.env, process.env som fallback
  const val =
    (import.meta.env?.[key] as string | undefined) ??
    (typeof process !== 'undefined' ? process.env[key] : undefined) ??
    fallback;

  if (val === undefined || val === '') {
    throw new Error(
      `Miljövariabel ${key} saknas. Se .env.example för dokumentation.`,
    );
  }
  return val;
}

export const RODA_API_URL = getEnv('RODA_API_URL', 'http://localhost:8080');
export const PORTAL_SERVICE_USER = getEnv('PORTAL_SERVICE_USER');
export const PORTAL_SERVICE_PASSWORD = getEnv('PORTAL_SERVICE_PASSWORD');

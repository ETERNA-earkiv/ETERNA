/**
 * Config store — appkonfiguration, sökkonfig och synlighetsregler.
 */

import { atom, computed } from 'nanostores';
import { loadConfig as fetchConfig, type AppConfig } from '../api/config';

export const $config = atom<AppConfig | null>(null);
export const $configLoading = atom(false);
export const $configError = atom<string | null>(null);

export const $siteConfig = computed($config, (c) => c?.siteConfig ?? null);
export const $searchConfig = computed($config, (c) => c?.searchConfig ?? null);
export const $visibilityConfig = computed($config, (c) => c?.visibilityConfig ?? null);
export const $siteName = computed($siteConfig, (s) => s?.siteName ?? 'E-Arkiv Portal');
export const $resultsPerPage = computed($searchConfig, (s) => s?.resultsPerPage ?? 10);
export const $isLoaded = computed($config, (c) => c != null);

export async function initConfig(): Promise<void> {
  if ($config.get()) return;
  $configLoading.set(true);
  $configError.set(null);
  try {
    const config = await fetchConfig();
    $config.set(config);
  } catch (err) {
    $configError.set(err instanceof Error ? err.message : 'Kunde inte ladda konfiguration');
  } finally {
    $configLoading.set(false);
  }
}

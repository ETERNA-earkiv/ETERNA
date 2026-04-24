/**
 * Configuration API - appkonfiguration och tema.
 */

import { apiGet } from './client';
import {
  DEFAULT_SITE_THEME,
  applyThemeToDocument,
  normalizeSiteTheme,
} from '@lib/theme/theme';
import { DEFAULT_ADVANCED_SEARCH_FIELDS } from '@lib/config/search-fields';

export interface SiteConfig {
  siteName: string;
  primaryColor: string;
  secondaryColor: string;
  logoUrl?: string;
}

export interface SearchFieldConfig {
  fieldName: string;
  label: string;
  type: string;
}

export interface SearchConfig {
  mainSearchField: SearchFieldConfig;
  advancedFields: SearchFieldConfig[];
  resultsPerPage: number;
}

export interface XpathRule {
  metadataId: string;
  xpath: string;
  fullAccessValues: string[];
  titleOnlyValues: string[];
  hiddenValues: string[];
}

export interface VisibilityConfig {
  allowedLevels: string[];
  xpathRulesEnabled: boolean;
  xpathRules: XpathRule[];
  visibleMetadataFields: Record<string, string[]>;
  /** Alla fält som upptäckts via AIP-referens, oavsett om de visas */
  knownMetadataFields?: Record<string, string[]>;
}

export interface AppConfig {
  siteConfig: SiteConfig;
  searchConfig: SearchConfig;
  visibilityConfig: VisibilityConfig;
}

const DEFAULT_CONFIG: AppConfig = {
  siteConfig: {
    ...DEFAULT_SITE_THEME,
  },
  searchConfig: {
    mainSearchField: { fieldName: 'search', label: 'Sök i arkivet', type: 'text' },
    advancedFields: DEFAULT_ADVANCED_SEARCH_FIELDS,
    resultsPerPage: 10,
  },
  visibilityConfig: {
    allowedLevels: [],
    xpathRulesEnabled: false,
    xpathRules: [],
    visibleMetadataFields: {},
  },
};

let configCache: AppConfig | null = null;
let configPromise: Promise<AppConfig> | null = null;

/** Hämta config med cache (säker för söksida, filter, tema etc.) */
export async function loadConfig(): Promise<AppConfig> {
  if (configCache) return configCache;
  if (configPromise) return configPromise;

  configPromise = fetchAndMergeConfig();
  configCache = await configPromise;
  configPromise = null;
  return configCache;
}

/** Hämta config utan cache (admin-sidor som behöver färska värden) */
export async function loadConfigFresh(): Promise<AppConfig> {
  configCache = null;
  configPromise = null;
  return loadConfig();
}

/** Invalidera cache efter sparning */
export function invalidateConfigCache(): void {
  configCache = null;
  configPromise = null;
}

async function fetchAndMergeConfig(): Promise<AppConfig> {
  try {
    const appConfig = await apiGet<AppConfig>('/api/config').catch(() => DEFAULT_CONFIG);

    const merged: AppConfig = {
      ...DEFAULT_CONFIG,
      ...appConfig,
      siteConfig: normalizeSiteTheme(appConfig?.siteConfig),
    };

    if (typeof document !== 'undefined') {
      applyThemeToDocument(merged.siteConfig);
    }

    return merged;
  } catch {
    return DEFAULT_CONFIG;
  }
}


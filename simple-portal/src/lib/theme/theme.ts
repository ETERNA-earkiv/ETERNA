export interface ThemePreset {
  label: string;
  color: string;
  contrastRatio: string;
}

export interface SiteTheme {
  siteName: string;
  primaryColor: string;
  secondaryColor: string;
  logoUrl?: string;
}

export const THEME_PRESETS: ThemePreset[] = [
  { label: 'Röd', color: '#C4122A', contrastRatio: '5.8:1' },
  { label: 'Blå', color: '#1E40AF', contrastRatio: '8.4:1' },
  { label: 'Grön', color: '#15803D', contrastRatio: '4.8:1' },
  { label: 'Lila', color: '#6D28D9', contrastRatio: '6.8:1' },
  { label: 'Teal', color: '#0F766E', contrastRatio: '5.2:1' },
  { label: 'Orange', color: '#9A3412', contrastRatio: '7.0:1' },
  { label: 'Senap', color: '#8A5A00', contrastRatio: '5.7:1' },
  { label: 'Bordeaux', color: '#8A1538', contrastRatio: '9.0:1' },
  { label: 'Mossa', color: '#3F6212', contrastRatio: '6.8:1' },
  { label: 'Kol', color: '#1F2937', contrastRatio: '14.1:1' },
];

export const DEFAULT_SITE_THEME: SiteTheme = {
  siteName: 'E-Arkiv Portal',
  primaryColor: '#C4122A',
  secondaryColor: '#424242',
};

export function getThemePresetByColor(color: string) {
  return THEME_PRESETS.find((p) => p.color.toLowerCase() === color.toLowerCase());
}

export function normalizeSiteTheme(theme?: Partial<SiteTheme> | null): SiteTheme {
  return {
    siteName: theme?.siteName?.trim() || DEFAULT_SITE_THEME.siteName,
    primaryColor: theme?.primaryColor || DEFAULT_SITE_THEME.primaryColor,
    secondaryColor: theme?.secondaryColor || DEFAULT_SITE_THEME.secondaryColor,
    logoUrl: theme?.logoUrl || '',
  };
}

export function applyThemeToDocument(theme: SiteTheme) {
  if (typeof document === 'undefined') return;
  const root = document.documentElement;
  root.style.setProperty('--brand1', theme.primaryColor);
  root.style.setProperty('--brand2', theme.secondaryColor);
  root.style.setProperty('--focus-color', theme.primaryColor);

  // Uppdatera portalnamn i header/footer (bara i body, aldrig html)
  for (const el of document.body.querySelectorAll<HTMLElement>('[data-site-name]')) {
    el.textContent = theme.siteName;
  }
}

/**
 * Hämta tema från config.json (samma källa för alla besökare).
 */
export async function loadThemeFromConfig(): Promise<SiteTheme> {
  try {
    const { loadConfig } = await import('../api/config');
    const config = await loadConfig();
    return normalizeSiteTheme(config?.siteConfig);
  } catch {
    return DEFAULT_SITE_THEME;
  }
}

/**
 * Spara tema till config.json via API.
 */
export async function saveTheme(theme: SiteTheme): Promise<{ theme: SiteTheme; saved: boolean }> {
  const normalized = normalizeSiteTheme(theme);

  try {
    const res = await fetch('/api/config', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ siteConfig: normalized }),
    });

    if (!res.ok) throw new Error('Save failed');
    return { theme: normalized, saved: true };
  } catch {
    return { theme: normalized, saved: false };
  }
}

/**
 * Hydrera tema vid sidladdning — läser från config.json.
 */
export async function hydrateTheme(): Promise<SiteTheme> {
  const theme = await loadThemeFromConfig();
  applyThemeToDocument(theme);
  return theme;
}


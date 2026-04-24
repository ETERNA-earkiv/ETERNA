import { useEffect, useRef, useState } from 'react';
import {
  DEFAULT_SITE_THEME,
  THEME_PRESETS,
  hydrateTheme,
  normalizeSiteTheme,
  saveTheme,
  type SiteTheme,
} from '@lib/theme/theme';
import { PortalInput } from '../portal-ui/PortalInput';
import { PortalButton } from '../portal-ui/PortalButton';
import { PortalAlert } from '../portal-ui/PortalAlert';
import './ThemeSettings.css';

export function ThemeSettings() {
  const [theme, setTheme] = useState<SiteTheme>(DEFAULT_SITE_THEME);
  const [status, setStatus] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const fileRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    hydrateTheme().then((t) => setTheme(normalizeSiteTheme(t)));
  }, []);

  // Only apply color changes live (not site name — that would alter DOM React doesn't own)
  useEffect(() => {
    document.documentElement.style.setProperty('--brand1', theme.primaryColor);
  }, [theme.primaryColor]);

  function update(next: Partial<SiteTheme>) {
    setTheme((cur) => normalizeSiteTheme({ ...cur, ...next }));
    setStatus(null);
    setError(null);
  }

  async function handleSave() {
    setSaving(true);
    setStatus(null);
    setError(null);
    try {
      const { invalidateConfigCache } = await import('@lib/api/config');
      const result = await saveTheme(theme);
      setTheme(result.theme);
      invalidateConfigCache();
      window.location.reload();
    } catch {
      setError('Kunde inte spara temat.');
      setSaving(false);
    }
  }

  async function handleLogoUpload(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;
    if (file.size > 2 * 1024 * 1024) { setError('Max 2 MB.'); return; }

    const reader = new FileReader();
    reader.onload = () => {
      update({ logoUrl: reader.result as string });
      setStatus('Logotyp vald. Klicka Spara tema för att bekräfta.');
    };
    reader.readAsDataURL(file);
    if (fileRef.current) fileRef.current.value = '';
  }

  function removeLogo() {
    update({ logoUrl: '' });
    setStatus('Logotyp borttagen. Spara för att bekräfta.');
  }

  return (
    <div className="theme-page">
      <h1>Tema</h1>
      <p className="theme-page__subtitle">Anpassa portalens utseende med logotyp och varumärkesfärg.</p>

      {status && (
        <PortalAlert variant="success" size="small">
          {status}
        </PortalAlert>
      )}
      {error && (
        <PortalAlert variant="danger" size="small">
          {error}
        </PortalAlert>
      )}

      {/* Site name */}
      <div className="theme-section">
        <PortalInput
          label="Portalnamn"
          description="Visas bredvid logotypen i headern."
          value={theme.siteName}
          onChange={(val) => update({ siteName: val })}
          placeholder="E-Arkiv Portal"
        />
      </div>

      <hr className="theme-divider" />

      {/* Logo */}
      <div className="theme-section">
        <h3>Logotyp</h3>
        <p className="theme-desc">Bilden visas i portalens header. Rekommenderad storlek: 200×100 px.</p>
        {theme.logoUrl ? (
          <div className="theme-logo-row">
            <img src={theme.logoUrl} alt="Nuvarande logotyp" className="theme-logo-img" />
            <div className="theme-logo-actions">
              <PortalButton variant="secondary" size="small" onClick={() => fileRef.current?.click()} ariaLabel="Byt logotyp">
                Byt logotyp
              </PortalButton>
              <button type="button" className="theme-remove-btn" onClick={removeLogo} aria-label="Ta bort logotyp">
                Ta bort
              </button>
            </div>
          </div>
        ) : (
          <div className="theme-logo-row">
            <div className="theme-logo-placeholder">
              <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" aria-hidden="true">
                <rect width="18" height="18" x="3" y="3" rx="2" /><circle cx="9" cy="9" r="2" /><path d="m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21" />
              </svg>
              <span>Ingen logotyp uppladdad</span>
            </div>
            <PortalButton variant="secondary" size="small" onClick={() => fileRef.current?.click()} ariaLabel="Ladda upp logotyp">
              Ladda upp logotyp
            </PortalButton>
          </div>
        )}
        <input ref={fileRef} type="file" accept="image/*" onChange={handleLogoUpload} className="sr-only" tabIndex={-1} />
      </div>

      <hr className="theme-divider" />

      {/* Brand color */}
      <div className="theme-section">
        <h3>Varumärkesfärg</h3>
        <p className="theme-desc">Styr färgen på knappar, länkar, ramar och andra accentelement. Alla färger är verifierade mot portalens ljusa bakgrund.</p>
        <div className="theme-color-grid" role="radiogroup" aria-label="Välj varumärkesfärg">
          {THEME_PRESETS.map((preset) => (
            <button
              key={preset.color}
              type="button"
              className={`theme-swatch ${theme.primaryColor === preset.color ? 'theme-swatch--selected' : ''}`}
              style={{ backgroundColor: preset.color }}
              onClick={() => update({ primaryColor: preset.color })}
              role="radio"
              aria-checked={theme.primaryColor === preset.color}
              aria-label={`${preset.label} (${preset.color}, kontrast ${preset.contrastRatio})`}
            >
              {theme.primaryColor === preset.color && (
                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="3"><polyline points="20 6 9 17 4 12" /></svg>
              )}
            </button>
          ))}
        </div>

        {/* Save */}
        <div className="theme-actions">
          <PortalButton variant="primary" onClick={handleSave} disabled={saving} loading={saving}>
            Spara tema
          </PortalButton>
        </div>

        {/* Preview */}
        <div className="theme-preview">
          <span className="theme-preview__label">Förhandsgranskning:</span>
          <span className="theme-preview__btn" style={{ backgroundColor: theme.primaryColor }}>Exempelknapp</span>
          <span className="theme-preview__link" style={{ color: theme.primaryColor }}>Exempellänk</span>
          <span className="theme-preview__badge" style={{ backgroundColor: theme.primaryColor }}>3</span>
        </div>
      </div>
    </div>
  );
}

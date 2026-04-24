import { useEffect, useState } from 'react';
import { LevelSelector } from './LevelSelector';
import { XpathRuleEditor } from './XpathRuleEditor';
import { SearchFieldConfig } from './SearchFieldConfig';
import { MetadataFieldConfig } from './MetadataFieldConfig';
import { PortalSpinner } from '../portal-ui/PortalSpinner';
import { PortalAlert } from '../portal-ui/PortalAlert';
import { PortalButton } from '../portal-ui/PortalButton';
import { loadConfigFresh, invalidateConfigCache } from '@lib/api/config';
import type { VisibilityConfig } from '@lib/api/config';
import './MetadataConfig.css';

const DEFAULT_VIS: VisibilityConfig = {
  allowedLevels: [],
  xpathRulesEnabled: false,
  visibleMetadataFields: {},
  xpathRules: [],
};

export function MetadataConfig() {
  const [visConfig, setVisConfig] = useState<VisibilityConfig>(DEFAULT_VIS);
  const [status, setStatus] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    loadConfigFresh()
      .then((config) => {
        if (config?.visibilityConfig) setVisConfig(config.visibilityConfig);
      })
      .catch(() => {
        setError('Konfigurationen kunde inte laddas — standardvärden används. Ändringar du gör kan skriva över befintlig konfiguration.');
      })
      .finally(() => setLoaded(true));
  }, []);

  function updateVis(next: Partial<VisibilityConfig>) {
    setVisConfig((cur) => ({ ...cur, ...next }));
    setStatus(null);
    setError(null);
  }

  async function handleSave() {
    setSaving(true);
    setStatus(null);
    setError(null);
    try {
      const res = await fetch('/api/config', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ visibilityConfig: visConfig }),
      });
      if (!res.ok) throw new Error('Sparning misslyckades');
      invalidateConfigCache();
      setStatus('Inställningarna sparades.');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Kunde inte spara.');
    } finally {
      setSaving(false);
    }
  }

  if (!loaded) return <PortalSpinner />;

  return (
    <section className="meta-config">
      <h1>Metadata och synlighet</h1>
      <p className="meta-config__desc">
        Konfigurera vilka beskrivningsnivåer, sökfält och synlighetsregler som gäller i portalen.
      </p>

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

      <div className="meta-config__section">
        <LevelSelector
          selected={visConfig.allowedLevels}
          onChange={(levels) => updateVis({ allowedLevels: levels })}
        />
      </div>

      <div className="meta-config__section">
        <SearchFieldConfig />
      </div>

      <div className="meta-config__section">
        <MetadataFieldConfig
          visibleFields={
            Array.isArray(visConfig.visibleMetadataFields)
              ? {}
              : visConfig.visibleMetadataFields || {}
          }
          knownFields={visConfig.knownMetadataFields || {}}
          onChange={(visible, known) => updateVis({
            visibleMetadataFields: visible,
            knownMetadataFields: known,
          })}
        />
      </div>

      <div className="meta-config__section">
        <XpathRuleEditor
          enabled={visConfig.xpathRulesEnabled}
          rules={visConfig.xpathRules}
          onEnabledChange={(enabled) => updateVis({ xpathRulesEnabled: enabled })}
          onRulesChange={(rules) => updateVis({ xpathRules: rules })}
        />
      </div>

      <PortalButton
        variant="primary"
        onClick={handleSave}
        disabled={saving}
        loading={saving}
        ariaLabel="Spara metadata-konfiguration"
      >
        Spara alla inställningar
      </PortalButton>
    </section>
  );
}

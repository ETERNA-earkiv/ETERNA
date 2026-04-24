import { useEffect, useRef, useState } from 'react';
import { translateLabel } from '@lib/utils/i18n';
import { PortalButton } from '../portal-ui/PortalButton';
import { PortalAlert } from '../portal-ui/PortalAlert';
import { PortalSpinner } from '../portal-ui/PortalSpinner';
import { loadConfigFresh, invalidateConfigCache } from '@lib/api/config';
import './SearchFieldConfig.css';

export interface FieldConfig {
  fieldName: string;
  label: string;
  type: 'select' | 'text' | 'date-range';
  enabled: boolean;
}

const DEFAULT_ADVANCED_SEARCH_FIELDS: FieldConfig[] = [
  { fieldName: 'level', label: 'Beskrivningsnivå', type: 'select', enabled: true },
  { fieldName: 'type', label: 'Typ', type: 'select', enabled: true },
  { fieldName: 'createdOn', label: 'Skapad', type: 'date-range', enabled: false },
];

export function SearchFieldConfig() {
  const [fields, setFields] = useState<FieldConfig[]>([]);
  const [availableFields, setAvailableFields] = useState<string[]>([]);

  const [status, setStatus] = useState<string | null>(null);
  const [loaded, setLoaded] = useState(false);
  const [discovering, setDiscovering] = useState(false);
  const discoveredLabelsRef = useRef<Record<string, string>>({});
  const discoveredTypesRef = useRef<Record<string, FieldConfig['type']>>({});

  useEffect(() => {
    loadConfigFresh()
      .then((config) => {
        const saved = config?.searchConfig?.advancedFields as FieldConfig[] | undefined;
        setFields(Array.isArray(saved) && saved.length > 0 ? saved : DEFAULT_ADVANCED_SEARCH_FIELDS);
      })
      .catch(() => {
        setFields(DEFAULT_ADVANCED_SEARCH_FIELDS);
        setStatus('Kunde inte ladda sparade sökfält — standardfält visas.');
      })
      .finally(() => setLoaded(true));
  }, []);

  async function discoverFields() {
    setDiscovering(true);
    setStatus(null);
    try {
      const res = await fetch('/api/v2/configurations/shared-properties', {
        credentials: 'include',
      });
      if (!res.ok) throw new Error(`${res.status}`);
      const data = await res.json();
      const props: Record<string, string[]> = data.properties || data;

      const fieldList: string[] = props['ui.search.fields.IndexedAIP'] || [];

      const candidates: { fieldName: string; label: string; type: FieldConfig['type'] }[] = [];
      for (const fieldName of fieldList) {
        const i18nKey = `i18n.ui.search.fields.IndexedAIP.${fieldName}`;
        const englishLabel = props[i18nKey]?.[0] || fieldName;
        const label = translateLabel(englishLabel);

        const typeKey = `ui.search.fields.IndexedAIP.${fieldName}.type`;
        const rodaType = props[typeKey]?.[0] || 'text';
        let type: FieldConfig['type'] = 'text';
        if (rodaType === 'date_interval') type = 'date-range';
        else if (rodaType === 'controlled') type = 'select';

        candidates.push({ fieldName, label, type });
      }

      const existing = new Set(fields.map((f) => f.fieldName));
      const newFields = candidates.filter((f) => !existing.has(f.fieldName));
      setAvailableFields(newFields.map((f) => f.fieldName));

      discoveredLabelsRef.current = Object.fromEntries(candidates.map((f) => [f.fieldName, f.label]));
      discoveredTypesRef.current = Object.fromEntries(candidates.map((f) => [f.fieldName, f.type]));

      setStatus(`Hittade ${candidates.length} sökfält konfigurerade i ETERNA.`);
    } catch (err) {
      setStatus(`Kunde inte hämta fält: ${err instanceof Error ? err.message : 'Okänt fel'}`);
    } finally {
      setDiscovering(false);
    }
  }

  function update(index: number, changes: Partial<FieldConfig>) {
    setFields((prev) => prev.map((f, i) => i === index ? { ...f, ...changes } : f));
    setStatus(null);
  }

  function moveUp(index: number) {
    if (index <= 0) return;
    setFields((prev) => {
      const next = [...prev];
      [next[index - 1], next[index]] = [next[index], next[index - 1]];
      return next;
    });
  }

  function moveDown(index: number) {
    if (index >= fields.length - 1) return;
    setFields((prev) => {
      const next = [...prev];
      [next[index], next[index + 1]] = [next[index + 1], next[index]];
      return next;
    });
  }

  function removeField(index: number) {
    setFields((prev) => prev.filter((_, i) => i !== index));
  }

  function addField(fieldName: string) {
    if (!fieldName.trim()) return;
    if (fields.some((f) => f.fieldName === fieldName)) return;
    const label = discoveredLabelsRef.current[fieldName] || fieldName;
    const type: FieldConfig['type'] = discoveredTypesRef.current[fieldName] || 'text';
    setFields((prev) => [...prev, {
      fieldName,
      label,
      type,
      enabled: true,
    }]);
    setAvailableFields((prev) => prev.filter((f) => f !== fieldName));
  }

  async function save() {
    setStatus(null);
    try {
      const res = await fetch('/api/config', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ searchConfig: { advancedFields: fields } }),
      });
      if (res.ok) {
        invalidateConfigCache();
        setStatus('Sökfältskonfigurationen sparades.');
      } else {
        setStatus('Kunde inte spara.');
      }
    } catch {
      setStatus('Kunde inte spara.');
    }
  }

  if (!loaded) return <PortalSpinner />;

  return (
    <section className="sfc">
      <h3>Sökfilter</h3>
      <p className="sfc__desc">Konfigurera vilka fält som visas som filter i sökpanelen. Fälten hämtas från ETERNA:s Solr-index.</p>

      <PortalButton
        variant="secondary"
        onClick={discoverFields}
        disabled={discovering}
        loading={discovering}
        ariaLabel="Hämta tillgängliga fält från ETERNA"
      >
        Hämta fält från ETERNA
      </PortalButton>

      {status && (
        <PortalAlert variant="info" size="small">
          {status}
        </PortalAlert>
      )}

      <div className="sfc__list" role="list" aria-label="Konfigurerade sökfält">
        {fields.map((field, i) => (
          <div key={i} className={`sfc__row ${!field.enabled ? 'sfc__row--disabled' : ''}`} role="listitem">
            {/* Native inline-kontroller i tabellrader — Digi:s Shadow DOM-labels passar inte i kompakt grid */}
            <input
              type="checkbox"
              checked={field.enabled}
              onChange={(e) => update(i, { enabled: e.target.checked })}
              aria-label={`Aktivera ${field.fieldName}`}
            />
            <div className="sfc__info">
              <input
                type="text"
                value={field.label}
                onChange={(e) => update(i, { label: e.target.value })}
                className="sfc__label-input"
                placeholder="Visningsnamn"
                aria-label={`Visningsnamn för ${field.fieldName}`}
              />
              <span className="sfc__field-name">{field.fieldName}</span>
            </div>
            <select
              value={field.type}
              onChange={(e) => update(i, { type: e.target.value as FieldConfig['type'] })}
              className="sfc__type-select"
              aria-label={`Filtertyp för ${field.fieldName}`}
            >
              <option value="select">Flerval</option>
              <option value="text">Fritext</option>
              <option value="date-range">Datumintervall</option>
            </select>
            <div className="sfc__actions">
              <button type="button" onClick={() => moveUp(i)} disabled={i === 0} aria-label="Flytta upp">↑</button>
              <button type="button" onClick={() => moveDown(i)} disabled={i === fields.length - 1} aria-label="Flytta ner">↓</button>
              <button type="button" onClick={() => removeField(i)} aria-label="Ta bort fält" className="sfc__remove">×</button>
            </div>
          </div>
        ))}
      </div>

      {/* Native select — dynamiska options kraschar i Digi Shadow DOM vid removeChild */}
      {availableFields.length > 0 && (
        <div className="sfc__available">
          <label htmlFor="sfc-available-select">Lägg till fält från ETERNA:</label>
          <select
            id="sfc-available-select"
            onChange={(e) => { if (e.target.value) addField(e.target.value); e.target.value = ''; }}
            aria-label="Välj fält att lägga till"
          >
            <option value="">Välj fält...</option>
            {availableFields.map((f) => (
              <option key={f} value={f}>{discoveredLabelsRef.current[f] || f} ({f})</option>
            ))}
          </select>
        </div>
      )}

      <PortalButton variant="primary" onClick={save} ariaLabel="Spara sökfältskonfiguration">
        Spara sökfält
      </PortalButton>
    </section>
  );
}

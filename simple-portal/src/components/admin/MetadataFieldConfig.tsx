import { useEffect, useState } from 'react';
import { parseXmlToFields } from '@lib/utils/metadata-parser';
import { formatStandardName, normalizeMetadataStandardId } from '@lib/utils/i18n';
import { PortalButton } from '../portal-ui/PortalButton';
import { PortalAlert } from '../portal-ui/PortalAlert';
import { PortalAccordion } from '../portal-ui/PortalAccordion';
import './MetadataFieldConfig.css';

/** Per-standard fältval: { "ead_2002": ["Titel", "Datum"], "ead_3": ["Titel"] } */
export type VisibleFieldsByStandard = Record<string, string[]>;
/** Per-standard alla kända fält: { "ead_2002": ["Titel", "Datum", "Referenskod", ...] } */
export type KnownFieldsByStandard = Record<string, string[]>;

interface DiscoveredStandard {
  id: string;
  label: string;
  fields: { label: string; sample: string }[];
}

interface Props {
  visibleFields: VisibleFieldsByStandard;
  knownFields: KnownFieldsByStandard;
  onChange: (visible: VisibleFieldsByStandard, known: KnownFieldsByStandard) => void;
}

export function MetadataFieldConfig({ visibleFields, knownFields, onChange }: Props) {
  const [aipId, setAipId] = useState('');
  const [discovering, setDiscovering] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [standardsMap, setStandardsMap] = useState<Map<string, DiscoveredStandard>>(new Map());
  const [selected, setSelected] = useState<VisibleFieldsByStandard>(visibleFields);
  const [expanded, setExpanded] = useState<Set<string>>(new Set());

  useEffect(() => { setSelected(visibleFields); }, [visibleFields]);

  // Bygg upp standards från knownFields vid mount
  useEffect(() => {
    const source = (typeof knownFields === 'object' && !Array.isArray(knownFields) && Object.keys(knownFields).length > 0)
      ? knownFields
      : (typeof visibleFields === 'object' && !Array.isArray(visibleFields) && Object.keys(visibleFields).length > 0)
        ? visibleFields
        : null;

    if (source && standardsMap.size === 0) {
      const map = new Map<string, DiscoveredStandard>();
      for (const [id, fields] of Object.entries(source)) {
        const nid = normalizeStandardId(id);
        const existing = map.get(nid);
        const mergedFields = new Set([
          ...(existing?.fields.map((field) => field.label) || []),
          ...(Array.isArray(fields) ? fields : []),
        ]);

        map.set(nid, {
          id: nid,
          label: existing?.label || formatStandardName(id),
          fields: Array.from(mergedFields).map((label) => ({ label, sample: '' })),
        });
      }
      setStandardsMap(map);
    }
  }, [knownFields, visibleFields]);

  /** Hämta fält från ett AIP och mergea med befintliga kända fält */
  async function discover() {
    if (!aipId.trim()) return;
    setDiscovering(true);
    setError(null);

    try {
      const infoRes = await fetch(`/api/v2/aips/${aipId.trim()}/metadata/descriptive/information`, { credentials: 'include' });
      if (!infoRes.ok) throw new Error(`Kunde inte hämta (${infoRes.status})`);
      const info = await infoRes.json();
      const stdList: { id: string; label: string }[] = info?.descriptiveMetadataInfoList || [];

      if (stdList.length === 0) {
        setError('Ingen metadata hittades.');
        return;
      }

      const updated = new Map(standardsMap);
      const skipped: string[] = [];

      for (const std of stdList) {
        const nid = normalizeStandardId(std.id);

        try {
          const xmlRes = await fetch(`/api/v2/aips/${aipId.trim()}/metadata/descriptive/${std.id}/download`, { credentials: 'include' });
          if (!xmlRes.ok) { skipped.push(std.id); continue; }
          const xml = await xmlRes.text();
          const fields = parseXmlToFields(xml);

          let existing = updated.get(nid);
          if (!existing) {
            existing = { id: nid, label: std.label || formatStandardName(std.id), fields: [] };
            updated.set(nid, existing);
          }

          // Mergea nya fält — plussa ihop från flera AIP:er.
          // Om fältet redan finns, uppdatera sample från senast inlästa AIP
          // så att tidigare config-fält också får ett visningsvärde.
          const existingByLabel = new Map(existing.fields.map((field) => [field.label, field]));
          for (const f of fields) {
            const existingField = existingByLabel.get(f.label);
            if (existingField) {
              existingField.sample = f.value.slice(0, 80);
            } else {
              existing.fields.push({ label: f.label, sample: f.value.slice(0, 80) });
              existingByLabel.set(f.label, existing.fields[existing.fields.length - 1]);
            }
          }
        } catch { skipped.push(std.id); }
      }

      setStandardsMap(updated);

      // Auto-expandera nyligen hämtade
      const newExpanded = new Set(expanded);
      for (const std of stdList) newExpanded.add(normalizeStandardId(std.id));
      setExpanded(newExpanded);

      // Nya standarder som inte har val → markera alla som synliga
      const newSelected = { ...selected };
      for (const [nid, std] of updated) {
        if (!newSelected[nid] || newSelected[nid].length === 0) {
          newSelected[nid] = std.fields.map((f) => f.label);
        }
      }
      setSelected(newSelected);

      // Spara både visible och ALLA kända fält
      const newKnown = buildKnownFields(updated);
      onChange(newSelected, newKnown);

      if (skipped.length > 0) {
        setError(`${skipped.length} standard(er) kunde inte laddas: ${skipped.join(', ')}`);
      }

    } catch (err) {
      setError(err instanceof Error ? err.message : 'Kunde inte hämta metadatafält');
    } finally {
      setDiscovering(false);
    }
  }

  function toggleField(standardId: string, label: string) {
    const nid = normalizeStandardId(standardId);
    const current = selected[nid] || [];
    const next = current.includes(label) ? current.filter((l) => l !== label) : [...current, label];
    const updated = { ...selected, [nid]: next };
    setSelected(updated);
    onChange(updated, buildKnownFields(standardsMap));
  }

  function selectAllForStandard(standardId: string) {
    const nid = normalizeStandardId(standardId);
    const std = standardsMap.get(nid);
    if (!std) return;
    const updated = { ...selected, [nid]: std.fields.map((f) => f.label) };
    setSelected(updated);
    onChange(updated, buildKnownFields(standardsMap));
  }

  function selectNoneForStandard(standardId: string) {
    const nid = normalizeStandardId(standardId);
    const updated = { ...selected, [nid]: [] };
    setSelected(updated);
    onChange(updated, buildKnownFields(standardsMap));
  }

  function toggleExpanded(standardId: string) {
    const nid = normalizeStandardId(standardId);
    const next = new Set(expanded);
    if (next.has(nid)) next.delete(nid);
    else next.add(nid);
    setExpanded(next);
  }

  function buildKnownFields(map: Map<string, DiscoveredStandard>): KnownFieldsByStandard {
    const known: KnownFieldsByStandard = {};
    for (const [nid, std] of map) {
      known[nid] = std.fields.map((f) => f.label);
    }
    return known;
  }

  return (
    <div className="mfc">
      <h3>Synliga metadatafält</h3>
      <p className="mfc__help">
        Välj vilka fält som visas i portalen och inkluderas vid paketnedladdning, grupperat per metadata-standard.
        Hämta fält från ett eller flera AIP:er — nya standarder och fält läggs till automatiskt.
      </p>

      <div className="mfc__discover">
        <label htmlFor="mfc-aip-id" className="mfc__label">AIP-ID (sparas ej)</label>
        <div className="mfc__row">
          <input
            id="mfc-aip-id"
            type="text"
            className="mfc__input"
            value={aipId}
            onChange={(e) => setAipId(e.target.value)}
            placeholder="Klistra in ett AIP-ID"
            onKeyDown={(e) => e.key === 'Enter' && discover()}
          />
          <PortalButton
            variant="secondary"
            onClick={discover}
            disabled={!aipId.trim() || discovering}
            loading={discovering}
          >
            Hämta fält
          </PortalButton>
        </div>
      </div>

      {error && (
        <PortalAlert variant="danger" size="small">
          {error}
        </PortalAlert>
      )}

      {standardsMap.size > 0 && (
        <div className="mfc__standards">
          {Array.from(standardsMap.entries()).map(([nid, std]) => {
            const isOpen = expanded.has(nid);
            const stdSelected = selected[nid] || [];
            const count = stdSelected.length;
            const total = std.fields.length;

            return (
              <PortalAccordion
                key={nid}
                heading={`${std.label || formatStandardName(std.id)} — ${count} av ${total} fält`}
                expanded={isOpen}
                onToggle={() => toggleExpanded(std.id)}
              >
                <div className="mfc__standard-body">
                  <div className="mfc__toolbar">
                    <PortalButton variant="function" size="small" onClick={() => selectAllForStandard(std.id)}>Markera alla</PortalButton>
                    <PortalButton variant="function" size="small" onClick={() => selectNoneForStandard(std.id)}>Avmarkera alla</PortalButton>
                  </div>

                  <div className="mfc__list" role="list">
                    {std.fields.map((field) => (
                      <label key={field.label} className={`mfc__item ${stdSelected.includes(field.label) ? 'mfc__item--selected' : ''}`} role="listitem">
                        <input type="checkbox" checked={stdSelected.includes(field.label)} onChange={() => toggleField(std.id, field.label)} aria-label={`Visa ${field.label}`} />
                        <div className="mfc__field-info">
                          <strong>{field.label}</strong>
                          {field.sample && <span className="mfc__sample">{field.sample}</span>}
                        </div>
                      </label>
                    ))}
                  </div>
                </div>
              </PortalAccordion>
            );
          })}
        </div>
      )}
    </div>
  );
}

function normalizeStandardId(id: string): string {
  return normalizeMetadataStandardId(id);
}

import { useState, useRef } from 'react';
import { evaluateXpath } from '@lib/utils/xpath';
import { apiGetXml } from '@lib/api/client';
import type { XpathRule } from '@lib/api/config';
import { PortalCheckbox } from '../portal-ui/PortalCheckbox';
import { PortalButton } from '../portal-ui/PortalButton';
import { PortalAlert } from '../portal-ui/PortalAlert';
import './XpathRuleEditor.css';

interface Props {
  enabled: boolean;
  rules: XpathRule[];
  onEnabledChange: (enabled: boolean) => void;
  onRulesChange: (rules: XpathRule[]) => void;
}

type ValueField = 'fullAccessValues' | 'titleOnlyValues' | 'hiddenValues';

export function XpathRuleEditor({ enabled, rules, onEnabledChange, onRulesChange }: Props) {
  const [sampleAipId, setSampleAipId] = useState('');
  const [detectedIds, setDetectedIds] = useState<string[]>([]);
  const [detecting, setDetecting] = useState(false);
  const [detectError, setDetectError] = useState<string | null>(null);

  const [testAipIds, setTestAipIds] = useState<Record<number, string>>({});
  const [testResults, setTestResults] = useState<Record<number, string | null>>({});
  const [testVisibilities, setTestVisibilities] = useState<Record<number, string>>({});
  const [testErrors, setTestErrors] = useState<Record<number, string | null>>({});
  const [testingIndex, setTestingIndex] = useState<number | null>(null);

  function updateRules(newRules: XpathRule[]) { onRulesChange([...newRules]); }

  function addRule() {
    updateRules([...rules, { metadataId: detectedIds[0] || '', xpath: '', fullAccessValues: [], titleOnlyValues: [], hiddenValues: [] }]);
  }

  function removeRule(index: number) { updateRules(rules.filter((_, i) => i !== index)); }

  function updateRule(index: number, field: string, value: unknown) {
    updateRules(rules.map((r, i) => i === index ? { ...r, [field]: value } : r));
  }

  function addValue(ri: number, field: ValueField, value: string) {
    const v = value.trim();
    if (!v || rules[ri][field].includes(v)) return;
    updateRule(ri, field, [...rules[ri][field], v]);
  }

  function removeValue(ri: number, field: ValueField, vi: number) {
    updateRule(ri, field, rules[ri][field].filter((_, i) => i !== vi));
  }

  async function detectMetadata() {
    if (!sampleAipId.trim()) return;
    setDetecting(true); setDetectedIds([]); setDetectError(null);
    try {
      const res = await fetch(`/api/v2/aips/${sampleAipId.trim()}/metadata/descriptive/information`, { credentials: 'include' });
      if (!res.ok) throw new Error(`${res.status}`);
      const data = await res.json();
      const ids = (data?.descriptiveMetadataInfoList || []).map((d: { id?: string }) => d.id).filter(Boolean) as string[];
      setDetectedIds(ids);
      if (ids.length === 0) setDetectError('Ingen metadata hittades.');
    } catch (err) { setDetectError(`Kunde inte hämta: ${err instanceof Error ? err.message : 'Okänt fel'}`); }
    finally { setDetecting(false); }
  }

  async function testRule(index: number) {
    const rule = rules[index]; const aipId = testAipIds[index]?.trim();
    if (!aipId || !rule.metadataId || !rule.xpath) return;
    setTestingIndex(index); setTestErrors(p => ({ ...p, [index]: null }));
    try {
      const xml = await apiGetXml(`/api/v2/aips/${aipId}/metadata/descriptive/${rule.metadataId}/download`);
      const value = evaluateXpath(xml, rule.xpath);
      setTestResults(p => ({ ...p, [index]: value }));
      const vis = resolveVis(value, rule);
      setTestVisibilities(p => ({ ...p, [index]: vis }));
    } catch (err) { setTestErrors(p => ({ ...p, [index]: `Fel: ${err instanceof Error ? err.message : 'Okänt'}` })); }
    finally { setTestingIndex(null); }
  }

  function resolveVis(value: string | null, rule: XpathRule): string {
    if (!value) return 'full';
    const n = value.trim().toLowerCase();
    if (rule.hiddenValues?.some(v => v.trim().toLowerCase() === n)) return 'hidden';
    if (rule.titleOnlyValues?.some(v => v.trim().toLowerCase() === n)) return 'title-only';
    return 'full';
  }

  return (
    <fieldset className="xpath-editor">
      <legend>XPath-baserade synlighetsregler</legend>

      <PortalCheckbox
        label="Aktivera XPath-baserad filtrering"
        checked={enabled}
        onChange={(checked) => onEnabledChange(checked)}
      />

      {enabled && (
        <>
          {/* Detect */}
          <div className="xpath-detect">
            <h4>Detektera metadata från ett AIP</h4>
            <p className="xpath-help">Ange ett AIP-ID för att hämta tillgängliga metadata-standarder.</p>
            <div className="xpath-row">
              <label htmlFor="xpath-sample-aip" className="xpath-label">AIP-ID (sparas ej)</label>
              <input id="xpath-sample-aip" type="text" className="xpath-input" value={sampleAipId} onChange={e => setSampleAipId(e.target.value)} placeholder="T.ex. ett känt AIP-ID" />
              <PortalButton variant="secondary" onClick={detectMetadata} disabled={!sampleAipId.trim() || detecting} loading={detecting}>
                Detektera
              </PortalButton>
            </div>
            {detectedIds.length > 0 && (
              <PortalAlert variant="success" size="small">
                Hittade: <strong>{detectedIds.join(', ')}</strong>
              </PortalAlert>
            )}
            {detectError && (
              <PortalAlert variant="danger" size="small">
                {detectError}
              </PortalAlert>
            )}
          </div>

          <hr className="xpath-hr" />

          {/* Rules */}
          {rules.map((rule, i) => (
            <div key={i} className="xpath-rule">
              <div className="xpath-rule-head">
                <h4>Regel {i + 1}: {rule.metadataId || '(ej angiven)'}</h4>
                <button type="button" className="xpath-remove" onClick={() => removeRule(i)} aria-label="Ta bort regel">×</button>
              </div>

              <label className="xpath-label">Metadata-ID</label>
              {detectedIds.length > 0 ? (
                <select className="xpath-input" value={rule.metadataId} onChange={e => updateRule(i, 'metadataId', e.target.value)}>
                  <option value="">Välj...</option>
                  {detectedIds.map(id => <option key={id} value={id}>{id}</option>)}
                </select>
              ) : (
                <input type="text" className="xpath-input" value={rule.metadataId} onChange={e => updateRule(i, 'metadataId', e.target.value)} placeholder="T.ex. ead_2002" />
              )}

              <label className="xpath-label">XPath-uttryck</label>
              <input type="text" className="xpath-input" value={rule.xpath} onChange={e => updateRule(i, 'xpath', e.target.value)} placeholder="T.ex. //ead:accessrestrict/ead:p" />

              <ChipList label="Visa allt (filer + metadata)" cls="chip-full" values={rule.fullAccessValues} onAdd={v => addValue(i, 'fullAccessValues', v)} onRemove={vi => removeValue(i, 'fullAccessValues', vi)} />
              <ChipList label="Visa enbart titel" cls="chip-title-only" values={rule.titleOnlyValues} onAdd={v => addValue(i, 'titleOnlyValues', v)} onRemove={vi => removeValue(i, 'titleOnlyValues', vi)} />
              <ChipList label="Dölj helt" cls="chip-hidden" values={rule.hiddenValues} onAdd={v => addValue(i, 'hiddenValues', v)} onRemove={vi => removeValue(i, 'hiddenValues', vi)} />

              {/* Test */}
              <div className="xpath-test">
                <label className="xpath-label">Testa mot AIP-ID (sparas ej)</label>
                <div className="xpath-row">
                  <input type="text" className="xpath-input" value={testAipIds[i] || ''} onChange={e => setTestAipIds(p => ({ ...p, [i]: e.target.value }))} placeholder="AIP-ID" />
                  <button type="button" className="xpath-btn xpath-btn--primary" onClick={() => testRule(i)} disabled={!testAipIds[i]?.trim() || !rule.metadataId || !rule.xpath || testingIndex === i}>
                    {testingIndex === i ? 'Testar...' : 'Testa'}
                  </button>
                </div>
                {testResults[i] !== undefined && testVisibilities[i] && (
                  <div className={`xpath-test-result result-${testVisibilities[i]}`} role="status">
                    <p>XPath-värde: <code>{testResults[i] || '(tomt)'}</code></p>
                    <p>Synlighet: <strong>{testVisibilities[i] === 'full' ? 'Visa allt' : testVisibilities[i] === 'title-only' ? 'Enbart titel' : 'Dölj helt'}</strong></p>
                  </div>
                )}
                {testErrors[i] && <p className="xpath-err">{testErrors[i]}</p>}
              </div>
            </div>
          ))}

          <PortalButton variant="secondary" onClick={addRule}>+ Lägg till regel</PortalButton>
        </>
      )}
    </fieldset>
  );
}

function ChipList({ label, cls, values, onAdd, onRemove }: { label: string; cls: string; values: string[]; onAdd: (v: string) => void; onRemove: (i: number) => void }) {
  const ref = useRef<HTMLInputElement>(null);
  function handleAdd() { const v = ref.current?.value.trim(); if (v) { onAdd(v); if (ref.current) ref.current.value = ''; } }
  return (
    <div className="xpath-value-group">
      <span className={`xpath-value-label ${cls}`}>{label}</span>
      <div className="xpath-chip-list">
        {values.map((v, i) => (
          <span key={i} className={`xpath-chip ${cls}`}>
            {v}
            <button type="button" className="xpath-chip-remove" onClick={() => onRemove(i)} aria-label={`Ta bort ${v}`}>×</button>
          </span>
        ))}
        <div className="xpath-chip-add">
          <input ref={ref} className="xpath-chip-input" placeholder="Nytt värde..." onKeyDown={e => { if (e.key === 'Enter') { e.preventDefault(); handleAdd(); } }} aria-label={`Lägg till värde för ${label}`} />
          <button type="button" className="xpath-chip-add-btn" onClick={handleAdd} aria-label="Lägg till">+</button>
        </div>
      </div>
    </div>
  );
}

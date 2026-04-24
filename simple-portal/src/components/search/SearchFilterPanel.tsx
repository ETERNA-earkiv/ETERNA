import { useEffect, useState } from 'react';
import { DEFAULT_ADVANCED_SEARCH_FIELDS, type AdvancedSearchFieldConfig } from '@lib/config/search-fields';
import type { FilterParameter } from '@lib/types/api';
import { getLevelLabel } from '@lib/utils/i18n';
import { PortalInput } from '../portal-ui/PortalInput';
import { PortalSelect } from '../portal-ui/PortalSelect';
import { PortalButton } from '../portal-ui/PortalButton';
import { PortalAlert } from '../portal-ui/PortalAlert';
import { loadConfig } from '@lib/api/config';
import './SearchFilterPanel.css';

type FieldConfig = AdvancedSearchFieldConfig;

interface Props {
  onFiltersChange: (filters: FilterParameter[]) => void;
}

export function SearchFilterPanel({ onFiltersChange }: Props) {
  const [fields, setFields] = useState<FieldConfig[]>([]);
  const [values, setValues] = useState<Record<string, string>>({});
  const [dateFrom, setDateFrom] = useState<Record<string, string>>({});
  const [dateTo, setDateTo] = useState<Record<string, string>>({});
  const [selectOptions, setSelectOptions] = useState<Record<string, string[]>>({});
  const [loaded, setLoaded] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);

  useEffect(() => {
    Promise.all([
      loadConfig().catch(() => null),
      fetch('/api/v2/configurations/shared-properties', { credentials: 'include' }).then((r) => r.ok ? r.json() : null).catch(() => null),
    ]).then(([configData, propsData]) => {
      const configured = (configData?.searchConfig?.advancedFields || []) as FieldConfig[];
      const source = configured.length > 0 ? configured : DEFAULT_ADVANCED_SEARCH_FIELDS;
      setFields(source.filter((field) => field.enabled));

      if (!propsData) {
        setLoadError('Kunde inte hämta filteralternativ från arkivet. Fritextfilter fungerar, men flervalsfält kan sakna alternativ.');
      }

      const props = propsData?.properties || propsData || {};
      const opts: Record<string, string[]> = {};
      for (const field of source) {
        if (field.type === 'select') {
          const valuesKey = `ui.search.fields.IndexedAIP.${field.fieldName}.values`;
          const rawValues: string[] = props[valuesKey] || [];
          opts[field.fieldName] = rawValues.filter((v) => v !== 'noneselected');
        }
      }
      setSelectOptions(opts);
    }).finally(() => setLoaded(true));
  }, []);

  function buildFilters(): FilterParameter[] {
    const filters: FilterParameter[] = [];

    for (const field of fields) {
      if (field.type === 'date-range') {
        const from = dateFrom[field.fieldName];
        const to = dateTo[field.fieldName];
        if (from || to) {
          filters.push({
            type: 'DateRangeFilterParameter',
            name: field.fieldName,
            fromValue: from || undefined,
            toValue: to || undefined,
          } as FilterParameter);
        }
      } else if (field.type === 'select') {
        const val = values[field.fieldName];
        if (val?.trim()) {
          filters.push({
            type: 'SimpleFilterParameter',
            name: field.fieldName,
            value: val.trim(),
          } as FilterParameter);
        }
      } else {
        const val = values[field.fieldName];
        if (val?.trim()) {
          filters.push({
            type: 'BasicSearchFilterParameter',
            name: field.fieldName,
            value: val.trim(),
          } as FilterParameter);
        }
      }
    }

    return filters;
  }

  function handleApply() {
    onFiltersChange(buildFilters());
  }

  function handleClear() {
    setValues({});
    setDateFrom({});
    setDateTo({});
    onFiltersChange([]);
  }

  if (!loaded || fields.length === 0) return null;

  return (
    <div className="filter-panel" role="region" aria-label="Avancerade sökfilter">
      {loadError && (
        <PortalAlert variant="warning" size="small">{loadError}</PortalAlert>
      )}
      <div className="filter-panel__fields">
        {fields.map((field) => (
          <div key={field.fieldName} className="filter-panel__field">
            {field.type === 'date-range' ? (
              <div className="filter-panel__date-range">
                <PortalInput
                  label={`${field.label} från`}
                  type="date"
                  value={dateFrom[field.fieldName] || ''}
                  onChange={(val) => setDateFrom((prev) => ({ ...prev, [field.fieldName]: val }))}
                  size="small"
                />
                <span className="filter-panel__date-sep">–</span>
                <PortalInput
                  label={`${field.label} till`}
                  type="date"
                  value={dateTo[field.fieldName] || ''}
                  onChange={(val) => setDateTo((prev) => ({ ...prev, [field.fieldName]: val }))}
                  size="small"
                />
              </div>
            ) : field.type === 'select' && selectOptions[field.fieldName] ? (
              <PortalSelect
                label={field.label}
                value={values[field.fieldName] || ''}
                onChange={(val) => setValues((prev) => ({ ...prev, [field.fieldName]: val }))}
                options={[
                  { value: '', label: 'Alla' },
                  ...selectOptions[field.fieldName].map((opt) => ({
                    value: opt,
                    label: getLevelLabel(opt),
                  })),
                ]}
              />
            ) : (
              <PortalInput
                label={field.label}
                type="text"
                value={values[field.fieldName] || ''}
                onChange={(val) => setValues((prev) => ({ ...prev, [field.fieldName]: val }))}
                placeholder={`Filtrera på ${field.label.toLowerCase()}...`}
              />
            )}
          </div>
        ))}
      </div>
      <div className="filter-panel__actions">
        <PortalButton variant="primary" onClick={handleApply}>Tillämpa filter</PortalButton>
        <PortalButton variant="secondary" onClick={handleClear}>Rensa filter</PortalButton>
      </div>
    </div>
  );
}

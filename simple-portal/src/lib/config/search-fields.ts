export interface AdvancedSearchFieldConfig {
  fieldName: string;
  label: string;
  type: 'select' | 'text' | 'date-range';
  enabled: boolean;
  /** Fältet finns inte i Solr-indexet (t.ex. EAD-metadata som origination) —
   * filtreras client-side genom att läsa varje AIP:s metadata-XML istället
   * för att skickas som serverfilter. */
  clientFilter?: boolean;
}

export const DEFAULT_ADVANCED_SEARCH_FIELDS: AdvancedSearchFieldConfig[] = [
  { fieldName: 'level', label: 'Beskrivningsniva', type: 'select', enabled: true },
  { fieldName: 'type', label: 'Typ', type: 'select', enabled: true },
  { fieldName: 'dates', label: 'Datum', type: 'date-range', enabled: false, clientFilter: true },
  { fieldName: 'origination', label: 'Arkivbildare', type: 'text', enabled: true, clientFilter: true },
];

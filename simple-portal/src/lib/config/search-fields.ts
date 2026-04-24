export interface AdvancedSearchFieldConfig {
  fieldName: string;
  label: string;
  type: 'select' | 'text' | 'date-range';
  enabled: boolean;
}

export const DEFAULT_ADVANCED_SEARCH_FIELDS: AdvancedSearchFieldConfig[] = [
  { fieldName: 'level', label: 'Beskrivningsniva', type: 'select', enabled: true },
  { fieldName: 'type', label: 'Typ', type: 'select', enabled: true },
  { fieldName: 'createdOn', label: 'Skapad', type: 'date-range', enabled: false },
];

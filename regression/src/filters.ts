export type FilterParameter = { type: string } & Record<string, unknown>;

/** En tom parameterlista ger tomt resultat i ETERNA, så "allt" måste anges explicit. */
export const matchAll = (): FilterParameter => ({ type: 'AllFilterParameter' });

export const equals = (name: string, value: string): FilterParameter => ({
  type: 'SimpleFilterParameter',
  name,
  value,
});

export const freeText = (value: string): FilterParameter => ({
  type: 'BasicSearchFilterParameter',
  name: 'search',
  value,
});

import { describe, it, expect } from 'vitest';
import { normalizeStartDate, normalizeEndDate, dateRangesOverlap } from './date-range';

describe('normalizeStartDate', () => {
  it('fyller ut år och månad nedåt', () => {
    expect(normalizeStartDate('1888')).toBe('1888-01-01');
    expect(normalizeStartDate('1888-03')).toBe('1888-03-01');
    expect(normalizeStartDate('1888-03-15')).toBe('1888-03-15');
  });

  it('returnerar null för ogiltigt värde', () => {
    expect(normalizeStartDate('')).toBeNull();
    expect(normalizeStartDate('okänt')).toBeNull();
  });
});

describe('normalizeEndDate', () => {
  it('fyller ut år och månad uppåt till sista dagen', () => {
    expect(normalizeEndDate('1900')).toBe('1900-12-31');
    expect(normalizeEndDate('1900-02')).toBe('1900-02-28');
    expect(normalizeEndDate('1904-02')).toBe('1904-02-29');
    expect(normalizeEndDate('1900-01-05')).toBe('1900-01-05');
  });

  it('returnerar null för ogiltigt värde', () => {
    expect(normalizeEndDate('')).toBeNull();
  });
});

describe('dateRangesOverlap', () => {
  it('matchar intervall inom filtret', () => {
    expect(dateRangesOverlap('1895-01-01', '1897-12-31', '1888-01-01', '1900-01-01')).toBe(true);
  });

  it('matchar intervall som bara delvis överlappar filtret', () => {
    expect(dateRangesOverlap('1899-06-01', '1905-01-01', '1888-01-01', '1900-01-01')).toBe(true);
    expect(dateRangesOverlap('1880-01-01', '1888-01-01', '1888-01-01', '1900-01-01')).toBe(true);
  });

  it('avvisar intervall helt utanför filtret — buggen med 1940-träffar', () => {
    expect(dateRangesOverlap('1940-05-01', '1940-05-01', '1888-01-01', '1900-01-01')).toBe(false);
    expect(dateRangesOverlap('1850-01-01', '1860-01-01', '1888-01-01', '1900-01-01')).toBe(false);
  });

  it('behandlar enkelt datum (bara Startdatum) som en punkt', () => {
    expect(dateRangesOverlap('1890-05-01', '', '1888-01-01', '1900-01-01')).toBe(true);
    expect(dateRangesOverlap('1940-05-01', '', '1888-01-01', '1900-01-01')).toBe(false);
  });

  it('hanterar partiella arkivdatum', () => {
    expect(dateRangesOverlap('1888', '1900', '1888-01-01', '1900-01-01')).toBe(true);
    expect(dateRangesOverlap('1900', '', '1888-01-01', '1900-01-01')).toBe(true);
    expect(dateRangesOverlap('1901', '', '1888-01-01', '1900-01-01')).toBe(false);
  });

  it('tillåter öppna filtergränser', () => {
    expect(dateRangesOverlap('1940-05-01', '', '1900-01-01', '')).toBe(true);
    expect(dateRangesOverlap('1850-01-01', '1860-01-01', '1900-01-01', '')).toBe(false);
    expect(dateRangesOverlap('1850-01-01', '', '', '1900-01-01')).toBe(true);
  });

  it('post utan datum matchar aldrig ett aktivt datumfilter', () => {
    expect(dateRangesOverlap('', '', '1888-01-01', '1900-01-01')).toBe(false);
  });
});

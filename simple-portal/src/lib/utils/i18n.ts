/**
 * i18n — Svenska labels för RODA/ETERNA-värden.
 *
 * Strategi:
 * 1. Portalen har inbyggda svenska översättningar (nedan)
 * 2. Vid start hämtas i18n-nycklar från ETERNA:s shared-properties
 * 3. ETERNA-värden skriver ÖVER inbyggda — så om instansen har
 *    svenska ServerMessages konfigurerade används de automatiskt
 */

// ── Inbyggda svenska översättningar ──

const builtinLabels: Record<string, string> = {
  // Beskrivningsnivåer (i18n.level.*)
  // Beskrivningsnivåer — både EAD-ID:n och engelska namn som ETERNA kan returnera
  'fonds': 'Arkivbestånd',
  'subfonds': 'Delarkiv',
  'collection': 'Samling',
  'series': 'Serie',
  'subseries': 'Underserie',
  'sub-series': 'Underserie',
  'file': 'Volym',
  'item': 'Handling',
  'class': 'Klass',
  'subclass': 'Underklass',
  'recordgrp': 'Förvaringsgrupp',
  'record group': 'Förvaringsgrupp',
  'subgrp': 'Undergrupp',
  'subgroup': 'Undergrupp',
  'sub-group': 'Undergrupp',
  'otherlevel': 'Annan nivå',
  'other': 'Övrigt',
  'noneselected': 'Inget valt',

  // Sökfält (i18n.ui.search.fields.IndexedAIP.*)
  'Original reference': 'Originalreferens',
  'Title': 'Titel',
  'Description': 'Beskrivning',
  'Scope and content': 'Omfattning och innehåll',
  'Origination': 'Ursprung',
  'Ingest SIP identifier': 'Inleverans SIP-identifierare',
  'Date': 'Datum',
  'Level': 'Beskrivningsnivå',
  'Type': 'Typ',
  'Identifier': 'Identifierare',
  'With files': 'Med filer',

  // Facetter
  'Description levels': 'Beskrivningsnivåer',
  'Representations': 'Representationer',
  'with files': 'med filer',
  'without files': 'utan filer',
  'Fonds': 'Arkivbestånd',
  'Series': 'Serie',
  'File': 'Volym',
  'Item': 'Handling',

  // Status
  'ACTIVE': 'Aktiv',
  'INACTIVE': 'Inaktiv',
  'UNDER_APPRAISAL': 'Under värdering',
  'DESTROYED': 'Förstörd',
};

// Lokala fallback-översättningar för metadatafält när ServerMessages saknar nyckel.
const metadataFallbackLabels: Record<string, string> = {
  'agencyname': 'Myndighetsnamn',
  'agency name': 'Myndighetsnamn',
  'recordid': 'Post-ID',
  'record id': 'Post-ID',
  'maintenanceagency': 'Ansvarig myndighet',
  'maintenance agency': 'Ansvarig myndighet',
  'maintenanceevent': 'Ändringshändelse',
  'maintenance event': 'Ändringshändelse',
  'eventdatetime': 'Ändringstid',
  'event datetime': 'Ändringstid',
  'eventtype': 'Ändringstyp',
  'event type': 'Ändringstyp',
  'agenttype': 'Aktörstyp',
  'agent type': 'Aktörstyp',
  'agent': 'Aktör',
};

// Dynamisk overlay — fylls på från ETERNA:s shared-properties
let remoteLabels: Record<string, string> = {};
let remoteLoaded = false;

function lookupLocalLabel(label: string): string | undefined {
  const clean = label.trim();
  const lower = clean.toLowerCase();

  return (
    builtinLabels[clean] ||
    builtinLabels[lower] ||
    metadataFallbackLabels[clean] ||
    metadataFallbackLabels[lower]
  );
}

/**
 * Hämta i18n-nycklar från ETERNA och berika/skriv över inbyggda labels.
 * Anropas en gång vid appstart.
 */
export async function loadRemoteLabels(): Promise<void> {
  if (remoteLoaded) return;
  try {
    const res = await fetch('/api/v2/configurations/shared-properties', {
      credentials: 'include',
    });
    if (!res.ok) return;
    const data = await res.json();
    const props: Record<string, string[]> = data.properties || data;

    // Parsa alla i18n-nycklar
    for (const [key, values] of Object.entries(props)) {
      if (!key.startsWith('i18n.')) continue;
      const label = Array.isArray(values) ? values[0] : String(values);
      if (!label) continue;

      // i18n.level.fonds → 'fonds' som nyckel
      if (key.startsWith('i18n.level.')) {
        const levelKey = key.slice('i18n.level.'.length);
        remoteLabels[levelKey] = label;
      }

      // i18n.ui.search.fields.IndexedAIP.title → engelska labeln som nyckel
      if (key.startsWith('i18n.ui.search.fields.IndexedAIP.')) {
        remoteLabels[label] = label; // Om ETERNA har svenska här, används den
      }

      // i18n.ui.facets.IndexedAIP.level.fonds → 'Fonds' som nyckel
      if (key.startsWith('i18n.ui.facets.IndexedAIP.level.')) {
        const levelValue = key.slice('i18n.ui.facets.IndexedAIP.level.'.length);
        remoteLabels[levelValue] = label;
        // Också med stor bokstav
        remoteLabels[levelValue.charAt(0).toUpperCase() + levelValue.slice(1)] = label;
      }
    }

    remoteLoaded = true;
  } catch {
    // Ignorera — använd inbyggda
  }
}

/**
 * Översätt en label. Kollar först remote (ETERNA), sedan inbyggd.
 */
export function translateLabel(label: string): string {
  const clean = label.trim();
  return (
    remoteLabels[clean] ||
    remoteLabels[clean.toLowerCase()] ||
    lookupLocalLabel(clean) ||
    label
  );
}

/**
 * Hämta svensk label för en beskrivningsnivå.
 */
export function getLevelLabel(level: string): string {
  const key = level?.toLowerCase();
  // Svenska inbyggda labels har prioritet över engelska remote-labels.
  // Remote overridar bara om ETERNA faktiskt har svenska konfigurerat.
  return builtinLabels[key] || remoteLabels[key] || level || 'Okänd';
}

export function getStateLabel(state: string): string {
  return remoteLabels[state] || builtinLabels[state] || state || 'Okänd';
}

/**
 * Normalize metadata standard IDs from RODA/ETERNA into a canonical portal key.
 * Handles variants like ead_2002, ead2002, ead-2002.xml and ead3/ead_3.
 */
export function normalizeMetadataStandardId(metadataId: string): string {
  if (!metadataId) return metadataId;

  const clean = metadataId
    .trim()
    .replace(/\.xml$/i, '')
    .toLowerCase();

  const fuzzy = clean.replace(/[_\-\s]+/g, '');

  if (fuzzy === 'ead2002') return 'ead_2002';
  if (fuzzy === 'ead3') return 'ead_3';
  if (fuzzy === 'dublincore' || fuzzy === 'dc') return 'dc';

  return clean.replace(/[-\s]+/g, '_');
}

export function formatStandardName(metadataId: string): string {
  if (!metadataId) return metadataId;
  const clean = metadataId.replace(/\.xml$/i, '');
  if (/ead.?2002/i.test(clean)) return 'Encoded Archival Description 2002';
  if (/ead.?3/i.test(clean)) return 'Encoded Archival Description 3';
  if (/dc/i.test(clean)) return 'Dublin Core';
  const match = clean.match(/^(\w+?)[-_]?(\d{4})?$/i);
  if (!match) return clean;
  const name = match[1].toUpperCase();
  const year = match[2];
  return year ? `${name} (${year})` : name;
}

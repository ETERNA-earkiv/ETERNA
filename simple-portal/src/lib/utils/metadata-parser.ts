/**
 * Metadata XML parser — porterad exakt från Angular metadata-api.service.ts.
 * Parsar EAD, Dublin Core och andra XML-standarder till label/value-fält.
 */

import type { MetadataField } from '../api/metadata';
import { translateLabel } from './i18n';

const PREFERRED_FIELD_ORDER: string[] = [
  'Titel', 'Filnamn', 'Startdatum', 'Slutdatum', 'Arkivbildare', 'Omfattning och innehåll',
  'Beskrivningsnivå', 'EAD-identifierare', 'Skapad', 'Referenskod', 'Datum', 'Landskod',
  'Förvaringsplats', 'Fysisk beskrivning', 'Sammanfattning',
  'Biografi och historia', 'Förvaringshistorik', 'Förvärvsuppgifter',
  'Gallring', 'Tillägg', 'Ordning och struktur',
  'Villkor för tillträde', 'Villkor för användning', 'Materialets språk', 'Språk',
  'Anmärkning', 'Övrig information', 'Beskrivningsregler', 'Bearbetningsinformation',
  'Kontrollerade termer', 'Ämne', 'Geografiskt namn', 'Person', 'Organisation',
  'Skapare', 'Utgivare', 'Medverkande', 'Typ', 'Format',
  'Identifierare', 'Källa', 'Relation', 'Täckning', 'Rättigheter', 'Beskrivning',
];

const ELEMENT_LABELS: Record<string, string> = {
  'unitid': 'Referenskod',
  'unittitle': 'Titel',
  'unitdate': 'Datum',
  'physdesc': 'Fysisk beskrivning',
  'origination': 'Arkivbildare',
  'repository': 'Förvaringsplats',
  'abstract': 'Sammanfattning',
  'langmaterial': 'Materialets språk',
  'language': 'Språk',
  'accessrestrict': 'Villkor för tillträde',
  'userestrict': 'Villkor för användning',
  'scopecontent': 'Omfattning och innehåll',
  'bioghist': 'Biografi och historia',
  'arrangement': 'Ordning och struktur',
  'acqinfo': 'Förvärvsuppgifter',
  'custodhist': 'Förvaringshistorik',
  'appraisal': 'Gallring',
  'accruals': 'Tillägg',
  'processinfo': 'Bearbetningsinformation',
  'descrules': 'Beskrivningsregler',
  'note': 'Anmärkning',
  'odd': 'Övrig information',
  'controlaccess': 'Kontrollerade termer',
  'subject': 'Ämne',
  'geogname': 'Geografiskt namn',
  'persname': 'Person',
  'corpname': 'Organisation',
  'eadid': 'EAD-identifierare',
  'titleproper': 'Filnamn',
  'creation': 'Skapad',
  'title': 'Titel',
  'creator': 'Skapare',
  'publisher': 'Utgivare',
  'contributor': 'Medverkande',
  'date': 'Datum',
  'type': 'Typ',
  'format': 'Format',
  'identifier': 'Identifierare',
  'source': 'Källa',
  'relation': 'Relation',
  'coverage': 'Täckning',
  'rights': 'Rättigheter',
  'description': 'Beskrivning',
};

const EAD_LEVELS: Record<string, string> = {
  'collection': 'Samling',
  'fonds': 'Arkivbestånd',
  'subfonds': 'Delarkiv',
  'series': 'Serie',
  'subseries': 'Underserie',
  'file': 'Akt',
  'item': 'Handling',
  'class': 'Klass',
  'subclass': 'Underklass',
  'recordgrp': 'Handlingsgrupp',
  'subgrp': 'Undergrupp',
  'otherlevel': 'Annan nivå',
};

const SKIP_ELEMENTS = new Set([
  'ead', 'eadheader', 'archdesc', 'filedesc', 'titlestmt',
  'publicationstmt', 'profiledesc', 'revisiondesc', 'did',
  'dsc', 'change', 'langusage', 'simpledc', 'dc', 'metadata',
  'record', 'head', 'c', 'c01', 'c02', 'c03',
  // EAD 3 date structures (extracted separately in extractAttributeFields)
  'unitdatestructured', 'daterange', 'fromdate', 'todate', 'datesingle',
  // EAD 3 physical description
  'physdescstructured', 'quantity', 'unittype', 'dimensions',
]);

// Elements whose text content should inherit the nearest ancestor label
// (they are transparent "wrappers" in EAD's name/date structures)
const INHERIT_ANCESTOR_LABEL = new Set(['p', 'item', 'part', 'corpname', 'persname', 'famname', 'name']);
// When walking ancestors for label, skip these transparent containers
const TRANSPARENT_CONTAINERS = new Set(['corpname', 'persname', 'famname', 'name']);

/**
 * Parse metadata XML into label/value fields.
 * Handles EAD 2002, Dublin Core, and generic XML.
 */
export function parseXmlToFields(xml: string): MetadataField[] {
  if (!xml) return [];

  // Strip namespace declarations and prefixes
  const cleanXml = xml
    .replace(/\s+xmlns(:[a-zA-Z0-9]+)?\s*=\s*"[^"]*"/g, '')
    .replace(/\s+[a-zA-Z0-9]+:[a-zA-Z0-9]+\s*=\s*"[^"]*"/g, '')
    .replace(/<(\/?)([a-zA-Z0-9]+):([a-zA-Z0-9]+)/g, '<$1$3');

  const parser = new DOMParser();
  const doc = parser.parseFromString(cleanXml, 'application/xml');

  if (doc.querySelector('parsererror')) return [];

  const fields: MetadataField[] = [];
  const seen = new Set<string>();

  // Extract attribute-based fields (level, dates, country code)
  extractAttributeFields(doc.documentElement, fields, seen);

  // Walk tree for text content
  walkXmlTree(doc.documentElement, fields, seen);

  // Sort fields to match preferred order
  fields.sort((a, b) => {
    const ai = PREFERRED_FIELD_ORDER.indexOf(a.label);
    const bi = PREFERRED_FIELD_ORDER.indexOf(b.label);
    return (ai >= 0 ? ai : PREFERRED_FIELD_ORDER.length) -
           (bi >= 0 ? bi : PREFERRED_FIELD_ORDER.length);
  });

  return fields;
}

function walkXmlTree(el: Element, fields: MetadataField[], seen: Set<string>): void {
  for (const child of Array.from(el.children)) {
    const tag = child.tagName.toLowerCase();

    if (child.children.length === 0) {
      const text = child.textContent?.trim();
      if (!text) continue;

      let label: string | null = null;
      if (INHERIT_ANCESTOR_LABEL.has(tag)) {
        // Walk ancestors (skipping transparent name containers) to find context label
        let ancestor: Element | null = child.parentElement;
        while (ancestor) {
          const aTag = ancestor.tagName.toLowerCase();
          if (SKIP_ELEMENTS.has(aTag)) break;
          if (!TRANSPARENT_CONTAINERS.has(aTag)) {
            const aLabel = resolveMetadataLabel(aTag);
            if (aLabel) { label = aLabel; break; }
          }
          ancestor = ancestor.parentElement;
        }
        // Fallback: use element's own label (e.g. corpname → Organisation)
        if (!label && !SKIP_ELEMENTS.has(tag)) {
          label = resolveMetadataLabel(tag);
        }
      } else if (!SKIP_ELEMENTS.has(tag)) {
        label = resolveMetadataLabel(tag);
      }

      if (label) {
        const key = `${label}::${text}`;
        if (!seen.has(key)) {
          seen.add(key);
          fields.push({ label, value: text });
        }
      }
    } else {
      walkXmlTree(child, fields, seen);
    }
  }
}

function extractAttributeFields(root: Element, fields: MetadataField[], seen: Set<string>): void {
  // EAD: archdesc/@level
  const archdesc = root.querySelector('archdesc');
  if (archdesc) {
    const level = archdesc.getAttribute('level');
    if (level) {
      const value = EAD_LEVELS[level] || level;
      fields.push({ label: 'Beskrivningsnivå', value });
      seen.add(`Beskrivningsnivå::${value}`);
    }
  }

  // EAD: unitdate/@normal
  const unitdates = root.querySelectorAll('unitdate[normal]');
  unitdates.forEach(ud => {
    const normal = ud.getAttribute('normal') || '';
    if (normal.includes('/')) {
      const [start, end] = normal.split('/');
      if (start && !seen.has(`Startdatum::${start}`)) {
        fields.push({ label: 'Startdatum', value: start });
        seen.add(`Startdatum::${start}`);
      }
      if (end && !seen.has(`Slutdatum::${end}`)) {
        fields.push({ label: 'Slutdatum', value: end });
        seen.add(`Slutdatum::${end}`);
      }
    } else if (normal && !seen.has(`Startdatum::${normal}`)) {
      fields.push({ label: 'Startdatum', value: normal });
      seen.add(`Startdatum::${normal}`);
    }
  });

  // EAD 3: unitdatestructured (daterange)
  root.querySelectorAll('unitdatestructured fromdate').forEach(fd => {
    const date = fd.getAttribute('standarddate') || fd.textContent?.trim() || '';
    if (date && !seen.has(`Startdatum::${date}`)) {
      fields.push({ label: 'Startdatum', value: date });
      seen.add(`Startdatum::${date}`);
    }
  });
  root.querySelectorAll('unitdatestructured todate').forEach(td => {
    const date = td.getAttribute('standarddate') || td.textContent?.trim() || '';
    if (date && !seen.has(`Slutdatum::${date}`)) {
      fields.push({ label: 'Slutdatum', value: date });
      seen.add(`Slutdatum::${date}`);
    }
  });
  // EAD 3: unitdatestructured (datesingle)
  root.querySelectorAll('unitdatestructured datesingle').forEach(ds => {
    const date = ds.getAttribute('standarddate') || ds.textContent?.trim() || '';
    if (date && !seen.has(`Startdatum::${date}`)) {
      fields.push({ label: 'Startdatum', value: date });
      seen.add(`Startdatum::${date}`);
    }
  });

  // EAD: eadid/@countrycode
  const eadid = root.querySelector('eadid');
  if (eadid) {
    const country = eadid.getAttribute('countrycode');
    if (country) {
      fields.push({ label: 'Landskod', value: country });
      seen.add(`Landskod::${country}`);
    }
  }
}

function formatTagName(tag: string): string {
  return tag
    .replace(/[_-]+/g, ' ')
    .replace(/([a-z])([A-Z])/g, '$1 $2')
    .replace(/\s+/g, ' ')
    .trim()
    .replace(/\b\w/g, (match) => match.toUpperCase());
}

function resolveMetadataLabel(tag: string): string | null {
  if (!tag || SKIP_ELEMENTS.has(tag)) return null;

  const staticLabel = ELEMENT_LABELS[tag];
  if (staticLabel) return staticLabel;

  const translatedRaw = translateLabel(tag);
  if (translatedRaw !== tag) return translatedRaw;

  const formatted = formatTagName(tag);
  return translateLabel(formatted);
}

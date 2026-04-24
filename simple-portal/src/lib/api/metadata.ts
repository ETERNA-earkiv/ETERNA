/**
 * Metadata API — HTML/XML-metadata, XPath-evaluering.
 */

import { apiGet, apiGetHtml, apiGetXml } from './client';
import { evaluateXpath } from '../utils/xpath';
import { TtlCache } from '../utils/ttl-cache';
import { formatStandardName } from '../utils/i18n';

const htmlCache = new TtlCache<string>();
const xmlCache = new TtlCache<string>();

export interface MetadataField {
  label: string;
  value: string;
}

export interface MetadataResult {
  id: string;
  displayName: string;
  html: string;
  fields: MetadataField[];
}

export async function getMetadataHtml(
  aipId: string,
  metadataId: string,
): Promise<string> {
  return htmlCache.getOrFetch(`html:${aipId}:${metadataId}`, () =>
    apiGetHtml(`/api/v2/aips/${aipId}/metadata/descriptive/${metadataId}/html`),
  );
}

export async function getMetadataXml(
  aipId: string,
  metadataId: string,
): Promise<string> {
  return xmlCache.getOrFetch(`xml:${aipId}:${metadataId}`, () =>
    apiGetXml(`/api/v2/aips/${aipId}/metadata/descriptive/${metadataId}/download`),
  );
}

export async function getMetadataResult(
  aipId: string,
  metadataId: string,
): Promise<MetadataResult> {
  const html = await getMetadataHtml(aipId, metadataId);
  const fields = parseHtmlToFields(html);

  return {
    id: metadataId,
    displayName: formatStandardName(metadataId),
    html,
    fields,
  };
}

/**
 * Evaluate an XPath expression against a metadata XML document.
 */
export async function evaluateMetadataXpath(
  aipId: string,
  metadataId: string,
  xpath: string,
): Promise<string | null> {
  const xml = await getMetadataXml(aipId, metadataId);
  return evaluateXpath(xml, xpath);
}

/**
 * Get list of available metadata standard IDs for an AIP.
 * Uses /information endpoint (JSON), same as Angular.
 */
export async function getAvailableMetadataIds(
  aipId: string,
): Promise<string[]> {
  try {
    const data = await apiGet<{ descriptiveMetadataInfoList?: { id: string }[] }>(
      `/api/v2/aips/${aipId}/metadata/descriptive/information`,
    );
    const list = data?.descriptiveMetadataInfoList || [];
    return list.map((info) => info.id).filter(Boolean);
  } catch {
    return [];
  }
}

/**
 * Parse HTML metadata content to label/value pairs.
 */
function parseHtmlToFields(html: string): MetadataField[] {
  if (!html) return [];

  try {
    const parser = new DOMParser();
    const doc = parser.parseFromString(html, 'text/html');
    const fields: MetadataField[] = [];

    // Try table rows (common RODA format)
    const rows = doc.querySelectorAll('tr');
    for (const row of rows) {
      const cells = row.querySelectorAll('td, th');
      if (cells.length >= 2) {
        const label = cells[0]?.textContent?.trim() || '';
        const value = cells[1]?.textContent?.trim() || '';
        if (label && value) {
          fields.push({ label, value });
        }
      }
    }

    // Try definition lists (dl/dt/dd)
    if (fields.length === 0) {
      const dts = doc.querySelectorAll('dt');
      for (const dt of dts) {
        const dd = dt.nextElementSibling;
        if (dd?.tagName === 'DD') {
          fields.push({
            label: dt.textContent?.trim() || '',
            value: dd.textContent?.trim() || '',
          });
        }
      }
    }

    return fields;
  } catch {
    return [];
  }
}

export function invalidateMetadataCache(aipId?: string): void {
  if (aipId) {
    htmlCache.invalidateByPrefix(`html:${aipId}`);
    xmlCache.invalidateByPrefix(`xml:${aipId}`);
  } else {
    htmlCache.clear();
    xmlCache.clear();
  }
}

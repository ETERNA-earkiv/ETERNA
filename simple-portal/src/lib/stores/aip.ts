/**
 * AIP detail store — enskild AIP med metadata och representationer.
 */

import { atom, computed } from 'nanostores';
import { findAIPById, getAIPAncestors } from '../api/aip';
import { getRepresentationsForAIP } from '../api/representations';
import { getAvailableMetadataIds, getMetadataResult, type MetadataResult } from '../api/metadata';
import type { IndexedAIP, Representation } from '../types/api';

export const $aip = atom<IndexedAIP | null>(null);
export const $aipLoading = atom(false);
export const $aipError = atom<string | null>(null);
export const $ancestors = atom<IndexedAIP[]>([]);
export const $representations = atom<Representation[]>([]);
export const $metadataIds = atom<string[]>([]);
export const $metadataResults = atom<MetadataResult[]>([]);

export const $hasAip = computed($aip, (a) => a != null);
export const $aipTitle = computed($aip, (a) => a?.title || a?.id || '');

/**
 * Load full AIP details: AIP data, ancestors, representations, metadata.
 */
export async function loadAip(aipId: string): Promise<void> {
  $aipLoading.set(true);
  $aipError.set(null);

  try {
    // Fetch AIP and ancestors in parallel
    const [aip, ancestors] = await Promise.all([
      findAIPById(aipId),
      getAIPAncestors(aipId).catch(() => []),
    ]);

    if (!aip) {
      $aipError.set('Arkivobjektet hittades inte');
      return;
    }

    $aip.set(aip);
    $ancestors.set(ancestors);

    // Fetch representations and metadata in parallel
    const [reps, metaIds] = await Promise.all([
      getRepresentationsForAIP(aipId).catch(() => []),
      getAvailableMetadataIds(aipId).catch(() => []),
    ]);

    $representations.set(reps);
    $metadataIds.set(metaIds);

    // Fetch metadata results for each ID
    if (metaIds.length > 0) {
      const metaResults = await Promise.all(
        metaIds.map((id) => getMetadataResult(aipId, id).catch(() => null)),
      );
      $metadataResults.set(metaResults.filter(Boolean) as MetadataResult[]);
    }
  } catch (err) {
    $aipError.set(err instanceof Error ? err.message : 'Kunde inte hämta arkivobjektet');
  } finally {
    $aipLoading.set(false);
  }
}

export function clearAip(): void {
  $aip.set(null);
  $ancestors.set([]);
  $representations.set([]);
  $metadataIds.set([]);
  $metadataResults.set([]);
  $aipError.set(null);
}

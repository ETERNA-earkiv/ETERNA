/**
 * Representations API
 */

import { apiPost } from './client';
import { getFilesForRepresentation, buildFileTree } from './files';
import { TtlCache } from '../utils/ttl-cache';
import type {
  FindRequest,
  Representation,
  SimpleFilterParameter,
  FileNode,
  Job,
} from '../types/api';

const repsCache = new TtlCache<Representation[]>();

type RepIndexResult = {
  offset: number;
  limit: number;
  totalCount: number;
  results: Array<Representation | { representation: Representation }>;
};

export interface RepresentationWithFiles extends Representation {
  fileTree: FileNode[];
  totalFiles: number;
}

export async function getRepresentationsForAIP(
  aipId: string,
): Promise<Representation[]> {
  return repsCache.getOrFetch(`reps:${aipId}`, async () => {
    const request: FindRequest = {
      filter: {
        parameters: [{
          type: 'SimpleFilterParameter',
          name: 'aipId',
          value: aipId,
        } as SimpleFilterParameter],
      },
      onlyActive: true,
      sublist: { firstElementIndex: 0, maximumElementCount: 100 },
    };

    const result = await apiPost<RepIndexResult>('/api/v2/representations/find', request);
    return (result?.results || []).map((item) =>
      'representation' in item ? item.representation : item,
    );
  });
}

export async function getRepresentationWithFiles(
  aipId: string,
  repId: string,
): Promise<RepresentationWithFiles> {
  const reps = await getRepresentationsForAIP(aipId);
  const rep = reps.find((r) => r.uuid === repId || r.id === repId);
  if (!rep) throw new Error(`Representation ${repId} not found`);

  const files = await getFilesForRepresentation(rep.uuid);
  const fileTree = buildFileTree(files);

  return {
    ...rep,
    fileTree,
    totalFiles: files.filter((f) => !f.isDirectory).length,
  };
}

export async function createRepresentation(
  aipId: string,
  type: string,
): Promise<Representation> {
  return apiPost<Representation>(`/api/v2/representations/${aipId}`, { type });
}

export async function deleteRepresentations(
  ids: string[],
  details: string,
): Promise<Job> {
  return apiPost<Job>('/api/v2/representations/delete', {
    selectedItems: { ids },
    details,
  });
}

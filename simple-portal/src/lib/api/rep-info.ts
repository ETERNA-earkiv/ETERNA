/**
 * Representation Information API — formatregister.
 */

import { apiPost, apiGet } from './client';
import type { FindRequest } from '../types/api';

export interface RepresentationInfo {
  id: string;
  name: string;
  description: string;
  family: string;
  categories: string[];
  support: string;
  extensions: string[];
  mimetypes: string[];
  pronoms: string[];
}

type RepInfoIndexResult = {
  offset: number;
  limit: number;
  totalCount: number;
  results: RepresentationInfo[];
};

export async function searchRepInfo(
  query = '',
  offset = 0,
  limit = 50,
): Promise<RepInfoIndexResult> {
  const parameters = query
    ? [{ type: 'BasicSearchFilterParameter' as const, name: 'search', value: query }]
    : [{ type: 'AllFilterParameter' as const }];

  const request: FindRequest = {
    filter: {
      parameters: parameters as FindRequest['filter']['parameters'],
    },
    onlyActive: true,
    sublist: { firstElementIndex: offset, maximumElementCount: limit },
  };

  return apiPost<RepInfoIndexResult>('/api/v2/representations-information/find', request);
}

export async function getRepInfoById(id: string): Promise<RepresentationInfo> {
  return apiGet<RepresentationInfo>(`/api/v2/representations-information/${id}`);
}

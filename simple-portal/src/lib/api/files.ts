/**
 * Files API — filer, filträd, upload, download.
 */

import { apiPost, apiPostMultipart } from './client';
import { TtlCache } from '../utils/ttl-cache';
import type {
  FindRequest,
  IndexedFile,
  FileNode,
  SimpleFilterParameter,
  Job,
} from '../types/api';

const filesCache = new TtlCache<IndexedFile[]>();

type FileIndexResult = {
  offset: number;
  limit: number;
  totalCount: number;
  results: IndexedFile[];
};

export async function getFilesForRepresentation(
  representationUUID: string,
): Promise<IndexedFile[]> {
  return filesCache.getOrFetch(`files:${representationUUID}`, async () => {
    const request: FindRequest = {
      filter: {
        parameters: [{
          type: 'SimpleFilterParameter',
          name: 'representationUUID',
          value: representationUUID,
        } as SimpleFilterParameter],
      },
      onlyActive: true,
      sublist: { firstElementIndex: 0, maximumElementCount: 1000 },
    };

    const result = await apiPost<FileIndexResult>('/api/v2/files/find', request);
    return result?.results || [];
  });
}

export function getFileDownloadUrl(fileId: string): string {
  return `/api/v2/files/${fileId}/preview`;
}

export function getThumbnailUrl(fileId: string, _filename: string): string {
  // Use ETERNA preview directly — portal-backend thumbnails not available
  return `/api/v2/files/${fileId}/preview`;
}

/**
 * Build a hierarchical file tree from a flat list of IndexedFile entries.
 */
export function buildFileTree(files: IndexedFile[]): FileNode[] {
  const nodeMap = new Map<string, FileNode>();

  for (const file of files) {
    const name = file.id?.split('/').pop() || file.id;
    const node: FileNode = {
      id: file.uuid,
      name,
      path: file.path || [],
      isDirectory: file.isDirectory || false,
      size: file.size,
      mimetype: file.fileFormat?.mimeType,
      extension: file.fileFormat?.extension,
      pronom: file.fileFormat?.pronom,
      createdOn: file.createdOn,
      children: [],
    };
    nodeMap.set(file.uuid, node);
  }

  const roots: FileNode[] = [];

  for (const file of files) {
    const node = nodeMap.get(file.uuid)!;
    if (file.parentUUID && nodeMap.has(file.parentUUID)) {
      nodeMap.get(file.parentUUID)!.children!.push(node);
    } else {
      roots.push(node);
    }
  }

  const sortNodes = (nodes: FileNode[]): FileNode[] => {
    return nodes.sort((a, b) => {
      if (a.isDirectory && !b.isDirectory) return -1;
      if (!a.isDirectory && b.isDirectory) return 1;
      return a.name.localeCompare(b.name, 'sv');
    }).map((n) => ({ ...n, children: n.children ? sortNodes(n.children) : [] }));
  };

  return sortNodes(roots);
}

export async function uploadFile(
  aipId: string,
  representationId: string,
  file: File,
  path: string[],
  onProgress?: (percent: number) => void,
): Promise<Response> {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('aipId', aipId);
  formData.append('representationId', representationId);
  if (path.length > 0) {
    formData.append('path', path.join('/'));
  }

  return apiPostMultipart('/api/v2/files/upload', formData, onProgress);
}

export async function createFolder(
  aipId: string,
  representationId: string,
  folderName: string,
  parentPath: string[],
): Promise<void> {
  await apiPost('/api/v2/files/create/folder', {
    aipId,
    representationId,
    folderName,
    path: parentPath.join('/'),
  });
}

export async function deleteFiles(ids: string[], details: string): Promise<Job> {
  return apiPost<Job>('/api/v2/files/delete', {
    selectedItems: { ids },
    details,
  });
}

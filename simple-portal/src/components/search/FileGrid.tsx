import { lazy, Suspense, useState, useCallback } from 'react';
import type { FileNode } from '@lib/types/api';
import { getFileDownloadUrl, getThumbnailUrl } from '@lib/api/files';
import { formatFileSize } from '@lib/utils/file-size';
import { getFileExtension, isPreviewable } from '@lib/utils/file-utils';
import './FileGrid.css';

const FileViewer = lazy(() => import('./FileViewer'));

interface Props {
  files: FileNode[];
}

export function FileGrid({ files }: Props) {
  const [failedImages, setFailedImages] = useState<Set<string>>(new Set());
  const [loadedImages, setLoadedImages] = useState<Set<string>>(new Set());
  const [viewerFile, setViewerFile] = useState<FileNode | null>(null);
  const [viewerIndex, setViewerIndex] = useState(0);

  const handleImageLoad = useCallback((id: string) => {
    setLoadedImages((prev) => new Set(prev).add(id));
  }, []);

  const handleImageError = useCallback((id: string) => {
    setFailedImages((prev) => new Set(prev).add(id));
  }, []);

  function canPreview(node: FileNode): boolean {
    return isPreviewable(node.mimetype || '');
  }

  function downloadFile(node: FileNode) {
    window.open(getFileDownloadUrl(node.id), '_blank');
  }

  function previewFile(node: FileNode) {
    const previewable = files.filter(f => !f.isDirectory);
    const idx = previewable.findIndex(f => f.id === node.id);
    setViewerFile(node);
    setViewerIndex(idx >= 0 ? idx : 0);
  }

  function FileIcon({ name }: { name: string }) {
    const ext = getFileExtension(name);
    // SVG icons matching Angular Material Icons style
    const stroke = '#757575';
    const size = 48;
    const sw = 1.5;
    switch (ext) {
      case 'pdf':
        return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={stroke} strokeWidth={sw}><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8Z"/><polyline points="14 2 14 8 20 8"/><path d="M10 12h1.5a1.5 1.5 0 1 1 0 3H10v-3Zm0 3v3"/></svg>;
      case 'jpg': case 'jpeg': case 'png': case 'gif': case 'svg': case 'webp': case 'tiff': case 'tif':
        return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={stroke} strokeWidth={sw}><rect width="18" height="18" x="3" y="3" rx="2"/><circle cx="9" cy="9" r="2"/><path d="m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21"/></svg>;
      case 'mp4': case 'avi': case 'mov':
        return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={stroke} strokeWidth={sw}><polygon points="5 3 19 12 5 21 5 3"/></svg>;
      case 'mp3': case 'wav':
        return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={stroke} strokeWidth={sw}><path d="M9 18V5l12-2v13"/><circle cx="6" cy="18" r="3"/><circle cx="18" cy="16" r="3"/></svg>;
      case 'zip': case 'tar': case 'gz': case '7z':
        return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={stroke} strokeWidth={sw}><path d="M21 8v13H3V8"/><path d="M1 3h22v5H1z"/><path d="M10 12h4"/></svg>;
      case 'xml': case 'json': case 'csv':
        return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={stroke} strokeWidth={sw}><polyline points="16 18 22 12 16 6"/><polyline points="8 6 2 12 8 18"/></svg>;
      default:
        return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={stroke} strokeWidth={sw}><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8Z"/><polyline points="14 2 14 8 20 8"/><line x1="16" x2="8" y1="13" y2="13"/><line x1="16" x2="8" y1="17" y2="17"/><polyline points="10 9 9 9 8 9"/></svg>;
    }
  }

  return (
    <div className="file-grid" role="list" aria-label="Fillista">
      {files.map((node) => {
        const showThumb = canPreview(node) && !failedImages.has(node.id);
        const isLoaded = loadedImages.has(node.id);
        const ext = getFileExtension(node.name).toUpperCase();

        return (
          <div key={node.id} className="file-card" role="listitem">
            <div className="file-card__preview">
              {showThumb && !isLoaded && (
                <div className="file-card__thumb-loading" aria-hidden="true">
                  <svg className="spin" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="#999" strokeWidth="2">
                    <path d="M21 12a9 9 0 1 1-6.219-8.56" />
                  </svg>
                </div>
              )}

              {showThumb ? (
                <img
                  src={getThumbnailUrl(node.id, node.name)}
                  className={`file-card__thumbnail ${!isLoaded ? 'file-card__thumbnail--loading' : ''}`}
                  loading="lazy" alt={`Förhandsgranskning av ${node.name}`}
                  onLoad={() => handleImageLoad(node.id)}
                  onError={() => handleImageError(node.id)}
                />
              ) : (
                <div className="file-card__icon-fallback" aria-hidden="true">
                  <FileIcon name={node.name} />
                </div>
              )}

              <div className="file-card__overlay">
                <button
                  type="button"
                  className="file-card__overlay-btn"
                  onClick={() => previewFile(node)}
                  aria-label={`Förhandsgranska ${node.name}`}
                  title="Förhandsgranska"
                >
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z" /><circle cx="12" cy="12" r="3" />
                  </svg>
                </button>
                <button
                  type="button"
                  className="file-card__overlay-btn"
                  onClick={() => downloadFile(node)}
                  aria-label={`Ladda ner ${node.name}`}
                  title="Ladda ner"
                >
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" /><polyline points="7 10 12 15 17 10" /><line x1="12" x2="12" y1="15" y2="3" />
                  </svg>
                </button>
              </div>
            </div>

            <div className="file-card__info">
              <span className="file-card__name" title={node.name}>{node.name}</span>
              <div className="file-card__meta">
                <span className="file-card__type">{ext || 'FIL'}</span>
                {node.size != null && <span className="file-card__size">{formatFileSize(node.size)}</span>}
              </div>
            </div>
          </div>
        );
      })}

      {viewerFile && (
        <Suspense fallback={null}>
          <FileViewer
            file={viewerFile}
            siblings={files.filter(f => !f.isDirectory)}
            initialIndex={viewerIndex}
            onClose={() => setViewerFile(null)}
          />
        </Suspense>
      )}
    </div>
  );
}

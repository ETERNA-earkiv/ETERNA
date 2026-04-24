import { useState, useEffect, useCallback } from 'react';
import type { FileNode } from '@lib/types/api';
import { getFileDownloadUrl } from '@lib/api/files';
import { formatFileSize } from '@lib/utils/file-size';
import { getFileExtension } from '@lib/utils/file-utils';
import './FileViewer.css';

interface Props {
  file: FileNode;
  siblings?: FileNode[];
  initialIndex?: number;
  onClose: () => void;
}

type FileType = 'image' | 'pdf' | 'text' | 'video' | 'audio' | 'unknown';

function detectType(node: FileNode): FileType {
  const mime = node.mimetype || '';
  if (mime.startsWith('image/')) return 'image';
  if (mime === 'application/pdf') return 'pdf';
  if (mime.startsWith('text/') || mime === 'application/json' || mime === 'application/xml') return 'text';
  if (mime.startsWith('video/')) return 'video';
  if (mime.startsWith('audio/')) return 'audio';

  const ext = getFileExtension(node.name);
  if (['jpg', 'jpeg', 'png', 'gif', 'svg', 'webp', 'bmp', 'tiff', 'tif'].includes(ext)) return 'image';
  if (ext === 'pdf') return 'pdf';
  if (['txt', 'xml', 'json', 'csv', 'md', 'log', 'html', 'htm', 'css', 'js', 'ts'].includes(ext)) return 'text';
  return 'unknown';
}

export default function FileViewer({ file: initialFile, siblings, initialIndex = 0, onClose }: Props) {
  const [currentIndex, setCurrentIndex] = useState(initialIndex);
  const [retryCount, setRetryCount] = useState(0);
  const [textContent, setTextContent] = useState<string | null>(null);
  const [blobUrl, setBlobUrl] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [maximized, setMaximized] = useState(false);

  const files = siblings && siblings.length > 1 ? siblings : [initialFile];
  const currentFile = files[currentIndex];
  const fileType = detectType(currentFile);
  const fileUrl = getFileDownloadUrl(currentFile.id);

  const hasPrev = currentIndex > 0;
  const hasNext = currentIndex < files.length - 1;

  const handleKeyDown = useCallback((e: KeyboardEvent) => {
    if (e.key === 'Escape') onClose();
    if (e.key === 'ArrowLeft' && hasPrev) setCurrentIndex((i) => i - 1);
    if (e.key === 'ArrowRight' && hasNext) setCurrentIndex((i) => i + 1);
  }, [onClose, hasPrev, hasNext]);

  useEffect(() => {
    document.addEventListener('keydown', handleKeyDown);
    document.body.style.overflow = 'hidden';
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      document.body.style.overflow = '';
    };
  }, [handleKeyDown]);

  // Load file content (text as string, PDF as blob URL)
  useEffect(() => {
    setTextContent(null);
    setError(null);

    // Clean up previous blob URL
    if (blobUrl) { URL.revokeObjectURL(blobUrl); setBlobUrl(null); }

    if (fileType === 'text') {
      setLoading(true);
      fetch(fileUrl, { credentials: 'include' })
        .then((r) => { if (!r.ok) throw new Error(`${r.status}`); return r.text(); })
        .then(setTextContent)
        .catch(() => setError('Kunde inte ladda filen.'))
        .finally(() => setLoading(false));
    } else if (fileType === 'pdf') {
      // Fetch PDF as blob to bypass Content-Disposition: attachment
      setLoading(true);
      fetch(fileUrl, { credentials: 'include' })
        .then((r) => { if (!r.ok) throw new Error(`${r.status}`); return r.blob(); })
        .then((blob) => {
          const url = URL.createObjectURL(new Blob([blob], { type: 'application/pdf' }));
          setBlobUrl(url);
        })
        .catch(() => setError('Kunde inte ladda PDF:en.'))
        .finally(() => setLoading(false));
    }

    return () => { if (blobUrl) URL.revokeObjectURL(blobUrl); };
  }, [fileUrl, fileType, retryCount]);

  return (
    <div className="file-viewer-backdrop" onClick={onClose}>
      <div className={`file-viewer ${maximized ? 'file-viewer--maximized' : ''}`} onClick={(e) => e.stopPropagation()} role="dialog" aria-modal="true" aria-label={`Filvisare: ${currentFile.name}`}>
        {/* Header */}
        <div className="file-viewer__header">
          <div className="file-viewer__title">
            <strong>{currentFile.name}</strong>
            {currentFile.size != null && <span>{formatFileSize(currentFile.size)}</span>}
            {files.length > 1 && <span>{currentIndex + 1} / {files.length}</span>}
          </div>
          <div className="file-viewer__actions">
            <a href={fileUrl} target="_blank" rel="noopener noreferrer" className="file-viewer__download-btn" aria-label="Ladda ner">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" /><polyline points="7 10 12 15 17 10" /><line x1="12" x2="12" y1="15" y2="3" /></svg>
              Ladda ner
            </a>
            <button type="button" className="file-viewer__btn" onClick={() => setMaximized(!maximized)} aria-label={maximized ? 'Minimera' : 'Maximera'}>
              {maximized ? (
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><polyline points="4 14 10 14 10 20" /><polyline points="20 10 14 10 14 4" /><line x1="14" x2="21" y1="10" y2="3" /><line x1="3" x2="10" y1="21" y2="14" /></svg>
              ) : (
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><polyline points="15 3 21 3 21 9" /><polyline points="9 21 3 21 3 15" /><line x1="21" x2="14" y1="3" y2="10" /><line x1="3" x2="10" y1="21" y2="14" /></svg>
              )}
            </button>
            <button type="button" className="file-viewer__btn" onClick={onClose} aria-label="Stäng">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M18 6 6 18" /><path d="m6 6 12 12" /></svg>
            </button>
          </div>
        </div>

        {/* Content */}
        <div className="file-viewer__content">
          {/* Navigation arrows */}
          {hasPrev && (
            <button type="button" className="file-viewer__nav file-viewer__nav--prev" onClick={() => setCurrentIndex((i) => i - 1)} aria-label="Föregående fil">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="m15 18-6-6 6-6" /></svg>
            </button>
          )}
          {hasNext && (
            <button type="button" className="file-viewer__nav file-viewer__nav--next" onClick={() => setCurrentIndex((i) => i + 1)} aria-label="Nästa fil">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="m9 18 6-6-6-6" /></svg>
            </button>
          )}

          {fileType === 'image' && (
            <img src={fileUrl} alt={currentFile.name} className="file-viewer__image" />
          )}

          {fileType === 'pdf' && (
            loading ? (
              <p style={{ color: 'white', padding: '2rem' }}>Laddar PDF...</p>
            ) : blobUrl ? (
              <iframe src={blobUrl} className="file-viewer__iframe" title={currentFile.name} />
            ) : error ? (
              <div className="file-viewer__error-box">
                <p>{error}</p>
                <div className="file-viewer__error-actions">
                  <button type="button" className="file-viewer__retry-btn" onClick={() => setRetryCount((c) => c + 1)}>Försök igen</button>
                  <a href={fileUrl} target="_blank" rel="noopener noreferrer" className="file-viewer__retry-btn">Ladda ner istället</a>
                </div>
              </div>
            ) : null
          )}

          {fileType === 'text' && (
            <div className="file-viewer__text">
              {loading && <p>Laddar...</p>}
              {error && (
                <div className="file-viewer__error-box">
                  <p>{error}</p>
                  <button type="button" className="file-viewer__retry-btn" onClick={() => setRetryCount((c) => c + 1)}>Försök igen</button>
                </div>
              )}
              {textContent && <pre>{textContent}</pre>}
            </div>
          )}

          {fileType === 'video' && (
            <video src={fileUrl} controls className="file-viewer__video">
              Din webbläsare stöder inte video.
            </video>
          )}

          {fileType === 'audio' && (
            <audio src={fileUrl} controls className="file-viewer__audio">
              Din webbläsare stöder inte ljud.
            </audio>
          )}

          {fileType === 'unknown' && (
            <div className="file-viewer__unknown">
              <p>Förhandsgranskning stöds inte för denna filtyp.</p>
              <a href={fileUrl} target="_blank" rel="noopener noreferrer" className="file-viewer__download-link">
                Ladda ner {currentFile.name}
              </a>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

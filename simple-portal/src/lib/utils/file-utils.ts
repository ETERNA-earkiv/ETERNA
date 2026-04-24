/**
 * File path utilities.
 */

export function getFileName(path: string): string {
  return path.split('/').pop() || path;
}

export function getFileExtension(filename: string): string {
  const parts = filename.split('.');
  return parts.length > 1 ? parts.pop()!.toLowerCase() : '';
}

export function getFileIcon(extension: string): string {
  const iconMap: Record<string, string> = {
    pdf: 'pdf',
    doc: 'document',
    docx: 'document',
    xls: 'document',
    xlsx: 'document',
    ppt: 'document',
    pptx: 'document',
    txt: 'document',
    xml: 'document',
    json: 'document',
    csv: 'document',
    jpg: 'image',
    jpeg: 'image',
    png: 'image',
    gif: 'image',
    svg: 'image',
    tiff: 'image',
    tif: 'image',
    mp4: 'video',
    avi: 'video',
    mov: 'video',
    mp3: 'audio',
    wav: 'audio',
    zip: 'archive',
    tar: 'archive',
    gz: 'archive',
    '7z': 'archive',
    warc: 'web-archive',
    wacz: 'web-archive',
  };
  return iconMap[extension] || 'file';
}

export function isPreviewable(mimetype: string): boolean {
  if (!mimetype) return false;
  return (
    mimetype.startsWith('image/') ||
    mimetype === 'application/pdf' ||
    mimetype.startsWith('text/') ||
    mimetype === 'application/json' ||
    mimetype === 'application/xml'
  );
}

export function buildPath(segments: string[]): string {
  return segments.filter(Boolean).join('/');
}

/**
 * FilePreview — renders a file preview based on MIME type / format.
 * Replaces GWT BitstreamPreview.java + IndexedFilePreview.java.
 *
 * Supports: images, text, PDF (embedded), audio, video, HTML.
 * Falls back to a download link for unknown formats.
 */
import { useState, useEffect } from "react";

interface FilePreviewProps {
  fileId: string;
  filename?: string;
  mimeType?: string;
  size?: number;
}

type PreviewMode = "image" | "text" | "pdf" | "audio" | "video" | "html" | "download";

function detectMode(mimeType?: string, filename?: string): PreviewMode {
  const mime = (mimeType ?? "").toLowerCase();
  const ext = (filename ?? "").split(".").pop()?.toLowerCase() ?? "";

  if (mime.startsWith("image/") || ["png", "jpg", "jpeg", "gif", "webp", "svg", "bmp", "tiff"].includes(ext)) return "image";
  if (mime === "application/pdf" || ext === "pdf") return "pdf";
  if (mime.startsWith("audio/") || ["mp3", "wav", "ogg", "flac", "m4a"].includes(ext)) return "audio";
  if (mime.startsWith("video/") || ["mp4", "webm", "ogv", "mov"].includes(ext)) return "video";
  if (mime === "text/html" || ext === "html") return "html";
  if (
    mime.startsWith("text/") ||
    ["txt", "xml", "json", "csv", "md", "log", "yaml", "yml", "properties"].includes(ext)
  ) return "text";
  return "download";
}

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
  return `${(bytes / 1024 / 1024 / 1024).toFixed(2)} GB`;
}

export default function FilePreview({ fileId, filename, mimeType, size }: FilePreviewProps) {
  const downloadUrl = `/api/v2/files/${fileId}/download`;
  const previewUrl = `/api/v2/files/${fileId}/preview`;
  const mode = detectMode(mimeType, filename);
  const [textContent, setTextContent] = useState<string | null>(null);
  const [textLoading, setTextLoading] = useState(false);
  const [textError, setTextError] = useState(false);

  useEffect(() => {
    if (mode !== "text") return;
    setTextLoading(true);
    fetch(previewUrl, { credentials: "include" })
      .then((r) => {
        if (!r.ok) throw new Error(`HTTP ${r.status}`);
        return r.text();
      })
      .then((t) => { setTextContent(t); setTextLoading(false); })
      .catch(() => { setTextError(true); setTextLoading(false); });
  }, [fileId, mode, previewUrl]);

  return (
    <div className="bg-white rounded-lg border border-gray-200 shadow-sm overflow-hidden">
      {/* Header */}
      <div className="px-4 py-3 border-b border-gray-100 flex items-center justify-between">
        <div className="text-sm font-medium text-gray-700 truncate">
          {filename ?? fileId}
          {size != null && <span className="ml-2 text-xs text-gray-400">({formatBytes(size)})</span>}
        </div>
        <a
          href={downloadUrl}
          className="text-xs text-blue-600 hover:underline shrink-0 ml-4"
          download
        >
          Download
        </a>
      </div>

      {/* Preview area */}
      <div className="p-4">
        {mode === "image" && (
          <div className="flex justify-center">
            <img
              src={previewUrl}
              alt={filename ?? "Preview"}
              className="max-w-full max-h-[600px] object-contain"
              loading="lazy"
            />
          </div>
        )}

        {mode === "pdf" && (
          <div className="h-[600px]">
            <iframe
              src={`${previewUrl}#toolbar=1&navpanes=0`}
              className="w-full h-full rounded"
              title={filename ?? "PDF preview"}
            />
          </div>
        )}

        {mode === "audio" && (
          <audio controls className="w-full" src={previewUrl}>
            Your browser does not support audio playback.
          </audio>
        )}

        {mode === "video" && (
          <video controls className="w-full max-h-[500px]" src={previewUrl}>
            Your browser does not support video playback.
          </video>
        )}

        {mode === "html" && (
          <div className="h-[500px]">
            <iframe
              src={previewUrl}
              className="w-full h-full rounded border border-gray-100"
              sandbox="allow-same-origin"
              title={filename ?? "HTML preview"}
            />
          </div>
        )}

        {mode === "text" && (
          <div>
            {textLoading && <div className="flex justify-center py-8"><span className="spinner" /></div>}
            {textError && (
              <div className="text-sm text-red-500 py-4 text-center">Failed to load preview.</div>
            )}
            {textContent !== null && (
              <pre className="text-xs text-gray-700 bg-gray-50 rounded p-3 overflow-x-auto max-h-[600px] overflow-y-auto whitespace-pre-wrap font-mono">
                {textContent}
              </pre>
            )}
          </div>
        )}

        {mode === "download" && (
          <div className="py-8 text-center text-gray-400">
            <div className="text-4xl mb-3">📄</div>
            <p className="text-sm mb-3">No preview available for this file type.</p>
            {mimeType && <p className="text-xs text-gray-400 mb-4">{mimeType}</p>}
            <a
              href={downloadUrl}
              className="px-4 py-2 bg-[var(--color-primary)] text-white text-sm rounded hover:bg-[var(--color-primary-dark)] transition-colors"
              download
            >
              Download file
            </a>
          </div>
        )}
      </div>
    </div>
  );
}

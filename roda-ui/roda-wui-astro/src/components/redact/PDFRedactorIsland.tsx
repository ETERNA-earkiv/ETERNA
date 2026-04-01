import { useEffect, useRef, useState } from "react";
import { apiPost } from "@/lib/api/client";

declare global {
  interface Window {
    PDFRedactor: {
      setUrl: (url: string) => void;
      mount: (root: HTMLElement) => void;
      unmount: () => void;
      setSaveCallback: (callback: (blob: Blob) => boolean) => void;
    };
  }
}

interface PDFRedactorIslandProps {
  fileUuid: string;
  aipId: string;
  representationId: string;
  filePath: string[];
  fileId: string;
}

function loadScript(src: string): Promise<void> {
  return new Promise((resolve, reject) => {
    if (document.querySelector(`script[src="${src}"]`)) {
      resolve();
      return;
    }
    const el = document.createElement("script");
    el.src = src;
    el.onload = () => resolve();
    el.onerror = () => reject(new Error(`Failed to load script: ${src}`));
    document.head.appendChild(el);
  });
}

function loadStyle(href: string): void {
  if (document.querySelector(`link[href="${href}"]`)) return;
  const el = document.createElement("link");
  el.rel = "stylesheet";
  el.href = href;
  document.head.appendChild(el);
}

async function findOrCreateRedactedRepresentation(
  aipId: string,
  representationId: string
): Promise<string> {
  // Search for existing "Redacted" representation
  try {
    const result = await apiPost<{
      results: Array<{ id: string; uuid: string; type: string }>;
      totalCount: number;
    }>("/representations/find", {
      filter: {
        parameters: [
          { type: "SimpleFilterParameter", name: "aipId", value: aipId },
          { type: "SimpleFilterParameter", name: "type", value: "Redacted" },
        ],
      },
      sublist: { firstElementIndex: 0, maximumElementCount: 1 },
      onlyActive: true,
    });

    if (result.totalCount > 0) {
      return result.results[0].id;
    }
  } catch {
    // Fall through to create
  }

  // Create a new representation
  const newRep = await apiPost<{ id: string }>(`/aips/${encodeURIComponent(aipId)}/representations`, {
    type: "Redacted",
    details: "Creating representation for redacted files",
  });

  const newRepId = newRep.id;

  // Change its type to "Redacted" via a job
  await apiPost("/representations/changeType", {
    selectedItems: {
      ids: [newRepId],
      filter: null,
    },
    newType: "Redacted",
    details: 'Setting representation type to "Redacted"',
  });

  return newRepId;
}

export default function PDFRedactorIsland({
  fileUuid,
  aipId,
  representationId,
  filePath,
  fileId,
}: PDFRedactorIslandProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [status, setStatus] = useState<"loading" | "ready" | "error">("loading");
  const [errorMessage, setErrorMessage] = useState("");
  const [saveStatus, setSaveStatus] = useState<"idle" | "saving" | "saved" | "error">("idle");

  const downloadUrl = `/api/v2/files/${encodeURIComponent(fileUuid)}/preview`;

  useEffect(() => {
    let mounted = true;

    async function init() {
      try {
        loadStyle("/webjars/pdf-redactor/1.0.1/pdf-redactor.css");
        await loadScript("/webjars/pdf-redactor/1.0.1/pdf-redactor.js");

        if (!mounted || !containerRef.current) return;
        if (!window.PDFRedactor) {
          throw new Error("PDFRedactor library failed to initialize");
        }

        window.PDFRedactor.setUrl(downloadUrl);
        window.PDFRedactor.setSaveCallback((blob: Blob) => {
          setSaveStatus("saving");

          findOrCreateRedactedRepresentation(aipId, representationId)
            .then((redactedRepId) => {
              const params = new URLSearchParams({
                "aip-id": aipId,
                "representation-id": redactedRepId,
                details: "Saved redacted version",
              });
              filePath.forEach((p) => params.append("folder", p));

              const formData = new FormData();
              formData.append("upl", blob, fileId);

              return fetch(`/api/v2/files/upload?${params.toString()}`, {
                method: "POST",
                body: formData,
                credentials: "include",
              });
            })
            .then(() => {
              setSaveStatus("saved");
              setTimeout(() => setSaveStatus("idle"), 3000);
            })
            .catch(() => {
              setSaveStatus("error");
            });

          return true;
        });

        window.PDFRedactor.mount(containerRef.current);
        if (mounted) setStatus("ready");
      } catch (err) {
        if (mounted) {
          setErrorMessage(err instanceof Error ? err.message : "Unknown error");
          setStatus("error");
        }
      }
    }

    init();

    return () => {
      mounted = false;
      if (window.PDFRedactor) {
        window.PDFRedactor.unmount();
      }
    };
  }, [downloadUrl, aipId, representationId, fileId]);

  return (
    <div className="flex flex-col h-full">
      {saveStatus === "saving" && (
        <div className="mb-2 px-4 py-2 bg-blue-50 border border-blue-200 rounded text-sm text-blue-700">
          Saving redacted file...
        </div>
      )}
      {saveStatus === "saved" && (
        <div className="mb-2 px-4 py-2 bg-green-50 border border-green-200 rounded text-sm text-green-700">
          Redacted file saved successfully.
        </div>
      )}
      {saveStatus === "error" && (
        <div className="mb-2 px-4 py-2 bg-red-50 border border-red-200 rounded text-sm text-red-700">
          Failed to save redacted file.
        </div>
      )}

      {status === "loading" && (
        <div className="flex items-center justify-center h-64 text-gray-500">
          Loading PDF redactor...
        </div>
      )}
      {status === "error" && (
        <div className="flex items-center justify-center h-64 text-red-500">
          Error: {errorMessage}
        </div>
      )}

      <div
        ref={containerRef}
        className="flex-1 min-h-[600px]"
        style={{ display: status === "ready" ? "block" : "none" }}
      />
    </div>
  );
}

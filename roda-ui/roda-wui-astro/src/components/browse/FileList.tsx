/**
 * FileList — list files in a representation.
 * Replaces GWT FileList.java.
 */
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import DataTable from "@/components/common/DataTable";
import type { ColumnDef } from "@tanstack/react-table";

const qc = new QueryClient();

interface IndexedFile {
  id: string;
  uuid?: string;
  aipId: string;
  representationId: string;
  path: string[];
  originalName?: string;
  size?: number;
  formatDesignationName?: string;
  formatDesignationVersion?: string;
  pronom?: string;
  directory?: boolean;
}

function formatBytes(bytes: number | undefined): string {
  if (!bytes) return "—";
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
  return `${(bytes / 1024 / 1024 / 1024).toFixed(2)} GB`;
}

const columns: ColumnDef<IndexedFile>[] = [
  {
    id: "name",
    header: "Name",
    cell: (info) => {
      const f = info.row.original;
      const name = f.originalName ?? (f.path?.length ? f.path[f.path.length - 1] : f.id);
      if (f.directory) {
        return (
          <span className="flex items-center gap-1 text-gray-700">
            <span className="text-yellow-500">📁</span>
            <span className="text-sm">{name}</span>
          </span>
        );
      }
      return (
        <a href={`/browse/file/${f.uuid ?? f.id}`} className="text-blue-700 hover:underline text-sm flex items-center gap-1">
          <span className="text-gray-400">📄</span>
          {name}
        </a>
      );
    },
  },
  {
    id: "format",
    header: "Format",
    cell: (info) => {
      const f = info.row.original;
      return (
        <div className="text-xs text-gray-600">
          {f.formatDesignationName || "—"}
          {f.formatDesignationVersion && ` ${f.formatDesignationVersion}`}
          {f.pronom && <span className="ml-1 text-gray-400">({f.pronom})</span>}
        </div>
      );
    },
  },
  {
    id: "size",
    accessorKey: "size",
    header: "Size",
    size: 100,
    cell: (info) => <span className="text-xs text-gray-600">{formatBytes(info.getValue() as number)}</span>,
  },
  {
    id: "actions",
    header: "",
    size: 100,
    cell: (info) => {
      const f = info.row.original;
      if (f.directory) return null;
      return (
        <a
          href={`/api/v2/files/${f.uuid ?? f.id}/download`}
          className="text-xs text-blue-600 hover:underline"
        >
          Download
        </a>
      );
    },
  },
];

interface FileListProps {
  aipId: string;
  representationId: string;
}

function Inner({ aipId, representationId }: FileListProps) {
  return (
    <DataTable<IndexedFile>
      resource="files"
      columns={columns}
      filter={{
        parameters: [
          { type: "SimpleFilterParameter", name: "aipId", value: aipId },
          { type: "SimpleFilterParameter", name: "representationUUID", value: representationId },
        ],
      }}
      defaultPageSize={25}
      emptyMessage="No files found."
    />
  );
}

export default function FileList(props: FileListProps) {
  return (
    <QueryClientProvider client={qc}>
      <Inner {...props} />
    </QueryClientProvider>
  );
}

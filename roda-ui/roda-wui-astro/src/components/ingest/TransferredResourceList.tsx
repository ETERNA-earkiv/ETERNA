/**
 * TransferredResourceList — browse submitted SIP/transfer resources.
 * Replaces GWT TransferredResourceList.java.
 */
import { useState } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import DataTable from "@/components/common/DataTable";
import type { ColumnDef } from "@tanstack/react-table";

const qc = new QueryClient();

interface TransferredResource {
  id: string;
  uuid?: string;
  name: string;
  fullPath?: string;
  size?: number;
  creationDate?: string;
  lastScanDate?: string;
  isDirectory?: boolean;
  parentUUID?: string;
}

function formatBytes(bytes?: number): string {
  if (!bytes) return "—";
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
  return `${(bytes / 1024 / 1024 / 1024).toFixed(2)} GB`;
}

const makeColumns = (onSelect: (ids: string[]) => void): ColumnDef<TransferredResource>[] => [
  {
    id: "select",
    header: "",
    size: 36,
    cell: (info) => (
      <input
        type="checkbox"
        checked={info.row.getIsSelected()}
        onChange={info.row.getToggleSelectedHandler()}
        className="rounded border-gray-300"
      />
    ),
  },
  {
    id: "name",
    accessorKey: "name",
    header: "Name",
    cell: (info) => {
      const r = info.row.original;
      return (
        <div className="flex items-center gap-2">
          <span className="text-lg leading-none">{r.isDirectory ? "📁" : "📦"}</span>
          <div>
            <p className="text-sm font-medium text-gray-800">{r.name}</p>
            {r.fullPath && r.fullPath !== r.name && (
              <p className="text-xs text-gray-400 font-mono">{r.fullPath}</p>
            )}
          </div>
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
    id: "creationDate",
    accessorKey: "creationDate",
    header: "Created",
    size: 130,
    cell: (info) => {
      const v = info.getValue() as string | undefined;
      return <span className="text-xs text-gray-600">{v ? new Date(v).toLocaleDateString() : "—"}</span>;
    },
  },
  {
    id: "actions",
    header: "",
    size: 140,
    cell: (info) => {
      const r = info.row.original;
      return (
        <div className="flex gap-2">
          <a
            href={`/api/v2/transfers/${r.uuid ?? r.id}/download`}
            className="text-xs text-blue-600 hover:underline"
          >
            Download
          </a>
          <a
            href={`/ingest/transfer/${r.uuid ?? r.id}`}
            className="text-xs text-blue-600 hover:underline"
          >
            Details
          </a>
        </div>
      );
    },
  },
];

interface TransferredResourceListProps {
  parentId?: string;
}

function Inner({ parentId }: TransferredResourceListProps) {
  const [selectedIds, setSelectedIds] = useState<string[]>([]);

  const filter = parentId
    ? { parameters: [{ type: "SimpleFilterParameter", name: "parentUUID", value: parentId }] }
    : { parameters: [{ type: "EmptyKeyFilterParameter", name: "parentUUID" }] };

  const columns = makeColumns(setSelectedIds);

  return (
    <div className="space-y-4">
      {selectedIds.length > 0 && (
        <div className="flex items-center gap-3 p-3 bg-blue-50 rounded-lg text-sm">
          <span className="font-medium text-blue-700">{selectedIds.length} selected</span>
          <a
            href={`/ingest/process/create?ids=${selectedIds.join(",")}`}
            className="px-3 py-1 bg-blue-600 text-white rounded text-xs hover:bg-blue-700 transition-colors"
          >
            Start ingest
          </a>
        </div>
      )}
      <DataTable<TransferredResource>
        resource="transfers"
        columns={columns}
        filter={filter}
        defaultPageSize={25}
        emptyMessage="No transferred resources found."
        onSelectionChange={(rows) => setSelectedIds(rows.map((r) => r.uuid ?? r.id))}
      />
    </div>
  );
}

export default function TransferredResourceList(props: TransferredResourceListProps) {
  return (
    <QueryClientProvider client={qc}>
      <Inner {...props} />
    </QueryClientProvider>
  );
}

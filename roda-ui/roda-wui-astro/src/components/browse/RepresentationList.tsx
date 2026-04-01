/**
 * RepresentationList — list representations for a given AIP.
 * Replaces GWT RepresentationList.java.
 */
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import DataTable from "@/components/common/DataTable";
import type { ColumnDef } from "@tanstack/react-table";

const qc = new QueryClient();

interface IndexedRepresentation {
  id: string;
  uuid?: string;
  aipId: string;
  type?: string;
  status?: string;
  original?: boolean;
  numberOfDataFiles?: number;
  sizeInBytes?: number;
  title?: string;
}

function formatBytes(bytes: number): string {
  if (!bytes) return "—";
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
  return `${(bytes / 1024 / 1024 / 1024).toFixed(2)} GB`;
}

const columns: ColumnDef<IndexedRepresentation>[] = [
  {
    id: "type",
    accessorKey: "type",
    header: "Type",
    size: 140,
    cell: (info) => (
      <span className="px-2 py-0.5 rounded bg-purple-50 text-purple-700 text-xs">
        {info.getValue() as string || "—"}
      </span>
    ),
  },
  {
    id: "original",
    accessorKey: "original",
    header: "Original",
    size: 90,
    cell: (info) => (
      <span className={`px-2 py-0.5 rounded text-xs ${info.getValue() ? "bg-green-100 text-green-700" : "bg-gray-100 text-gray-500"}`}>
        {info.getValue() ? "Yes" : "No"}
      </span>
    ),
  },
  {
    id: "status",
    accessorKey: "status",
    header: "Status",
    size: 100,
    cell: (info) => {
      const v = info.getValue() as string | undefined;
      return v ? <span className="text-xs text-gray-600">{v}</span> : <span className="text-xs text-gray-400">—</span>;
    },
  },
  {
    id: "numberOfDataFiles",
    accessorKey: "numberOfDataFiles",
    header: "Files",
    size: 80,
    cell: (info) => <span className="text-xs text-gray-600">{(info.getValue() as number) ?? "—"}</span>,
  },
  {
    id: "sizeInBytes",
    accessorKey: "sizeInBytes",
    header: "Size",
    size: 100,
    cell: (info) => <span className="text-xs text-gray-600">{formatBytes(info.getValue() as number)}</span>,
  },
  {
    id: "actions",
    header: "",
    size: 120,
    cell: (info) => (
      <div className="flex gap-2">
        <a
          href={`/browse/${info.row.original.aipId}/representation/${info.row.original.uuid ?? info.row.original.id}`}
          className="text-xs text-blue-600 hover:underline"
        >
          Browse
        </a>
        <a
          href={`/api/v2/representations/${info.row.original.uuid ?? info.row.original.id}/download`}
          className="text-xs text-blue-600 hover:underline"
        >
          Download
        </a>
      </div>
    ),
  },
];

interface RepresentationListProps {
  aipId: string;
}

function Inner({ aipId }: RepresentationListProps) {
  return (
    <DataTable<IndexedRepresentation>
      resource="representations"
      columns={columns}
      filter={{
        parameters: [
          { type: "SimpleFilterParameter", name: "aipId", value: aipId },
        ],
      }}
      defaultPageSize={10}
      emptyMessage="No representations found."
    />
  );
}

export default function RepresentationList(props: RepresentationListProps) {
  return (
    <QueryClientProvider client={qc}>
      <Inner {...props} />
    </QueryClientProvider>
  );
}

/**
 * AIPList — browse top-level Archival Information Packages.
 * Replaces GWT BrowseTop + AIPList.java.
 */
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import DataTable from "@/components/common/DataTable";
import type { ColumnDef } from "@tanstack/react-table";
import type { AstroUser } from "@/lib/store/userStore";

const qc = new QueryClient();

interface IndexedAIP {
  id: string;
  title: string;
  level?: string;
  dateInitial?: string;
  dateFinal?: string;
  numberOfSubmissionFiles?: number;
  numberOfDocumentationFiles?: number;
  state?: string;
}

const columns: ColumnDef<IndexedAIP>[] = [
  {
    id: "title",
    accessorKey: "title",
    header: "Title",
    cell: (info) => (
      <a
        href={`/browse/${info.row.original.id}`}
        className="text-blue-700 hover:underline font-medium"
      >
        {info.getValue() as string || "(untitled)"}
      </a>
    ),
  },
  {
    id: "level",
    accessorKey: "level",
    header: "Level",
    size: 120,
    cell: (info) => (
      <span className="px-2 py-0.5 rounded-full text-xs bg-gray-100 text-gray-600">
        {info.getValue() as string || "—"}
      </span>
    ),
  },
  {
    id: "dateInitial",
    accessorKey: "dateInitial",
    header: "Date (initial)",
    size: 140,
    cell: (info) => {
      const v = info.getValue() as string | undefined;
      return v ? new Date(v).toLocaleDateString() : "—";
    },
  },
  {
    id: "dateFinal",
    accessorKey: "dateFinal",
    header: "Date (final)",
    size: 140,
    cell: (info) => {
      const v = info.getValue() as string | undefined;
      return v ? new Date(v).toLocaleDateString() : "—";
    },
  },
  {
    id: "state",
    accessorKey: "state",
    header: "State",
    size: 100,
    cell: (info) => {
      const v = info.getValue() as string | undefined;
      const color = v === "ACTIVE" ? "bg-green-100 text-green-700" : "bg-gray-100 text-gray-600";
      return v ? (
        <span className={`px-2 py-0.5 rounded-full text-xs ${color}`}>{v}</span>
      ) : "—";
    },
  },
];

interface AIPListProps {
  user: AstroUser | null;
  parentId?: string;
}

function Inner({ parentId }: AIPListProps) {
  const filter = parentId
    ? {
        parameters: [
          { type: "SimpleFilterParameter", name: "parentId", value: parentId },
        ],
      }
    : {
        parameters: [
          { type: "EmptyKeyFilterParameter", name: "parentId" },
        ],
      };

  return (
    <DataTable<IndexedAIP>
      resource="aips"
      columns={columns}
      filter={filter}
      defaultPageSize={25}
      emptyMessage="No archival information packages found."
    />
  );
}

export default function AIPList(props: AIPListProps) {
  return (
    <QueryClientProvider client={qc}>
      <Inner {...props} />
    </QueryClientProvider>
  );
}

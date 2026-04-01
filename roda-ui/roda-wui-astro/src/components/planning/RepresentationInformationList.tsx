/**
 * RepresentationInformationList — representation information register.
 * Replaces GWT RepresentationInformationRegister.java.
 * Calls POST /api/v2/representation_information/find
 */
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import DataTable from "@/components/common/DataTable";
import type { ColumnDef } from "@tanstack/react-table";

const qc = new QueryClient();

interface RepresentationInformation {
  id: string;
  name?: string;
  family?: string;
  categories?: string[];
  tags?: string[];
  extras?: string;
  support?: string;
  relations?: { relationType?: string; link?: string }[];
}

const COLUMNS: ColumnDef<RepresentationInformation>[] = [
  {
    id: "name",
    accessorKey: "name",
    header: "Name",
    cell: (info) => (
      <a href={`/planning/representation-information/${info.row.original.id}`} className="text-blue-600 hover:underline text-sm font-medium">
        {(info.getValue() as string) || info.row.original.id}
      </a>
    ),
  },
  {
    id: "family",
    accessorKey: "family",
    header: "Family",
    size: 120,
    cell: (info) => <span className="text-xs text-gray-600">{(info.getValue() as string) || "—"}</span>,
  },
  {
    id: "support",
    accessorKey: "support",
    header: "Support",
    size: 100,
    cell: (info) => {
      const s = info.getValue() as string | undefined;
      const cls = s === "SUPPORTED" ? "bg-green-100 text-green-700"
        : s === "UNSUPPORTED" ? "bg-red-100 text-red-700"
        : "bg-gray-100 text-gray-600";
      return s ? <span className={`px-2 py-0.5 rounded text-xs ${cls}`}>{s}</span> : <span className="text-xs text-gray-400">—</span>;
    },
  },
  {
    id: "tags",
    accessorKey: "tags",
    header: "Tags",
    cell: (info) => {
      const tags = info.getValue() as string[] | undefined;
      if (!tags?.length) return <span className="text-xs text-gray-400">—</span>;
      return (
        <div className="flex flex-wrap gap-1">
          {tags.slice(0, 3).map((t) => (
            <span key={t} className="px-1.5 py-0.5 bg-blue-50 text-blue-600 rounded text-xs">{t}</span>
          ))}
          {tags.length > 3 && <span className="text-xs text-gray-400">+{tags.length - 3}</span>}
        </div>
      );
    },
  },
];

export default function RepresentationInformationList() {
  return (
    <QueryClientProvider client={qc}>
      <DataTable<RepresentationInformation>
        resource="representation_information"
        columns={COLUMNS}
        defaultPageSize={25}
        emptyMessage="No representation information found."
      />
    </QueryClientProvider>
  );
}

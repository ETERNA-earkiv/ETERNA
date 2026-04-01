/**
 * DisposalConfirmationList — paginated disposal confirmations.
 * Replaces GWT DisposalConfirmationList.java.
 * Calls POST /api/v2/disposal/confirmations/find
 */
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import DataTable from "@/components/common/DataTable";
import type { ColumnDef } from "@tanstack/react-table";

const qc = new QueryClient();

interface DisposalConfirmation {
  id: string;
  title?: string;
  state?: string;
  createdBy?: string;
  createdOn?: string;
  executedBy?: string;
  executedOn?: string;
  numberOfAIPs?: number;
  numberOfDestroyedAIPs?: number;
  scheduleTitle?: string;
}

const COLUMNS: ColumnDef<DisposalConfirmation>[] = [
  {
    id: "title",
    accessorKey: "title",
    header: "Title",
    cell: (info) => (
      <a href={`/disposal/confirmations/${info.row.original.id}`} className="text-blue-600 hover:underline text-sm font-medium">
        {(info.getValue() as string) || info.row.original.id}
      </a>
    ),
  },
  {
    id: "state",
    accessorKey: "state",
    header: "State",
    size: 110,
    cell: (info) => {
      const s = info.getValue() as string | undefined;
      const cls = s === "APPROVED" ? "bg-green-100 text-green-700"
        : s === "REJECTED" ? "bg-red-100 text-red-700"
        : s === "PENDING_APPROVAL" ? "bg-yellow-100 text-yellow-700"
        : "bg-gray-100 text-gray-600";
      return s ? <span className={`px-2 py-0.5 rounded text-xs font-medium ${cls}`}>{s.replace(/_/g, " ")}</span>
        : <span className="text-xs text-gray-400">—</span>;
    },
  },
  {
    id: "numberOfAIPs",
    accessorKey: "numberOfAIPs",
    header: "AIPs",
    size: 80,
    cell: (info) => <span className="text-xs text-gray-600">{String(info.getValue() ?? "—")}</span>,
  },
  {
    id: "scheduleTitle",
    accessorKey: "scheduleTitle",
    header: "Schedule",
    cell: (info) => <span className="text-xs text-gray-600">{(info.getValue() as string) || "—"}</span>,
  },
  {
    id: "createdOn",
    accessorKey: "createdOn",
    header: "Created",
    size: 110,
    cell: (info) => {
      const v = info.getValue() as string | undefined;
      return <span className="text-xs text-gray-500">{v ? new Date(v).toLocaleDateString() : "—"}</span>;
    },
  },
];

export default function DisposalConfirmationList() {
  return (
    <QueryClientProvider client={qc}>
      <DataTable<DisposalConfirmation>
        resource="disposal/confirmations"
        columns={COLUMNS}
        defaultPageSize={25}
        emptyMessage="No disposal confirmations found."
      />
    </QueryClientProvider>
  );
}

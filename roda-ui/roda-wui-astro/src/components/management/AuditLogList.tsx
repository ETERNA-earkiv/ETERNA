/**
 * AuditLogList — audit log viewer.
 * Replaces GWT LogList.java.
 * Calls POST /api/v2/log/find
 */
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import DataTable from "@/components/common/DataTable";
import type { ColumnDef } from "@tanstack/react-table";

const qc = new QueryClient();

interface LogEntry {
  id: string;
  username?: string;
  actionComponent?: string;
  actionMethod?: string;
  address?: string;
  datetime?: string;
  duration?: number;
  parameters?: string[];
  relatedObjectID?: string;
  relatedObjectClass?: string;
  outcome?: string;
}

const COLUMNS: ColumnDef<LogEntry>[] = [
  {
    id: "datetime",
    accessorKey: "datetime",
    header: "Date",
    size: 150,
    cell: (info) => {
      const v = info.getValue() as string | undefined;
      return <span className="text-xs text-gray-500">{v ? new Date(v).toLocaleString() : "—"}</span>;
    },
  },
  {
    id: "username",
    accessorKey: "username",
    header: "User",
    size: 120,
    cell: (info) => <span className="text-xs text-gray-700">{(info.getValue() as string) || "—"}</span>,
  },
  {
    id: "actionComponent",
    header: "Action",
    cell: (info) => {
      const r = info.row.original;
      const action = [r.actionComponent, r.actionMethod].filter(Boolean).join(" → ");
      return <span className="text-xs text-gray-700 font-mono">{action || "—"}</span>;
    },
  },
  {
    id: "address",
    accessorKey: "address",
    header: "IP",
    size: 120,
    cell: (info) => <span className="text-xs text-gray-500 font-mono">{(info.getValue() as string) || "—"}</span>,
  },
  {
    id: "outcome",
    accessorKey: "outcome",
    header: "Outcome",
    size: 90,
    cell: (info) => {
      const v = info.getValue() as string | undefined;
      const cls = v === "SUCCESS" ? "bg-green-100 text-green-700"
        : v === "FAILURE" ? "bg-red-100 text-red-700"
        : "bg-gray-100 text-gray-600";
      return v ? <span className={`px-2 py-0.5 rounded text-xs ${cls}`}>{v}</span> : <span className="text-xs text-gray-400">—</span>;
    },
  },
  {
    id: "duration",
    accessorKey: "duration",
    header: "ms",
    size: 70,
    cell: (info) => {
      const v = info.getValue() as number | undefined;
      return <span className="text-xs text-gray-400">{v != null ? v : "—"}</span>;
    },
  },
];

export default function AuditLogList() {
  return (
    <QueryClientProvider client={qc}>
      <DataTable<LogEntry>
        resource="log"
        columns={COLUMNS}
        defaultPageSize={50}
        emptyMessage="No log entries found."
      />
    </QueryClientProvider>
  );
}

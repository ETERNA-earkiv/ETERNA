/**
 * PreservationAgentList — paginated preservation agents.
 * Replaces GWT PreservationAgentRegister.java.
 * Calls POST /api/v2/preservation_agents/find
 */
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import DataTable from "@/components/common/DataTable";
import type { ColumnDef } from "@tanstack/react-table";

const qc = new QueryClient();

interface PreservationAgent {
  id: string;
  name?: string;
  type?: string;
  version?: string;
  note?: string;
  extension?: unknown[];
}

const COLUMNS: ColumnDef<PreservationAgent>[] = [
  {
    id: "name",
    accessorKey: "name",
    header: "Agent name",
    cell: (info) => (
      <a href={`/planning/agents/${info.row.original.id}`} className="text-blue-600 hover:underline text-sm font-medium">
        {(info.getValue() as string) || info.row.original.id}
      </a>
    ),
  },
  {
    id: "type",
    accessorKey: "type",
    header: "Type",
    size: 120,
    cell: (info) => <span className="text-xs text-gray-600">{(info.getValue() as string) || "—"}</span>,
  },
  {
    id: "version",
    accessorKey: "version",
    header: "Version",
    size: 100,
    cell: (info) => <span className="text-xs text-gray-500">{(info.getValue() as string) || "—"}</span>,
  },
  {
    id: "note",
    accessorKey: "note",
    header: "Note",
    cell: (info) => (
      <span className="text-xs text-gray-500 truncate block max-w-xs">{(info.getValue() as string) || "—"}</span>
    ),
  },
];

export default function PreservationAgentList() {
  return (
    <QueryClientProvider client={qc}>
      <DataTable<PreservationAgent>
        resource="preservation_agents"
        columns={COLUMNS}
        defaultPageSize={25}
        emptyMessage="No preservation agents found."
      />
    </QueryClientProvider>
  );
}

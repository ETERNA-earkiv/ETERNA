/**
 * PreservationEventList — paginated preservation events.
 * Replaces GWT PreservationEventRegister.java.
 * Calls POST /api/v2/preservation_events/find
 */
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import DataTable from "@/components/common/DataTable";
import type { ColumnDef } from "@tanstack/react-table";
import type { FilterParameter } from "@/lib/api/client";

const qc = new QueryClient();

interface PreservationEvent {
  id: string;
  eventType?: string;
  eventDateTime?: string;
  eventDetail?: string;
  eventOutcome?: string;
  eventOutcomeDetailNote?: string;
  aipId?: string;
  representationId?: string;
  fileId?: string;
  agentId?: string;
  agentName?: string;
}

const COLUMNS: ColumnDef<PreservationEvent>[] = [
  {
    id: "eventType",
    accessorKey: "eventType",
    header: "Event type",
    cell: (info) => <span className="text-sm text-gray-700">{(info.getValue() as string) || "—"}</span>,
  },
  {
    id: "eventDateTime",
    accessorKey: "eventDateTime",
    header: "Date",
    size: 160,
    cell: (info) => {
      const v = info.getValue() as string | undefined;
      return <span className="text-xs text-gray-500">{v ? new Date(v).toLocaleString() : "—"}</span>;
    },
  },
  {
    id: "eventOutcome",
    accessorKey: "eventOutcome",
    header: "Outcome",
    size: 100,
    cell: (info) => {
      const v = info.getValue() as string | undefined;
      const cls = v === "SUCCESS" ? "bg-green-100 text-green-700"
        : v === "FAILURE" ? "bg-red-100 text-red-700"
        : "bg-gray-100 text-gray-600";
      return v ? <span className={`px-2 py-0.5 rounded text-xs font-medium ${cls}`}>{v}</span> : <span className="text-xs text-gray-400">—</span>;
    },
  },
  {
    id: "agentName",
    accessorKey: "agentName",
    header: "Agent",
    size: 140,
    cell: (info) => {
      const r = info.row.original;
      return r.agentId ? (
        <a href={`/planning/agents/${r.agentId}`} className="text-xs text-blue-600 hover:underline">
          {(info.getValue() as string) || r.agentId}
        </a>
      ) : <span className="text-xs text-gray-500">{(info.getValue() as string) || "—"}</span>;
    },
  },
  {
    id: "aipId",
    accessorKey: "aipId",
    header: "AIP",
    cell: (info) => {
      const v = info.getValue() as string | undefined;
      return v ? (
        <a href={`/browse/${v}`} className="text-xs text-blue-600 hover:underline font-mono truncate block max-w-xs">{v}</a>
      ) : <span className="text-xs text-gray-400">—</span>;
    },
  },
];

interface PreservationEventListProps {
  aipId?: string;
  representationId?: string;
}

function Inner({ aipId, representationId }: PreservationEventListProps) {
  const parameters: FilterParameter[] = [];
  if (aipId) parameters.push({ type: "SimpleFilterParameter", name: "aipId", value: aipId });
  if (representationId) parameters.push({ type: "SimpleFilterParameter", name: "representationId", value: representationId });
  const filter = parameters.length > 0 ? { parameters } : undefined;

  return (
    <DataTable<PreservationEvent>
      resource="preservation_events"
      columns={COLUMNS}
      filter={filter}
      defaultPageSize={25}
      emptyMessage="No preservation events found."
    />
  );
}

export default function PreservationEventList(props: PreservationEventListProps) {
  return (
    <QueryClientProvider client={qc}>
      <Inner {...props} />
    </QueryClientProvider>
  );
}

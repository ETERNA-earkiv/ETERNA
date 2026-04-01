/**
 * JobList — paginated job table.
 * Replaces GWT IngestJobList.java / ActionJobList.java.
 * Calls POST /api/v2/jobs/find
 */
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import DataTable from "@/components/common/DataTable";
import type { ColumnDef } from "@tanstack/react-table";

const qc = new QueryClient();

interface Job {
  id: string;
  name?: string;
  username?: string;
  state?: string;
  objectsCount?: number;
  objectsProcessedWithSuccess?: number;
  objectsProcessedWithFailure?: number;
  startDate?: string;
  endDate?: string;
  pluginType?: string;
}

function stateClass(state?: string): string {
  switch (state) {
    case "COMPLETED": return "bg-green-100 text-green-700";
    case "FAILED_DURING_CREATION":
    case "FAILED_DURING_EXECUTION": return "bg-red-100 text-red-700";
    case "CREATED":
    case "STARTED": return "bg-blue-100 text-blue-700";
    default: return "bg-gray-100 text-gray-600";
  }
}

const COLUMNS: ColumnDef<Job>[] = [
  {
    id: "name",
    accessorKey: "name",
    header: "Name",
    cell: (info) => (
      <a href={`/ingest/process/${info.row.original.id}`} className="text-blue-600 hover:underline text-sm font-medium">
        {info.getValue() as string || info.row.original.id}
      </a>
    ),
  },
  {
    id: "username",
    accessorKey: "username",
    header: "Started by",
    size: 140,
    cell: (info) => <span className="text-xs text-gray-600">{info.getValue() as string || "—"}</span>,
  },
  {
    id: "state",
    accessorKey: "state",
    header: "State",
    size: 160,
    cell: (info) => {
      const s = info.getValue() as string | undefined;
      return <span className={`px-2 py-0.5 rounded text-xs font-medium ${stateClass(s)}`}>{s ?? "—"}</span>;
    },
  },
  {
    id: "progress",
    header: "Progress",
    size: 120,
    cell: (info) => {
      const r = info.row.original;
      const total = r.objectsCount ?? 0;
      const ok = r.objectsProcessedWithSuccess ?? 0;
      const err = r.objectsProcessedWithFailure ?? 0;
      if (!total) return <span className="text-xs text-gray-400">—</span>;
      return (
        <span className="text-xs text-gray-600">
          {ok}/{total}
          {err > 0 && <span className="text-red-600 ml-1">({err} failed)</span>}
        </span>
      );
    },
  },
  {
    id: "startDate",
    accessorKey: "startDate",
    header: "Started",
    size: 130,
    cell: (info) => {
      const v = info.getValue() as string | undefined;
      return <span className="text-xs text-gray-500">{v ? new Date(v).toLocaleString() : "—"}</span>;
    },
  },
  {
    id: "endDate",
    accessorKey: "endDate",
    header: "Finished",
    size: 130,
    cell: (info) => {
      const v = info.getValue() as string | undefined;
      return <span className="text-xs text-gray-500">{v ? new Date(v).toLocaleString() : "—"}</span>;
    },
  },
];

interface JobListProps {
  pluginType?: string;
}

function Inner({ pluginType }: JobListProps) {
  const filter = pluginType
    ? { parameters: [{ type: "SimpleFilterParameter", name: "pluginType", value: pluginType }] }
    : { parameters: [] };

  return (
    <DataTable<Job>
      resource="jobs"
      columns={COLUMNS}
      filter={filter}
      defaultPageSize={25}
      emptyMessage="No jobs found."
    />
  );
}

export default function JobList(props: JobListProps) {
  return (
    <QueryClientProvider client={qc}>
      <Inner {...props} />
    </QueryClientProvider>
  );
}

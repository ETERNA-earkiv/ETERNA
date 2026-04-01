/**
 * RiskIncidenceList — paginated risk incidence register.
 * Replaces GWT RiskIncidenceRegister.java.
 * Calls POST /api/v2/risks/incidences/find
 */
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import DataTable from "@/components/common/DataTable";
import type { ColumnDef } from "@tanstack/react-table";
import type { FilterParameter } from "@/lib/api/client";

const qc = new QueryClient();

interface RiskIncidence {
  id: string;
  riskId?: string;
  riskName?: string;
  objectId?: string;
  objectClass?: string;
  description?: string;
  probability?: number;
  impact?: number;
  severity?: number;
  status?: string;
  detectedOn?: string;
  detectedBy?: string;
  mitigatedOn?: string;
  mitigationDescription?: string;
}

const COLUMNS: ColumnDef<RiskIncidence>[] = [
  {
    id: "description",
    accessorKey: "description",
    header: "Description",
    cell: (info) => (
      <a href={`/planning/incidences/${info.row.original.id}`} className="text-blue-600 hover:underline text-sm">
        {(info.getValue() as string) || info.row.original.id}
      </a>
    ),
  },
  {
    id: "riskName",
    accessorKey: "riskName",
    header: "Risk",
    cell: (info) => {
      const r = info.row.original;
      return r.riskId ? (
        <a href={`/planning/risks/${r.riskId}`} className="text-xs text-blue-600 hover:underline">{(info.getValue() as string) || r.riskId}</a>
      ) : <span className="text-xs text-gray-500">—</span>;
    },
  },
  {
    id: "severity",
    accessorKey: "severity",
    header: "Severity",
    size: 90,
    cell: (info) => {
      const v = info.getValue() as number | undefined;
      if (!v) return <span className="text-xs text-gray-400">—</span>;
      const cls = v >= 15 ? "bg-red-100 text-red-700" : v >= 8 ? "bg-yellow-100 text-yellow-700" : "bg-green-100 text-green-700";
      return <span className={`px-2 py-0.5 rounded text-xs font-medium ${cls}`}>{v}</span>;
    },
  },
  {
    id: "status",
    accessorKey: "status",
    header: "Status",
    size: 100,
    cell: (info) => {
      const s = info.getValue() as string | undefined;
      const cls = s === "MITIGATED" ? "bg-green-100 text-green-700" : "bg-yellow-100 text-yellow-700";
      return s ? <span className={`px-2 py-0.5 rounded text-xs ${cls}`}>{s}</span> : <span className="text-xs text-gray-400">—</span>;
    },
  },
  {
    id: "detectedOn",
    accessorKey: "detectedOn",
    header: "Detected",
    size: 110,
    cell: (info) => {
      const v = info.getValue() as string | undefined;
      return <span className="text-xs text-gray-500">{v ? new Date(v).toLocaleDateString() : "—"}</span>;
    },
  },
];

interface RiskIncidenceListProps {
  riskId?: string;
}

function Inner({ riskId }: RiskIncidenceListProps) {
  const filter = riskId
    ? { parameters: [{ type: "SimpleFilterParameter", name: "riskId", value: riskId } as FilterParameter] }
    : undefined;

  return (
    <DataTable<RiskIncidence>
      resource="risks/incidences"
      columns={COLUMNS}
      filter={filter}
      defaultPageSize={25}
      emptyMessage="No incidences found."
    />
  );
}

export default function RiskIncidenceList(props: RiskIncidenceListProps) {
  return (
    <QueryClientProvider client={qc}>
      <Inner {...props} />
    </QueryClientProvider>
  );
}

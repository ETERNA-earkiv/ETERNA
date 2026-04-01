/**
 * RiskList — paginated risk register table.
 * Replaces GWT RiskRegister.java.
 * Calls POST /api/v2/risks/find
 */
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import DataTable from "@/components/common/DataTable";
import type { ColumnDef } from "@tanstack/react-table";

const qc = new QueryClient();

interface Risk {
  id: string;
  name: string;
  description?: string;
  category?: string;
  probability?: number;
  impact?: number;
  severity?: number;
  preMitigationProbability?: number;
  preMitigationImpact?: number;
  preMitigationSeverity?: number;
  postMitigationProbability?: number;
  postMitigationImpact?: number;
  postMitigationSeverity?: number;
  mitigationStrategy?: string;
  incidencesCount?: number;
}

function severityBadge(val?: number) {
  if (!val) return <span className="text-xs text-gray-400">—</span>;
  const cls = val >= 15 ? "bg-red-100 text-red-700" : val >= 8 ? "bg-yellow-100 text-yellow-700" : "bg-green-100 text-green-700";
  return <span className={`px-2 py-0.5 rounded text-xs font-medium ${cls}`}>{val}</span>;
}

const COLUMNS: ColumnDef<Risk>[] = [
  {
    id: "name",
    accessorKey: "name",
    header: "Risk",
    cell: (info) => (
      <div>
        <a href={`/planning/risks/${info.row.original.id}`} className="text-blue-600 hover:underline text-sm font-medium">
          {info.getValue() as string}
        </a>
        {info.row.original.category && (
          <p className="text-xs text-gray-400 mt-0.5">{info.row.original.category}</p>
        )}
      </div>
    ),
  },
  {
    id: "preMitigationSeverity",
    accessorKey: "preMitigationSeverity",
    header: "Pre-mitigation",
    size: 120,
    cell: (info) => severityBadge(info.getValue() as number | undefined),
  },
  {
    id: "postMitigationSeverity",
    accessorKey: "postMitigationSeverity",
    header: "Post-mitigation",
    size: 120,
    cell: (info) => severityBadge(info.getValue() as number | undefined),
  },
  {
    id: "incidencesCount",
    accessorKey: "incidencesCount",
    header: "Incidences",
    size: 100,
    cell: (info) => {
      const n = info.getValue() as number | undefined;
      return n ? (
        <a href={`/planning/incidences?riskId=${info.row.original.id}`} className="text-xs text-blue-600 hover:underline">{n}</a>
      ) : <span className="text-xs text-gray-400">0</span>;
    },
  },
];

export default function RiskList() {
  return (
    <QueryClientProvider client={qc}>
      <DataTable<Risk>
        resource="risks"
        columns={COLUMNS}
        defaultPageSize={25}
        emptyMessage="No risks found."
      />
    </QueryClientProvider>
  );
}

/**
 * StatisticsPanel — system metrics dashboard.
 * Replaces GWT Statistics.java.
 * Calls GET /api/v2/statistics
 */
import { useQuery, QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { apiGet } from "@/lib/api/client";

const qc = new QueryClient();

interface Statistics {
  repositoryObjectCount?: number;
  repositoryTotalSize?: number;
  repositoryAIPCount?: number;
  repositoryRepresentationCount?: number;
  repositoryFileCount?: number;
  repositoryDIPCount?: number;
  repositoryTransferredResourceCount?: number;
  repositoryPreservationEventCount?: number;
  repositoryJobCount?: number;
  repositoryRiskCount?: number;
  ingestJobCount?: number;
  ingestJobCountSuccess?: number;
  ingestJobCountFailure?: number;
}

function formatBytes(bytes?: number): string {
  if (!bytes) return "0 B";
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 ** 2) return `${(bytes / 1024).toFixed(1)} KB`;
  if (bytes < 1024 ** 3) return `${(bytes / 1024 ** 2).toFixed(1)} MB`;
  if (bytes < 1024 ** 4) return `${(bytes / 1024 ** 3).toFixed(2)} GB`;
  return `${(bytes / 1024 ** 4).toFixed(2)} TB`;
}

interface StatCardProps {
  label: string;
  value: string | number | undefined;
  sub?: string;
}

function StatCard({ label, value, sub }: StatCardProps) {
  return (
    <div className="bg-white rounded-lg border border-gray-200 p-4 shadow-sm">
      <p className="text-xs font-medium text-gray-400 uppercase tracking-wide mb-1">{label}</p>
      <p className="text-2xl font-bold text-gray-800">{value ?? "—"}</p>
      {sub && <p className="text-xs text-gray-400 mt-0.5">{sub}</p>}
    </div>
  );
}

function Inner() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ["statistics"],
    queryFn: () => apiGet<Statistics>("/statistics"),
    staleTime: 60_000,
  });

  if (isLoading) return <div className="py-8 text-center text-gray-400">Loading statistics…</div>;
  if (isError) return <div className="py-8 text-center text-red-500">Failed to load statistics.</div>;

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3">Repository</h2>
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
          <StatCard label="Intellectual entities" value={data?.repositoryAIPCount?.toLocaleString()} />
          <StatCard label="Representations" value={data?.repositoryRepresentationCount?.toLocaleString()} />
          <StatCard label="Files" value={data?.repositoryFileCount?.toLocaleString()} />
          <StatCard label="Total size" value={formatBytes(data?.repositoryTotalSize)} />
          <StatCard label="DIPs" value={data?.repositoryDIPCount?.toLocaleString()} />
          <StatCard label="Transfer resources" value={data?.repositoryTransferredResourceCount?.toLocaleString()} />
          <StatCard label="Preservation events" value={data?.repositoryPreservationEventCount?.toLocaleString()} />
          <StatCard label="Risks" value={data?.repositoryRiskCount?.toLocaleString()} />
        </div>
      </div>
      <div>
        <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3">Ingest jobs</h2>
        <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
          <StatCard label="Total" value={data?.ingestJobCount?.toLocaleString()} />
          <StatCard label="Successful" value={data?.ingestJobCountSuccess?.toLocaleString()} />
          <StatCard label="Failed" value={data?.ingestJobCountFailure?.toLocaleString()} />
        </div>
      </div>
    </div>
  );
}

export default function StatisticsPanel() {
  return (
    <QueryClientProvider client={qc}>
      <Inner />
    </QueryClientProvider>
  );
}

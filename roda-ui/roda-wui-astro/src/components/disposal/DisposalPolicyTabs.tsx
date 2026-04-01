/**
 * DisposalPolicyTabs — tabbed view of schedules, holds, and rules.
 * Replaces GWT DisposalPolicy.java tabs.
 */
import { useState } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import DataTable from "@/components/common/DataTable";
import type { ColumnDef } from "@tanstack/react-table";

const qc = new QueryClient();

interface DisposalSchedule {
  id: string;
  title?: string;
  description?: string;
  mandate?: string;
  scopeNotes?: string;
  documentIds?: string[];
  actionCode?: string;
  period?: string;
  periodUnit?: string;
  state?: string;
}

interface DisposalHold {
  id: string;
  title?: string;
  description?: string;
  mandate?: string;
  scopeNotes?: string;
  documentIds?: string[];
  createdBy?: string;
  createdOn?: string;
}

interface DisposalRule {
  id: string;
  title?: string;
  aipIds?: string[];
  scheduleId?: string;
  scheduleTitle?: string;
  order?: number;
  condition?: string;
  conditionType?: string;
}

const SCHEDULE_COLUMNS: ColumnDef<DisposalSchedule>[] = [
  {
    id: "title",
    accessorKey: "title",
    header: "Title",
    cell: (info) => (
      <a href={`/disposal/schedules/${info.row.original.id}`} className="text-blue-600 hover:underline text-sm font-medium">
        {(info.getValue() as string) || info.row.original.id}
      </a>
    ),
  },
  {
    id: "actionCode",
    accessorKey: "actionCode",
    header: "Action",
    size: 100,
    cell: (info) => <span className="text-xs text-gray-600">{(info.getValue() as string) || "—"}</span>,
  },
  {
    id: "period",
    header: "Retention period",
    size: 130,
    cell: (info) => {
      const r = info.row.original;
      if (!r.period) return <span className="text-xs text-gray-400">—</span>;
      return <span className="text-xs text-gray-600">{r.period} {r.periodUnit}</span>;
    },
  },
  {
    id: "state",
    accessorKey: "state",
    header: "State",
    size: 90,
    cell: (info) => {
      const s = info.getValue() as string | undefined;
      const cls = s === "ACTIVE" ? "bg-green-100 text-green-700" : "bg-gray-100 text-gray-600";
      return s ? <span className={`px-2 py-0.5 rounded text-xs ${cls}`}>{s}</span> : <span className="text-xs text-gray-400">—</span>;
    },
  },
];

const HOLD_COLUMNS: ColumnDef<DisposalHold>[] = [
  {
    id: "title",
    accessorKey: "title",
    header: "Title",
    cell: (info) => (
      <a href={`/disposal/holds/${info.row.original.id}`} className="text-blue-600 hover:underline text-sm font-medium">
        {(info.getValue() as string) || info.row.original.id}
      </a>
    ),
  },
  {
    id: "description",
    accessorKey: "description",
    header: "Description",
    cell: (info) => <span className="text-xs text-gray-600 truncate block max-w-xs">{(info.getValue() as string) || "—"}</span>,
  },
  {
    id: "createdBy",
    accessorKey: "createdBy",
    header: "Created by",
    size: 120,
    cell: (info) => <span className="text-xs text-gray-500">{(info.getValue() as string) || "—"}</span>,
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

const RULE_COLUMNS: ColumnDef<DisposalRule>[] = [
  {
    id: "order",
    accessorKey: "order",
    header: "#",
    size: 50,
    cell: (info) => <span className="text-xs text-gray-400">{String(info.getValue() ?? "—")}</span>,
  },
  {
    id: "title",
    accessorKey: "title",
    header: "Rule",
    cell: (info) => (
      <a href={`/disposal/rules/${info.row.original.id}`} className="text-blue-600 hover:underline text-sm font-medium">
        {(info.getValue() as string) || info.row.original.id}
      </a>
    ),
  },
  {
    id: "scheduleTitle",
    accessorKey: "scheduleTitle",
    header: "Schedule",
    cell: (info) => {
      const r = info.row.original;
      return r.scheduleId ? (
        <a href={`/disposal/schedules/${r.scheduleId}`} className="text-xs text-blue-600 hover:underline">
          {(info.getValue() as string) || r.scheduleId}
        </a>
      ) : <span className="text-xs text-gray-400">—</span>;
    },
  },
  {
    id: "conditionType",
    accessorKey: "conditionType",
    header: "Condition",
    size: 110,
    cell: (info) => <span className="text-xs text-gray-600">{(info.getValue() as string) || "—"}</span>,
  },
];

type Tab = "schedules" | "holds" | "rules";

function Inner() {
  const [tab, setTab] = useState<Tab>("schedules");
  const tabs: { id: Tab; label: string }[] = [
    { id: "schedules", label: "Schedules" },
    { id: "holds", label: "Holds" },
    { id: "rules", label: "Rules" },
  ];

  return (
    <div className="space-y-4">
      <div className="flex gap-1 border-b border-gray-200">
        {tabs.map((t) => (
          <button
            key={t.id}
            onClick={() => setTab(t.id)}
            className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors -mb-px ${
              tab === t.id ? "border-blue-600 text-blue-600" : "border-transparent text-gray-500 hover:text-gray-700"
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>
      {tab === "schedules" && (
        <DataTable<DisposalSchedule>
          resource="disposal/schedules"
          columns={SCHEDULE_COLUMNS}
          defaultPageSize={25}
          emptyMessage="No disposal schedules found."
        />
      )}
      {tab === "holds" && (
        <DataTable<DisposalHold>
          resource="disposal/holds"
          columns={HOLD_COLUMNS}
          defaultPageSize={25}
          emptyMessage="No disposal holds found."
        />
      )}
      {tab === "rules" && (
        <DataTable<DisposalRule>
          resource="disposal/rules"
          columns={RULE_COLUMNS}
          defaultPageSize={25}
          emptyMessage="No disposal rules found."
        />
      )}
    </div>
  );
}

export default function DisposalPolicyTabs() {
  return (
    <QueryClientProvider client={qc}>
      <Inner />
    </QueryClientProvider>
  );
}

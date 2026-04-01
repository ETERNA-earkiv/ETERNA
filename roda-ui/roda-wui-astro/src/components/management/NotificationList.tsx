/**
 * NotificationList — user notifications.
 * Replaces GWT NotificationList.java.
 * Calls POST /api/v2/notifications/find
 */
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import DataTable from "@/components/common/DataTable";
import type { ColumnDef } from "@tanstack/react-table";

const qc = new QueryClient();

interface Notification {
  id: string;
  subject?: string;
  body?: string;
  fromUser?: string;
  toUser?: string;
  sentOn?: string;
  isAcknowledged?: boolean;
  acknowledgedOn?: string;
  state?: string;
}

const COLUMNS: ColumnDef<Notification>[] = [
  {
    id: "subject",
    accessorKey: "subject",
    header: "Subject",
    cell: (info) => (
      <span className="text-sm text-gray-800">{(info.getValue() as string) || "—"}</span>
    ),
  },
  {
    id: "fromUser",
    accessorKey: "fromUser",
    header: "From",
    size: 120,
    cell: (info) => <span className="text-xs text-gray-600">{(info.getValue() as string) || "—"}</span>,
  },
  {
    id: "sentOn",
    accessorKey: "sentOn",
    header: "Sent",
    size: 140,
    cell: (info) => {
      const v = info.getValue() as string | undefined;
      return <span className="text-xs text-gray-500">{v ? new Date(v).toLocaleString() : "—"}</span>;
    },
  },
  {
    id: "isAcknowledged",
    accessorKey: "isAcknowledged",
    header: "Acknowledged",
    size: 120,
    cell: (info) => {
      const v = info.getValue() as boolean | undefined;
      return v
        ? <span className="px-2 py-0.5 bg-green-100 text-green-700 rounded text-xs">Yes</span>
        : <span className="px-2 py-0.5 bg-yellow-100 text-yellow-700 rounded text-xs">No</span>;
    },
  },
];

export default function NotificationList() {
  return (
    <QueryClientProvider client={qc}>
      <DataTable<Notification>
        resource="notifications"
        columns={COLUMNS}
        defaultPageSize={25}
        emptyMessage="No notifications found."
      />
    </QueryClientProvider>
  );
}

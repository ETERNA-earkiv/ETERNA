/**
 * MemberList — paginated user/group table.
 * Replaces GWT UserList.java / GroupList.java.
 * Calls POST /api/v2/members/{users|groups}/find
 */
import { useState } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import DataTable from "@/components/common/DataTable";
import type { ColumnDef } from "@tanstack/react-table";

const qc = new QueryClient();

interface RodaUser {
  id: string;
  name: string;
  fullname?: string;
  email?: string;
  active?: boolean;
  isGuest?: boolean;
  groups?: string[];
}

interface RodaGroup {
  id: string;
  name: string;
  fullname?: string;
  users?: string[];
}

const USER_COLUMNS: ColumnDef<RodaUser>[] = [
  {
    id: "name",
    accessorKey: "name",
    header: "Username",
    cell: (info) => (
      <a href={`/management/members/users/${info.row.original.id}`} className="text-blue-600 hover:underline text-sm font-medium">
        {info.getValue() as string}
      </a>
    ),
  },
  {
    id: "fullname",
    accessorKey: "fullname",
    header: "Full name",
    cell: (info) => <span className="text-sm text-gray-700">{(info.getValue() as string) || "—"}</span>,
  },
  {
    id: "email",
    accessorKey: "email",
    header: "Email",
    cell: (info) => <span className="text-sm text-gray-600">{(info.getValue() as string) || "—"}</span>,
  },
  {
    id: "active",
    accessorKey: "active",
    header: "Status",
    size: 100,
    cell: (info) => {
      const active = info.getValue() as boolean;
      return (
        <span className={`px-2 py-0.5 rounded text-xs font-medium ${active ? "bg-green-100 text-green-700" : "bg-gray-100 text-gray-500"}`}>
          {active ? "Active" : "Inactive"}
        </span>
      );
    },
  },
  {
    id: "actions",
    header: "",
    size: 80,
    cell: (info) => (
      <a href={`/management/members/users/${info.row.original.id}`} className="text-xs text-blue-600 hover:underline">
        Edit
      </a>
    ),
  },
];

const GROUP_COLUMNS: ColumnDef<RodaGroup>[] = [
  {
    id: "name",
    accessorKey: "name",
    header: "Group name",
    cell: (info) => (
      <a href={`/management/members/groups/${info.row.original.id}`} className="text-blue-600 hover:underline text-sm font-medium">
        {info.getValue() as string}
      </a>
    ),
  },
  {
    id: "fullname",
    accessorKey: "fullname",
    header: "Description",
    cell: (info) => <span className="text-sm text-gray-600">{(info.getValue() as string) || "—"}</span>,
  },
  {
    id: "userCount",
    header: "Members",
    size: 100,
    cell: (info) => {
      const users = info.row.original.users ?? [];
      return <span className="text-xs text-gray-600">{users.length}</span>;
    },
  },
  {
    id: "actions",
    header: "",
    size: 80,
    cell: (info) => (
      <a href={`/management/members/groups/${info.row.original.id}`} className="text-xs text-blue-600 hover:underline">
        Edit
      </a>
    ),
  },
];

type Tab = "users" | "groups";

function Inner() {
  const [tab, setTab] = useState<Tab>("users");

  return (
    <div className="space-y-4">
      {/* Tabs */}
      <div className="flex gap-1 border-b border-gray-200">
        {(["users", "groups"] as Tab[]).map((t) => (
          <button
            key={t}
            onClick={() => setTab(t)}
            className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors -mb-px ${
              tab === t ? "border-blue-600 text-blue-600" : "border-transparent text-gray-500 hover:text-gray-700"
            }`}
          >
            {t === "users" ? "Users" : "Groups"}
          </button>
        ))}
      </div>

      {tab === "users" ? (
        <DataTable<RodaUser>
          resource="members/users"
          columns={USER_COLUMNS}
          defaultPageSize={25}
          emptyMessage="No users found."
        />
      ) : (
        <DataTable<RodaGroup>
          resource="members/groups"
          columns={GROUP_COLUMNS}
          defaultPageSize={25}
          emptyMessage="No groups found."
        />
      )}
    </div>
  );
}

export default function MemberList() {
  return (
    <QueryClientProvider client={qc}>
      <Inner />
    </QueryClientProvider>
  );
}

/**
 * UserForm — create/edit a RODA user.
 * Replaces GWT EditUser.java.
 * POST /api/v2/members/users  (create)
 * PUT  /api/v2/members/users/{id} (update)
 */
import { useState, useEffect } from "react";
import { useQuery, useMutation, QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { apiGet, apiPost, apiPut } from "@/lib/api/client";

const qc = new QueryClient();

interface RodaUser {
  id: string;
  name: string;
  fullName?: string;
  email?: string;
  active?: boolean;
  guest?: boolean;
  groups?: string[];
  allRoles?: string[];
}

interface RodaGroup {
  id: string;
  name: string;
}

interface UserFormProps {
  userId?: string;
  /** Called after successful save */
  onSuccess?: (userId: string) => void;
}

function Inner({ userId, onSuccess }: UserFormProps) {
  const isEdit = !!userId;

  const [form, setForm] = useState({
    name: "",
    fullName: "",
    email: "",
    password: "",
    active: true,
    groups: [] as string[],
  });
  const [error, setError] = useState<string | null>(null);

  const { data: user } = useQuery({
    queryKey: ["user", userId],
    queryFn: () => apiGet<RodaUser>(`/members/users/${userId}`),
    enabled: isEdit,
  });

  const { data: allGroups } = useQuery({
    queryKey: ["groups-all"],
    queryFn: () => apiPost<{ results: RodaGroup[] }>("/members/find", {
      filter: { parameters: [{ type: "SimpleFilterParameter", name: "isUser", value: "false" }] },
      onlyActive: true,
      sublist: { firstElementIndex: 0, maximumElementCount: 200 },
    }).then((r) => r.results),
  });

  useEffect(() => {
    if (user) {
      setForm({
        name: user.name,
        fullName: user.fullName ?? "",
        email: user.email ?? "",
        password: "",
        active: user.active ?? true,
        groups: user.groups ?? [],
      });
    }
  }, [user]);

  const saveMutation = useMutation({
    mutationFn: (data: typeof form) => {
      const body = { ...data, password: data.password || undefined };
      if (isEdit) return apiPut<RodaUser>("/members/users", { ...body, id: userId, name: form.name });
      return apiPost<RodaUser>("/members/users", body);
    },
    onSuccess: (saved) => {
      onSuccess?.(saved.id);
      window.location.href = `/management/members/users/${saved.id}`;
    },
    onError: (err: unknown) => setError(err instanceof Error ? err.message : "Save failed"),
  });

  function toggleGroup(groupId: string) {
    setForm((prev) => ({
      ...prev,
      groups: prev.groups.includes(groupId)
        ? prev.groups.filter((g) => g !== groupId)
        : [...prev.groups, groupId],
    }));
  }

  function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError(null);
    saveMutation.mutate(form);
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-6 max-w-lg">
      <div className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Username *</label>
          <input
            required
            disabled={isEdit}
            value={form.name}
            onChange={(e) => setForm((p) => ({ ...p, name: e.target.value }))}
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-50 disabled:text-gray-500"
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Full name</label>
          <input
            value={form.fullName}
            onChange={(e) => setForm((p) => ({ ...p, fullName: e.target.value }))}
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
          <input
            type="email"
            value={form.email}
            onChange={(e) => setForm((p) => ({ ...p, email: e.target.value }))}
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Password {isEdit && <span className="text-gray-400 font-normal">(leave blank to keep unchanged)</span>}
          </label>
          <input
            type="password"
            required={!isEdit}
            value={form.password}
            onChange={(e) => setForm((p) => ({ ...p, password: e.target.value }))}
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>
        <div className="flex items-center gap-2">
          <input
            type="checkbox"
            id="active"
            checked={form.active}
            onChange={(e) => setForm((p) => ({ ...p, active: e.target.checked }))}
            className="rounded border-gray-300"
          />
          <label htmlFor="active" className="text-sm text-gray-700">Active</label>
        </div>
      </div>

      {/* Groups */}
      {(allGroups ?? []).length > 0 && (
        <div>
          <h3 className="text-sm font-medium text-gray-700 mb-2">Groups</h3>
          <div className="border border-gray-200 rounded-lg divide-y divide-gray-100 max-h-48 overflow-y-auto">
            {(allGroups ?? []).map((g) => (
              <label key={g.id} className="flex items-center gap-2 px-3 py-2 hover:bg-gray-50 cursor-pointer">
                <input
                  type="checkbox"
                  checked={form.groups.includes(g.id)}
                  onChange={() => toggleGroup(g.id)}
                  className="rounded border-gray-300"
                />
                <span className="text-sm text-gray-700">{g.name}</span>
              </label>
            ))}
          </div>
        </div>
      )}

      {error && <p className="text-sm text-red-600">{error}</p>}

      <div className="flex gap-3">
        <button
          type="submit"
          disabled={saveMutation.isPending}
          className="px-4 py-2 bg-blue-600 text-white text-sm rounded hover:bg-blue-700 disabled:opacity-40 transition-colors"
        >
          {saveMutation.isPending ? "Saving…" : isEdit ? "Save changes" : "Create user"}
        </button>
        <a href="/management/members" className="px-4 py-2 border border-gray-300 text-sm rounded hover:bg-gray-50 transition-colors">
          Cancel
        </a>
      </div>
    </form>
  );
}

export default function UserForm(props: UserFormProps) {
  return (
    <QueryClientProvider client={qc}>
      <Inner {...props} />
    </QueryClientProvider>
  );
}

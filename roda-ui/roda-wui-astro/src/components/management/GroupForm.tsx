/**
 * GroupForm — create/edit a RODA group.
 * Replaces GWT EditGroup.java.
 * POST /api/v2/members/groups   (create)
 * PUT  /api/v2/members/groups/{id} (update)
 */
import { useState, useEffect } from "react";
import { useQuery, useMutation, QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { apiGet, apiPost, apiPut } from "@/lib/api/client";

const qc = new QueryClient();

interface RodaGroup {
  id: string;
  name: string;
  fullname?: string;
  roles?: string[];
  users?: string[];
}

interface GroupFormProps {
  groupId?: string;
  onSuccess?: (groupId: string) => void;
}

function Inner({ groupId, onSuccess }: GroupFormProps) {
  const isEdit = !!groupId;

  const [form, setForm] = useState({ name: "", fullname: "" });
  const [error, setError] = useState<string | null>(null);

  const { data: group } = useQuery({
    queryKey: ["group", groupId],
    queryFn: () => apiGet<RodaGroup>(`/members/groups/${groupId}`),
    enabled: isEdit,
  });

  useEffect(() => {
    if (group) setForm({ name: group.name, fullname: group.fullname ?? "" });
  }, [group]);

  const saveMutation = useMutation({
    mutationFn: (data: typeof form) => {
      if (isEdit) return apiPut<RodaGroup>(`/members/groups/${groupId}`, data);
      return apiPost<RodaGroup>("/members/groups", data);
    },
    onSuccess: (saved) => {
      onSuccess?.(saved.id);
      window.location.href = `/management/members/groups/${saved.id}`;
    },
    onError: (err: unknown) => setError(err instanceof Error ? err.message : "Save failed"),
  });

  function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError(null);
    saveMutation.mutate(form);
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4 max-w-lg">
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">Group name *</label>
        <input
          required
          disabled={isEdit}
          value={form.name}
          onChange={(e) => setForm((p) => ({ ...p, name: e.target.value }))}
          className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-50"
        />
      </div>
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
        <input
          value={form.fullname}
          onChange={(e) => setForm((p) => ({ ...p, fullname: e.target.value }))}
          className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
      </div>
      {error && <p className="text-sm text-red-600">{error}</p>}
      <div className="flex gap-3">
        <button
          type="submit"
          disabled={saveMutation.isPending}
          className="px-4 py-2 bg-blue-600 text-white text-sm rounded hover:bg-blue-700 disabled:opacity-40 transition-colors"
        >
          {saveMutation.isPending ? "Saving…" : isEdit ? "Save changes" : "Create group"}
        </button>
        <a href="/management/members" className="px-4 py-2 border border-gray-300 text-sm rounded hover:bg-gray-50 transition-colors">
          Cancel
        </a>
      </div>
    </form>
  );
}

export default function GroupForm(props: GroupFormProps) {
  return (
    <QueryClientProvider client={qc}>
      <Inner {...props} />
    </QueryClientProvider>
  );
}

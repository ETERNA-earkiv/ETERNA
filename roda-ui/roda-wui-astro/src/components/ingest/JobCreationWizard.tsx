/**
 * JobCreationWizard — multi-step ingest job creation.
 * Replaces GWT CreateIngestJobWizard.java.
 *
 * Steps:
 *  1. Select plugin — GET /api/v2/configurations/plugins?type=INGEST
 *  2. Configure parameters — plugin-specific form fields
 *  3. Confirm & submit — POST /api/v2/jobs
 */
import { useState, useEffect } from "react";
import { QueryClient, QueryClientProvider, useQuery, useMutation } from "@tanstack/react-query";
import { apiGet, apiPost } from "@/lib/api/client";

const qc = new QueryClient();

interface PluginInfo {
  id: string;
  name?: string;
  description?: string;
  version?: string;
  type?: string;
  parameters?: PluginParameter[];
}

interface PluginParameter {
  name: string;
  defaultValue?: string;
  description?: string;
  type?: string;
  mandatory?: boolean;
  possibleValues?: string[];
}

interface JobCreationWizardProps {
  /** Pre-selected transferred resource IDs to ingest */
  selectedIds?: string[];
  /** Called after job is successfully created */
  onSuccess?: (jobId: string) => void;
}

type Step = "plugin" | "params" | "confirm";

function Inner({ selectedIds = [], onSuccess }: JobCreationWizardProps) {
  const [step, setStep] = useState<Step>("plugin");
  const [selectedPlugin, setSelectedPlugin] = useState<PluginInfo | null>(null);
  const [paramValues, setParamValues] = useState<Record<string, string>>({});
  const [error, setError] = useState<string | null>(null);

  const { data: plugins, isLoading: loadingPlugins, isError: pluginsError } = useQuery({
    queryKey: ["plugins", "INGEST"],
    queryFn: () => apiGet<PluginInfo[]>("/configurations/plugins?type=INGEST"),
  });

  useEffect(() => {
    if (selectedPlugin?.parameters) {
      const defaults: Record<string, string> = {};
      for (const p of selectedPlugin.parameters) {
        defaults[p.name] = p.defaultValue ?? "";
      }
      setParamValues(defaults);
    }
  }, [selectedPlugin]);

  const createMutation = useMutation({
    mutationFn: (body: unknown) => apiPost<{ id: string }>("/jobs", body),
    onSuccess: (job) => {
      onSuccess?.(job.id);
      window.location.href = `/ingest/process/${job.id}`;
    },
    onError: (err: unknown) => {
      setError(err instanceof Error ? err.message : "Failed to create job");
    },
  });

  function handleSubmit() {
    if (!selectedPlugin) return;
    setError(null);
    const pluginParams = Object.entries(paramValues).map(([name, value]) => ({ name, value }));
    createMutation.mutate({
      pluginId: selectedPlugin.id,
      pluginParameters: pluginParams,
      sourceObjects: selectedIds.map((id) => ({ objectClass: "TransferredResource", objectUUID: id })),
    });
  }

  return (
    <div className="space-y-6">
      {/* Step indicator */}
      <div className="flex items-center gap-0">
        {(["plugin", "params", "confirm"] as Step[]).map((s, i) => {
          const labels: Record<Step, string> = { plugin: "Select plugin", params: "Configure", confirm: "Confirm" };
          const active = step === s;
          const done = (step === "params" && s === "plugin") || (step === "confirm" && s !== "confirm");
          return (
            <div key={s} className="flex items-center">
              {i > 0 && <div className={`h-px w-8 ${done ? "bg-blue-500" : "bg-gray-200"}`} />}
              <div className={`flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-medium ${
                active ? "bg-blue-600 text-white" : done ? "bg-blue-100 text-blue-700" : "bg-gray-100 text-gray-500"
              }`}>
                <span>{i + 1}</span>
                <span>{labels[s]}</span>
              </div>
            </div>
          );
        })}
      </div>

      {/* Step: Select plugin */}
      {step === "plugin" && (
        <div className="space-y-3">
          <h2 className="font-semibold text-gray-800">Select ingest plugin</h2>
          {loadingPlugins && <p className="text-sm text-gray-400">Loading plugins…</p>}
          {pluginsError && <p className="text-sm text-red-600">Failed to load plugins.</p>}
          <div className="grid gap-2">
            {(plugins ?? []).map((p) => (
              <button
                key={p.id}
                onClick={() => setSelectedPlugin(p)}
                className={`text-left p-4 rounded-lg border transition-colors ${
                  selectedPlugin?.id === p.id
                    ? "border-blue-500 bg-blue-50"
                    : "border-gray-200 hover:border-gray-300 bg-white"
                }`}
              >
                <p className="font-medium text-sm text-gray-800">{p.name ?? p.id}</p>
                {p.description && <p className="text-xs text-gray-500 mt-1">{p.description}</p>}
                {p.version && <p className="text-xs text-gray-400 mt-1">v{p.version}</p>}
              </button>
            ))}
          </div>
          <div className="flex justify-end">
            <button
              disabled={!selectedPlugin}
              onClick={() => setStep("params")}
              className="px-4 py-2 bg-blue-600 text-white text-sm rounded disabled:opacity-40 hover:bg-blue-700 transition-colors"
            >
              Next →
            </button>
          </div>
        </div>
      )}

      {/* Step: Configure parameters */}
      {step === "params" && selectedPlugin && (
        <div className="space-y-4">
          <h2 className="font-semibold text-gray-800">Configure: {selectedPlugin.name ?? selectedPlugin.id}</h2>
          {(selectedPlugin.parameters ?? []).length === 0 ? (
            <p className="text-sm text-gray-400">No configurable parameters for this plugin.</p>
          ) : (
            <div className="space-y-3">
              {selectedPlugin.parameters!.map((param) => (
                <div key={param.name}>
                  <label className="block text-xs font-medium text-gray-700 mb-1">
                    {param.name}
                    {param.mandatory && <span className="text-red-500 ml-1">*</span>}
                  </label>
                  {param.description && (
                    <p className="text-xs text-gray-400 mb-1">{param.description}</p>
                  )}
                  {param.possibleValues?.length ? (
                    <select
                      value={paramValues[param.name] ?? ""}
                      onChange={(e) => setParamValues((prev) => ({ ...prev, [param.name]: e.target.value }))}
                      className="w-full border border-gray-300 rounded px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                    >
                      <option value="">— Select —</option>
                      {param.possibleValues.map((v) => (
                        <option key={v} value={v}>{v}</option>
                      ))}
                    </select>
                  ) : (
                    <input
                      type="text"
                      value={paramValues[param.name] ?? ""}
                      onChange={(e) => setParamValues((prev) => ({ ...prev, [param.name]: e.target.value }))}
                      className="w-full border border-gray-300 rounded px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                    />
                  )}
                </div>
              ))}
            </div>
          )}
          <div className="flex justify-between">
            <button onClick={() => setStep("plugin")} className="px-4 py-2 text-sm border border-gray-300 rounded hover:bg-gray-50 transition-colors">
              ← Back
            </button>
            <button
              onClick={() => setStep("confirm")}
              className="px-4 py-2 bg-blue-600 text-white text-sm rounded hover:bg-blue-700 transition-colors"
            >
              Next →
            </button>
          </div>
        </div>
      )}

      {/* Step: Confirm */}
      {step === "confirm" && selectedPlugin && (
        <div className="space-y-4">
          <h2 className="font-semibold text-gray-800">Confirm job creation</h2>
          <div className="bg-gray-50 rounded-lg p-4 text-sm space-y-2">
            <div className="flex gap-2">
              <span className="text-gray-500 w-24 shrink-0">Plugin:</span>
              <span className="text-gray-800 font-medium">{selectedPlugin.name ?? selectedPlugin.id}</span>
            </div>
            <div className="flex gap-2">
              <span className="text-gray-500 w-24 shrink-0">Objects:</span>
              <span className="text-gray-800">{selectedIds.length} selected</span>
            </div>
            {Object.entries(paramValues).filter(([, v]) => v).map(([k, v]) => (
              <div key={k} className="flex gap-2">
                <span className="text-gray-500 w-24 shrink-0 truncate">{k}:</span>
                <span className="text-gray-800 truncate">{v}</span>
              </div>
            ))}
          </div>
          {error && <p className="text-sm text-red-600">{error}</p>}
          <div className="flex justify-between">
            <button onClick={() => setStep("params")} className="px-4 py-2 text-sm border border-gray-300 rounded hover:bg-gray-50 transition-colors">
              ← Back
            </button>
            <button
              disabled={createMutation.isPending}
              onClick={handleSubmit}
              className="px-4 py-2 bg-green-600 text-white text-sm rounded disabled:opacity-40 hover:bg-green-700 transition-colors"
            >
              {createMutation.isPending ? "Creating…" : "Create job"}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

export default function JobCreationWizard(props: JobCreationWizardProps) {
  return (
    <QueryClientProvider client={qc}>
      <Inner {...props} />
    </QueryClientProvider>
  );
}

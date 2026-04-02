/**
 * SearchPanel — full-text + faceted search island.
 * Replaces GWT SearchWrapper.java + CatalogueSearch.java.
 *
 * Supports searching across multiple resource types via POST /api/v2/{resource}/find.
 */
import { useState, useCallback } from "react";
import { useQuery } from "@tanstack/react-query";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { apiFindRequest, type FindRequest, type FacetParameter, type FacetResult, type FacetValue } from "@/lib/api/client";

const qc = new QueryClient({ defaultOptions: { queries: { retry: 1 } } });

export type SearchResourceType = "aips" | "representations" | "files" | "dips";

export interface SearchResultItem {
  id: string;
  title?: string;
  type?: string;
  level?: string;
  state?: string;
  format?: string;
  size?: number;
  dateInitial?: string;
  dateFinal?: string;
  aipId?: string;
  representationId?: string;
  representationUUID?: string;
  label?: string;
  [key: string]: unknown;
}

interface ActiveFacet {
  field: string;
  value: string;
  label: string;
}

interface SearchPanelProps {
  defaultResource?: SearchResourceType;
  showResourceToggle?: boolean;
}

const RESOURCE_LABELS: Record<SearchResourceType, string> = {
  aips: "Intellectual entities",
  representations: "Representations",
  files: "Files",
  dips: "Disseminations",
};

const FACET_FIELDS: Record<SearchResourceType, string[]> = {
  aips: ["level", "hasRepresentations"],
  representations: ["type", "representationStates", "original"],
  files: ["fileFormat", "formatPronom"],
  dips: [],
};

const FACET_LABELS: Record<string, string> = {
  level: "Level",
  hasRepresentations: "Has representations",
  type: "Type",
  representationStates: "States",
  original: "Original",
  fileFormat: "Format",
  formatPronom: "PRONOM",
};

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
  return `${(bytes / 1024 / 1024 / 1024).toFixed(2)} GB`;
}

function getItemHref(item: SearchResultItem, resource: SearchResourceType): string {
  switch (resource) {
    case "aips": return `/browse/${item.id}`;
    case "representations":
      return item.aipId
        ? `/browse/${item.aipId}/representation/${item.id}`
        : `/browse/representation/${item.id}`;
    case "files":
      return `/browse/file/${item.id}`;
    case "dips": return `/browse/dip/${item.id}`;
    default: return "#";
  }
}

function Inner({ defaultResource = "aips", showResourceToggle = true }: SearchPanelProps) {
  const [query, setQuery] = useState("");
  const [submittedQuery, setSubmittedQuery] = useState("");
  const [resource, setResource] = useState<SearchResourceType>(defaultResource);
  const [activeFacets, setActiveFacets] = useState<ActiveFacet[]>([]);
  const [page, setPage] = useState(0);
  const PAGE_SIZE = 25;

  const buildFindRequest = useCallback((): FindRequest => {
    const filterParameters: FindRequest["filter"]["parameters"] = [];

    if (submittedQuery.trim()) {
      filterParameters.push({
        type: "BasicSearchFilterParameter",
        value: submittedQuery.trim(),
      });
    } else {
      filterParameters.push({ type: "AllFilterParameter" });
    }

    for (const facet of activeFacets) {
      filterParameters.push({
        type: "SimpleFilterParameter",
        name: facet.field,
        value: facet.value,
      });
    }

    const facetFields = FACET_FIELDS[resource] ?? [];
    const facetsMap: Record<string, import("@/lib/api/client").FacetParameter> = {};
    for (const f of facetFields) {
      facetsMap[f] = { type: "SimpleFacetParameter", name: f, limit: 10, minCount: 1 };
    }

    return {
      filter: { parameters: filterParameters },
      onlyActive: true,
      sublist: {
        firstElementIndex: page * PAGE_SIZE,
        maximumElementCount: PAGE_SIZE,
      },
      facets: facetFields.length ? { parameters: facetsMap } : undefined,
    };
  }, [submittedQuery, activeFacets, resource, page]);

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ["search", resource, submittedQuery, activeFacets, page],
    queryFn: () => apiFindRequest<SearchResultItem>(resource, buildFindRequest()),
    enabled: true,
  });

  function handleSearch(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setSubmittedQuery(query);
    setPage(0);
    setActiveFacets([]);
  }

  function toggleFacet(field: string, value: string, label: string) {
    setActiveFacets((prev) => {
      const exists = prev.find((f) => f.field === field && f.value === value);
      if (exists) return prev.filter((f) => !(f.field === field && f.value === value));
      return [...prev, { field, value, label }];
    });
    setPage(0);
  }

  function isFacetActive(field: string, value: string) {
    return activeFacets.some((f) => f.field === field && f.value === value);
  }

  const totalPages = Math.ceil((data?.totalCount ?? 0) / PAGE_SIZE);

  return (
    <div className="space-y-4">
      {/* Search bar */}
      <form onSubmit={handleSearch} className="flex gap-2">
        <input
          type="search"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Search…"
          className="flex-1 border border-gray-300 rounded-lg px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
        <button
          type="submit"
          className="px-6 py-2 bg-[var(--color-primary)] text-white rounded-lg text-sm font-medium hover:bg-[var(--color-primary-dark)] transition-colors"
        >
          Search
        </button>
      </form>

      {/* Resource toggle */}
      {showResourceToggle && (
        <div className="flex gap-2 flex-wrap">
          {(Object.keys(RESOURCE_LABELS) as SearchResourceType[]).map((r) => (
            <button
              key={r}
              onClick={() => { setResource(r); setPage(0); setActiveFacets([]); }}
              className={`px-3 py-1 text-xs rounded-full border transition-colors ${
                resource === r
                  ? "bg-[var(--color-primary)] text-white border-transparent"
                  : "bg-white text-gray-600 border-gray-300 hover:border-gray-400"
              }`}
            >
              {RESOURCE_LABELS[r]}
            </button>
          ))}
        </div>
      )}

      {/* Active facets */}
      {activeFacets.length > 0 && (
        <div className="flex flex-wrap gap-2">
          {activeFacets.map((f) => (
            <button
              key={`${f.field}-${f.value}`}
              onClick={() => toggleFacet(f.field, f.value, f.label)}
              className="flex items-center gap-1 px-2 py-1 bg-blue-100 text-blue-700 rounded text-xs hover:bg-blue-200 transition-colors"
            >
              <span>{FACET_LABELS[f.field] ?? f.field}: {f.label}</span>
              <span>×</span>
            </button>
          ))}
          <button
            onClick={() => { setActiveFacets([]); setPage(0); }}
            className="text-xs text-gray-400 hover:text-gray-600 underline"
          >
            Clear all
          </button>
        </div>
      )}

      <div className="flex gap-6">
        {/* Facets sidebar */}
        {(data?.facetResults?.length ?? 0) > 0 && (
          <aside className="w-52 shrink-0 space-y-4">
            {data!.facetResults!.map((fr: FacetResult) => (
              <div key={fr.field} className="bg-white border border-gray-200 rounded-lg p-3 shadow-sm">
                <h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-2">
                  {FACET_LABELS[fr.field] ?? fr.field}
                </h3>
                <ul className="space-y-1">
                  {fr.values.map((v: FacetValue) => (
                    <li key={v.value}>
                      <button
                        onClick={() => toggleFacet(fr.field, v.value, v.label)}
                        className={`w-full text-left flex justify-between items-center text-xs px-1 py-0.5 rounded hover:bg-gray-50 ${
                          isFacetActive(fr.field, v.value) ? "text-blue-700 font-medium" : "text-gray-700"
                        }`}
                      >
                        <span className="truncate">{v.label || v.value}</span>
                        <span className="ml-2 text-gray-400 shrink-0">({v.count})</span>
                      </button>
                    </li>
                  ))}
                </ul>
              </div>
            ))}
          </aside>
        )}

        {/* Results */}
        <div className="flex-1 min-w-0 space-y-3">
          {/* Results header */}
          <div className="flex items-center justify-between text-sm text-gray-500">
            {isLoading ? (
              <span>Searching…</span>
            ) : isError ? (
              <span className="text-red-600">Search failed: {error instanceof Error ? error.message : String(error)}</span>
            ) : (
              <span>
                {data?.totalCount ?? 0} result{(data?.totalCount ?? 0) !== 1 ? "s" : ""}
                {submittedQuery && ` for "${submittedQuery}"`}
              </span>
            )}
          </div>

          {/* Result list */}
          {isLoading ? (
            <div className="flex justify-center py-8"><span className="spinner" /></div>
          ) : (
            <ul className="divide-y divide-gray-100 bg-white rounded-lg border border-gray-200 shadow-sm overflow-hidden">
              {(data?.results ?? []).length === 0 ? (
                <li className="px-4 py-8 text-center text-gray-400 text-sm">
                  No results found.
                </li>
              ) : (
                (data?.results ?? []).map((item) => (
                  <li key={item.id} className="px-4 py-3 hover:bg-blue-50/30 transition-colors">
                    <a href={getItemHref(item, resource)} className="block">
                      <div className="flex items-start justify-between gap-2">
                        <div className="min-w-0">
                          <p className="font-medium text-blue-700 hover:underline truncate text-sm">
                            {item.title ?? item.label ?? item.id}
                          </p>
                          <p className="text-xs text-gray-400 mt-0.5 font-mono">{item.id}</p>
                          <div className="flex flex-wrap gap-2 mt-1">
                            {item.level && (
                              <span className="px-1.5 py-0.5 bg-gray-100 rounded text-xs text-gray-600">{item.level}</span>
                            )}
                            {item.type && (
                              <span className="px-1.5 py-0.5 bg-purple-50 rounded text-xs text-purple-600">{item.type}</span>
                            )}
                            {item.state && (
                              <span className={`px-1.5 py-0.5 rounded text-xs ${item.state === "ACTIVE" ? "bg-green-100 text-green-700" : "bg-gray-100 text-gray-600"}`}>
                                {item.state}
                              </span>
                            )}
                            {item.size != null && (
                              <span className="text-xs text-gray-400">{formatBytes(item.size)}</span>
                            )}
                          </div>
                        </div>
                        {(item.dateInitial || item.dateFinal) && (
                          <div className="text-xs text-gray-400 shrink-0 text-right">
                            {item.dateInitial && <div>{new Date(item.dateInitial).toLocaleDateString()}</div>}
                            {item.dateFinal && item.dateFinal !== item.dateInitial && (
                              <div>— {new Date(item.dateFinal).toLocaleDateString()}</div>
                            )}
                          </div>
                        )}
                      </div>
                    </a>
                  </li>
                ))
              )}
            </ul>
          )}

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex items-center justify-between text-sm text-gray-600">
              <span>
                {page * PAGE_SIZE + 1}–{Math.min((page + 1) * PAGE_SIZE, data?.totalCount ?? 0)} of {data?.totalCount ?? 0}
              </span>
              <div className="flex gap-2">
                <button disabled={page === 0} onClick={() => setPage(0)} className="px-2 py-1 rounded border border-gray-300 disabled:opacity-40 hover:bg-gray-50">«</button>
                <button disabled={page === 0} onClick={() => setPage((p) => p - 1)} className="px-2 py-1 rounded border border-gray-300 disabled:opacity-40 hover:bg-gray-50">‹</button>
                <span>Page {page + 1} of {totalPages}</span>
                <button disabled={page >= totalPages - 1} onClick={() => setPage((p) => p + 1)} className="px-2 py-1 rounded border border-gray-300 disabled:opacity-40 hover:bg-gray-50">›</button>
                <button disabled={page >= totalPages - 1} onClick={() => setPage(totalPages - 1)} className="px-2 py-1 rounded border border-gray-300 disabled:opacity-40 hover:bg-gray-50">»</button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default function SearchPanel(props: SearchPanelProps) {
  return (
    <QueryClientProvider client={qc}>
      <Inner {...props} />
    </QueryClientProvider>
  );
}

/**
 * DataTable — generic paginated, sortable, selectable table island.
 * Replaces all GWT AsyncTableCell / *List.java implementations.
 *
 * Usage:
 *   <DataTable
 *     resource="aips"
 *     columns={[...]}
 *     defaultPageSize={25}
 *   />
 */
import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import {
  useReactTable,
  getCoreRowModel,
  flexRender,
  type ColumnDef,
  type SortingState,
} from "@tanstack/react-table";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { apiFindRequest, type FindRequest, type IndexResult } from "@/lib/api/client";

const qc = new QueryClient({ defaultOptions: { queries: { retry: 1 } } });

export interface DataTableProps<T> {
  resource: string;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  columns: ColumnDef<T, any>[];
  defaultPageSize?: number;
  filter?: FindRequest["filter"];
  facets?: FindRequest["facets"];
  onSelectionChange?: (rows: T[]) => void;
  emptyMessage?: string;
}

function Inner<T extends object>({
  resource,
  columns,
  defaultPageSize = 25,
  filter,
  facets,
  onSelectionChange,
  emptyMessage = "No items found.",
}: DataTableProps<T>) {
  const [pageIndex, setPageIndex] = useState(0);
  const [pageSize] = useState(defaultPageSize);
  const [sorting, setSorting] = useState<SortingState>([]);
  const [rowSelection, setRowSelection] = useState<Record<string, boolean>>({});

  const findBody: FindRequest = {
    filter,
    sublist: {
      firstElementIndex: pageIndex * pageSize,
      maximumElementCount: pageSize,
    },
    sortParameters: sorting.map((s) => ({ name: s.id, descending: s.desc })),
    facets,
  };

  const { data, isLoading, isError, error } = useQuery<IndexResult<T>>({
    queryKey: [resource, "find", findBody],
    queryFn: () => apiFindRequest<T>(resource, findBody),
  });

  const table = useReactTable<T>({
    data: data?.results ?? [],
    columns,
    state: { sorting, rowSelection },
    manualSorting: true,
    manualPagination: true,
    rowCount: data?.totalCount ?? 0,
    onSortingChange: (updater) => {
      setSorting(updater);
      setPageIndex(0);
    },
    onRowSelectionChange: (updater) => {
      setRowSelection(updater);
      if (onSelectionChange && data?.results) {
        const updated = typeof updater === "function" ? updater(rowSelection) : updater;
        onSelectionChange(
          data.results.filter((_, i) => updated[String(i)]),
        );
      }
    },
    getCoreRowModel: getCoreRowModel(),
  });

  const totalPages = Math.ceil((data?.totalCount ?? 0) / pageSize);

  if (isError) {
    return (
      <div className="p-4 bg-red-50 border border-red-200 rounded text-sm text-red-700">
        Error loading data: {(error as Error).message}
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {/* Table */}
      <div className="overflow-x-auto rounded-lg border border-gray-200 bg-white shadow-sm">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 border-b border-gray-200">
            {table.getHeaderGroups().map((hg) => (
              <tr key={hg.id}>
                {hg.headers.map((header) => (
                  <th
                    key={header.id}
                    className="px-4 py-3 text-left font-medium text-gray-600 whitespace-nowrap"
                    style={{ width: header.getSize() !== 150 ? header.getSize() : undefined }}
                  >
                    {header.isPlaceholder ? null : (
                      <div
                        className={header.column.getCanSort() ? "cursor-pointer select-none flex items-center gap-1" : ""}
                        onClick={header.column.getToggleSortingHandler()}
                      >
                        {flexRender(header.column.columnDef.header, header.getContext())}
                        {header.column.getIsSorted() === "asc" && " ↑"}
                        {header.column.getIsSorted() === "desc" && " ↓"}
                      </div>
                    )}
                  </th>
                ))}
              </tr>
            ))}
          </thead>
          <tbody>
            {isLoading ? (
              <tr>
                <td colSpan={columns.length} className="px-4 py-8 text-center text-gray-400">
                  <div className="flex justify-center"><span className="spinner" /></div>
                </td>
              </tr>
            ) : table.getRowModel().rows.length === 0 ? (
              <tr>
                <td colSpan={columns.length} className="px-4 py-8 text-center text-gray-400">
                  {emptyMessage}
                </td>
              </tr>
            ) : (
              table.getRowModel().rows.map((row) => (
                <tr
                  key={row.id}
                  className={`border-b border-gray-100 last:border-0 hover:bg-blue-50/40 ${row.getIsSelected() ? "bg-blue-50" : ""}`}
                >
                  {row.getVisibleCells().map((cell) => (
                    <td key={cell.id} className="px-4 py-3 text-gray-700">
                      {flexRender(cell.column.columnDef.cell, cell.getContext())}
                    </td>
                  ))}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between text-sm text-gray-600">
          <span>
            {pageIndex * pageSize + 1}–{Math.min((pageIndex + 1) * pageSize, data?.totalCount ?? 0)} of {data?.totalCount ?? 0}
          </span>
          <div className="flex items-center gap-2">
            <button
              disabled={pageIndex === 0}
              onClick={() => setPageIndex(0)}
              className="px-2 py-1 rounded border border-gray-300 disabled:opacity-40 hover:bg-gray-50"
            >
              «
            </button>
            <button
              disabled={pageIndex === 0}
              onClick={() => setPageIndex((p) => p - 1)}
              className="px-2 py-1 rounded border border-gray-300 disabled:opacity-40 hover:bg-gray-50"
            >
              ‹
            </button>
            <span>
              Page {pageIndex + 1} of {totalPages}
            </span>
            <button
              disabled={pageIndex >= totalPages - 1}
              onClick={() => setPageIndex((p) => p + 1)}
              className="px-2 py-1 rounded border border-gray-300 disabled:opacity-40 hover:bg-gray-50"
            >
              ›
            </button>
            <button
              disabled={pageIndex >= totalPages - 1}
              onClick={() => setPageIndex(totalPages - 1)}
              className="px-2 py-1 rounded border border-gray-300 disabled:opacity-40 hover:bg-gray-50"
            >
              »
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

export default function DataTable<T extends object>(props: DataTableProps<T>) {
  return (
    <QueryClientProvider client={qc}>
      <Inner {...props} />
    </QueryClientProvider>
  );
}

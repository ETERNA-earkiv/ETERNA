import { jsx, jsxs } from 'react/jsx-runtime';
import { QueryClient, QueryClientProvider, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useReactTable, getCoreRowModel, flexRender } from '@tanstack/react-table';
import { b as apiFindRequest } from './client_BbTdbxg1.mjs';

const qc$1 = new QueryClient({ defaultOptions: { queries: { retry: 1 } } });
function Inner$1({
  resource,
  columns,
  defaultPageSize = 25,
  filter,
  facets,
  onSelectionChange,
  emptyMessage = "No items found."
}) {
  const [pageIndex, setPageIndex] = useState(0);
  const [pageSize] = useState(defaultPageSize);
  const [sorting, setSorting] = useState([]);
  const [rowSelection, setRowSelection] = useState({});
  const findBody = {
    filter,
    sublist: {
      firstElementIndex: pageIndex * pageSize,
      maximumElementCount: pageSize
    },
    sortParameters: sorting.map((s) => ({ name: s.id, descending: s.desc })),
    facets
  };
  const { data, isLoading, isError, error } = useQuery({
    queryKey: [resource, "find", findBody],
    queryFn: () => apiFindRequest(resource, findBody)
  });
  const table = useReactTable({
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
          data.results.filter((_, i) => updated[String(i)])
        );
      }
    },
    getCoreRowModel: getCoreRowModel()
  });
  const totalPages = Math.ceil((data?.totalCount ?? 0) / pageSize);
  if (isError) {
    return /* @__PURE__ */ jsxs("div", { className: "p-4 bg-red-50 border border-red-200 rounded text-sm text-red-700", children: [
      "Error loading data: ",
      error.message
    ] });
  }
  return /* @__PURE__ */ jsxs("div", { className: "space-y-3", children: [
    /* @__PURE__ */ jsx("div", { className: "overflow-x-auto rounded-lg border border-gray-200 bg-white shadow-sm", children: /* @__PURE__ */ jsxs("table", { className: "w-full text-sm", children: [
      /* @__PURE__ */ jsx("thead", { className: "bg-gray-50 border-b border-gray-200", children: table.getHeaderGroups().map((hg) => /* @__PURE__ */ jsx("tr", { children: hg.headers.map((header) => /* @__PURE__ */ jsx(
        "th",
        {
          className: "px-4 py-3 text-left font-medium text-gray-600 whitespace-nowrap",
          style: { width: header.getSize() !== 150 ? header.getSize() : void 0 },
          children: header.isPlaceholder ? null : /* @__PURE__ */ jsxs(
            "div",
            {
              className: header.column.getCanSort() ? "cursor-pointer select-none flex items-center gap-1" : "",
              onClick: header.column.getToggleSortingHandler(),
              children: [
                flexRender(header.column.columnDef.header, header.getContext()),
                header.column.getIsSorted() === "asc" && " ↑",
                header.column.getIsSorted() === "desc" && " ↓"
              ]
            }
          )
        },
        header.id
      )) }, hg.id)) }),
      /* @__PURE__ */ jsx("tbody", { children: isLoading ? /* @__PURE__ */ jsx("tr", { children: /* @__PURE__ */ jsx("td", { colSpan: columns.length, className: "px-4 py-8 text-center text-gray-400", children: /* @__PURE__ */ jsx("div", { className: "flex justify-center", children: /* @__PURE__ */ jsx("span", { className: "spinner" }) }) }) }) : table.getRowModel().rows.length === 0 ? /* @__PURE__ */ jsx("tr", { children: /* @__PURE__ */ jsx("td", { colSpan: columns.length, className: "px-4 py-8 text-center text-gray-400", children: emptyMessage }) }) : table.getRowModel().rows.map((row) => /* @__PURE__ */ jsx(
        "tr",
        {
          className: `border-b border-gray-100 last:border-0 hover:bg-blue-50/40 ${row.getIsSelected() ? "bg-blue-50" : ""}`,
          children: row.getVisibleCells().map((cell) => /* @__PURE__ */ jsx("td", { className: "px-4 py-3 text-gray-700", children: flexRender(cell.column.columnDef.cell, cell.getContext()) }, cell.id))
        },
        row.id
      )) })
    ] }) }),
    totalPages > 1 && /* @__PURE__ */ jsxs("div", { className: "flex items-center justify-between text-sm text-gray-600", children: [
      /* @__PURE__ */ jsxs("span", { children: [
        pageIndex * pageSize + 1,
        "–",
        Math.min((pageIndex + 1) * pageSize, data?.totalCount ?? 0),
        " of ",
        data?.totalCount ?? 0
      ] }),
      /* @__PURE__ */ jsxs("div", { className: "flex items-center gap-2", children: [
        /* @__PURE__ */ jsx(
          "button",
          {
            disabled: pageIndex === 0,
            onClick: () => setPageIndex(0),
            className: "px-2 py-1 rounded border border-gray-300 disabled:opacity-40 hover:bg-gray-50",
            children: "«"
          }
        ),
        /* @__PURE__ */ jsx(
          "button",
          {
            disabled: pageIndex === 0,
            onClick: () => setPageIndex((p) => p - 1),
            className: "px-2 py-1 rounded border border-gray-300 disabled:opacity-40 hover:bg-gray-50",
            children: "‹"
          }
        ),
        /* @__PURE__ */ jsxs("span", { children: [
          "Page ",
          pageIndex + 1,
          " of ",
          totalPages
        ] }),
        /* @__PURE__ */ jsx(
          "button",
          {
            disabled: pageIndex >= totalPages - 1,
            onClick: () => setPageIndex((p) => p + 1),
            className: "px-2 py-1 rounded border border-gray-300 disabled:opacity-40 hover:bg-gray-50",
            children: "›"
          }
        ),
        /* @__PURE__ */ jsx(
          "button",
          {
            disabled: pageIndex >= totalPages - 1,
            onClick: () => setPageIndex(totalPages - 1),
            className: "px-2 py-1 rounded border border-gray-300 disabled:opacity-40 hover:bg-gray-50",
            children: "»"
          }
        )
      ] })
    ] })
  ] });
}
function DataTable(props) {
  return /* @__PURE__ */ jsx(QueryClientProvider, { client: qc$1, children: /* @__PURE__ */ jsx(Inner$1, { ...props }) });
}

const qc = new QueryClient();
const columns = [
  {
    id: "title",
    accessorKey: "title",
    header: "Title",
    cell: (info) => /* @__PURE__ */ jsx(
      "a",
      {
        href: `/browse/${info.row.original.id}`,
        className: "text-blue-700 hover:underline font-medium",
        children: info.getValue() || "(untitled)"
      }
    )
  },
  {
    id: "level",
    accessorKey: "level",
    header: "Level",
    size: 120,
    cell: (info) => /* @__PURE__ */ jsx("span", { className: "px-2 py-0.5 rounded-full text-xs bg-gray-100 text-gray-600", children: info.getValue() || "—" })
  },
  {
    id: "dateInitial",
    accessorKey: "dateInitial",
    header: "Date (initial)",
    size: 140,
    cell: (info) => {
      const v = info.getValue();
      return v ? new Date(v).toLocaleDateString() : "—";
    }
  },
  {
    id: "dateFinal",
    accessorKey: "dateFinal",
    header: "Date (final)",
    size: 140,
    cell: (info) => {
      const v = info.getValue();
      return v ? new Date(v).toLocaleDateString() : "—";
    }
  },
  {
    id: "state",
    accessorKey: "state",
    header: "State",
    size: 100,
    cell: (info) => {
      const v = info.getValue();
      const color = v === "ACTIVE" ? "bg-green-100 text-green-700" : "bg-gray-100 text-gray-600";
      return v ? /* @__PURE__ */ jsx("span", { className: `px-2 py-0.5 rounded-full text-xs ${color}`, children: v }) : "—";
    }
  }
];
function Inner({ parentId }) {
  const filter = parentId ? {
    parameters: [
      { type: "SimpleFilterParameter", name: "parentId", value: parentId }
    ]
  } : {
    parameters: [
      { type: "EmptyKeyFilterParameter", name: "parentId" }
    ]
  };
  return /* @__PURE__ */ jsx(
    DataTable,
    {
      resource: "aips",
      columns,
      filter,
      defaultPageSize: 25,
      emptyMessage: "No archival information packages found."
    }
  );
}
function AIPList(props) {
  return /* @__PURE__ */ jsx(QueryClientProvider, { client: qc, children: /* @__PURE__ */ jsx(Inner, { ...props }) });
}

export { AIPList as A };

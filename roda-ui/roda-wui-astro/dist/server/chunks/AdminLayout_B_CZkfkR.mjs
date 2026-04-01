import { e as createComponent, m as maybeRenderHead, r as renderTemplate, g as addAttribute, h as createAstro, n as renderHead, k as renderComponent, o as renderSlot } from './astro/server_BTiD7zzO.mjs';
import 'piccolore';
import { jsx, jsxs } from 'react/jsx-runtime';
import { useState } from 'react';
import 'clsx';
/* empty css                           */

const NAV_ITEMS = [
  {
    key: "browse",
    label: "Browse",
    href: "/browse",
    roles: ["dissemination.browse"]
  },
  {
    key: "search",
    label: "Search",
    href: "/search",
    roles: ["dissemination.metadata.read"]
  },
  {
    key: "ingest",
    label: "Ingest",
    children: [
      { key: "ingest_pre", label: "Pre-ingest", href: "/ingest/pre", roles: ["ingest.pre.ingest"] },
      { key: "ingest_transfer", label: "Transfer", href: "/ingest/transfer", roles: ["ingest.submission.upload"] },
      { key: "ingest_process", label: "Process", href: "/ingest/process", roles: ["ingest.list"] },
      { key: "ingest_appraisal", label: "Appraisal", href: "/ingest/appraisal", roles: ["ingest.appraisal.access"] }
    ],
    roles: ["ingest.pre.ingest", "ingest.submission.upload", "ingest.list", "ingest.appraisal.access"]
  },
  {
    key: "disposal",
    label: "Disposal",
    children: [
      { key: "disposal_policy", label: "Policy", href: "/disposal/policy", roles: ["disposal.policy.read"] },
      { key: "disposal_confirmations", label: "Confirmations", href: "/disposal/confirmations", roles: ["disposal.confirmation.read"] },
      { key: "disposal_destroyed", label: "Destroyed records", href: "/disposal/destroyed", roles: ["disposal.confirmation.read"] }
    ],
    roles: ["disposal.policy.read", "disposal.confirmation.read"]
  },
  {
    key: "planning",
    label: "Planning",
    children: [
      { key: "planning_representation_information", label: "Representation information", href: "/planning/representation-information", roles: ["representation.information.manage"] },
      { key: "planning_risk", label: "Risk register", href: "/planning/risks", roles: ["risk.management.read"] },
      { key: "planning_event", label: "Preservation events", href: "/planning/events", roles: ["preservation.events.read"] },
      { key: "planning_agent", label: "Preservation agents", href: "/planning/agents", roles: ["preservation.events.read"] }
    ],
    roles: ["representation.information.manage", "risk.management.read", "preservation.events.read"]
  },
  {
    key: "administration",
    label: "Administration",
    children: [
      { key: "administration_actions", label: "Actions", href: "/management/actions", roles: ["process.run"] },
      { key: "administration_internal_actions", label: "Internal actions", href: "/management/internal", roles: ["process.run.internal"] },
      { key: "administration_user", label: "Users and groups", href: "/management/members", roles: ["user.management.read"] },
      { key: "administration_log", label: "Audit logs", href: "/management/log", roles: ["log.view"] },
      { key: "administration_notifications", label: "Notifications", href: "/management/notifications", roles: ["notification.view"] },
      { key: "administration_statistics", label: "Statistics", href: "/management/statistics", roles: ["administration.reporting.access"] }
    ],
    roles: ["process.run", "user.management.read", "log.view", "notification.view", "administration.reporting.access"]
  },
  {
    key: "help",
    label: "Help",
    href: "/help"
  }
];
function hasRole(userRoles, required) {
  if (!required || required.length === 0) return true;
  return required.some((r) => userRoles.includes(r));
}
function Dropdown({ item, userRoles, currentPath }) {
  const [open, setOpen] = useState(false);
  const visibleChildren = (item.children ?? []).filter(
    (c) => hasRole(userRoles, c.roles)
  );
  if (visibleChildren.length === 0) return null;
  return /* @__PURE__ */ jsxs("div", { className: "relative", onMouseLeave: () => setOpen(false), children: [
    /* @__PURE__ */ jsxs(
      "button",
      {
        className: "px-3 py-2 text-sm font-medium text-white hover:bg-white/10 rounded flex items-center gap-1",
        onMouseEnter: () => setOpen(true),
        onClick: () => setOpen((v) => !v),
        "aria-haspopup": "true",
        "aria-expanded": open,
        children: [
          item.label,
          /* @__PURE__ */ jsx("svg", { className: "w-3 h-3", fill: "none", stroke: "currentColor", viewBox: "0 0 24 24", children: /* @__PURE__ */ jsx("path", { strokeLinecap: "round", strokeLinejoin: "round", strokeWidth: 2, d: "M19 9l-7 7-7-7" }) })
        ]
      }
    ),
    open && /* @__PURE__ */ jsx("div", { className: "absolute left-0 top-full mt-1 w-52 bg-white shadow-lg rounded border border-gray-200 z-50", children: visibleChildren.map((child) => /* @__PURE__ */ jsx(
      "a",
      {
        href: child.href,
        className: `block px-4 py-2 text-sm hover:bg-gray-50 ${currentPath.startsWith(child.href ?? "") ? "bg-blue-50 text-blue-700 font-medium" : "text-gray-700"}`,
        children: child.label
      },
      child.key
    )) })
  ] });
}
function Header({ user, currentPath }) {
  const userRoles = user?.roles ?? [];
  return /* @__PURE__ */ jsx(
    "header",
    {
      className: "bg-[var(--color-primary)] text-white shadow-md sticky top-0 z-40",
      style: { height: "var(--header-height)" },
      children: /* @__PURE__ */ jsxs("div", { className: "max-w-[1400px] mx-auto px-4 h-full flex items-center gap-4", children: [
        /* @__PURE__ */ jsxs("a", { href: "/", className: "flex items-center gap-2 shrink-0 font-bold text-lg text-white hover:no-underline", children: [
          /* @__PURE__ */ jsxs("svg", { className: "w-7 h-7", fill: "none", stroke: "currentColor", viewBox: "0 0 24 24", children: [
            /* @__PURE__ */ jsx("path", { strokeLinecap: "round", strokeLinejoin: "round", strokeWidth: 1.5, d: "M3 7a2 2 0 012-2h14a2 2 0 012 2v10a2 2 0 01-2 2H5a2 2 0 01-2-2V7z" }),
            /* @__PURE__ */ jsx("path", { strokeLinecap: "round", strokeLinejoin: "round", strokeWidth: 1.5, d: "M16 3v4M8 3v4M3 11h18" })
          ] }),
          "ETERNA"
        ] }),
        /* @__PURE__ */ jsx("nav", { className: "flex items-center gap-1 flex-1", children: NAV_ITEMS.map((item) => {
          if (!hasRole(userRoles, item.roles)) return null;
          if (item.children) {
            return /* @__PURE__ */ jsx(Dropdown, { item, userRoles, currentPath }, item.key);
          }
          return /* @__PURE__ */ jsx(
            "a",
            {
              href: item.href,
              className: `px-3 py-2 text-sm font-medium rounded ${currentPath.startsWith(item.href ?? "") && item.href !== "/" ? "bg-white/20" : "hover:bg-white/10"} text-white`,
              children: item.label
            },
            item.key
          );
        }) }),
        /* @__PURE__ */ jsx("div", { className: "flex items-center gap-3 shrink-0", children: user && !user.isGuest ? /* @__PURE__ */ jsx(UserMenu, { user }) : /* @__PURE__ */ jsx("a", { href: "/login", className: "text-sm text-white/80 hover:text-white", children: "Sign in" }) })
      ] })
    }
  );
}
function UserMenu({ user }) {
  const [open, setOpen] = useState(false);
  return /* @__PURE__ */ jsxs("div", { className: "relative", onMouseLeave: () => setOpen(false), children: [
    /* @__PURE__ */ jsxs(
      "button",
      {
        className: "flex items-center gap-2 text-sm text-white hover:text-white/80",
        onClick: () => setOpen((v) => !v),
        children: [
          /* @__PURE__ */ jsx("svg", { className: "w-5 h-5", fill: "none", stroke: "currentColor", viewBox: "0 0 24 24", children: /* @__PURE__ */ jsx("path", { strokeLinecap: "round", strokeLinejoin: "round", strokeWidth: 1.5, d: "M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" }) }),
          /* @__PURE__ */ jsx("span", { children: user.name })
        ]
      }
    ),
    open && /* @__PURE__ */ jsxs("div", { className: "absolute right-0 top-full mt-1 w-44 bg-white shadow-lg rounded border border-gray-200 z-50", children: [
      /* @__PURE__ */ jsx("a", { href: "/auth/profile", className: "block px-4 py-2 text-sm text-gray-700 hover:bg-gray-50", children: "Profile" }),
      /* @__PURE__ */ jsx("hr", { className: "my-1 border-gray-100" }),
      /* @__PURE__ */ jsx("a", { href: "/api/v2/members/users/logout", className: "block px-4 py-2 text-sm text-gray-700 hover:bg-gray-50", children: "Sign out" })
    ] })
  ] });
}

const $$Astro$1 = createAstro();
const $$Breadcrumb = createComponent(($$result, $$props, $$slots) => {
  const Astro2 = $$result.createAstro($$Astro$1, $$props, $$slots);
  Astro2.self = $$Breadcrumb;
  const { items } = Astro2.props;
  return renderTemplate`${items.length > 0 && renderTemplate`${maybeRenderHead()}<nav aria-label="Breadcrumb" class="px-4 py-2 text-sm text-gray-500 bg-white border-b border-gray-200"><ol class="flex flex-wrap items-center gap-1">${items.map((item, idx) => renderTemplate`<li class="flex items-center gap-1">${idx > 0 && renderTemplate`<span class="text-gray-300">/</span>`}${item.href && idx < items.length - 1 ? renderTemplate`<a${addAttribute(item.href, "href")} class="hover:text-blue-600 hover:underline">${item.label}</a>` : renderTemplate`<span${addAttribute(idx === items.length - 1 ? "text-gray-700 font-medium" : "", "class")}>${item.label}</span>`}</li>`)}</ol></nav>`}`;
}, "/home/jerry/Git/eterna-migrate-to-astro/roda-ui/roda-wui-astro/src/components/layout/Breadcrumb.astro", void 0);

const $$Astro = createAstro();
const $$AdminLayout = createComponent(($$result, $$props, $$slots) => {
  const Astro2 = $$result.createAstro($$Astro, $$props, $$slots);
  Astro2.self = $$AdminLayout;
  const { title, breadcrumb = [] } = Astro2.props;
  const user = Astro2.locals.user;
  const currentPath = new URL(Astro2.request.url).pathname;
  return renderTemplate`<html lang="en"> <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"><title>${title} — ETERNA</title><link rel="icon" type="image/x-icon" href="/favicon.ico">${renderHead()}</head> <body> ${renderComponent($$result, "Header", Header, { "user": user, "currentPath": currentPath, "client:load": true, "client:component-hydration": "load", "client:component-path": "/home/jerry/Git/eterna-migrate-to-astro/roda-ui/roda-wui-astro/src/components/layout/Header.tsx", "client:component-export": "default" })} ${breadcrumb.length > 0 && renderTemplate`${renderComponent($$result, "Breadcrumb", $$Breadcrumb, { "items": breadcrumb })}`} <main class="page-content"> ${renderSlot($$result, $$slots["default"])} </main> <footer class="text-center text-xs text-gray-400 py-6 border-t border-gray-200 mt-8">
ETERNA &mdash; Digital Preservation Repository
</footer> </body></html>`;
}, "/home/jerry/Git/eterna-migrate-to-astro/roda-ui/roda-wui-astro/src/components/layout/AdminLayout.astro", void 0);

export { $$AdminLayout as $ };

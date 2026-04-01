import { e as createComponent, k as renderComponent, r as renderTemplate, m as maybeRenderHead, g as addAttribute } from '../chunks/astro/server_BTiD7zzO.mjs';
import 'piccolore';
import { $ as $$AdminLayout } from '../chunks/AdminLayout_B_CZkfkR.mjs';
export { renderers } from '../renderers.mjs';

const $$Index = createComponent(($$result, $$props, $$slots) => {
  const sections = [
    { title: "Actions", href: "/management/actions", desc: "Run background preservation actions." },
    { title: "Internal actions", href: "/management/internal", desc: "Monitor internal system processes." },
    { title: "Users and groups", href: "/management/members", desc: "Manage system users and groups." },
    { title: "Audit logs", href: "/management/log", desc: "Browse the system audit log." },
    { title: "Notifications", href: "/management/notifications", desc: "View system notifications." },
    { title: "Statistics", href: "/management/statistics", desc: "System and repository statistics." }
  ];
  return renderTemplate`${renderComponent($$result, "AdminLayout", $$AdminLayout, { "title": "Administration", "breadcrumb": [{ label: "Administration" }] }, { "default": ($$result2) => renderTemplate` ${maybeRenderHead()}<h1 class="text-2xl font-bold text-gray-800 mb-6">Administration</h1> <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4"> ${sections.map((s) => renderTemplate`<a${addAttribute(s.href, "href")} class="block p-5 bg-white rounded-lg border border-gray-200 hover:border-blue-300 hover:shadow-sm transition-all"> <h2 class="font-semibold text-gray-800 mb-1">${s.title}</h2> <p class="text-sm text-gray-500">${s.desc}</p> </a>`)} </div> ` })}`;
}, "/home/jerry/Git/eterna-migrate-to-astro/roda-ui/roda-wui-astro/src/pages/management/index.astro", void 0);

const $$file = "/home/jerry/Git/eterna-migrate-to-astro/roda-ui/roda-wui-astro/src/pages/management/index.astro";
const $$url = "/management";

const _page = /*#__PURE__*/Object.freeze(/*#__PURE__*/Object.defineProperty({
  __proto__: null,
  default: $$Index,
  file: $$file,
  url: $$url
}, Symbol.toStringTag, { value: 'Module' }));

const page = () => _page;

export { page };

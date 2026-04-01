import { e as createComponent, k as renderComponent, r as renderTemplate, m as maybeRenderHead, g as addAttribute } from '../chunks/astro/server_BTiD7zzO.mjs';
import 'piccolore';
import { $ as $$AdminLayout } from '../chunks/AdminLayout_B_CZkfkR.mjs';
export { renderers } from '../renderers.mjs';

const $$Index = createComponent(($$result, $$props, $$slots) => {
  const sections = [
    { title: "Policy", href: "/disposal/policy", desc: "Manage disposal schedules, holds, and rules." },
    { title: "Confirmations", href: "/disposal/confirmations", desc: "Create and manage disposal confirmations." },
    { title: "Destroyed records", href: "/disposal/destroyed", desc: "View records of destroyed content." }
  ];
  return renderTemplate`${renderComponent($$result, "AdminLayout", $$AdminLayout, { "title": "Disposal", "breadcrumb": [{ label: "Disposal" }] }, { "default": ($$result2) => renderTemplate` ${maybeRenderHead()}<h1 class="text-2xl font-bold text-gray-800 mb-6">Disposal</h1> <div class="grid grid-cols-1 sm:grid-cols-3 gap-4"> ${sections.map((s) => renderTemplate`<a${addAttribute(s.href, "href")} class="block p-5 bg-white rounded-lg border border-gray-200 hover:border-blue-300 hover:shadow-sm transition-all"> <h2 class="font-semibold text-gray-800 mb-1">${s.title}</h2> <p class="text-sm text-gray-500">${s.desc}</p> </a>`)} </div> ` })}`;
}, "/home/jerry/Git/eterna-migrate-to-astro/roda-ui/roda-wui-astro/src/pages/disposal/index.astro", void 0);

const $$file = "/home/jerry/Git/eterna-migrate-to-astro/roda-ui/roda-wui-astro/src/pages/disposal/index.astro";
const $$url = "/disposal";

const _page = /*#__PURE__*/Object.freeze(/*#__PURE__*/Object.defineProperty({
  __proto__: null,
  default: $$Index,
  file: $$file,
  url: $$url
}, Symbol.toStringTag, { value: 'Module' }));

const page = () => _page;

export { page };

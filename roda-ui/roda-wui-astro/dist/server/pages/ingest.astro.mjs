import { e as createComponent, k as renderComponent, r as renderTemplate, m as maybeRenderHead, g as addAttribute } from '../chunks/astro/server_BTiD7zzO.mjs';
import 'piccolore';
import { $ as $$AdminLayout } from '../chunks/AdminLayout_B_CZkfkR.mjs';
export { renderers } from '../renderers.mjs';

const $$Index = createComponent(($$result, $$props, $$slots) => {
  const sections = [
    { title: "Pre-ingest", href: "/ingest/pre", desc: "Pre-ingest assessment and validation instructions." },
    { title: "Transfer", href: "/ingest/transfer", desc: "Browse and upload submission information packages." },
    { title: "Process", href: "/ingest/process", desc: "Monitor ongoing and completed ingest jobs." },
    { title: "Appraisal", href: "/ingest/appraisal", desc: "Assess and appraise ingested content." }
  ];
  return renderTemplate`${renderComponent($$result, "AdminLayout", $$AdminLayout, { "title": "Ingest", "breadcrumb": [{ label: "Ingest" }] }, { "default": ($$result2) => renderTemplate` ${maybeRenderHead()}<h1 class="text-2xl font-bold text-gray-800 mb-6">Ingest</h1> <div class="grid grid-cols-1 sm:grid-cols-2 gap-4"> ${sections.map((s) => renderTemplate`<a${addAttribute(s.href, "href")} class="block p-5 bg-white rounded-lg border border-gray-200 hover:border-blue-300 hover:shadow-sm transition-all"> <h2 class="font-semibold text-gray-800 mb-1">${s.title}</h2> <p class="text-sm text-gray-500">${s.desc}</p> </a>`)} </div> ` })}`;
}, "/home/jerry/Git/eterna-migrate-to-astro/roda-ui/roda-wui-astro/src/pages/ingest/index.astro", void 0);

const $$file = "/home/jerry/Git/eterna-migrate-to-astro/roda-ui/roda-wui-astro/src/pages/ingest/index.astro";
const $$url = "/ingest";

const _page = /*#__PURE__*/Object.freeze(/*#__PURE__*/Object.defineProperty({
  __proto__: null,
  default: $$Index,
  file: $$file,
  url: $$url
}, Symbol.toStringTag, { value: 'Module' }));

const page = () => _page;

export { page };

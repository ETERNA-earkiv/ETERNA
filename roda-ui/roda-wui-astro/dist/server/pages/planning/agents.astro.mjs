import { e as createComponent, k as renderComponent, r as renderTemplate, m as maybeRenderHead } from '../../chunks/astro/server_BTiD7zzO.mjs';
import 'piccolore';
import { $ as $$AdminLayout } from '../../chunks/AdminLayout_B_CZkfkR.mjs';
export { renderers } from '../../renderers.mjs';

const $$Index = createComponent(($$result, $$props, $$slots) => {
  return renderTemplate`${renderComponent($$result, "AdminLayout", $$AdminLayout, { "title": "Preservation agents", "breadcrumb": [{ label: "Planning", href: "/planning" }, { label: "Preservation agents" }] }, { "default": ($$result2) => renderTemplate` ${maybeRenderHead()}<h1 class="text-2xl font-bold text-gray-800 mb-6">Preservation agents</h1>  <div class="bg-white rounded-lg border border-gray-200 p-8 text-center text-gray-400 shadow-sm"> <p>Preservation agents coming in Phase 4.</p> </div> ` })}`;
}, "/home/jerry/Git/eterna-migrate-to-astro/roda-ui/roda-wui-astro/src/pages/planning/agents/index.astro", void 0);

const $$file = "/home/jerry/Git/eterna-migrate-to-astro/roda-ui/roda-wui-astro/src/pages/planning/agents/index.astro";
const $$url = "/planning/agents";

const _page = /*#__PURE__*/Object.freeze(/*#__PURE__*/Object.defineProperty({
  __proto__: null,
  default: $$Index,
  file: $$file,
  url: $$url
}, Symbol.toStringTag, { value: 'Module' }));

const page = () => _page;

export { page };

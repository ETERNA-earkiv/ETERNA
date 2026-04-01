import { e as createComponent, k as renderComponent, r as renderTemplate, m as maybeRenderHead } from '../../chunks/astro/server_BTiD7zzO.mjs';
import 'piccolore';
import { $ as $$AdminLayout } from '../../chunks/AdminLayout_B_CZkfkR.mjs';
export { renderers } from '../../renderers.mjs';

const $$Index = createComponent(($$result, $$props, $$slots) => {
  return renderTemplate`${renderComponent($$result, "AdminLayout", $$AdminLayout, { "title": "Risk register", "breadcrumb": [{ label: "Planning", href: "/planning" }, { label: "Risk register" }] }, { "default": ($$result2) => renderTemplate` ${maybeRenderHead()}<div class="flex items-center justify-between mb-6"> <h1 class="text-2xl font-bold text-gray-800">Risk register</h1> <a href="/planning/risks/new" class="px-4 py-2 bg-[var(--color-primary)] text-white text-sm rounded hover:bg-[var(--color-primary-dark)] transition-colors">
New risk
</a> </div>  <div class="bg-white rounded-lg border border-gray-200 p-8 text-center text-gray-400 shadow-sm"> <p>Risk register coming in Phase 4.</p> </div> ` })}`;
}, "/home/jerry/Git/eterna-migrate-to-astro/roda-ui/roda-wui-astro/src/pages/planning/risks/index.astro", void 0);

const $$file = "/home/jerry/Git/eterna-migrate-to-astro/roda-ui/roda-wui-astro/src/pages/planning/risks/index.astro";
const $$url = "/planning/risks";

const _page = /*#__PURE__*/Object.freeze(/*#__PURE__*/Object.defineProperty({
  __proto__: null,
  default: $$Index,
  file: $$file,
  url: $$url
}, Symbol.toStringTag, { value: 'Module' }));

const page = () => _page;

export { page };

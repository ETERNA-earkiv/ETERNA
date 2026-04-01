import { e as createComponent, k as renderComponent, r as renderTemplate, m as maybeRenderHead } from '../../chunks/astro/server_BTiD7zzO.mjs';
import 'piccolore';
import { $ as $$AdminLayout } from '../../chunks/AdminLayout_B_CZkfkR.mjs';
export { renderers } from '../../renderers.mjs';

const $$Policy = createComponent(($$result, $$props, $$slots) => {
  return renderTemplate`${renderComponent($$result, "AdminLayout", $$AdminLayout, { "title": "Disposal policy", "breadcrumb": [{ label: "Disposal", href: "/disposal" }, { label: "Policy" }] }, { "default": ($$result2) => renderTemplate` ${maybeRenderHead()}<h1 class="text-2xl font-bold text-gray-800 mb-6">Disposal policy</h1> <div class="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-8"> <a href="/disposal/schedule/new" class="px-4 py-2 bg-[var(--color-primary)] text-white text-sm rounded hover:bg-[var(--color-primary-dark)] text-center">
New schedule
</a> <a href="/disposal/hold/new" class="px-4 py-2 border border-gray-300 text-gray-700 text-sm rounded hover:bg-gray-50 text-center">
New hold
</a> <a href="/disposal/rule/new" class="px-4 py-2 border border-gray-300 text-gray-700 text-sm rounded hover:bg-gray-50 text-center">
New rule
</a> </div>  <div class="bg-white rounded-lg border border-gray-200 p-8 text-center text-gray-400 shadow-sm"> <p>Disposal policy tabs (schedules, holds, rules) coming in Phase 5.</p> </div> ` })}`;
}, "/home/jerry/Git/eterna-migrate-to-astro/roda-ui/roda-wui-astro/src/pages/disposal/policy.astro", void 0);

const $$file = "/home/jerry/Git/eterna-migrate-to-astro/roda-ui/roda-wui-astro/src/pages/disposal/policy.astro";
const $$url = "/disposal/policy";

const _page = /*#__PURE__*/Object.freeze(/*#__PURE__*/Object.defineProperty({
  __proto__: null,
  default: $$Policy,
  file: $$file,
  url: $$url
}, Symbol.toStringTag, { value: 'Module' }));

const page = () => _page;

export { page };

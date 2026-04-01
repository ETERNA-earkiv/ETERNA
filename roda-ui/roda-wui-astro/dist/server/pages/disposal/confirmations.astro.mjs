import { e as createComponent, k as renderComponent, r as renderTemplate, m as maybeRenderHead } from '../../chunks/astro/server_BTiD7zzO.mjs';
import 'piccolore';
import { $ as $$AdminLayout } from '../../chunks/AdminLayout_B_CZkfkR.mjs';
export { renderers } from '../../renderers.mjs';

const $$Index = createComponent(($$result, $$props, $$slots) => {
  return renderTemplate`${renderComponent($$result, "AdminLayout", $$AdminLayout, { "title": "Disposal confirmations", "breadcrumb": [{ label: "Disposal", href: "/disposal" }, { label: "Confirmations" }] }, { "default": ($$result2) => renderTemplate` ${maybeRenderHead()}<div class="flex items-center justify-between mb-6"> <h1 class="text-2xl font-bold text-gray-800">Disposal confirmations</h1> <a href="/disposal/confirmations/new" class="px-4 py-2 bg-[var(--color-primary)] text-white text-sm rounded hover:bg-[var(--color-primary-dark)] transition-colors">
New confirmation
</a> </div>  <div class="bg-white rounded-lg border border-gray-200 p-8 text-center text-gray-400 shadow-sm"> <p>Disposal confirmation list coming in Phase 5.</p> </div> ` })}`;
}, "/home/jerry/Git/eterna-migrate-to-astro/roda-ui/roda-wui-astro/src/pages/disposal/confirmations/index.astro", void 0);

const $$file = "/home/jerry/Git/eterna-migrate-to-astro/roda-ui/roda-wui-astro/src/pages/disposal/confirmations/index.astro";
const $$url = "/disposal/confirmations";

const _page = /*#__PURE__*/Object.freeze(/*#__PURE__*/Object.defineProperty({
  __proto__: null,
  default: $$Index,
  file: $$file,
  url: $$url
}, Symbol.toStringTag, { value: 'Module' }));

const page = () => _page;

export { page };

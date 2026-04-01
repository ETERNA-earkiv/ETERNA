import { e as createComponent, k as renderComponent, r as renderTemplate, h as createAstro, m as maybeRenderHead } from '../chunks/astro/server_BTiD7zzO.mjs';
import 'piccolore';
import { $ as $$AdminLayout } from '../chunks/AdminLayout_B_CZkfkR.mjs';
import { A as AIPList } from '../chunks/AIPList_uHC8qN4n.mjs';
export { renderers } from '../renderers.mjs';

const $$Astro = createAstro();
const $$Index = createComponent(($$result, $$props, $$slots) => {
  const Astro2 = $$result.createAstro($$Astro, $$props, $$slots);
  Astro2.self = $$Index;
  const user = Astro2.locals.user;
  return renderTemplate`${renderComponent($$result, "AdminLayout", $$AdminLayout, { "title": "Browse", "breadcrumb": [{ label: "Browse" }] }, { "default": ($$result2) => renderTemplate` ${maybeRenderHead()}<div class="flex items-center justify-between mb-6"> <h1 class="text-2xl font-bold text-gray-800">Browse</h1> </div> ${renderComponent($$result2, "AIPList", AIPList, { "user": user, "client:load": true, "client:component-hydration": "load", "client:component-path": "@/components/browse/AIPList.tsx", "client:component-export": "default" })} ` })}`;
}, "/home/jerry/Git/eterna-migrate-to-astro/roda-ui/roda-wui-astro/src/pages/browse/index.astro", void 0);

const $$file = "/home/jerry/Git/eterna-migrate-to-astro/roda-ui/roda-wui-astro/src/pages/browse/index.astro";
const $$url = "/browse";

const _page = /*#__PURE__*/Object.freeze(/*#__PURE__*/Object.defineProperty({
  __proto__: null,
  default: $$Index,
  file: $$file,
  url: $$url
}, Symbol.toStringTag, { value: 'Module' }));

const page = () => _page;

export { page };

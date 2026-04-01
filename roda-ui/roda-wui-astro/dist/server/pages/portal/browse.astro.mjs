import { e as createComponent, k as renderComponent, r as renderTemplate, m as maybeRenderHead } from '../../chunks/astro/server_BTiD7zzO.mjs';
import 'piccolore';
import { $ as $$PortalLayout } from '../../chunks/PortalLayout_C11Df-vl.mjs';
import { A as AIPList } from '../../chunks/AIPList_uHC8qN4n.mjs';
export { renderers } from '../../renderers.mjs';

const $$Index = createComponent(($$result, $$props, $$slots) => {
  return renderTemplate`${renderComponent($$result, "PortalLayout", $$PortalLayout, { "title": "Browse" }, { "default": ($$result2) => renderTemplate` ${maybeRenderHead()}<h1 class="text-2xl font-bold text-gray-800 mb-6">Browse the archive</h1> ${renderComponent($$result2, "AIPList", AIPList, { "user": null, "client:load": true, "client:component-hydration": "load", "client:component-path": "@/components/browse/AIPList.tsx", "client:component-export": "default" })} ` })}`;
}, "/home/jerry/Git/eterna-migrate-to-astro/roda-ui/roda-wui-astro/src/pages/portal/browse/index.astro", void 0);

const $$file = "/home/jerry/Git/eterna-migrate-to-astro/roda-ui/roda-wui-astro/src/pages/portal/browse/index.astro";
const $$url = "/portal/browse";

const _page = /*#__PURE__*/Object.freeze(/*#__PURE__*/Object.defineProperty({
  __proto__: null,
  default: $$Index,
  file: $$file,
  url: $$url
}, Symbol.toStringTag, { value: 'Module' }));

const page = () => _page;

export { page };

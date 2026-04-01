import { e as createComponent, k as renderComponent, r as renderTemplate, h as createAstro, l as Fragment, u as unescapeHTML, m as maybeRenderHead } from '../chunks/astro/server_BTiD7zzO.mjs';
import 'piccolore';
import { $ as $$PortalLayout } from '../chunks/PortalLayout_C11Df-vl.mjs';
import { a as apiGet } from '../chunks/client_BbTdbxg1.mjs';
export { renderers } from '../renderers.mjs';

const $$Astro = createAstro();
const $$Index = createComponent(async ($$result, $$props, $$slots) => {
  const Astro2 = $$result.createAstro($$Astro, $$props, $$slots);
  Astro2.self = $$Index;
  const cookieHeader = Astro2.request.headers.get("cookie") ?? void 0;
  let welcomeHtml = "";
  try {
    welcomeHtml = await apiGet("/themes?resource-id=WelcomePortal.html", { cookie: cookieHeader });
  } catch {
  }
  return renderTemplate`${renderComponent($$result, "PortalLayout", $$PortalLayout, { "title": "Welcome" }, { "default": async ($$result2) => renderTemplate`${welcomeHtml ? renderTemplate`${renderComponent($$result2, "Fragment", Fragment, {}, { "default": async ($$result3) => renderTemplate`${unescapeHTML(welcomeHtml)}` })}` : renderTemplate`${maybeRenderHead()}<div class="py-12 text-center max-w-2xl mx-auto"> <h1 class="text-3xl font-bold text-[var(--color-primary)] mb-4">ETERNA Archive</h1> <p class="text-gray-600 mb-8">
Welcome to the ETERNA digital preservation repository. Browse or search the archive to find preserved records.
</p> <div class="flex justify-center gap-4"> <a href="/portal/browse" class="px-6 py-3 bg-[var(--color-primary)] text-white rounded-lg hover:bg-[var(--color-primary-dark)] transition-colors">
Browse archive
</a> <a href="/portal/search" class="px-6 py-3 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors">
Search
</a> </div> </div>`}` })}`;
}, "/home/jerry/Git/eterna-migrate-to-astro/roda-ui/roda-wui-astro/src/pages/portal/index.astro", void 0);

const $$file = "/home/jerry/Git/eterna-migrate-to-astro/roda-ui/roda-wui-astro/src/pages/portal/index.astro";
const $$url = "/portal";

const _page = /*#__PURE__*/Object.freeze(/*#__PURE__*/Object.defineProperty({
  __proto__: null,
  default: $$Index,
  file: $$file,
  url: $$url
}, Symbol.toStringTag, { value: 'Module' }));

const page = () => _page;

export { page };

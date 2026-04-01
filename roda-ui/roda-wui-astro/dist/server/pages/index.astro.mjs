import { e as createComponent, k as renderComponent, r as renderTemplate, h as createAstro, l as Fragment, u as unescapeHTML, m as maybeRenderHead } from '../chunks/astro/server_BTiD7zzO.mjs';
import 'piccolore';
import { $ as $$AdminLayout } from '../chunks/AdminLayout_B_CZkfkR.mjs';
import { a as apiGet } from '../chunks/client_BbTdbxg1.mjs';
export { renderers } from '../renderers.mjs';

const $$Astro = createAstro();
const $$Index = createComponent(async ($$result, $$props, $$slots) => {
  const Astro2 = $$result.createAstro($$Astro, $$props, $$slots);
  Astro2.self = $$Index;
  const cookieHeader = Astro2.request.headers.get("cookie") ?? void 0;
  const user = Astro2.locals.user;
  let welcomeHtml = "";
  try {
    welcomeHtml = await apiGet("/themes?resource-id=Welcome.html", { cookie: cookieHeader });
  } catch {
  }
  return renderTemplate`${renderComponent($$result, "AdminLayout", $$AdminLayout, { "title": "Welcome" }, { "default": async ($$result2) => renderTemplate`${welcomeHtml ? renderTemplate`${renderComponent($$result2, "Fragment", Fragment, {}, { "default": async ($$result3) => renderTemplate`${unescapeHTML(welcomeHtml)}` })}` : renderTemplate`${maybeRenderHead()}<div class="max-w-2xl mx-auto py-12 text-center"> <h1 class="text-3xl font-bold text-[var(--color-primary)] mb-4">Welcome to ETERNA</h1> <p class="text-gray-600 mb-8">
ETERNA is an open-source digital preservation repository implementing the OAIS reference model.
</p> ${user && renderTemplate`<div class="grid grid-cols-2 sm:grid-cols-3 gap-4 mt-8 text-left"> <a href="/browse" class="block p-4 bg-white rounded-lg border border-gray-200 hover:border-blue-300 hover:shadow-sm transition-all"> <div class="text-2xl mb-2">🗂️</div> <div class="font-medium text-gray-800">Browse</div> <div class="text-sm text-gray-500 mt-1">Browse the archive</div> </a> <a href="/search" class="block p-4 bg-white rounded-lg border border-gray-200 hover:border-blue-300 hover:shadow-sm transition-all"> <div class="text-2xl mb-2">🔍</div> <div class="font-medium text-gray-800">Search</div> <div class="text-sm text-gray-500 mt-1">Search across content</div> </a> <a href="/ingest/transfer" class="block p-4 bg-white rounded-lg border border-gray-200 hover:border-blue-300 hover:shadow-sm transition-all"> <div class="text-2xl mb-2">📥</div> <div class="font-medium text-gray-800">Ingest</div> <div class="text-sm text-gray-500 mt-1">Submit new content</div> </a> </div>`} </div>`}` })}`;
}, "/home/jerry/Git/eterna-migrate-to-astro/roda-ui/roda-wui-astro/src/pages/index.astro", void 0);

const $$file = "/home/jerry/Git/eterna-migrate-to-astro/roda-ui/roda-wui-astro/src/pages/index.astro";
const $$url = "";

const _page = /*#__PURE__*/Object.freeze(/*#__PURE__*/Object.defineProperty({
  __proto__: null,
  default: $$Index,
  file: $$file,
  url: $$url
}, Symbol.toStringTag, { value: 'Module' }));

const page = () => _page;

export { page };

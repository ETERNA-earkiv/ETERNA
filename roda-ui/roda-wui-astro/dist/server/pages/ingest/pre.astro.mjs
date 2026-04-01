import { e as createComponent, k as renderComponent, r as renderTemplate, h as createAstro, m as maybeRenderHead, l as Fragment, u as unescapeHTML } from '../../chunks/astro/server_BTiD7zzO.mjs';
import 'piccolore';
import { $ as $$AdminLayout } from '../../chunks/AdminLayout_B_CZkfkR.mjs';
import { a as apiGet } from '../../chunks/client_BbTdbxg1.mjs';
export { renderers } from '../../renderers.mjs';

const $$Astro = createAstro();
const $$Pre = createComponent(async ($$result, $$props, $$slots) => {
  const Astro2 = $$result.createAstro($$Astro, $$props, $$slots);
  Astro2.self = $$Pre;
  const cookieHeader = Astro2.request.headers.get("cookie") ?? void 0;
  let preIngestHtml = "";
  try {
    preIngestHtml = await apiGet("/themes?resource-id=IngestTransferDescription.html", { cookie: cookieHeader });
  } catch {
  }
  return renderTemplate`${renderComponent($$result, "AdminLayout", $$AdminLayout, { "title": "Pre-ingest", "breadcrumb": [{ label: "Ingest", href: "/ingest" }, { label: "Pre-ingest" }] }, { "default": async ($$result2) => renderTemplate` ${maybeRenderHead()}<h1 class="text-2xl font-bold text-gray-800 mb-6">Pre-ingest</h1> ${preIngestHtml ? renderTemplate`<div class="prose max-w-none"> ${renderComponent($$result2, "Fragment", Fragment, {}, { "default": async ($$result3) => renderTemplate`${unescapeHTML(preIngestHtml)}` })} </div>` : renderTemplate`<div class="bg-white rounded-lg border border-gray-200 p-6 shadow-sm prose max-w-none"> <p>This section describes the pre-ingest process, including how to prepare Submission Information Packages (SIPs) for ingestion.</p> </div>`}` })}`;
}, "/home/jerry/Git/eterna-migrate-to-astro/roda-ui/roda-wui-astro/src/pages/ingest/pre.astro", void 0);

const $$file = "/home/jerry/Git/eterna-migrate-to-astro/roda-ui/roda-wui-astro/src/pages/ingest/pre.astro";
const $$url = "/ingest/pre";

const _page = /*#__PURE__*/Object.freeze(/*#__PURE__*/Object.defineProperty({
  __proto__: null,
  default: $$Pre,
  file: $$file,
  url: $$url
}, Symbol.toStringTag, { value: 'Module' }));

const page = () => _page;

export { page };

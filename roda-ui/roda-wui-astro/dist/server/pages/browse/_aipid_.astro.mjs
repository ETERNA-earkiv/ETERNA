import { e as createComponent, k as renderComponent, r as renderTemplate, h as createAstro, m as maybeRenderHead, g as addAttribute } from '../../chunks/astro/server_BTiD7zzO.mjs';
import 'piccolore';
import { $ as $$AdminLayout } from '../../chunks/AdminLayout_B_CZkfkR.mjs';
import { a as apiGet } from '../../chunks/client_BbTdbxg1.mjs';
export { renderers } from '../../renderers.mjs';

const $$Astro = createAstro();
const $$aipId = createComponent(async ($$result, $$props, $$slots) => {
  const Astro2 = $$result.createAstro($$Astro, $$props, $$slots);
  Astro2.self = $$aipId;
  const { aipId } = Astro2.params;
  const cookieHeader = Astro2.request.headers.get("cookie") ?? void 0;
  let aip = null;
  let metadata = [];
  let notFound = false;
  try {
    aip = await apiGet(`/aips/find/${aipId}`, { cookie: cookieHeader });
    const metaRes = await apiGet(
      `/aips/${aipId}/metadata/descriptive/information`,
      { cookie: cookieHeader }
    ).catch(() => ({ metadataList: [] }));
    metadata = metaRes.metadataList ?? [];
  } catch (e) {
    if (e.status === 404) {
      notFound = true;
    } else {
      throw e;
    }
  }
  if (notFound) {
    return Astro2.redirect("/404");
  }
  const title = aip?.title ?? aipId ?? "AIP";
  const breadcrumb = [
    { label: "Browse", href: "/browse" },
    { label: title }
  ];
  return renderTemplate`${renderComponent($$result, "AdminLayout", $$AdminLayout, { "title": title, "breadcrumb": breadcrumb }, { "default": async ($$result2) => renderTemplate`${aip && renderTemplate`${maybeRenderHead()}<div class="space-y-6"> <!-- AIP Header --> <div class="bg-white rounded-lg border border-gray-200 p-6 shadow-sm"> <div class="flex items-start justify-between"> <div> <h1 class="text-2xl font-bold text-gray-800">${title}</h1> <div class="mt-2 flex items-center gap-3 text-sm text-gray-500"> ${aip.level && renderTemplate`<span class="px-2 py-0.5 bg-gray-100 rounded-full">${aip.level}</span>`} ${aip.state && renderTemplate`<span${addAttribute(`px-2 py-0.5 rounded-full ${aip.state === "ACTIVE" ? "bg-green-100 text-green-700" : "bg-gray-100 text-gray-600"}`, "class")}> ${aip.state} </span>`} <span class="font-mono text-xs text-gray-400">${aip.id}</span> </div> </div> <div class="flex gap-2"> <a${addAttribute(`/api/v2/aips/${aipId}/download`, "href")} class="px-3 py-1.5 text-sm border border-gray-300 rounded hover:bg-gray-50 flex items-center gap-1">
↓ Download
</a> </div> </div> </div> <!-- Descriptive Metadata --> ${metadata.length > 0 && renderTemplate`<div class="bg-white rounded-lg border border-gray-200 shadow-sm"> <div class="px-6 py-4 border-b border-gray-100"> <h2 class="text-lg font-semibold text-gray-700">Descriptive Metadata</h2> </div> <div class="divide-y divide-gray-100"> ${metadata.map((m) => renderTemplate`<div class="px-6 py-4 flex items-center justify-between"> <div> <span class="font-medium text-gray-800">${m.type}</span> ${m.version && renderTemplate`<span class="ml-2 text-sm text-gray-400">v${m.version}</span>`} </div> <div class="flex gap-2"> <a${addAttribute(`/api/v2/aips/${aipId}/metadata/descriptive/${m.id}/html`, "href")} target="_blank" class="text-sm text-blue-600 hover:underline">
View
</a> <a${addAttribute(`/api/v2/aips/${aipId}/metadata/descriptive/${m.id}/download`, "href")} class="text-sm text-blue-600 hover:underline">
Download
</a> </div> </div>`)} </div> </div>`} <!-- Representations (lazy-loaded) --> <div class="bg-white rounded-lg border border-gray-200 shadow-sm"> <div class="px-6 py-4 border-b border-gray-100"> <h2 class="text-lg font-semibold text-gray-700">Representations</h2> </div> <div class="p-4"> <!-- TODO: RepresentationList island (Phase 1) --> <p class="text-sm text-gray-400 italic">Representations loading coming in Phase 1.</p> </div> </div> </div>`}` })}`;
}, "/home/jerry/Git/eterna-migrate-to-astro/roda-ui/roda-wui-astro/src/pages/browse/[aipId].astro", void 0);

const $$file = "/home/jerry/Git/eterna-migrate-to-astro/roda-ui/roda-wui-astro/src/pages/browse/[aipId].astro";
const $$url = "/browse/[aipId]";

const _page = /*#__PURE__*/Object.freeze(/*#__PURE__*/Object.defineProperty({
  __proto__: null,
  default: $$aipId,
  file: $$file,
  url: $$url
}, Symbol.toStringTag, { value: 'Module' }));

const page = () => _page;

export { page };

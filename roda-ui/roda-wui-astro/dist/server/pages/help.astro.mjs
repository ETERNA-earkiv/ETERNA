import { e as createComponent, k as renderComponent, r as renderTemplate, h as createAstro, m as maybeRenderHead, l as Fragment, u as unescapeHTML } from '../chunks/astro/server_BTiD7zzO.mjs';
import 'piccolore';
import { $ as $$AdminLayout } from '../chunks/AdminLayout_B_CZkfkR.mjs';
import { a as apiGet } from '../chunks/client_BbTdbxg1.mjs';
export { renderers } from '../renderers.mjs';

const $$Astro = createAstro();
const $$Help = createComponent(async ($$result, $$props, $$slots) => {
  const Astro2 = $$result.createAstro($$Astro, $$props, $$slots);
  Astro2.self = $$Help;
  const cookieHeader = Astro2.request.headers.get("cookie") ?? void 0;
  let helpHtml = "";
  try {
    helpHtml = await apiGet("/themes?resource-id=Help.html", { cookie: cookieHeader });
  } catch {
  }
  return renderTemplate`${renderComponent($$result, "AdminLayout", $$AdminLayout, { "title": "Help", "breadcrumb": [{ label: "Help" }] }, { "default": async ($$result2) => renderTemplate`${helpHtml ? renderTemplate`${maybeRenderHead()}<div class="prose max-w-none"> ${renderComponent($$result2, "Fragment", Fragment, {}, { "default": async ($$result3) => renderTemplate`${unescapeHTML(helpHtml)}` })} </div>` : renderTemplate`<div class="prose max-w-none bg-white rounded-lg border border-gray-200 p-6 shadow-sm"> <h1>Help</h1> <p>For documentation and support, please refer to the ETERNA documentation.</p> </div>`}` })}`;
}, "/home/jerry/Git/eterna-migrate-to-astro/roda-ui/roda-wui-astro/src/pages/help.astro", void 0);

const $$file = "/home/jerry/Git/eterna-migrate-to-astro/roda-ui/roda-wui-astro/src/pages/help.astro";
const $$url = "/help";

const _page = /*#__PURE__*/Object.freeze(/*#__PURE__*/Object.defineProperty({
  __proto__: null,
  default: $$Help,
  file: $$file,
  url: $$url
}, Symbol.toStringTag, { value: 'Module' }));

const page = () => _page;

export { page };

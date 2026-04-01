import { e as createComponent, k as renderComponent, r as renderTemplate, m as maybeRenderHead } from '../../chunks/astro/server_BTiD7zzO.mjs';
import 'piccolore';
import { $ as $$AuthLayout } from '../../chunks/AuthLayout_CvBeulq5.mjs';
export { renderers } from '../../renderers.mjs';

const $$Recover = createComponent(($$result, $$props, $$slots) => {
  return renderTemplate`${renderComponent($$result, "AuthLayout", $$AuthLayout, { "title": "Password recovery" }, { "default": ($$result2) => renderTemplate` ${maybeRenderHead()}<h1 class="text-xl font-semibold text-gray-800 mb-6 text-center">Recover your password</h1>  <p class="text-sm text-gray-500 text-center">Password recovery coming in Phase 3.</p> <div class="mt-4 text-center text-sm"> <a href="/login" class="text-[var(--color-primary)] hover:underline">Back to sign in</a> </div> ` })}`;
}, "/home/jerry/Git/eterna-migrate-to-astro/roda-ui/roda-wui-astro/src/pages/auth/recover.astro", void 0);

const $$file = "/home/jerry/Git/eterna-migrate-to-astro/roda-ui/roda-wui-astro/src/pages/auth/recover.astro";
const $$url = "/auth/recover";

const _page = /*#__PURE__*/Object.freeze(/*#__PURE__*/Object.defineProperty({
  __proto__: null,
  default: $$Recover,
  file: $$file,
  url: $$url
}, Symbol.toStringTag, { value: 'Module' }));

const page = () => _page;

export { page };

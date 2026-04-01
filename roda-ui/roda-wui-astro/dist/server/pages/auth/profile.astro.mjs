import { e as createComponent, k as renderComponent, r as renderTemplate, h as createAstro, m as maybeRenderHead } from '../../chunks/astro/server_BTiD7zzO.mjs';
import 'piccolore';
import { $ as $$AdminLayout } from '../../chunks/AdminLayout_B_CZkfkR.mjs';
export { renderers } from '../../renderers.mjs';

const $$Astro = createAstro();
const $$Profile = createComponent(($$result, $$props, $$slots) => {
  const Astro2 = $$result.createAstro($$Astro, $$props, $$slots);
  Astro2.self = $$Profile;
  const user = Astro2.locals.user;
  return renderTemplate`${renderComponent($$result, "AdminLayout", $$AdminLayout, { "title": "Profile", "breadcrumb": [{ label: "Profile" }] }, { "default": ($$result2) => renderTemplate` ${maybeRenderHead()}<h1 class="text-2xl font-bold text-gray-800 mb-6">Profile</h1> ${user && renderTemplate`<div class="bg-white rounded-lg border border-gray-200 p-6 shadow-sm max-w-lg"> <dl class="space-y-3 text-sm"> <div class="flex gap-4"> <dt class="w-32 font-medium text-gray-500">Username</dt> <dd class="text-gray-800">${user.name}</dd> </div> <div class="flex gap-4"> <dt class="w-32 font-medium text-gray-500">Full name</dt> <dd class="text-gray-800">${user.fullname || "\u2014"}</dd> </div> <div class="flex gap-4"> <dt class="w-32 font-medium text-gray-500">Email</dt> <dd class="text-gray-800">${user.email || "\u2014"}</dd> </div> <div class="flex gap-4"> <dt class="w-32 font-medium text-gray-500">Groups</dt> <dd class="text-gray-800">${user.groups.join(", ") || "\u2014"}</dd> </div> </dl> <!-- TODO: UserForm island (Phase 3) --> </div>`}` })}`;
}, "/home/jerry/Git/eterna-migrate-to-astro/roda-ui/roda-wui-astro/src/pages/auth/profile.astro", void 0);

const $$file = "/home/jerry/Git/eterna-migrate-to-astro/roda-ui/roda-wui-astro/src/pages/auth/profile.astro";
const $$url = "/auth/profile";

const _page = /*#__PURE__*/Object.freeze(/*#__PURE__*/Object.defineProperty({
  __proto__: null,
  default: $$Profile,
  file: $$file,
  url: $$url
}, Symbol.toStringTag, { value: 'Module' }));

const page = () => _page;

export { page };

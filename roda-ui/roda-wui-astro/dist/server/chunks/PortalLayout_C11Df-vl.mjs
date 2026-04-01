import { e as createComponent, n as renderHead, r as renderTemplate, o as renderSlot, h as createAstro } from './astro/server_BTiD7zzO.mjs';
import 'piccolore';
import 'clsx';
/* empty css                           */

const $$Astro = createAstro();
const $$PortalLayout = createComponent(($$result, $$props, $$slots) => {
  const Astro2 = $$result.createAstro($$Astro, $$props, $$slots);
  Astro2.self = $$PortalLayout;
  const { title } = Astro2.props;
  const user = Astro2.locals.user;
  return renderTemplate`<html lang="en"> <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"><title>${title} — ETERNA Archive</title><link rel="icon" type="image/x-icon" href="/favicon.ico">${renderHead()}</head> <body> <header class="bg-[var(--color-primary)] text-white shadow-md"> <div class="max-w-[1400px] mx-auto px-4 h-14 flex items-center gap-4"> <a href="/portal" class="font-bold text-lg text-white hover:no-underline flex items-center gap-2"> <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"> <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M3 7a2 2 0 012-2h14a2 2 0 012 2v10a2 2 0 01-2 2H5a2 2 0 01-2-2V7z"></path> </svg>
ETERNA Archive
</a> <nav class="flex gap-3 flex-1"> <a href="/portal" class="text-sm text-white/80 hover:text-white px-2 py-1 rounded hover:bg-white/10">Home</a> <a href="/portal/search" class="text-sm text-white/80 hover:text-white px-2 py-1 rounded hover:bg-white/10">Search</a> <a href="/portal/browse" class="text-sm text-white/80 hover:text-white px-2 py-1 rounded hover:bg-white/10">Browse</a> </nav> ${user ? renderTemplate`<a href="/" class="text-sm text-white/80 hover:text-white">Admin</a>` : renderTemplate`<a href="/login" class="text-sm text-white/80 hover:text-white">Sign in</a>`} </div> </header> <main class="page-content"> ${renderSlot($$result, $$slots["default"])} </main> <footer class="text-center text-xs text-gray-400 py-6 border-t border-gray-200 mt-8">
ETERNA &mdash; Digital Preservation Repository
</footer> </body></html>`;
}, "/home/jerry/Git/eterna-migrate-to-astro/roda-ui/roda-wui-astro/src/components/layout/PortalLayout.astro", void 0);

export { $$PortalLayout as $ };

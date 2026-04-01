import { e as createComponent, n as renderHead, o as renderSlot, r as renderTemplate, h as createAstro } from './astro/server_BTiD7zzO.mjs';
import 'piccolore';
import 'clsx';
/* empty css                           */

const $$Astro = createAstro();
const $$AuthLayout = createComponent(($$result, $$props, $$slots) => {
  const Astro2 = $$result.createAstro($$Astro, $$props, $$slots);
  Astro2.self = $$AuthLayout;
  const { title } = Astro2.props;
  return renderTemplate`<html lang="en"> <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"><title>${title} — ETERNA</title><link rel="icon" type="image/x-icon" href="/favicon.ico">${renderHead()}</head> <body class="min-h-screen bg-gray-100 flex items-center justify-center"> <div class="w-full max-w-md"> <div class="text-center mb-8"> <a href="/" class="inline-flex items-center gap-2 text-2xl font-bold text-[var(--color-primary)] no-underline"> <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24"> <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M3 7a2 2 0 012-2h14a2 2 0 012 2v10a2 2 0 01-2 2H5a2 2 0 01-2-2V7z"></path> </svg>
ETERNA
</a> </div> <div class="bg-white rounded-lg shadow-md p-8"> ${renderSlot($$result, $$slots["default"])} </div> </div> </body></html>`;
}, "/home/jerry/Git/eterna-migrate-to-astro/roda-ui/roda-wui-astro/src/components/layout/AuthLayout.astro", void 0);

export { $$AuthLayout as $ };

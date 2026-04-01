import { renderers } from './renderers.mjs';
import { c as createExports, s as serverEntrypointModule } from './chunks/_@astrojs-ssr-adapter_8pbOofSh.mjs';
import { manifest } from './manifest_IbUkTCMC.mjs';

const serverIslandMap = new Map();;

const _page0 = () => import('./pages/_image.astro.mjs');
const _page1 = () => import('./pages/auth/profile.astro.mjs');
const _page2 = () => import('./pages/auth/recover.astro.mjs');
const _page3 = () => import('./pages/auth/register.astro.mjs');
const _page4 = () => import('./pages/browse/_aipid_.astro.mjs');
const _page5 = () => import('./pages/browse.astro.mjs');
const _page6 = () => import('./pages/disposal/confirmations.astro.mjs');
const _page7 = () => import('./pages/disposal/destroyed.astro.mjs');
const _page8 = () => import('./pages/disposal/policy.astro.mjs');
const _page9 = () => import('./pages/disposal.astro.mjs');
const _page10 = () => import('./pages/help.astro.mjs');
const _page11 = () => import('./pages/ingest/appraisal.astro.mjs');
const _page12 = () => import('./pages/ingest/pre.astro.mjs');
const _page13 = () => import('./pages/ingest/process.astro.mjs');
const _page14 = () => import('./pages/ingest/transfer.astro.mjs');
const _page15 = () => import('./pages/ingest.astro.mjs');
const _page16 = () => import('./pages/login.astro.mjs');
const _page17 = () => import('./pages/management/actions.astro.mjs');
const _page18 = () => import('./pages/management/internal.astro.mjs');
const _page19 = () => import('./pages/management/log.astro.mjs');
const _page20 = () => import('./pages/management/members.astro.mjs');
const _page21 = () => import('./pages/management/notifications.astro.mjs');
const _page22 = () => import('./pages/management/statistics.astro.mjs');
const _page23 = () => import('./pages/management.astro.mjs');
const _page24 = () => import('./pages/planning/agents.astro.mjs');
const _page25 = () => import('./pages/planning/events.astro.mjs');
const _page26 = () => import('./pages/planning/incidences.astro.mjs');
const _page27 = () => import('./pages/planning/representation-information.astro.mjs');
const _page28 = () => import('./pages/planning/risks.astro.mjs');
const _page29 = () => import('./pages/planning.astro.mjs');
const _page30 = () => import('./pages/portal/browse.astro.mjs');
const _page31 = () => import('./pages/portal/search.astro.mjs');
const _page32 = () => import('./pages/portal.astro.mjs');
const _page33 = () => import('./pages/search.astro.mjs');
const _page34 = () => import('./pages/index.astro.mjs');
const pageMap = new Map([
    ["node_modules/astro/dist/assets/endpoint/node.js", _page0],
    ["src/pages/auth/profile.astro", _page1],
    ["src/pages/auth/recover.astro", _page2],
    ["src/pages/auth/register.astro", _page3],
    ["src/pages/browse/[aipId].astro", _page4],
    ["src/pages/browse/index.astro", _page5],
    ["src/pages/disposal/confirmations/index.astro", _page6],
    ["src/pages/disposal/destroyed.astro", _page7],
    ["src/pages/disposal/policy.astro", _page8],
    ["src/pages/disposal/index.astro", _page9],
    ["src/pages/help.astro", _page10],
    ["src/pages/ingest/appraisal.astro", _page11],
    ["src/pages/ingest/pre.astro", _page12],
    ["src/pages/ingest/process/index.astro", _page13],
    ["src/pages/ingest/transfer/index.astro", _page14],
    ["src/pages/ingest/index.astro", _page15],
    ["src/pages/login.astro", _page16],
    ["src/pages/management/actions.astro", _page17],
    ["src/pages/management/internal.astro", _page18],
    ["src/pages/management/log/index.astro", _page19],
    ["src/pages/management/members/index.astro", _page20],
    ["src/pages/management/notifications/index.astro", _page21],
    ["src/pages/management/statistics.astro", _page22],
    ["src/pages/management/index.astro", _page23],
    ["src/pages/planning/agents/index.astro", _page24],
    ["src/pages/planning/events/index.astro", _page25],
    ["src/pages/planning/incidences/index.astro", _page26],
    ["src/pages/planning/representation-information/index.astro", _page27],
    ["src/pages/planning/risks/index.astro", _page28],
    ["src/pages/planning/index.astro", _page29],
    ["src/pages/portal/browse/index.astro", _page30],
    ["src/pages/portal/search.astro", _page31],
    ["src/pages/portal/index.astro", _page32],
    ["src/pages/search/index.astro", _page33],
    ["src/pages/index.astro", _page34]
]);

const _manifest = Object.assign(manifest, {
    pageMap,
    serverIslandMap,
    renderers,
    actions: () => import('./noop-entrypoint.mjs'),
    middleware: () => import('./_astro-internal_middleware.mjs')
});
const _args = {
    "mode": "standalone",
    "client": "file:///home/jerry/Git/eterna-migrate-to-astro/roda-ui/roda-wui-astro/dist/client/",
    "server": "file:///home/jerry/Git/eterna-migrate-to-astro/roda-ui/roda-wui-astro/dist/server/",
    "host": "0.0.0.0",
    "port": 4321,
    "assets": "_astro",
    "experimentalStaticHeaders": false
};
const _exports = createExports(_manifest, _args);
const handler = _exports['handler'];
const startServer = _exports['startServer'];
const options = _exports['options'];
const _start = 'start';
if (Object.prototype.hasOwnProperty.call(serverEntrypointModule, _start)) {
	serverEntrypointModule[_start](_manifest, _args);
}

export { handler, options, pageMap, startServer };

import { d as defineMiddleware, s as sequence } from './chunks/index_D_7L3yns.mjs';
import { a as apiGet } from './chunks/client_BbTdbxg1.mjs';
import 'es-module-lexer';
import './chunks/astro-designed-error-pages_CX09QLHj.mjs';
import 'piccolore';
import './chunks/astro/server_BTiD7zzO.mjs';
import 'clsx';

const PUBLIC_PATHS = /* @__PURE__ */ new Set(["/login", "/auth/register", "/auth/recover", "/auth/reset-password", "/auth/set-password", "/auth/verify-email"]);
const PORTAL_PREFIX = "/portal";
const onRequest$1 = defineMiddleware(async (context, next) => {
  const { pathname } = context.url;
  if (pathname.startsWith("/_astro/") || pathname.startsWith("/api/") || pathname.startsWith("/webjars/")) {
    return next();
  }
  if (PUBLIC_PATHS.has(pathname)) {
    context.locals.user = null;
    return next();
  }
  const cookieHeader = context.request.headers.get("cookie") ?? void 0;
  const isPortal = pathname.startsWith(PORTAL_PREFIX);
  if (!cookieHeader) {
    if (isPortal) {
      context.locals.user = null;
      return next();
    }
    return context.redirect(`/login?redirect=${encodeURIComponent(pathname)}`);
  }
  try {
    const user = await apiGet("/members/users/authenticated", { cookie: cookieHeader });
    context.locals.user = {
      id: user.id,
      name: user.name,
      fullname: user.fullname,
      email: user.email,
      roles: user.roles ?? [],
      groups: user.groups ?? [],
      isActive: user.active,
      isGuest: user.guest
    };
  } catch {
    context.locals.user = null;
    if (!isPortal) {
      return context.redirect(`/login?redirect=${encodeURIComponent(pathname)}`);
    }
  }
  return next();
});

const onRequest = sequence(
	
	onRequest$1
	
);

export { onRequest };

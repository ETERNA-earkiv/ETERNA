import { defineMiddleware } from "astro:middleware";
import { apiGet } from "@/lib/api/client";

/** Routes that do not require authentication */
const PUBLIC_PATHS = new Set(["/login", "/auth/register", "/auth/recover", "/auth/reset-password", "/auth/set-password", "/auth/verify-email"]);

/** Portal routes allow guest (unauthenticated) access */
const PORTAL_PREFIX = "/portal";

export const onRequest = defineMiddleware(async (context, next) => {
  const { pathname } = context.url;

  // Always allow static assets and API passthrough
  if (
    pathname.startsWith("/_astro/") ||
    pathname.startsWith("/api/") ||
    pathname.startsWith("/webjars/")
  ) {
    return next();
  }

  // Legacy GWT entry points — redirect to root so the hash-redirect script can run
  if (pathname === "/Main.html" || pathname === "/Portal.html") {
    return context.redirect("/", 302);
  }

  // Public auth pages — no session needed
  if (PUBLIC_PATHS.has(pathname)) {
    context.locals.user = null;
    return next();
  }

  // Resolve the session cookie for server-side API calls
  const cookieHeader = context.request.headers.get("cookie") ?? undefined;

  // Portal: allow guests but still try to resolve user
  const isPortal = pathname.startsWith(PORTAL_PREFIX);

  if (!cookieHeader) {
    if (isPortal) {
      context.locals.user = null;
      return next();
    }
    return context.redirect(`/login?redirect=${encodeURIComponent(pathname)}`);
  }

  try {
    const user = await apiGet<{
      id: string;
      name: string;
      fullname: string;
      email: string;
      roles: string[];
      groups: string[];
      active: boolean;
      guest: boolean;
    }>("/members/users/authenticated", { cookie: cookieHeader });

    context.locals.user = {
      id: user.id,
      name: user.name,
      fullname: user.fullname,
      email: user.email,
      roles: user.roles ?? [],
      groups: user.groups ?? [],
      isActive: user.active,
      isGuest: user.guest,
    };
  } catch {
    context.locals.user = null;
    if (!isPortal) {
      return context.redirect(`/login?redirect=${encodeURIComponent(pathname)}`);
    }
  }

  return next();
});

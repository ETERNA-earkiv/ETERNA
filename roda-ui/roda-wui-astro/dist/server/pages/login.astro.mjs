import { e as createComponent, k as renderComponent, r as renderTemplate, h as createAstro, m as maybeRenderHead } from '../chunks/astro/server_BTiD7zzO.mjs';
import 'piccolore';
import { $ as $$AuthLayout } from '../chunks/AuthLayout_CvBeulq5.mjs';
import { jsxs, jsx } from 'react/jsx-runtime';
import { useState } from 'react';
export { renderers } from '../renderers.mjs';

function LoginForm({ redirectTo }) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);
  async function handleSubmit(e) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const res = await fetch("/api/v2/members/users/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify({ username, password })
      });
      if (res.ok) {
        window.location.href = redirectTo;
      } else {
        let message = "Invalid username or password.";
        try {
          const body = await res.json();
          message = body.message ?? message;
        } catch {
        }
        setError(message);
      }
    } catch {
      setError("Unable to connect. Please try again.");
    } finally {
      setLoading(false);
    }
  }
  return /* @__PURE__ */ jsxs("form", { onSubmit: handleSubmit, className: "space-y-4", children: [
    error && /* @__PURE__ */ jsx("div", { className: "bg-red-50 border border-red-200 text-red-700 text-sm rounded px-3 py-2", children: error }),
    /* @__PURE__ */ jsxs("div", { children: [
      /* @__PURE__ */ jsx("label", { htmlFor: "username", className: "block text-sm font-medium text-gray-700 mb-1", children: "Username" }),
      /* @__PURE__ */ jsx(
        "input",
        {
          id: "username",
          type: "text",
          required: true,
          autoComplete: "username",
          value: username,
          onChange: (e) => setUsername(e.target.value),
          className: "w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent",
          disabled: loading
        }
      )
    ] }),
    /* @__PURE__ */ jsxs("div", { children: [
      /* @__PURE__ */ jsx("label", { htmlFor: "password", className: "block text-sm font-medium text-gray-700 mb-1", children: "Password" }),
      /* @__PURE__ */ jsx(
        "input",
        {
          id: "password",
          type: "password",
          required: true,
          autoComplete: "current-password",
          value: password,
          onChange: (e) => setPassword(e.target.value),
          className: "w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent",
          disabled: loading
        }
      )
    ] }),
    /* @__PURE__ */ jsx(
      "button",
      {
        type: "submit",
        disabled: loading,
        className: "w-full bg-[var(--color-primary)] text-white py-2 px-4 rounded text-sm font-medium hover:bg-[var(--color-primary-dark)] disabled:opacity-50 disabled:cursor-not-allowed transition-colors",
        children: loading ? "Signing in…" : "Sign in"
      }
    )
  ] });
}

const $$Astro = createAstro();
const $$Login = createComponent(($$result, $$props, $$slots) => {
  const Astro2 = $$result.createAstro($$Astro, $$props, $$slots);
  Astro2.self = $$Login;
  if (Astro2.locals.user && !Astro2.locals.user.isGuest) {
    return Astro2.redirect(Astro2.url.searchParams.get("redirect") ?? "/");
  }
  return renderTemplate`${renderComponent($$result, "AuthLayout", $$AuthLayout, { "title": "Sign in" }, { "default": ($$result2) => renderTemplate` ${maybeRenderHead()}<h1 class="text-xl font-semibold text-gray-800 mb-6 text-center">Sign in to ETERNA</h1> ${renderComponent($$result2, "LoginForm", LoginForm, { "redirectTo": Astro2.url.searchParams.get("redirect") ?? "/", "client:load": true, "client:component-hydration": "load", "client:component-path": "@/components/auth/LoginForm.tsx", "client:component-export": "default" })} <div class="mt-4 text-center text-sm"> <a href="/auth/recover" class="text-[var(--color-primary)] hover:underline">Forgot password?</a> </div> <div class="mt-2 text-center text-sm text-gray-500">
Don't have an account?${" "} <a href="/auth/register" class="text-[var(--color-primary)] hover:underline">Register</a> </div> ` })}`;
}, "/home/jerry/Git/eterna-migrate-to-astro/roda-ui/roda-wui-astro/src/pages/login.astro", void 0);

const $$file = "/home/jerry/Git/eterna-migrate-to-astro/roda-ui/roda-wui-astro/src/pages/login.astro";
const $$url = "/login";

const _page = /*#__PURE__*/Object.freeze(/*#__PURE__*/Object.defineProperty({
  __proto__: null,
  default: $$Login,
  file: $$file,
  url: $$url
}, Symbol.toStringTag, { value: 'Module' }));

const page = () => _page;

export { page };

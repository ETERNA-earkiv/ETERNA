import { defineConfig } from "astro/config";
import react from "@astrojs/react";
import node from "@astrojs/node";
import tailwindcss from "@tailwindcss/vite";

export default defineConfig({
  output: "server",
  adapter: node({ mode: "standalone" }),
  integrations: [react()],
  server: {
    port: 4321,
    host: "0.0.0.0",
  },
  vite: {
    plugins: [tailwindcss()],
    server: {
      proxy: {
        "/api": {
          target: "http://localhost:8080",
          changeOrigin: true,
          configure: (proxy) => {
            // Strip "Secure" from Set-Cookie headers so cookies work over plain HTTP in dev
            proxy.on("proxyRes", (proxyRes) => {
              const cookies = proxyRes.headers["set-cookie"];
              if (cookies) {
                proxyRes.headers["set-cookie"] = cookies.map((c) =>
                  c.replace(/;\s*Secure/gi, "").replace(/;\s*SameSite=Strict/gi, "; SameSite=Lax")
                );
              }
            });
          },
        },
        "/webjars": {
          target: "http://localhost:8080",
          changeOrigin: true,
        },
        "/logout": {
          target: "http://localhost:8080",
          changeOrigin: true,
        },
      },
    },
  },
});

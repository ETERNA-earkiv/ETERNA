import { defineConfig } from 'astro/config';
import react from '@astrojs/react';
import node from '@astrojs/node';

export default defineConfig({
  integrations: [react()],
  output: 'server',
  adapter: node({ mode: 'standalone' }),
  devToolbar: { enabled: false },
  server: {
    host: true,
    port: 4321,
  },
  vite: {
    server: {
      strictPort: true,
      hmr: false,
      proxy: {
        // /api/v2 hanteras nu av Astro catch-all route med service account auth
        '/api/portal': {
          target: 'http://localhost:3000',
          changeOrigin: true,
        },
      },
    },
  },
});

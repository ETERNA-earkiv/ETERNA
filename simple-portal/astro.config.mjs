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
    // Förbundla de lazy-laddade nedladdningsberoendena redan vid uppstart.
    // Annars upptäcker Vite dem först vid första "Öppna och ladda ner fil",
    // optimerar om och byter dep-hash — och eftersom hmr är avstängd kan den
    // inte be webbläsaren ladda om, så importen failar mitt i klicket.
    optimizeDeps: {
      include: ['jspdf', 'jszip', 'file-saver'],
    },
    server: {
      strictPort: true,
      hmr: false,
      proxy: {
        // /api/v2 hanteras av Astro catch-all route (anonymt → RODA guest,
        // inloggad → egen session)
        '/api/portal': {
          target: 'http://localhost:3000',
          changeOrigin: true,
        },
      },
    },
  },
});

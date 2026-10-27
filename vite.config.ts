import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Prevent Vite from exiting when stdin is closed in non-interactive container environments
process.env.CI = 'true';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    host: '0.0.0.0',
    hmr: false
  },
  preview: {
    port: 3000,
    host: '0.0.0.0'
  },
  build: {
    outDir: 'dist',
    sourcemap: false
  }
});

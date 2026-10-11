import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  base: './',
  build: {
    outDir: path.resolve(__dirname, '../docs'),
    emptyOutDir: false,
  },
  server: {
    port: 5173,
    open: false,
  }
});

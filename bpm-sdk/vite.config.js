import { defineConfig } from 'vite';
import path from 'node:path';

export default defineConfig({
  build: {
    lib: {
      entry: path.resolve(__dirname, 'index.js'),
      name: 'BpmSdk',
      fileName: (format) => `bpm-sdk.${format}.js`,
    },
    outDir: 'dist',
    emptyOutDir: true,
    rollupOptions: {
      // Externalize dependencies if needed
      external: [],
      output: {
        globals: {},
      },
    },
  },
});

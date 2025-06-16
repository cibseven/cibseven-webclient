import { defineConfig } from 'vite';
import path from 'node:path';

export default defineConfig({
  build: {
    lib: {
      entry: path.resolve(__dirname, 'index.js'),
      name: 'CamundaBpmSdk',
      fileName: (format) => `camunda-bpm-sdk.${format}.js`,
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

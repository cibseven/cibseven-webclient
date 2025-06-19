import { defineConfig } from 'vite';
import path from 'node:path';

// Detect build mode
const isLibrary = process.env.BUILD_MODE === 'library';

console.log('isLibrary', isLibrary);

export default defineConfig({
  build: isLibrary
    ? {
        lib: {
          entry: path.resolve(__dirname, 'index.js'),
          name: 'BpmSdk',
          formats: ['es', 'umd'],
          fileName: (format) => `bpm-sdk.${format}.js`,
        },
        outDir: 'dist',
        emptyOutDir: true,
        rollupOptions: {
          external: [], // Add external deps here if needed
          output: {
            globals: {}, // Add globals here if needed
            assetFileNames: 'bpm-sdk.[ext]',
          },
        },
      }
    : {},
});

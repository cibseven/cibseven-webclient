import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// The build writes straight into the layout a plugin jar needs, so packaging is
// only 'jar --create --file <name>.jar -C dist META-INF'. Everything in public/
// (the manifest, translations) is copied along.
export default defineConfig({
  plugins: [vue()],
  publicDir: 'public',
  build: {
    outDir: 'dist/META-INF/cibseven-plugins/demo-report',
    emptyOutDir: true,
    lib: {
      entry: 'src/index.js',
      formats: ['es'],
      fileName: () => 'index.js'
    },
    rollupOptions: {
      // Provided by the webclient through the import map. Bundling any of them
      // would load a second copy, and a second Vue breaks reactivity, inject and
      // the slot registry - usually without an error.
      external: ['vue', 'axios', 'bootstrap', '@cibseven/plugin-runtime'],
      output: {
        // matches the "styles" entry of plugin.json
        assetFileNames: 'styles.css'
      }
    }
  }
})

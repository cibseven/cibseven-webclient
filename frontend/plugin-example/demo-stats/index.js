/*
 * Example webclient plugin. Deliberately hand-written plain ES module with no
 * build step, to demonstrate that a plugin does not take part in the webclient's
 * Vite compilation: this file is fetched and evaluated at runtime.
 *
 * Everything it needs comes from '@cibseven/plugin-runtime', which an import map
 * in index.html resolves to the webclient's own runtime entry. That is what
 * makes 'vue' below the *same* Vue instance the application uses - importing
 * 'vue' from the plugin's own bundle would create a second runtime, and
 * reactivity would not cross the boundary.
 *
 * The 'template' option below is compiled in the browser, which works because
 * the webclient bundles the full Vue build (see the 'vue' alias in
 * vite.config.js). A plugin that prefers single-file components can be built
 * with Vite instead, keeping 'vue' external - it then ships precompiled render
 * functions and needs no compiler at runtime.
 */
import { vue, services, registerPlugin, getContext, getRuntimeInfo } from '@cibseven/plugin-runtime'

const DemoStats = vue.defineComponent({
  name: 'DemoStats',
  // The slot hands every contribution the props declared by its host, here the
  // process instance the user is looking at.
  props: {
    instance: { type: Object, default: null },
    process: { type: Object, default: null }
  },
  template: `
    <div class="container-fluid px-4 p-4">
      <h5>{{ $t('plugins.demo-stats.title') }}</h5>
      <p class="mb-1">{{ $t('plugins.demo-stats.instance') }}: {{ instance?.id ?? '-' }}</p>
      <p class="mb-1">{{ $t('plugins.demo-stats.definition') }}: {{ process?.key ?? '-' }}</p>
      <p class="mb-1">{{ $t('plugins.demo-stats.theme') }}: {{ theme }}</p>
      <p class="mb-1">{{ $t('plugins.demo-stats.apiVersion') }}: {{ apiVersion }}</p>
      <p v-if="error" class="mb-0 text-danger">
        {{ $t('plugins.demo-stats.failed') }}: {{ error }}
      </p>
      <p v-else class="mb-0">
        {{ $t('plugins.demo-stats.processes') }}: {{ processCount ?? '…' }}
      </p>
    </div>
  `,
  data() {
    return {
      // Reads the application configuration handed over through the context
      theme: getContext().config?.theme ?? 'unknown',
      apiVersion: getRuntimeInfo().apiVersion,
      processCount: null,
      error: null
    }
  },
  async mounted() {
    try {
      // Uses the application's axios instance, so this request carries the
      // logged-in user's authentication and goes through the configured
      // services base path. A user without rights gets a 403 here.
      const processes = await services.ProcessService.findProcesses()
      this.processCount = Array.isArray(processes) ? processes.length : 0
    } catch (error) {
      this.error = error?.response?.status ?? 'error'
    }
  }
})

export function register({ id }) {
  // 'id' is the tab this contribution belongs to and is what appears in the URL
  // as ?tab=demo-stats; 'text' is a translation key, resolved from the plugin's
  // own translation files under the 'plugins.<pluginId>' namespace.
  registerPlugin('process-instance-tab', DemoStats, {
    pluginId: id,
    id: 'demo-stats',
    text: `plugins.${id}.title`
  })
}

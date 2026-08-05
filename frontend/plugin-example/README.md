# Example webclient plugin

Proof of concept for runtime-loaded frontend plugins. The plugin in
`plugins/demo-stats/` is a plain ES module with **no build step**: it is fetched
and evaluated after the webclient has been compiled and deployed, which is the
constraint that rules out Camunda's "drop a JS file into a folder" approach.

## How it fits together

| Piece | Where |
|---|---|
| Runtime API plugins compile against | `src/plugin-runtime.js` (separate entry of the app build) |
| Import map resolving `@cibseven/plugin-runtime` and `vue` | injected into `index.html` by `vite.config.js` |
| Classpath discovery and serving | `PluginRegistry.java`, `PluginService.java`, `PluginResourceConfiguration.java` (in `cibseven-webclient-core`) |
| Manifest discovery and loading | `src/plugins/pluginLoader.js`, called from `src/app.js` |
| Slot registry | `src/plugins/pluginsConfig.js` |
| Rendering, with per-plugin error isolation | `src/components/common/PluginSlot.vue`, `PluginBoundary.vue` |

## Slots

| Slot | Contributes | Params handed to the contribution |
|---|---|---|
| `process-instance-tab` | one tab of a process instance, `id` becomes `?tab=<id>` | `instance`, `process`, `tenantId` |

A contribution registers the tab label and its content in one call: the tab bar
in `ProcessInstanceTabs.vue` reads `id` and `text`, and `ProcessInstanceView.vue`
renders the component of whichever contribution matches the active tab.

In the enterprise webclient the tab bar is replaced by
`ProcessInstanceTabsPlugin.vue`, an older *build-time* extension point injected
through `overlayComponents.js`. Plugin tabs therefore appear in CE today; making
them appear in EE means the same few lines in that component.

The runtime is an entry of the *application* build on purpose. Because it is
bundled together with `app.js`, both share the same chunks, so a plugin importing
`vue` from it gets the application's Vue instance. A plugin that bundled its own
Vue would get a second runtime, and reactivity, `provide`/`inject` and the slot
registry would not cross that boundary.

## Deploying a plugin

Plugins are switched off until the backend is told otherwise:

```yaml
cibseven:
  webclient:
    plugins:
      enabled: true
```

A plugin is then a folder on the classpath - normally shipped inside a jar:

```
META-INF/cibseven-plugins/demo-stats/plugin.json
META-INF/cibseven-plugins/demo-stats/index.js
META-INF/cibseven-plugins/demo-stats/translations_en.json
```

`plugin.json` holds `entry`, `apiVersion` and optionally `slots` and
`translations`. The **folder name is the plugin id**, and an `id` inside the file
is ignored: the folder is where the files are served from, so it decides the
identity.

Note the two manifest formats, one per discovery source:

- `plugins/demo-stats/plugin.json` - one file per plugin, read by the backend
  from the classpath. No `id`, it comes from the folder.
- `plugins.json` - the whole list in one file, only used as the fallback for a
  frontend running without the backend. Here each entry needs its `id`. `GET /info/plugins` lists what was found, and the files are served under
`/plugins/<id>/…`. Several plugin jars can be deployed together; each contributes
its own folder. Nothing here depends on a filesystem layout, so a war and a Spring
Boot jar behave the same.

## Trying it locally

1. Either package the example as described above, or - without a backend - copy it
   next to the application's static files, which the loader falls back to:

   ```sh
   # development
   cp -r plugin-example/plugins.json plugin-example/plugins public/
   # or, against a build
   cp -r plugin-example/plugins.json plugin-example/plugins dist/
   ```

2. Start the webclient, log in and open any process instance. The console logs
   `Plugin "demo-stats" loaded`, a **Demo plugin** tab appears after the built-in
   tabs, and its content shows the instance id and definition key handed over by
   the slot, the theme taken from the application config, the plugin API version,
   and the number of processes the logged-in user may see - fetched with the
   application's axios instance, so it carries the user's authentication and
   returns 403 if rights are missing.

3. The tab is deep-linkable like any built-in one, because `tabUrlMixin` keeps
   the active tab in the URL: append `?tab=demo-stats` to a process instance URL.

Without `plugins.json` deployed, nothing of this runs: the manifest request 404s,
the loader stays silent and the application behaves exactly as before.

## Writing a plugin

A plugin needs a manifest entry and a module exporting `register`:

```json
{ "plugins": [ { "id": "my-plugin", "entry": "index.js", "apiVersion": "1",
  "translations": { "en": "translations_en.json" } } ] }
```

```js
import { vue, services, registerPlugin, getContext } from '@cibseven/plugin-runtime'

export function register({ id }) {
  registerPlugin('demo', vue.defineComponent({ /* ... */ }), { pluginId: id })
}
```

Rules that come with it:

- `apiVersion` must match `PLUGIN_API_VERSION`; the loader rejects anything else
  instead of failing in confusing ways later.
- Translations are merged under `plugins.<id>.*` and cannot collide with
  application or theme keys.
- Never bundle `vue`, `axios` or `bootstrap` - declare them external and let the
  import map resolve them, or you get a second instance.
- Plugin code is loaded inside our page: it can only be as trustworthy as the
  party that deployed it on the classpath.

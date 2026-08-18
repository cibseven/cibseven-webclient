# Webclient plugins

Frontend plugins let a deployment add UI to the webclient without forking it. A
plugin is a folder of JavaScript on the backend classpath: it is fetched and
evaluated at runtime, after the webclient has been built, and renders into
defined slots.

A worked example lives in [frontend/plugin-example](frontend/plugin-example).

## Enabling

Plugins are off unless the backend is told otherwise:

```yaml
cibseven:
  webclient:
    plugins:
      enabled: true
```

With the property off the frontend does not even ask for plugins, and no plugin
file is served.

## Deploying a plugin

A plugin is a folder below `META-INF/cibseven-plugins/` on the classpath,
normally shipped inside a jar:

```
META-INF/cibseven-plugins/demo-report/plugin.json
META-INF/cibseven-plugins/demo-report/index.js
META-INF/cibseven-plugins/demo-report/styles.css
META-INF/cibseven-plugins/demo-report/translations_en.json
```

The **folder name is the plugin id**. An `id` inside the manifest is ignored,
because the folder decides where the files are served from. Two folders with the
same name are a conflict: only the first is loaded and the second is logged and
skipped, since both would be served from one location.

Two separate things decide that a plugin is picked up, and both are needed:

- **where the jar is put** - that is what places it on the application's
  classpath, and it is the distribution's mechanism, the same one used for JDBC
  drivers and engine plugins
- **the path inside the jar** - that is what marks it as a plugin, and it is ours:
  the classpath is searched for `META-INF/cibseven-plugins/*/plugin.json`, and the
  same location is what the files are then served from

A jar in the right place without that folder loads and does nothing.

### Packaging

Building the plugin produces that layout already, so packaging is one command:

```sh
npm run package     # builds, then jars dist/META-INF into <name>.jar
```

**One jar per plugin.** Its name and version are the plugin author's business;
only the folder name identifies the plugin, and it has to be unique across
everything on the classpath.

### Where the jar goes

Installing a plugin is the same gesture as installing a JDBC driver or an engine
plugin - putting a jar where the distribution already looks:

| Distribution | Where the plugin jar goes |
|---|---|
| CIB seven Run | `configuration/userlib` |
| Tomcat, WildFly | the webapp's `WEB-INF/lib` |
| Docker, Kubernetes | into `configuration/userlib` of the image, by deriving an image or mounting a volume |
| Webclient embedded in an application | a dependency of that application |

| Endpoint | Purpose |
|---|---|
| `GET /info/plugins` | manifests of the plugins found on the classpath |
| `GET /plugins/<id>/…` | the plugin's own files |

## Writing a plugin

A plugin is a small Vite project of single-file components.
[frontend/plugin-example/demo-report](frontend/plugin-example/demo-report) is the
starter: copy the folder, rename it, write your components.

```
demo-report/
├── package.json
├── vite.config.js              # keep as it is, see below
├── public/
│   ├── plugin.json             # the manifest, copied into the build
│   ├── translations_en.json
│   └── translations_de.json
└── src/
    ├── index.js                # exports register()
    ├── styles.css              # styles shared by the components
    ├── DemoReport.vue          # registered into the slot
    ├── ProcessTable.vue        # ordinary child components
    └── SummaryCard.vue
```

Components are split as in any Vue project - only the one passed to
`registerPlugin` is special. Everything is bundled into a single `index.js`, and
all `<style>` blocks plus any imported CSS into a single `styles.css`.

`npm install`, then `npm run package`, and the jar is ready to deploy. The build
writes into `dist/META-INF/cibseven-plugins/<id>/`, which is exactly the layout
described above.

`plugin.json`:

```json
{
  "entry": "index.js",
  "apiVersion": "1",
  "slots": ["process-instance-tab"],
  "translations": { "en": "translations_en.json" }
}
```

| Field | Meaning |
|---|---|
| `entry` | module to import, relative to the plugin folder |
| `apiVersion` | must match the webclient's `PLUGIN_API_VERSION`, otherwise the plugin is refused |
| `slots` | documentation only; what is rendered is decided by `registerPlugin` |
| `translations` | per language, merged under `plugins.<id>.*` |
| `styles` | stylesheets of the plugin, added to the page before it registers |

`src/index.js` exports `register`, called once during startup:

```js
import { registerPlugin } from '@cibseven/plugin-runtime'
import DemoReport from './DemoReport.vue'

export function register({ id }) {
  registerPlugin('process-instance-tab', DemoReport, {
    pluginId: id,
    id: 'demo-report',                // becomes ?tab=demo-report
    text: `plugins.${id}.title`       // translation key
  })
}
```

The components themselves import what they need from the same module:

`@cibseven/plugin-runtime` hands over the application's own instances:

| Export | What it is |
|---|---|
| `vue`, and the bare `vue` exports | the application's Vue runtime |
| `services` | the webclient service objects (`ProcessService`, `TaskService`, …) |
| `axios` | the configured instance, carrying the user's authentication |
| `getContext()` | `config` and the Vuex store |
| `registerPlugin`, `getPlugin` | the slot registry |
| `i18n`, `mergeTranslations` | translations, namespaced per plugin |

### The one line that must not change

```js
// vite.config.js
rollupOptions: {
  external: ['vue', 'axios', 'bootstrap', '@cibseven/plugin-runtime']
}
```

A compiled component imports its helpers from `vue` by name, and the import map
resolves them to the webclient's instance. Bundling Vue instead gives the plugin a
second runtime, across which reactivity, `provide`/`inject` and the slot registry
silently stop working. Use the same Vue version as the webclient, since compiled
output and runtime belong together.

A `.vue` file is never deployed: the browser cannot parse it, so what ships is
always the built output.

## Styling

Components can use the application's Bootstrap and theme classes, which are
already loaded. Styles from `<style>` blocks are built into one file that the
manifest lists under `styles`, and it is added to the page before the plugin
registers anything - the starter is already set up that way.

Nothing scopes those styles, so prefix your selectors with something belonging to
the plugin rather than styling shared elements.

## When changes take effect

Discovery happens once: the classpath is scanned on the first request and the
served locations are resolved while the application context starts.

| Change | What is needed |
|---|---|
| a plugin added or removed | restart the backend, then reload the page |
| `plugin.json` changed, or `plugins.enabled` toggled | restart the backend, then reload the page |
| a deployed plugin's own files edited in place | reload the page; plugin files are served without caching |

## Slots

| Slot | Contributes | Props handed to the contribution |
|---|---|---|
| `process-instance-tab` | one tab of a process instance; the registered `id` becomes `?tab=<id>` | `instance`, `process`, `tenantId` |
| `decision-definition-tab` | one tab of a decision definition version | `decision`, `tenantId` |

One registration carries both the tab label and its content: the tab bar reads
`id` and `text`, and the view renders whichever contribution matches the active
tab. Slots are added on demand rather than up front.

In the enterprise webclient the tab bar is replaced by
`ProcessInstanceTabsPlugin.vue`, an older build-time extension point, so plugin
tabs appear in CE today and EE needs the same few lines in that component.

## Compatibility

`PLUGIN_API_VERSION` is the contract between a plugin and the webclient. What
this document describes - the runtime exports, the slot names and their props,
the manifest fields - may not change incompatibly without raising it. A plugin
declaring a different version is refused at load time with a log entry, rather
than failing somewhere unpredictable later.

## Trust model and limits

**Plugin code is not sandboxed.** It is imported into the webclient page and has
everything the application has: the session, the DOM, the configured axios
instance, the services and the store. A plugin can do anything the logged-in user
can do. The slot registry and the API version are a compatibility contract, not a
security boundary.

**The control is deployment.** Plugins are only found on the backend classpath,
so installing one means adding a jar to the deployment - the same level of trust
as any other jar there, and an administrator's action rather than a user's. On
top of that, plugins are off by default.

What this does **not** change:

- **Data access.** Plugins reach data through the same authenticated endpoints,
  with the user's own permissions: a request the user is not allowed to make
  still returns 403. There is no direct database access.
- **Authentication.** Plugin files under `/plugins/**` are served without
  authentication on purpose - the code is not the secret, the data is - and the
  manifest list is readable before login, exposing plugin ids and file names only.
- **Availability.** A plugin that fails to load, exports no `register`, or throws
  while rendering is isolated: it is logged and dropped, and the rest of the
  application keeps working.

For whoever deploys a plugin: it is their code and their responsibility. It has
to be retested on every webclient update, and layout changes on our side can
affect content rendered next to ours.

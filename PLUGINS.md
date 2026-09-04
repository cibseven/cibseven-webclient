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

With the property off the backend wires no plugin beans at all: nothing is
scanned, the endpoints below do not exist, and the frontend does not even ask.

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

That script belongs to the starter; nothing depends on it. Any way of getting the
files to that path inside a jar does the same - `jar cf`, a Maven or Gradle copy.

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
| `GET <basePath>/plugins` | manifests of the plugins found on the classpath |
| `GET <basePath>/plugins/<id>/…` | the plugin's own files |

Both sit below `cibseven.webclient.services.basePath` like the rest of the API, and
below the application path the distribution mounts the webclient under -
`/webapp/services/v1/plugins` in CIB seven Run. The frontend reads the base path
from `/info` before it loads plugins and resolves the endpoints relative to its own
location, so this needs no configuration.

## Writing a plugin

What the webclient loads is an ES module: `entry` exports `register`, and `vue` and
`@cibseven/plugin-runtime` stay bare imports that the page's import map resolves to
the application's own instances. How that module is produced is the plugin author's
choice - Vite, Rollup, webpack, esbuild - and a plugin that needs no build at all
can ship a hand-written module:

```js
import { h } from 'vue'

export function register({ id, registerPlugin }) {
  registerPlugin('process-instance-tab', {
    name: 'MiniReport',
    props: { instance: { type: Object, default: null } },
    render() { return h('p', this.instance?.id) }
  }, { id: 'mini-report', text: `plugins.${id}.title` })
}
```

Single-file components are what needs a build, since `.vue` has to be compiled. The
starter does that with Vite, the tool the webclient itself is built with, so its
output and our runtime always belong together.
[frontend/plugin-example/demo-report](frontend/plugin-example/demo-report) is that
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
  "apiVersion": ["2.3"],
  "slots": ["process-instance-tab"],
  "translations": { "en": "translations_en.json" }
}
```

| Field | Meaning |
|---|---|
| `entry` | module to import, relative to the plugin folder |
| `apiVersion` | webclient lines this build was tested against, as `major.minor` - one of them has to be the webclient's own, otherwise the plugin is refused. A single string is accepted as well |
| `slots` | documentation only; what is rendered is decided by `registerPlugin` |
| `translations` | per language, merged under `plugins.<id>.*` |
| `styles` | stylesheets of the plugin, added to the page before it registers |

Translations are loaded per language, as the application loads its own, and a
plugin's file for a language is fetched before the application switches to it. A
component only uses `$t('plugins.<id>.…')` and follows the switch like any other
label; a language the manifest does not list falls back to the key.

`src/index.js` exports `register`, called once during startup with the plugin's id,
its base URL, and a `registerPlugin` bound to it:

```js
import DemoReport from './DemoReport.vue'

export function register({ id, registerPlugin }) {
  registerPlugin('process-instance-tab', DemoReport, {
    id: 'demo-report',                // becomes ?tab=demo-report
    text: `plugins.${id}.title`       // translation key
  })
}
```

Which plugin a contribution came from is not passed in but stamped on it, so it is
neither forgotten nor claimed for another plugin. The same goes for the key it is
rendered under, which is why registering several times is no problem.

The components themselves import what they need from the same module:

`@cibseven/plugin-runtime` hands over the application's own instances:

| Export | What it is |
|---|---|
| `vue`, and the bare `vue` exports | the application's Vue runtime |
| `services` | the webclient service objects (`ProcessService`, `TaskService`, …) |
| `axios` | the configured instance, carrying the user's authentication |
| `getContext()` | `config`, a frozen copy of how the application is configured |
| `navigation` | `push`, `replace` and `currentRoute()`, to send the user to another view |
| `registerPlugin`, `getPlugin` | the slot registry - prefer the `registerPlugin` handed to `register`, which knows the plugin's id |
| `i18n`, `mergeTranslations` | translations, namespaced per plugin |

Navigating is a capability rather than the router itself, so defining routes and
guards stays with the application and the contract does not depend on a
`vue-router` version:

```js
import { navigation } from '@cibseven/plugin-runtime'

navigation.push({ name: 'process', params: { processKey } })
navigation.currentRoute()   // a snapshot: { name, path, params, query, hash }
```

### What must never be bundled

```js
// vite.config.js in the starter; the same list under 'externals' in webpack,
// '--external:' in esbuild, or nothing at all in a plugin that is not bundled
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

The list is the set the application provides, and nothing else is shared. `axios`
and `bootstrap` are on it so that they can never be bundled: take the configured
axios from the runtime, since a bundled one would carry neither the user's
authentication nor the engine header. Importing either by name instead fails while
the plugin loads, which is the intended outcome - it is a mistake to be told about,
not to debug through missing headers.

### Other libraries

Anything else a plugin needs is its own dependency: add it to the plugin's
`package.json` and it is bundled into `index.js`.

```sh
npm i dmn-js         # in the plugin project
```

That is the supported way, including for `bpmn-js` and `dmn-js`. Those two carry no
shared state - a viewer is constructed against the plugin's own element - so a
second copy alongside the webclient's is correct, only larger. Do not add them to
the externals list: they are not provided through the import map, and the plugin
would fail to load.

## Styling

Components can use the application's Bootstrap and theme classes, which are
already loaded. Styles from `<style>` blocks are built into one file that the
manifest lists under `styles`, and it is added to the page before the plugin
registers anything - the starter is already set up that way.

Nothing scopes those styles, so prefix your selectors with something belonging to
the plugin rather than styling shared elements.

## When changes take effect

Discovery happens once, while the backend starts, and the result is logged - which
is where an operator checks that a jar was picked up:

```
INFO o.cibseven.webapp.plugin.PluginRegistry : Found 1 frontend plugin(s) on the classpath: [demo-report]
```

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

### Several contributions in one slot

A slot holds a list, so any number of plugins can contribute to it and each
contribution becomes its own tab, appended after the built-in ones. One plugin may
just as well register several times, in one slot or across slots.

Two things follow from the list being shared:

- **Registered ids have to be unique across all plugins**, not only within one:
  the id becomes `?tab=<id>`, so a second contribution registering an id that is
  already taken is dropped with a console warning. Prefixing it with the plugin
  name is the simplest way to be safe. The ids the application renders itself -
  `variables`, `jobs`, the rest of the built-in tabs - are reserved and refused
  the same way, so a plugin cannot shadow one of them either.
- **The order of contributed tabs is not defined.** Plugins are loaded
  concurrently and register when their module has arrived, so with two plugins
  their tabs can appear in either order. The built-in tabs always come first.

In the enterprise webclient the tab bar is replaced by
`ProcessInstanceTabsPlugin.vue`, an older build-time extension point, so plugin
tabs appear in CE today and EE needs the same few lines in that component.

## Compatibility

`PLUGIN_API_VERSION` is the contract between a plugin and the webclient: the
webclient's own line, as `major.minor`, taken from the frontend it is built from -
that is the artifact a plugin binds to, since the runtime exports, the slot names
and their props and the manifest fields all live there.

A plugin lists the lines it was built and tested against, so one published build
can serve several webclient versions, and a patch release invalidates nothing. A
manifest naming none of the lines the webclient provides is refused at load time
with a log entry, rather than failing somewhere unpredictable later.

What this document describes may not change incompatibly within a line. A plugin
should therefore be retested when the minor version rises, and can be published
for several lines at once once it has been.

## Trust model and limits

**Plugin code is not sandboxed.** It is imported into the webclient page and has
everything the application has: the session, the DOM, the configured axios
instance and the services. A plugin can do anything the logged-in user can do.
What the runtime hands over is the supported surface, not a limit on what plugin
code could reach - the store is not handed over, and a copy of the config rather
than the config itself, so that an honest plugin cannot break the application by
accident. The slot registry and the API version are a compatibility contract, not
a security boundary.

**The control is deployment.** Plugins are only found on the backend classpath,
so installing one means adding a jar to the deployment - the same level of trust
as any other jar there, and an administrator's action rather than a user's. On
top of that, plugins are off by default.

What this does **not** change:

- **Data access.** Plugins reach data through the same authenticated endpoints,
  with the user's own permissions: a request the user is not allowed to make
  still returns 403. There is no direct database access.
- **Authentication.** Everything under `<basePath>/plugins/**` is served without
  authentication on purpose - the code is not the secret, the data is - and the
  manifest list is readable before login, exposing plugin ids and file names only.
- **Availability.** A plugin that fails to load, exports no `register`, or throws
  while rendering is isolated: it is logged and dropped, and the rest of the
  application keeps working.
- **Startup.** Plugins are loaded next to the application rather than before it,
  so a slow or unreachable one cannot delay it: its contributions appear once it
  has registered them, and the webclient starts at its usual speed either way.

For whoever deploys a plugin: it is their code and their responsibility. It has
to be retested on every webclient update, and layout changes on our side can
affect content rendered next to ours.

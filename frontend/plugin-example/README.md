# Example webclient plugin

`demo-stats/` is a working plugin, kept deliberately as a plain ES module with
**no build step**: it is fetched and evaluated after the webclient has been
compiled and deployed. It contributes a tab to the process instance view showing
the instance it was handed, the application config it was given, and the number of
processes the logged-in user may see.

See [PLUGINS.md](../../PLUGINS.md) for the manifest format, the runtime API, the
available slots and the trust model.

## Trying it out

1. Copy `demo-stats/` onto the classpath of the webclient you run, for example
   into `cibseven-webclient-web/src/main/resources/META-INF/cibseven-plugins/`,
   and set `cibseven.webclient.plugins.enabled: true`.

2. Start the webclient, log in and open any process instance. The console logs
   `Plugin "demo-stats" loaded` and a **Demo plugin** tab appears after the
   built-in tabs. Its process count comes from the application's axios instance,
   so it carries the user's authentication and shows the failure status instead
   when rights are missing.

3. The tab is deep-linkable like any built-in one: append `?tab=demo-stats` to a
   process instance URL.

With the property off, or no plugin on the classpath, none of this runs and the
application behaves exactly as before.

## What to copy from it

- `plugin.json` — the manifest; no `id`, the folder name is the id
- `index.js` — `register()` putting one component into `process-instance-tab`,
  using `vue`, `services` and `getContext` from `@cibseven/plugin-runtime`
- `translations_*.json` — merged under `plugins.demo-stats.*`
- `styles.css` — listed in the manifest and added to the page; every selector is
  prefixed with the plugin name, since nothing scopes them

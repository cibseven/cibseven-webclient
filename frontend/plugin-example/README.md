# Example webclient plugin

`demo-report/` is a working plugin and the starter to copy. It contributes a tab
to the process instance view, listing processes the logged-in user may see, and
brings its own component, translations and styles.

See [PLUGINS.md](../../PLUGINS.md) for the manifest format, the runtime API, the
available slots and the trust model.

## Building it

```sh
cd demo-report
npm install
npm run package     # builds, then jars dist/META-INF into demo-report-plugin.jar
```

The build writes into `dist/META-INF/cibseven-plugins/demo-report/`, the layout a
plugin jar needs, and copies the manifest and translations from `public/`. The
built `index.js` keeps its imports bare (`from "vue"`,
`from "@cibseven/plugin-runtime"`), which the webclient's import map resolves at
load time.

## Making it your own

1. Copy the folder and rename it.
2. Change `build.outDir` in `vite.config.js` and the `id` passed to
   `registerPlugin` in `src/index.js` - that name is the plugin id, and it has to
   be unique across everything on the classpath.
3. Rename the jar in the `package` script.
4. Write your components. Leave `rollupOptions.external` alone: `vue`, `axios`,
   `bootstrap` and `@cibseven/plugin-runtime` come from the webclient, and
   bundling them breaks the plugin in ways that produce no error message.

## Trying it out

Deploy the jar as described in [PLUGINS.md](../../PLUGINS.md) and set
`cibseven.webclient.plugins.enabled: true`.

While developing, the files can also be copied onto the classpath of the
webclient you run, for example into
`cibseven-webclient-web/src/main/resources/META-INF/cibseven-plugins/demo-report/`,
which makes them part of the webclient's own artifact. That is a shortcut for
development only - a plugin is normally shipped as a jar of its own.

After a restart, log in and open any process instance: a **Demo report** tab
appears after the built-in ones, and `?tab=demo-report` links straight to it.

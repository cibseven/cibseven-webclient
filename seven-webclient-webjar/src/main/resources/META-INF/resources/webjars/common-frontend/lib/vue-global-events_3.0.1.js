/*!
 * vue-global-events v3.0.1
 * (c) 2019-2023 Eduardo San Martin Morote, Damian Dulisz
 * Released under the MIT License.
 */
var VueGlobalEvents = (function (exports, vue) {
  'use strict';

  let _isIE;
  function isIE() {
    return _isIE == null ? _isIE = /msie|trident/.test(window.navigator.userAgent.toLowerCase()) : _isIE;
  }

  const EVENT_NAME_RE = /^on(\w+?)((?:Once|Capture|Passive)*)$/;
  const MODIFIERS_SEPARATOR_RE = /[OCP]/g;
  function extractEventOptions(modifiersRaw) {
    if (!modifiersRaw)
      return;
    if (isIE()) {
      return modifiersRaw.includes("Capture");
    }
    const modifiers = modifiersRaw.replace(MODIFIERS_SEPARATOR_RE, ",$&").toLowerCase().slice(1).split(",");
    return modifiers.reduce((options, modifier) => {
      options[modifier] = true;
      return options;
    }, {});
  }
  const GlobalEventsImpl = vue.defineComponent({
    name: "GlobalEvents",
    props: {
      target: {
        type: String,
        default: "document"
      },
      filter: {
        type: [Function, Array],
        default: () => () => true
      },
      // global event options
      stop: Boolean,
      prevent: Boolean
      // Cannot be implemented because we don't have access to other modifiers at runtime
      // exact: Boolean,
    },
    setup(props, { attrs }) {
      let activeListeners = /* @__PURE__ */ Object.create(null);
      const isActive = vue.ref(true);
      vue.onActivated(() => {
        isActive.value = true;
      });
      vue.onDeactivated(() => {
        isActive.value = false;
      });
      vue.onMounted(() => {
        Object.keys(attrs).filter((name) => name.startsWith("on")).forEach((eventNameWithModifiers) => {
          const listener = attrs[eventNameWithModifiers];
          const listeners = Array.isArray(listener) ? listener : [listener];
          const match = eventNameWithModifiers.match(EVENT_NAME_RE);
          if (!match) {
            if (__DEV__) {
              console.warn(
                `[vue-global-events] Unable to parse "${eventNameWithModifiers}". If this should work, you should probably open a new issue on https://github.com/shentao/vue-global-events.`
              );
            }
            return;
          }
          let [, eventName, modifiersRaw] = match;
          eventName = eventName.toLowerCase();
          const handlers = listeners.map(
            (listener2) => (event) => {
              const filters = Array.isArray(props.filter) ? props.filter : [props.filter];
              if (isActive.value && filters.every((filter) => filter(event, listener2, eventName))) {
                if (props.stop)
                  event.stopPropagation();
                if (props.prevent)
                  event.preventDefault();
                listener2(event);
              }
            }
          );
          const options = extractEventOptions(modifiersRaw);
          handlers.forEach((handler) => {
            window[props.target].addEventListener(
              eventName,
              handler,
              options
            );
          });
          activeListeners[eventNameWithModifiers] = [
            handlers,
            eventName,
            options
          ];
        });
      });
      vue.onBeforeUnmount(() => {
        for (const eventNameWithModifiers in activeListeners) {
          const [handlers, eventName, options] = activeListeners[eventNameWithModifiers];
          handlers.forEach((handler) => {
            window[props.target].removeEventListener(eventName, handler, options);
          });
        }
        activeListeners = {};
      });
      return () => null;
    }
  });
  const GlobalEvents = GlobalEventsImpl;

  function excludeElements(tagNames) {
    return (event) => {
      const target = event.target;
      return !tagNames.includes(target.tagName);
    };
  }
  function includeElements(tagNames) {
    return (event) => {
      const target = event.target;
      return tagNames.includes(target.tagName);
    };
  }

  exports.GlobalEvents = GlobalEvents;
  exports.excludeElements = excludeElements;
  exports.includeElements = includeElements;

  return exports;

})({}, Vue);
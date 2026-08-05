<!--

    Copyright CIB software GmbH and/or licensed to CIB software GmbH
    under one or more contributor license agreements. See the NOTICE file
    distributed with this work for additional information regarding copyright
    ownership. CIB software licenses this file to you under the Apache License,
    Version 2.0; you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

     Unless required by applicable law or agreed to in writing, software
     distributed under the License is distributed on an "AS IS" BASIS,
     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     See the License for the specific language governing permissions and
     limitations under the License.

-->
<template>
  <component :is="component" v-if="!failed" v-bind="params"></component>
</template>

<script>
export default {
  name: 'PluginBoundary',
  props: {
    // Component contributed by a plugin
    component: { type: [Object, Function], required: true },
    // Props handed to the contributed component
    params: { type: Object, default: () => ({}) },
    // Id of the contributing plugin, used for diagnostics
    pluginId: { type: String, default: 'unknown' }
  },
  data() {
    return { failed: false }
  },
  // Plugin code is third-party code: an error inside a contribution must not
  // take down the view that hosts it. The contribution is dropped, the
  // surrounding application keeps working.
  errorCaptured(error) {
    console.error(`Plugin "${this.pluginId}" failed and was removed from the page:`, error)
    this.failed = true
    return false
  }
}
</script>

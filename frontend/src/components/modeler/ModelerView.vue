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
  <div class="d-flex flex-column overflow-hidden h-100">
    <component v-if="modeler" :is="modeler.CibsevenModeler" ref="modeler" />
    <div v-else-if="loadFailed" class="alert alert-danger m-3" role="alert">{{ $t('start.modeler.loadFailed') }}</div>
    <div v-else class="d-flex align-items-center justify-content-center h-100" role="status">
      <BWaitingBox class="d-inline me-2" styling="width: 35px"></BWaitingBox> {{ $t('start.modeler.loading') }}
    </div>
  </div>
</template>

<script>
import { markRaw } from 'vue'
import { BWaitingBox } from '@cib/common-frontend'
import { axios } from '@/globals.js'
import { getServicesBasePath } from '@/services.js'
import { loadModeler } from '@/utils/modelerLoader.js'

const ENGINE_REST_PREFIX = '/client/cibseven-engine'

export default {
  name: 'ModelerView',
  components: { BWaitingBox },
  inject: ['currentLanguage'],
  provide() {
    return {
      config: this.$root.config
    }
  },
  data() {
    return {
      // The modeler package, loaded when the view is first opened rather than with the application
      modeler: null,
      loadFailed: false
    }
  },
  async created() {
    this.engineRestPathInterceptor = axios.interceptors.request.use(config => {
      if (config.url && config.url.startsWith(ENGINE_REST_PREFIX)) {
        config.url = getServicesBasePath() + config.url.slice(ENGINE_REST_PREFIX.length)
      }
      return config
    })
    let modeler
    try {
      modeler = await loadModeler(this.$store)
    } catch (error) {
      console.error('The modeler could not be loaded:', error)
      this.loadFailed = true
      return
    }
    // Configure modeler to use webclient's axios and base path
    modeler.setAxiosInstance(axios)
    modeler.setServicesBasePath(getServicesBasePath())
    this.$store.dispatch('modeler/elementTemplates/fetchAllElementTemplates')
      .catch(error => console.warn('Could not load element templates:', error))
    this.modeler = markRaw(modeler)
  },
  beforeUnmount() {
    axios.interceptors.request.eject(this.engineRestPathInterceptor)
  }
}
</script>

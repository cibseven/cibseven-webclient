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
    <div v-if="$root.config.modelerDbConfigured === false"
         class="d-flex flex-column align-items-center justify-content-center h-100 text-center p-5">
      <span class="mdi mdi-database-off-outline text-secondary mb-3" style="font-size: 4rem;" aria-hidden="true"></span>
      <h4 class="text-secondary mb-2">{{ $t('start.modeler.dbNotConfigured.title') }}</h4>
      <p class="text-muted">{{ $t('start.modeler.dbNotConfigured.description') }}</p>
    </div>
    <CibsevenModeler v-else ref="modeler" />
  </div>
</template>

<script>
import { CibsevenModeler, setAxiosInstance, setServicesBasePath } from 'cibseven-modeler'
import 'cibseven-modeler/dist/cibseven-modeler.css'
import { axios } from '@/globals.js'
import { getServicesBasePath } from '@/services.js'

export default {
  name: 'ModelerView',
  components: { CibsevenModeler },
  inject: ['currentLanguage'],
  provide() {
    return {
      config: this.$root.config
    }
  },
  created() {
    if (this.$root.config.modelerDbConfigured === false) return
    // Configure modeler to use webclient's axios and base path
    setAxiosInstance(axios)
    setServicesBasePath(getServicesBasePath())
    this.$store.dispatch('modeler/elementTemplates/fetchAllElementTemplates')
      .catch(error => console.warn('Could not load element templates:', error))
  }
}
</script>

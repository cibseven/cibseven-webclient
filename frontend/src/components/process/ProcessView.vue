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
    <ProcessDefinitionView
      v-if="!loading && computedVersionIndex"
      :processKey="processKey"
      :versionIndex="computedVersionIndex"
      :instanceId="computedInstanceId"
      :tenantId="tenantId"
    ></ProcessDefinitionView>
</template>

<script>
import ProcessDefinitionView from '@/components/process/ProcessDefinitionView.vue'

export default {
  name: 'ProcessView',
  components: { ProcessDefinitionView },
  props: {
    processKey: { type: String, required: true },
    versionIndex: { type: String, default: '' },
    instanceId: { type: String, default: '' },
    tenantId: { type: String, default: null }
  },
  watch: {
    processKey: 'loadProcess',    
    versionIndex: 'loadProcess',
    tenantId: 'loadProcess',
    instanceId: 'loadProcess'
  },
  data: function() {
    return {
      process: null,
      loading: false
    }
  },
  computed: {
    computedVersionIndex: function() {
      if (this.loading) {
        return ''
      } else if (this.versionIndex) {
        return this.versionIndex
      } else if (this.process !== null) {
        return this.process.version
      } else {
        return ''
      }
    },
    computedInstanceId: function() {
      // only valid with proper "versionIndex"
      return this.versionIndex ? this.instanceId : '';
    }
  },
  methods: {
    loadProcess: function() {
      if (!this.versionIndex) {
        this.loading = true
        this.$store.dispatch('getProcessByDefinitionKey', { key: this.processKey, tenantId: this.tenantId })
		.then(process => {
          this.process = process
          this.loading = false
        })
      }
    }
  },
  created: function() {
    this.loadProcess()
  },
  beforeUpdate: function() {
    if (this.process != null && this.process.version !== this.computedVersionIndex) {
      this.process = null
    }
  }
}
</script>

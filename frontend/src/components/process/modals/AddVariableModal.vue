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
  <AddVariableModalUI ref="addVariableModalUI"
    :edit-mode="false"
    :saving="saving"
    :error="error"
    @add-variable="addVariable"
  ></AddVariableModalUI>
</template>

<script>
import { ProcessService } from '@/services.js'
import AddVariableModalUI from '@/components/process/modals/AddVariableModalUI.vue'

export default {
  name: 'AddVariableModal',
  components: { AddVariableModalUI },
  props: { selectedInstance: Object },
  emits: ['variable-added'],
  data: function() {
    return {
      saving: false,
      error: null,
    }
  },
  methods: {
    reset: function() {
      this.error = null
      this.saving = false
    },
    show: function() {
      this.reset()
      this.$refs.addVariableModalUI.show()
    },
    addVariable: async function(variable) {
      this.saving = true
      return ProcessService.putLocalExecutionVariable(this.selectedInstance.id, variable.name, variable).then(() => {
        this.$refs.addVariableModalUI.hide()
        this.$emit('variable-added')
      }).catch(error => {
        this.error = error.message
        this.saving = false
      })
    }
  }
}
</script>

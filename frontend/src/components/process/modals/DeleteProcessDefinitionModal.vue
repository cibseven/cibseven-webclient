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
  <ConfirmDialog v-if="params" ref="confirm"
    @ok="params.ok(params.processDefinition)"
    :ok-title="$t('process.deleteProcessDefinition.confirm.title')">

    <i18n-t keypath="process.deleteProcessDefinition.confirm.line1" tag="p" scope="global"></i18n-t>
    <i18n-t keypath="process.deleteProcessDefinition.confirm.line2" tag="p" scope="global">
      <template #name>
        <strong>{{ params?.processDefinition?.name }}</strong>
        <br>
      </template>
      <template #version>
        <strong>{{ params?.processDefinition?.version }}</strong>
      </template>
    </i18n-t>

    <b-form-checkbox disabled v-model="cascadeDelete" class="mt-3">
      {{ $t('deployment.deleteRunningInstances') }}
    </b-form-checkbox>

  </ConfirmDialog>
</template>

<script>
import { ConfirmDialog } from '@cib/common-frontend'

export default {
  name: 'DeleteProcessDefinitionModal',
  components: { ConfirmDialog },
  data: function() {
    return {
      params: Object,
      cascadeDelete: true,
    }
  },
  methods: {
    show: function(params) {
      this.params = params
      this.$refs.confirm.show()
    }
  }
}
</script>

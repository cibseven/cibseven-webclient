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
    @ok="params.ok(params.instance)"
    :ok-title="params?.okTitle">

    <p>{{ params?.message }}</p>
    <p>
      {{ $t('process.id') }}:
      <strong>{{ params?.instance?.id }}</strong>

      <br>{{ $t('process.startTime') }}:
      <strong>{{ params?.instance?.startTime }}</strong>

      <br>{{ $t('process.state') }}:
      <strong>{{ params?.instance?.state }}</strong>
    </p>

  </ConfirmDialog>
</template>

<script>
import ConfirmDialog from '@/components/common-components/ConfirmDialog.vue'

export default {
  name: 'ConfirmActionOnProcessInstanceModal',
  components: { ConfirmDialog },
  data: function() {
    return {
      params: Object
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

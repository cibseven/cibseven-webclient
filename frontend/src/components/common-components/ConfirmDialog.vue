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
  <b-modal ref='modal' :title="$t('confirm.title')">
    <div class="row">
      <div class="col-2 d-flex justify-content-center">
        <span class="mdi-36px mdi mdi-alert-outline text-warning"></span>
      </div>
      <div class="col-10 d-flex align-items-center ps-0">
        <div>
          <slot :param="param"></slot>
        </div>
      </div>
    </div>
    <template v-slot:modal-footer>
      <b-button @click="$refs.modal.hide('cancel')" variant="light">{{ $t('confirm.cancel') }}</b-button>
      <b-button @click="$emit('ok', param); $refs.modal.hide('ok')" variant="primary">{{ okTitle || $t('confirm.ok') }}</b-button>
    </template>
  </b-modal>
</template>

<script>
export default {
  name: 'ConfirmDialog',
  props: {
    okTitle: String
  },
  emits: ['ok'],
  data: function() { return { param: null } },
  methods: {
    show: function(param) {
      this.param = param
      this.$refs.modal.show()
    }
  }
}
</script>

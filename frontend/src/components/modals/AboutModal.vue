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
  <b-modal ref="about" :title="$t('infoAndHelp.flowModalAbout.title')">
    <div class="row">
      <div class="col-2">
        <svg id="Ebene_1" data-name="Ebene 1"
          xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24">
            <path
            d="M12,2A10,10,0,1,1,2,12,10,10,0,0,1,12,2m0-2A12,12,0,1,0,24,12,12,12,0,0,0,12,0Z"
            fill="var(--info)" />
            <g>
              <path
            d="M13.11,9.93s-.1,6.9,0,7a5.81,5.81,0,0,0,1.33.24,2.13,2.13,0,0,1,1.08.3,1.05,1.05,0,0,1,0,1.51,1.58,1.58,0,0,1-1.08.3H9.57A1.58,1.58,0,0,1,8.49,19a1,1,0,0,1-.32-.76.93.93,0,0,1,.32-.75,2.43,2.43,0,0,1,1.08-.3c.27,0,1.42,0,1.42-.33s0-4.18,0-4.53S10.44,12,10,12a1.67,1.67,0,0,1-1-.25,1,1,0,0,1,0-1.52,1.61,1.61,0,0,1,1.08-.29Z"
            fill="var(--info)" />
              <path d="M13,7.43a1.5,1.5,0,1,1-1.5-1.5A1.5,1.5,0,0,1,13,7.43Z"
            fill="var(--info)" />
            </g>
          </svg>
      </div>
      <div class="col-10 d-flex align-items-center">{{ $t('infoAndHelp.flowModalSupport.version') }}: {{ version }}</div>
    </div>
    <template v-slot:modal-footer>
      <div class="row w-100 me-0">
        <div class="col-6 p-0">
          <b-button v-if="permissionsAdmin" variant="light" @click="onDiagnosticData">{{ $t('admin.system.system-diagnostics.diagnosticData') }}</b-button>
        </div>
        <div class="col-6 p-0">
          <b-button variant="primary" @click="$refs.about.hide()" class="float-end">{{ $t('confirm.ok') }}</b-button>
        </div>
      </div>
    </template>
  </b-modal>
</template>

<script>
import { InfoService } from '@/services.js'
import { permissionsMixin } from '@/permissions.js'

export default {
  name: 'AboutModal',
  mixins: [permissionsMixin],
  data: function() {
    return {
      version: ''
    }
  },
  computed: {
    permissionsAdmin: function() {
      return this.$root.user && this.adminManagementPermissions(this.$root.config.permissions.systemManagement, 'system')
    }
  },
  methods: {
    show: function() {
      if (this.version === '') {
        this.version = ' '
        InfoService.getVersion().then(version => {
          this.version = version || 'n/a'
        })
        .catch(error => {
          console.error("Error loading version:", error)
          this.version = 'n/a'
        })
      }
      this.$refs.about.show()
    },
    onDiagnosticData: function() {
      this.$refs.about.hide()
      this.$router.push('/seven/auth/admin/system/system-diagnostics')
    }
  }
}
</script>

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
    <div class="container-fluid">
      <div v-for="group of groups" :key="group.name">
        <p role="heading" aria-level="4" class="mdi mdi-18px text-end pt-3 mb-1" :class="group.visible ? 'mdi-minus' : 'mdi-plus'"
          @click="$eventBus.emit('bv::toggle::collapse', group.name)" style="cursor: pointer">
          <span class="float-start h5">{{ group.name }}</span>
        </p>
        <hr class="mt-0 mb-0">
        <b-collapse class="me-3" :id="group.name" v-model="group.visible">
          <div class="row">
            <div v-for="d of group.data" :key="d.id" class="col-md-6 col-lg-3 col-12 my-3">
              <b-button @click="setDeployment(d)" variant="link" class="text-decoration-none p-0 w-100 shadow-sm">
                <b-card style="min-height: 120px;">
                  <b-card-body :class="d === deployment ? 'border-start border-primary border-4' : ''">
                    <b-card-text>
                      <label @click.stop class="d-flex align-items-start m-0 hover-highlight">
                        <b-form-checkbox size="sm" v-model="d.isSelected" @click.stop></b-form-checkbox>
                        <span class="fw-bold text-break">{{ d.name || d.id }}</span>
                      </label>
                      <div class="d-flex align-items-center pt-2">
                        <span>{{ formatDate(d.deploymentTime) }}</span>
                      </div>
                      <div class="d-flex align-items-center pt-2">
                        <small>{{ $t('deployment.tenant') }}: {{ d.tenantId ? d.tenantId : '-' }}</small>
                      </div>
                      <div class="d-flex align-items-center pt-2">
                        <small>{{ $t('deployment.source') }}: {{ d.source }}</small>
                      </div>
                    </b-card-text>
                  </b-card-body>
                </b-card>
              </b-button>
            </div>
          </div>
        </b-collapse>
      </div>
    </div>
</template>

<script>
import { formatDate } from '@/utils/dates.js'

export default {
  name: 'DeploymentList',
  emits: [ 'select-deployment' ],
  props: {
    groups: Array,
    deployments: Array,
    deployment: Object,
  },
  watch: {
    '$route.params.deploymentId': {
      handler: function() {
        this.setDeploymentFromUrl(this.$route.params.deploymentId)
      },
      immediate: true
    },
  },
  methods: {
    formatDate,
    setDeployment: function(d) {
      this.$router.push('/seven/auth/deployments/' + d.id)
    },
    setDeploymentFromUrl: function(deploymentId) {
      if (this.deployments.length > 0 && deploymentId) {
        var deployment = this.deployments.find(d => {
          return d.id === deploymentId
        })
        if (deployment) this.$emit('select-deployment', deployment)
      }
    }
  }
}
</script>

<style>
.hover-highlight {
  transition: border 0.2s ease;
  border: 1px solid white;
  border-radius: 4px;
}

.hover-highlight:hover {
  border: 1px solid #ced4da; /* Bootstrap gray border, bs-gray-400 */
}
</style>

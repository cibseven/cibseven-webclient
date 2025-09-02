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
  <div class="overflow-auto h-100">
    <div class="h-100 d-flex flex-column">
      <div class="overflow-auto flex-fill">
        <div class="p-2 bg-white">
          <div class="d-flex justify-content-between bg-white ps-3 pe-3">
            <div v-if="deployment">
              <h4>{{ deployment.name || deployment.id }}</h4>
            </div>
            <a class="btn btn-sm btn-primary text-dark border-white bg-white shadow-none" data-bs-toggle="collapse"
              href="#collapseExample" role="button" aria-expanded="false" aria-controls="collapseExample"
              @click="toggleButton">
              <span class="mdi mdi-18px" :class="toggleIcon"></span>
            </a>
          </div>
          <div class="collapse border-none" id="collapseExample">
            <div class="card card-body text-dark border-white bg-white">
              <div v-if="deployment">
                <p>{{ formatDate(deployment.deploymentTime) }}</p>
                <small>
                  <p>{{ $t('deployment.tenant') }}: {{ deployment.tenantId }}</p>
                </small>
                <small>
                  <p>{{ $t('deployment.source') }}: {{ deployment.source }}</p>
                </small>
              </div>
            </div>
          </div>
          <b-list-group v-if="resources && resources.length > 0">
            <b-list-group-item v-for="resource of resources" :key="resource.id" action
              class="border-0 rounded-0 text-dark border-white bg-white" @click="showResource(resource)">
              <div class="d-flex align-items-center justify-content-between">
                <div class="text-truncate me-0" style="flex: 1">
                  <span :title="$t('deployment.showModel')">{{ resource.name }}</span>
                </div>
                <b-button @click="showResource(resource)" size="sm" variant="outline-secondary"
                  class="border-0 mdi mdi-18px mdi-eye-outline text-dark bg-white"
                  :title="$t('deployment.showModel')"></b-button>
              </div>
            </b-list-group-item>
            <b-list-group-item class="text-dark border-white bg-white">
              <div class="d-flex">
                <b-button @click="$emit('delete-deployment', this.deployment)"
                  class="border-dark text-dark bg-white me-3" :title="$t('deployment.delete')">
                  <span class="mdi mdi-trash-can"></span>
                  {{ $t('deployment.delete') }}</b-button>
                <b-button @click="$emit('show-deployment', this.deployment)"
                  class="border-dark text-dark bg-white" :title="$t('deployment.showDeployment')">
                  <span class="mdi mdi-download-multiple-outline"></span>
                  {{ $t('deployment.showDeployment') }}</b-button>
              </div>
            </b-list-group-item>
          </b-list-group>
          <div v-else-if="resources">
            <img src="@/assets/images/task/no_tasks_pending.svg" class="d-block mx-auto mt-5 mb-3" style="width: 200px">
          </div>
          <div v-else class="h-100 d-flex flex-column justify-content-center align-items-center text-center">
            <span class="mdi mdi-48px mdi-file-cancel-outline pe-1 text-warning"></span>
            <span>{{ $t('deployment.errorLoading') }}</span>
          </div>
        </div>
      </div>
    </div>

    <b-modal static ref="diagramModal" :size="error ? '' : 'xl'" :title="resource.name" dialog-class="h-75"
      content-class="h-100" :ok-only="true">
      <div class="container-fluid h-100 p-0">
        <div v-if="diagramLoading" class="text-center">
          <b-waiting-box styling="width: 35px"></b-waiting-box>
        </div>
        <div v-else-if="error" class="d-flex align-items-center">
          <span class="mdi mdi-48px mdi-file-cancel-outline pe-1 text-warning"></span>
          <span>{{ $t('deployment.errorLoading') }}</span>
        </div>
        <BpmnViewer v-show="!diagramLoading && error === false" class="h-100" ref="diagram"></BpmnViewer>
      </div>
    </b-modal>

  </div>
</template>

<script>
import { ProcessService } from '@/services.js'
import BpmnViewer from '@/components/process/BpmnViewer.vue'
import { formatDate } from '@/utils/dates.js'

export default {
  name: 'ResourcesNavBar',
  emits: ['delete-deployment', 'show-deployment'],
  components: { BpmnViewer },
  props: { resources: Array, deploymentId: String },
  data: function () {
    return {
      resource: {},
      diagramLoading: false,
      error: false,
      deployment: null,
      toggleIcon: 'mdi-chevron-down'
    }
  },
  created: function () {
    this.loadDeployment()
  },
  watch: {
    deploymentId: function () {
      this.loadDeployment()
    },
  },
  methods: {
    formatDate,
    showResource: function (resource) {
      this.error = false
      this.diagramLoading = true
      this.$refs.diagram.cleanDiagramState()
      this.$refs.diagram.drawDiagramState()
      this.$refs.diagramModal.show()

      this.resource = resource
      ProcessService.findProcessesWithFilters('deploymentId=' + this.deployment.id + '&resourceName=' + resource.name)
        .then(processesDefinition => {
          if (processesDefinition.length > 0) {
            var processDefinition = processesDefinition[0]
            ProcessService.fetchDiagram(processDefinition.id).then(response => {
              setTimeout(() => {
                this.diagramLoading = false
                this.$refs.diagram.showDiagram(response.bpmn20Xml, null, null)
              }, 500)
            }).catch(() => {
              this.diagramLoading = false
              this.error = true
            })
          }
          else {
            this.diagramLoading = false
            this.error = true
          }
        }).catch(() => {
          this.diagramLoading = false
          this.error = true
        })
    },
    loadDeployment: function () {
      if (this.deploymentId) {
        if (this.deployments) {
          let found = this.deployments.find(d => {
            return d.id === this.deploymentId
          })
          if (found) {
            this.deployment = found
          }
          else {
            ProcessService.findDeployment(this.deploymentId).then(deployment => {
              this.deployment = deployment
            })
          }
        }
        else {
          ProcessService.findDeployment(this.deploymentId).then(deployment => {
            this.deployment = deployment
          })
        }
      }
    },
    toggleButton: function () {
      if (this.toggleIcon === 'mdi-chevron-up') {
        this.toggleIcon = 'mdi-chevron-down'
      }
      else {
        this.toggleIcon = 'mdi-chevron-up'
      }
    }
  }
}
</script>

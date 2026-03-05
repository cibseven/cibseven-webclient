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
        <div v-if="deployment" class="p-2 bg-white">
          <a class="d-flex justify-content-between bg-white ps-3 pe-3 align-items-center text-decoration-none text-dark cursor-pointer"
            data-bs-toggle="collapse" href="#deploymentInfo" role="button" aria-expanded="false" 
            aria-controls="deploymentInfo" @click="toggleButton">
            <h6 class="fw-bold m-0">{{ deployment.name || deployment.id }}</h6>
            <span class="mdi mdi-18px" :class="toggleIcon"></span>
          </a>
          <div class="collapse border-none" id="deploymentInfo">
            <div class="card card-body text-dark border-white py-2">
              <div>
                <div class="pb-2" :title="formatDateForTooltips(deployment.deploymentTime)">{{ formatDate(deployment.deploymentTime) }}</div>
                <div class="pb-2 small">{{ $t('deployment.tenant') }}: {{ deployment.tenantId }}</div>
                <div class="small">{{ $t('deployment.source') }}: {{ deployment.source }}</div>
              </div>
            </div>
          </div>
          <ul v-if="resources && resources.length > 0" class="list-group">
            <li v-for="resource of resources" :key="resource.id"
              class="list-group-item border-0 rounded-0 text-dark border-white ps-1" >
              <div class="d-flex align-items-center justify-content-between">
                <button class="btn btn-link text-truncate text-decoration-none me-0 text-start" style="flex: 1" @click.stop="showResource(resource)">
                  <span :title="$t('deployment.showModel')">{{ resource.name }}</span>
                </button>
                <CellActionButton @click.stop="showResource(resource)"
                  tabindex="-1"
                  icon="mdi-eye-outline"
                  :title="$t('deployment.showModel')"></CellActionButton>
                <CellActionButton v-if="canDownload(resource)" @click.stop="download(resource)"
                  tabindex="-1"
                  icon="mdi-download"
                  :title="$t('process.downloadBpmn')"></CellActionButton>
                <component :is="ResourcesNavBarActionsPlugin" v-if="ResourcesNavBarActionsPlugin" :resource="resource" :deployment="deployment" @deployment-success="$emit('deployment-success')"></component>
              </div>
            </li>
            <div class="p-2">
              <div class="d-flex flex-column align-items-start gap-2">
                <component :is="ResourcesNavBarDeploymentActionsPlugin" v-if="ResourcesNavBarDeploymentActionsPlugin" :deployment="deployment" @deployment-success="$emit('deployment-success')" class="w-100"></component>
                <b-button variant="light" size="sm" @click="$emit('show-deployment', this.deployment)"
                  :title="$t('deployment.showDeployment')">
                  <span class="mdi mdi-download-multiple-outline"></span>
                  {{ $t('deployment.showDeployment') }}</b-button>
                <b-button variant="light" size="sm" @click="$emit('delete-deployment', this.deployment)"
                  :title="$t('deployment.delete')">
                  <span class="mdi mdi-delete-outline"></span>
                  {{ $t('deployment.delete') }}</b-button>
              </div>
            </div>
          </ul>
          <div v-else class="h-100 d-flex flex-column justify-content-center align-items-center text-center">
            <span class="mdi mdi-48px mdi-file-cancel-outline pe-1 text-warning"></span>
            <span>{{ $t('deployment.errorLoading') }}</span>
          </div>
        </div> 
        <div v-else>
          <img src="@/assets/images/task/no_tasks_pending.svg" class="d-block mx-auto mt-5 mb-3" style="width: 200px" alt="">
          <div class="h5 text-secondary text-center">{{ $t('deployment.noDeploymentSelected') }}</div>
        </div>
      </div>
    </div>

    <b-modal static ref="diagramModal" :size="error ? '' : 'xl'" :title="resource.name" :ok-only="true">
      <div class="container-fluid h-100 p-0">
        <div v-if="diagramLoading" class="text-center">
          <b-waiting-box styling="width: 35px"></b-waiting-box>
        </div>
        <div v-else-if="error" class="d-flex align-items-center">
          <span class="mdi mdi-48px mdi-file-cancel-outline pe-1 text-warning"></span>
          <span>{{ $t('deployment.errorLoading') }}</span>
        </div>
        <div v-show="!diagramLoading && error === false" style="height: calc(100vh - 210px)">
          <BpmnViewer v-show="!isDmnResource" class="h-100" ref="diagram"></BpmnViewer>
          <DmnViewer v-show="isDmnResource" class="h-100" ref="dmnDiagram"></DmnViewer>
        </div>
      </div>
    </b-modal>

  </div>
</template>

<script>
import { ProcessService } from '@/services.js'
import BpmnViewer from '@/components/process/BpmnViewer.vue'
import DmnViewer from '@/components/decision/DmnViewer.vue'
import CellActionButton from '@/components/common-components/CellActionButton.vue'
import { formatDate, formatDateForTooltips } from '@/utils/dates.js'
import { mapActions } from 'vuex'

export default {
  name: 'ResourcesNavBar',
  emits: ['delete-deployment', 'show-deployment', 'deployment-success'],
  components: { BpmnViewer, DmnViewer, CellActionButton },
  props: { resources: Array, deploymentId: String },
  data: function () {
    return {
      resource: {},
      diagramLoading: false,
      error: false,
      deployment: null,
      toggleIcon: 'mdi-chevron-down',
      isDmnResource: false
    }
  },
  watch: {
    deploymentId: function () {
      if(this.deploymentId){
      this.loadDeployment()
      } else {
        this.deployment = null
      }
    },
  },
  computed: {
    ResourcesNavBarActionsPlugin: function() {
      return this.$options.components && this.$options.components.ResourcesNavBarActionsPlugin
        ? this.$options.components.ResourcesNavBarActionsPlugin
        : null
    },
    ResourcesNavBarDeploymentActionsPlugin: function() {
      return this.$options.components && this.$options.components.ResourcesNavBarDeploymentActionsPlugin
        ? this.$options.components.ResourcesNavBarDeploymentActionsPlugin
        : null
    }
  },
  created: function () {
    this.loadDeployment()
  },
  methods: {
    ...mapActions(['getDecisionList', 'getXmlById']),
    formatDate,
    formatDateForTooltips,
    async showResource(resource) {
      this.error = false
      this.diagramLoading = true
      this.resource = resource
      
      // Determine if this is a DMN resource based on file extension
      this.isDmnResource = resource.name.toLowerCase().endsWith('.dmn')
      
      // Clean diagram state for the appropriate viewer
      if (this.isDmnResource) {
        this.$refs.dmnDiagram?.cleanDiagramState()
      } else {
        this.$refs.diagram?.cleanDiagramState()
        this.$refs.diagram?.drawDiagramState()
      }
      
      this.$refs.diagramModal.show()

      if (this.isDmnResource) {
        // Handle DMN resources
        const content = await this.getContent(resource)
        if (content) {
          setTimeout(() => {
            this.diagramLoading = false
            this.$refs.dmnDiagram.showDiagram(content)
          }, 500)
        }
        else {
          this.diagramLoading = false
          this.error = true
        }
      }
      else {
        // Handle BPMN resources
        const content = await this.getContent(resource)
        if (content) {
          setTimeout(() => {
            this.diagramLoading = false
            this.$refs.diagram.showDiagram(content)
          }, 500)
        }
        else {
          this.diagramLoading = false
          this.error = true
        }
      }
    },
    loadDeployment: function () {
      if (this.deploymentId) {
        if (this.deployments) {
          const found = this.deployments.find(d => {
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
    canDownload(resource) {
      return resource.name.toLowerCase().endsWith('.bpmn') || resource.name.toLowerCase().endsWith('.dmn')
    },
    async getContent(resource) {
      this.diagramLoading = true
      let content = null
      const isBpmn = resource.name.toLowerCase().endsWith('.bpmn')
      if (isBpmn) {
        const processesDefinition = await ProcessService.findProcessesWithFilters('deploymentId=' + this.deployment.id + '&resourceName=' + resource.name)
        const processDefinition = Array.isArray(processesDefinition) ? processesDefinition[0] : null
        const response = processDefinition ? await ProcessService.fetchDiagram(processDefinition.id) : null
        content = response ? response.bpmn20Xml : null
      }
      else {
        const decisions = await this.getDecisionList({ 
          deploymentId: this.deployment.id,
          resourceName: resource.name 
        })
        const descision = Array.isArray(decisions) ? decisions[0] : null
        const response = descision ? await this.getXmlById(descision.id) : null
        content = response ? response.dmnXml : null
      }
      return content
    },
    async download(resource) {
      const content = await this.getContent(resource)
      this.diagramLoading = false
      // download BPMN XML if content is available
      if (content) {
        const blob = new Blob([content], { type: 'application/xml' })
        const url = URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = resource.name
        document.body.appendChild(a)
        a.click()
        document.body.removeChild(a)
        URL.revokeObjectURL(url)
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

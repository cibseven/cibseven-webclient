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
        <b-list-group v-if="resources && resources.length > 0">
          <b-list-group-item v-for="resource of resources" :key="resource.id" action class="border-0 rounded-0 p-2" @click="showResource(resource)">
            <div class="d-flex align-items-center justify-content-between">
              <div class="text-truncate me-0" style="flex: 1">
                <span :title="$t('deployment.showModel')">{{ resource.name }}</span>
              </div>
              <b-button
                @click="showResource(resource)"
                size="sm"
                variant="outline-secondary"
                class="border-0 mdi mdi-18px mdi-eye-outline"
                :title="$t('deployment.showModel')"
              ></b-button>
            </div>
          </b-list-group-item>
        </b-list-group>
        <div v-else-if="resources">
          <img src="/src/assets/images/task/no_tasks_pending.svg" class="d-block mx-auto mt-5 mb-3" style="width: 200px">
        </div>
        <div v-else class="h-100 d-flex flex-column justify-content-center align-items-center text-center">
          <span class="mdi mdi-48px mdi-file-cancel-outline pe-1 text-warning"></span>
          <span>{{ $t('deployment.errorLoading') }}</span>
        </div>
      </div>
    </div>

    <b-modal static ref="diagramModal" :size="error ? '' : 'xl'" :title="resource.name" dialog-class="h-75" content-class="h-100" :ok-only="true">
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

export default {
  name: 'ResourcesNavBar',
  components: { BpmnViewer },
  props: { resources: Array, deployment: Object },
  data: function() {
    return {
      resource: {},
      diagramLoading: false,
      error: false
    }
  },
  methods: {
    showResource: function(resource) {
      this.error = false
      this.diagramLoading = true
      this.$refs.diagram.cleanDiagramState()
      this.$refs.diagram.drawDiagramState()
      this.$refs.diagramModal.show()

      this.resource = resource
      ProcessService.findProcessesWithFilters('deploymentId=' + this.deployment.id + '&resourceName=' + resource.name)
      .then(processesDefinition => {
        if (processesDefinition.length > 0 ) {
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
    }
  }
}
</script>

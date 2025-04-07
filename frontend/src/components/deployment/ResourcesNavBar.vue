<template>
  <div class="overlflow-auto h-100">
    <div class="h-100 d-flex flex-column">
      <div class="overflow-auto flex-fill">
        <b-list-group v-if="resources && resources.length > 0">
          <b-list-group-item v-for="resource of resources" :key="resource.id" action class="border-0 rounded-0 p-2">
            <div class="row no-gutters align-items-center">
              <div class="col-10 text-truncate">
                <span :title="resource.name" class="mb-1">{{ resource.name }}</span><br>
              </div>
              <div class="col-2">
                <b-button @click="showResource(resource)" size="sm" variant="outline-secondary" class="border-0 mdi mdi-18px mdi-eye-outline" :title="$t('deployment.showModel')"></b-button>
              </div>
            </div>
          </b-list-group-item>
        </b-list-group>
        <div v-else>
          <img src="/assets/images/task/no_tasks_pending.svg" class="d-block mx-auto mt-5 mb-3" style="width: 200px">
        </div>
      </div>
    </div>

    <b-modal static ref="diagramModal" size="xl" :title="resource.name" dialog-class="h-75" content-class="h-100" :ok-only="true">
      <div class="container-fluid h-100 p-0">
        <BpmnViewer class="h-100" ref="diagram"></BpmnViewer>
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
    return { resource: {} }
  },
  methods: {
    showResource: function(resource) {
      this.resource = resource
      ProcessService.findProcessesWithFilters('deploymentId=' + this.deployment.id + '&resourceName=' + resource.name)
      .then(processesDefinition => {
        if (processesDefinition.length > 0 ) {
          this.$refs.diagramModal.show()
          var processDefinition = processesDefinition[0]
          ProcessService.fetchDiagram(processDefinition.id).then(response => {
            setTimeout(() => {
              this.$refs.diagram.showDiagram(response.bpmn20Xml, null, null)
            }, 500)
          })
        }
        else this.processesDefinition = null
      })
    }
  }
}
</script>

<template>
  <div class="h-100 d-flex flex-column bg-light">
    <div class="h-100 container-fluid overflow-auto">
      <div v-for="(group, key) of deploymentsGrouped" :key="key">
        <p role="heading" aria-level="4" class="mdi mdi-18px border-black border-bottom text-end pt-3" :class="group.visible ? 'mdi-minus' : 'mdi-plus'"
          @click="$eventBus.emit('bv::toggle::collapse', key)" style="cursor: pointer">
          <span class="float-start h5">{{ title(key) }}</span>
        </p>
        <b-collapse class="me-3" :id="key" v-model="group.visible">
          <div class="row">
            <div v-for="d of group.data" :key="d.id" class="col-md-6 col-lg-3 col-12 my-3">
              <b-button @click="setDeployment(d)" variant="link" class="text-decoration-none p-0 w-100 shadow-sm">
                <b-card style="min-height: 120px;">
                  <b-card-body :class="d === deployment ? 'border-start border-primary border-4' : ''">
                    <b-card-text>
                      <span @click.stop class="d-flex justify-content-start">
                        <b-form-checkbox size="sm" v-model="d.isSelected">
                          <span class="fw-bold" style="word-break: break-word">{{ d.name }}</span>
                        </b-form-checkbox>
                      </span>
                      <div class="d-flex align-items-center pt-2">
                        <span>{{ d.deploymentTime }}</span>
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
  </div>
</template>

<script>
import moment from 'moment'
import { sortDeployments } from '@/components/deployment/utils.js'

export default {
  name: 'DeploymentList',
  emits: [ 'select-deployment' ],
  props: { deployments: Array, deployment: Object, sorting: Object },
  data: function() {
    return { deploymentsGrouped: {} }
  },
  watch: {
    '$route.params.deploymentId': {
      handler: function() {
        this.setDeploymentFromUrl(this.$route.params.deploymentId)
      },
      immediate: true
    },
    deployments: {
      handler: function() {
        if (this.sorting.key === 'originalTime') this.deploymentsGrouped = this.groupByDate(this.deployments)
        if (this.sorting.key === 'name') this.deploymentsGrouped = this.groupByName(this.deployments)
      },
      immediate: true
    }
  },
  methods: {
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
    },
    groupByName: function(deployments) {
      var deploymentsGrouped = {}
      deployments.forEach(d => {
        if (!deploymentsGrouped[d.name[0]]) {
          deploymentsGrouped[d.name[0]] = { visible: true, data: [] }
        }
        deploymentsGrouped[d.name[0]].data.push(d)
      })
      Object.keys(deploymentsGrouped).forEach(key => {
        deploymentsGrouped[key].data.sort((a, b) => sortDeployments(a, b, 'originalTime', this.sorting.key))
      })
      return deploymentsGrouped
    },
    groupByDate: function(deployments) {
      var deploymentsGrouped = {}
      deployments.forEach(d => {
        var date = moment(d.originalTime).format('YYYY-MM-DD')
        if (!deploymentsGrouped[date]) {
          deploymentsGrouped[date] = { visible: true, data: [] }
        }
        deploymentsGrouped[date].data.push(d)
      })
      return deploymentsGrouped
    },
    title: function(key) {
      if (this.sorting.key === 'originalTime') {
        return moment(key).format('LL')
      } else return key
    }
  }
}
</script>

<template>
  <div class="d-flex flex-column">
    <div style="background-color: rgba(98, 142, 199, 0.2)">
      <div class="d-flex align-items-center py-2 container-fluid">
        <div class="col-3">
          <b-input-group>
            <b-input-group-prepend>
              <b-button :title="$t('searches.search')" aria-hidden="true" size="sm" class="rounded-left" variant="secondary"><span class="mdi mdi-magnify" style="line-height: initial"></span></b-button>
            </b-input-group-prepend>
            <b-input-group-append>
              <b-form-input :title="$t('searches.search')" size="sm" :placeholder="$t('searches.search')" v-model="filter"></b-form-input>
            </b-input-group-append>
          </b-input-group>
        </div>
        <div class="col-3">
          <b-form-group class="mb-0">
            <b-input-group size="sm" class="align-items-center">
              <b-input-group-prepend class="me-2 align-items-center">
                <span class="text-primary mdi mdi-18px mdi-filter-variant" :title="$t('sorting.sortBy')"></span><span>{{ $t('sorting.sortBy') }}</span>
              </b-input-group-prepend>
              <b-input-group-append class="d-flex align-items-center">
                <b-form-select size="sm" v-model="sorting.key" :options="sortingFields" class="mb-0"></b-form-select>
                <b-button size="sm" v-hover-style="{ classes: ['text-primary'] }" variant="secondary-outline" @click="changeSortingOrder()" class="mdi mdi-18px ms-1"
                  :class="sorting.order === 'desc' ? 'mdi-arrow-down' : 'mdi-arrow-up'"
                  :title="sorting.order === 'desc' ? $t('sorting.desc') : $t('sorting.asc')"></b-button>
              </b-input-group-append>
            </b-input-group>
          </b-form-group>
        </div>
        <div class="col-6">
          <b-input-group size="sm" class="align-items-center justify-content-end">
            <b-form-checkbox class="me-3" size="sm" v-model="isAllChecked">
              <span>{{ $t('deployment.selectAll') }}</span>
            </b-form-checkbox>
            <b-button class="border-secondary" size="sm" :disabled="!deploymentsSelected.length > 0 || deleteLoader" variant="light" @click="$refs.deleteModal.show()" :title="$t('deployment.deleteDeployments')">
              <span v-if="deleteLoader"><b-spinner small></b-spinner> {{ $t('deployment.deleteDeployments') }}</span>
              <span v-else class="mdi mdi-delete-outline">{{ $t('deployment.deleteDeployments') }}</span>
            </b-button>
          </b-input-group>
        </div>
      </div>
    </div>
    <SidebarsFlow ref="sidebars" class="border-top overflow-auto" v-model:right-open="rightOpen" :right-caption="$t('deployment.resourcesCaption')" :rightSize="[12, 4, 3, 3, 3]">
      <template v-slot:right>
        <ResourcesNavBar v-if="!resourcesLoading" :resources="resources" :deployment="deployment"></ResourcesNavBar>
        <b-waiting-box v-else styling="width: 35px" class="h-100 d-flex justify-content-center"></b-waiting-box>
      </template>
      <DeploymentList v-if="!loading && deploymentsFiltered.length > 0" :deployments="deploymentsFiltered" :deployment="deployment" :sorting="sorting"
        @select-deployment="selectDeployment($event)"></DeploymentList>
      <div v-else-if="!loading && deploymentsFiltered.length === 0" class="text-center text-secondary">
        <img src="/assets/images/task/no_tasks_pending.svg" class="d-block mx-auto mt-5 mb-3" style="width: 200px">
        <div class="h5 text-secondary text-center">{{ $t('deployment.noDeployments') }}</div>
      </div>
      <div v-else class="h-100 d-flex justify-content-center align-items-center">
        <b-waiting-box styling="width: 55%"></b-waiting-box>
      </div>
    </SidebarsFlow>
    <b-modal ref="deleteModal" :title="$t('confirm.title')">
      <div class="container-fluid">
        <div class="row align-items-center">
          <div class="col-2">
            <span class="mdi-36px mdi mdi-alert-outline text-warning me-3"></span>
          </div>
          <div class="col-10">
            <span>{{ $t('deployment.confirmDeleteDeployment') }}</span>
            <b-form-checkbox disabled v-model="cascadeDelete" class="mt-3">{{ $t('deployment.deleteRunningInstances') }}</b-form-checkbox>
          </div>
        </div>
      </div>
      <template v-slot:modal-footer>
        <b-button @click="$refs.deleteModal.hide()" variant="link">{{ $t('confirm.cancel') }}</b-button>
        <b-button @click="deleteDeployments(); $refs.deleteModal.hide()" variant="primary">{{ $t('confirm.delete') }}</b-button>
      </template>
    </b-modal>
    <SuccessAlert top="0" style="z-index: 1031" ref="deploymentsDeleted"> {{ $t('deployment.deploymentsDeleted', [deploymentsDelData.deleted, deploymentsDelData.total]) }}</SuccessAlert>
  </div>
</template>

<script>
import { sortDeployments } from '@/components/deployment/utils.js'
import { permissionsMixin } from '@/permissions.js'
import { ProcessService } from '@/services.js'
import DeploymentList from '@/components/deployment/DeploymentList.vue'
import ResourcesNavBar from '@/components/deployment/ResourcesNavBar.vue'
import SidebarsFlow from '@/components/common-components/SidebarsFlow.vue'
import SuccessAlert from '@/components/common-components/SuccessAlert.vue'

export default {
  name: 'DeploymentsView',
  components: { DeploymentList, ResourcesNavBar, SidebarsFlow, SuccessAlert },
  inject: ['loadProcesses'],
  mixins: [permissionsMixin],
  data: function() {
    return {
      rightOpen: false,
      deployments: [],
      deployment: null,
      loading: true,
      deleteLoader: false,
      filter: this.$route.query.filter || '',
      sorting: {},
      cascadeDelete: true,
      resources: [],
      resourcesLoading: false,
      deploymentsDelData: { total: 0, deleted: 0 }
    }
  },
  watch: {
    sorting: {
      handler: function() {
        localStorage.setItem('deploymentSorting', JSON.stringify(this.sorting))
      },
      deep: true
    }
  },
  computed: {
    deploymentsFiltered: function() {
      const filterUpper = this.filter.toUpperCase()
      return this.deployments
        .filter(d => this.isDeploymentFiltered(d, filterUpper))
        .sort((a, b) => sortDeployments(a, b, this.sorting.key, this.sorting.order))
    },
    sortingFields: function() {
      return [
        { text: this.$t('sorting.deployments.deploymentTime'), value: 'originalTime' },
        { text: this.$t('sorting.deployments.name'), value: 'name' }
      ]
    },
    deploymentsSelected: function() {
      return this.deploymentsFiltered.filter(d => {
        return d.isSelected
      })
    },
    isAllChecked: {
      get: function() {
        return this.deploymentsFiltered.length > 0 && this.deploymentsFiltered.reduce((allSelected, d) => (allSelected && d.isSelected), true)
      },
      set: function(checked) {
        this.deploymentsFiltered.forEach(d => { d.isSelected = checked })
      }
    }
  },
  created: function () {
    this.sorting = localStorage.getItem('deploymentSorting') ? JSON.parse(localStorage.getItem('deploymentSorting')) :
      { key: 'originalTime', order: 'asc' }
    ProcessService.findDeployments().then(deployments => {
      deployments.forEach(d => {
        d.isSelected = false
      })
      this.deployments = deployments
      this.loading = false
    })
  },
  methods: {
    deleteDeployments: function() {
      var vm = this
      this.deleteLoader = true
      this.deploymentsDelData.total = this.deploymentsSelected.length
      this.deploymentsDelData.deleted = 0
      var pool = this.deploymentsSelected.slice(0, this.deploymentsSelected.length)
      startTask()
      function startTask() {
        var deployment = pool.shift()
        if (deployment) {
          deleteDeployment(deployment)
        } else {
          vm.loadProcesses(false)
          vm.deleteLoader = false
          vm.$refs.deploymentsDeleted.show()
        }
      }
      function deleteDeployment(deployment) {
        ProcessService.deleteDeployment(deployment.id, true).then(() => {
          vm.deploymentsDelData.deleted++
          vm.deployments = vm.deployments.filter(df => {
            return deployment.id !== df.id
          })
          if (vm.deployment && deployment.id === vm.deployment.id) vm.deployment = null
          setTimeout(() => {
            startTask()
          }, 1000)
        })
      }
    },
    selectDeployment: function(d) {
      this.deployment = d
      this.rightOpen = true
      this.findDeploymentResources(d.id)
    },
    isDeploymentFiltered: function(d, filterUpper) {
      if (!filterUpper) {
        return true
      }
      const value = (d.name || d.id).toUpperCase()
      return value.includes(filterUpper)
    },
    findDeploymentResources: function(deploymentId) {
      this.resourcesLoading = true
      this.resources = null
      ProcessService.findDeploymentResources(deploymentId).then(resources => {
        this.resources = resources
        this.resourcesLoading = false
      }).catch(() => {
        this.resources = null
        this.resourcesLoading = false
      })
    },
    changeSortingOrder: function() {
      this.sorting.order = this.sorting.order === 'desc' ? 'asc' : 'desc'
    }
  }
}
</script>

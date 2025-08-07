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
  <div class="d-flex flex-column">
    <div style="background-color: rgba(98, 142, 199, 0.2);">
      <div class="d-flex align-items-center py-2 container-fluid">
        <div class="col-3">
          <b-input-group>
            <b-input-group-prepend>
              <b-button :title="$t('searches.search')" aria-hidden="true" size="sm" class="rounded-left"
                variant="secondary"><span class="mdi mdi-magnify" style="line-height: initial"></span></b-button>
            </b-input-group-prepend>
            <b-input-group-append>
              <b-form-input :title="$t('searches.search')" size="sm" :placeholder="$t('searches.search')"
                v-model.trim="filter"></b-form-input>
            </b-input-group-append>
          </b-input-group>
        </div>
        <div class="col-3">
          <b-form-group class="mb-0">
            <b-input-group size="sm" class="align-items-center">
              <b-input-group-prepend class="me-2 align-items-center">
                <span class="text-primary mdi mdi-18px mdi-filter-variant" :title="$t('sorting.sortBy')"></span><span>{{
                  $t('sorting.sortBy') }}</span>
              </b-input-group-prepend>
              <b-input-group-append class="d-flex align-items-center">
                <b-form-select size="sm" v-model="sortBy" :options="sortingFields" class="mb-0"></b-form-select>
                <b-button size="sm" v-hover-style="{ classes: ['text-primary'] }" variant="secondary-outline"
                  @click="changeSortingOrder()" class="mdi mdi-18px ms-1 border-0"
                  :class="sortOrder === 'desc' ? 'mdi-arrow-down' : 'mdi-arrow-up'"
                  :title="sortOrder === 'desc' ? $t('sorting.desc') : $t('sorting.asc')"></b-button>
              </b-input-group-append>
            </b-input-group>
          </b-form-group>
        </div>
        <div class="col-2 text-secondary p-0 m-0">
          <div class="">
            <span>{{ $t('deployment.title') }}:&nbsp;</span>
            <div class="d-inline-block text-nowrap">
              <span v-if="!loading">{{ deployments.length }}</span>
              <span v-else>0</span>
              /
              <span v-if="totalCount !== undefined">{{ totalCount }}</span>
              <span v-else>0</span>
            </div>
          </div>
        </div>
        <div class="col-4">
          <b-input-group size="sm" class="align-items-center justify-content-end">
            <b-form-checkbox class="me-3" size="sm" v-model="isAllChecked">
              <span>{{ $t('deployment.selectAll') }}</span>
            </b-form-checkbox>
            <b-button class="border-secondary" size="sm" :disabled="!deploymentsSelected.length > 0 || deleteLoader"
              variant="light" @click="$refs.deleteModal.show()" :title="$t('deployment.deleteDeployments')">
              <span v-if="deleteLoader"><b-spinner small></b-spinner> {{ $t('deployment.deleteDeployments') }}</span>
              <span v-else class="mdi mdi-delete-outline">{{ $t('deployment.deleteDeployments') }}</span>
            </b-button>
          </b-input-group>
        </div>
      </div>
    </div>
    <div class="flex-grow-1" style="min-height: 0">
      <SidebarsFlow class="border-top" v-model:right-open="rightOpen" :right-caption="$t('deployment.resourcesCaption')"
        :rightSize="[12, 4, 3, 3, 3]">
        <template v-slot:right>
          <ResourcesNavBar v-if="!resourcesLoading" :resources="resources" :deploymentId="deploymentId"
            @delete-deployment="deleteDeployment($event)" @show-deployment="loadToSelectedDeployment()">
          </ResourcesNavBar>
          <b-waiting-box v-else styling="width: 35px" class="h-100 d-flex justify-content-center"></b-waiting-box>
        </template>
        <div ref="scrollableArea" class="w-100 h-100 d-flex flex-column overflow-auto overflow-y-scroll">

          <PagedScrollableContent :loading="loading" :loaded-count="deployments.length" :total-count="totalCount"
            :chunk-size="maxResults" :scrollable-area="$refs.scrollableArea" @load-next-page="loadNextPage"
            :show-loading-spinner="loading && deployments.length > 0">
            <DeploymentList v-if="deployments.length > 0" :groups="groups" :deployments="deployments"
              :deployment="deployment" :deploymentId="deploymentId" :deploymentsReady="deploymentsReady"
              @select-deployment="selectDeployment">
            </DeploymentList>
            <div v-else class="h-100 d-flex justify-content-center align-items-center text-center text-secondary">
              <div v-if="loading">
                <b-waiting-box class="d-inline me-2" styling="width: 35px"></b-waiting-box> {{ $t('admin.loading') }}
              </div>
              <div v-else>
                <img src="@/assets/images/task/no_tasks_pending.svg" class="d-block mx-auto mt-5 mb-3"
                  style="width: 200px">
                <div class="h5">{{ $t('deployment.noDeployments') }}</div>
              </div>
            </div>
          </PagedScrollableContent>

        </div>
      </SidebarsFlow>
    </div>
    <b-modal ref="deleteModal" :title="$t('confirm.title')">
      <div class="container-fluid">
        <div class="row align-items-center">
          <div class="col-2">
            <span class="mdi-36px mdi mdi-alert-outline text-warning me-3"></span>
          </div>
          <div class="col-10">
            <span>{{ $t('deployment.confirmDeleteDeployment') }}</span>
            <b-form-checkbox disabled v-model="cascadeDelete" class="mt-3">{{ $t('deployment.deleteRunningInstances')
              }}</b-form-checkbox>
          </div>
        </div>
      </div>
      <template v-slot:modal-footer>
        <b-button @click="$refs.deleteModal.hide()" variant="link">{{ $t('confirm.cancel') }}</b-button>
        <b-button @click="deleteDeployments(); $refs.deleteModal.hide()" variant="primary">{{ $t('confirm.delete')
          }}</b-button>
      </template>
    </b-modal>
    <SuccessAlert top="0" style="z-index: 1031" ref="deploymentsDeleted"> {{ $t('deployment.deploymentsDeleted',
      [deploymentsDelData.deleted, deploymentsDelData.total]) }}</SuccessAlert>
  </div>
</template>

<script>
import { permissionsMixin } from '@/permissions.js'
import { ProcessService } from '@/services.js'
import { moment } from '@/globals.js'
import { debounce } from '@/utils/debounce.js'
import DeploymentList from '@/components/deployment/DeploymentList.vue'
import PagedScrollableContent from '@/components/common-components/PagedScrollableContent.vue'
import ResourcesNavBar from '@/components/deployment/ResourcesNavBar.vue'
import SidebarsFlow from '@/components/common-components/SidebarsFlow.vue'
import SuccessAlert from '@/components/common-components/SuccessAlert.vue'

export default {
  name: 'DeploymentsView',
  components: { PagedScrollableContent, DeploymentList, ResourcesNavBar, SidebarsFlow, SuccessAlert },
  inject: ['loadProcesses'],
  mixins: [permissionsMixin],
  props: { deploymentId: String },
  data: function () {
    return {
      rightOpen: false,
      groups: [],
      deployments: [],
      deployment: null,
      totalCount: undefined,
      loading: false,
      deleteLoader: false,
      filter: this.$route.query.filter || '',
      firstResult: 0,
      maxResults: 50, // The maximum number of results to fetch per page.
      sortBy: 'deploymentTime', // Enum: "id" "name" "deploymentTime" "tenantId"
      sortOrder: 'desc', // Enum: "asc" "desc"
      cascadeDelete: true,
      resources: [],
      resourcesLoading: false,
      deploymentsDelData: { total: 0, deleted: 0 },
      debouncedSearch: null,
      deploymentsReady: false,
      searchDeployment: false
    }
  },
  watch: {
    filter: function () {
      this.debouncedSearch()
    },
    sortBy: function (newSortBy) {
      localStorage.setItem('cibseven:deployments.sortBy', newSortBy)
      this.groups = []
      this.deployments = []
      this.deployment = null
      this.loadNextPage()
    },
    deploymentId: function () {
      let found = this.deployments.some(d => {
        return (d.id === this.deploymentId)
      })
      if (!found) {
        this.deploymentsReady = false
        this.loadToSelectedDeployment()
      }
    }
  },
  computed: {
    sortingFields: function () {
      return [
        { text: this.$t('sorting.deployments.deploymentTime'), value: 'deploymentTime' },
        { text: this.$t('sorting.deployments.name'), value: 'name' }
      ]
    },
    deploymentsSelected: function () {
      return this.deployments.filter(d => {
        return d.isSelected
      })
    },
    isAllChecked: {
      get: function () {
        return this.deployments.length > 0 && this.deployments.reduce((allSelected, d) => (allSelected && d.isSelected), true)
      },
      set: function (checked) {
        this.deployments.forEach(d => { d.isSelected = checked })
      }
    },
    allLoaded() {
      return (this.totalCount === undefined) ? false : (this.deployments.length >= this.totalCount)
    },
  },
  created: function () {
    this.debouncedSearch = debounce(800, this.performSearch)

    // load sortBy
    const newSortBy = localStorage.getItem('cibseven:deployments.sortBy')
    if (this.sortingFields.some((field) => field.value === newSortBy)) {
      this.sortBy = newSortBy
    } else {
      this.sortBy = 'deploymentTime'
    }

    // load sortOrder
    const newSortOrder = localStorage.getItem('cibseven:deployments.sortOrder')
    if (['asc', 'desc'].includes(newSortOrder)) {
      this.sortOrder = newSortOrder
    }
    else {
      this.sortOrder = this.sortBy === 'deploymentTime' ? 'desc' : 'asc'
    }
    if (this.deploymentId) {
      this.findDeploymentResources(this.deploymentId)
    }
    this.rightOpen = true
    this.loadNextPage()

  },
  methods: {
    performSearch: function () {
      this.groups = []
      this.deployments = []
      this.deployment = null
      this.loadNextPage()
    },
    refreshTotalCount() {
      this.totalCount = undefined
      ProcessService.findDeploymentsCount(this.filter).then(count => {
        this.totalCount = count
      })
        .catch(error => {
          console.error(error)
          this.totalCount = undefined
        })
    },
    loadNextChunk(offset) {
      if (this.loading || this.allLoaded) {
        return
      }
      this.loading = true
      // Perform the search with the current query object and offset
      this.loadDeployments(offset)
    },
    loadDeployments: function (offset) {
      let found = false
      this.loading = true
      ProcessService.findDeployments(this.filter, offset, this.maxResults, this.sortBy, this.sortOrder).then(deployments => {
        deployments.forEach(d => {
          d.isSelected = false
          d.name = d.name || d.id

          let group = '-'
          let name = '-'
          if (this.sortBy === 'name') {
            group = (d.name || '-')[0].toUpperCase() || '-'
            name = group
          }
          else {
            group = moment(d.deploymentTime).format('YYYY-MM-DD') || '-'
            name = group === '-' ? group : moment(group).format('LL')
          }

          if (this.groups.length === 0 || this.groups[this.groups.length - 1].name !== name) {
            this.groups.push({ visible: true, data: [d], name: name })
          }
          else {
            this.groups[this.groups.length - 1].data.push(d)
          }
        })
        this.deployments.push(...deployments)
        this.loading = false
        if (this.deploymentId && this.searchDeployment) {
          offset += deployments.length
          found = this.deployments.some(d => {
            if (d.id === this.deploymentId) {
              this.selectDeployment(d)
              this.searchDeployment = false
              this.loading = false
              this.deploymentsReady = true
              return true
            }
          })
          if (!found) {
            this.loadDeployments(offset)
          }
        }
      }).catch(error => {
        console.error(error)
        this.loading = false
      })
    },
    loadNextPage: function () {
      // refresh the total count in case remote data has changed
      this.refreshTotalCount()

      // Fetch next chunk based on the query object
      this.loadNextChunk(this.deployments.length)
    },
    loadToSelectedDeployment: async function () {
      this.searchDeployment = true
      this.refreshTotalCount()
      this.loadDeployments(this.deployments.length)
    },
    deleteDeployments: function () {
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
    deleteDeployment: function (deployment) {
      ProcessService.deleteDeployment(deployment.id, true).then(() => {
        this.deploymentsDelData.deleted++
        this.deployments = this.deployments.filter(d => {
          return deployment.id !== d.id
        })
        this.loadProcesses(false)
        this.$refs.deploymentsDeleted.show()
        if (this.deployment && deployment.id === this.deployment.id) this.deployment = null
      })
    },
    selectDeployment: function (d) {
      this.deployment = d
      this.rightOpen = true
      this.findDeploymentResources(d.id)
    },
    findDeploymentResources: function (deploymentId) {
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
    changeSortingOrder: function () {
      this.sortOrder = this.sortOrder === 'desc' ? 'asc' : 'desc'
      localStorage.setItem('cibseven:deployments.sortOrder', this.sortOrder)

      // Clear current deployments and reload with new sorting
      this.groups = []
      this.deployments = []
      this.deployment = null
      this.loadNextPage()
    }
  }
}
</script>

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
  <div class="d-flex flex-column bg-light pt-3">
    <div class="d-flex flex-column container bg-white border rounded shadow-sm p-0 mt-3 mb-3 overflow-y-auto">
      <div class="container pt-4">
        <div class="row align-items-center pb-1">
          <div class="col-8">
            <div class="border rounded d-flex flex-fill align-items-center">
              <b-button @click.stop="null"
                size="sm" class="mdi mdi-magnify mdi-24px text-secondary" variant="link"
                :title="$t('searches.refreshAndFilter')"></b-button>
              <div class="flex-grow-1">
                <input
                  type="text"
                  v-model.trim="filter"
                  :placeholder="$t('searches.filter')"
                  class="form-control-plaintext w-100"
                />
              </div>
              <div class="block text-secondary ms-2 me-3 text-nowrap"
                :title="$t('start.cockpit.decisions.title') + ': ' + decisionsFiltered.length"
              >{{ decisionsFiltered.length }}</div>
            </div>
          </div>
        </div>
      </div>
      <div class="container overflow-auto h-100 rounded g-0">
        <div class="m-3 mb-0">
          <FlowTable :items="decisionsFiltered" thead-class="sticky-header" striped native-layout primary-key="id" prefix="decision." :fields="fields" @click="goToDecision($event)" @select="focused = $event[0]" @mouseenter="focused = $event" @mouseleave="focused = null">
            <template v-slot:cell(actions)="table">
              <b-button :disabled="focused !== table.item" style="opacity: 1" @click.stop="goToDecision(table.item)" class="px-2 border-0 shadow-none" :title="$t('decision.showManagement')" variant="link">
                <span class="mdi mdi-18px mdi-account-tie-outline"></span>
              </b-button>
            </template>
          </FlowTable>
          <div v-if="!decisionsFiltered.length">
            <img :alt="$t(textEmptyDecisionsList)" src="@/assets/images/decision/empty_processes_list.svg" class="d-block mx-auto mt-5 mb-3" style="max-width: 250px">
            <div class="h5 text-secondary text-center">{{ $t(textEmptyDecisionsList) }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { permissionsMixin } from '@/permissions.js'
import FlowTable from '@/components/common-components/FlowTable.vue'
import { mapActions, mapGetters } from 'vuex'

export default {
  name: 'DecisionList',
  components: { FlowTable },
  mixins: [permissionsMixin],
  data: function() {
    return {
      selected: null,
      filter: '',
      focused: null,
      loadingInstances: true
    }
  },
  computed: {
    ...mapGetters(['decisionDefinitions', 'getFilteredDecisions']),
    decisionsFiltered: function() {
      return this.getFilteredDecisions(this.filter)
    },
    textEmptyDecisionsList: function() {
      return this.filter === '' ? 'decision.emptyProcessList' : 'decision.emptyProcessListFiltered' // TODO: change the images for decicions
    },
    fields: function() {
      return [
        { label: 'name', key: 'name'},
        { label: 'tenantId', key: 'tenantId'},
        { label: 'actions', key: 'actions', sortable: false, tdClass: 'py-0 text-center', thClass: 'justify-content-center' }
      ]
    }
  },
  async created() {
    await this.loadDecisions()
  },
  methods: {
    ...mapActions(['getDecisionList']),
    loadDecisions() {
      this.getDecisionList({ latestVersion: true })
    },
    goToDecision: function(decision) {
      this.$router.push('/seven/auth/decision/' + decision.key + '/' + decision.latestVersion)
    }
  }
}
</script>

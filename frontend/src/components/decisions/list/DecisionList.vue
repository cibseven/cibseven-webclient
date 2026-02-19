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
  <transition name="slide-in" mode="out-in">
    <BWaitingBox v-if="isLoading" class="h-100 d-flex justify-content-center" styling="width:20%"></BWaitingBox>
    <div v-else class="d-flex flex-column bg-light pt-3">
      <div class="d-flex flex-column container bg-white border rounded shadow-sm p-0 mt-3 mb-3 overflow-y-auto">
        <div class="container pt-4">
          <div class="row align-items-center pb-1">
            <div class="col-8">
              <div class="border rounded d-flex flex-fill align-items-center">
                <b-button @click.stop="loadDecisions()"
                  size="sm" class="mdi mdi-magnify mdi-24px text-secondary" variant="link"
                  :title="$t('searches.refreshAndFilter')"></b-button>
                <div class="flex-grow-1">
                  <label class="visually-hidden" for="filter-decision-list">{{ $t('searches.filter') }}</label>
                  <input
                    id="filter-decision-list"
                    type="text"
                    v-model.trim="filter"
                    :placeholder="$t('searches.filter')"
                    :aria-label="$t('searches.filter')"
                    class="form-control-plaintext w-100"
                  />
                </div>
                <div class="block text-secondary ms-2 me-3 text-nowrap"
                  :title="$t('start.cockpit.decisions.title') + ': ' + statistics"
                >{{ statistics }}</div>
              </div>
            </div>
          </div>
        </div>
        <div class="container overflow-auto h-100 rounded g-0">
          <div class="m-3 mb-0">
            <FlowTable :items="decisionsFiltered" thead-class="sticky-header" striped native-layout primary-key="id" :fields="fields" @click="goToDecision($event)">
              <template v-slot:cell(actions)="table">
                <CellActionButton @click="goToDecision(table.item)" :title="$t('decision.showManagement')" icon="mdi-account-tie-outline"></CellActionButton>
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
  </transition>
</template>

<script>
import { permissionsMixin } from '@/permissions.js'
import { FlowTable, BWaitingBox } from '@cib/common-frontend'
import { mapActions, mapGetters } from 'vuex'
import CellActionButton from '@/components/common-components/CellActionButton.vue'

export default {
  name: 'DecisionList',
  components: { FlowTable, BWaitingBox, CellActionButton },
  mixins: [permissionsMixin],
  data: function() {
    return {
      selected: null,
      filter: '',
    }
  },
  computed: {
    ...mapGetters(['getFilteredDecisions', 'isLoading']),
    decisionsFiltered: function() {
      return this.getFilteredDecisions(this.filter)
    },
    statistics: function() {
      const total = this.getFilteredDecisions().length
      if (total === 0) {
        return ''
      }
      const filtered = this.decisionsFiltered.length
      return filtered === total ? total : `${filtered} / ${total}`
    },
    textEmptyDecisionsList: function() {
      return this.filter === '' ? 'decision.emptyProcessList' : 'decision.emptyProcessListFiltered' // TODO: change the images for decicions
    },
    fields: function() {
      return [
        { label: 'decision.name', key: 'name'},
        { label: 'decision.tenantId', key: 'tenantId'},
        { label: 'decision.actions', key: 'actions', sortable: false, tdClass: 'py-0 d-flex justify-content-center', thClass: 'justify-content-center' }
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

<template>
  <div class="d-flex flex-column bg-light" :style="{ height: 'calc(100% - 55px)' }">
    <div class="container pt-4">
      <div class="row align-items-center pb-2">
        <div class="col-4">
          <b-input-group size="sm">
            <template #prepend>
              <b-button :title="$t('searches.search')" aria-hidden="true" class="rounded-left" variant="secondary"><span class="mdi mdi-magnify" style="line-height: initial"></span></b-button>
            </template>
            <b-form-input :title="$t('searches.search')" :placeholder="$t('searches.search')" v-model="filter"></b-form-input>
          </b-input-group>
        </div>
      </div>
    </div>
    <div class="container overflow-auto h-100 bg-white shadow g-0">
      <FlowTable :items="decisionsFiltered" thead-class="sticky-header" striped primary-key="id" prefix="decision." :fields="fields" @click="showDecision($event)" @select="focused = $event[0]" @mouseenter="focused = $event" @mouseleave="focused = null">
        <template v-slot:cell(actions)="table">
          <component :is="DecisionDefinitionActions" v-if="DecisionDefinitionActions" :focused="focused" :item="table.item"></component>
          <b-button :disabled="focused !== table.item" style="opacity: 1" @click.stop="showDecision(table.item)" class="px-2 border-0 shadow-none" :title="$t('decision.showManagement')" variant="link">
            <span class="mdi mdi-18px mdi-account-tie-outline"></span>
          </b-button>
        </template>
      </FlowTable>
      <div v-if="!decisionsFiltered.length">
        <img :alt="$t(textEmptyDecisionsList)" src="/assets/images/decision/empty_processes_list.svg" class="d-block mx-auto mt-5 mb-3" style="max-width: 250px">
        <div class="h5 text-secondary text-center">{{ $t(textEmptyDecisionsList) }}</div>
      </div>
    </div>
  </div>
</template>

<script>
import { permissionsMixin } from '@/permissions.js'
import FlowTable from '@/components/common-components/FlowTable.vue'

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
    DecisionDefinitionActions: function() {
      return this.$options.components && this.$options.components.DecisionDefinitionActions
        ? this.$options.components.DecisionDefinitionActions
        : null
    },
    decisionsFiltered: function() {
      if (!this.$store.state.decision.list) return []
      //console.log(this.$store.state.decision.list)
      var decisions = this.$store.state.decision.list.filter(decision => {
        return ((decision.key.toUpperCase().includes(this.filter.toUpperCase()) ||
            ((decision.name) ? decision.name.toUpperCase().includes(this.filter.toUpperCase()) : false)))
      })
      decisions.sort((objA, objB) => {
        var nameA = objA.name ? objA.name.toUpperCase() : objA.name
        var nameB = objB.name ? objB.name.toUpperCase() : objB.name
        var comp = nameA < nameB ? -1 : nameA > nameB ? 1 : 0

        return comp
      })
      return decisions
    },
    textEmptyDecisionsList: function() {
      return this.filter === '' ? 'decision.emptyProcessList' : 'decision.emptyProcessListFiltered' // TODO: change the images for decicions
    },
    fields: function() {
      return [
        { label: 'name', key: 'name', class: 'col-9', tdClass: 'border-end py-1 border-top-0' },
        { label: 'actions', key: 'actions', sortable: false, class: 'col-3 d-flex justify-content-center',
        tdClass: 'border-end py-0 border-top-0' },
      ]
    }
  },
  methods: {
    showDecision: function(decision) {
      this.$router.push('/seven/auth/decision/' + decision.key)
    }
  }
}
</script>

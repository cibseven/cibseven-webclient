<template>
  <div v-if="decision" class="h-100">
    <div @mousedown="handleMouseDown" class="v-resizable position-absolute w-100" style="left: 0" :style="'height: ' + bpmnViewerHeight + 'px; ' + toggleTransition">
      <DmnViewer ref="diagram" class="h-100" />
    </div>

    <ul class="nav nav-tabs position-absolute border-0 bg-light" style="left: -1px" :style="'top: ' + (bottomContentPosition - toggleButtonHeight) + 'px; ' + toggleTransition">
      <span role="button" size="sm" variant="light" class="border-bottom-0 bg-white rounded-top border py-1 px-2 me-1" @click="toggleContent">
        <span class="mdi mdi-18px" :class="toggleIcon"></span>
      </span>
      <li class="nav-item m-0" v-for="(tab, index) in tabs" :key="index">
        <a role="button" @click="changeTab(tab)" class="nav-link py-2" :class="{ 'active': tab.active, 'bg-light border border-bottom-0': !tab.active }">
          {{ $t('decision.' + tab.id) }}
        </a>
      </li>
    </ul>

    <div class="position-absolute w-100" style="left: 0; bottom: 0" :style="'top: ' + bottomContentPosition + 'px; ' + toggleTransition">
      <div v-if="activeTab === 'inputs'">
        <div ref="filterTable" class="bg-light d-flex position-absolute w-100">
          <div class="col-3 p-3">
            <b-input-group size="sm">
              <template #prepend>
                <b-button :title="$t('searches.search')" aria-hidden="true" size="sm" class="rounded-left" variant="secondary"><span class="mdi mdi-magnify" style="line-height: initial"></span></b-button>
              </template>
              <b-form-input :title="$t('searches.search')" size="sm" :placeholder="$t('searches.search')" @input="onInput"></b-form-input>
            </b-input-group>
          </div>
        </div>
        <div ref="rContent" class="overflow-auto bg-white position-absolute w-100" style="top: 60px; left: 0; bottom: 0" @scroll="handleScrollDecisions">
          <div v-if="loading" class="py-3 text-center w-100">
            <BWaitingBox class="d-inline me-2" styling="width: 35px"></BWaitingBox> {{ $t('admin.loading') }}
          </div>
          <!-- Table with inputs and outputs could go here -->
        </div>
      </div>
      <div v-if="activeTab === 'outputs'">
        <div ref="filterTable" class="bg-light d-flex position-absolute w-100">
          <div class="col-3 p-3">
            <b-input-group size="sm">
              <template #prepend>
                <b-button :title="$t('searches.search')" aria-hidden="true" size="sm" class="rounded-left" variant="secondary"><span class="mdi mdi-magnify" style="line-height: initial"></span></b-button>
              </template>
              <b-form-input :title="$t('searches.search')" size="sm" :placeholder="$t('searches.search')" @input="onInput"></b-form-input>
            </b-input-group>
          </div>
        </div>
        <div ref="rContent" class="overflow-auto bg-white position-absolute w-100" style="top: 60px; left: 0; bottom: 0" @scroll="handleScrollDecisions">
          <div v-if="loading" class="py-3 text-center w-100">
            <BWaitingBox class="d-inline me-2" styling="width: 35px"></BWaitingBox> {{ $t('admin.loading') }}
          </div>
          <!-- Table with inputs and outputs could go here -->
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { mapActions } from 'vuex'
import { permissionsMixin } from '@/permissions.js'
import DmnViewer from '@/components/decision/DmnViewer.vue'
import resizerMixin from '@/components/process/mixins/resizerMixin.js'
import { debounce } from '@/utils/debounce.js'
import { BWaitingBox } from 'cib-common-components'

export default {
  name: 'DecisionInstance',
  components: { DmnViewer, BWaitingBox },
  mixins: [permissionsMixin, resizerMixin],
  props: {
    firstResult: Number,
    maxResults: Number,
    instanceId: String,
    loading: Boolean
  },
  data() {
    return {
      tabs: [
        { id: 'inputs', active: true },
        { id: 'outputs', active: true }
      ],
      activeTab: 'inputs'
    }
  },
  watch: {
    watch: {
      '$route.params.instanceId': {
        immediate: true,
        handler(instanceId) {
          if (instanceId && this.$store.state.decision.list.length > 0) {
            this.setSelectedDecisionVersion({ key: this.decisionKey, version: this.versionIndex})
          }
        }
      }
    },
    activeTab: function () {}
  },
  mounted() {

  },
  methods: {
    ...mapActions(['getXmlById']),

    changeTab(selectedTab) {
      this.tabs.forEach(tab => {
        tab.active = tab.id === selectedTab.id
      })
      this.activeTab = selectedTab.id
    },

    handleScrollDecisions(el) {
      if (this.instances.length < this.firstResult) return
      if (Math.ceil(el.target.scrollTop + el.target.clientHeight) >= el.target.scrollHeight) {
        this.$emit('show-more')
      }
    },

    onInput: debounce(800, function (evt) {
      this.$emit('filter-instances', evt)
    })
  }
}
</script>

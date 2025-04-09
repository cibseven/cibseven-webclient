<template>
  <div class="d-flex flex-column">
    <SidebarsFlow ref="sidebars" class="border-top overflow-auto" v-model:left-open="leftOpen" :left-caption="shortendLeftCaption">
      <template v-slot:left>
        <b-list-group>
          <b-list-group-item 
            v-for="setting in systemSettings" 
            :key="setting" 
            class="border-0 px-3 py-2"
            action 
            :to="`/seven/auth/admin/system/${setting}`">
            <span> {{ $t(`admin.system.${setting}`) }}</span>
          </b-list-group-item>
        </b-list-group>
      </template>
      <router-view/>
    </SidebarsFlow>
  </div>
</template>

<script>

import SidebarsFlow from '@/components/common-components/SidebarsFlow.vue'

export default {
  name: 'SystemView',
  components: { SidebarsFlow },
  data() {
    return {
      leftOpen: true
    }
  },
  computed: {
    shortendLeftCaption() {
      return this.$t('admin.system.settings')
    },
    systemSettings() {
      return ['diagnostics', 'execution-metrics']
    }
  }
}
</script>

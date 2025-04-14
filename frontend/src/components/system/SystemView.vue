<template>
  <div class="d-flex flex-column">
    <SidebarsFlow ref="sidebars" class="border-top overflow-auto" v-model:left-open="leftOpen" :left-caption="leftCaption">
      <template v-slot:left>
        <b-list-group>
          <b-list-group-item
            v-for="setting in systemSettings"
            :key="setting"
            class="border-0 px-3 py-2 no-radius-right"
            :active="$route.path.includes(`/seven/auth/admin/system/${setting}`)"
            action
            :to="`/seven/auth/admin/system/${setting}`">
            <span>{{ $t(`admin.system.${setting}.title`) }}</span>
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
    leftCaption() {
      return this.$t('admin.system.settings')
    },
    systemSettings() {
      return ['system-diagnostics', 'execution-metrics']
    }
  }
}
</script>

<style lang="css" scoped>
.no-radius-right {
  border-top-right-radius: 0 !important;
  border-bottom-right-radius: 0 !important;
}
</style>

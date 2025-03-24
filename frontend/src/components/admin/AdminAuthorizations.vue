<template>
  <div>
    <SidebarsFlow ref="sidebars" v-model:left-open="leftOpen"
      :left-caption="$t('seven.types')" :left-size="[12, 6, 4, 3, 2]">
      <template v-slot:left>
        <AuthorizationsNavBar ref="navbar" @middle="$refs.sidebars.showMain(false)" class="h-100" style="overflow-y: auto"></AuthorizationsNavBar>
      </template>

      <router-view v-slot="{ Component }">
        <transition name="slide-in" mode="out-in">
          <component v-if="$route.params.resourceTypeId !== undefined" :is="Component" class="h-100" style="overflow-y: auto" />
          <div v-else>
            <div class="text-center text-secondary">
              <img src="/assets/images/task/no_tasks_pending.svg" class="d-block mx-auto mt-5 mb-3" style="width: 200px">
              <h5>{{ $t('admin.authorizations.noAuthSelected') }}</h5>
            </div>
          </div>
        </transition>
      </router-view>
    </SidebarsFlow>
  </div>
</template>

<script>
import AuthorizationsNavBar from '@/components/admin/AuthorizationsNavBar.vue'
import SidebarsFlow from '@/components/common-components/SidebarsFlow.vue'

export default {
  components: { AuthorizationsNavBar, SidebarsFlow },
  data: function () {
    return {
      leftOpen: true
    }
  }
}
</script>

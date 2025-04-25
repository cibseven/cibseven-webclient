<template>
  <div class="col-3">
    <router-link :to="link" :title="tooltip" class="text-decoration-none">
      <div class="bg-light py-3 text-center">
        <h5 class="link-dark">{{ title }}</h5>
        <h2 class="link-dark">
          <span v-if="count !== null" :class="computedValueClass">{{ count }}</span>
          <span v-else><BWaitingBox class="d-inline" styling="width: 24px" :title="$t('admin.loading')"></BWaitingBox></span>
        </h2>
      </div>
    </router-link>
  </div>
</template>

<script>
import { BWaitingBox } from 'cib-common-components'

export default {
  name: 'DeploymentItem',
  components: { BWaitingBox },
  props: {
    title: String,
    tooltip: String,
    count: [ Number, String ],
    link: String
  },
  computed: {
    computedValueClass: function() {
      return this.count === 'x' ? 'text-warning' : ''
    },
  }
}
</script>

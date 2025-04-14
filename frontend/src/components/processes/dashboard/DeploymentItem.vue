<template>
  <div class="col-3">
    <router-link :to="link" :title="$t(tooltip)" class="text-decoration-none">
      <div class="bg-light py-3 m-2 text-center">
        <h5 class="link-dark">{{ $t(title) }}</h5>
        <h4 class="link-dark">
          <span v-if="count !== null">{{ count }}</span>
          <span v-else><BWaitingBox class="d-inline" styling="width: 19px" :title="$t('admin.loading')"></BWaitingBox></span>
        </h4>
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
    titlePrefix: String,
    count: [ Number, String ],
    link: String
  },
  computed: {
    title: function() {
      return this.titlePrefix + '.title'
    },
    tooltip: function() {
      return this.titlePrefix + '.tooltip'
    },
  }
}
</script>

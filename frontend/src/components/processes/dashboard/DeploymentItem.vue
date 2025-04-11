<template>
  <div class="col-3">
    <router-link :to="link" :title="$t(tooltip)" class="text-decoration-none">
      <div class="bg-light py-3 m-2 text-center">
        <h5 class="link-dark">{{ $t(title) }}</h5>
        <h4 class="link-dark">{{ count }}</h4>
      </div>
    </router-link>
  </div>
</template>

<script>
export default {
  name: 'DeploymentItem',
  components: {  },
  props: {
    titlePrefix: String,
    count: Number,
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

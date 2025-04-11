<template>
  <div>
    <div class="bg-light py-3 text-center">
      <h5>
        <router-link :to="link" :title="$t(tooltip)" class="text-decoration-none">
          <span class="link-dark">{{ $t(title) }}</span>
        </router-link>
      </h5>
    </div>
  </div>
</template>

<script>
export default {
  name: 'PieChart',
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

<template>
  <div class="tab-pane fade" :class="{ 'show active': isActive }"
    :id="'nav-' + id" role="tabpanel" :aria-labelledby="'nav-' + id + '-tab'">
    <slot></slot>
  </div>
</template>

<script>
export default {
  props: {
    id: { type: String, required: true },
    title: { type: String, required: true }
  },
  inject: ['registerTab', 'activeTab', 'registerClickHandler'],
  computed: {
    isActive() {
      return this.activeTab() === this.id;
    }
  },
  methods: {
    handleClick: function () {
      this.$emit('click')
    },
    externalClick: function (tabId) {
      if (tabId === this.id) {
        this.handleClick()
      }
    }
  },
  created() {
    this.registerTab({ id: this.id, title: this.title })
    this.registerClickHandler(this.externalClick)
  }
}
</script>

<style scoped>
</style>

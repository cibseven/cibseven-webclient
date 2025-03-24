<template>
  <form
    :class="formClasses"
    @submit="handleSubmit"
    :novalidate="novalidate"
    role="form"
    :aria-describedby="ariaDescribedby">
    <slot></slot>
  </form>
</template>

<script>
export default {
  props: {
    novalidate: { type: Boolean, default: false },
    formClass: { type: String, default: '' },
    ariaDescribedby: { type: String, default: null }
  },
  computed: {
    formClasses: function () {
      return ['needs-validation', this.formClass]
    }
  },
  methods: {
    handleSubmit: function (event) {
      if (!this.novalidate && !event.target.checkValidity()) {
        event.preventDefault()
        event.stopPropagation()
        this.$emit('validation-failed', event)
      } else {
        this.$emit('submit', event)
      }
      event.target.classList.add('was-validated')
    }
  }
}
</script>

<style scoped>
</style>

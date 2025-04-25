<template>
  <form @submit.prevent="onSubmit" :class="{ 'was-validated': showValidation }" ref="form" novalidate>
    <slot :showValidation="showValidation"></slot>
  </form>
</template>

<script>
export default {
  name: 'CIBForm',
  data: function() {
    return { showValidation: false }
  },
  methods: {
    onSubmit: function() {
      if (this.$refs.form.checkValidity()) {
        this.showValidation = false
        this.$emit('submitted')
        return true
      } else {
        this.showValidation = true
        this.$emit('fail')
        return false
      }
    }
  }
}
</script>

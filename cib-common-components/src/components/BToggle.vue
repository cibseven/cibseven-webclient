<script>
const BToggle = eventBus => {
  return {
    mounted(el, binding) {
      let targetId = Object.keys(binding.modifiers)[0] || binding.value

      if (!targetId) {
        console.warn('v-b-toggle requires a modifier or a value with the ID of the target collapse.')
        return
      }

      if (!eventBus) {
        console.warn('No event bus found to toggle collapse with ID ' + targetId)
        return
      }

      el.addEventListener('click', () => {
        eventBus.emit('bv::toggle::collapse', targetId)
      })
    }
  }
}

export default BToggle
</script>

<style scoped>
</style>

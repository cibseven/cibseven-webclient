<script>
import { Popover } from 'bootstrap'

export default {
  mounted(el, binding) {
    let content = binding.value || el.getAttribute('data-bs-content') || ''
    let title = el.getAttribute('title') || ''
    let placement = Object.keys(binding.modifiers)[1] || 'top'
    let event = Object.keys(binding.modifiers)[0] || 'hover'

    if (!content) return

    const popover = new Popover(el, {
      title,
      content,
      placement,
      trigger: event,
      html: true
    })

    el._bs_popover = popover
  },
  beforeUnmount(el) {
    if (el._bs_popover) {
      el._bs_popover.dispose()
      delete el._bs_popover
    }
  }
}
</script>

<style scoped>
</style>


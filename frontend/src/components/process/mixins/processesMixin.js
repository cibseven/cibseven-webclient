export default {
  props: { view: String, processName: String },
  data: function() {
    return {
      focused: null
    }
  },
  methods: {
    onImageLoadFailure: function(event) {
      event.target.src = 'assets/images/process/default.svg'
    },
    showDescription: function(key) {
      if (this.$te('process-descriptions.' + key)) return this.$t('process-descriptions.' + key)
      return ''
    }
  }
}

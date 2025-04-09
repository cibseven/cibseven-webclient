<template>
    <ProcessDefinitionView
      v-if="!loading && computedVersionIndex"
      :processKey="processKey"
      :versionIndex="computedVersionIndex"
      :instanceId="computedInstanceId"
    ></ProcessDefinitionView>
</template>

<script>
import ProcessDefinitionView from '@/components/process/ProcessDefinitionView.vue'

export default {
  name: 'ProcessView',
  components: { ProcessDefinitionView },
  props: {
    processKey: { type: String, required: true },
    versionIndex: { type: String, default: '' },
    instanceId: { type: String, default: '' }
  },
  watch: {
    processKey: 'loadProcess',    
    versionIndex: 'loadProcess',
    instanceId: 'loadProcess'
  },
  data: function() {
    return {
      process: null,
      loading: false
    }
  },
  computed: {
    computedVersionIndex: function() {
      if (this.loading) {
        return ''
      }
      else if (this.versionIndex) {
        return this.versionIndex
      }
      else if (this.process !== null) {
        return this.process.version
      }
      else {
        return ''
      }
    },
    computedInstanceId: function() {
      // only valid with proper "versionIndex"
      return this.versionIndex ? this.instanceId : '';
    }
  },
  methods: {
    loadProcess: function() {
      if (!this.versionIndex) {
        this.loading = true
        this.$store.dispatch('getProcessByDefinitionKey', { key: this.processKey }).then(process => {
          this.process = process
          this.loading = false
        })
      }
    }
  },
  created: function() {
    this.loadProcess()
  },
  beforeUpdate: function() {
    if (this.process != null && this.process.version !== this.computedVersionIndex) {
      this.process = null
    }
  },
}
</script>

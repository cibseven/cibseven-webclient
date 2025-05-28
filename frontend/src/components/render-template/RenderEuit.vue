<template>
  <BWaitingBox v-if="loader" class="h-100 d-flex justify-content-center" ref="loader" styling="width:20%"></BWaitingBox>
  <div v-show="!loader">
    <iframe v-once class="h-100 w-100" ref="euit-frame" frameBorder="0"
            src="" allow="fullscreen"
            allowfullscreen webkitallowfullscreen mozallowfullscreen oallowfullscreen msallowfullscreen></iframe>
  </div>
</template>

<script>
import { UiLocator } from 'cibseven-ui-locator'
import { BWaitingBox } from 'cib-common-components'

export default {
  name: 'RenderEuit',
  components: { BWaitingBox },
  emits: ['complete-task', 'error', 'cancel'],
  props: ['task'],
  data() {
    return {
      loader: true,
      locator: null
    }
  },
  watch: {
    task: {
      handler: function() {
        this.loadEuit()
      }
    }
  },
  mounted() {
    this.loadEuit()
  },
  beforeUnmount() {
    this.locator?.destroy()
  },
  methods: {
    loadEuit() {
      this.loader = true;
      if (!this.locator) {
        const formFrame = this.$refs['euit-frame']
  
        const resolveCallback = () => {
          this.$emit('complete-task', this.task)
        }
        const errorCallback = (error) => {
          this.$emit('error', error)
        }
        // Use UiLocator from local npm package
        this.locator = new UiLocator(
          this.$root.config.servicesBasePath + '/locator',
          formFrame,
          resolveCallback,
          errorCallback
        );
        const cancelCallback = () => {
          this.$emit('cancel')
        }
        this.locator.onCancel(cancelCallback);
      }

      this.locator.setAuthorization(this.$root.user.authToken);
      // Detect if this is a startform or a user task
      if (this.task.isStartform) {
        this.locator.showStartform(this.task.processDefinitionId)
      } else {
        this.locator.showUserTask(this.task.id)
      }
      this.loader = false
    }
  }
}
</script>

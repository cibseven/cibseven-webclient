<template>
  <b-modal ref="annotationModal" :title="$t('process-instance.incidents.editAnnotation')">
    <div>
      <b-form-group>
        <template #label>{{ $t('process-instance.incidents.annotation') }}</template>
        <b-form-textarea v-model="annotation" :maxlength="annotationMaxLength" class="mb-1"></b-form-textarea>
        <div class="small float-end" :class="{ 'text-danger': invalidAnnotation }">{{ annotationLengthInfo }}</div>
      </b-form-group>
    </div>
    <template v-slot:modal-footer>
      <b-button @click="$refs.annotationModal.hide()" variant="link">{{ $t('confirm.cancel') }}</b-button>
      <b-button @click="saveAnnotation()" :disabled="invalidAnnotation" variant="primary">{{ $t('commons.save') }}</b-button>
    </template>
  </b-modal>
</template>

<script>

export default {
  name: 'AnnotationModal',
  data: function() {
    return {
      incidentId: null,
      annotation: null,
      annotationMaxLength: 4000
    }
  },
  computed: {
    invalidAnnotation: function() {
      if (!this.annotation) return false
      return this.annotation.length > this.annotationMaxLength
    },
    annotationLengthInfo: function() {
      const length = this.annotation?.length || 0
      return `${length} / ${this.annotationMaxLength}`
    }
  },
  methods: {
    show: function(id, annotation) {
      this.incidentId = id
      this.annotation = annotation
      this.$refs.annotationModal.show()
    },
    hide: function() {
      this.$refs.annotationModal.hide()
    },
    saveAnnotation: function() {
      if (this.invalidAnnotation) return
      var params = { annotation: this.annotation }
      this.$emit('set-incident-annotation', {
          id: this.incidentId,
          params
      })
    }
  }
}
</script>

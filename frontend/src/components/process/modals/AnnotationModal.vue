<!--

    Copyright CIB software GmbH and/or licensed to CIB software GmbH
    under one or more contributor license agreements. See the NOTICE file
    distributed with this work for additional information regarding copyright
    ownership. CIB software licenses this file to you under the Apache License,
    Version 2.0; you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

     Unless required by applicable law or agreed to in writing, software
     distributed under the License is distributed on an "AS IS" BASIS,
     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     See the License for the specific language governing permissions and
     limitations under the License.

-->
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
  emits: ['set-incident-annotation'],
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

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
  <fieldset class="form-group" :id="id" role="group" :aria-labelledby="label ? labelId : null">
    <div class="row gx-1">
      <legend v-if="label || $slots.label" tabindex="-1" :id="labelId" :class="[labelClass, labelColumnClasses, labelSizeClass]">
          <slot name="label">{{ label }}</slot>
      </legend>
      <div :class="inputColumnClasses">
          <slot></slot>
          <div v-if="state === false" class="invalid-feedback d-block" role="alert" aria-live="assertive">{{ invalidFeedback }}</div>
      </div>
    </div>
  </fieldset>
</template>

<script>
export default {
  props: {
    label: { type: String, default: '' },
    labelFor: { type: String, default: null },
    labelCols: { type: [Number, String], default: null },
    labelColsSm: { type: [Number, String], default: null },
    labelColsMd: { type: [Number, String], default: null },
    labelColsLg: { type: [Number, String], default: null },
    labelColsXl: { type: [Number, String], default: null },
    contentCols: { type: [Number, String], default: null },
    contentColsSm: { type: [Number, String], default: null },
    contentColsMd: { type: [Number, String], default: null },
    contentColsLg: { type: [Number, String], default: null },
    contentColsXl: { type: [Number, String], default: null },
    labelSize: { type: String, default: null },
    labelClass: { type: String, default: '' },
    state: { type: Boolean, default: null },
    invalidFeedback: { type: String, default: '' },
    id: { type: String, default: null }
  },
  computed: {
    labelColumnClasses: function () {
      const classes = []
      if (this.labelCols) classes.push(`col-${this.labelCols}`)
      if (this.labelColsSm) classes.push(`col-sm-${this.labelColsSm}`)
      if (this.labelColsMd) classes.push(`col-md-${this.labelColsMd}`)
      if (this.labelColsLg) classes.push(`col-lg-${this.labelColsLg}`)
      if (this.labelColsXl) classes.push(`col-xl-${this.labelColsXl}`)
      classes.push('col-form-label') // Bootstrap form label class
      return classes.join(' ')
    },
    inputColumnClasses: function () {
      const classes = []
      if (this.contentCols) classes.push(`col-${this.contentCols}`)
      if (this.contentColsSm) classes.push(`col-sm-${this.contentColsSm}`)
      if (this.contentColsMd) classes.push(`col-md-${this.contentColsMd}`)
      if (this.contentColsLg) classes.push(`col-lg-${this.contentColsLg}`)
      if (this.contentColsXl) classes.push(`col-xl-${this.contentColsXl}`)
      return classes.length ? classes.join(' ') : 'col' // Fallback to `col` if no content columns specified
    },
    labelSizeClass: function () {
      return this.labelSize ? `col-form-label-${this.labelSize}` : ''
    },
    labelId: function () {
      return this.labelFor || `${this.id || 'form-group'}__label`
    }
  }
}
</script>

<style scoped>
</style>

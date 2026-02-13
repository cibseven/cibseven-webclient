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
  <div class="container overflow-auto h-100 bg-white shadow-sm border rounded">
    <FlowTable striped :items="processes" primary-key="id" :fields="fields" @select="focused = $event[0]"
      @mouseenter="focused = $event" @focusin="focused = $event"
      @mouseleave="focused = null" @focusout="focused = null">
      <template v-slot:cell(favorite)="table">
        <b-button :title="$t('process.favorite')" tabindex="-1" @click="$emit('favorite', table.item)" variant="link" class="mdi mdi-24px" :class="table.item.favorite ? 'mdi-star text-primary' : 'mdi-star-outline text-secondary'"></b-button>
      </template>
      <template v-slot:cell(name)="table">
        <b-button variant="link" @click="$emit('start-process', table.item)" :title="table.item.name || table.item.key" class="ps-0 pe-0 text-start">
          <HighlightedText :text="table.item.name || table.item.key" :keyword="processesFilter"/>
        </b-button>
      </template>
      <template v-slot:cell(key)="table">
        <HighlightedText :text="table.item.key" :keyword="processesFilter" :title="table.item.key"/>
      </template>
      <template v-slot:cell(tenantId)="table">
        <div class="text-truncate" :title="$t('process.tenant') + ': ' + table.item.tenantId">{{ table.item.tenantId }}</div>
      </template>
      <template v-slot:cell(description)="table">
        <div v-if="getDescription(table.item)" v-b-popover.hover.left="getDescription(table.item)" v-html="getDescription(table.item)"
          :class="['inline-description', { 'with-mask': isDescriptionTruncated(table.item) }]" >
        </div>
      </template>
      <template v-slot:cell(actions)="table">
        <transition name="fade">
          <div v-show="(focused && focused.id === table.item.id)">
            <b-button v-if="table.item.suspended !== 'true'" variant="primary" @click="$emit('start-process', table.item)">{{ $t('process.start') }}</b-button>
          </div>
        </transition>
      </template>
    </FlowTable>
  </div>
</template>

<script>
import { FlowTable, HighlightedText } from '@cib/common-frontend'

export default {
  name: 'ProcessTable',
  components: { FlowTable, HighlightedText },
  props: {
    processes: Array,
    processesFilter: String
  },
  data() {
    return {
      focused: null
    }
  },
  emits: ['favorite', 'start-process'],
  computed: {
    fields: function() {
      return [
        { label: 'process.favorite', key: 'favorite', sortable: false, thClass:'py-0', tdClass:'py-0 ps-0', class: 'col-1 d-flex align-items-center justify-content-center'},
        { label: 'process.name', key: 'name', class: 'col-3' },
        { label: 'process.key', key: 'key', class: 'col-2' },
        { label: 'process.tenant', key: 'tenantId', class: 'col-2' },
        { label: 'process.description', key: 'description', sortable: false, class: 'col-2' },
        { label: 'process.actions', key: 'actions', sortable: false, tdClass: 'py-0', class: 'col-2 d-flex justify-content-center' },
      ]
    }
  },
  methods: {
    getDescription(process) {
      if (this.$te('process-descriptions.' + process.key)) return this.$t('process-descriptions.' + process.key)
      return process.description || ''
    },
    isDescriptionTruncated(process) {
      const desc = this.getDescription(process)
      if (!desc) return false
      // Approximate: check if text length suggests more than 5 lines (rough estimate)
      const avgCharsPerLine = 25
      return desc.length > (avgCharsPerLine * 5)
    }
  }  
}
</script>

<style lang="css" scoped>
.inline-description {
  display: -webkit-box;
  line-clamp: 5;
  -webkit-line-clamp: 5;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.inline-description.with-mask {
  -webkit-mask-image: linear-gradient(to bottom, black 50%, transparent 100%);
  mask-image: linear-gradient(to bottom, black 50%, transparent 100%);
}
</style>

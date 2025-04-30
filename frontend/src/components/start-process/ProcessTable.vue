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
    <FlowTable striped :items="processes" primary-key="id" prefix="process." :fields="fields" @select="focused = $event[0]" @mouseenter="focused = $event" @mouseleave="focused = null">
      <template v-slot:cell(favorite)="table">
        <b-button :title="$t('process.favorite')" tabindex="-1" @click="$emit('favorite', table.item)" variant="link" class="mdi mdi-24px" :class="table.item.favorite ? 'mdi-star text-primary' : 'mdi-star-outline text-secondary'"></b-button>
      </template>
      <template v-slot:cell(actions)="table">
        <transition name="fade">
          <div v-show="(focused && focused.id === table.item.id)">
            <b-button v-if="table.item.suspended !== 'true'" variant="primary" @click="$emit('start-process', table.item)">{{ $t('process.start') }}</b-button>
          </div>
        </transition>
      </template>
      <template v-slot:cell(description)="table">
        <div v-if="$te('process-descriptions.' + table.item.key)" v-b-popover.hover.left="$t('process-descriptions.' + table.item.key)" class="text-truncate">
          {{ $t('process-descriptions.' + table.item.key) }}
        </div>
      </template>
    </FlowTable>
  </div>
</template>

<script>
import { permissionsMixin } from '@/permissions.js'
import processesMixin from '@/components/process/mixins/processesMixin.js'
import FlowTable from '@/components/common-components/FlowTable.vue'

export default {
  name: 'ProcessTable',
  components: { FlowTable },
  mixins: [permissionsMixin, processesMixin],
  props: { processes: Array },
  computed: {
    fields: function() {
      return [{ label: 'favorite', key: 'favorite', sortable: false, thClass:'py-0', tdClass:'py-0 ps-0',
        class: 'col-1 d-flex align-items-center justify-content-center'},
          { label: 'name', key: 'name', class: 'col-3' },
          { label: 'key', key: 'key', class: 'col-2' },
          { label: 'tenant', key: 'tenantId', class: 'col-2' },
          { label: 'description', key: 'description', sortable: false, class: 'col-2' },
          { label: 'actions', key: 'actions', sortable: false, tdClass: 'py-0', class: 'col-2 d-flex justify-content-center' },
        ]
    }
  }
}
</script>

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
  <FlowTable v-if="items && items.length > 0" striped :resizable="false" thead-class="sticky-header" :items="items"
    primary-key="buttons"
    prefix=""
    sort-by=""
    :fields="[
      { label: 'infoAndHelp.shortcuts.columnShortcut', key: 'buttons', class: 'col-4 justify-content-center', tdClass: 'py-1', sortable: false },
      { label: 'infoAndHelp.shortcuts.columnDescription', key: 'description', class: 'col-8 ps-0', tdClass: 'py-1', sortable: false },
    ]">
    <template v-slot:cell(buttons)="table">
      <span v-for="(button, index) in table.item.buttons" :key="index"
        :class="button === '+'? 'text-body-secondary': 'btn btn-outline-secondary py-0 px-2 m-1'">
        {{ button === "+" ? "+": (button === "Ctrl" ? $t('infoAndHelp.shortcuts.buttonCtrl') : button) }}
      </span>
    </template>
    <template v-slot:cell(description)="table">
      {{ $t(table.item.description) }}
    </template>
  </FlowTable>
</template>

<script>
import FlowTable from '@/components/common-components/FlowTable.vue'

export default {
  name: 'ShortcutsTable',
  components: { FlowTable },
  props: {
    items: Array
  }
}
</script>

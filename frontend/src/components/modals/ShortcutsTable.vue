<template>
  <FlowTable striped :resizable="false" thead-class="sticky-header" :items="items"
    primary-key="buttons"
    prefix=""
    sort-by=""
    :fields="[
      { label: 'infoAndHelp.shortcuts.columnShortcut', key: 'buttons', class: 'col-4 justify-content-center', tdClass: 'py-1 border-top-0', sortable: false },
      { label: 'infoAndHelp.shortcuts.columnDescription', key: 'description', class: 'col-8 ps-0', tdClass: 'py-1 border-top-0', sortable: false },
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

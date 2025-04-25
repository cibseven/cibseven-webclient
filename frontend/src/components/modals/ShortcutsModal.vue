<template>
  <b-modal ref="shortcuts" :title="$t('infoAndHelp.shortcuts.title')" :ok-only="true" size="lg">
    <div class="row">
      <div class="col-2 d-flex justify-content-center">
        <span class="mdi mdi-48px mdi-keyboard-outline pe-1 text-info"></span>
      </div>
      <div class="col-10 ps-0">
        <h5>{{ $t('infoAndHelp.flowModalAbout.title') }}</h5>
        <ShortcutsTable :items="itemsGlobal"></ShortcutsTable>
        <h5>{{ $t('start.taskList.title') }}</h5>
        <ShortcutsTable :items="itemsTasks"></ShortcutsTable>
      </div>
    </div>
  </b-modal>
</template>

<script>
import ShortcutsTable from '@/components/modals/ShortcutsTable.vue'

export default {
  name: 'ShortcutsModal',
  components: { ShortcutsTable },
  data: function() {
    return {
      itemsGlobal: [{
          buttons: ["Ctrl", "+", "🠄"],
          description: 'infoAndHelp.shortcuts.shortcuts.ctrlLeft',
        }, {
          buttons: ["Ctrl", "+", "🠇"],
          description: 'infoAndHelp.shortcuts.shortcuts.ctrlDown',
        }, {
          buttons: ["Ctrl", "+", "🠆"],
          description: 'infoAndHelp.shortcuts.shortcuts.ctrlRight',
        }
      ],
      itemsTasks: [{
          buttons: ["Ctrl", "+", "Alt", "+", "C"],
          description: 'infoAndHelp.shortcuts.shortcuts.ctrlAltC',
        }, {
          buttons: ["Ctrl", "+", "Alt", "+", "P"],
          description: 'infoAndHelp.shortcuts.shortcuts.ctrlAltP',
        }, {
          buttons: ["Alt", "+", "1"],
          description: 'infoAndHelp.shortcuts.shortcuts.alt1',
        }, {
          buttons: ["Alt", "+", "2"],
          description: 'infoAndHelp.shortcuts.shortcuts.alt2',
        }, {
          buttons: ["Alt", "+", "3"],
          description: 'infoAndHelp.shortcuts.shortcuts.alt3',
        }
      ]
    }
  },
  methods: {
    show: function() {
      this.$refs.shortcuts.show()
    }
  }
}
</script>

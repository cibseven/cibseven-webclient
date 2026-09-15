<template>
  <table class="table table-sm">
    <thead>
      <tr>
        <th scope="col">{{ $t('plugins.demo-report.name') }}</th>
        <th scope="col">{{ $t('plugins.demo-report.key') }}</th>
        <th scope="col" class="text-end">{{ $t('plugins.demo-report.version') }}</th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="row in rows" :key="row.id">
        <td>
          <!-- a button, not a link: the plugin asks the application to navigate
               instead of building a URL of its own -->
          <button type="button" class="btn btn-link p-0 border-0 align-baseline"
            @click="open(row)">{{ row.name }}</button>
        </td>
        <td class="text-muted">{{ row.key }}</td>
        <td class="text-end demo-report-value">{{ row.version }}</td>
      </tr>
      <tr v-if="!rows.length">
        <td colspan="3" class="text-muted">{{ $t('plugins.demo-report.empty') }}</td>
      </tr>
    </tbody>
  </table>
</template>

<script>
import { navigation } from '@cibseven/plugin-runtime'

export default {
  name: 'ProcessTable',
  props: {
    rows: { type: Array, default: () => [] }
  },
  methods: {
    // route names and params are the application's; a plugin only asks to go there
    open(row) {
      navigation.push({ name: 'process-definition-id', params: { definitionId: row.id } })
    }
  }
}
</script>

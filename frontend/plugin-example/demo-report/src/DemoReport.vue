<template>
  <!-- h-100 overflow-auto: the slot renders this straight into the tab area, which clips.
       Scrolling is the contribution's own, exactly as for the built-in tabs. -->
  <div class="container-fluid px-4 p-4 h-100 overflow-auto demo-report">
    <h5 class="demo-report-title">{{ $t('plugins.demo-report.title') }}</h5>

    <p class="mb-3 demo-report-instance">
      {{ $t('plugins.demo-report.instance') }}: {{ instance?.id ?? '-' }}
    </p>

    <div v-if="error" class="alert alert-warning py-2">
      {{ $t('plugins.demo-report.failed') }}: {{ error }}
    </div>

    <template v-else>
      <div class="d-flex gap-3 mb-3">
        <SummaryCard :label="$t('plugins.demo-report.total')" :value="rows.length"></SummaryCard>
        <SummaryCard :label="$t('plugins.demo-report.definition')" :value="process?.key ?? '-'"></SummaryCard>
      </div>

      <ProcessTable :rows="rows"></ProcessTable>
    </template>
  </div>
</template>

<script>
import { services } from '@cibseven/plugin-runtime'
import ProcessTable from './ProcessTable.vue'
import SummaryCard from './SummaryCard.vue'

export default {
  name: 'DemoReport',
  // components of the same plugin are imported as usual
  components: { ProcessTable, SummaryCard },
  props: {
    // handed over by the slot
    instance: { type: Object, default: null },
    process: { type: Object, default: null }
  },
  data() {
    return { rows: [], error: null }
  },
  async mounted() {
    try {
      // the application's axios instance, so this carries the user's authentication
      const processes = await services.ProcessService.findProcesses()
      this.rows = (Array.isArray(processes) ? processes : []).map(process => ({
        id: process.id,
        key: process.key,
        name: process.name ?? process.key,
        version: process.version ?? '-'
      }))
    } catch (error) {
      // a user without the right permissions gets a 403 here
      this.error = error?.response?.status ?? 'error'
    }
  }
}
</script>

<style>
.demo-report-instance {
	font-size: 0.9rem;
	opacity: 0.8;
}
</style>

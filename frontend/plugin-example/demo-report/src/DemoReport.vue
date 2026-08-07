<template>
  <div class="container-fluid px-4 p-4 demo-report">
    <h5 class="demo-report-title">{{ $t('plugins.demo-report.title') }}</h5>

    <p class="mb-3">{{ $t('plugins.demo-report.instance') }}: {{ instance?.id ?? '-' }}</p>

    <table class="table table-sm">
      <thead>
        <tr>
          <th scope="col">{{ $t('plugins.demo-report.name') }}</th>
          <th scope="col" class="text-end">{{ $t('plugins.demo-report.count') }}</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in rows" :key="row.key">
          <td>{{ row.name }}</td>
          <td class="text-end demo-report-value">{{ row.count }}</td>
        </tr>
        <tr v-if="!rows.length">
          <td colspan="2" class="text-muted">{{ $t('plugins.demo-report.empty') }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script>
import { services } from '@cibseven/plugin-runtime'

export default {
  name: 'DemoReport',
  props: {
    // handed over by the slot
    instance: { type: Object, default: null },
    process: { type: Object, default: null }
  },
  data() {
    return { rows: [] }
  },
  async mounted() {
    // the application's axios instance, so this carries the user's authentication
    const processes = await services.ProcessService.findProcesses()
    this.rows = (Array.isArray(processes) ? processes : [])
      .slice(0, 5)
      .map(process => ({ key: process.key, name: process.name ?? process.key, count: process.version ?? 0 }))
  }
}
</script>

<style>
/* Nothing scopes plugin styles, so every selector is prefixed */
.demo-report-title {
	display: flex;
	align-items: center;
	gap: 0.5rem;
}

.demo-report-value {
	font-variant-numeric: tabular-nums;
	font-weight: 600;
}
</style>

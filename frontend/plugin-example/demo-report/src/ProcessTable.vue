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
        <td>{{ row.name }}</td>
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
export default {
  name: 'ProcessTable',
  props: {
    rows: { type: Array, default: () => [] }
  }
}
</script>

<style>
.demo-report-value {
	font-variant-numeric: tabular-nums;
	font-weight: 600;
}
</style>

<template>
  <div class="card demo-report-card">
    <div class="card-body py-2">
      <div class="demo-report-card-label">{{ label }}</div>
      <div class="demo-report-card-value">{{ value }}</div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'SummaryCard',
  props: {
    label: { type: String, required: true },
    value: { type: [String, Number], default: '-' }
  }
}
</script>

<style>
.demo-report-card {
	min-width: 8rem;
}

.demo-report-card-label {
	font-size: 0.8rem;
	opacity: 0.75;
}

.demo-report-card-value {
	font-size: 1.25rem;
	font-weight: 600;
	font-variant-numeric: tabular-nums;
}
</style>

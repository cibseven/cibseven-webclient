<template>
	<div>
		Coming soon
	</div>
</template>
  
<script>
  
import { SystemService } from '@/services.js'

export default {
	name: 'ExecutionMetrics',
	data() {
		return {
			metrics: null
		}
	},
	async mounted() {
		this.metrics = await SystemService.getMetrics()
	}
}

</script>
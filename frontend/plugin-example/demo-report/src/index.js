import { registerPlugin } from '@cibseven/plugin-runtime'
import DemoReport from './DemoReport.vue'

export function register({ id }) {
  registerPlugin('process-instance-tab', DemoReport, {
    pluginId: id,
    id: 'demo-report',
    text: `plugins.${id}.title`
  })
}

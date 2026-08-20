import { registerPlugin } from '@cibseven/plugin-runtime'
import DemoReport from './DemoReport.vue'
// bundled into styles.css together with the components' <style> blocks
import './styles.css'

export function register({ id }) {
  registerPlugin('process-instance-tab', DemoReport, {
    pluginId: id,
    id: 'demo-report',
    text: `plugins.${id}.title`
  })
}

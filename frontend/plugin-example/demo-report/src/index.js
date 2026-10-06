import DemoReport from './DemoReport.vue'
// bundled into styles.css together with the components' <style> blocks
import './styles.css'

export function register({ id, registerPlugin }) {
  registerPlugin('process-instance-tab', DemoReport, {
    id: 'demo-report',
    text: `plugins.${id}.title`
  })
}

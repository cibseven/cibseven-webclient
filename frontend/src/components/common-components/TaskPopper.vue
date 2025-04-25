<template>
  <div ref="targetRef">
    <slot :add="addTask" :download="triggerDownload" :busy="busy"></slot>
    <b-popover :manual="true" :placement="placement" ref="pop" @hidden="tasks = []"	:content="''">
      <template v-slot:title>
        <b-button-close style="margin-top: -0.25rem" @click="closePopover" :title="$t('task-popper.close')"></b-button-close>
        <span>{{ title }}</span>
      </template>
      <TaskList :tasks="tasks" :almost="delayAt"></TaskList>
    </b-popover>
  </div>
</template>

<script>
import TaskList from '../../components/common-components/TaskList.vue'

function repeatWithTimeout(interval, fun) {
  setTimeout(function() {	if (fun()) repeatWithTimeout(interval, fun)	}, interval)
}

export default {
  name: 'TaskPopper',
  components: { TaskList },
  props: { placement: String, target: [Function, String], title: String, delayAt: Number },
  data: function() { return {	tasks: [], busy: false } },
  methods: {
    addTask: function(name, cancel, promiseFactory) {
      var self = this
      var length = this.tasks.push({ name: name, cancel: cancel, state: null, progress: 0 })
      this.$refs.pop.$emit('open')
      if (promiseFactory) {
        return promiseFactory(handleProgress, fakeProgress).then(function(res) {
          update(true)
          return res
        }, function(err) {
          update(false)
          return Promise.reject(err)
        })
      } else return update

      function handleProgress(evt) { update(evt.loaded * (self.delayAt || 100) / evt.total) }
      function update(val) {
        self.tasks[length-1][typeof val === 'boolean' ? 'state' : 'progress'] = val
        self.busy = self.tasks.some(function(f) { return f.state == null })
        if (val === true && self.tasks.every(function(t) { return t.state }) && self.$refs.pop) // autoclose
          self.$refs.pop.$emit('close')
      }
      function fakeProgress() {
        var seq = [45, 66, 79, 87, 92, 95, 98, 99] // reverse fibonacci complement
        var i = 0
        repeatWithTimeout(1000, function() {
          update(seq[i++])
          return i < seq.length && self.tasks[length-1].state == null
        })
      }
    },
    triggerDownload: function(blob, filename) {
      if (navigator.msSaveBlob) navigator.msSaveBlob(blob, filename) // Internet Explorer
      else {
        var hiddenFile = document.createElement('a')
        hiddenFile.href = window.URL.createObjectURL(blob)
        hiddenFile.download = filename
        document.body.appendChild(hiddenFile)
        hiddenFile.click()
        setTimeout(function() { // Workaround for Edge
          document.body.removeChild(hiddenFile)
          window.URL.revokeObjectURL(hiddenFile.href)
        }, 500)
      }
    }
  }
}
</script>

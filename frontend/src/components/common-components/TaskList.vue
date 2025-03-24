<template>
  <div>
    <div class="row flex-nowrap" v-for="(task, index) in tasks" :key="index">
      <div class="col-10">
        <b-progress
          :variant="task.state == null ? 'info' : (task.state ? 'success' : 'danger')"
          :animated="task.progress === (almost || 100) && task.state == null">
          <b-progress-bar :value="task.progress" :label="task.name"></b-progress-bar>
        </b-progress>
      </div>
      <div class="col-1">
        <b-button-close v-if="task.cancel && task.state == null" @click="task.cancel" style="margin-top: -0.4rem" :title="$t('task-list.close')"></b-button-close>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'TaskList',
  props: { tasks: Array, almost: Number }
}
</script>

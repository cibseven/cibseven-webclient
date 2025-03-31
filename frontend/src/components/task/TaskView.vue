<template>
  <Task ref="task"
    :task="task"
    @complete-task="$emit('complete-task', $event)"
    @update-assignee="$emit('update-assignee', $event)"
    @update-task="$emit('update-task', $event)"
  ></Task>
</template>

<script>
import Task from '@/components/task/Task.vue'

export default {
  name: 'TaskView',
  components: { Task },
  props: { task: Object }
}
</script>

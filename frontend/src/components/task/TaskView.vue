<template>
  <TaskContent ref="task"
    :task="task"
    @complete-task="$emit('complete-task', $event)"
    @update-assignee="$emit('update-assignee', $event)"
    @update-task="$emit('update-task', $event)"
  ></TaskContent>
</template>

<script>
import TaskContent from '@/components/task/TaskContent.vue'

export default {
  name: 'TaskView',
  components: { TaskContent },
  props: { task: Object }
}
</script>

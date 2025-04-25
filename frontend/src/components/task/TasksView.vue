<template>
  <TasksContent ref="tasks"></TasksContent>
</template>

<script>
import TasksContent from '@/components/task/TasksContent.vue'

export default {
  name: 'TasksView',
  components: { TasksContent },
  watch: {
    '$route.query.filtername': function(filtername) {
      if (filtername) {
        if (this.$store.state.filter.list) {
          var taskId = this.$route.params.filterId
          var filter = this.$store.state.filter.list.find(filter => {
            return filtername.toUpperCase() === filter.name.toUpperCase()
          })
          if (filter) {
            this.$router.push('/seven/auth/tasks/' + filter.id + '/' + taskId)
          }
        }
      }
    }
  }
}
</script>

<!--

    Copyright CIB software GmbH and/or licensed to CIB software GmbH
    under one or more contributor license agreements. See the NOTICE file
    distributed with this work for additional information regarding copyright
    ownership. CIB software licenses this file to you under the Apache License,
    Version 2.0; you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

     Unless required by applicable law or agreed to in writing, software
     distributed under the License is distributed on an "AS IS" BASIS,
     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     See the License for the specific language governing permissions and
     limitations under the License.

-->
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

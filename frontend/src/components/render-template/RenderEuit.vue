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
  <BWaitingBox v-if="loader" class="h-100 d-flex justify-content-center" ref="loader" styling="width:20%"></BWaitingBox>
  <div v-show="!loader">
    <iframe v-once class="h-100 w-100" ref="euit-frame" frameBorder="0"
            src="" :title="task?.name" allow="fullscreen"
            allowfullscreen webkitallowfullscreen mozallowfullscreen oallowfullscreen msallowfullscreen></iframe>
  </div>
</template>

<script>
import { UiLocator } from 'cibseven-ui-locator'
import { BWaitingBox } from '@cib/common-frontend'

export default {
  name: 'RenderEuit',
  components: { BWaitingBox },
  emits: ['complete-task', 'error', 'cancel'],
  props: ['task'],
  data() {
    return {
      loader: true,
      locator: null
    }
  },
  watch: {
    task: {
      handler: function() {
        this.loadEuit()
      }
    }
  },
  mounted() {
    this.loadEuit()
  },
  beforeUnmount() {
    this.locator?.destroy()
  },
  methods: {
    loadEuit() {
      this.loader = true;
      if (!this.locator) {
        const formFrame = this.$refs['euit-frame']
  
        const resolveCallback = () => {
          this.$emit('complete-task', this.task)
        }
        const errorCallback = (error) => {
          this.$emit('error', error)
        }
        // Use UiLocator from local npm package
        this.locator = new UiLocator(
          this.$root.config.servicesBasePath + '/locator',
          formFrame,
          resolveCallback,
          errorCallback
        );
        const cancelCallback = () => {
          this.$emit('cancel')
        }
        this.locator.onCancel(cancelCallback);
      }

      this.locator.setAuthorization(this.$root.user.authToken);
      // Detect if this is a startform or a user task
      if (this.task.isStartform) {
        this.locator.showStartform(this.task.processDefinitionId)
      } else {
        this.locator.showUserTask(this.task.id)
      }
      this.loader = false
    }
  }
}
</script>

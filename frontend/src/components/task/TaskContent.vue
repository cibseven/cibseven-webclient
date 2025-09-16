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
  <div class="h-100 d-flex flex-column">
    <div class="visually-hidden" ref="ariaLiveText" aria-live="polite"></div>
    <div v-if="isMobile()" class="container-fluid border-top-0" style="min-height: 40px;">
      <div v-if="task" class="row pt-2">
        <div class="col-12">
          <b-input-group v-if="task.assignee == null">
            <b-input-group-prepend>
              <span><b-button ref="assignToMeButton" variant="link" class="p-0 text-dark me-2"
                  @click="assignee = $root.user.id"><span class="mdi mdi-18px mdi-account-question mdi-dark"></span> {{
                  $t('task.assignToMe') }}</b-button></span>
            </b-input-group-prepend>
            <FilterableSelect v-model:loading="loadingUsers" @enter="findUsers($event)"
              @clean-elements="resetUsers($event)" v-model="assignee" :elements="$store.state.user.searchUsers"
              :placeholder="$t('task.assign')" noInvalidValues />
          </b-input-group>
          <b-input-group v-else>
            <b-form-tag variant="secondary" @remove="assignee = null; update()" :key="task.id" :title="getCompleteName"
              :remove-label="$t('task.assignedUserTitle')" class="mdi mdi-18px mdi-account">
              {{ ' ' + getCompleteName }}
            </b-form-tag>
          </b-input-group>
        </div>
      </div>
    </div>
    <div v-else class="container-fluid border-bottom border-top-0 bg-white" style="min-height: 40px;">
      <div v-if="task" class="row py-2">
        <div class="col-12">
          <div class="form-inline">
            <div class="form-group d-flex align-items-center mb-0">
              <component ref="titleTask" tabindex="0" class="mb-0 me-4 d-inline" :is="isMobile() ? 'h6' : 'h5'">{{
                task.name }}</component>
              <span v-if="task.assignee != null">
                <b-form-tag variant="secondary" @remove="assignee = null; update()" :key="task.id"
                  :title="getCompleteName" :remove-label="$t('task.assignedUserTitle')"
                  class="mdi mdi-18px mdi-account">
                  {{ ' ' + getCompleteName }}
                </b-form-tag>
                <!-- <span class="mdi mdi-18px mdi-account mdi-dark"></span><span class="p-1" style="line-height: initial">{{ getCompleteName }}</span> -->
              </span>
              <span v-else>
                <b-button ref="assignToMeButton" variant="link" class="p-0 text-dark me-2"
                  @click="assignee = $root.user.id" :title="$t('infoAndHelp.shortcuts.shortcuts.claimTask')"><span
                    class="mdi mdi-18px mdi-account-question mdi-dark"></span> {{ $t('task.assignToMe') }}</b-button>
              </span>
              <FilterableSelect v-if="task.assignee == null" v-model:loading="loadingUsers" @enter="findUsers($event)"
                @clean-elements="resetUsers($event)" class="w-25" v-model="assignee"
                :elements="$store.state.user.searchUsers" :placeholder="$t('task.assign')" noInvalidValues />
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="flex-grow-1">
      <div class="overflow-auto shadow bg-white" style="margin: 10px; height: calc(100% - 25px)">
        <div class="h-100 p-2">
          <div class="h-100 position-relative" v-if="task">
            <div
              v-if="(task.assignee && task.assignee.toLowerCase() !== $root.user.id.toLowerCase()) || this.task.assignee == null"
              class="col-12 shadow-0"
              style="top: 0px; left: 0px; bottom: 0px; right: 0px; cursor: not-allowed; position: absolute; z-index: 1; background-color: rgba(238,238,238,0.33)">
            </div>
            <RenderTemplate v-if="task" :task="task" @click.prevent class="h-100"
              @complete-task="$emit('complete-task', $event)" :style="renderTemplateStyles"></RenderTemplate>
          </div>
        </div>
      </div>
    </div>
    <b-popover ref="howToAssignPopover" v-if="$root.config.layout.showPopoverHowToAssign"
      :target="function() { return $refs.titleTask }" :show="displayPopover && !isMobile()" placement="bottom"
      triggers="manual" max-width="300px">
      <b>{{ $t('task.assignPopoverTitle') }}</b>
      <p>{{ $t('task.assignPopoverContent') }}</p>
      <p>{{ $t('task.assignPopoverContent2') }}</p>
      <b-form-checkbox @input="disablePopover()">{{ $t('task.disablePopover') }}</b-form-checkbox>
      <img :alt="$t('task.assign')" src="@/assets/images/task/asign_task.svg">
    </b-popover>
    <ConfirmDialog ref="confirmTaskAssign" @ok="update()" @cancel="assignee = null">
      <span>{{ $t('confirm.assignUser') }}</span>
    </ConfirmDialog>
  </div>
</template>

<script>
import { nextTick } from 'vue'
import { mapActions } from 'vuex'
import { TaskService, AdminService } from '@/services.js'
import usersMixin from '@/mixins/usersMixin.js'
import RenderTemplate from '@/components/render-template/RenderTemplate.vue'
import FilterableSelect from '@/components/task/filter/FilterableSelect.vue'
import ConfirmDialog from '@/components/common-components/ConfirmDialog.vue'
import assigneeMixin from '@/mixins/assigneeMixin.js'

export default {
  name: 'TaskContent',
  components: { RenderTemplate, FilterableSelect, ConfirmDialog },
  mixins: [usersMixin, assigneeMixin],
  inject: ['isMobile'],
  props: { task: Object },
  emits: ['complete-task', 'update-assignee'],
  setup: function() {
    const POPOVER_DELAY = 1200 // 1.2 seconds
    return { POPOVER_DELAY }
  },
  data: function() {
    return {
      displayPopover: false,
      timer: null,
      loadingUsers: false,
      candidateUsers: []
    }
  },
  watch: {
    'task.assignee': {
      handler(val) {
        if (this.task && this.$store.state.task.selectedAssignee && this.task.id === this.$store.state.task.selectedAssignee.taskId) {
          this.setSelectedAssignee({ taskId: this.task.id, assignee: val })
          this.showPopoverWithDelay(val)
        }
      },
      immediate: true
    },
    assignee(val) {
      if (this.task && this.$store.state.task.selectedAssignee && this.task.id === this.$store.state.task.selectedAssignee.taskId && val != null) {
        this.checkAssignee()
      }
    },
    'task.id': {
      handler(taskId) {
        this.setSelectedAssignee({ selectedAssignee: this.task.assignee })
        this.$store.commit('setCandidateUsers', [])
        this.$store.commit('setSearchUsers', [])
        this.loadIdentityLinks(taskId)
        this.$refs.titleTask.focus()
        this.showPopoverWithDelay(this.task.assignee) // when opened task is changed
      },
      
    }
  },
  computed: {
    renderTemplateStyles: function() {
      if (this.task) {
        if ((this.task.assignee && this.task.assignee.toLowerCase() !== this.$root.user.id.toLowerCase()) ||
            (this.task.assignee == null && this.$root.config.notAssignedTaskLayer)) {
          return { filter: 'blur(1px)', '-webkit-filter': 'blur(1px)' }
        }
      }
      return {}
    }
  },
  mounted: function() {
    this.setSelectedAssignee({ taskId: this.task.id, assignee: this.task.assignee })
    this.$store.commit('setCandidateUsers', [])
    this.$store.commit('setSearchUsers', [])
    this.loadIdentityLinks(this.task.id)
    this.showPopoverWithDelay(this.task.assignee) // when first task is opened
  },
  beforeUnmount: function() {
    this.resetTimer() // stop timeout for showPopoverWithDelay()
    this.stopListeningTouchEvents()
  },
  methods: {
    ...mapActions('task', ['setSelectedAssignee']),
    claimCurrentTask: function() {
      if (!this.task.assignee) {
        this.assignee = this.$root.user.id
      }
    },
    loadIdentityLinks: function(taskId) {
      this.candidateUsers = []
      var promises = []
      TaskService.findIdentityLinks(taskId).then(identityLinks => {
        identityLinks.forEach(identityLink => {
          if (identityLink.type === 'candidate') {
            if (identityLink.groupId !== null) {
              var promise = AdminService.findUsers({ memberOfGroup: identityLink.groupId }).then(users => {
                this.$store.commit('setCandidateUsers', users)
                this.$store.commit('setSearchUsers', users)
              })
              promises.push(promise)
            }
            if (identityLink.userId !== null) {
              this.candidateUsers.push(identityLink.userId)
            }
          }
        })
        Promise.all(promises).then(() => {
          this.setAllUsersCandidates()
        })
      })
    },
    showPopoverWithDelay: function(assignee) {
      this.resetTimer()
      this.timer = setTimeout(() => {
        if (assignee === null || assignee !== this.$root.user.id) {
          this.displayPopover = localStorage.getItem('showPopoverHowToAssign') === 'false' ? false : true
          // To hide popover when clicking outside of it
          if (this.displayPopover) {
            this.startListeningTouchEvents()
          }
        }
      }, this.POPOVER_DELAY)
    },
    startListeningTouchEvents: function() {
      document.addEventListener("click", this.handleTouchEvent)
      document.addEventListener("focusin", this.handleTouchEvent)
    },
    stopListeningTouchEvents: function() {
      document.removeEventListener("click", this.handleTouchEvent)
      document.removeEventListener("focusin", this.handleTouchEvent)
    },
    handleTouchEvent: function(event) {
      // Hide popover when clicking outside of it
      if (this.$refs.howToAssignPopover && !this.$refs.howToAssignPopover.$el.contains(event.target)) {
        this.displayPopover = false
        this.stopListeningTouchEvents()
      }
    },
    resetTimer: function() {
      if (this.timer) {
        clearTimeout(this.timer)
        this.timer = null
      }
    },
    disablePopover: function() {
      localStorage.setItem('showPopoverHowToAssign', false)
      this.displayPopover = false
    },
    update: function() {
        if (!this.task || this.task.id !== this.$store.state.task.selectedAssignee.taskId) return
        this.$refs.ariaLiveText.textContent = ''
        TaskService.setAssignee(this.task.id, this.assignee).then(() => {
        // eslint-disable-next-line vue/no-mutating-props
        this.task.assignee = this.assignee
        this.setSelectedAssignee({ taskId: this.task.id, assignee: this.assignee })
        this.$emit('update-assignee', { taskId: this.task.id, assignee: this.task.assignee })
        if (this.task.assignee != null) {
          this.$refs.ariaLiveText.textContent = this.$t('task.userAssigned', [this.getCompleteName])
        }
        nextTick(() => {
          if (this.$refs.assignToMeButton) this.$refs.assignToMeButton.focus()
        })
      })
    },
    checkAssignee: function () {
      if (this.task.assignee && this.task.assignee === this.assignee) {
        return
      }
      TaskService.findTaskById(this.task.id).then(task => {
        if (task.assignee === null) {
          this.update()
        } else if (task.assignee.toLowerCase() === this.$root.user.id.toLowerCase()) {
          this.assignee = task.assignee
          this.setSelectedAssignee({ taskId: task.id, assignee: task.assignee })
          this.$emit('update-assignee', { taskId: task.id, assignee: task.assignee })
        } else {
          this.$refs.confirmTaskAssign.show()
        }
      })
    },
    setAllUsersCandidates: function() {
      if (!this.$root.config.layout.disableCandidateUsers) {
        this.$store.dispatch('findUsersByCandidates', { idIn: this.candidateUsers })
      }
    }
  }
}
</script>

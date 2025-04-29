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
  <div class="about">
    <h1>All UI elements</h1>
  </div>
  <GroupButtons></GroupButtons>
  <GroupLists></GroupLists>
  <GroupTabs></GroupTabs>
  <GroupDialogs></GroupDialogs>
  <GroupAvatars></GroupAvatars>
  <GroupAlerts></GroupAlerts>
  <GroupProgress></GroupProgress>
  <GroupWaitingBox></GroupWaitingBox>
  <GroupForm></GroupForm>
</template>

<script setup>
import GroupButtons from './GroupButtons.vue'
import GroupDialogs from './GroupDialogs.vue'
import GroupLists from './GroupLists.vue'
import GroupTabs from './GroupTabs.vue'
import GroupAvatars from './GroupAvatars.vue'
import GroupAlerts from './GroupAlerts.vue'
import GroupProgress from './GroupProgress.vue'
import GroupWaitingBox from './GroupWaitingBox.vue'
import GroupForm from './GroupForm.vue'
</script>

<style scoped>
</style>

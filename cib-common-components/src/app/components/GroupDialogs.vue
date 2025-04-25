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
    <h1>Dialogs</h1>
  </div>
  <div>
    <h2>Simple dialog</h2>

    <button @click="$refs.about.show()">Show BModal</button>
    <BModal ref="about" :title="'Custom Title'">
      <p>Custom body.</p>
      <p>Press OK to continue.</p>
      <template v-slot:modal-footer>
			  <BButton variant="primary" @click="$emit('ok'); $refs.about.hide()">{{$t('confirm.ok')}}</BButton>
		  </template>
    </BModal>

    &nbsp;

    <button @click="$refs.error.show()">Show Error dialog</button>
    <BModal ref="error" :title="$t('error.title')" :ok-title="$t('error.ok')" :ok-only="true">
      <div class="container-fluid">
      <div class="d-flex align-items-center">
        <div class="me-4">
          <span class="mdi-36px mdi mdi-alert-octagon-outline text-danger"></span>
        </div>
        <!-- min-width: auto leads to min-width: longest word so it will overflow the parent despite the overflow-wrap: break-word -->
        <div style="min-width: 0">
          <p class="my-4" style="overflow-wrap: break-word">{{ message }}</p>
        </div>
      </div>
    </div>
    </BModal>

    &nbsp;

    <button @click="$refs.error2.show()">With OK and CANCEL</button>
    <BModal ref="error2" :title="$t('error.title')" :ok-title="$t('error.ok')" :ok-only="false">
      <div class="container-fluid">
      <div class="d-flex align-items-center">
        <div class="me-4">
          <span class="mdi-36px mdi mdi-alert-octagon-outline text-danger"></span>
        </div>
        <!-- min-width: auto leads to min-width: longest word so it will overflow the parent despite the overflow-wrap: break-word -->
        <div style="min-width: 0">
          <p class="my-4" style="overflow-wrap: break-word">{{ message }}</p>
        </div>
      </div>
    </div>
    </BModal>
  </div>
</template>

<script setup>
import BButton from "@/components/BButton.vue";
import BModal from "@/components/BModal.vue";

const message = "Error message box body."
</script>

<style scoped>
</style>

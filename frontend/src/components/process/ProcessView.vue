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
  <ProcessDefinitionView
    v-if="computedVersionIndex && !isError"
    :processKey="processKey"
    :versionIndex="computedVersionIndex"
    :instanceId="computedInstanceId"
    :tenantId="tenantId"
  ></ProcessDefinitionView>

  <div v-else class="h-100 d-flex flex-column justify-content-center align-items-center">

    <!-- Show error message if the URL parameters are not valid -->
    <WarningBox v-if="isError">
      <h2 class="h5 text-danger">Error: Unrecognized process definition/instance URL</h2>
      <p class="text-muted">The URL must be in one of the following formats:</p>
      <ul class="text-muted">
        <li><code>#&#47;seven&#47;auth&#47;process&#47;:processKey</code> (alias for <code>#&#47;seven&#47;auth&#47;process&#47;:processKey&#47;:versionIndex</code> with the latest version)</li>
        <li><code>#&#47;seven&#47;auth&#47;process&#47;:processKey&#47;:versionIndex</code></li>
        <li><code>#&#47;seven&#47;auth&#47;process&#47;:processKey&#47;:versionIndex&#47;:instanceId</code></li>
      </ul>
      <p class="text-muted">If your process definition or instance has a tenant, please include it in the end of the URL as a mandatory parameter:
        <br>
        <code>#&#47;seven&#47;auth&#47;process&#47;:processKey&#47;:versionIndex&#47;:instanceId<span class="border border-primary p-1">?tenantId=:tenantId</span></code>.
      </p>
      <p class="text-muted">Please check the URL and try again.</p>
    </WarningBox>

    <!-- Show loading spinner while waiting for the latest version to be loaded -->
    <div v-else class="text-center">
      <BWaitingBox class="d-inline me-2" styling="width: 35px"></BWaitingBox> {{ $t('admin.loading') }}
    </div>

  </div>

</template>

<script>
import ProcessDefinitionView from '@/components/process/ProcessDefinitionView.vue'
import { BWaitingBox } from '@cib/common-frontend'
import WarningBox from '@/components/common-components/WarningBox.vue';

export default {
  name: 'ProcessView',
  components: { ProcessDefinitionView, BWaitingBox, WarningBox },
  props: {
    processKey: { type: String, required: true },
    versionIndex: { type: String, default: null },
    instanceId: { type: String, default: null },
    tenantId: { type: String, default: null }
  },
  watch: {
    combinedProps: {
      handler: 'openLatestVersion',
      immediate: true,
    },
  },
  data() {
    return {
      latestVersionIndex: null,
    }
  },
  computed: {
    isError() {
      if (!this.processKey) {
        // processKey is required, but check it anyway
        return true
      }
      if (this.instanceId && !this.versionIndex) {
        // instanceId is only valid if versionIndex is provided, otherwise it is an error
        return true
      }
      return false
    },
    combinedProps() {
      // Combine all props into a single computed property to watch for changes in any of them.
      // This allows us to react once to changes in any of the URL parameters and reload the process definition accordingly.
      return [
        this.processKey,
        this.versionIndex,
        this.tenantId,
        this.instanceId
      ];
    },
    computedVersionIndex() {
      return this.versionIndex || this.latestVersionIndex
    },
    computedInstanceId() {
      // only valid with proper "versionIndex"
      return this.versionIndex ? this.instanceId : null
    }
  },
  methods: {
    openLatestVersion() {
      if (!this.versionIndex && !this.instanceId) {
        // version was not specified =>
        // request URL is: #/process/:processKey (alias for #/process/:processKey/<latest>)
        // => get latest version and soft "redirect" (without changing the URL) to it
        // #/process/:processKey/:versionIndex
        this.$store.dispatch('getProcessByDefinitionKey', { key: this.processKey, tenantId: this.tenantId }).then(process => {
          this.latestVersionIndex = process?.version || null
        })
      }
    }
  },
}
</script>

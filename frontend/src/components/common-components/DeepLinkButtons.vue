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
  <button
    v-for="link in links" :key="link.id"
    @click="openLink(link)"
    :title="tooltip(link)"
    type="button"
    class="btn btn-sm btn-light" >
    {{ link.text }} 
  </button>
</template>

<script>
import { getDeepLinkEntries, resolveDeepLinkLabel, buildDeepLinkUrl } from '@/utils/deepLinks.js'

export default {
  name: 'DeepLinkButtons',
  props: {
    section: {
      type: String,
      required: true,
      validator: value => ['processInstance', 'processDefinition', 'decisionDefinition', 'decisionInstance'].includes(value)
    },
    params: { type: Object, required: true },
    collapseButtons: { type: Boolean, required: false, default: false },
  },
  computed: {
    links() {
      return getDeepLinkEntries(this.$root.config, this.section)
        .filter(entry => entry.type === 'button')
        .map(entry => ({
          ...entry,
          text: resolveDeepLinkLabel(this.$t, entry),
        }))
    }
  },
  methods: {
    tooltip(link) {
      return this.$t('deepLink.tooltip', {
        text: resolveDeepLinkLabel(this.$t, link),
        url: buildDeepLinkUrl(link.url, this.params),
      })
    },
    openLink(link) {
      const completeUrl = buildDeepLinkUrl(link.url, this.params)
      window.open(completeUrl, link.target || '_blank')
    }
  }
}
</script>

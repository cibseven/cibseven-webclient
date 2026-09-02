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
  <iframe :src="url" :title="title" width="100%" height="100%" frameBorder="0" class="border-0 overflow-hidden"></iframe>
</template>

<script>
import { buildDeepLinkUrl, resolveDeepLinkLabel } from '@/utils/deepLinks.js'

export default {
  name: 'DeepLinkFrame',
  props: {
    link: { type: Object, required: true },
    params: { type: Object, default: () => ({}) }
  },
  computed: {
    url() {
      if (!this.link) {
        return ''
      }
      return buildDeepLinkUrl(this.link.url, this.params)
    },
    title() {
      return resolveDeepLinkLabel(this.$t, this.link)
    }
  }
}
</script>

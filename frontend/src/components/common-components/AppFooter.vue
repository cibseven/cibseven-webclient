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
  <footer class="fixed-bottom footer-bg d-flex justify-content-center align-items-center gap-4 py-2" style="color: var(--bs-gray-800); background-color: var(--bs-gray-400);">
    <p class="m-0">CIB seven &emsp;&copy; CIB, {{ new Date().getFullYear() }} &emsp;Version {{ version }}</p>
    <a class="hovered-link link-underline link-underline-opacity-0" v-if="$root.config.flowLinkPrivacy" :href="$root.config.flowLinkPrivacy" target="_blank" rel="noopener noreferrer">{{ $t('infoAndHelp.flowLinkPrivacy') }}</a>
    <a class="hovered-link link-underline link-underline-opacity-0" v-if="$root.config.flowLinkImprint" :href="$root.config.flowLinkImprint" target="_blank" rel="noopener noreferrer">{{ $t('infoAndHelp.flowLinkImprint') }}</a>
  </footer>
</template>
<script>

import { InfoService } from '@/services';

export default {
  name: 'AppFooter',
  data: function() {
    return {
      version: ''
    }
  },
  async created() {
    if (this.version === '') {
      this.version = ' '
      InfoService.getVersion().then(version => {
        this.version = version || 'n/a'
      })
      .catch(error => {
        console.error("Error loading version: ", error)
        this.version = 'n/a'
      })
    }
  }
}
</script>

<style lang="css" scoped>
/* hover style for link*/

.hovered-link:hover {
  color: var(--bs-dark) !important;
}
</style>
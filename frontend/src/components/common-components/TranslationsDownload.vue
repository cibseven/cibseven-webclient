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
  <div style="display:none"></div>
</template>

<script setup>
import { onMounted } from 'vue'
import { i18n } from '@/i18n.js'

onMounted(() => {
  const currentLocale = i18n.global.locale.value || i18n.global.locale
  const messages = i18n.global.getLocaleMessage(currentLocale)

  const response = {
    locale: currentLocale,
    translations: messages
  }

  const blob = new Blob([JSON.stringify(response, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)

  const a = document.createElement('a')
  a.href = url
  a.download = `translations_${currentLocale}.json`
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
})
</script>

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
  <li class="nav-item dropdown">
  <a class="nav-link dropdown-toggle" href="javascript:void(0)" role="button" data-bs-toggle="dropdown"
    aria-expanded="false" aria-haspopup="true" :aria-label="$t('bcomponents.navbar.toggleDropdown')">
    <slot name="button-content"></slot>
  </a>
    <ul class="dropdown-menu dropdown-menu-end" role="menu">
      <slot></slot>
    </ul>
  </li>
</template>

<script>
export default {
}
</script>

<style scoped>
</style>

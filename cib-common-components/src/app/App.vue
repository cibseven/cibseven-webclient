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
  <header>
    <nav>
      <RouterLink to="/">All UI elements</RouterLink>
      <RouterLink to="/buttons">Buttons</RouterLink>
      <RouterLink to="/lists">Lists</RouterLink>
      <RouterLink to="/tabs">Tabs</RouterLink>
      <RouterLink to="/dialogs">Dialogs</RouterLink>
      <RouterLink to="/avatars">Avatars</RouterLink>
      <RouterLink to="/alerts">Alerts</RouterLink>
      <RouterLink to="/progress">Progress</RouterLink>
      <RouterLink to="/waiting-box">Waiting Box</RouterLink>
      <RouterLink to="/forms">Forms</RouterLink>
    </nav>
  </header>

  <div>
    <RouterView />
  </div>
</template>

<script setup>
import { RouterLink, RouterView } from 'vue-router'
</script>

<style scoped>
header {
  line-height: 1.5;
  max-height: 100vh;
}

nav {
  width: 100%;
  font-size: 12px;
  text-align: center;
  margin-top: 2rem;
}

nav a.router-link-exact-active {
  color: var(--color-text);
}

nav a.router-link-exact-active:hover {
  background-color: transparent;
}

nav a {
  display: inline-block;
  padding: 0 1rem;
  border-left: 1px solid var(--color-border);
}

nav a:first-of-type {
  border: 0;
}

div {
  margin-left: 10pt;
}
</style>

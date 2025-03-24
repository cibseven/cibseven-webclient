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

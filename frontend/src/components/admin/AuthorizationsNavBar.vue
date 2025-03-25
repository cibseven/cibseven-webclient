<template>
  <b-list-group>
    <b-list-group-item 
      v-for="resourceType in $root.config.admin.resourcesTypes" 
      :key="resourceType.id" 
      class="border-0 px-3 py-2"
      action 
      :to="`/seven/auth/admin/authorizations/${resourceType.id}/${resourceType.key}`"
      @click="$emit('middle')">
      <span> {{ $t(`admin.authorizations.resourcesTypes.${resourceType.key}`) }}</span>
    </b-list-group-item>
    <component v-if="ExtendedAuthorizations" :is="ExtendedAuthorizations"></component>
  </b-list-group>
</template>

<script>
export default {
  computed: {
    ExtendedAuthorizations: function() {
      return this.$options.components && this.$options.components.ExtendedAuthorizations
        ? this.$options.components.ExtendedAuthorizations
        : null
    }
  }
}
</script>
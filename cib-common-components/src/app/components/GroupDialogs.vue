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

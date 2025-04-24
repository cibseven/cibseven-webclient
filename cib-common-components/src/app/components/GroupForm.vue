<template>
  <div class="about">
    <h1>Form Elements</h1>
  </div>

  <div>
    <BForm>

      <BFormGroup>
        <BFormCheckbox
          id="checkbox1"
          name="checkbox1"
          value="checkbox1"
          label="Checkbox 1"
          :modelValue="true">Checkbox 1</BFormCheckbox>

        <BFormCheckbox
          id="checkbox2"
          name="checkbox2"
          value="checkbox2"
          label="Checkbox 2"
          :modelValue="true"
          :disabled="true">Checkbox 2</BFormCheckbox>
        </BFormGroup>

      <BFormDatepicker
        id="datepicker1"
        name="datepicker1"
        label="Datepicker 1"
        :value="new Date()" />

      <BFormFile
        id="file1"
        name="file1"
        label="File 1"
        :value="null" />

      <BFormSelect :options="[9,8,7]" :modelValue="7" />

      <BFormSelect :options="[1,2,3]" :modelValue="0">
        <template #first>
          <BFormSelectOption :value="0" disabled>
            Option 0
          </BFormSelectOption>
        </template> 
      </BFormSelect>

      <BFormSelect :options="[1,2,3]" :modelValue="4">
        <BFormSelectOption :value="4" disabled>Option 4</BFormSelectOption>
      </BFormSelect>

      <BFormTag
        id="select1"
        name="select1"
        label="Select 1"
        :options="[1,2,3]"
        :modelValue="2" />

      <BFormTextarea
        id="textarea1"
        name="textarea1"
        :modelValue="'Hello'"
        :placeholder="'Enter text here'"
        :rows="3"
        :resize="'vertical'"
        :textareaClass="'form-control'"
        :wrapperClass="'form-group'"
        :ariaDescribedby="'textarea1-help'"
        :ariaLabel="'Textarea 1'"
        :noResize="false"
        :resizeStyle="'vertical'"
        :parsedRows="3"
        >Hello World!</BFormTextarea>

      <BFormTimepicker
        id="timepicker1"
        name="timepicker1"
        label="Timepicker 1"
        :model-value="'12:34'"
        :placeholder="'Select time'"
        />

  </BForm>

  </div>

</template>

<script setup>
import BForm from "@/components/BForm.vue";
import BFormGroup from "@/components/BFormGroup.vue";
import BFormTag from "@/components/BFormTag.vue";
import BFormCheckbox from "@/components/BFormCheckbox.vue";
import BFormDatepicker from "@/components/BFormDatepicker.vue";
import BFormFile from "@/components/BFormFile.vue";
import BFormSelect from "@/components/BFormSelect.vue";
import BFormSelectOption from "@/components/BFormSelectOption.vue";
import BFormTextarea from "@/components/BFormTextarea.vue";
import BFormTimepicker from "@/components/BFormTimepicker.vue";
</script>

<style scoped>
</style>

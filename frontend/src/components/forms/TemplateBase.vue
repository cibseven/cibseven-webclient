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
  <div class="h-100 d-flex flex-column pt-2">
    <BWaitingBox v-if="loader" class="d-flex justify-content-center" ref="loader" styling="width:20%"></BWaitingBox>
    <div v-show="!loader">
      <slot name="fixed-row"></slot>

      <b-modal v-if="!noDiagramm" static ref="process" size="xl" :title="title" dialog-class="h-90" content-class="h-100">
        <div class="container-fluid h-100">
          <BpmnViewer v-if="templateMetaData" :activityInstance="templateMetaData.activityInstances"
          :activityInstanceHistory="templateMetaData.activityInstanceHistory" class="h-100" ref="diagram"></BpmnViewer>
        </div>
        <template v-slot:modal-footer>
          <b-button variant="secondary" @click="$refs.process.hide()">{{ $t('bpmn-viewer.accept') }}</b-button>
        </template>
      </b-modal>

    </div>
    <div v-show="!loader" class="flex-grow-1" style="min-height: 1px">
      <slot></slot>
    </div>

    <div v-if="showButtons" v-show="!loader" class="border-top pb-2 pt-3 shadow text-center bg-white" style="z-index: 9999">
      <slot name="button-row"></slot>
      <IconButton v-if="!isMobile" icon="fullscreen" @click="fullScreen()" :text="$t('actions.fullscreen')"></IconButton>
      <IconButton v-if="!noDiagramm && !isMobile()" icon="package" @click="showDiagram()" :text="$t('actions.showProcess')"></IconButton>
    </div>

  </div>
</template>

<script>

import { BWaitingBox } from 'cib-common-components'

import postMessageMixin from '@/components/forms/postMessage.js'
import IconButton from '@/components/forms/IconButton.vue'
import BpmnViewer from '@/components/process/BpmnViewer.vue'
import { FormsService, getServicesBasePath } from '@/services.js'
import { findDocumentPreviewComponents, getDocumentReferenceVariableName } from './formJsUtils.js'

export default {
  name: 'TemplateBase',
  props: { templateMetaData: Object, noDiagramm: Boolean, noTitle: Boolean, loader: Boolean, showButtons: { type: Boolean, default: true } },
  mixins: [postMessageMixin],
  components: { IconButton, BpmnViewer, BWaitingBox },
  inject: ['isMobile'],
  data: function() {
    return {
      // Stores files selected in the form, keyed by variable name, for later upload during submission
      formFiles: {}
    }
  },
  computed: {
    title: function() {
      return this.templateMetaData && this.templateMetaData.activityInstances.name + " - " + this.templateMetaData.task.name
    }
  },
  methods: {
    /**
     * Creates a document reference for file preview with endpoint and metadata
     * @param {string} variableName - Variable name for the file
     * @param {string} fileType - MIME type of the file
     * @param {string} fileName - Name of the file
     * @returns {Array} Document reference array for form-js
     */
    createDocumentReference(variableName, fileType, fileName) {
      const documentReference = {
        documentId: variableName,
        metadata: {
          contentType: fileType,
          fileName: fileName
        }
      };

      const authToken = this.$root.user.authToken;
      const encodedContentType = encodeURIComponent(fileType);
      const cacheBust = Date.now().toString();
      documentReference.endpoint = `${window.location.origin}/${getServicesBasePath()}/process/process-instance/${this.templateMetaData.task.processInstanceId}/variables/${variableName}/data?token=${authToken}&contentType=${encodedContentType}&cacheBust=${cacheBust}`;
      
      return [documentReference];
    },
    handleFileSelection: async function(event, fileInput, formularContent, taskId, form) {
      const file = event.target.files[0];
      if (file) {
        // Find the corresponding field in formContent to get the key
        let variableName = null;

        // Extract field ID from input ID (e.g., "fjs-form-0q23qer-Field_0cz77cj" -> "Field_0cz77cj")
        const fieldIdMatch = fileInput.id.match(/Field_[a-zA-Z0-9]+$/);
        const fieldId = fieldIdMatch ? fieldIdMatch[0] : null;

        if (fieldId && formularContent && formularContent.components) {
          const field = formularContent.components.find(component => component.id === fieldId);
          if (field && field.key) {
            variableName = field.key;
            // Store file for later upload - actual file upload will be done during form submission
            this.formFiles[variableName] = file;

            // Check if there's a documentPreview component that references this variable
            const documentPreviews = findDocumentPreviewComponents(formularContent);
            const documentReferenceVar = getDocumentReferenceVariableName(variableName);
            const hasDocumentPreview = documentPreviews.some(component =>
              component.dataSource === `=${documentReferenceVar}` || 
              component.dataSource === `=${variableName}`
            );

            // Create document reference only if there's a documentPreview component that uses this variable
            if (hasDocumentPreview) {

              // Document preview is only supported for task forms (taskId exists), not for start forms
              if (!taskId) {
                this.sendMessageToParent({
                  method: 'displayErrorMessage',
                  data: 'File preview is only available after the task has started.'
                });
                return; // Don't create document reference if preview is not possible
              }

              const uploadResponse = await FormsService.uploadVariableFileData(taskId, variableName, file, 'File');

              // Check if upload was successful (expecting 204 No Content)
              if (uploadResponse && uploadResponse.status !== 204) {
                this.sendMessageToParent({
                  method: 'displayErrorMessage',
                  data: 'Cannot preview this file due to file upload failure.'
                });
                return; // Don't create document reference if upload failed
              }

              // Create a document reference variable for preview (includes endpoint with auth token and file metadata)
              const documentReference = this.createDocumentReference(variableName, file.type, file.name);

              // Direct state update (more reliable for document previews)
              const formState = form._getState();
              formState.data[documentReferenceVar] = documentReference;
              form._setState(formState);
            }
          }
        }
      }
    },
    convertFilesToVariables: async function() {
      const fileVariables = {};
      
      if (this.formFiles && Object.keys(this.formFiles).length > 0) {
        for (const [key, file] of Object.entries(this.formFiles)) {
          // Convert file to base64
          const base64Content = await new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = () => {
              // Remove the data:*/*;base64, prefix to get only the base64 content
              const base64 = reader.result.split(',')[1];
              resolve(base64);
            };
            reader.onerror = () => {
              reject(new Error(`Failed to read file: ${file.name}`));
            };
            reader.readAsDataURL(file);
          });

          fileVariables[key] = {
            name: key,
            value: base64Content,
            type: "File",
            valueInfo: {
              filename: file.name,
              mimeType: file.type
            }
          };
        }
      }
      
      return fileVariables;
    },
    showDiagram: function () {
      this.$refs.process.show()
      //TODO: Review b-modal static
      setTimeout(() => {
        this.$refs.diagram.showDiagram(this.templateMetaData.bpmDiagram.bpmn20Xml, this.templateMetaData.activityInstances,
          this.templateMetaData.activityInstanceHistory)
      }, 500)
    },
    fullScreen: function() {
      var onFullscreenError = function() {
        //TODO: error on fullscreen or exit
      }
      if (!document.fullscreenElement) {
        if (document.documentElement.requestFullscreen)
          document.documentElement.requestFullscreen().catch(onFullscreenError)
        else if (document.documentElement.msRequestFullscreen)
          document.documentElement.msRequestFullscreen()
        else if (document.documentElement.webkitRequestFullscreen) {
          document.documentElement.webkitRequestFullscreen().catch(onFullscreenError)
        }
      } else {
        if (document.exitFullscreen) {
          document.exitFullscreen().catch(onFullscreenError)
        }
      }
    }
  }
}
</script>

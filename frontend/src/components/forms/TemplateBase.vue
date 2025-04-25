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

export default {
  name: 'TemplateBase',
  props: { templateMetaData: Object, noDiagramm: Boolean, noTitle: Boolean, loader: Boolean, showButtons: { type: Boolean, default: true } },
  mixins: [postMessageMixin],
  components: { IconButton, BpmnViewer, BWaitingBox },
  inject: ['isMobile'],
  computed: {
    title: function() {
      return this.templateMetaData && this.templateMetaData.activityInstances.name + " - " + this.templateMetaData.task.name
    }
  },
  methods: {
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

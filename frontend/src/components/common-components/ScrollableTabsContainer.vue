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
  <div class="tabs-scroll-container flex-grow-1" :style="containerStyle">
    <!-- Left scroll button -->
    <button 
      v-if="showLeftButton" 
      type="button" 
      @click="scrollLeft" 
      class="scroll-button scroll-button-left border border-start-0 btn btn-light position-absolute rounded-0" 
      :style="buttonStyle">
      <span class="mdi mdi-chevron-left"></span>
    </button>

    <!-- Tabs container (slot content) -->
    <div 
      ref="tabsContainer" 
      class="nav nav-tabs m-0 border-0 flex-nowrap tabs-scroll-container" 
      style="display: flex; overflow-y: hidden" 
      @scroll="checkScrollButtons">
      <slot></slot>
    </div>

    <!-- Right scroll button -->
    <button 
      v-if="showRightButton" 
      type="button" 
      @click="scrollRight" 
      class="scroll-button scroll-button-right border border-end-0 btn btn-light position-absolute rounded-0" 
      :style="buttonStyle">
      <span class="mdi mdi-chevron-right"></span>
    </button>
  </div>
</template>

<script>
import tabScrollButtons from '@/components/process/mixins/tabScrollButtons.js'

export default {
  name: 'ScrollableTabsContainer',
  mixins: [tabScrollButtons],
  watch: {
    activeTab() {
      this.$nextTick(() => this.scrollToActiveTab())
    }
  },
  props: {
    // Height of the tabs area (for button styling)
    tabsAreaHeight: {
      type: Number
    },
    // Container style (for white-space, etc.)
    containerStyle: {
      type: [String, Object],
      default: () => ({})
    },
    activeTab: {
      type: String,
      default: null
    }
  },
  computed: {
    buttonStyle() {
      return {
        height: this.tabsAreaHeight + 'px'
      }
    }
  }
}
</script>

<style scoped>
.tabs-scroll-container {
  overflow: hidden;
  scroll-behavior: smooth;
  -ms-overflow-style: none;  /* IE and Edge */
  scrollbar-width: none;  /* Firefox */
}
.tabs-scroll-container::-webkit-scrollbar {
  display: none;  /* Chrome, Safari and Opera */
}
.scroll-button {
  top: 0; 
  bottom: 0; 
  z-index: 10; 
  transition: all 0.2s ease;
}

.scroll-button-left {
  left: 0;
  box-shadow: 5px 0 5px -5px rgba(0, 0, 0, 0.1);
}

.scroll-button-right {
  right: 0;
  box-shadow: -5px 0 5px -5px rgba(0, 0, 0, 0.1);
}

/* Ensure buttons don't interfere with tab content */
.nav-tabs {
  position: relative;
  z-index: 0;
}
</style>

/*
 * Copyright CIB software GmbH and/or licensed to CIB software GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. CIB software licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
const DEFAULT_BPMN_VIEWER_HEIGHT = 400
const TOP_BAR_HEIGHT = 41
const DRAG_SELECTOR_HEIGHT = 10
const FILTER_HEIGHT = 60
const DEFAULT_TRANSITION_TIME = 0.4
const TABS_AREA_HEIGHT = 38

export default {
    data: function() {
        return {
            bpmnViewerOriginalHeight: DEFAULT_BPMN_VIEWER_HEIGHT,
            bpmnViewerHeight: DEFAULT_BPMN_VIEWER_HEIGHT,
            topBarHeight: TOP_BAR_HEIGHT,
            dragSelectorHeight: DRAG_SELECTOR_HEIGHT,
            filterHeight: FILTER_HEIGHT,
            mousePosition: null,
            toggleIcon: 'mdi-chevron-down',
            toggleTransition: '',
            transitionTime: DEFAULT_TRANSITION_TIME,
            tabsAreaHeight: TABS_AREA_HEIGHT
        }
    },
    computed: {
        bottomContentPosition: function() {
            return this.bpmnViewerHeight + this.topBarHeight + this.tabsAreaHeight
        }
    },
    methods: {
        handleMouseDown: function(e) {
            if (e.offsetY > this.bpmnViewerHeight - this.dragSelectorHeight) {
                this.mousePosition = e.y
                document.addEventListener('mousemove', this.resize, false)
                document.addEventListener('mouseup', this.handleMouseUp, false)
                document.body.style.userSelect = 'none'
            }
        },
        resize: function(e) {
            var dy = e.y - this.mousePosition
            this.mousePosition = e.y
            this.bpmnViewerHeight += dy
            if (this.bpmnViewerHeight < (this.bpmnViewerHeight + this.$refs.rContent.offsetHeight)) this.toggleIcon = 'mdi-chevron-down'
            else this.toggleIcon = 'mdi-chevron-up'
        },
        handleMouseUp: function() {
            document.removeEventListener('mousemove', this.resize, false)
            document.removeEventListener('mouseup', this.handleMouseUp, false)
            document.body.style.userSelect = "text"
        },
        toggleContent: function() {
            this.toggleTransition = 'transition: top '+ this.transitionTime +'s ease, height '+ this.transitionTime +'s ease'
            if (this.bpmnViewerHeight < (this.bpmnViewerHeight + this.$refs.rContent.offsetHeight)) {
                this.bpmnViewerHeight += this.$refs.rContent.offsetHeight
                if (this.$refs.filterTable) this.bpmnViewerHeight += this.$refs.filterTable.offsetHeight
                this.toggleIcon = 'mdi-chevron-up'
            }
            else {
                this.bpmnViewerHeight = this.bpmnViewerOriginalHeight
                this.toggleIcon = 'mdi-chevron-down'
            }
            setTimeout(() => { this.toggleTransition = '' }, this.transitionTime * 1000)
        }
    }
}
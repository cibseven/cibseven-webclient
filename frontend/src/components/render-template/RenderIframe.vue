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
    <BWaitingBox v-if="loader" class="h-100 d-flex justify-content-center" ref="loader" styling="width:20%"></BWaitingBox>
    <iframe v-show="!submitForm && formFrame" class="h-100 w-100" ref="template-frame" frameBorder="0"
            src="" :style="fullModeStyles" :title="task?.name" allow="fullscreen"
            allowfullscreen webkitallowfullscreen mozallowfullscreen oallowfullscreen msallowfullscreen></iframe>
</template>

<script>
import { BWaitingBox } from '@cib/common-frontend'

export default {
    name: 'RenderIframe',
    props: ['task'],
    emits: ['complete-task', 'error', 'cancel', 'saved', 'generic-success', 'filter-update'],
    components: { BWaitingBox },
    inject: ['currentLanguage', 'TaskService'],
    data: function() {
        return {
            submitForm: false,
            formFrame: true,
            loader: true
        }
    },
    computed: {
        fullModeStyles: function() {
            if (this.$route.query.fullMode === 'true') {
                return 'position: fixed; top: 0; left: 0; z-index: 1030'
            }
            return ''
        }
    },
    watch: {
        task: {
        handler(newVal, oldVal) {
            if (newVal && oldVal && newVal.id !== oldVal.id) {
            this.onBeforeUnload()
            }
        },
        immediate: true
        }
    },
    mounted: function() {
        this.loadIframe()
        window.addEventListener('message', this.processMessage)

        //window.addEventListener('beforeunload', this.processMessage)
        window.onbeforeunload = function() {
        this.onBeforeUnload()
        }.bind(this)
    },
    methods: {
        loadIframe: function() {
            this.loader = true
            this.submitForm = false
            this.formFrame = true
            const theme = localStorage.getItem('theme') || this.$root.theme
            let themeContext = ''
            let translationContext = ''
            if (['cib', 'generic'].includes(theme) || !theme) {
                themeContext = encodeURIComponent('bootstrap/bootstrap_4.5.0.min.css?v=1.14.0')
            }
            else {
                translationContext = 'themes/' + theme + '/uiet-translations_'
                themeContext = 'themes/' + theme + '/bootstrap_4.5.0.min.css'
            }

            const formFrame = this.$refs['template-frame']
            //Startforms
            if (this.task.url) {
                formFrame.src = this.task.url + '/' + themeContext + '/' + translationContext

                this.loader = false
            } else if (this.task.isStartform && this.task.processDefinitionId  && this.task.formKey.startsWith('embedded:')) {
                formFrame.src = `embedded-forms.html?processDefinitionId=${this.task.processDefinitionId}&lang=${this.currentLanguage()}&authorization=${this.$root.user.authToken}`
                
                this.loader = false
            } else if (this.task.id) {
                //Embedded forms if not "standard" ui-element-templates
                if (this.task.formKey && this.task.formKey.startsWith('embedded:') && this.task.formKey !== 'embedded:/camunda/app/tasklist/ui-element-templates/template.html') {
                    this.formFrame = true
                    formFrame.src = `embedded-forms.html?taskId=${this.task.id}&lang=${this.currentLanguage()}&authorization=${this.$root.user.authToken}`
                    this.loader = false
                } else {
                    let formReferencePromise
                    //Camunda Forms
                    if (this.task.camundaFormRef) {
                        formReferencePromise = Promise.resolve('deployed-form')
                    } else {
                        formReferencePromise = this.TaskService.formReference(this.task.id)
                    }
                    formReferencePromise.then(formReference => {    
                        //Empty Tasks
                        if (formReference === 'empty-task') {
                            this.loader = false
                            this.formFrame = false
                            return
                        }
                        //Ui-element-templates
                        
                        formFrame.src = '#/' + formReference + '/' + this.currentLanguage() + '/' +
                        this.task.id + '/' + this.$root.user.authToken + '/' + themeContext + '/' + translationContext

                        this.loader = false
                    }, () => {
                        // Not needed but just in case something changes in the backend method
                        this.formFrame = false
                        this.loader = false
                    })
                }
            }
        },
        processMessage: function(e) {
            const formFrame = this.$refs['template-frame']
            if (e.source === formFrame.contentWindow && e.data.method) {
                if (e.data.method === 'completeTask') this.$emit('complete-task', e.data.task)
                else if (e.data.method === 'displaySuccessMessage') this.$emit('saved')
                else if (e.data.method === 'displayGenericSuccessMessage') this.$emit('generic-success')
                else if (e.data.method === 'displayErrorMessage') this.$emit('error')
                else if (e.data.method === 'cancelTask') this.$emit('cancel')
                else if (e.data.method === 'updateFilters') this.$emit('filter-update')
        //					let res = this[e.data.method](e.data)
        //					if (e.data.callback) {
        //						formFrame.contentWindow.postMessage({
        //						    'callback': e.data.callback,
        //							'result': res
        //						}, '*');
        //					}
            }
        },
        onBeforeUnload: function() {
            const formFrame = this.$refs['template-frame']
            if (formFrame) {
                formFrame.contentWindow.postMessage({ type: 'contextChanged' }, '*');
                this.loadIframe()
            }
        }
    },
    unmounted: function() {
        this.onBeforeUnload()
    },
    beforeUnmount: function() {
        window.removeEventListener('message', this.processMessage)
    }
}
</script>
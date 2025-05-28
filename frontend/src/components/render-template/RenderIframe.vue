<template>
    <BWaitingBox v-if="loader" class="h-100 d-flex justify-content-center" ref="loader" styling="width:20%"></BWaitingBox>
    <iframe v-show="!submitForm && formFrame" class="h-100 w-100" ref="template-frame" frameBorder="0"
            src="" :style="fullModeStyles" allow="fullscreen"
            allowfullscreen webkitallowfullscreen mozallowfullscreen oallowfullscreen msallowfullscreen></iframe>
</template>

<script>
import { BWaitingBox } from 'cib-common-components'

export default {
    name: 'RenderIframe',
    props: ['task'],
    emits: ['complete-task', 'error', 'cancel'],
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
            var theme = localStorage.getItem('theme') || this.$root.theme
            var themeContext = ''
            var translationContext = ''
            if (['cib', 'generic'].includes(theme) || !theme) {
                themeContext = encodeURIComponent('bootstrap/bootstrap_4.5.0.min.css?v=1.14.0')
            }
            else {
                translationContext = 'themes/' + theme + '/uiet-translations_'
                themeContext = 'themes/' + theme + '/bootstrap_4.5.0.min.css'
            }

            let formFrame = this.$refs['template-frame']
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
            var formFrame = this.$refs['template-frame']
            if (e.source === formFrame.contentWindow && e.data.method) {
                if (e.data.method === 'completeTask') this.$emit('complete-task', e.data.task)
                else if (e.data.method === 'displaySuccessMessage') this.$emit('saved')
                else if (e.data.method === 'displayGenericSuccessMessage') this.$emit('generic-success')
                else if (e.data.method === 'displayErrorMessage') this.$emit('error')
                else if (e.data.method === 'cancelTask') this.$emit('cancel')
                else if (e.data.method === 'updateFilters') this.$emit('filter-update')
        //					var res = this[e.data.method](e.data)
        //					if (e.data.callback) {
        //						formFrame.contentWindow.postMessage({
        //						    'callback': e.data.callback,
        //							'result': res
        //						}, '*');
        //					}
            }
        },
        onBeforeUnload: function() {
            var formFrame = this.$refs['template-frame']
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
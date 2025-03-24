export default {
    data: function() {
        return {
            bpmnViewerOriginalHeight: 400,
            bpmnViewerHeight: 400,
            topBarHeight: 40,
            dragSelectorHeight: 10,
            filterHeight: 60,
            mousePosition: null,
            toggleIcon: 'mdi-chevron-down',
            toggleButtonHeight: 40,
            toggleTransition: '',
            transitionTime: 0.4

        }
    },
    computed: {
        bottomContentPosition: function() {
            return this.bpmnViewerHeight + this.topBarHeight
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
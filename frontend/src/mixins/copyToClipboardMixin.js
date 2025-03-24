export default {
    methods: {
        copyValueToClipboard: function(val) {
            if (val) {
                var textToCopy = (typeof val === 'object') ? JSON.stringify(val) : val
                var tmpHtml = document.createElement('textarea')
                document.body.appendChild(tmpHtml)
                tmpHtml.value = textToCopy
                tmpHtml.select()
                document.execCommand('copy')
                document.body.removeChild(tmpHtml)
                this.$refs.messageCopy.show()
            }
        }
    }
}
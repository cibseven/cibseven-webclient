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
export default {
    methods: {
        copyValueToClipboard: function(val) {
            if (val) {
                const textToCopy = (typeof val === 'object') ? JSON.stringify(val) : val
                const previousFocus = document.activeElement
                // Append inside an open modal, otherwise its focus trap prevents select()
                const container = previousFocus?.closest('.modal') ?? document.body
                const tmpHtml = document.createElement('textarea')
                tmpHtml.value = textToCopy
                tmpHtml.setAttribute('readonly', '')
                tmpHtml.style.position = 'fixed'
                tmpHtml.style.opacity = '0'
                container.appendChild(tmpHtml)
                tmpHtml.select()
                const copied = document.execCommand('copy')
                container.removeChild(tmpHtml)
                previousFocus?.focus()
                if (copied) this.$refs.messageCopy.show()
            }
        }
    }
}
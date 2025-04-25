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
  <b-modal ref="support" :title="$t('infoAndHelp.flowModalSupport.title')" :ok-only="true">
    <div class="container-fluid">
      <div class="row">
        <div class="col-12">
          <div class="row pb-2">
            <div class="col-12 pb-3 h2">
              {{ isTextProperty('support-hotline') ? getTextFromProperty('support-hotline') : $t('infoAndHelp.flowModalSupport.supportHotline') }}
            </div>
          </div>
          <div class="row pb-2">
            <div class="col-6 float-left">
              {{ isTextProperty('opening-hours') ? getTextFromProperty('opening-hours') : $t('infoAndHelp.flowModalSupport.openingHours') }}
            </div>
            <div class="col-6 float-right">
              {{ isTextProperty('opening-hours-info') ? getTextFromProperty('opening-hours-info') : $t('infoAndHelp.flowModalSupport.openingHoursInfo') }}
            </div>
          </div>
          <div class="row pb-2">
            <div class="col-6 float-left">
              {{ isTextProperty('phone') ? getTextFromProperty('phone') : $t('infoAndHelp.flowModalSupport.phone') }}
            </div>
            <div class="col-6 float-right">
              <a :href="'tel:' + phone.replace(/\s/g,'')">{{ phone }}</a>
            </div>
          </div>
          <div class="row pb-2">
            <div class="col-6 float-left">
              {{ isTextProperty('email-address') ? getTextFromProperty('email-address') : $t('infoAndHelp.flowModalSupport.emailAddress') }}
            </div>
            <div class="col-6 float-right">
              <a :href="'mailto:' + email">{{ email }}</a>
            </div>
          </div>
        </div>
      </div>
    </div>
  </b-modal>
</template>

<script>
export default {
  name: 'SupportModal',
  computed: {
    phone: function() {
      return this.isTextProperty('phone-number') ? this.getTextFromProperty('phone-number') : this.$t('infoAndHelp.flowModalSupport.phoneNumber')
    },
    email: function() {
      return this.isTextProperty('email') ? this.getTextFromProperty('email') : this.$t('infoAndHelp.flowModalSupport.email')
    }
  },
  methods: {
    isTextProperty: function(property) {
      let language = localStorage.getItem('language')
      if (this.$root.config.supportDialog && this.$root.config.supportDialog[language] &&
          this.$root.config.supportDialog[language][property]) return true
      return false
    },
    getTextFromProperty: function(property) {
      let language = localStorage.getItem('language')
      if (this.$root.config.supportDialog && this.$root.config.supportDialog[language] &&
        this.$root.config.supportDialog[language][property])
          return this.$root.config.supportDialog[language][property]
      return false
    },
    show: function() {
      this.$refs.support.show()
    }
  }
}
</script>

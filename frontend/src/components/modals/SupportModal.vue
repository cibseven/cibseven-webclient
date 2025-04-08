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
  data: function() {
    return {
      phone: this.isTextProperty('phone-number') ? this.getTextFromProperty('phone-number') : this.$t('infoAndHelp.flowModalSupport.phoneNumber'),
      email: this.isTextProperty('email') ? this.getTextFromProperty('email') : this.$t('infoAndHelp.flowModalSupport.email'),
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

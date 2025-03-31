<template>
  <b-modal ref="support" :title="$t('infoAndHelp.flowModalSupport.title')" okOnly>
    <div class="container-fluid">
      <div class="row">
        <div class="col-12">
          <div class="row pb-2">
            <div class="col-12 pb-3 h2">
            {{ isTextProperty('support-hotline') ? getTextFromProperty('support-hotline') : $t('infoAndHelp.flowModalSupport.supportHotline') }}</div>
          </div>
          <div class="row pb-2">
            <div class="col-6 float-left">
            {{ isTextProperty('opening-hours') ? getTextFromProperty('opening-hours') : $t('infoAndHelp.flowModalSupport.openingHours') }}</div>
            <div class="col-6 float-right">
            {{ isTextProperty('opening-hours-info') ? getTextFromProperty('opening-hours-info') : $t('infoAndHelp.flowModalSupport.openingHoursInfo') }}</div>
          </div>
          <div class="row pb-2">
            <div class="col-6 float-left">
            {{ isTextProperty('phone') ? getTextFromProperty('phone') : $t('infoAndHelp.flowModalSupport.phone') }}</div>
            <div class="col-6 float-right">
            {{ isTextProperty('phone-number') ? getTextFromProperty('phone-number') : $t('infoAndHelp.flowModalSupport.phoneNumber') }}</div>
          </div>
          <div class="row pb-2">
            <div class="col-6 float-left">
            {{ isTextProperty('email-address') ? getTextFromProperty('email-address') : $t('infoAndHelp.flowModalSupport.emailAddress') }}</div>
            <div class="col-6 float-right">
            {{ isTextProperty('email') ? getTextFromProperty('email') : $t('infoAndHelp.flowModalSupport.email') }}</div>
          </div>
        </div>
      </div>
    </div>
  </b-modal>
</template>

<script>
export default {
  name: 'SupportModal',
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

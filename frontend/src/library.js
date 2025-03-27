
// Import the CSS to ensure it is bundled with the package
import './assets/main.css';

import BAlert from './components/BAlert.vue'
import BAvatar from './components/BAvatar.vue'
import BBadge from './components/BBadge.vue'
import BButton from './components/BButton.vue'
import BButtonClose from './components/BButtonClose.vue'
import BCalendar from './components/BCalendar.vue'
import BCard from './components/BCard.vue'
import BCardBody from './components/BCardBody.vue'
import BCardText from './components/BCardText.vue'
import BCol from './components/BCol.vue'
import BCollapse from './components/BCollapse.vue'
import BDdItemBtn from './components/BDdItemBtn.vue'
import BDPopover from './components/BDPopover.vue'
import BDropdown from './components/BDropdown.vue'
import BDropdownDivider from './components/BDropdownDivider.vue'
import BDropdownForm from './components/BDropdownForm.vue'
import BDropdownGroup from './components/BDropdownGroup.vue'
import BDropdownItem from './components/BDropdownItem.vue'
import BDropdownItemButton from './components/BDropdownItemButton.vue'
import BForm from './components/BForm.vue'
import BFormCheckbox from './components/BFormCheckbox.vue'
import BFormDatepicker from './components/BFormDatepicker.vue'
import BFormFile from './components/BFormFile.vue'
import BFormGroup from './components/BFormGroup.vue'
import BFormInput from './components/BFormInput.vue'
import BFormRow from './components/BFormRow.vue'
import BFormSelect from './components/BFormSelect.vue'
import BFormSelectOption from './components/BFormSelectOption.vue'
import BFormTag from './components/BFormTag.vue'
import BFormTextarea from './components/BFormTextarea.vue'
import BFormTimepicker from './components/BFormTimepicker.vue'
import BInputGroup from './components/BInputGroup.vue'
import BInputGroupAppend from './components/BInputGroupAppend.vue'
import BInputGroupPrepend from './components/BInputGroupPrepend.vue'
import BInputGroupText from './components/BInputGroupText.vue'
import BLink from './components/BLink.vue'
import BListGroup from './components/BListGroup.vue'
import BListGroupItem from './components/BListGroupItem.vue'
import BModal from './components/BModal.vue'
import BNavItemDropdown from './components/BNavItemDropdown.vue'
import BNavbar from './components/BNavbar.vue'
import BNavbarBrand from './components/BNavbarBrand.vue'
import BNavbarNav from './components/BNavbarNav.vue'
import BNavbarToggle from './components/BNavbarToggle.vue'
import BOverlay from './components/BOverlay.vue'
import BPagination from './components/BPagination.vue'
import BPopover from './components/BPopover.vue'
import BProgress from './components/BProgress.vue'
import BProgressBar from './components/BProgressBar.vue'
import BSpinner from './components/BSpinner.vue'
import BTab from './components/BTab.vue'
import BTabs from './components/BTabs.vue'
import BToggle from './components/BToggle.vue'
import BWaitingBox from './components/BWaitingBox.vue'

const registerComponents = function(app) {

  app.component('b-alert', BAlert)
  app.component('b-avatar', BAvatar)
  app.component('b-badge', BBadge)
  app.component('b-button', BButton)
  app.component('b-button-close', BButtonClose)
  app.component('b-calendar', BCalendar)
  app.component('b-card', BCard)
  app.component('b-card-body', BCardBody)
  app.component('b-card-text', BCardText)
  app.component('b-col', BCol)
  app.component('b-collapse', BCollapse)
  app.component('b-dd-item-btn', BDdItemBtn)
  app.component('b-dropdown', BDropdown)
  app.component('b-dropdown-divider', BDropdownDivider)
  app.component('b-dropdown-form', BDropdownForm)
  app.component('b-dropdown-group', BDropdownGroup)
  app.component('b-dropdown-item', BDropdownItem)
  app.component('b-dropdown-item-button', BDropdownItemButton)
  app.component('b-form', BForm)
  app.component('b-form-checkbox', BFormCheckbox)
  app.component('b-form-datepicker', BFormDatepicker)
  app.component('b-form-file', BFormFile)
  app.component('b-form-group', BFormGroup)
  app.component('b-form-input', BFormInput)
  app.component('b-form-row', BFormRow)
  app.component('b-form-select', BFormSelect)
  app.component('b-form-select-option', BFormSelectOption)
  app.component('b-form-tag', BFormTag)
  app.component('b-form-textarea', BFormTextarea)
  app.component('b-form-timepicker', BFormTimepicker)
  app.component('b-input-group', BInputGroup)
  app.component('b-input-group-append', BInputGroupAppend)
  app.component('b-input-group-prepend', BInputGroupPrepend)
  app.component('b-input-group-text', BInputGroupText)
  app.component('b-link', BLink)
  app.component('b-list-group', BListGroup)
  app.component('b-list-group-item', BListGroupItem)
  app.component('b-modal', BModal)
  app.component('b-nav-item-dropdown', BNavItemDropdown)
  app.component('b-navbar', BNavbar)
  app.component('b-navbar-brand', BNavbarBrand)
  app.component('b-navbar-nav', BNavbarNav)
  app.component('b-navbar-toggle', BNavbarToggle)
  app.component('b-overlay', BOverlay)
  app.component('b-pagination', BPagination)
  app.component('b-popover', BPopover)
  app.component('b-progress', BProgress)
  app.component('b-progress-bar', BProgressBar)
  app.component('b-spinner', BSpinner)
  app.component('b-tab', BTab)
  app.component('b-tabs', BTabs)
  app.component('b-waiting-box', BWaitingBox)
  app.directive('b-popover', BDPopover)

  const eventBus = mitt()
  app.directive('b-toggle', BToggle(eventBus))
  app.config.globalProperties.$eventBus = eventBus
}

import en from '@/assets/translations_en.json'
import de from '@/assets/translations_de.json'
import es from '@/assets/translations_es.json'
import it from '@/assets/translations_it.json'
import ru from '@/assets/translations_ru.json'

const mergeLocaleMessage = function(i18n, lang) {
  if (lang === 'en') {
    i18n.global.mergeLocaleMessage(lang, en)
  }
  else if (lang === 'de') {
    i18n.global.mergeLocaleMessage(lang, de)
  }
  else if (lang === 'es') {
    i18n.global.mergeLocaleMessage(lang, es)
  }
  else if (lang === 'it') {
    i18n.global.mergeLocaleMessage(lang, it)
  }
  else if (lang === 'ru') {
    i18n.global.mergeLocaleMessage(lang, ru)
  }
  else {
    i18n.global.mergeLocaleMessage(lang, en)
  }
}

export {
  registerComponents,
  mergeLocaleMessage,

  BAlert,
  BAvatar,
  BBadge,
  BButton,
  BButtonClose,
  BCalendar,
  BCard,
  BCardBody,
  BCardText,
  BCol,
  BCollapse,
  BDPopover,
  BDdItemBtn,
  BDropdown,
  BDropdownDivider,
  BDropdownForm,
  BDropdownGroup,
  BDropdownItem,
  BDropdownItemButton,
  BForm,
  BFormCheckbox,
  BFormDatepicker,
  BFormFile,
  BFormGroup,
  BFormInput,
  BFormRow,
  BFormSelect,
  BFormSelectOption,
  BFormTag,
  BFormTextarea,
  BFormTimepicker,
  BInputGroup,
  BInputGroupAppend,
  BInputGroupPrepend,
  BInputGroupText,
  BLink,
  BListGroup,
  BListGroupItem,
  BModal,
  BNavItemDropdown,
  BNavbar,
  BNavbarBrand,
  BNavbarNav,
  BNavbarToggle,
  BOverlay,
  BPagination,
  BPopover,
  BProgress,
  BProgressBar,
  BSpinner,
  BTab,
  BTabs,
  BToggle,
  BWaitingBox
}

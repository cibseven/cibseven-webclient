import { createI18n } from 'vue-i18n'

import en from '../assets/translations_en.json'
import de from '../assets/translations_de.json'
import es from '../assets/translations_es.json'
import it from '../assets/translations_it.json'
import ru from '../assets/translations_ru.json'

export const languages = {
  en: en,
  de: de,
  es: es,
  it: it,
  ru: ru
}

const i18n = createI18n({
  legacy: false,
  locale: localStorage.getItem('language') || navigator.language.split('-')[0] || navigator.userLanguage || 'en',
  fallbackLocale: 'en',
  messages: languages
})

export default i18n

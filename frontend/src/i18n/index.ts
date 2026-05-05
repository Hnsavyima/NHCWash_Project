import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import LanguageDetector from 'i18next-browser-languagedetector';

import fr from './locales/fr.json';
import en from './locales/en.json';
import nl from './locales/nl.json';
import de from './locales/de.json';

i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources: {
      fr: { translation: fr },
      en: { translation: en },
      nl: { translation: nl },
      de: { translation: de },
    },
    supportedLngs: ['fr', 'en', 'nl', 'de'],
    fallbackLng: 'fr',
    interpolation: {
      escapeValue: false,
    },
    detection: {
      order: ['localStorage', 'navigator'],
      caches: ['localStorage'],
      /** Default i18next key; kept in sync by `applyPreferredLanguage` when loading the user from the API */
      lookupLocalStorage: 'i18nextLng',
    },
  });

export default i18n;

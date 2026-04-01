import i18next from "i18next";
import en from "./en.json";
import svSE from "./sv_SE.json";

export const SUPPORTED_LOCALES = ["en", "sv_SE"] as const;
export type Locale = (typeof SUPPORTED_LOCALES)[number];

let initialized = false;

export function initI18n(locale: string = "en") {
  if (!initialized) {
    i18next.init({
      lng: locale,
      fallbackLng: "en",
      resources: {
        en: { translation: en },
        sv_SE: { translation: svSE },
      },
      interpolation: {
        escapeValue: false,
      },
    });
    initialized = true;
  } else {
    i18next.changeLanguage(locale);
  }
}

export function t(key: string, params?: Record<string, string | number>): string {
  return i18next.t(key, params) as string;
}

export { i18next };

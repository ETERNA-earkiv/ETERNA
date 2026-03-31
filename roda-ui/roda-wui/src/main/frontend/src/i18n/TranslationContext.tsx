import { createContext, useContext, useState } from 'react'
import { sv } from './sv'

type Translations = Record<string, string>

interface TranslationContextValue {
  t: (key: string, fallback?: string) => string
  locale: string
  setLocale: (locale: string) => void
}

const TranslationContext = createContext<TranslationContextValue | null>(null)

const LOCALE_KEY = 'eterna.locale'
const DEFAULT_LOCALE = 'sv'

const BUNDLES: Record<string, Translations> = {
  sv,
}

function loadBundle(locale: string): Translations {
  return BUNDLES[locale] ?? BUNDLES[DEFAULT_LOCALE] ?? {}
}

interface TranslationProviderProps {
  children: React.ReactNode
}

export function TranslationProvider({ children }: TranslationProviderProps) {
  const [locale, setLocaleState] = useState(
    () => localStorage.getItem(LOCALE_KEY) ?? DEFAULT_LOCALE
  )
  const [bundle, setBundle] = useState<Translations>(() => loadBundle(locale))

  function setLocale(newLocale: string) {
    localStorage.setItem(LOCALE_KEY, newLocale)
    setLocaleState(newLocale)
    setBundle(loadBundle(newLocale))
  }

  function t(key: string, fallback?: string): string {
    return bundle[key] ?? fallback ?? key
  }

  return (
    <TranslationContext.Provider value={{ t, locale, setLocale }}>
      {children}
    </TranslationContext.Provider>
  )
}

export function useTranslation() {
  const ctx = useContext(TranslationContext)
  if (!ctx) {
    throw new Error('useTranslation must be used inside TranslationProvider')
  }
  return ctx
}

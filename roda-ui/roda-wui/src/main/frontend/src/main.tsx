import React from 'react'
import ReactDOM from 'react-dom/client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { TranslationProvider } from './i18n/TranslationContext'
import App from './App'
import '@designsystem-se/af/dist/digi-arbetsformedlingen/digi-arbetsformedlingen.css'
import '@designsystem-se/af/dist/digi-arbetsformedlingen/digi-arbetsformedlingen.esm.js'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: 1,
    },
  },
})

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <TranslationProvider>
        <App />
      </TranslationProvider>
    </QueryClientProvider>
  </React.StrictMode>
)

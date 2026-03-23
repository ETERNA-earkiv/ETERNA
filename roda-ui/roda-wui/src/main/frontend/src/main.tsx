import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'
import '@designsystem-se/af/dist/digi-arbetsformedlingen/digi-arbetsformedlingen.css'
import '@designsystem-se/af/dist/digi-arbetsformedlingen/digi-arbetsformedlingen.esm.js'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
)

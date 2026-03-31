// Type declarations for Digi Web Components used as JSX elements.
// The @designsystem-se/af-react package provides React wrappers,
// but we use the web component names directly here for simplicity in the POC.

import type { DetailedHTMLProps, HTMLAttributes } from 'react'

type WCProps = DetailedHTMLProps<HTMLAttributes<HTMLElement>, HTMLElement> & {
  [key: string]: unknown
}

declare global {
  namespace JSX {
    interface IntrinsicElements {
      'digi-button': WCProps
      'digi-input': WCProps
      'digi-form-input': WCProps
      'digi-table': WCProps
      'digi-pagination': WCProps
      'digi-loader': WCProps
      'digi-message': WCProps
      'digi-navigation-menu': WCProps
      'digi-navigation-menu-item': WCProps
    }
  }
}

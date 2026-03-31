/**
 * Module augmentations for @designsystem-se/af-react.
 *
 * The Stencil-generated React wrappers have incomplete prop type inference
 * in TypeScript 5.x due to how the component class types are resolved.
 * We re-declare the specific components we use with their correct props.
 */

declare module '@designsystem-se/af-react' {
  import type { ReactNode, RefAttributes, HTMLAttributes } from 'react'

  type DigiVariation = 'primary' | 'secondary' | 'action'
  type DigiNotificationVariation = 'info' | 'warning' | 'danger' | 'success'
  type DigiFormInputType = 'text' | 'password' | 'email' | 'number' | 'tel' | 'url'
  type DigiLoaderVariation = 'primary' | 'secondary' | 'white'

  interface DigiTableProps extends HTMLAttributes<HTMLElement> {
    children?: ReactNode
    [key: string]: unknown
  }

  interface DigiButtonProps extends HTMLAttributes<HTMLElement> {
    children?: ReactNode
    afVariation?: DigiVariation | string
    afAriaLabel?: string
    onAfOnClick?: (event: CustomEvent) => void
    onAfOnFocus?: (event: CustomEvent) => void
    onAfOnBlur?: (event: CustomEvent) => void
    style?: React.CSSProperties
    [key: string]: unknown
  }

  interface DigiNotificationAlertProps extends HTMLAttributes<HTMLElement> {
    children?: ReactNode
    afVariation?: DigiNotificationVariation | string
    afHeading?: string
    afCloseable?: boolean
    style?: React.CSSProperties
    [key: string]: unknown
  }

  interface DigiFormInputProps extends HTMLAttributes<HTMLElement> {
    afLabel?: string
    afId?: string
    afType?: DigiFormInputType | string
    afRequired?: boolean
    afValue?: string
    onAfOnInput?: (event: CustomEvent) => void
    onAfOnKeyup?: (event: CustomEvent) => void
    onAfOnChange?: (event: CustomEvent) => void
    [key: string]: unknown
  }

  interface DigiFormInputSearchProps extends HTMLAttributes<HTMLElement> {
    afLabel?: string
    afId?: string
    afValue?: string
    onAfOnInput?: (event: CustomEvent) => void
    onAfOnSubmitSearch?: (event: CustomEvent) => void
    [key: string]: unknown
  }

  interface DigiLoaderSpinnerProps extends HTMLAttributes<HTMLElement> {
    afVariation?: DigiLoaderVariation | string
    [key: string]: unknown
  }

  interface DigiNavigationPaginationProps extends HTMLAttributes<HTMLElement> {
    afInitActivePage?: number
    afTotalPages?: number
    onAfOnPageChange?: (event: CustomEvent) => void
    [key: string]: unknown
  }

  export const DigiTable: React.FC<DigiTableProps>
  export const DigiButton: React.FC<DigiButtonProps>
  export const DigiNotificationAlert: React.FC<DigiNotificationAlertProps>
  export const DigiFormInput: React.FC<DigiFormInputProps>
  export const DigiFormInputSearch: React.FC<DigiFormInputSearchProps>
  export const DigiLoaderSpinner: React.FC<DigiLoaderSpinnerProps>
  export const DigiNavigationPagination: React.FC<DigiNavigationPaginationProps>
}

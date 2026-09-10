import { DigiFormInput } from '@designsystem-se/af-react';
import {
  FormInputType,
  FormInputVariation,
  FormInputValidation,
} from '@designsystem-se/af';
import type { ComponentProps } from 'react';
import { definedProps } from './helpers';

export interface PortalInputProps {
  label: string;
  value?: string;
  onChange?: (value: string) => void;
  onBlur?: () => void;
  type?: 'text' | 'email' | 'password' | 'search' | 'tel' | 'url' | 'number' | 'date';
  size?: 'small' | 'medium' | 'large';
  validation?: 'neutral' | 'success' | 'error' | 'warning';
  validationText?: string;
  required?: boolean;
  requiredText?: string;
  placeholder?: string;
  description?: string;
  disabled?: boolean;
  ariaLabel?: string;
  autoComplete?: string;
}

const typeMap: Record<string, FormInputType> = {
  text: FormInputType.TEXT,
  email: FormInputType.EMAIL,
  password: FormInputType.PASSWORD,
  search: FormInputType.SEARCH,
  tel: FormInputType.TEL,
  url: FormInputType.URL,
  number: FormInputType.NUMBER,
  date: FormInputType.DATE,
};

const sizeMap: Record<string, FormInputVariation> = {
  small: FormInputVariation.SMALL,
  medium: FormInputVariation.MEDIUM,
  large: FormInputVariation.LARGE,
};

const validationMap: Record<string, FormInputValidation> = {
  neutral: FormInputValidation.NEUTRAL,
  success: FormInputValidation.SUCCESS,
  error: FormInputValidation.ERROR,
  warning: FormInputValidation.WARNING,
};

/** Digi lägger det nativa eventet i `detail`; värdet sitter på dess target. */
function readDigiValue(e: CustomEvent): string {
  const nativeEvent = e.detail;
  return String(nativeEvent?.target?.value ?? nativeEvent?.value ?? '');
}

export function PortalInput({
  label,
  value,
  onChange,
  onBlur,
  type = 'text',
  size = 'medium',
  validation = 'neutral',
  validationText,
  required,
  requiredText,
  placeholder,
  description,
  disabled,
  ariaLabel,
  autoComplete,
}: PortalInputProps) {
  const digiProps = definedProps({
    afLabel: label,
    afLabelDescription: description,
    afType: typeMap[type],
    afVariation: sizeMap[size],
    afValidation: validationMap[validation],
    afValidationText: validationText,
    afRequired: required,
    afRequiredText: requiredText,
    afPlaceholder: placeholder,
    afValue: value,
    afAriaLabel: ariaLabel,
    afAutocomplete: autoComplete,
    disabled,
    // Digi skickar afOnInput vid varje inmatning och afOnChange först vid nativt
    // change. Vi lyssnar på båda så att värdet når React-state direkt — annars
    // kan t.ex. ett datumfilter tillämpas innan datumet hunnit registreras.
    onAfOnInput: (e: CustomEvent) => onChange?.(readDigiValue(e)),
    onAfOnChange: (e: CustomEvent) => onChange?.(readDigiValue(e)),
    onAfOnBlur: () => onBlur?.(),
  }) as ComponentProps<typeof DigiFormInput>;

  return (
    <DigiFormInput {...digiProps} />
  );
}

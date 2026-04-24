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
    onAfOnChange: (e: CustomEvent) => {
      const nativeEvent = e.detail;
      const val = nativeEvent?.target?.value ?? nativeEvent?.value ?? '';
      onChange?.(val);
    },
    onAfOnBlur: () => onBlur?.(),
  }) as ComponentProps<typeof DigiFormInput>;

  return (
    <DigiFormInput {...digiProps} />
  );
}

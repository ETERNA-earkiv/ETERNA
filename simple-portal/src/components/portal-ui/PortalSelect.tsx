import { DigiFormSelect } from '@designsystem-se/af-react';
import { FormSelectVariation, FormSelectValidation } from '@designsystem-se/af';
import type { ComponentProps } from 'react';
import { definedProps } from './helpers';

export interface SelectOption {
  value: string;
  label: string;
}

export interface PortalSelectProps {
  label: string;
  value?: string;
  onChange?: (value: string) => void;
  options: SelectOption[];
  size?: 'small' | 'medium' | 'large';
  required?: boolean;
  placeholder?: string;
  validation?: 'neutral' | 'error' | 'warning';
  validationText?: string;
}

const sizeMap: Record<string, FormSelectVariation> = {
  small: FormSelectVariation.SMALL,
  medium: FormSelectVariation.MEDIUM,
  large: FormSelectVariation.LARGE,
};

const validationMap: Record<string, FormSelectValidation> = {
  neutral: FormSelectValidation.NEUTRAL,
  error: FormSelectValidation.ERROR,
  warning: FormSelectValidation.WARNING,
};

export function PortalSelect({
  label,
  value,
  onChange,
  options,
  size = 'medium',
  required,
  placeholder,
  validation = 'neutral',
  validationText,
}: PortalSelectProps) {
  const digiProps = definedProps({
    afLabel: label,
    afVariation: sizeMap[size],
    afRequired: required,
    afPlaceholder: placeholder,
    afValue: value,
    afValidation: validationMap[validation],
    afValidationText: validationText,
    onAfOnChange: (e: CustomEvent) => {
      const nativeEvent = e.detail;
      const val = nativeEvent?.target?.value ?? nativeEvent?.value ?? '';
      onChange?.(val);
    },
  }) as ComponentProps<typeof DigiFormSelect>;

  return (
    <DigiFormSelect {...digiProps}>
      {options.map((opt) => (
        <option key={opt.value} value={opt.value}>
          {opt.label}
        </option>
      ))}
    </DigiFormSelect>
  );
}

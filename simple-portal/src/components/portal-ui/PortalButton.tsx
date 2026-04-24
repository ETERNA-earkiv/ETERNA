import { DigiButton } from '@designsystem-se/af-react';
import { ButtonVariation, ButtonSize, ButtonType } from '@designsystem-se/af';
import type { ComponentProps, ReactNode } from 'react';
import { definedProps } from './helpers';

export interface PortalButtonProps {
  variant?: 'primary' | 'secondary' | 'function' | 'tertiary';
  size?: 'small' | 'medium';
  fullWidth?: boolean;
  type?: 'button' | 'submit' | 'reset';
  onClick?: () => void;
  disabled?: boolean;
  loading?: boolean;
  ariaLabel?: string;
  children: ReactNode;
}

const variationMap: Record<string, ButtonVariation> = {
  primary: ButtonVariation.PRIMARY,
  secondary: ButtonVariation.SECONDARY,
  function: ButtonVariation.FUNCTION,
  tertiary: ButtonVariation.TERTIARY,
};

const typeMap: Record<string, ButtonType> = {
  button: ButtonType.BUTTON,
  submit: ButtonType.SUBMIT,
  reset: ButtonType.RESET,
};

export function PortalButton({
  variant = 'primary',
  size = 'medium',
  fullWidth,
  type = 'button',
  onClick,
  disabled,
  loading,
  ariaLabel,
  children,
}: PortalButtonProps) {
  const digiProps = definedProps({
    afVariation: variationMap[variant],
    afSize: size === 'small' ? ButtonSize.SMALL : ButtonSize.MEDIUM,
    afFullWidth: fullWidth,
    afType: typeMap[type],
    afAriaLabel: ariaLabel,
    disabled: disabled || loading,
    onAfOnClick: onClick,
  }) as ComponentProps<typeof DigiButton>;

  return (
    <DigiButton {...digiProps}>
      {loading ? 'Laddar...' : children}
    </DigiButton>
  );
}
